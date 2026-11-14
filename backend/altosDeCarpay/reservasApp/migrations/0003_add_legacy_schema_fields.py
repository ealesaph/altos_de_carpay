import secrets

from django.db import migrations
from django.db.models.fields import NOT_PROVIDED


def _column_names(schema_editor, model):
    with schema_editor.connection.cursor() as cursor:
        return {
            column.name
            for column in schema_editor.connection.introspection.get_table_description(
                cursor, model._meta.db_table
            )
        }


def _add_field(schema_editor, model, field):
    if schema_editor.connection.vendor != "sqlite":
        schema_editor.add_field(model, field)
        return

    original_fields = model._meta.local_fields
    columns = _column_names(schema_editor, model)
    model._meta.local_fields = [
        existing for existing in original_fields
        if existing.column is None or existing.column in columns
    ]
    model._meta._expire_cache()
    try:
        schema_editor.add_field(model, field)
    finally:
        model._meta.local_fields = original_fields
        model._meta._expire_cache()


def _add_field_if_missing(schema_editor, model, field_name):
    field = model._meta.get_field(field_name)
    if field.column in _column_names(schema_editor, model):
        return

    if field_name != "token_qr":
        _add_field(schema_editor, model, field)
        return

    nullable_field = field.clone()
    nullable_field.model = model
    nullable_field.set_attributes_from_name(field.name)
    nullable_field.null = True
    nullable_field.unique = False
    nullable_field.default = NOT_PROVIDED
    _add_field(schema_editor, model, nullable_field)

    table = schema_editor.quote_name(model._meta.db_table)
    primary_key = schema_editor.quote_name(model._meta.pk.column)
    column = schema_editor.quote_name(field.column)
    with schema_editor.connection.cursor() as cursor:
        cursor.execute(f"SELECT {primary_key} FROM {table} WHERE {column} IS NULL")
        for (row_id,) in cursor.fetchall():
            cursor.execute(
                f"UPDATE {table} SET {column} = %s WHERE {primary_key} = %s",
                [secrets.token_urlsafe(32), row_id],
            )

    schema_editor.alter_field(model, nullable_field, field, strict=False)


def add_legacy_schema_fields(apps, schema_editor):
    usuario = apps.get_model("usuariosApp", "Usuario")
    for field_name in ("is_superuser", "telefono", "consentimiento_datos"):
        _add_field_if_missing(schema_editor, usuario, field_name)

    for field_name in ("groups", "user_permissions"):
        through = usuario._meta.get_field(field_name).remote_field.through
        tables = schema_editor.connection.introspection.table_names()
        if through._meta.db_table not in tables:
            schema_editor.create_model(through)

    sector = apps.get_model("reservasApp", "Sector")
    for field_name in ("descripcion", "fotografias"):
        _add_field_if_missing(schema_editor, sector, field_name)

    reserva = apps.get_model("reservasApp", "Reserva")
    for field_name in ("hora_inicio", "condiciones_aceptadas_en", "token_qr"):
        _add_field_if_missing(schema_editor, reserva, field_name)

    pago = apps.get_model("reservasApp", "Pago")
    for field_name in ("tipo_pago", "metodo_pago"):
        _add_field_if_missing(schema_editor, pago, field_name)


class Migration(migrations.Migration):
    dependencies = [
        ("reservasApp", "0002_initial"),
        ("usuariosApp", "0001_initial"),
    ]

    operations = [
        migrations.RunPython(add_legacy_schema_fields),
    ]
