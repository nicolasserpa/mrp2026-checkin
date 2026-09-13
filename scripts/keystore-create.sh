#!/usr/bin/env bash
# Gera o keystore de release UMA vez (fora do repo) e grava credenciais em
# $HOME/.keys/mrp-release.env (chmod 600). As senhas nunca vão para o git.
set -euo pipefail

STORE_DIR="${KEYSTORE_DIR:-$HOME/.keys}"
STORE="$STORE_DIR/mrp-release.jks"
PASS_FILE="$STORE_DIR/mrp-release.env"

mkdir -p "$STORE_DIR"

if [ -f "$STORE" ]; then
    echo "Já existe: $STORE"
    echo "Apague o arquivo se quiser regenerar (invalida APKs já instalados)."
    exit 1
fi

PASS="$(head -c 24 /dev/urandom | base64 | tr -d '/+=' | head -c 20)"
ALIAS="checkin"

keytool -genkeypair \
    -keystore "$STORE" \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$PASS" \
    -keypass "$PASS" \
    -dname "CN=MRP2026 Check-In,O=Mousetrap Racing,C=BR"

chmod 600 "$STORE"

cat > "$PASS_FILE" <<EOF
KEYSTORE_PATH=$STORE
KEYSTORE_PASS=$PASS
KEY_ALIAS=$ALIAS
EOF
chmod 600 "$PASS_FILE"

echo "Keystore criado em: $STORE"
echo "Credenciais salvas em $PASS_FILE (chmod 600, fora do repo)."
echo "Backup desse par fora do aparelho: sem ele, o APK instalado não recebe update."