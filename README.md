# MRP2026 Check-In

Leitor **Android nativo** (Java + ZXing, sem Gradle) para o check-in e
credenciamento do **Mousetrap Racing 2026 — Projeto 7**. O app faz login de
staff, escaneia o QR criptografado (credencial ou chassi), registra **presença**
e **conformidade técnica** contra a API Django DRF (`/api/v1`).

Câmera nativa (Camera2): dispensa HTTPS/CA interna — o evento roda em HTTP puro
de LAN (`http://<IP>:8000`), sem Caddy.

## Build (Termux/Linux, CLI puro)

```bash
pkg install openjdk-21 aapt2 aapt apksigner
bash scripts/build.sh      # gera out/app-unsigned.apk (baixa android.jar + r8.jar em libs/)
bash scripts/keystore-create.sh   # gera ~/.keys/mrp-release.jks + credenciais (fora do repo)
bash scripts/sign.sh       # gera out/mrp-checkin-release.apk assinado e verificado
```

Instalador:
```bash
cp out/mrp-checkin-release.apk ~/storage/downloads/   # requer termux-setup-storage uma vez
termux-open ~/storage/downloads/mrp-checkin-release.apk
```

## Requisitos do backend para LAN

- `ALLOWED_HOSTS` do Django deve incluir o IP do servidor (`192.168.0.10` neste ambiente).
- Com `DEBUG=False`, libere HTTP puro: `LAN_INSECURE=1` (desliga redirect HTTPS/HSTS/cookies secure).
- O token JWT do app expira em 12h; **revogação imediata**: desativar o staff no Admin
  (`is_active=False`) — o backend revalida em cada request.

## Notas de segurança

- Keystore e credenciais **fora do repositório** (`~/.keys/`, chmod 600). Perder o keystore = sem update do APK instalado.
- APK fala HTTP em claro na LAN (app interno). Token armazenado em `SharedPreferences` (MODE_PRIVATE) com `allowBackup=false`.
- Sem PII persistido no aparelho; o app só exibe o nome retornado pelo `verify` após decodificar.

Arquitetura e runbook: `docs/ARCHITECTURE.md`.