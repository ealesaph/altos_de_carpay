#!/bin/sh
python - <<'PY'
import os, psycopg2
c=psycopg2.connect(dbname=os.environ['DB_NAME'],user=os.environ['DB_USER'],password=os.environ['DB_PASSWORD'],host='db')
k=c.cursor()
print("== Base de datos en produccion:", os.environ['DB_NAME'], "| PostgreSQL", end=" ")
k.execute("show server_version");print(k.fetchone()[0])
k.execute("""select t.table_name,(select count(*) from information_schema.columns c where c.table_name=t.table_name) from information_schema.tables t
where table_schema='public' and table_name in ('usuario','sector','servicio','reserva','reserva_sector','reserva_servicio','pago','validacion_acceso','bitacora_incidencia','review','consulta','logs_modificacion','brecha_seguridad') order by 1""")
r=k.fetchall();print(f"== Tablas del modelo de negocio ({len(r)}):")
for n,cc in r:
    k.execute(f'select count(*) from "{n}"');print(f"   {n:<22} columnas={cc:<3} filas={k.fetchone()[0]}")
k.execute("""select tc.constraint_type,count(*) from information_schema.table_constraints tc where tc.table_schema='public' and tc.table_name in ('usuario','sector','servicio','reserva','reserva_sector','reserva_servicio','pago','validacion_acceso','bitacora_incidencia','review','consulta','logs_modificacion','brecha_seguridad') group by 1 order by 1""")
print("== Restricciones:", ", ".join(f"{a}={b}" for a,b in k.fetchall()))
k.execute("""select tablename,indexname from pg_indexes where schemaname='public' and tablename in ('reserva','reserva_sector','reserva_servicio','logs_modificacion','usuario','pago','validacion_acceso','bitacora_incidencia') and indexname not like '%pkey' order by 1,2""")
print("== Indices (sin claves primarias):")
for t,i in k.fetchall(): print(f"   {t:<20} {i}")
PY
