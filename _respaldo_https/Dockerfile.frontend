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

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
