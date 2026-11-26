#!/bin/sh
python manage.py test 2>&1 | tail -n 25
