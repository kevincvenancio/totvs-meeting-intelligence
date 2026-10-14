# Hermes — Front-End

Interface da plataforma de *meeting intelligence*: a página inicial, que é o material
do pitch (substitui o PowerPoint), e o aplicativo que consome a API REST em `/api/v1`.

## Executar

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
```

O Vite faz proxy de `/api` para `http://localhost:8080`, então basta subir a API
Spring em paralelo (`./mvnw spring-boot:run` na raiz do projeto) para ver a base real.

**Sem a API no ar o front continua funcionando**: cada consulta cai num conjunto de
demonstração determinístico (`src/lib/demoData.ts`) e a interface avisa em uma faixa
que está em modo vitrine. Isso vale tanto para o fetch que estoura quanto para o
502 que o proxy do Vite devolve quando o back-end está fora.

```bash
npm run build    # tsc -b && vite build  →  dist/
npm run preview  # serve o dist
```

## Identidade visual

Descritor: **obsidiana com azul TOTVS — analytics editorial sobre papel quente**.

| | |
|---|---|
| Display | Instrument Serif (400 / 400 itálico) |
| Interface | Inter Tight Variable |
| Dados e códigos | JetBrains Mono Variable |
| Superfícies escuras | `#08090b` · `#0e1015` · `#12141a` · `#1a1e26` |
| Superfícies claras | `#f4f1ea` · `#eae6dc` · `#ded9cc` |
| Acento (azul TOTVS) | `#2f7bff` no escuro, `#0057ff` no claro; realce `#5c9bff` |

As fontes são auto-hospedadas via `@fontsource`: o site abre igual sem rede e sem
CDN de terceiros. Marca, ícone e imagem de compartilhamento são SVG gerados em
`public/brand/` — não há bitmap no projeto.

### Paleta de dados

Seis slots, validados com o roteiro de cores do skill `dataviz` nos dois modos
(banda de luminosidade, piso de croma, separação para daltonismo, contraste
contra a superfície):

| Slot | Escuro | Claro |
|---|---|---|
| 1 azure | `#3e87d8` | `#2b6fbf` |
| 2 brasa | `#d9622c` | `#c4551f` |
| 3 jade | `#2a9770` | `#1b7f5c` |
| 4 violeta | `#7e70dc` | `#6355c4` |
| 5 âmbar | `#b58412` | `#8f6706` |
| 6 rosa | `#d44e70` | `#be3c5c` |

**A ordem dos slots é o mecanismo de segurança para daltonismo, não é estética** —
foi escolhida entre as ordenações que passam em todos os limites com os vizinhos
adjacentes. Reordenar quebra a validação. O modo claro não é o escuro invertido:
são passos próprios, validados contra o papel.

Cores de estado (`--color-state-*`) são reservadas e nunca reaproveitadas como
série de gráfico; sempre aparecem com rótulo por escrito ao lado, nunca sozinhas.

## Estrutura

```
src/
  lib/          motion (GSAP + Lenis), api, types, demoData, format, roteiro do pitch
  components/
    brand/      marca, logotipo, órbitas
    chrome/     preloader, cursor, navegação, trilho de índice, grão, modo, modo apresentação
    fx/         campo de luz azul (WebGL), faixa, primitivas
    viz/        rosca, ranking, tendência, moldura de gráfico
    app/        moldura das telas, selo, estados vazios
  sections/     pitch/ (os cinco blocos da home) e o rodapé
  routes/       landing, entrar, painel, reuniões, clientes, planos, importações
  styles/       tokens · base · app (chrome e rodapé) · painel (aplicativo) · pitch (home)
```

## A página do pitch

A home segue o roteiro da apresentação final: cinco blocos de tela cheia, cada um
correspondendo a um trecho da fala. A ordem, o tempo, quem fala e quantos itens cada
bloco revela ficam em [`src/lib/roteiro.ts`](src/lib/roteiro.ts).

| Bloco | Tempo | Na tela |
|---|---|---|
| 01 · O problema | 0:00–0:40 | Herói com a tese; três trechos de transcrição (reclamação, oportunidade, churn) acendem um a um; o último passo liga a reclamação ao churn da mesma conta |
| 02 · Por que vale dinheiro | 0:40–1:15 | "5–25×" e "~30%" com a fonte embaixo, depois a conclusão |
| 03 · O que construímos | 1:15–2:30 | As quatro etapas do motor (ingestão → extração → classificação → agregação) acendem quando citadas; por fim a API, o volume processado e a stack |
| 04 · Três níveis | 2:30–3:20 | Reunião → conta → carteira, a frase do diferencial e as áreas que usam |
| 05 · Impacto financeiro | 3:20–4:00 | Três ganhos com a premissa embaixo, o total e o botão "Abrir painel" |

### Apresentando

O passador de slides funciona como num deck: ele envia setas ou Page Up/Down, e cada
clique revela o próximo item do bloco antes de passar ao seguinte.

| Tecla | Ação |
|---|---|
| → ↓ espaço Page Down | Avança um item (ou vai para o próximo bloco) |
| ← ↑ Page Up | Volta um item |
| 1 a 5 | Pula para o bloco, do começo |
| Home | Volta ao início |
| P | Painel do apresentador: quem fala, janela de tempo e cronômetro do ensaio |
| F | Tela cheia |

Rolando com o mouse, a página funciona como uma landing comum: cada bloco chega
completo. Durante a apresentação pelo teclado, a navegação do topo fica escondida e
volta ao primeiro clique ou rolagem.

### Movimento reduzido

`prefers-reduced-motion: reduce` é tratado como requisito, não como enfeite: o
preloader some, o shader desenha um quadro e congela e os itens do pitch aparecem
sem transição. Rolando a página, todo bloco chega completo — ninguém recebe texto
invisível esperando um gatilho que não vai disparar.

## Notas de implementação

- **`fromTo`, nunca `from`, em efeito com limpeza.** Em desenvolvimento o React
  monta duas vezes; um `from` na segunda montagem lê como destino o estado inicial
  deixado pela primeira e anima de A para A.
- **Nada rola dentro de seção presa.** Uma área com `overflow: auto` dentro de um
  trecho pinado rouba a roda do mouse e trava a animação no meio.
- **O `GenericDao` do back-end tem paralelo aqui**: `Grafico` é a moldura única de
  todo gráfico — título, nota, alternância para tabela e legenda saem de um lugar
  só, e nenhum gráfico existe sem tabela equivalente.
- O painel do plano de ação desenha os botões de transição a partir de
  `transicoesPermitidas`, que a API já devolve pronto: a regra do ciclo de vida
  não é reimplementada no front.
- Em desenvolvimento, `window.__gsap` expõe `gsap` e `ScrollTrigger` para inspeção
  no console (`__gsap.ScrollTrigger.getAll()`); não existe no build de produção.
