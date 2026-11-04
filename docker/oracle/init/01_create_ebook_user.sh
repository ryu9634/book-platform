#!/bin/bash
# gvenzl/oracle-free 는 APP_USER 로 첫 사용자만 만들어 주므로 두 번째 스키마(ebook)는 직접 생성합니다.
# 비밀번호는 docker-compose 가 .env 에서 넘겨준 EBOOK_DB_PASSWORD 를 씁니다.
set -e

if [ -z "${EBOOK_DB_PASSWORD}" ]; then
  echo "EBOOK_DB_PASSWORD 가 비어 있습니다." >&2
  exit 1
fi

sqlplus -s / as sysdba <<SQL
WHENEVER SQLERROR EXIT SQL.SQLCODE
ALTER SESSION SET CONTAINER = FREEPDB1;
CREATE USER ebook IDENTIFIED BY "${EBOOK_DB_PASSWORD}" QUOTA UNLIMITED ON USERS;
GRANT CONNECT, RESOURCE TO ebook;
SQL
