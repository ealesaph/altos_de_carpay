"""Exportación de reportes compatibles con Excel (HU-08 · RF-09 · RNF-05).

``formato=xlsx`` genera un libro de Excel con tipos nativos (fechas, horas y montos) y
``formato=csv`` (por defecto) un CSV con BOM y separador «;». En ambos casos los textos que
empiezan con =, +, -, @ se neutralizan para que Excel no los ejecute como fórmulas.
"""
import csv
import io
from datetime import date, datetime, time
from decimal import Decimal

from django.http import HttpResponse
from django.utils import timezone
from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

PELIGROSOS = ("=", "+", "-", "@", "\t", "\r")
TIPO_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"


def celda_segura(valor):
    texto = "" if valor is None else str(valor)
    return "'" + texto if texto[:1] in PELIGROSOS else texto


def _texto_csv(valor):
    if isinstance(valor, bool):
        return "Sí" if valor else "No"
    if isinstance(valor, datetime):
        return timezone.localtime(valor).strftime("%Y-%m-%d %H:%M") if timezone.is_aware(valor) else valor.strftime("%Y-%m-%d %H:%M")
    if isinstance(valor, time):
        return valor.strftime("%H:%M")
    return celda_segura(valor)


def _valor_excel(valor):
    if isinstance(valor, bool):
        return "Sí" if valor else "No"
    if isinstance(valor, datetime) and timezone.is_aware(valor):
        return timezone.localtime(valor).replace(tzinfo=None)
    return valor


def _formato_numero(valor, columna):
    if isinstance(valor, datetime):
        return "DD-MM-YYYY HH:MM"
    if isinstance(valor, date):
        return "DD-MM-YYYY"
    if isinstance(valor, time):
        return "HH:MM"
    if isinstance(valor, Decimal) or columna.lower().startswith(("monto", "saldo", "valor", "cargo")):
        return '"$"#,##0'
    return None


def _xlsx(titulo, columnas, filas):
    libro = Workbook()
    hoja = libro.active
    hoja.title = titulo[:31]
    hoja.append(columnas)
    for celda in hoja[1]:
        celda.font = Font(bold=True, color="FFFFFF")
        celda.fill = PatternFill("solid", fgColor="C00000")
        celda.alignment = Alignment(vertical="center")
    anchos = [len(str(c)) for c in columnas]
    for fila in filas:
        hoja.append([_valor_excel(v) for v in fila])
        for i, (valor, celda) in enumerate(zip(fila, hoja[hoja.max_row])):
            if isinstance(valor, str) and valor[:1] in PELIGROSOS:
                celda.value = valor
                celda.data_type = "s"  # texto literal: nunca fórmula
            formato = _formato_numero(valor, columnas[i])
            if formato:
                celda.number_format = formato
            anchos[i] = max(anchos[i], min(len(_texto_csv(valor)), 50))
    for i, ancho in enumerate(anchos, start=1):
        hoja.column_dimensions[get_column_letter(i)].width = ancho + 2
    hoja.freeze_panes = "A2"
    hoja.auto_filter.ref = hoja.dimensions
    salida = io.BytesIO()
    libro.save(salida)
    return salida.getvalue()


def respuesta_exportacion(nombre_archivo, titulo, columnas, filas, formato="csv"):
    filas = list(filas)
    if (formato or "csv").lower() == "xlsx":
        respuesta = HttpResponse(_xlsx(titulo, columnas, filas), content_type=TIPO_XLSX)
        respuesta["Content-Disposition"] = f'attachment; filename="{nombre_archivo}.xlsx"'
        return respuesta
    respuesta = HttpResponse(content_type="text/csv; charset=utf-8")
    respuesta["Content-Disposition"] = f'attachment; filename="{nombre_archivo}.csv"'
    respuesta.write("﻿")
    escritor = csv.writer(respuesta, delimiter=";")
    escritor.writerow(columnas)
    for fila in filas:
        escritor.writerow([_texto_csv(v) for v in fila])
    return respuesta
