# Manual de pruebas · Altos de Carpay

Esta carpeta reúne todas las pruebas del sistema (web React + API Django + PostgreSQL), ordenadas
según el Plan de Pruebas (Etapa 3). Las pruebas se ejecutan **dentro de los contenedores de Docker**,
así que no hace falta instalar Python ni Node en el computador.

## 1. Estructura

```
pruebas/
├── README.md                 ← este manual
├── ejecutar_pruebas.ps1      ← ejecuta cualquier tipo de prueba con un solo comando
├── _comun.py                 ← utilidades compartidas (cliente HTTP, informe de resultados)
├── unitarias/                ← funciones aisladas, sin base de datos ni red
│   ├── test_validadores_y_texto.py   RUT y limpieza de texto
│   ├── test_cifrado.py               cifrado del RUT/teléfono y rotación de clave
│   ├── test_reglas_de_negocio.py     anticipo 30 %, saldo, plazos, bloques horarios, devoluciones
│   └── frontend/reglas.test.mjs      reglas de la web (frontend/src/reglas.js)
├── integracion/              ← API + base de datos de prueba (Django crea y borra una BD temporal)
│   ├── test_usuarios_ley21719.py     registro, consentimiento, ARCO, brechas
│   ├── test_cifrado_en_base_de_datos.py
│   ├── test_correo_reserva.py
│   ├── test_reportes_y_espacios.py   Excel/CSV y gestión de espacios
│   └── test_trazabilidad_historias.py  HU-01 a HU-09
├── sistema/                  ← sistema completo levantado (Nginx → Django → PostgreSQL)
│   ├── prueba_sistema.py             flujo completo cliente + administrador (24 casos)
│   └── humo_api_app.py               endpoints que usa la app Android (21 verificaciones)
├── regresion/                ← correcciones pedidas por el cliente (bitácora 23-11-2026)
│   └── test_observaciones_cliente.py
├── rendimiento/
│   ├── tiempos_respuesta.py          tiempos de respuesta (< 2 s)
│   ├── estres.sh / estres.py         carga de 10 a 200 usuarios simultáneos
├── seguridad/
│   ├── test_seguridad_api.py         JWT, roles, IDOR, SQLi, XSS, datos sensibles (17 pruebas)
│   ├── seguridad_en_vivo.py          HTTPS, cabeceras, archivos expuestos, fuerza bruta, avalancha
│   ├── limite_avalancha.py / limite_secuencial.py  (diagnóstico del límite de solicitudes)
├── aceptacion/               ← pendiente (se hará más adelante con el cliente)
├── evidencias_etapa3/        ← scripts usados para las capturas del informe de la Etapa 3
└── resultados/               ← registros de cada ejecución (no se suben a Git)
```

`docker-compose.yml` monta esta carpeta en el backend como `/app/pruebas`.

## 2. Antes de empezar

Abrir **Docker Desktop** y, en una terminal de PowerShell en la carpeta del proyecto (`D:\altos_de_carpay`):

| Comando | Para qué sirve |
| :--- | :--- |
| `docker compose up -d --build` | Construye y levanta los 3 contenedores (base de datos, backend y web). |
| `docker ps` | Verifica que `carpay_postgres`, `carpay_backend` y `carpay_frontend` estén «Up». |
| `docker compose up -d backend` | Recrea solo el backend (necesario la primera vez para que vea la carpeta `pruebas/`). |
| `docker logs -f carpay_backend` | Muestra lo que está haciendo el backend (Ctrl+C para salir). |

La web queda en `http://localhost/` (y `https://localhost/`), el panel en `http://localhost/panel`.

## 3. Ejecución rápida (recomendada)

```powershell
.\pruebas\ejecutar_pruebas.ps1 <tipo>
```

Si PowerShell no permite ejecutar scripts:
`powershell -ExecutionPolicy Bypass -File .\pruebas\ejecutar_pruebas.ps1 <tipo>`

