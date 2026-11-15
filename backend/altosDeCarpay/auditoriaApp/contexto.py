from contextvars import ContextVar

_usuario_actual = ContextVar("usuario_actual", default=None)


def fijar_usuario_actual(usuario):
    _usuario_actual.set(usuario)

def obtener_usuario_actual():
    usuario = _usuario_actual.get()
    if usuario is not None and getattr(usuario, "is_authenticated", False):
        return usuario
    return None

def limpiar_usuario_actual():
    _usuario_actual.set(None)