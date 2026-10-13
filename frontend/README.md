# Hermes — Front-End

Interface da plataforma de *meeting intelligence*: uma página de apresentação com
scroll em camadas e o aplicativo que consome a API REST em `/api/v1`.

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

Descritor: **obsidiana com brasa — analytics editorial sobre papel quente**.

| | |
|---|---|
| Display | Instrument Serif (400 / 400 itálico) |
| Interface | Inter Tight Variable |
| Dados e códigos | JetBrains Mono Variable |
| Superfícies escuras | `#08090b` · `#0e1015` · `#12141a` · `#1a1e26` |
| Superfícies claras | `#f4f1ea` · `#eae6dc` · `#ded9cc` |
| Acento (brasa) | `#d9622c`, quente `#ff7a3d` |

As fontes são auto-hospedadas via `@fontsource`: o site abre igual sem rede e sem
CDN de terceiros. Marca, ícone e imagem de compartilhamento são SVG gerados em
`public/brand/` — não há bitmap no projeto.

### Paleta de dados

Seis slots, validados com o roteiro de cores do skill `dataviz` nos dois modos
(banda de luminosidade, piso de croma, separação para daltonismo, contraste
contra a superfície):

| Slot | Escuro | Claro |
|---|---|---|
| 1 brasa | `#d9622c` | `#c4551f` |
| 2 azure | `#3e87d8` | `#2b6fbf` |
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
  lib/          motion (GSAP + Lenis), api, types, demoData, format
  components/
    brand/      marca, logotipo, órbitas
    chrome/     preloader, cursor, navegação, trilho de índice, grão, modo
    fx/         campo de brasa (WebGL), campo de churn (canvas), faixa, primitivas
    viz/        rosca, ranking, tendência, moldura de gráfico
    app/        moldura das telas, selo, estados vazios
  sections/     as seções da home, na ordem em que aparecem
  routes/       landing, entrar, painel, reuniões, clientes, planos, importações
  styles/       tokens · base · app (home) · painel (aplicativo)
```

## O scroll em camadas

| Seção | O que acontece |
|---|---|
| Herói | Shader de brasa em WebGL, órbitas SVG, título dividido em linhas, cinco camadas com parallax de ponteiro; o scroll empurra a câmera para dentro |
| Léxico | Duas faixas infinitas cuja velocidade e inclinação vêm da velocidade do scroll |
| A escuta | Seção presa: a transcrição avança fala por fala, os trechos acendem e cada insight entra quando a frase que o originou é dita |
| As camadas | Quatro cartões empilhados com `position: sticky`; o GSAP só cuida do recuo (escala e brilho) de quem fica embaixo |
| A plataforma | Galeria horizontal presa, com contra-movimento interno entre texto, desenho e numeral |
| O risco | 1.126 partículas em canvas saem da nuvem e se agrupam em três colunas de risco conforme o scroll |
| A escala | Contadores e gráficos que se desenham ao entrar |
| O painel | Uma lâmina de papel abre por `clip-path` e a página troca de material — do escuro para o claro, definitivamente |

### Movimento reduzido

`prefers-reduced-motion: reduce` é tratado como requisito, não como enfeite: o
preloader some, o shader desenha um quadro e congela, nada fica preso, a galeria
horizontal vira uma pilha vertical e todo conteúdo nasce no estado final. Ninguém
recebe texto invisível esperando um gatilho que não vai disparar.

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