| `<tipo>` | Qué ejecuta | Tiempo aprox. |
| :--- | :--- | :--- |
| `unitarias` | Pruebas unitarias del backend (37) y de la web (7) | 5 s |
| `integracion` | API + base de datos de prueba (28) | 20 s |
| `sistema` | Flujo completo por la web (24 casos) + humo de la app móvil (21) | 15 s |
| `regresion` | Toda la suite automática (93) + web + prueba de sistema | 1 min |
| `rendimiento` | Tiempos de respuesta de la web y la API (< 2 s) | 10 s |
| `estres` | Carga de 10 a 200 usuarios simultáneos (3.850 solicitudes) | 40 s |
| `seguridad` | Seguridad de la API (17) + seguridad del despliegue en vivo (10) | 30 s |
| `todas` | Todo lo anterior salvo el estrés, en el orden correcto y con las pausas necesarias | 4 min |

Al final se muestra un **RESUMEN** (APROBADO / CON FALLAS) y la ruta del registro guardado en
`pruebas\resultados\<tipo>_<fecha>.txt`, que sirve como evidencia para el informe.

## 4. Comandos individuales (para ejecutar en vivo, paso a paso)

### 4.1 Pruebas unitarias
Verifican piezas pequeñas del código por separado, sin base de datos ni red.

```powershell
docker exec carpay_backend python manage.py test pruebas.unitarias -v 2
docker run --rm -v "${PWD}:/w" -w /w node:22-alpine node --test "pruebas/unitarias/frontend/*.test.mjs"
```
- Validación del RUT (dígito verificador, formatos, K), limpieza de HTML en textos.
- Cifrado Fernet del RUT/teléfono, huella HMAC y rotación de claves.
- Cálculo del anticipo (30 %, redondeo), saldo, plazos de 24 h, bloques 13–18 / 19–00 y devoluciones.
- Reglas de la web: horas de inicio permitidas, código `RES-0000`, formato CLP.

Para una sola clase o prueba: `docker exec carpay_backend python manage.py test pruebas.unitarias.test_cifrado.RotacionDeClaveTests`

### 4.2 Pruebas de integración
Verifican que la API, los serializadores, los modelos y PostgreSQL funcionan juntos. Django crea
una base temporal (`test_altos_de_carpay_db`), la usa y la borra: **no toca los datos reales**.

```powershell
docker exec carpay_backend python manage.py test pruebas.integracion -v 2
```
Cubre: registro con consentimiento (Ley 21.719), derechos ARCO y PDF, RUT cifrado en la base,
correo de la reserva, reportes Excel/CSV, gestión de espacios y la trazabilidad HU-01 a HU-09.

### 4.3 Pruebas de sistema
Prueban el sistema completo como lo usa una persona, entrando por Nginx (la web) igual que el navegador.

```powershell
docker exec carpay_backend python pruebas/sistema/prueba_sistema.py
docker exec carpay_backend python pruebas/sistema/humo_api_app.py
```
`prueba_sistema.py` recorre: portada y archivos de la web, recarga de las páginas (`/login`, `/panel`,
`/condiciones`…), catálogo público, registro de un cliente nuevo, inicio de sesión, reserva con
servicio extra, rechazo de doble reserva y de horario fuera de bloque, pago del anticipo,
consulta del cliente y respuesta del administrador, check-in por QR, incidencia en la bitácora,
check-out con sobretiempo, bitácora en solo lectura, exportación a Excel y cierre de sesión.

> Para el check-in, el script «adelanta el reloj» de la reserva de prueba (lo indica en pantalla),
> porque el sistema solo permite registrar la entrada desde 1 hora antes del evento.
> Deja en la base un cliente `cliente.sistema.<fecha>@example.com` y una reserva finalizada como evidencia.
> Para recibir los correos reales en su bandeja:
> `docker exec -e PRUEBAS_CORREO=su@correo.cl carpay_backend python pruebas/sistema/prueba_sistema.py`

### 4.4 Pruebas de regresión
Se ejecutan **después de cada cambio** para comprobar que lo que ya funcionaba sigue funcionando.
Incluyen las pruebas de las observaciones del cliente (bloques horarios, editar selección, plazos,
consultas sin cuenta, saldo por cobrar, cierre automático, devoluciones, check-in el día del evento
y bloqueo de cuenta tras 5 intentos).

```powershell
docker exec carpay_backend python manage.py test pruebas.regresion -v 2   # solo observaciones del cliente
docker exec carpay_backend python manage.py test pruebas                  # suite completa (93 pruebas)
.\pruebas\ejecutar_pruebas.ps1 regresion                                  # suite + web + sistema
```

