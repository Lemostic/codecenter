$file = "D:\dev\Projects\codecenter\src\main\java/com/meritdata/mdm/codecenter/infrastructure/cosid/CosIdGenerator.java"
$content = Get-Content $file -Raw
$old = "        return al.incrementAndGet();"
$new = "            long val = al.incrementAndGet();
            if (log.isDebugEnabled() && bizTag.Contains(""CONC"")) {
                log.debug(""CosIdSeq: bizTag={}, val={}, atomicId={}"", bizTag, val, System.identityHashCode(al));
            }
            return val;"
$content = $content.Replace($old, $new)
Set-Content $file $content -Encoding UTF8 -NoNewline
Write-Host "Patched"
