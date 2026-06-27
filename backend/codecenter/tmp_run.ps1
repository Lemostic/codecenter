$ErrorActionPreference = "Continue"
Set-Location "D:\dev\Projects\codecenter"
for ($i=1; $i -le 3; $i++) {
    Write-Host "===== Run $i ====="
    & ".\mvnw.cmd" test -Dtest=CodePerfQaTest#fiftyThreadsNoDuplicates 2>&1 | Select-String -Pattern "Tests run|BUILD|FAIL" | Select-Object -First 5
    Write-Host ""
}