### 4.5 Pruebas de rendimiento
```powershell
docker exec carpay_backend python pruebas/rendimiento/tiempos_respuesta.py
docker exec carpay_backend sh pruebas/rendimiento/estres.sh
```
- `tiempos_respuesta.py`: 20 solicitudes a cada recurso (portada, catálogo, reservas, resumen…) y
  muestra mínimo, promedio, percentil 95 y máximo. Aprueba si el p95 es menor a 2 s (RNF-03).
  Cambiar repeticiones: `docker exec -e PRUEBAS_REPETICIONES=50 carpay_backend python pruebas/rendimiento/tiempos_respuesta.py`
- `estres.sh`: levanta un Gunicorn temporal sin límites en el puerto 8001 y aumenta la carga de 10 a
  200 usuarios simultáneos. Aprueba con 0 % de errores y p95 < 2 s en todas las etapas.
  Guarda los datos en `pruebas/resultados/estres.json`. Etapas propias: `-e ESTRES_ETAPAS=10,50,300`.

### 4.6 Pruebas de seguridad
```powershell
docker exec carpay_backend python manage.py test pruebas.seguridad -v 2
docker exec carpay_backend python pruebas/seguridad/seguridad_en_vivo.py
```
- `test_seguridad_api.py`: rutas sin token (401), token alterado, cliente en rutas de administrador
  (403), reservas ajenas (404), inyección SQL en login y filtros, XSS en consultas, intento de
  registrarse como administrador, precio manipulado desde el cliente, clave débil, clave con hash,
  cabeceras de seguridad y cierre de sesión.
- `seguridad_en_vivo.py`: TLS 1.2/1.3 en HTTPS, cabeceras en Nginx, que no se publiquen `.env` ni
  `.git`, errores sin detalles internos, control de acceso a través del proxy, fuerza bruta con
  `X-Forwarded-For` falseado y avalancha de 150 solicitudes (debe responder 429 sin caerse).

> Después de `seguridad_en_vivo.py` el límite de solicitudes de esa IP queda activo ~1 minuto
> (HTTP 429). Es el comportamiento esperado; espere 1 minuto antes de ejecutar otra prueba en vivo.

### 4.7 Pruebas de aceptación
Pendientes; ver `aceptacion/README.md`.

## 5. Cómo leer los resultados

- Django: `Ran N tests ... OK` = todo aprobado; `FAILED (failures=X)` muestra cada falla con su detalle.
- Scripts en vivo: cada caso aparece como `[APROBADO]`, `[FALLIDO]` o `[ADVERTENCIA]` y al final
  `RESULTADO: X de Y casos aprobados`. Una **advertencia** no es una falla: indica algo aceptable
  en desarrollo que debe cambiarse en producción.
- Los mensajes `[WARNING] seguridad: Intento de acceso fallido` son esperados: los generan las
  pruebas de fuerza bruta.

## 6. Recomendaciones y problemas comunes

| Situación | Solución |
| :--- | :--- |
| `El contenedor carpay_backend no está en ejecución` | `docker compose up -d --build` |
| `El contenedor no ve la carpeta pruebas/` | `docker compose up -d backend` |
| La web no carga y `carpay_frontend` aparece «Restarting» | El backend estaba detenido: `docker compose up -d` y esperar 20 s |
| Pruebas en vivo con muchos `429` | Se alcanzó el límite de solicitudes: esperar 1 minuto |
| `SIS-06` con advertencia «Límite de 5 registros por hora» | Se ejecutó la prueba de sistema muchas veces; usa la cuenta de demostración |
| `database "test_altos_de_carpay_db" already exists` | Las pruebas usan `--noinput`, que la borra sola; si persiste, volver a ejecutar |

## 7. Resultados de referencia (26-11-2026)

| Tipo | Resultado |
| :--- | :--- |
| Unitarias | 37/37 backend + 7/7 web |
| Integración | 28/28 |
| Regresión (observaciones del cliente) | 11/11 · suite completa 93/93 |
| Sistema | 24/24 flujo web + 21/21 humo app |
| Rendimiento | p95 máximo 63 ms (umbral 2.000 ms) · estrés 3.850 solicitudes, 0 % errores, p95 1.397 ms con 200 usuarios |
| Seguridad | 17/17 API + 10/10 en vivo (2 advertencias propias del entorno de desarrollo: `DEBUG` activo y versión de Nginx visible) |
