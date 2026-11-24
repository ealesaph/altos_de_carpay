# Instala la app en los celulares conectados por USB y los enlaza al backend local con adb reverse.
# Uso (desde la carpeta del proyecto):  powershell -ep bypass -f .\app\AltosDeCarpay\conectar-celular.ps1
# Requisitos: celular con "Depuración USB" activada y la autorización del PC aceptada en el celular.
# El emulador puede seguir abierto: usa 10.0.2.2 y no necesita este paso.
$ErrorActionPreference = "Continue"
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "D:\Android\SDK" }
$adb = Join-Path $sdk "platform-tools\adb.exe"

$lineas = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\S" }
$celulares = @(); $pendientes = @()
foreach ($l in $lineas) {
    $id, $estado = ($l -split "\s+")[0..1]
    if ($id -like "emulator-*") { continue }
    if ($estado -eq "device") { $celulares += $id } else { $pendientes += "$id ($estado)" }
}
if ($pendientes) { "Acepta la autorización de depuración en el celular: $($pendientes -join ', ')" }
if (-not $celulares) { "No hay celulares listos por USB."; exit 1 }

foreach ($id in $celulares) {
    # 8000 = API (Django); 80 = página web (Nginx), usada por los botones «Reservar en la web».
    & $adb -s $id reverse tcp:8000 tcp:8000 | Out-Null
    & $adb -s $id reverse tcp:80 tcp:80 | Out-Null
    "Enlazado $id -> " + ((& $adb -s $id reverse --list) -join "; ")
}

# installDebug instala en todos los dispositivos conectados (celulares y emulador).
$env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "D:\archivos de programas\android studio\jbr" }
$proyecto = $PSScriptRoot
Set-Location $proyecto; [Environment]::CurrentDirectory = $proyecto
& (Join-Path $proyecto "gradlew.bat") ":app:installDebug" "--console=plain"
foreach ($id in $celulares) {
    & $adb -s $id shell monkey -p com.altosdecarpay.app -c android.intent.category.LAUNCHER 1 | Out-Null
}
"Listo. En el celular la app usa 127.0.0.1:8000 (se ve en «Servidor» al iniciar sesión)."
