<#
    Convenience stub: starts the self-contained kit from the project folder.

    The real launcher is START-DEMO.cmd / start-demo.ps1 in the kit root.
#>

& (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) '..\start-demo.ps1') @args
exit $LASTEXITCODE
