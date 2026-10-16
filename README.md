# MRP2026 Check-In

Leitor **Android nativo** (Java + ZXing) para o check-in e
credenciamento do **Mousetrap Racing 2026 — Projeto 7**. O app faz login de
staff, escaneia o QR criptografado (credencial ou chassi), registra **presença**
e **conformidade técnica** contra a API Django DRF (`/api/v1`).

Câmera nativa (Camera2): dispensa HTTPS/CA interna — o evento roda em HTTP puro
de LAN (`http://<IP>:8000`), sem Caddy.

## Build (Gradle + GitHub Actions)

Build canônico: **GitHub Actions** (workflow `build`), fonte da verdade.
Push em qualquer branch compila, roda os unit tests e publica os APKs como
artifacts (baixe no navegador do celular e instale).

```bash
# Local (Linux/macOS com Android SDK; Termux não roda o AGP de forma confiável)
export ANDROID_HOME=~/Android/Sdk
./gradlew :app:testProdDebugUnitTest :app:testDevDebugUnitTest  # 12 classes, JVM pura
./gradlew :app:assembleProdRelease :app:assembleDevRelease      # unsigned sem keystore
```

Flavors: `prod` (package `mrp.checkin`) e `dev` (package `mrp.checkin.dev`,
label "MRP Dev", instalável lado a lado — substitui o antigo `build-dev.sh`).
Release sai com R8 (`minifyEnabled` + `shrinkResources`); sem Material/AppCompat.

Assinatura (uma vez por ambiente):
```bash
bash scripts/keystore-create.sh   # gera ~/.keys/mrp-release.jks + credenciais (fora do repo)
# CI: cadastrar secrets KEYSTORE_BASE64 (base64 do .jks), KEYSTORE_PASS, KEY_ALIAS.
# Sem secrets o release compila unsigned (não instala). scripts/sign.sh segue
# disponível como helper de assinatura local.
```

Instalador: baixe o artifact `apks` da run do Actions no navegador do
celular (vai para Download) e instale por cima da versão anterior (mesmo
keystore, update sem perda):

## Requisitos do backend para LAN

- `ALLOWED_HOSTS` do Django deve incluir o IP do servidor (`192.168.0.30` neste ambiente; confira com `ip addr` — DHCP muda o IP).
- Com `DEBUG=False`, libere HTTP puro: `LAN_INSECURE=1` (desliga redirect HTTPS/HSTS/cookies secure).
- O token JWT do app expira em 12h; **revogação imediata**: desativar o staff no Admin
  (`is_active=False`) — o backend revalida em cada request.

## Notas de segurança

- Keystore e credenciais **fora do repositório** (`~/.keys/`, chmod 600). Perder o keystore = sem update do APK instalado.
- APK fala HTTP em claro na LAN (app interno). Token armazenado em `SharedPreferences` (MODE_PRIVATE) com `allowBackup=false`.
- Sem PII persistido no aparelho; o app só exibe o nome retornado pelo `verify` após decodificar.

Arquitetura e runbook: `docs/ARCHITECTURE.md`.