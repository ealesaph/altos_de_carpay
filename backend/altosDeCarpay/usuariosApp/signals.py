from django.utils import timezone


def registrar_ultima_conexion(sender, user, **kwargs):
    type(user).objects.filter(pk=user.pk).update(ultima_conexion=timezone.now())
