# Servidor de demostración en la nube · Altos de Carpay

Guía para publicar la web, la API y la base de datos en una máquina virtual **gratuita** con
HTTPS válido, y dejar la app Android conectada a ese servidor.

```
Internet ──HTTPS──> Caddy (certificado Let's Encrypt automático)
                      ├── /api, /admin, /static, /media ──> Django + Gunicorn (backend)
                      └── todo lo demás ──────────────────> Web React en Nginx (frontend)
                                                             PostgreSQL 16 (db, solo red interna)
```

La dirección pública es un subdominio gratuito de **sslip.io** formado con la IP del servidor,
por ejemplo `https://129-151-10-20.sslip.io`. No hay que comprar dominio ni configurar DNS.

## 1. Crear la máquina virtual (Oracle Cloud Free Tier, región Santiago)

1. Crear la cuenta en <https://www.oracle.com/cloud/free/> eligiendo **Chile Central (Santiago)**
   como región principal. Pide una tarjeta solo para verificar identidad; los recursos
   «Always Free» no se cobran.
2. Menú ☰ → **Compute → Instances → Create instance**.
   - **Name:** `altos-de-carpay-demo`
   - **Image:** *Canonical Ubuntu 24.04* (o 22.04).
   - **Shape:** *Ampere* → `VM.Standard.A1.Flex` con **2 OCPU y 12 GB** (Always Free).
     Si aparece «Out of capacity», usar *AMD* → `VM.Standard.E2.1.Micro` (1 GB; el script
     agrega memoria de intercambio para que alcance).
   - **Networking:** dejar «Create new virtual cloud network» y **Assign a public IPv4 address**.
   - **SSH keys:** «Generate a key pair» y **descargar la clave privada** (o pegar una propia).
3. **Create**. Cuando quede «Running», anotar la **Public IP address**.
4. Abrir los puertos web en la red de Oracle: en la instancia → **Subnet** → **Default Security List**
   → **Add Ingress Rules**:
   | Source CIDR | IP Protocol | Destination Port Range |
   | :--- | :--- | :--- |
   | `0.0.0.0/0` | TCP | `80,443` |

> Alternativa: Google Cloud `e2-micro` (gratis en us-central1/us-east1/us-west1), Ubuntu 24.04,
> marcando «Allow HTTP traffic» y «Allow HTTPS traffic». El resto de la guía es igual.

## 2. Instalar el sistema (un solo comando)

Conectarse por SSH (desde PowerShell, con la clave descargada):

```powershell
ssh -i C:\ruta\clave-privada.key ubuntu@<IP_PUBLICA>
```
(o con el botón **Cloud Shell / SSH** de la consola web). Luego ejecutar:

```bash
curl -fsSL https://raw.githubusercontent.com/ealesaph/altos_de_carpay/qa/deploy/instalar_demo.sh | bash
```

El script (`deploy/instalar_demo.sh`) hace, en orden:

| Paso | Qué hace |
| :--- | :--- |
| 1 | Instala git, curl y python3 |
| 2 | Agrega 2 GB de swap si la máquina tiene poca memoria |
| 3 | Instala Docker |
| 4 | Abre los puertos 80 y 443 en el cortafuegos de Ubuntu (Oracle los trae cerrados) |
| 5 | Clona el repositorio (rama `qa`) en `~/altos_de_carpay` |
| 6 | Crea `backend/altosDeCarpay/.env` en modo producción (`DJANGO_DEBUG=false`) con claves aleatorias nuevas: base de datos, `DJANGO_SECRET_KEY`, `FIELD_ENCRYPTION_KEY` y las contraseñas de las cuentas de demostración |
| 7 | Construye y levanta los 4 contenedores y espera a que `https://<ip>.sslip.io` responda |

Al terminar muestra la dirección y las credenciales, que quedan guardadas en
`~/credenciales_demo.txt` (solo las puede leer ese usuario). Si el repositorio es privado,
`git clone` pedirá usuario y un *token* de GitHub (Settings → Developer settings → Personal access tokens).

Opciones (se anteponen al comando):
- `RAMA=main` – desplegar otra rama.
- `DOMINIO=demo.altosdecarpay.cl` – usar un dominio propio (su DNS debe apuntar a la IP).
- `SMTP_HOST=smtp-relay.brevo.com SMTP_USUARIO=... SMTP_CLAVE=...` – enviar correos reales.
  Sin esto, los correos se escriben en el registro del backend (`sudo docker logs carpay_backend`).

## 3. Operación del servidor

| Comando (en el servidor, dentro de `~/altos_de_carpay`) | Para qué sirve |
| :--- | :--- |
| `bash deploy/actualizar_demo.sh` | Trae los últimos cambios de la rama y reconstruye |
| `sudo docker compose -f docker-compose.yml -f deploy/docker-compose.demo.yml ps` | Estado de los contenedores |
| `sudo docker logs -f carpay_backend` | Registro de Django/Gunicorn (Ctrl+C para salir) |
| `sudo docker logs carpay_caddy --tail 30` | Registro del HTTPS / certificado |
| `sudo docker compose -f docker-compose.yml -f deploy/docker-compose.demo.yml down` | Detener la demo (los datos se conservan) |
| `cat ~/credenciales_demo.txt` | Ver la dirección y las credenciales |
| `sudo docker exec -e PRUEBAS_WEB=https://<ip>.sslip.io carpay_backend python pruebas/sistema/prueba_sistema.py` | Prueba de sistema contra la demo |

## 4. App Android apuntando a la demo

Opción A – sin recompilar: en la pantalla de inicio de sesión tocar **«Servidor: …»** y escribir
`https://<ip>.sslip.io`.

Opción B – APK de demo que ya trae el servidor configurado (en el PC, PowerShell):

```powershell
cd D:\altos_de_carpay\app\AltosDeCarpay
$env:JAVA_HOME = "D:\Archivos de programas\Android Studio\jbr"
$env:ORG_GRADLE_PROJECT_servidorDemo = "https://<ip>.sslip.io"
.\gradlew.bat :app:assembleDebug
# APK: app\build\outputs\apk\debug\app-debug.apk  (instalar con: adb install -r app-debug.apk)
```
Sin la variable `ORG_GRADLE_PROJECT_servidorDemo`, la app compila como siempre (emulador o USB).

## 5. Seguridad de la demo

- Es un entorno de **pruebas**: los pagos son simulados y los datos son de ejemplo.
- Solo Caddy (80/443) queda expuesto; PostgreSQL y Django no son accesibles desde Internet.
- Las contraseñas de las cuentas de demostración son aleatorias y distintas de las del README.
  Al terminar la presentación conviene cambiarlas desde «Mi cuenta» o detener la demo.
- No usar datos personales reales de clientes en este servidor.
