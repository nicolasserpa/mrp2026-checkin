# RC Checklist — v0.7.0 (+ pós-RC)

Checklist manual p/ validar a candidate release **antes** do ensaio (hoje: 1
aparelho; o checklist 2-aparelhos fica para o ensaio).

**Legenda da coluna "OK?"** — importante para não dar um "ok" falso:

| Marca | Significado |
|-------|-------------|
| **OK (aparelho)** | Usuário confirmou no aparelho real. |
| **OK (dev)** | Exercitado no APK dev (`mrp.checkin.dev`) com **dados mockados**: valida UI, navegação e copy — **não** valida o contrato com o backend (verify/presence/conformity reais nunca foram chamados). |
| **parcial** | Visto na tela, mas a ação não foi confirmada. |
| ☐ | Não testado. |

## Pendências que exigem o backend real (itens 1–4, 7–14)

Nada disso foi exercitado: o modo dev não faz chamada de rede. Precisa do
servidor no evento (ou staging) para fechar.

| # | Cenário | Resultado esperado | OK? | Observação |
|---|---------|--------------------|-----|-----------|
| 1 | Login ok (admin) | Caminha p/ tela de scan; sem erro falso de rede | ☐ | |
| 2 | Login com servidor off | "Servidor inacessível" (não "credenciais") | ☐ | |
| 3 | Login senha errada | "Usuário ou senha inválidos" | ☐ | |
| 4 | Token expirado (401 no scan) | Volta ao login sozinho | ☐ | |
| 7 | Servidor off no verify | "Sem conexão" + botão "Tentar novamente" sem reescanear | ☐ | |
| 8 | QR adulterado/inválido | "QR inválido" (nunca "sem conexão") | ☐ | |
| 9 | 429 rápida (throttle) | Mensagem "aguarde" + retry | ☐ | |
| 10 | Presença: registrar | Status sucesso / duplicado com hora | ☐ | |
| 11 | Conformidade: aprovar/reprovar | Envio com trava (sem duplo POST) | ☐ | |
| 12 | Voltar do Check | Ficha do veículo restaurada (não volta p/ escaneando) | ☐ | |
| 13 | Sessão persiste | Fecha/reabre app → sessão anterior selecionada | ☐ | |
| 14 | Sessões sem rede no boot | Toast avisa; não silenciosamente "Sessão 1" | ☐ | |

## Itens de tela (testados no APK dev / aparelho)

| # | Cenário | Resultado esperado | OK? | Observação |
|---|---------|--------------------|-----|-----------|
| 5 | QR válido membro | Ficha com ✓ + "Escanear próximo" | **OK (dev)** | Simulado via botão dev; ficha e "Escanear próximo" conferidos. Backend não chamado. |
| 6 | QR válido veículo | Ficha de conformidade com ✓ | **OK (dev)** | Simulado; origem do bug de layout do botão (item 26). |
| 15 | Lanterna | some c/ ficha aberta, **volta** ao "Escanear próximo" | **OK (aparelho)** | Bug real corrigido: a política era avaliada durante o fade-out da ficha. Coberto por `TorchWiringSourceTest` (falha se o wiring voltar a ser síncrono). |
| 16 | Aparelho sem flash | Botão de lanterna desabilitado/oculto | ☐ | |
| 17 | Viewfinder | Quadrado branco central sobre o preview | ☐ | |
| 18 | Status bar | Scan: barra escura c/ ícones claros; outras telas: clara | ☐ | |
| 19 | Notch/insets (navbar + zona de gesto) | Nenhum botão sob a barra / faixa de "home" | **OK (aparelho)** | Insets de `navigationBars`+`systemGestures`+`tappableElement`+`ime`. **Falta**: notch/cutout real e navbar de OEM customizado (One UI/MIUI). |
| 20 | Back no scan | Ficha aberta → fecha e escaneia; sem ficha → confirma saída | ☐ | |
| 21 | Ajustes: endpoint vazio | Bloqueia salvar/testar com aviso | ☐ | |
| 22 | Ajustes: logout | Confirma; volta ao login | ☐ | |
| 23 | Versão no rodapé | "Versão 0.7.0" em Ajustes | ☐ | Ainda 0.7.0/code 7 — bump para 0.8.0 pendente. |
| 24 | Estresse câmera | pause/resume×30, scan→confirm→rescan×30, torch×20 sem crash | ☐ | |
| 25 | crash.log | Vazio após os testes 24 (ao abrir o app não exibe crash) | ☐ | |
| 26 | Animações | Ficha fade+slide; ✓ scale-in; lanterna fade; pulse no viewfinder | ☐ | O ✓ virou vetor (`CheckBadge`) — a animação de scale-in continua, mas **não** foi reconferida depois da troca. |
| 27 | Animações c/ "remover animações" ON | Sem movimento, só troca de estado | ☐ | |

## Entregue depois da RC 0.7.0 (fora do escopo original, já no código)

| Entrega | Status | Observação |
|---------|--------|-----------|
| Dark theme | código pronto, **não validado no aparelho** | Nunca houve confirmação explícita em dark; se o usuário validou sem dizer, registrar aqui. |
| Paisagem | código pronto, **não validado no aparelho** | `refreshTransform()` + layout por rotação. Risco conhecido: preview 90/270 pode precisar de ajuste fino (`ARCHITECTURE.md` §6). |
| Respiro de layout (itens 5/6) | **OK (aparelho)** | Gap de 12dp, ritmo label→campo, botão de busca no rodapé do card. |
| ✓ vetorial (anti-alias) | **OK (aparelho)** | `CheckBadge`; report "peludo" resolvido. |
| Modo dev offline | **OK (aparelho)** | APK lado a lado; é o que destravou os testes 5/6. |

**Build pumps:** `out/buildinfo.txt` com checksums; APK assinado e verificado
(`apksigner verify`); instala por cima da 0.5.0 (mesmo keystore).

**Versão validada:** 0.7.0 (versionCode 7) — data: ____/____/____
**HEAD:** além da 0.7.0 (dark, paisagem, insets, modo dev) **sem bump de versão**.

### Fora do escopo da RC
XML/Material Components; offline/sync; Robolectric; CI. Teste em 2 aparelhos
(com/sem notch) → ensaio.