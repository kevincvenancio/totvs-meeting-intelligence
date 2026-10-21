# Hermes IA — Sprint 4 (Data Science and Statistical Computing)

Solução que recebe **uma transcrição nova** e devolve automaticamente o que interessa à área
comercial, usando modelos pré-treinados do **Hugging Face**:

```
Transcrição → Processamento → Modelo → Indicadores → Recomendação
```

| Saída pedida | Onde aparece no JSON |
|---|---|
| Produto identificado | `produtos_identificados`, `produto_principal` |
| Sentimento | `sentimento` (rótulo + score de −1 a +1) |
| Risco de churn | `risco_churn` (nível + score 0–100 + trecho que justifica) |
| Oportunidade comercial | `oportunidade_comercial` (nível + score 0–100 + orçamento citado + venda cruzada) |
| Principais termos encontrados | `termos_principais` |
| Recomendação de ação | `recomendacoes` (ação, prioridade, prazo, responsável, motivo) |

## Como rodar

```bash
cd ciencia-de-dados
python -m venv .venv
.venv\Scripts\activate            # Windows  (Linux/macOS: source .venv/bin/activate)
pip install -r requirements.txt

python app_gradio.py              # protótipo em http://localhost:7860
python validacao/validar.py       # validação com as 20 transcrições novas (os 3 modos)
python analisar.py reuniao.txt    # linha de comando
uvicorn api:app --port 8000       # API HTTP (Swagger em /docs)
pytest -q                         # testes (rodam sem baixar modelo)
```

