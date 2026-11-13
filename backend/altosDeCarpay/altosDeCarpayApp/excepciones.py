from rest_framework import status
from rest_framework.exceptions import APIException

class ConflictoDeNegocio(APIException):
    status_code = status.HTTP_409_CONFLICT
    default_detail = "La operación entra en conflicto con el estado actual del recurso."
    default_code = "conflicto"