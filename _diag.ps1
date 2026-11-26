$out = "D:\altos_de_carpay\_diag.txt"
$adb = "D:\Android\SDK\platform-tools\adb.exe"
"== lote ==" | Out-File $out -Encoding utf8
& $adb -s emulator-5554 shell "input swipe 540 1900 540 300 300" 2>&1 | Out-File $out -Append -Encoding utf8 -Width 400
Start-Sleep -Milliseconds 1000
& $adb -s emulator-5554 shell "input swipe 540 1900 540 300 300" 2>&1 | Out-File $out -Append -Encoding utf8 -Width 400
Start-Sleep -Milliseconds 1000
& $adb -s emulator-5554 shell "uiautomator dump /sdcard/ui.xml >/dev/null 2>&1"
cmd /c "`"$adb`" -s emulator-5554 exec-out cat /sdcard/ui.xml > D:\altos_de_carpay\_qa\z20_explorar.xml"
"FIN" | Out-File $out -Append -Encoding utf8
