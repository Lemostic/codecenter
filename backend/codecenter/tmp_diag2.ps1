$log = "D:\dev\Projects\codecenter\target\surefire-reports\TEST-com.meritdata.mdm.codecenter.qa.CodePerfQaTest.xml"
# Find all val=1018 entries
Write-Host "=== val=1018 entries ==="
Select-String -Path $log -Pattern "CosIdSeq.*val=1018" | ForEach-Object { $_.Line.Substring(0, [Math]::Min(180, $_.Line.Length)) }
Write-Host ""
# Find the first few CosIdSeq entries
Write-Host "=== First 5 CosIdSeq entries ==="
Select-String -Path $log -Pattern "CosIdSeq" | Select-Object -First 5 | ForEach-Object { $_.Line.Substring(0, [Math]::Min(180, $_.Line.Length)) }
