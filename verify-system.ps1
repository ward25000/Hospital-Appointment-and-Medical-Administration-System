#requires -Version 5.1
<#
    MedFlow end-to-end smoke check.

    Start the application first (separate window):
        .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"

    Then run:
        powershell -ExecutionPolicy Bypass -File .\verify-system.ps1

    The script only uses public REST API and demo accounts from README.
    It creates one appointment, checks double-booking protection,
    cancels it, and verifies reporting / role restrictions.
#>

param(
    [string]$BaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'
$script:passed = 0
$script:failed = 0

function Write-Check {
    param(
        [string]$Name,
        [bool]$Ok,
        [string]$Details = ''
    )
    if ($Ok) {
        $script:passed++
        Write-Host ("[PASS] {0} {1}" -f $Name, $Details) -ForegroundColor Green
    } else {
        $script:failed++
        Write-Host ("[FAIL] {0} {1}" -f $Name, $Details) -ForegroundColor Red
    }
}

function Invoke-JsonGet {
    param(
        [string]$Uri,
        $Session
    )
    return Invoke-RestMethod -Uri $Uri -Method Get -WebSession $Session -UseBasicParsing
}

function Get-HttpStatus {
    param(
        [string]$Uri,
        [string]$Method = 'GET',
        $Session,
        [hashtable]$Headers = @{},
        [string]$Body = $null
    )
    try {
        $requestArgs = @{
            Uri             = $Uri
            Method          = $Method
            UseBasicParsing = $true
        }
        if ($Session) { $requestArgs.WebSession = $Session }
        if ($Headers -and $Headers.Count -gt 0) { $requestArgs.Headers = $Headers }
        if ($Body) {
            $requestArgs.Body = $Body
            $requestArgs.ContentType = 'application/json'
        }
        $response = Invoke-WebRequest @requestArgs
        return [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        return 0
    }
}

function Get-CsrfHeaders {
    param($Session)
    Invoke-WebRequest -Uri "$BaseUrl/api/auth/csrf" -WebSession $Session -UseBasicParsing | Out-Null
    $cookie = $Session.Cookies.GetCookies([uri]$BaseUrl) |
        Where-Object { $_.Name -eq 'XSRF-TOKEN' } |
        Select-Object -First 1
    if (-not $cookie) { throw 'CSRF cookie XSRF-TOKEN was not issued' }
    return @{ 'X-XSRF-TOKEN' = [System.Uri]::UnescapeDataString($cookie.Value) }
}

function New-ApiSession {
    param(
        [string]$Username,
        [string]$Password
    )
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $headers = Get-CsrfHeaders -Session $session
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json -Compress
    $user = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post `
        -WebSession $session -Headers $headers -ContentType 'application/json' `
        -Body $body -UseBasicParsing
    $headers = Get-CsrfHeaders -Session $session
    return [pscustomobject]@{
        Session = $session
        Headers = $headers
        User    = $user
    }
}

Write-Host "MedFlow verification against $BaseUrl" -ForegroundColor Cyan

# --- 0. Application availability -------------------------------------------
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/api/health" -UseBasicParsing
} catch {
    Write-Host "Cannot reach $BaseUrl/api/health. Start the application first." -ForegroundColor Red
    exit 2
}
Write-Check 'Health endpoint is UP' ($health.status -eq 'UP') "status=$($health.status), db=$($health.database)"

$openApiStatus = Get-HttpStatus "$BaseUrl/v3/api-docs"
Write-Check 'OpenAPI JSON is public' ($openApiStatus -eq 200) "HTTP $openApiStatus"

$index = Invoke-WebRequest -Uri "$BaseUrl/" -UseBasicParsing
Write-Check 'SPA index is served' ($index.StatusCode -eq 200 -and $index.Content -match 'MedFlow') "HTTP $($index.StatusCode)"

$calendar = Invoke-WebRequest -Uri "$BaseUrl/js/views/calendar.js" -UseBasicParsing
Write-Check 'Calendar UI module is served' ($calendar.StatusCode -eq 200) "HTTP $($calendar.StatusCode)"

# --- 1. Anonymous access ----------------------------------------------------
$anonymousStatus = Get-HttpStatus "$BaseUrl/api/doctors"
Write-Check 'Anonymous API request is rejected' ($anonymousStatus -eq 401) "HTTP $anonymousStatus (expected 401)"

# --- 2. Login for all three roles ------------------------------------------
$admin = New-ApiSession -Username 'admin' -Password 'Admin123!'
Write-Check 'ADMIN login' ($admin.User.role -eq 'ADMIN') "role=$($admin.User.role)"

$receptionist = New-ApiSession -Username 'reception' -Password 'Reception123!'
Write-Check 'RECEPTIONIST login' ($receptionist.User.role -eq 'RECEPTIONIST') "role=$($receptionist.User.role)"

$doctor = New-ApiSession -Username 'doctor.ivanov' -Password 'Doctor123!'
Write-Check 'DOCTOR login' ($doctor.User.role -eq 'DOCTOR' -and $doctor.User.doctorId) "role=$($doctor.User.role), doctorId=$($doctor.User.doctorId)"

# --- 3. Role restrictions ---------------------------------------------------
$status = Get-HttpStatus "$BaseUrl/api/patients" 'GET' $doctor.Session
Write-Check 'DOCTOR cannot read patient registry' ($status -eq 403) "HTTP $status (expected 403)"

$status = Get-HttpStatus "$BaseUrl/api/reports" 'GET' $doctor.Session
Write-Check 'DOCTOR cannot read reports' ($status -eq 403) "HTTP $status (expected 403)"

$doctorCreateBody = @{
    username = 'admin'
    password = 'Doctor123!'
    specializationId = 1
    firstName = 'Security'
    lastName = 'Check'
    email = 'a.ivanov@medcenter.ru'
    phone = '+7 (900) 000-00-00'
    office = '000'
} | ConvertTo-Json -Compress
$status = Get-HttpStatus "$BaseUrl/api/doctors" 'POST' $receptionist.Session $receptionist.Headers $doctorCreateBody
Write-Check 'RECEPTIONIST cannot create doctors' ($status -eq 403) "HTTP $status (expected 403)"

$status = Get-HttpStatus "$BaseUrl/api/reports" 'GET' $receptionist.Session
Write-Check 'RECEPTIONIST can read reports' ($status -eq 200) "HTTP $status (expected 200)"

# --- 4. Demo data sanity ----------------------------------------------------
$doctors = Invoke-JsonGet "$BaseUrl/api/doctors" $admin.Session
$patients = Invoke-JsonGet "$BaseUrl/api/patients" $admin.Session
$services = Invoke-JsonGet "$BaseUrl/api/services" $admin.Session
Write-Check 'Demo doctors / patients / services exist' `
    ($doctors.Count -ge 4 -and $patients.Count -ge 12 -and $services.Count -ge 8) `
    "doctors=$($doctors.Count), patients=$($patients.Count), services=$($services.Count)"

# --- 5. Server-side availability and booking --------------------------------
$doctorId = $doctors[0].id
$patientId = $patients[0].id
$serviceId = $services[0].id
$slot = $null
$slotDate = $null

for ($dayOffset = 0; $dayOffset -le 21 -and -not $slot; $dayOffset++) {
    $date = (Get-Date).Date.AddDays($dayOffset).ToString('yyyy-MM-dd')
    $availability = Invoke-JsonGet `
        "$BaseUrl/api/doctors/$doctorId/availability?date=$date&serviceId=$serviceId" $admin.Session
    $cancelled = Invoke-JsonGet `
        "$BaseUrl/api/appointments?from=$date&to=$date&doctorId=$doctorId&status=CANCELLED" $admin.Session
    $cancelledStarts = @($cancelled | ForEach-Object { $_.startTime })
    $slot = $availability.slots |
        Where-Object { $_.available -and ($cancelledStarts -notcontains $_.startTime) } |
        Select-Object -First 1
    if ($slot) { $slotDate = $date }
}

Write-Check 'Server returned at least one free slot' ($null -ne $slot) `
    "doctorId=$doctorId, date=$slotDate, start=$($slot.startTime)"

if (-not $slot) {
    Write-Host "No free slot within 21 days; aborting booking checks." -ForegroundColor Red
} else {
    $appointmentBody = @{
        doctorId  = $doctorId
        patientId = $patientId
        serviceId = $serviceId
        startTime = $slot.startTime
        notes     = 'verification smoke test'
    } | ConvertTo-Json -Compress

    $createResponse = Invoke-WebRequest -Uri "$BaseUrl/api/appointments" -Method Post `
        -WebSession $admin.Session -Headers $admin.Headers -ContentType 'application/json' `
        -Body $appointmentBody -UseBasicParsing
    $created = $createResponse.Content | ConvertFrom-Json
    Write-Check 'POST /api/appointments creates booking' ($createResponse.StatusCode -eq 201) `
        "HTTP $($createResponse.StatusCode), id=$($created.id)"
    Write-Check 'New booking has SCHEDULED status' ($created.status -eq 'SCHEDULED') `
        "status=$($created.status), price=$($created.price)"
    Write-Check 'Price is snapshotted from the service' ($created.price -eq $services[0].price) `
        "appointmentPrice=$($created.price), servicePrice=$($services[0].price)"
    Write-Check 'End time is calculated from service duration' `
        ([datetime]$created.endTime -eq ([datetime]$created.startTime).AddMinutes($services[0].durationMinutes)) `
        "$($created.startTime) -> $($created.endTime)"

    $duplicateStatus = Get-HttpStatus "$BaseUrl/api/appointments" 'POST' `
        $admin.Session $admin.Headers $appointmentBody
    Write-Check 'Double booking of the same slot is rejected' ($duplicateStatus -eq 409) `
        "HTTP $duplicateStatus (expected 409)"

    $availabilityAfter = Invoke-JsonGet `
        "$BaseUrl/api/doctors/$doctorId/availability?date=$slotDate&serviceId=$serviceId" $admin.Session
    $busySlot = $availabilityAfter.slots |
        Where-Object { $_.startTime -eq $slot.startTime } |
        Select-Object -First 1
    Write-Check 'Booked slot is reported as unavailable' ($busySlot -and -not $busySlot.available) `
        "available=$($busySlot.available), reason=$($busySlot.reason)"

    $pastBody = @{
        doctorId  = $doctorId
        patientId = $patientId
        serviceId = $serviceId
        startTime = '2020-01-01T10:00:00'
    } | ConvertTo-Json -Compress
    $status = Get-HttpStatus "$BaseUrl/api/appointments" 'POST' $admin.Session $admin.Headers $pastBody
    Write-Check 'Booking in the past is rejected' ($status -eq 400 -or $status -eq 422) `
        "HTTP $status (expected 400/422)"

    # A fresh session is required: PowerShell keeps request headers in WebRequestSession,
    # so reusing the admin session would silently resend X-XSRF-TOKEN.
    $cookieOnlySession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $cookieHeader = ($admin.Session.Cookies.GetCookies([uri]$BaseUrl) |
        ForEach-Object { "$($_.Name)=$($_.Value)" }) -join '; '
    $cookieOnlySession.Cookies.SetCookies([uri]$BaseUrl, $cookieHeader)
    $status = Get-HttpStatus "$BaseUrl/api/appointments" 'POST' $cookieOnlySession @{} $pastBody
    Write-Check 'POST without CSRF token is rejected' ($status -eq 403) "HTTP $status (expected 403)"

    $status = Get-HttpStatus "$BaseUrl/api/appointments/$($created.id)/complete" 'POST' `
        $doctor.Session $doctor.Headers
    Write-Check 'Completing a future appointment is rejected' ($status -eq 422) `
        "HTTP $status (expected 422)"

    $cancelBody = '{"reason":"verification smoke test"}'
    $status = Get-HttpStatus "$BaseUrl/api/appointments/$($created.id)/cancel" 'POST' `
        $admin.Session $admin.Headers $cancelBody
    Write-Check 'Cancel returns success' ($status -eq 200) "HTTP $status (expected 200)"

    $cancelledList = Invoke-JsonGet `
        "$BaseUrl/api/appointments?from=$slotDate&to=$slotDate&doctorId=$doctorId&status=CANCELLED" $admin.Session
    $cancelledItem = $cancelledList | Where-Object { $_.id -eq $created.id } | Select-Object -First 1
    Write-Check 'Cancelled booking has CANCELLED status' `
        ($cancelledItem -and $cancelledItem.status -eq 'CANCELLED') `
        "status=$($cancelledItem.status)"

    $availabilityAfterCancel = Invoke-JsonGet `
        "$BaseUrl/api/doctors/$doctorId/availability?date=$slotDate&serviceId=$serviceId" $admin.Session
    $freedSlot = $availabilityAfterCancel.slots |
        Where-Object { $_.startTime -eq $slot.startTime } |
        Select-Object -First 1
    Write-Check 'Cancellation frees the slot' ($freedSlot -and $freedSlot.available) `
        "available=$($freedSlot.available)"
}

# --- 6. Reports -------------------------------------------------------------
$report = Invoke-JsonGet "$BaseUrl/api/reports" $admin.Session
Write-Check 'Report summary contains period totals' `
    ($null -ne $report.summary -and $null -ne $report.summary.totalAppointments) `
    "total=$($report.summary.totalAppointments), scheduled=$($report.summary.scheduled), completed=$($report.summary.completed), cancelled=$($report.summary.cancelled)"
Write-Check 'Report groups data by doctor' (@($report.byDoctor).Count -gt 0) `
    "doctors=$(@($report.byDoctor).Count)"
Write-Check 'Report groups data by service' (@($report.byService).Count -gt 0) `
    "services=$(@($report.byService).Count)"
Write-Check 'Report contains daily breakdown' (@($report.daily).Count -gt 0) `
    "days=$(@($report.daily).Count)"
Write-Check 'Completed appointments produce revenue' `
    ([decimal]$report.summary.revenue -ge 0 -and $report.summary.completed -ge 1) `
    "completed=$($report.summary.completed), revenue=$($report.summary.revenue), avg=$($report.summary.averageCheck)"

# --- Summary ----------------------------------------------------------------
Write-Host ''
Write-Host ("Passed: {0}  Failed: {1}" -f $script:passed, $script:failed) `
    -ForegroundColor $(if ($script:failed -eq 0) { 'Green' } else { 'Red' })
if ($script:failed -gt 0) { exit 1 } else { exit 0 }
