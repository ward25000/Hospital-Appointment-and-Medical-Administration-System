export const store = {
  user: null,
  health: null,
};

export function navigate(path) {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  const target = `#${normalized}`;
  if (window.location.hash === target) {
    window.dispatchEvent(new Event('app:route'));
    return;
  }
  window.location.hash = target;
}

export function hasRole(...roles) {
  return Boolean(store.user && roles.includes(store.user.role));
}

export function isAdmin() {
  return store.user?.role === 'ADMIN';
}

export function canSchedule() {
  return hasRole('ADMIN', 'RECEPTIONIST');
}

export function canViewPatients() {
  return hasRole('ADMIN', 'RECEPTIONIST');
}

export function canViewReports() {
  return hasRole('ADMIN', 'RECEPTIONIST');
}

export function canManageCatalog() {
  return isAdmin();
}
