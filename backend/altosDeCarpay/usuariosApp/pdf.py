"""Generador liviano de PDF 1.4 (sin dependencias externas) para portabilidad de datos."""
import textwrap
import zlib
from decimal import Decimal
from django.utils import timezone


def _formatear_fecha(valor):
    if not valor:
        return "No registrada"
    if hasattr(valor, "astimezone"):
        valor = timezone.localtime(valor)
        return valor.strftime("%d-%m-%Y %H:%M")
    if hasattr(valor, "strftime"):
        return valor.strftime("%d-%m-%Y")
    return str(valor)


def _formatear_clp(valor):
    if valor is None:
        return "$0"
    try:
        entero = int(Decimal(valor))
        return f"${entero:,}".replace(",", ".")
    except Exception:
        return str(valor)


def _escapar_pdf(texto):
    limpio = (
        str(texto or "")
        .replace("\r", " ")
        .replace("\n", " ")
        .replace("—", "-")
        .replace("–", "-")
        .replace("…", "...")
    )
    codificado = limpio.encode("cp1252", errors="replace").decode("cp1252")
    return (
        codificado.replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)")
    )


def _construir_pdf(paginas_comandos):
    objetos = []

    def agregar_objeto(contenido_bytes):
        objetos.append(contenido_bytes)
        return len(objetos)

    id_catalogo = agregar_objeto(b"")  # Placeholder 1
    id_paginas = agregar_objeto(b"")   # Placeholder 2
    id_fuente_regular = agregar_objeto(
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"
    )
    id_fuente_bold = agregar_objeto(
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"
    )

    ids_paginas = []
    for comandos in paginas_comandos:
        stream = zlib.compress("\n".join(comandos).encode("cp1252", errors="replace"))
        id_contenido = agregar_objeto(
            f"<< /Length {len(stream)} /Filter /FlateDecode >>\nstream\n".encode("ascii")
            + stream
            + b"\nendstream"
        )
        id_pagina = agregar_objeto(
            (
                f"<< /Type /Page /Parent {id_paginas} 0 R "
                f"/MediaBox [0 0 595 842] "
                f"/Resources << /Font << /F1 {id_fuente_regular} 0 R /F2 {id_fuente_bold} 0 R >> >> "
                f"/Contents {id_contenido} 0 R >>"
            ).encode("ascii")
        )
        ids_paginas.append(id_pagina)

    kids = " ".join(f"{pid} 0 R" for pid in ids_paginas)
    objetos[id_catalogo - 1] = f"<< /Type /Catalog /Pages {id_paginas} 0 R >>".encode("ascii")
    objetos[id_paginas - 1] = f"<< /Type /Pages /Kids [{kids}] /Count {len(ids_paginas)} >>".encode("ascii")

    salida = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = [0]
    for idx, obj in enumerate(objetos, start=1):
        offsets.append(len(salida))
        salida.extend(f"{idx} 0 obj\n".encode("ascii"))
        salida.extend(obj)
        salida.extend(b"\nendobj\n")

    xref_pos = len(salida)
    salida.extend(f"xref\n0 {len(objetos) + 1}\n".encode("ascii"))
    salida.extend(b"0000000000 65535 f \n")
    for offset in offsets[1:]:
        salida.extend(f"{offset:010d} 00000 n \n".encode("ascii"))

    salida.extend(
        (
            f"trailer\n<< /Size {len(objetos) + 1} /Root {id_catalogo} 0 R >>\n"
            f"startxref\n{xref_pos}\n%%EOF\n"
        ).encode("ascii")
    )
    return bytes(salida)


def generar_pdf_mis_datos(datos):
    """Genera un documento PDF con los datos personales, consentimiento, reservas y consultas."""
    paginas = [[]]
    y = 790
    margen_x = 48

    def nueva_pagina():
        nonlocal y
        paginas.append([])
        y = 790

    def escribir(texto, *, bold=False, size=10, salto=15, ancho=88, indent=0):
        nonlocal y
        fuente = "F2" if bold else "F1"
        lineas = textwrap.wrap(str(texto or ""), width=ancho) or [""]
        for linea in lineas:
            if y < 56:
                nueva_pagina()
            paginas[-1].append(
                f"BT /{fuente} {size} Tf {margen_x + indent} {y} Td ({_escapar_pdf(linea)}) Tj ET"
            )
            y -= salto

    def separador():
        nonlocal y
        if y < 64:
            nueva_pagina()
        paginas[-1].append(f"0.85 w 0.78 0.78 0.78 RG {margen_x} {y} m 547 {y} l S 0 0 0 RG")
        y -= 16

    perfil = datos.get("perfil") or {}
    nombre_completo = " ".join(
        p for p in (
            perfil.get("nombre"),
            perfil.get("apellido_paterno"),
            perfil.get("apellido_materno"),
        ) if p
    ) or "Sin nombre"

    escribir("ALTOS DE CARPAY", bold=True, size=16, salto=20)
    escribir("Copia de Datos Personales (Ley N° 21.719)", bold=True, size=12, salto=16)
    escribir(f"Fecha de emisión: {_formatear_fecha(datos.get('fecha_emision'))}", size=9, salto=14)
    separador()

    escribir("1. Perfil del titular", bold=True, size=12, salto=18)
    escribir(f"Nombre completo: {nombre_completo}")
    escribir(f"Correo electrónico: {perfil.get('correo') or '-'}")
    escribir(f"RUT: {perfil.get('rut') or '-'}")
    escribir(f"Teléfono: {perfil.get('telefono') or 'No registrado'}")
    escribir(f"Rol: {perfil.get('rol') or '-'}   |   Estado: {perfil.get('estado') or '-'}")
    escribir(f"Última conexión: {_formatear_fecha(perfil.get('ultima_conexion'))}")
    y -= 6
    separador()

    escribir("2. Consentimiento de datos", bold=True, size=12, salto=18)
    escribir(f"Versión de política: {datos.get('consentimiento_version') or 'No registrada'}")
    escribir(f"Fecha de aceptación: {_formatear_fecha(datos.get('consentimiento_datos'))}")
    y -= 6
    separador()

    reservas = datos.get("reservas") or []
    escribir(f"3. Reservas asociadas ({len(reservas)})", bold=True, size=12, salto=18)
    if not reservas:
        escribir("No tienes reservas registradas.", indent=8)
    else:
        for r in reservas:
            hora = str(r.get("hora_inicio") or "")[:5]
            linea = (
                f"• Reserva #{r.get('id_reserva')} — Fecha: {_formatear_fecha(r.get('fecha'))} {hora} "
                f"({r.get('horas_reservadas')} h) | Estado: {r.get('estado')} | "
                f"Total: {_formatear_clp(r.get('monto_total'))}"
            )
            escribir(linea, size=9, salto=13, ancho=94, indent=8)
    y -= 6
    separador()

    consultas = datos.get("consultas") or []
    escribir(f"4. Consultas registradas ({len(consultas)})", bold=True, size=12, salto=18)
    if not consultas:
        escribir("No tienes consultas registradas.", indent=8)
    else:
        for c in consultas:
            escribir(
                f"• Consulta #{c.get('id_consulta')} — {c.get('asunto')} "
                f"[{c.get('estado')}] ({_formatear_fecha(c.get('fecha_envio'))})",
                bold=True,
                size=9,
                salto=13,
                ancho=92,
                indent=8,
            )
            escribir(c.get("mensaje") or "", size=9, salto=13, ancho=90, indent=16)
            y -= 3

    return _construir_pdf(paginas)
