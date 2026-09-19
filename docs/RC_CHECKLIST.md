# RC Checklist — v0.7.0

Checklist manual p/ validar a candidate release **antes** do ensaio. Preencha a
data e marque os itens conforme o teste **em aparelho real** (hoje: 1 aparelho;
o checklist 2-aparelhos fica para o ensaio).

| # | Cenário | Resultado esperado | OK? | Observação |
|---|---------|--------------------|-----|-----------|
| 1 | Login ok (admin) | Caminha p/ tela de scan; sem erro falso de rede | ☐ | |
| 2 | Login com servidor off | "Servidor inacessível" (não "credenciais") | ☐ | |
| 3 | Login senha errada | "Usuário ou senha inválidos" | ☐ | |
| 4 | Token expirado (401 no scan) | Volta ao login sozinho | ☐ | rede de staff |
| 5 | QR válido membro | Ficha com ✓ verde + "Escanear próximo" | ☐ | |
| 6 | QR válido veículo | Ficha de conformidade com ✓ verde | ☐ | |
| 7 | Servidor off no verify | "Sem conexão" + botão "Tentar novamente" sem reescanear | ☐ | |
| 8 | QR adulterado/inválido | "QR inválido" (nunca "sem conexão") | ☐ | |
| 9 | 429 rápida (throttle) | Mensagem "aguarde" + retry | ☐ | |
| 10 | Presença: registrar | Status sucesso / duplicado com hora | ☐ | |
| 11 | Conformidade: aprovar/reprovar | Envio com trava (sem duplo POST) | ☐ | |
| 12 | Voltar do Check | Ficha do veículo restaurada (não volta p/ escaneando) | ☐ | |
| 13 | Sessão persiste | Fecha/reabre app → sessão anterior selecionada | ☐ | |
| 14 | Sessões sem rede no boot | Toast avisa; não silenciosamente "Sessão 1" | ☐ | |
| 15 | Lanterna | Botão no canto; liga/desliga; some c/ ficha aberta | ☐ | |
| 16 | Aparelho sem flash | Botão de lanterna desabilitado/oculto | ☐ | |
| 17 | Viewfinder | Quadrado branco central aparece sobre o preview | ☐ | |
| 18 | Status bar | Scan: barra escura c/ ícones claros; outras telas: clara | ☐ | |
| 19 | Notch/insets | Nenhum conteúdo colide com status/notch (Login/Ajustes/Check/Scan) | ☐ | |
| 20 | Back no scan | Ficha aberta → fecha e escaneia; sem ficha → confirma saída | ☐ | |
| 21 | Ajustes: endpoint vazio | Bloqueia salvar/testar com aviso | ☐ | |
| 22 | Ajustes: logout | Confirma; volta ao login | ☐ | |
| 23 | Versão no rodapé | "Versão 0.7.0" em Ajustes | ☐ | |
| 24 | Estresse câmera | pause/resume×30, scan→confirm→rescan×30, torch×20 sem crash | ☐ | |
| 25 | crash.log | Vazio após os testes 24 (ao abrir o app não exibe crash) | ☐ | |
| 26 | Animações | Ficha sobe com fade; ✓ em scale-in; lanterna fade; pulse no viewfinder | ☐ | |
| 27 | Animações c/ "remover animações" ON | Sem movimento, só troca de estado | ☐ | |

**Build pumps:** `out/buildinfo.txt` com checksums; APK assinado e verificado
(`apksigner verify`); instala por cima da 0.5.0 (mesmo keystore).

**Versão validada:** 0.7.0 (versionCode 7) — data: ____/____/____

### Fora do escopo da RC
Dark theme; XML/Material Components; offline/sync; Robolectric;
paisagem. Teste em 2 aparelhos (com/sem notch) → ensaio.