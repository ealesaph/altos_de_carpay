$out = "D:\altos_de_carpay\_diag.txt"
"== avd ==" | Out-File $out -Encoding utf8
& "D:\Android\SDK\emulator\emulator.exe" -list-avds 2>&1 | Out-File $out -Append -Encoding utf8
$avd = (& "D:\Android\SDK\emulator\emulator.exe" -list-avds | Select-Object -First 1)
Start-Process "D:\Android\SDK\emulator\emulator.exe" -ArgumentList "-avd",$avd,"-no-snapshot-load" -WindowStyle Minimized
& "D:\Android\SDK\platform-tools\adb.exe" wait-for-device
do { Start-Sleep 3; $b = (& "D:\Android\SDK\platform-tools\adb.exe" -e shell getprop sys.boot_completed).Trim() } while ($b -ne "1")
"arrancado $avd" | Out-File $out -Append -Encoding utf8
"FIN" | Out-File $out -Append -Encoding utf8
