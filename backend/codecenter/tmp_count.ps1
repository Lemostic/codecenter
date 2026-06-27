$log = "D:\dev\Projects\codecenter\target\surefire-reports\TEST-com.meritdata.mdm.codecenter.qa.CodePerfQaTest.xml"
$vals = @{}
Select-String -Path $log -Pattern "CosIdSeq.*val=(\d+)" | ForEach-Object {
    if ($_ -match "val=(\d+)") {
        $v = [int]$Matches[1]
        if (-not $vals.ContainsKey($v)) { $vals[$v] = 0 }
        $vals[$v]++
    }
}
Write-Host "Total calls: $(($vals.Values | Measure-Object -Sum).Sum)"
Write-Host "Unique values: $($vals.Count)"
Write-Host "Max value: $(($vals.Keys | Measure-Object -Maximum).Maximum)"
$dupes = $vals.GetEnumerator() | Where-Object { $_.Value -gt 1 } | Sort-Object Key
Write-Host "Duplicates: $($dupes.Count)"
$dupes | Select-Object -First 10 | Format-Table -AutoSize
