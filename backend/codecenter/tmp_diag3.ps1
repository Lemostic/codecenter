$log = "D:\dev\Projects\codecenter\target\surefire-reports\TEST-com.meritdata.mdm.codecenter.qa.CodePerfQaTest.xml"
$content = Get-Content $log
# Find all "Code generated" lines (success) and DedupHIT lines (failure)
$seqs = @{}
$successCodes = @{}
Select-String -Path $log -Pattern "Code generated.*code=QA-(\d+).*seq=(\d+)" | ForEach-Object {
    if ($_ -match "code=QA-(\d+).*seq=(\d+)") {
        $code = "QA-" + $Matches[1]
        $seq = [int]$Matches[2]
        $successCodes[$code] = $seq
    }
}
Write-Host "Total successful generations: $($successCodes.Count)"
# Find all dedup hits
$dups = @()
Select-String -Path $log -Pattern "DedupHIT count=\d+ code=(QA-\d+)" | ForEach-Object {
    if ($_ -match "code=(QA-\d+)") {
        $code = $Matches[1]
        $dups += $code
    }
}
Write-Host "Total dedup hits: $($dups.Count)"
Write-Host ""
Write-Host "First 10 dedup hits and their expected seqs:"
$dups | Select-Object -First 10 | ForEach-Object {
    $seqFromCode = [int]$_.Substring(3)
    $inSuccess = $successCodes.ContainsKey($_)
    Write-Host "  $_ (seq=$seqFromCode) in success log: $inSuccess"
}
