# RC Vision — v0.7.0

Vision consolidada (Bruno/mobile + Carlos/tech-lead) para a **candidate release**
do app de check-in MRP2026. Foco: acabamento da UI + confiabilidade no dia do
evento. Fora de escopo: dark theme, XML/Material Components, offline/sync,
Robolectric, paisagem.

## Tema base (todas as telas)

- Status bar **SURFACE clara + ícones escuros** em Login, Ajustes e Check
  (`M3.surfaceSystemBars`). Scan usa overlay próprio (abaixo).
- Tema no `styles.xml`: `forceDarkAllowed=false`, `adjustResize` (teclado não
  cobre o rodapé), status/nav SURFACE por padrão.
- Insets resolvidos em código por `M3.safeArea()` / `M3.edgeToEdge(...)`
  (Login/Ajustes/Check/Setup): base = `max(navigationBars, systemGestures,
  tappableElement, ime)` — o mesmo cálculo serve para navbar de 3 botões e
  para o gesto "home", sem bifurcar. Rodapé dos formulários, botão primário da
  ficha e lanterna do Scan nunca caem atrás da barra.

## Login

- Rede ≠ credencial: `NETWORK` → "Servidor inacessível…"; `THROTTLE` →
  "aguarde"; senha/usuário inválidos separado.
- Toggle de senha com `contentDescription` (acessibilidade).

## Scan (tela crítica)

- Header **translúcido escuro** (`0xB3000000`) com texto branco; sessão
  selecionada em cliente escura.
- **Lanterna flutuante** no terço inferior-direito (zona do polegar); oculta
  quando a ficha está aberta; desabilitada sem flash (`onFlashSupport`).
- **Viewfinder** 240dp central (arredondado, contorno branco).
- Hint pill "Aponte a câmera…" abaixo do header.
- **Sessão persiste** em `checkin_prefs`; carregamento com retry (2x) e, se
  falhar, nunca troca por padrão em silêncio (avisa com Toast).
- **Erro de rede ≠ QR inválido**: mensagens por `errorKind`; "Tentar
  novamente" **reusa o QR em memória** (`RetryTarget.VERIFY`/`PRESENCE`).
- Back: ficha aberta → fecha a ficha e volta a escanear; senão → confirma saída.
- Voltar do Check → **restaura a ficha** (não reseta a confirmação).

## Ficha (ResultSheet)

- **Sucesso inequívoco**: bloco `SUCCESS_CONTAINER` com ✓ 56sp + "Confirmado"
  no topo da ficha.
- Um botão primário (Presença OU Conformidade) + secundário tonal; "Escanear
  próximo" sempre visível no confirmado.
- Erro de rede/429 → botão primário "Tentar novamente".

## Check

- Resultado identificado por **id** (não por texto); notas com
  `M3.outlinedTextArea`.
- **Trava de duplo envio** ("Enviando…"); 401 → vai ao login (não deixa app morto);
  rede/429 com mensagens próprias.

## Ajustes

- Endpoint vazio é validado; logout com confirmação; **versão no rodapé**
  (v0.7.0, lida do manifest em runtime).

## Animações (sutis, em código, respeitam `ANIMATOR_DURATION_SCALE`)

- ResultSheet sobe com fade + slide-up ao abrir; fade-out ao fechar.
- Bloco "✓ Confirmado" faz scale-in com overshoot leve.
- Botão de lanterna/hint: fade na transição abrir/fechar ficha (sem `setVisibility` seco).
- Viewfinder: pulse curto no anel ao decodificar o QR (sem tocar no fluxo da câmera).

## Código/qualidade

- `ScanPhase` (enum), `VerifyParser` (puro), `ApiClient.Result.errorKind()`,
  `AuthFlow` (401 centralizado), `CrashReporter` (dedupe), `LogTruncate`.
- `scripts/test.sh`: JUnit4 da lógica pura (28 testes).
- `build.sh` gera `out/buildinfo.txt` com checksums.
- Build assinável e instalável por cima da 0.5.0 (mesmo keystore).