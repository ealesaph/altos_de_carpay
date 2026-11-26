#!/usr/bin/env bash
# =====================================================================================
#  Instalación del servidor de DEMOSTRACIÓN de Altos de Carpay en una máquina virtual
#  Ubuntu 22.04 / 24.04 (Oracle Cloud Free Tier, Google Cloud e2-micro u otra).
#
#  Uso (en la consola SSH de la máquina virtual):
#     curl -fsSL https://raw.githubusercontent.com/ealesaph/altos_de_carpay/qa/deploy/instalar_demo.sh | bash
#  o, si el repositorio ya está clonado:
#     bash deploy/instalar_demo.sh
#
#  Variables opcionales (antes de «bash»: ... | RAMA=main bash):  RAMA=qa  DOMINIO=midominio.cl  SMTP_HOST= SMTP_USUARIO= SMTP_CLAVE=
#  El script se puede volver a ejecutar: no borra datos ni cambia las claves ya generadas.
# =====================================================================================
set -euo pipefail

REPO="${REPO:-https://github.com/ealesaph/altos_de_carpay.git}"
RAMA="${RAMA:-qa}"
DIR="${DIR:-$HOME/altos_de_carpay}"
COMPOSE="compose -f docker-compose.yml -f deploy/docker-compose.demo.yml"

paso() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
aleatorio() { python3 -c "import secrets,string; print(''.join(secrets.choice(string.ascii_letters + string.digits) for _ in range(${1:-32})))"; }

paso "1/7 Paquetes base"
sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq git curl python3 ca-certificates >/dev/null

paso "2/7 Memoria de intercambio (swap) para compilar en máquinas de 1 GB"
if [ "$(swapon --show | wc -l)" -eq 0 ] && [ "$(free -m | awk '/Mem:/{print $2}')" -lt 4000 ]; then
  sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile >/dev/null && sudo swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
  echo "   swap de 2 GB activada"
else
  echo "   no es necesaria"
fi

paso "3/7 Docker"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sudo sh >/dev/null
fi
sudo usermod -aG docker "$USER" || true
DOCKER="sudo docker"

paso "4/7 Cortafuegos del sistema (puertos 80 y 443)"
# Las imágenes Ubuntu de Oracle Cloud traen reglas iptables que bloquean todo salvo SSH.
for puerto in 80 443; do
  if ! sudo iptables -C INPUT -p tcp --dport "$puerto" -j ACCEPT 2>/dev/null; then
    sudo iptables -I INPUT 1 -p tcp --dport "$puerto" -j ACCEPT
  fi
done
sudo iptables -C INPUT -p udp --dport 443 -j ACCEPT 2>/dev/null || sudo iptables -I INPUT 1 -p udp --dport 443 -j ACCEPT
if command -v netfilter-persistent >/dev/null; then sudo netfilter-persistent save >/dev/null 2>&1 || true; fi
command -v ufw >/dev/null && sudo ufw status | grep -q active && { sudo ufw allow 80/tcp; sudo ufw allow 443; } || true

paso "5/7 Código del proyecto (rama $RAMA)"
if [ -d "$DIR/.git" ]; then
  git -C "$DIR" fetch -q origin "$RAMA" && git -C "$DIR" checkout -q "$RAMA" && git -C "$DIR" pull -q --ff-only origin "$RAMA"
else
  git clone -q -b "$RAMA" "$REPO" "$DIR"
fi
cd "$DIR"

paso "6/7 Configuración (.env con claves generadas en este servidor)"
IP_PUBLICA="$(curl -fsS https://api.ipify.org || curl -fsS https://ifconfig.me)"
DOMINIO="${DOMINIO:-$(echo "$IP_PUBLICA" | tr '.' '-').sslip.io}"
echo "DOMINIO=$DOMINIO" > .env          # lo usa docker-compose.demo.yml (Caddy)
ENV_BACK=backend/altosDeCarpay/.env
CREDENCIALES="$HOME/credenciales_demo.txt"
if [ ! -f "$ENV_BACK" ]; then
  CLAVE_BD="$(aleatorio 32)"
  CLAVE_ADMIN="Demo-$(aleatorio 10)!"
  CLAVE_CLIENTE="Demo-$(aleatorio 10)!"
  cat > "$ENV_BACK" <<ENV
