# Etapa 1: Compilación de la aplicación React con Vite
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# Etapa 2: Servidor Web Nginx ligero para producción
FROM nginx:alpine

# Copiar el build compilado al directorio de Nginx
COPY --from=build /app/dist /usr/share/nginx/html

# X-Forwarded-For se reemplaza (no se concatena) con la IP real: un cliente no puede falsear su IP para
# saltarse el límite de intentos de inicio de sesión. Django confía en 1 proxy (NUM_PROXIES=1).
# Configuración personalizada de Nginx para SPA (Single Page Application) y Proxy Inverso hacia Django
RUN echo 'server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html; \
        try_files $uri $uri/ /index.html; \
    } \
    location /api/ { \
        proxy_pass http://backend:8000/api/; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $remote_addr; \
    } \
    location /admin/ { \
        proxy_pass http://backend:8000/admin/; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $remote_addr; \
    } \
    location /static/ { \
        proxy_pass http://backend:8000/static/; \
        proxy_set_header Host $host; \
    } \
    location /media/ { \
        proxy_pass http://backend:8000/media/; \
        proxy_set_header Host $host; \
    } \
}' > /etc/nginx/conf.d/default.conf

# HTTPS (cifrado en tránsito): certificado autofirmado para el entorno local. En producción se reemplaza
# por uno emitido por una autoridad (por ejemplo, Let's Encrypt) montando los archivos en /etc/nginx/certs.
# El puerto 80 se mantiene para la app Android en desarrollo (10.0.2.2) y no se redirige. HSTS no se envía
# en local para no fijar HTTPS en el navegador para localhost; en producción lo activa Django (SECURE_HSTS_SECONDS).
RUN apk add --no-cache openssl \
 && mkdir -p /etc/nginx/certs \
 && openssl req -x509 -nodes -days 825 -newkey rsa:2048 \
    -keyout /etc/nginx/certs/carpay.key -out /etc/nginx/certs/carpay.crt \
    -subj "/C=CL/O=Altos de Carpay/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

RUN echo 'server { \
    listen 443 ssl; \
    http2 on; \
    server_name localhost; \
    ssl_certificate /etc/nginx/certs/carpay.crt; \
    ssl_certificate_key /etc/nginx/certs/carpay.key; \
    ssl_protocols TLSv1.2 TLSv1.3; \
    ssl_prefer_server_ciphers on; \
    add_header X-Content-Type-Options "nosniff" always; \
    add_header X-Frame-Options "DENY" always; \
    add_header Referrer-Policy "same-origin" always; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html; \
        try_files $uri $uri/ /index.html; \
    } \
    location /api/ { \
        proxy_pass http://backend:8000/api/; \
        proxy_set_header Host $host; \
        proxy_set_header X-Forwarded-Proto https; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $remote_addr; \
    } \
    location /admin/ { \
        proxy_pass http://backend:8000/admin/; \
        proxy_set_header Host $host; \
        proxy_set_header X-Forwarded-Proto https; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $remote_addr; \
    } \
    location /static/ { \
        proxy_pass http://backend:8000/static/; \
        proxy_set_header Host $host; \
        proxy_set_header X-Forwarded-Proto https; \
    } \
    location /media/ { \
        proxy_pass http://backend:8000/media/; \
        proxy_set_header Host $host; \
        proxy_set_header X-Forwarded-Proto https; \
    } \
}' > /etc/nginx/conf.d/https.conf

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