Na primeira análise os pesos dos modelos são baixados para `~/.cache/huggingface`
(~0,5 GB do zero-shot + ~0,5 GB do sentimento). Depois disso tudo roda offline, em CPU.
O perfil padrão é o **rápido** (MiniLMv2); `--perfil preciso` (ou a caixa no protótipo) troca
pelo mDeBERTa-v3, ~10× mais lento — ver [Velocidade](#velocidade).

O modo `heuristico` não precisa de `torch` nem de internet — útil para demonstrar a interface
sem baixar nada.

## Estrutura

```
ciencia-de-dados/
├── hermes_ia/
│   ├── config.py          modelos, hipóteses do zero-shot, limiares (todas as decisões num lugar)
│   ├── processamento.py   1. limpeza, locutores, trechos, seleção de trechos, termos principais
│   ├── lexico.py             regras do motor Java (baseline e sinais de alta precisão)
│   ├── modelos.py         2. wrappers dos pipelines do Hugging Face
│   ├── indicadores.py     3. scoring (churn, oportunidade, sentimento, produto, saúde do cliente)
│   ├── calibracao.py         regras de decisão calibradas (as mesmas usadas por calibrar.py)
│   ├── calibracao.json       pesos e limiares aprendidos, um conjunto por modelo zero-shot
│   ├── recomendacao.py    4. motor de recomendações
│   └── pipeline.py           orquestra as etapas → HermesPipeline().analisar(texto)
├── app_gradio.py          protótipo funcional (interface web)
├── api.py                 API FastAPI para o back-end Java consumir
├── analisar.py            linha de comando (.txt ou CSV no formato da importação)
├── validacao/
│   ├── novas_transcricoes.jsonl   20 transcrições novas rotuladas à mão
│   ├── diagnostico.py             exporta as probabilidades brutas do modelo
│   ├── calibrar.py                aprende pesos e limiares com validação cruzada (LOOCV)
│   └── validar.py                 baseline × modelo × híbrido, com métricas
└── tests/test_pipeline.py
```

---

## 1. Pipeline completo de processamento (15 pts)

**Processamento** (`processamento.py`)

1. **Normalização** — corrige a acentuação corrompida que existia na base (`gestÃ£o` → `gestão`),
   normaliza Unicode (NFC), quebras de linha e espaços; lê o cabeçalho opcional entre `---`
   (mesmo formato aceito pela importação de `.txt`/`.docx` do back-end).
2. **Locutores** — aceita `[LOCUTOR n]:` (formato da base TOTVS) e `Nome: fala`. Infere quem é
   TOTVS e quem é cliente pelo nome ("Vendedor", "Cliente") ou pelo vocabulário ("vou te mostrar",
   "nossa solução" × "nossa empresa", "hoje a gente usa"). A fala do cliente pesa 1,5× no sentimento.
3. **Trechos** — os modelos aceitam até 512 tokens, e há transcrições de 188 mil caracteres na base.
   As falas são agrupadas em trechos de até 90 palavras (≈ 150 tokens), quebrando por sentença.
4. **Seleção de trechos** — o custo por reunião é fixo, não importa o tamanho da transcrição:
   8 trechos (metade os de maior densidade de sinais comerciais — contrato, proposta, concorrente,
   dores — e metade espaçados ao longo da reunião) passam pelas 4 hipóteses de churn e pelas 4 de
   oportunidade, e 8 trechos falados pelo cliente passam pelo sentimento. São no máximo 64 pares no
   zero-shot e 8 no sentimento por reunião.
5. **Termos principais** — unigramas e bigramas sem stopwords (incluindo muletas de fala como "né",
   "tá", "então"); bigramas valem 2,5× porque carregam mais sentido ("folha pagamento" > "folha").

**Modelo** (`modelos.py`) → **Indicadores** (`indicadores.py`) → **Recomendação** (`recomendacao.py`),
descritos abaixo.

## 2. Modelo final selecionado e justificativa (15 pts)

**Modelo final: híbrido = dois modelos do Hugging Face + regras de alta precisão.**

| Tarefa | Modelo (Hugging Face) | Tipo |
|---|---|---|
| Sentimento | [`lxyuan/distilbert-base-multilingual-cased-sentiments-student`](https://huggingface.co/lxyuan/distilbert-base-multilingual-cased-sentiments-student) | classificação de texto (pos/neu/neg) |
| Churn, oportunidade, negociação, concorrência, produto | [`MoritzLaurer/multilingual-MiniLMv2-L6-mnli-xnli`](https://huggingface.co/MoritzLaurer/multilingual-MiniLMv2-L6-mnli-xnli) (padrão) ou [`MoritzLaurer/mDeBERTa-v3-base-mnli-xnli`](https://huggingface.co/MoritzLaurer/mDeBERTa-v3-base-mnli-xnli) (perfil preciso) | zero-shot (NLI) |

**Por que zero-shot (NLI) e não um classificador treinado?**
A base tem 1.126 reuniões, mas **nenhuma tem rótulo humano** de churn ou oportunidade — os rótulos
existentes foram produzidos pelas próprias regras, e treinar em cima deles só ensinaria o modelo a
copiar as regras (com os mesmos erros). Um modelo NLI responde "o trecho implica a hipótese
*o cliente quer cancelar o contrato*?" sem precisar de exemplos rotulados, e as hipóteses são
escritas em português, legíveis e fáceis de ajustar (`config.py`).

**Por que esses dois modelos?**

| Critério | Zero-shot: MiniLMv2-L6 XNLI (padrão) | Zero-shot: mDeBERTa-v3 XNLI | Sentimento: DistilBERT multilíngue |
|---|---|---|---|
| Português | XNLI (15 idiomas) + MNLI | XNLI (15 idiomas) + MNLI | destilado de um professor multilíngue |
| Tamanho | 6 camadas de 384 | 12 camadas de 768 | 6 camadas de 768 |
| Custo por par em CPU (int8, medido) | ~10–17 ms | ~130–370 ms | ~35–70 ms |
| Alternativas descartadas | `facebook/bart-large-mnli` (só inglês); LLM generativo (custo por chamada, saída não calibrada, dado do cliente sai da empresa) | — | modelos treinados em tweets (linguagem muito diferente de reunião B2B) |

**Por que o MiniLM e não o mDeBERTa como padrão?** É o modelo que analisa **todas** as reuniões,
então a velocidade decide. Com o mDeBERTa a base inteira levaria ~5 horas numa CPU de 2 núcleos;
com o MiniLM, ~30 minutos. O mDeBERTa é mais preciso no XNLI e continua disponível no perfil
`preciso` — a validação (`validar.py --perfil preciso`) mostra se a diferença compensa.

**Por que híbrido e não só o modelo?**
As regras do back-end Java (o "modelo" anterior, usado como baseline) têm **alta precisão e baixa
cobertura**: quando o cliente diz "não vamos renovar", não há dúvida; mas "o jurídico está vendo a
multa para encerrar o relacionamento" passa despercebido. O modelo cobre o segundo caso e também
corrige o falso positivo mais conhecido das regras (README do projeto, "Limitação conhecida"):
um *prospect* dizendo que vai **cancelar o contrato com o fornecedor atual** para vir para a TOTVS.

**Resultado da seleção (detalhes na seção 5):** o modelo final é o **híbrido calibrado** com o
MiniLMv2. Em validação cruzada ele supera as regras nas três tarefas — churn 0,70 → 0,75,
oportunidade 0,50 → 0,80, sentimento 0,75 → 0,80 —, enquanto o modelo sozinho perde para as
regras em sentimento (0,40) e não ganha em F1 no churn. Ou seja: nem as regras sozinhas nem o
modelo sozinho; a combinação dos dois.

**Como o modelo é usado (aprendido na validação):**

- **Hipóteses em contraste.** A pergunta isolada "o cliente quer cancelar?" não separa as classes
  (AUC 0,41–0,60, praticamente acaso). Obrigar o modelo a escolher entre opções concorrentes — "quer
  cancelar", "reclama de um problema", "está satisfeito", "assunto técnico" (softmax) — separa:
  AUC **0,86** para churn ALTO. O mesmo para oportunidade ("discutem preço/proposta": AUC 0,83).
- **Churn:** as regras dão o nível (frase explícita = ALTO, insatisfação/concorrente = MÉDIO) e o
  modelo **sobe um nível** quando vê insatisfação forte ou **desce um** quando a regra disparou mas
  não há insatisfação na conversa (o prospect do VAL-03). Formulação fixada por desenho: com só 3
  exemplos de churn ALTO, escolher a fórmula pelo próprio treino só troca de vencedor a cada rodada.
- **Oportunidade e sentimento:** pontuação = sinais do modelo + peso × sinais das regras, com
  dois limiares; pesos e limiares calibrados por validação cruzada (`validacao/calibrar.py`).
- **Produto:** pelo nome citado e pelo contexto da conversa. A inferência pelo modelo foi
  **desligada** (`inferir_produto_modelo=False`): acertou o produto em só 6 de 19 reuniões.

### Velocidade

Medido no pipeline completo (processamento + modelos + indicadores), numa CPU de **2 núcleos**,
com modelos de arquitetura e tamanho idênticos aos do Hugging Face (o tempo de um transformer
depende da arquitetura, não dos pesos):

| Perfil | Reunião curta (validação) | Reunião longa (~12 mil palavras, média da base) | Base inteira (1.126 reuniões) |
|---|---|---|---|
| **rápido** (MiniLMv2, padrão) | 0,46 s | 1,6 s | **~30 min** |
| preciso (mDeBERTa-v3) | 5,3 s | 16 s | ~5 h |

Em uma CPU de 4 a 8 núcleos os tempos caem proporcionalmente. O que tornou isso possível:

1. **Modelo menor** no zero-shot (MiniLMv2: 6 camadas de 384 contra 12 de 768): ~13× mais rápido.
2. **Custo fixo por reunião**: no máximo 64 pares no zero-shot, não importa o tamanho da transcrição
   (a primeira versão fazia 272 pares de 512 tokens: ~2 min por reunião, dezenas de horas na base).
3. **Trechos de 150 tokens** em vez de 250 (o custo do transformer cresce mais que linearmente).
4. **Quantização int8** das camadas lineares em CPU: ~1,5× mais rápido.
5. **Lote**: várias reuniões passam pelo modelo juntas (`/analisar-lote`).
6. **Fila em segundo plano no back-end**: a importação termina na hora e o modelo analisa a base
   enquanto o sistema está em uso.

## 3. Sistema de scoring / indicadores (20 pts)

Todos os scores vão de 0 a 100 e vêm com a **evidência** (trecho da transcrição + probabilidade do modelo).

Sinais do modelo (formulação em contraste, máximo entre os trechos avaliados):
`ins` = P("o cliente reclama de um problema"), `neg` = P("discutem preço, proposta ou orçamento"),
`int` = P("interesse em contratar ou ampliar"), `saldo` = média de P(positivo) − P(negativo) na fala
do cliente. Sinais das regras: frases explícitas, orçamento em R$, concorrentes, expressões positivas
e negativas. Pesos e limiares em `hermes_ia/calibracao.json` (um conjunto por modelo zero-shot).

| Indicador | Nível | Nota 0–100 |
|---|---|---|
| **Risco de churn** | nível das regras (explícito = ALTO; insatisfação/concorrente = MÉDIO); o modelo sobe um nível se `ins ≥ limiar_sobe` ou desce um se a regra disparou e `ins < limiar_desce` | 15 / 50 / 82 por nível + até 15 conforme `ins` |
| **Oportunidade** | `s = w₁·neg + w₂·int + w₃·regras`; BAIXA < limiar médio ≤ MÉDIA < limiar alto ≤ ALTA | linear por partes: limiar médio → 35, alto → 65 |
| **Sentimento** | `s = saldo + w·regras`; NEGATIVO < limiar médio ≤ NEUTRO < limiar alto ≤ POSITIVO; MISTO quando neutro com expressões positivas e negativas | mesma escala, reescalada para −1…+1 |
| **Produto** | citado pelo nome (0,99) ou pelo contexto da conversa (0,6) | — |
| **Saúde do cliente** | `0,5·(100 − churn) + 0,3·sentimento(0–100) + 0,2·oportunidade` | — |
| **Prioridade** | Alta se churn ALTO ou oportunidade ALTA; Média se algum MÉDIO ou sentimento negativo | Alta · Média · Baixa |

O nível é decidido pela calibração; a nota acompanha o nível para a tela ordenar e comparar
reuniões (as faixas 35 e 65 coincidem com os limiares calibrados).

O campo `indicadores.sentimento_hermes` devolve o mesmo enum do back-end Java
(`CRITICO`, `OPORTUNIDADE_COMERCIAL`, `POSITIVO`…), para gravar direto na tabela `reunioes`.

## 4. Protótipo funcional (20 pts)

**Integrado ao sistema Hermes.** O back-end Java chama esta API (`uvicorn api:app --port 8000`)
em toda análise de reunião — importação de CSV/.txt/.docx, cadastro manual e reanálise — e grava
no banco os scores, os termos, a recomendação e o JSON completo. No front-end:

- **tela "Análise IA"** (`/analise-ia`): cola uma transcrição nova e vê o resultado na hora;
- **detalhe da reunião**: bloco "Análise de IA (Hugging Face)" com saúde do cliente, churn,
  oportunidade, produto, termos, trechos que justificam e ações recomendadas;
- **insights** da reunião ganham os tipos "Risco de churn (IA)", "Oportunidade comercial (IA)" e
  "Produto identificado (IA)", com a probabilidade do modelo como confiança.

Se o serviço Python estiver fora, o Java segue só com as regras (fallback). Passo a passo em
[`COMO-RODAR.md`](../COMO-RODAR.md#ia-hermes-modelos-do-hugging-face--terminal-3).

Para demonstrar só a parte de ciência de dados, sem subir o Java:

`python app_gradio.py` abre uma página onde se cola (ou envia em `.txt`) uma transcrição e se
escolhe o modo. A tela mostra os cartões de saúde do cliente, produto, sentimento, churn (com o trecho
que justificou), oportunidade, termos e a tabela de ações; abaixo, o JSON completo. As 8 primeiras
transcrições de validação aparecem como exemplos clicáveis. `--share` gera um link público
temporário para a apresentação.

Também há `api.py` (FastAPI, `POST /analisar`) para o back-end Java chamar, e `analisar.py`
para processar em lote o CSV no mesmo formato da importação do sistema (`ANON_TRANSCRICAO`).

## 5. Validação com novas transcrições (15 pts)

`validacao/novas_transcricoes.jsonl` traz **20 transcrições novas** (escritas para esta sprint,
fora da base de 1.126) com rótulos humanos de churn, oportunidade, sentimento e produto. Elas
cobrem de propósito os casos difíceis:

- churn **explícito** (VAL-01) e **implícito**, sem nenhuma frase do léxico (VAL-02, VAL-13);
- o **falso positivo** do léxico: prospect cancelando o fornecedor atual (VAL-03);
- produto **sem o nome** do produto (ponto eletrônico, folha, PDV, prontuário);
- concorrente citado com cliente satisfeito (VAL-18) e reunião interrompida (VAL-11).

### 5.1 Primeira validação: o modelo sem calibração perdeu para as regras

Com os modelos reais (MiniLMv2 + DistilBERT) e os limiares definidos "no olho", o resultado foi:

| Métrica | Regras | Modelo | Híbrido |
|---|---|---|---|
| Churn — acurácia | **0,70** | 0,10 | 0,10 |
| Churn — F1-macro | **0,53** | 0,10 | 0,12 |
| Oportunidade — acurácia | **0,50** | 0,35 | 0,30 |
| Sentimento — acurácia | **0,60** | 0,35 | 0,35 |
| Produto — F1 | **0,54** | 0,36 | 0,36 |
| Tempo por reunião (s) | 0,00 | 0,57 | 0,54 |

O modelo marcava churn em quase todas as reuniões. Este é o resultado mais importante da sprint:
**o modelo pré-treinado não funciona "de fábrica" neste domínio.**

### 5.2 Diagnóstico

`validacao/diagnostico.py` exportou as probabilidades brutas de cada reunião em duas formulações,
e medimos quanto cada sinal separa as classes (AUC: 0,5 = acaso, 1,0 = perfeito):

| Sinal | O que mede | AUC |
|---|---|---|
| P("quer cancelar") — hipótese isolada | churn ALTO × resto | 0,41 (acaso) |
| P("quer cancelar") — em contraste | churn ALTO × resto | 0,47 (acaso) |
| **P("reclama de um problema") — em contraste** | churn ALTO × resto | **0,86** |
| **P("discutem preço/proposta") — em contraste** | oportunidade ALTA × resto | **0,83** |
| **saldo positivo − negativo na fala do cliente** | sentimento positivo × negativo | **0,90** |
| produto mais provável (12 hipóteses) | acerta o produto | 6 de 19 |

Conclusões: (1) o modelo pequeno não entende "cancelar o contrato" como intenção, mas reconhece
**reclamação** — que é o que acompanha o churn; (2) as hipóteses isoladas saturam (respondem "sim"
para tudo), as em contraste não; (3) o sentimento tem sinal forte, mas deslocado para o negativo —
o limiar fixo em ±0,2 estava errado; (4) produto pelo modelo não compensa.

### 5.3 Calibração com validação cruzada

`validacao/calibrar.py` aprende pesos e limiares a partir do diagnóstico. Para não "decorar" as
20 transcrições, a medida é **leave-one-out**: cada reunião é avaliada por uma calibração feita
nas outras 19. Acurácia / F1-macro (sentimento em 3 classes, MISTO = NEUTRO):

| Tarefa | Regras | Modelo calibrado | **Híbrido calibrado** |
|---|---|---|---|
| Churn | 0,70 / 0,53 | 0,75 / 0,45 | **0,75 / 0,57** |
| Oportunidade | 0,50 / 0,40 | 0,60 / 0,60 | **0,80 / 0,81** |
| Sentimento | 0,75 / 0,71 | 0,40 / 0,32 | **0,80 / 0,77** |

O híbrido calibrado é o melhor nas três tarefas e é o **modelo final**. O recall de churn ALTO
continua em 0,33 (1 de 3) nos três: o churn implícito (VAL-02, VAL-13) sobe para MÉDIO — gera
alerta e ação de Customer Success —, mas não chega a ALTO sem uma frase explícita.

### 5.4 Resultado final do pipeline (modelos reais, depois da calibração)

Saída de `python validacao/validar.py` com MiniLMv2 + DistilBERT. As colunas `modelo` e `hibrido`
são otimistas (limiares calibrados nestas mesmas 20 transcrições; sentimento em 4 classes) — a
medida honesta é a tabela LOOCV da seção 5.3, que o próprio relatório repete.

| Métrica | Regras | Modelo | Híbrido |
|---|---|---|---|
| Churn — acurácia | 0,70 | **0,80** | 0,75 |
| Churn — F1-macro | 0,53 | **0,62** | 0,57 |
| Churn ALTO — recall | 0,33 | 0,33 | 0,33 |
| Churn ALTO — precisão | 0,50 | **1,00** | **1,00** |
| Oportunidade — acurácia | 0,50 | 0,70 | **0,90** |
| Oportunidade — F1-macro | 0,40 | 0,70 | **0,90** |
| Sentimento — acurácia | 0,60 | 0,55 | **0,70** |
| Sentimento — F1-macro | 0,47 | 0,40 | **0,53** |
| Produto — F1 | 0,54 | 0,54 | 0,54 |
| Tempo por reunião (s) | 0,00 | 0,20 | 0,20 |

Leitura: o pipeline executa exatamente as regras calibradas (a coluna LOOCV bate com a da
calibração); o churn ALTO passou a ter **precisão 1,00** — o falso alarme do VAL-03 (prospect
cancelando o fornecedor atual) sumiu —, e a oportunidade foi o maior ganho. Em produto não há
ganho: a inferência pelo modelo foi desligada e o resultado é o das regras.

**Para reproduzir:**

```powershell
python validacao/diagnostico.py                     # ~1 min: probabilidades brutas
python validacao/calibrar.py validacao/resultados/diagnostico-rapido.csv
python validacao/validar.py                         # tabela final + LOOCV
```

Para comparar com o modelo maior, troque `rapido` por `preciso` (mDeBERTa-v3) nos dois primeiros
comandos e rode `validar.py --perfil preciso`.

> Para validar com dados reais: rotule 30–50 reuniões da base em um CSV
> (`id;transcricao;churn;oportunidade;sentimento;produtos`) e rode
> `python validacao/validar.py --arquivo amostra.csv`.

## 6. Recomendações de negócio (10 pts)

O motor (`recomendacao.py`) é um conjunto de regras explícitas sobre os indicadores — cada ação
sai com **prioridade, prazo, responsável e motivo**, para o vendedor confiar e agir:

| Situação | Ação | Prazo | Responsável |
|---|---|---|---|
| Churn ALTO | Acionar Customer Success e gerente da conta com plano de retenção | 24 h | CS |
| Churn MÉDIO | Health check com o decisor | 7 dias | CS |
| Concorrente citado | Comparativo de diferenciais TOTVS × concorrente | 5 dias | Pré-vendas |
| Reclamação de suporte | Escalonar chamados e dar retorno formal | 48 h | Suporte |
| Oportunidade ALTA com orçamento | Enviar proposta do produto principal | 48 h | Vendedor |
| Oportunidade ALTA sem orçamento | Qualificar BANT e preparar proposta | 3 dias | Vendedor |
| Oportunidade MÉDIA | Demonstração focada no produto identificado | 7 dias | Pré-vendas |
| Sinal de venda cruzada | Apresentar o produto complementar | 15 dias | Vendedor |
| Sentimento positivo e churn baixo | Pedir indicação / case de sucesso | 30 dias | Marketing / CS |

Recomendações para a TOTVS a partir da solução:

1. **Priorizar a carteira pelo score de churn**, e não pela ordem de chegada das reuniões: o CS
   começa o dia pelas contas com churn ≥ 65.
2. **Usar o produto inferido para venda cruzada**: o cliente descreve a dor ("o RH confere hora
   extra no caderno") sem citar o produto; o modelo liga a dor ao produto (TOTVS Agora).
3. **Medir o tempo entre o alerta de churn e o primeiro contato do CS** como indicador de
   processo — o alerta só vale se alguém agir em 24 h.
4. **Coletar o rótulo do vendedor** (concorda / discorda do alerta) na tela da reunião: em poucos
   meses isso vira a base rotulada que hoje não existe, e permite o fine-tuning (próximos passos).

## 7. Limitações e próximos passos (5 pts)

**Limitações**

- **Validação pequena e sintética**: 20 transcrições escritas para a sprint. Dão sinal de direção,
  não intervalo de confiança apertado (com n = 20, cada erro muda a acurácia em 5 p.p.).
- **Zero-shot não é calibrado**: as probabilidades do NLI não são frequências reais; os limiares
  (65/35, 0,80) foram definidos por julgamento e precisam ser recalibrados com dados reais.
- **Modelos genéricos**: nenhum dos dois viu reuniões de venda B2B; ironia e linguagem muito
  informal da transcrição automática derrubam o sentimento.
- **Cobertura parcial de reuniões longas**: só 8 trechos passam pelas intenções (o preço de um custo
  fixo por reunião); um sinal de churn isolado no meio de uma reunião de 2 horas pode ficar de fora
  se não tiver nenhuma palavra-chave. As regras ainda cobrem as frases explícitas.
- **Modelo rápido é menos preciso**: o MiniLMv2 erra mais que o mDeBERTa no XNLI; foi escolhido
  porque precisa analisar a base inteira em CPU.
- **Calibração com 20 exemplos**: só 3 reuniões de churn ALTO e 3 de MÉDIO. Os limiares são
  instáveis (mudam conforme a reunião que sai na validação cruzada) e o intervalo de confiança é
  largo — com n = 20, cada acerto vale 5 pontos percentuais.
- **Viés do conjunto de validação**: as 20 transcrições e o léxico de frases de interesse foram
  escritos na mesma sprint; o sinal "frase de interesse" pode estar otimista neste conjunto.
- **Churn implícito vai até MÉDIO, não ALTO**: o modelo sobe só um nível sobre as regras.
- **Papel do locutor é heurístico**: se errar quem é o cliente, o peso 1,5× vai para a pessoa errada.

**Próximos passos**

1. Rotular 300–500 reuniões reais (feedback do vendedor na tela), **recalibrar** com
   `calibrar.py` e, com dados suficientes, fazer **fine-tuning** de um classificador de churn em
   português (ex.: BERTimbau `neuralmind/bert-base-portuguese-cased`).
2. Com mais dados rotulados, trocar os limiares por **regressão logística** (Platt) sobre os sinais
   e escolher o ponto de corte pela curva precisão × recall,
   priorizando recall de churn ALTO.
3. **Hospedar a IA** num serviço com ≥ 2 GB de memória (ou GPU) para ligá-la também em produção —
   hoje ela roda local, porque o plano gratuito do Render tem 512 MB.
4. Trocar a seleção de trechos por **embeddings** (busca semântica dos trechos mais relevantes).
5. Monitorar **drift**: distribuição dos scores por mês e taxa de discordância do vendedor.