# Generado por deploy/instalar_demo.sh el $(date '+%d-%m-%Y %H:%M'). No subir a Git.
POSTGRES_DB=altos_de_carpay_db
POSTGRES_USER=carpay_admin
POSTGRES_PASSWORD=$CLAVE_BD
DB_NAME=altos_de_carpay_db
DB_USER=carpay_admin
DB_PASSWORD=$CLAVE_BD

DJANGO_DEBUG=false
DJANGO_SECRET_KEY=$(aleatorio 60)
DJANGO_ALLOWED_HOSTS=$DOMINIO,backend,localhost,127.0.0.1
CORS_ALLOWED_ORIGINS=https://$DOMINIO
CSRF_TRUSTED_ORIGINS=https://$DOMINIO
ADMIN_URL=admin/
DJANGO_DETRAS_DE_PROXY=true
NUM_PROXIES=1
GUNICORN_WORKERS=2
CARGAR_DATOS_SEMILLA=true
SEMILLA_CLAVE_ADMIN=$CLAVE_ADMIN
SEMILLA_CLAVE_CLIENTE=$CLAVE_CLIENTE

FIELD_ENCRYPTION_KEY=$(python3 -c 'import base64, os; print(base64.urlsafe_b64encode(os.urandom(32)).decode())')
FIELD_ENCRYPTION_KEYS_ANTERIORES=

EMAIL_HOST=${SMTP_HOST:-}
EMAIL_PORT=${SMTP_PUERTO:-587}
EMAIL_HOST_USER=${SMTP_USUARIO:-}
EMAIL_HOST_PASSWORD=${SMTP_CLAVE:-}
EMAIL_USE_TLS=true
EMAIL_USE_SSL=false
DEFAULT_FROM_EMAIL=${SMTP_REMITENTE:-no-responder@altosdecarpay.cl}

ANTICIPO_PORCENTAJE=30
PLAZO_PAGO_ANTICIPO_HORAS=24
PAGOS_SIMULADOR_PERMITIR_RECHAZO=true
ENV
  chmod 600 "$ENV_BACK"
  {
    echo "Altos de Carpay · servidor de demostración"
    echo "Web:            https://$DOMINIO"
    echo "Panel admin:    https://$DOMINIO/panel"
    echo "App Android:    servidor https://$DOMINIO"
    echo "Administrador:  admin@altosdecarpay.cl / $CLAVE_ADMIN"
    echo "Cliente demo:   camila.rojas@gmail.com / $CLAVE_CLIENTE"
  } > "$CREDENCIALES"
  chmod 600 "$CREDENCIALES"
else
  echo "   $ENV_BACK ya existe: se conserva (mismas claves y datos)"
  sed -i "s#^DJANGO_ALLOWED_HOSTS=.*#DJANGO_ALLOWED_HOSTS=$DOMINIO,backend,localhost,127.0.0.1#; s#^CORS_ALLOWED_ORIGINS=.*#CORS_ALLOWED_ORIGINS=https://$DOMINIO#; s#^CSRF_TRUSTED_ORIGINS=.*#CSRF_TRUSTED_ORIGINS=https://$DOMINIO#" "$ENV_BACK"
fi

paso "7/7 Construcción y arranque de los contenedores (la primera vez tarda 5–15 minutos)"
$DOCKER $COMPOSE up -d --build

echo "   Esperando que el sitio responda con HTTPS..."
for i in $(seq 1 60); do
  if curl -fsS -o /dev/null "https://$DOMINIO/api/v1/sectores/"; then OK=1; break; fi
  sleep 5
done
$DOCKER $COMPOSE ps
if [ "${OK:-0}" = 1 ]; then
  printf '\n\033[1;32m  LISTO. Demo disponible en https://%s\033[0m\n\n' "$DOMINIO"
else
  printf '\n\033[1;33m  Los contenedores están arriba, pero https://%s aún no responde.\033[0m\n' "$DOMINIO"
  echo "  Revise que la nube permita la entrada a los puertos 80 y 443 (ver deploy/README.md) y:"
  echo "    sudo docker logs carpay_caddy --tail 30"
fi
[ -f "$CREDENCIALES" ] && cat "$CREDENCIALES"
echo
echo "  (Las credenciales quedaron guardadas en $CREDENCIALES)"
