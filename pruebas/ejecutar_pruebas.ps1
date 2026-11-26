<#
.SYNOPSIS
  Ejecuta las pruebas del sistema Altos de Carpay sobre los contenedores de Docker.

.DESCRIPTION
  Uso (desde la carpeta raíz del proyecto, con Docker Desktop abierto y el sistema levantado):

    .\pruebas\ejecutar_pruebas.ps1 unitarias
    .\pruebas\ejecutar_pruebas.ps1 integracion
    .\pruebas\ejecutar_pruebas.ps1 sistema
    .\pruebas\ejecutar_pruebas.ps1 regresion
    .\pruebas\ejecutar_pruebas.ps1 rendimiento
    .\pruebas\ejecutar_pruebas.ps1 estres
    .\pruebas\ejecutar_pruebas.ps1 seguridad
    .\pruebas\ejecutar_pruebas.ps1 todas

  Si PowerShell bloquea el script:
    powershell -ExecutionPolicy Bypass -File .\pruebas\ejecutar_pruebas.ps1 unitarias

  Cada ejecución guarda un registro en pruebas\resultados\<tipo>_<fecha>.txt
#>
param(
    [ValidateSet("unitarias", "integracion", "sistema", "regresion", "rendimiento", "estres", "seguridad", "todas")]
    [string]$Tipo = "todas"
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$Raiz = Split-Path -Parent $PSScriptRoot
Set-Location $Raiz
$Resultados = Join-Path $PSScriptRoot "resultados"
New-Item -ItemType Directory -Force -Path $Resultados | Out-Null
$Registro = Join-Path $Resultados ("{0}_{1}.txt" -f $Tipo, (Get-Date -Format "yyyy-MM-dd_HH-mm-ss"))
$Resumen = [System.Collections.ArrayList]::new()

function Escribir([string]$Texto, [string]$Color = "Gray") {
    Write-Host $Texto -ForegroundColor $Color
    Add-Content -Path $Registro -Value $Texto -Encoding UTF8
}

function Paso([string]$Nombre, [string[]]$Comando) {
    Escribir ""
    Escribir ("=" * 90) "Cyan"
    Escribir "  $Nombre" "Cyan"
    Escribir ("  > " + ($Comando -join " ")) "DarkGray"
    Escribir ("=" * 90) "Cyan"
    $inicio = Get-Date
    & $Comando[0] $Comando[1..($Comando.Length - 1)] 2>&1 | ForEach-Object {
        $linea = "$_"
        if ($linea -eq "System.Management.Automation.RemoteException") { $linea = "" }
        Write-Host $linea
        Add-Content -Path $Registro -Value $linea -Encoding UTF8
    }
    $codigo = $LASTEXITCODE
    $segundos = [math]::Round(((Get-Date) - $inicio).TotalSeconds, 1)
    if ($codigo -eq 0) {
        Escribir "  -> APROBADO ($segundos s)" "Green"
        [void]$Resumen.Add(("APROBADO   {0} ({1} s)" -f $Nombre, $segundos))
    } else {
        Escribir "  -> CON FALLAS (código $codigo, $segundos s)" "Red"
        [void]$Resumen.Add(("CON FALLAS {0} ({1} s)" -f $Nombre, $segundos))
    }
}

function Esperar([int]$Segundos, [string]$Motivo) {
    Escribir "  ... esperando $Segundos s: $Motivo" "DarkYellow"
    Start-Sleep -Seconds $Segundos
}

# --- Comprobación previa: el sistema debe estar levantado ---
$enEjecucion = docker ps --format "{{.Names}}" 2>$null
foreach ($c in "carpay_postgres", "carpay_backend", "carpay_frontend") {
    if ($enEjecucion -notcontains $c) {
        Write-Host "El contenedor $c no está en ejecución. Levante el sistema con:  docker compose up -d --build" -ForegroundColor Red
        exit 1
    }
}
docker exec carpay_backend test -d /app/pruebas/unitarias 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "El contenedor no ve la carpeta pruebas/. Recree el backend:  docker compose up -d backend" -ForegroundColor Red
    exit 1
}

