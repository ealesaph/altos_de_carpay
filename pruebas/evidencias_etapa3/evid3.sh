#!/bin/sh
python - <<'PY'
import os, json, psycopg2
c=psycopg2.connect(dbname=os.environ['DB_NAME'],user=os.environ['DB_USER'],password=os.environ['DB_PASSWORD'],host='db')
k=c.cursor()
T=['usuario','sector','servicio','reserva','reserva_sector','reserva_servicio','pago','validacion_acceso','bitacora_incidencia','review','consulta','logs_modificacion','brecha_seguridad']
out={}
for t in T:
    k.execute("""select column_name,data_type,character_maximum_length,numeric_precision,numeric_scale,is_nullable,column_default
                 from information_schema.columns where table_schema='public' and table_name=%s order by ordinal_position""",(t,))
    cols=[list(r) for r in k.fetchall()]
    k.execute("""select tc.constraint_type,kcu.column_name,ccu.table_name,ccu.column_name,tc.constraint_name
                 from information_schema.table_constraints tc
                 join information_schema.key_column_usage kcu on tc.constraint_name=kcu.constraint_name and tc.table_schema=kcu.table_schema
                 left join information_schema.constraint_column_usage ccu on tc.constraint_name=ccu.constraint_name and tc.constraint_type='FOREIGN KEY'
                 where tc.table_schema='public' and tc.table_name=%s""",(t,))
    cons=[list(r) for r in k.fetchall()]
    k.execute("""select pg_get_constraintdef(oid),conname from pg_constraint where conrelid=%s::regclass and contype='c'""",(t,))
    chk=[list(r) for r in k.fetchall()]
    k.execute("select indexname,indexdef from pg_indexes where schemaname='public' and tablename=%s",(t,))
    idx=[list(r) for r in k.fetchall()]
    out[t]={'cols':cols,'cons':cons,'checks':chk,'idx':idx}
os.makedirs('/app/_evidencia',exist_ok=True)
json.dump(out,open('/app/_evidencia/esquema.json','w'),default=str,ensure_ascii=False,indent=1)
print("Esquema exportado:",len(out),"tablas ->",'/app/_evidencia/esquema.json')
for t in T: print(f"  {t:<22} columnas={len(out[t]['cols']):<3} restricciones={len(out[t]['cons'])+len(out[t]['checks'])}")
PY
