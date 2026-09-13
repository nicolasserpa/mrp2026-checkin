# ARCHITECTURE — MRP2026 Check-In (app Android)

Leitor **nativo** (Java puro) para o Projeto 7 do Mousetrap Racing 2026.
Complementa o backend em `docs/ARCHITECTURE.md` do repo bourbonAPP (o app
consome a mesma API, sem tocar no backend).

## 1. Por que nativo (e não o PWA)

- `getUserMedia` exige *secure context*: em HTTP de LAN com IP, o PWA não abre a câmera.
- Câmera nativa (Camera2) não exige HTTPS → o evento roda **HTTP puro de LAN**:
  a escada Caddy/CA interna deixa de existir.
- Controle direto de hardware: autofoco contínuo, exposição e **torch (LED)** no box escuro.
- App fino (1 Activity por tela), bateria previsível, build/rebuild em minutos no próprio aparelho.

## 2. Módulos (package `mrp.checkin`)

```
src/mrp/checkin/
  MainActivity        login de staff (POST /api/v1/auth/staff/login)
  ScanActivity        scan contínuo full-screen + verify/presence + torch
  CheckActivity       checklist manual de conformidade (whitelist fixa, espelha o backend)
  SettingsActivity    endpoint da API, teste de /health, troca de usuário
  core/TokenStore     SharedPreferences MODE_PRIVATE (jwt + endpoint + operador)
  net/ApiClient       HttpURLConnection + org.json; callback no main thread
  net/EndpointResolver
  scan/CameraEngine   Camera2: preview + ImageReader + sessão + torch (CaptureRequest)
  scan/DecodeThread   ZXing: YUV -> PlanarYUVLuminanceSource -> MultiFormatReader
  scan/ScanFeedback   vibração + beep (sucesso/erro)
  ui/ResultSheet      card de resultado pós-verify (participante/veículo)
```

Build 100% CLI (sem Gradle): `aapt2 compile --dir res` → `aapt2 link -I android.jar`
→ `javac --release 8` → **D8** → `aapt add classes.dex` → `apksigner sign` (v2/v3).

## 3. Contrato consumido (espelha `pwa/src/api/client.ts` + `types.ts`)

| Fluxo | Endpoint | Corpo |
|---|---|---|
| Login staff | `POST /api/v1/auth/staff/login` | `{username, password}` → `{token}` (JWT 12h) |
| Sessões abertas | `GET /api/v1/sessions?status=open` | lista `[{id, name, kind, date, status}]` |
| Verificar QR | `POST /api/v1/checkin/verify` | `{qr_text, session_id}` → `{valid, subject_type, member/vehicle{...}}` |
| Registrar presença | `POST /api/v1/checkin/presence` | `{qr_text, session_id}` → `{duplicate, last_seen_at}` |
| Conformidade | `POST /api/v1/checkin/conformity` | `{qr_text, session_id, result, items{...}, notes}` → `{conformity_status, checked_at}` |
| Health | `GET /api/v1/health` | `{status, db, signing}` |

### Estado do scan (1 QR → 4 desfechos)

```
SCANNING -> decodificou -> VERIFYING -> verify? 
   200 valid=true   -> CONFIRMED (mostra nome/time p/ conferência visual)
       member: [Registrar presença]  -> presence ok/duplicate
       vehicle: [Conformidade técnica] -> CheckActivity
   400/!valid        -> ERROR (QR inválido/adulterado)
   401               -> logout -> MainActivity (token expirado)
   429               -> ERROR (throttle: ver backend verify:30/min)
```

## 4. Decisões de execução (correções do review — obrigatórias)

1. **Torch é por `CaptureRequest`, não `setTorchMode`.** `CameraManager.setTorchMode`
   exige a câmera FECHADA; durante o scan o torch é ligado/desligado via
   `previewBuilder.set(CaptureRequest.FLASH_MODE, FLASH_MODE_TORCH)` +
   `session.setRepeatingRequest(...)`. Guardado por `FLASH_INFO_AVAILABLE`.
2. **D8, sem `dx`.** Como o `d8` do Termux (3.3.20) tem bug com classes do `javac 21`,
   o build prefere `libs/r8.jar` (8.3.37 do Google Maven) e usa o `d8` do Termux só como
   fallback. `--lib android.jar` para resolução de símbolos Android.
3. **ImageReader com pool pequeno e close garantido.** `newInstance(w, h, YUV_420_888, 2)`
   (2 buffers) e `image.close()` em `finally` em todo frame — previne estouro do pool e
   congelamento da preview. Frames subsampleados para ≤ 1280x720 (target 960x540:
   ZXing não precisa de mais). Decode em `DecodeThread` separado (fila), 120ms entre frames.
4. **Timeouts rígidos + fora da UI thread.** `setConnectTimeout(3000)` e
   `setReadTimeout(5000)`; toda chamada roda em `ExecutorService`; callback devolvido via
   `Handler(Looper.getMainLooper())`. Nunca travar a UI.

## 5. Segurança operacional (LGPD / evento)

- **Cleartext**: `usesCleartextTraffic=true` (endpoint configurável; LAN interna). Riscos
  mitigados: staff desativado revoga token na hora; troca de senhas pré-evento; 1 usuário
  por aparelho (`AuditLog` grava `actor_user` por check-in).
- **Storage**: token em `SharedPreferences` MODE_PRIVATE; `allowBackup=false`. Sem PII
  persistido (QR decodificado fica só em memória até o verify).
- **Runbook do dia**: servidor em `http://192.168.0.10:8000`; `LAN_INSECURE=1`;
  distribuir `mrp-checkin-release.apk` aos fiscais; keystore + credenciais em `~/.keys/`.

## 6. TODO (validar no aparelho real)

- Orientação do preview (postRotate baseado em `SENSOR_ORIENTATION` × rotação da tela):
  conferir com o preview ao vivo e ajustar `configureTransform()` se 90°/270° trocados.
- Teste de leitura em box escuro com torch; presença nos 4 desfechos; duplicidade.
- Opcional: `.github/workflows/build.yml` para build em nuvem (AGP no runner) e release.