Escribir "Pruebas Altos de Carpay · tipo: $Tipo · $(Get-Date -Format 'dd-MM-yyyy HH:mm:ss')" "White"
$Django = @("docker", "exec", "carpay_backend", "python", "manage.py", "test", "--noinput", "-v", "2")
$Node = @("docker", "run", "--rm", "-v", "${Raiz}:/w", "-w", "/w", "node:22-alpine", "node", "--test", "pruebas/unitarias/frontend/*.test.mjs")

switch ($Tipo) {
    "unitarias" {
        Paso "Unitarias · backend (Django, sin base de datos)" ($Django + "pruebas.unitarias")
        Paso "Unitarias · frontend (reglas.js con node --test)" $Node
    }
    "integracion" {
        Paso "Integración · API + base de datos de prueba" ($Django + "pruebas.integracion")
    }
    "sistema" {
        Paso "Sistema · flujo completo por la web (Nginx -> Django -> PostgreSQL)" @("docker", "exec", "carpay_backend", "python", "pruebas/sistema/prueba_sistema.py")
        Paso "Sistema · humo de la API de la app móvil" @("docker", "exec", "carpay_backend", "python", "pruebas/sistema/humo_api_app.py")
    }
    "regresion" {
        Paso "Regresión · suite automática completa (unitarias + integración + seguridad + observaciones)" ($Django + "pruebas")
        Paso "Regresión · frontend" $Node
        Paso "Regresión · prueba de sistema" @("docker", "exec", "carpay_backend", "python", "pruebas/sistema/prueba_sistema.py")
    }
    "rendimiento" {
        Paso "Rendimiento · tiempos de respuesta (< 2 s)" @("docker", "exec", "carpay_backend", "python", "pruebas/rendimiento/tiempos_respuesta.py")
    }
    "estres" {
        Paso "Rendimiento · estrés de 10 a 200 usuarios simultáneos" @("docker", "exec", "carpay_backend", "sh", "pruebas/rendimiento/estres.sh")
    }
    "seguridad" {
        Paso "Seguridad · API (JWT, roles, IDOR, SQLi, XSS, datos sensibles)" ($Django + "pruebas.seguridad")
        Paso "Seguridad · despliegue en vivo (HTTPS, cabeceras, fuerza bruta, avalancha)" @("docker", "exec", "carpay_backend", "python", "pruebas/seguridad/seguridad_en_vivo.py")
    }
    "todas" {
        Paso "Unitarias · backend" ($Django + "pruebas.unitarias")
        Paso "Unitarias · frontend" $Node
        Paso "Integración" ($Django + "pruebas.integracion")
        Paso "Regresión · observaciones del cliente" ($Django + "pruebas.regresion")
        Paso "Seguridad · API" ($Django + "pruebas.seguridad")
        Esperar 61 "se renuevan los límites de solicitudes antes de usar el sistema en vivo"
        Paso "Sistema · flujo completo por la web" @("docker", "exec", "carpay_backend", "python", "pruebas/sistema/prueba_sistema.py")
        Paso "Sistema · humo de la API de la app móvil" @("docker", "exec", "carpay_backend", "python", "pruebas/sistema/humo_api_app.py")
        Paso "Rendimiento · tiempos de respuesta" @("docker", "exec", "carpay_backend", "python", "pruebas/rendimiento/tiempos_respuesta.py")
        Esperar 61 "se renuevan los límites antes de la prueba de seguridad en vivo"
        Paso "Seguridad · despliegue en vivo" @("docker", "exec", "carpay_backend", "python", "pruebas/seguridad/seguridad_en_vivo.py")
    }
}

Escribir ""
Escribir ("=" * 90) "White"
Escribir "  RESUMEN" "White"
foreach ($r in $Resumen) {
    if ($r.StartsWith("APROBADO")) { Escribir "  $r" "Green" } else { Escribir "  $r" "Red" }
}
Escribir "  Registro guardado en: $Registro" "White"
Escribir ("=" * 90) "White"
if ($Resumen | Where-Object { -not $_.StartsWith("APROBADO") }) { exit 1 } else { exit 0 }
