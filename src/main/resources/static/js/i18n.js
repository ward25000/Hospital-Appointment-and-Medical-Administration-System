// Russian text is the key, the dictionary holds [Kazakh, English]. The server uses the same file.

export const LANGS = [
  { code: 'kk', label: 'ҚАЗ', name: 'Қазақша' },
  { code: 'ru', label: 'РУС', name: 'Русский' },
  { code: 'en', label: 'ENG', name: 'English' },
];

const STORAGE_KEY = 'medflow.lang';
let dictionary = {};
let lang = readStored();

function readStored() {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    return LANGS.some((item) => item.code === value) ? value : 'ru';
  } catch {
    return 'ru';
  }
}

export function getLang() {
  return lang;
}

export async function loadDictionary() {
  try {
    const response = await fetch('/i18n/dictionary.json');
    dictionary = response.ok ? await response.json() : {};
  } catch {
    dictionary = {};
  }
  document.documentElement.lang = lang;
}

function lookup(text, target = lang) {
  if (target === 'ru') {
    return text;
  }
  return dictionary[text]?.[target === 'kk' ? 0 : 1] || text;
}

function fill(text, params) {
  return params.reduce((result, value, index) => result.replaceAll(`{${index}}`, String(value ?? '')), text);
}

export function t(text, params = []) {
  return fill(lookup(text), params);
}

/** Plural forms: Russian ones come from the call, the others from the "plural:" entries. */
export function plural(count, forms) {
  const number = Math.abs(Number(count) || 0);
  if (lang === 'ru') {
    const tail = number % 100;
    const last = tail % 10;
    if (tail > 10 && tail < 20) return forms[2];
    if (last > 1 && last < 5) return forms[1];
    return last === 1 ? forms[0] : forms[2];
  }
  const variants = lookup(`plural:${forms[0]}`).replace(/^plural:/, '').split('|');
  return number === 1 ? variants[0] : variants[variants.length - 1];
}

/** Fills elements marked with data-i18n / data-i18n-placeholder / data-i18n-label; values typed by the user stay. */
export function translateStatic(root = document) {
  root.querySelectorAll('[data-i18n]').forEach((element) => {
    element.textContent = t(element.dataset.i18n);
  });
  root.querySelectorAll('[data-i18n-placeholder]').forEach((element) => {
    element.placeholder = t(element.dataset.i18nPlaceholder);
  });
  root.querySelectorAll('[data-i18n-label]').forEach((element) => {
    element.setAttribute('aria-label', t(element.dataset.i18nLabel));
    if (element.title) element.title = t(element.dataset.i18nLabel);
  });
}

// ---------------------------------------------------------------------------
// Switching the language while a form is open: the markup is translated in
// place, so nothing the user has typed or selected is lost.
// ---------------------------------------------------------------------------

function buildReverse(from) {
  const exact = new Map();
  const patterns = [];
  Object.keys(dictionary).forEach((key) => {
    if (key.startsWith('plural:')) return;
    const text = lookup(key, from);
    if (/\{\d\}/.test(text)) {
      const order = [];
      const source = text
        .replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        .replace(/\\\{(\d)\\\}/g, (match, index) => {
          order.push(Number(index));
          return '(.+?)';
        });
      patterns.push({ key, order, regex: new RegExp(`^${source}$`, 's'), length: text.length });
    } else if (!exact.has(text)) {
      exact.set(text, key);
    }
  });
  patterns.sort((left, right) => right.length - left.length);
  return { exact, patterns };
}

function convert(text, reverse, to, entities) {
  const core = text.trim();
  if (!core) return text;
  const result = convertCore(core, reverse, to, entities);
  return result === core ? text : text.replace(core, () => result);
}

function convertCore(core, reverse, to, entities) {
  if (reverse.exact.has(core)) return lookup(reverse.exact.get(core), to);
  if (entities.has(core)) return entities.get(core);
  for (const { key, order, regex } of reverse.patterns) {
    const match = regex.exec(core);
    if (match) {
      const params = [];
      order.forEach((index, position) => {
        params[index] = convertCore(match[position + 1], reverse, to, entities);
      });
      return fill(lookup(key, to), params);
    }
  }
  for (const separator of [' · ', ' — ']) {
    if (core.includes(separator)) {
      return core.split(separator).map((part) => convert(part, reverse, to, entities)).join(separator);
    }
  }
  return core;
}

function translateInPlace(root, from, to, entities) {
  const reverse = buildReverse(from);
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  while (walker.nextNode()) {
    const node = walker.currentNode;
    if (node.parentElement?.closest('textarea, [data-no-translate]')) continue;
    const next = convert(node.nodeValue, reverse, to, entities);
    if (next !== node.nodeValue) node.nodeValue = next;
  }
  ['placeholder', 'title', 'aria-label'].forEach((name) => {
    root.querySelectorAll(`[${name}]`).forEach((element) => {
      element.setAttribute(name, convert(element.getAttribute(name), reverse, to, entities));
    });
  });
}

/**
 * Changes the interface language. {@code loadEntities} returns pairs [old name, new name]
 * for directory entries (services, departments) shown inside open forms.
 */
export async function changeLang(next, { openRoots = [], loadEntities = null } = {}) {
  if (next === lang || !LANGS.some((item) => item.code === next)) {
    return false;
  }
  const previous = lang;
  lang = next;
  try {
    localStorage.setItem(STORAGE_KEY, next);
  } catch {
    // the choice simply will not survive a reload
  }
  document.documentElement.lang = next;
  translateStatic();
  const roots = openRoots.filter((root) => root && root.childElementCount > 0);
  if (roots.length > 0) {
    let entities = new Map();
    try {
      entities = new Map(loadEntities ? await loadEntities(previous, next) : []);
    } catch {
      // directory names keep the previous language until the form is reopened
    }
    roots.forEach((root) => translateInPlace(root, previous, next, entities));
  }
  return true;
}
