# TOTVS Meeting Intelligence — Hermes

Sistema de inteligência comercial para análise automática de transcrições de reuniões TOTVS.
Importa arquivos CSV com transcrições, classifica completude, extrai sentimento, risco de churn e
oportunidades de venda cruzada, gera relatórios executivos em PDF e — a partir da versão 3.0 —
expõe toda a plataforma por uma **API RESTful** e permite o **acompanhamento comercial** das
reuniões (clientes, planos de ação e comentários).

## Acesse online

| | Endereço |
|---|---|
| **Aplicação** | https://totvs-meeting-intelligence.vercel.app |
| **API REST** | https://hermes-api-vn8l.onrender.com/api/v1 |
| **Documentação da API (Swagger UI)** | https://hermes-api-vn8l.onrender.com/swagger-ui.html |

Login de demonstração: **admin@hermes.com.br / admin123**. A navegação pelas telas não exige login.

A base tem as **1.126 reuniões reais** (anonimizadas) do lote importado e, para simular um
ambiente de produção, uma carteira de **22 clientes fictícios** com reuniões vinculadas,
28 planos de ação e comentários de uma equipe comercial fictícia.

## Hospedagem

Tudo roda em planos gratuitos, com deploy automático a cada push na `main`:

```
Navegador ──▶ Vercel (front-end React)
                 │  /api/*  (proxy, mesma origem — sem CORS)
                 ▼
              Render (API Spring Boot em Docker) ──▶ Oracle da FIAP
                 ▲
              UptimeRobot (ping a cada 5 min — mantém a API acordada)
```

| Peça | Serviço | Configuração no repositório |
|------|---------|-----------------------------|
| Front-end | **Vercel** (Root Directory `frontend`) | [`frontend/vercel.json`](frontend/vercel.json): encaminha `/api/*` para o Render e devolve `index.html` nas rotas do React |
| API | **Render**, Web Service gratuito com runtime Docker (região Virginia) | [`Dockerfile`](Dockerfile) multi-stage e [`render.yaml`](render.yaml) |
| Banco | **Oracle da FIAP** (`oracle.fiap.com.br:1521/ORCL`) | Credenciais só em variáveis de ambiente do Render (`ORACLE_USUARIO`, `ORACLE_SENHA`) |
| Monitor | **UptimeRobot**, HTTP a cada 5 min em `/v3/api-docs` | — |

Como o front chama a API por caminho relativo (`/api/v1`), o mesmo código funciona no
`localhost` (proxy do Vite) e em produção (rewrite da Vercel).

**Limites do plano gratuito, e como foram tratados:**

| Limite | Tratamento |
|--------|------------|
| O Render desliga o serviço após 15 min sem acesso, e a API leva cerca de 1,5 min para voltar | O UptimeRobot acessa a API a cada 5 min. As 750 h/mês do plano cobrem um serviço ligado 24 h |
| 512 MB de RAM no total | Heap fixo em 256 MB, metaspace e code cache limitados, 20 threads no Tomcat (`JAVA_TOOL_OPTIONS` e `SERVER_TOMCAT_THREADS_MAX` no Render) |
| CPU fraca (cerca de 0,1 vCPU) | JIT só no nível 1 (`-XX:TieredStopAtLevel=1`), o que acelera a inicialização |
| Banco no Brasil, API nos EUA | O painel lê só as colunas que agrega, sem os CLOBs; prefetch de LOB e `fetch_size` maiores no driver |

## Ciência de dados — Sprint 4 (modelo do Hugging Face)

A pasta [`ciencia-de-dados/`](ciencia-de-dados/) tem o pipeline
**Transcrição → Processamento → Modelo → Indicadores → Recomendação** em Python, com dois modelos
do Hugging Face (sentimento `lxyuan/distilbert-base-multilingual-cased-sentiments-student` e
zero-shot `MoritzLaurer/multilingual-MiniLMv2-L6-mnli-xnli`) combinados com as regras deste back-end.
Recebe uma transcrição nova e devolve produto identificado, sentimento, risco de churn, oportunidade
comercial, termos principais e recomendações de ação. Detalhes do modelo, scoring e validação em
[`ciencia-de-dados/README.md`](ciencia-de-dados/README.md).

**Acoplado ao sistema:**

```
Front (React) ──▶ API Java ──▶ AnaliseTranscricaoService ──regras──▶ análise base
   /analise-ia        │                    │
                      │                    └──HermesIaClient──▶ IA Python (FastAPI :8000)
                      └── /api/v1/ia/*        FilaAnaliseIa ──┘    Hugging Face: DistilBERT + MiniLMv2
```

| Peça | Arquivo |
|------|---------|
| Cliente HTTP da IA (timeout, fallback para as regras) | `ia/HermesIaClient.java` |
| Grava o resultado da IA na reunião + insights com as evidências | `ia/AplicadorAnaliseIa.java` |
| Endpoints `GET /api/v1/ia/status` e `POST /api/v1/ia/analises` | `ia/IaController.java` |
| Chamada na análise de cada reunião (cadastro e reanálise: na hora) | `service/AnaliseTranscricaoService.java` |
| Fila em segundo plano: importação e "analisar a base" (lotes de 8, tenta de novo se a IA cair) | `ia/FilaAnaliseIa.java` |
| Colunas novas em `reunioes`: `origem_analise`, `score_churn`, `score_oportunidade`, `saude_cliente`, `termos_principais`, `analise_ia` | `model/Reuniao.java`, `docs/ddl-oracle.sql` |
| Tela "Análise IA" e bloco de IA no detalhe da reunião | `frontend/src/routes/AnaliseIa.tsx`, `components/ia/PainelAnaliseIa.tsx` |

A IA liga com `HERMES_IA_URL=http://localhost:8000` (variável de ambiente ou
`application-local.properties`). Sem ela, ou se o serviço Python cair, a análise continua só
com as regras — nada quebra. Passo a passo em [`COMO-RODAR.md`](COMO-RODAR.md#ia-hermes-modelos-do-hugging-face--terminal-3).
No Render a IA fica desligada: os modelos precisam de ~2 GB de memória e o plano gratuito tem 512 MB.

## Atualizações recentes (deploy e qualidade da análise)

| Entrega | Descrição |
|---------|-----------|
| **Deploy em produção** | Vercel + Render + Oracle FIAP, com deploy automático pela `main` (ver [Hospedagem](#hospedagem)) |
| **Credenciais fora do repositório** | `application.properties` não tem mais usuário e senha do Oracle; eles vêm de variáveis de ambiente ou de um `application-local.properties` ignorado pelo Git |
| **Painel mais leve** | Os indicadores saem de uma projeção sem CLOBs, e só as 10 reuniões dos rankings são carregadas inteiras: de ~40 s para ~4 s em produção e de ~400 MB para ~260 MB de memória |
| **Regra de budget** | Um número só conta como orçamento com "R$", com "reais" depois ou com uma palavra financeira antes. "3 mil colaboradores" deixou de virar "Valor financeiro mencionado", e "3 milhões" não é mais truncado em "3 mil" |
| **Regra de churn** | O risco alto exige uma expressão sobre o contrato ("cancelamento do contrato", "não vamos renovar", "trocar de fornecedor"). Palavras soltas como "rescisão" e "cancelamento" marcavam churn em demonstrações da folha de pagamento |
| **Acentuação das transcrições** | As transcrições e os trechos de insight tinham sido gravados com a codificação trocada ("gestÃ£o"); foram corrigidos na base, e a base foi reanalisada com as regras novas |
| **Correções** | Os cards de cliente mostram o total de reuniões; "Ver reuniões" filtra pelo cliente; mudar o status de um plano pela tela voltou a funcionar; reanalisar uma reunião não falha mais com `ORA-00001` |

## Novidades da versão 3.0

| Entrega | Descrição |
|---------|-----------|
| **Banco Oracle (FIAP)** | Persistência padrão no Oracle 19c da FIAP; H2 permanece disponível no perfil `h2` |
| **Migração de dados** | Ferramenta que copia a base já analisada do H2 para o Oracle preservando ids e chaves estrangeiras |
| **API RESTful** | 31 recursos / 44 operações em `/api/v1`, documentadas com OpenAPI 3 (Swagger UI) |
| **Módulo de acompanhamento comercial** | Novas entidades `Usuario`, `Cliente`, `PlanoAcao` e `ComentarioReuniao` com CRUD completo |
| **Camada DAO** | DAO genérico (`GenericDao` + `AbstractJpaDao`) com consultas dinâmicas e paginadas |
| **Camada de serviço com validações** | Bean Validation nos DTOs de entrada + regras de negócio nos serviços |
| **Tratamento centralizado de exceções** | `@RestControllerAdvice` traduzindo exceções de domínio em respostas HTTP padronizadas |
| **CLI preservada** | O menu de linha de comando continua funcionando, agora sob o perfil `cli` |
| **Front-End** | Aplicação React + Vite em `frontend/`, consumindo a API, com painel, grade de reuniões, carteira, quadro de planos e importação |

## Pré-requisitos

- Java 17 ou superior
- Acesso ao Oracle da FIAP (`oracle.fiap.com.br:1521/ORCL`) — ou use o perfil `h2` para trabalhar offline

Maven não precisa estar instalado: o projeto traz o **Maven Wrapper** (`mvnw`), que baixa a
versão correta (3.9.16) na primeira execução. Nos comandos abaixo, use `./mvnw` no Linux, macOS
e Git Bash; no PowerShell, `.\mvnw`; no `cmd`, `mvnw`. Quem já tem o Maven no `PATH` pode
trocar por `mvn` que o resultado é o mesmo.

> Passo a passo detalhado para quem vai rodar o projeto pela primeira vez (instalação dos
> pré-requisitos, ordem dos comandos nos dois terminais e problemas comuns):
> [`COMO-RODAR.md`](COMO-RODAR.md).

## Como executar

### API REST + Oracle (padrão)

Antes da primeira execução, configure usuário e senha do Oracle (ver
[Conexão com o Oracle](#conexão-com-o-oracle)).

```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`:

| Recurso | Endereço |
|---------|----------|
| Documentação interativa (Swagger UI) | http://localhost:8080/swagger-ui.html |
| Especificação OpenAPI (JSON) | http://localhost:8080/v3/api-docs |

No primeiro start, se não houver nenhum usuário cadastrado, é criado o administrador padrão
**admin@hermes.com.br / admin123** (troque a senha em `PATCH /api/v1/usuarios/{id}/senha`).

### Front-End

A interface fica em [`frontend/`](frontend/) — página de apresentação com scroll em
camadas e o aplicativo que consome a API.

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

O Vite faz proxy de `/api` para `localhost:8080`, então a API acima já é consumida
sem configuração. Se o back-end não estiver no ar, o front cai num conjunto de
demonstração e avisa em tela — útil para trabalhar a interface sem depender do
Oracle da FIAP. Detalhes de identidade visual, paleta e arquitetura em
[`frontend/README.md`](frontend/README.md).

### Perfis disponíveis

| Comando | Banco | Interface |
|---------|-------|-----------|
| `./mvnw spring-boot:run` | Oracle FIAP | API REST |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=h2` | H2 em arquivo (`./data`) | API REST + console H2 |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=cli` | Oracle FIAP | Menu de linha de comando |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=cli,h2` | H2 em arquivo | Menu de linha de comando |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=migracao` | H2 → Oracle | Copia os dados e encerra |

> No PowerShell o argumento precisa de aspas: `.\mvnw spring-boot:run "-Dspring-boot.run.profiles=h2"`.

No perfil `cli` o servidor web não é iniciado; no perfil `h2` o console fica em
http://localhost:8080/h2-console (JDBC `jdbc:h2:file:./data/hermes`, usuário `sa`, sem senha).

## Banco de dados

### Conexão com o Oracle

O endereço do banco fica em `application.properties`, mas **usuário e senha não estão no
repositório**. Informe-os de um destes dois jeitos:

1. **Arquivo local (recomendado para desenvolvimento).** Crie
   `src/main/resources/application-local.properties`. Ele já está no `.gitignore` e é
   carregado automaticamente:

   ```properties
   ORACLE_USUARIO=rm999999
   ORACLE_SENHA=minhaSenha
   ```

2. **Variáveis de ambiente**, que têm prioridade sobre o arquivo (é assim que o Render recebe as credenciais):

   ```powershell
   $env:ORACLE_USUARIO = "rm999999"
   $env:ORACLE_SENHA   = "minhaSenha"
   .\mvnw spring-boot:run
   ```

| Variável | Padrão |
|----------|--------|
| `ORACLE_HOST` | `oracle.fiap.com.br` |
| `ORACLE_PORTA` | `1521` |
| `ORACLE_SID` | `ORCL` |
| `ORACLE_USUARIO` | — (obrigatória) |
| `ORACLE_SENHA` | — (obrigatória) |

Sem as credenciais, a aplicação não sobe no perfil padrão. Para trabalhar sem o Oracle, use o
perfil `h2`.

O driver é o `com.oracle.database.jdbc:ojdbc11` e o dialeto é detectado automaticamente pelo
Hibernate a partir da conexão. O pool (HikariCP) está limitado a 5 conexões porque o ambiente
acadêmico restringe sessões simultâneas por usuário. O driver traz até 256 KB de cada CLOB junto
com a linha (`oracle.jdbc.defaultLobPrefetchSize`), em vez de uma ida ao banco por coluna.

### Schema

`spring.jpa.hibernate.ddl-auto=update` cria e evolui as 8 tabelas do projeto sem alterar outros
objetos já existentes no schema do aluno. O DDL equivalente está versionado em
[`docs/ddl-oracle.sql`](docs/ddl-oracle.sql), para criação manual ou consulta.

### Migração dos dados do H2 para o Oracle

A base analisada que estava no H2 já foi migrada para o Oracle. A ferramenta que faz a cópia
(`br.com.totvs.hermes.migracao.MigradorH2ParaOracle`) continua disponível e pode ser
executada novamente:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=migracao
```

Como funciona:

- descobre as colunas nos catálogos dos dois bancos e copia apenas as que existem em ambos;
- preserva os identificadores originais, mantendo as chaves estrangeiras válidas;
- percorre as tabelas na ordem das dependências (lotes → reuniões → insights → feedbacks);
- lê a origem por páginas (keyset por `id`), sem carregar tudo em memória;
- ao final de cada tabela reposiciona a sequência da coluna de identidade
  (`START WITH LIMIT VALUE`), para que novos cadastros continuem após o maior id copiado;
- **é seguro repetir**: tabelas que já têm dados no destino são ignoradas, sem duplicar nada.

Resultado da migração executada: 1 lote, 1.126 reuniões, 2.819 insights e 1.126 feedbacks
(86,3 MB de transcrições) em 172 segundos, sem registros órfãos.

> Detalhe de implementação: cada registro usa um `PreparedStatement` próprio. O driver Oracle
> fixa o formato do *bind* na primeira execução do comando e, ao encontrar mais adiante uma
> transcrição acima de 32 KB, tentaria trocar para o protocolo LONG e falharia com `ORA-01461`.
> Textos grandes ainda são enviados como CLOB temporário (locator), o que evita o problema por
> completo.

### Decisões de mapeamento para o Oracle

| Situação | Solução adotada |
|----------|-----------------|
| `TEXT` não existe no Oracle | Textos longos passaram a usar `@Lob` → `CLOB` (portável entre Oracle e H2) |
| Oracle não aceita `LOWER()` sobre `CLOB` | Os campos usados na pesquisa textual (`temaReuniao`, `resumoReuniao`, `descricao` do plano) são `VARCHAR2`, com folga sobre o maior valor real medido na base (79 e 163 caracteres) |
| Transcrições muito grandes | Continuam em `CLOB` — a maior transcrição da base tem 188 mil caracteres, e a gravação/leitura foi validada com 68 KB em uma única importação |
| Chaves primárias | `GenerationType.IDENTITY`, suportado pelo Oracle 19c (`generated as identity`) e pelo H2 |
| Booleanos | Mapeados pelo Hibernate como `NUMBER(1)` com `CHECK (0,1)` |

## Arquitetura em camadas

```
controller  ->  service  ->  dao  ->  banco (Oracle / H2)
    |             |            |
   dto        exception      model
```

| Pacote | Responsabilidade |
|--------|-----------------|
| `model` | Entidades JPA (`Reuniao`, `Insight`, `FeedbackReuniao`, `ImportacaoLote`, `Usuario`, `Cliente`, `PlanoAcao`, `ComentarioReuniao`) e enums de domínio |
| `dao` | Camada de acesso a dados: `GenericDao`, `AbstractJpaDao` e os DAOs específicos (implementações em `dao.impl`) |
| `repository` | Repositórios Spring Data JPA usados pelo motor de análise e pelos relatórios |
| `service` | Regras de negócio e validações; orquestra DAOs, análise automática, importação e relatórios |
| `controller` | Endpoints REST — sem regra de negócio, apenas recebem, delegam e devolvem DTOs |
| `dto.requisicao` | Objetos de entrada, anotados com Bean Validation |
| `dto.resposta` | Objetos de saída (a entidade nunca é serializada diretamente) |
| `dto.filtro` | Critérios de pesquisa das listagens paginadas |
| `dto.mapper` | Conversão entidade ⇄ DTO |
| `exception` | Exceções de domínio e o tratador global (`ManipuladorGlobalExcecoes`) |
| `config` | CORS, OpenAPI, codificador de senha e carga inicial |
| `migracao` | Ferramenta de cópia dos dados do H2 para o Oracle (perfil `migracao`) |
| `util` | Utilitários estáticos (`TextoUtils`, `ValidadorCnpj`) |
| `cli` | Menu de linha de comando (`MenuCli`, perfil `cli`) |

## Modelo de dados

| Tabela | Entidade | Relacionamentos |
|--------|----------|-----------------|
| `reunioes` | `Reuniao` | `N:1` `ImportacaoLote`, `N:1` `Cliente` (opcional) |
| `insights` | `Insight` | `N:1` `Reuniao` |
| `feedbacks_reuniao` | `FeedbackReuniao` | `1:1` `Reuniao` |
| `importacao_lotes` | `ImportacaoLote` | `1:N` `Reuniao` |
| `usuarios` | `Usuario` | `1:N` `PlanoAcao`, `1:N` `ComentarioReuniao` |
| `clientes` | `Cliente` | `1:N` `Reuniao` |
| `planos_acao` | `PlanoAcao` | `N:1` `Reuniao`, `N:1` `Usuario` (responsável) |
| `comentarios_reuniao` | `ComentarioReuniao` | `N:1` `Reuniao`, `N:1` `Usuario` (autor) |

Enums de domínio: `StatusCompletude`, `Sentimento`, `RiscoChurn`, `NivelCriticidade`,
`PerfilUsuario`, `StatusCliente`, `StatusPlanoAcao`, `PrioridadePlanoAcao`.

### Ciclo de vida do plano de ação

```
PENDENTE ──▶ EM_ANDAMENTO ──▶ CONCLUIDO
   │              │
   └──────────────┴────────▶ CANCELADO
```

As transições válidas são declaradas no próprio enum `StatusPlanoAcao`, e a resposta da API
devolve em `transicoesPermitidas` os status alcançáveis — o Front-End só habilita o que é válido.

## Motor de análise

A análise é feita por heurísticas sobre o texto da transcrição (`AnaliseTranscricaoService`,
`ChurnService`, `FeedbackReuniaoService`), sem modelo de linguagem. Cada insight guarda o trecho
da transcrição que o originou, e esse trecho aparece no detalhe da reunião.

| Sinal | Regra |
|-------|-------|
| **Budget** | Primeiro valor em dinheiro citado: exige "R$", "reais" logo depois ou uma palavra financeira logo antes (valor, custo, orçamento, mensalidade, preço…). Soma +15 no score de qualidade e +20 no comercial |
| **Churn alto** | Expressões sobre o contrato com o fornecedor: "cancelamento do contrato", "cancelar o contrato", "não vamos renovar", "trocar de fornecedor", "sair da totvs"… |
| **Churn médio** | Insatisfação ou avaliação de alternativas ("não está atendendo", "insatisfeito com", "proposta do concorrente"…) ou concorrente citado |

> **Limitação conhecida:** por ser baseada em palavras-chave, a regra de churn não sabe de quem
> é o contrato. Um prospect dizendo que vai cancelar o contrato com o fornecedor *atual* para vir
> para a TOTVS também aparece como churn alto.

Depois de mudar uma regra, as reuniões já gravadas podem ser reprocessadas com
`POST /api/v1/reunioes/{id}/analise`.

## Endpoints

### Autenticação

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/v1/autenticacao/login` | Valida e-mail e senha (BCrypt) e devolve o usuário autenticado |

### Usuários

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/v1/usuarios` | Lista paginada (`termo`, `ativos`, `pagina`, `tamanho`) |
| GET | `/api/v1/usuarios/{id}` | Busca por id |
| GET | `/api/v1/usuarios/perfis/{perfil}` | Usuários de um perfil |
| POST | `/api/v1/usuarios` | Cadastra usuário |
| PUT | `/api/v1/usuarios/{id}` | Atualiza dados cadastrais |
| PATCH | `/api/v1/usuarios/{id}/senha` | Troca a senha (exige a senha atual) |
| DELETE | `/api/v1/usuarios/{id}` | Remove usuário sem histórico |

### Clientes

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/v1/clientes` | Lista paginada (`termo`, `status`, `uf`, `pagina`, `tamanho`) |
| GET | `/api/v1/clientes/{id}` | Busca por id (traz o total de reuniões) |
| GET | `/api/v1/clientes/cnpj/{cnpj}` | Busca por CNPJ, com ou sem máscara |
| GET | `/api/v1/clientes/{id}/reunioes` | Reuniões vinculadas ao cliente |
| POST | `/api/v1/clientes` | Cadastra cliente (valida dígitos do CNPJ) |
| PUT | `/api/v1/clientes/{id}` | Atualiza cliente |
| DELETE | `/api/v1/clientes/{id}` | Remove cliente e desvincula as reuniões |

### Reuniões

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/v1/reunioes` | Lista paginada com filtros: `termo`, `clienteId`, `loteId`, `statusCompletude`, `sentimento`, `riscoChurn`, `prioridade`, `dataInicial`, `dataFinal`, `somenteAnalisadas`, `incluirDuplicadas`, `ordenarPor`, `direcao` |
| GET | `/api/v1/reunioes/{id}` | Análise completa da reunião |
| GET | `/api/v1/reunioes/{id}/insights` | Insights extraídos da transcrição |
| GET | `/api/v1/reunioes/{id}/feedback` | Feedback educativo gerado |
| GET | `/api/v1/reunioes/{id}/planos-acao` | Planos de ação da reunião |
| POST | `/api/v1/reunioes` | Cadastro manual + análise automática |
| PUT | `/api/v1/reunioes/{id}` | Atualiza (reanalisa quando a transcrição muda) |
| POST | `/api/v1/reunioes/{id}/analise` | Reexecuta a análise automática |
| PUT | `/api/v1/reunioes/{id}/cliente` | Vincula a reunião a um cliente |
| DELETE | `/api/v1/reunioes/{id}/cliente` | Desfaz o vínculo |
| DELETE | `/api/v1/reunioes/{id}` | Remove a reunião e o conteúdo derivado |

### Planos de ação

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/v1/planos-acao` | Lista paginada (`termo`, `reuniaoId`, `responsavelId`, `status`, `prioridade`, `somenteAtrasados`, `prazoAte`, `ordenarPor`, `direcao`) |
| GET | `/api/v1/planos-acao/{id}` | Busca por id |
| GET | `/api/v1/planos-acao/atrasados` | Planos em aberto com prazo vencido |
| GET | `/api/v1/planos-acao/responsaveis/{responsavelId}` | Planos de um responsável |
| GET | `/api/v1/planos-acao/indicadores` | Totais por status e em atraso |
| POST | `/api/v1/planos-acao` | Cria plano vinculado a uma reunião |
| PUT | `/api/v1/planos-acao/{id}` | Atualiza plano em aberto |
| PATCH | `/api/v1/planos-acao/{id}/status` | Move o plano no ciclo de vida |
| DELETE | `/api/v1/planos-acao/{id}` | Remove plano |

### Comentários

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/v1/reunioes/{reuniaoId}/comentarios` | Comentários da reunião (paginado) |
| POST | `/api/v1/reunioes/{reuniaoId}/comentarios` | Registra comentário |
| PUT | `/api/v1/comentarios/{id}` | Edita o texto (autor, gestor ou admin) |
| DELETE | `/api/v1/comentarios/{id}?usuarioId=` | Remove (autor, gestor ou admin) |

### Importações, dashboard e relatórios

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/v1/importacoes` | Upload do CSV de transcrições, ou de um `.txt`/`.docx` com uma transcrição (`multipart/form-data`, campo `arquivo`) |
| GET | `/api/v1/importacoes` | Lotes já importados |
| GET | `/api/v1/importacoes/{id}` | Lote por id |
| GET | `/api/v1/importacoes/{id}/reunioes` | Reuniões de um lote |
| GET | `/api/v1/dashboard` | Indicadores consolidados (reuniões, churn, planos e carteira) |
| GET | `/api/v1/relatorios/executivo` | PDF executivo consolidado |
| GET | `/api/v1/relatorios/reunioes/{id}` | PDF individual da reunião |
| GET | `/api/v1/relatorios/lotes/{id}` | PDF executivo do lote |

### Exemplos

```bash
# Login
curl -X POST http://localhost:8080/api/v1/autenticacao/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@hermes.com.br","senha":"admin123"}'

# Cadastro de cliente
curl -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -d '{"razaoSocial":"Industria Alfa LTDA","cnpj":"11.222.333/0001-81","uf":"SP","status":"ATIVO"}'

# Reuniões com risco de churn alto, ordenadas por score comercial
curl "http://localhost:8080/api/v1/reunioes?riscoChurn=ALTO&ordenarPor=scoreComercial&direcao=desc&tamanho=10"

# Importação de CSV
curl -X POST http://localhost:8080/api/v1/importacoes -F "arquivo=@transcricoes.csv"

# Importação de uma transcrição avulsa (.txt ou .docx)
curl -X POST http://localhost:8080/api/v1/importacoes -F "arquivo=@reuniao-alfa.docx"
```

### Transcrições avulsas (.txt e .docx)

Cada arquivo `.txt` ou `.docx` vira **uma** reunião, analisada pelo mesmo caminho das linhas do
CSV. O documento pode começar com um cabeçalho opcional entre linhas `---`:

```text
---
Cliente: Construtora Alfa
Data: 2025-03-10
Vendedor: T12345
Duração: 45min
---
Vendedor: Bom dia, tudo bem?
Cliente: Tudo, obrigado...
```

Chaves aceitas: `ID`, `Cliente`, `Data`, `Vendedor`, `Duração`, `Formato`, `Status`, `UF`, `Segmento`,
`CNAE`, `Faturamento` e `NPS`. Sem cabeçalho, o cliente vem do nome do arquivo e a data, se houver
uma no nome (`acme_2025-03-10.txt` ou `acme 10-03-2025.docx`). Sem `ID`, o identificador é derivado do
texto (`ARQ-…`), então reenviar a mesma transcrição é marcado como duplicada. Arquivos `.doc`
(Word 97-2003) não são aceitos — salve como `.docx`.

## Validações e regras de negócio

| Recurso | Regras aplicadas |
|---------|------------------|
| Usuário | e-mail único e válido; senha com 6 a 60 caracteres, armazenada em BCrypt; troca de senha exige a senha atual e uma senha diferente da anterior; o sistema nunca fica sem administrador ativo; usuário com histórico é inativado, não excluído |
| Cliente | CNPJ obrigatório, validado pelos dígitos verificadores e único; UF com 2 letras; NPS entre 0 e 10; exclusão desvincula as reuniões em vez de apagá-las |
| Reunião | data e cliente obrigatórios; transcrição com no mínimo 20 caracteres; identificador externo único; exclusão remove insights, feedback, comentários e planos |
| Plano de ação | título de 5 a 120 caracteres; reunião duplicada não aceita plano; responsável precisa estar ativo; prazo não pode ser retroativo; transições seguem o ciclo de vida; conclusão e cancelamento exigem o resultado; plano finalizado não pode ser editado |
| Comentário | texto de 3 a 2000 caracteres; edição e exclusão apenas pelo autor, gestor ou administrador; a autoria original é preservada e o comentário é marcado como editado |
| Importação | arquivo obrigatório, não vazio e com extensão `.csv`, `.txt` ou `.docx`; limite de 50 MB; duplicidade detectada dentro do lote e entre lotes |

## Tratamento de exceções

Todo erro devolve o mesmo corpo (`instante`, `status`, `erro`, `mensagem`, `caminho` e,
nas validações, a lista `campos`):

| Situação | Exceção | HTTP |
|----------|---------|------|
| Campos inválidos na requisição | `MethodArgumentNotValidException` / `ConstraintViolationException` | 400 |
| Credenciais inválidas ou usuário inativo | `CredenciaisInvalidasException` | 401 |
| Usuário sem permissão sobre o registro | `OperacaoNaoPermitidaException` | 403 |
| Recurso inexistente | `RecursoNaoEncontradoException` | 404 |
| Dado único já cadastrado | `ConflitoDeDadosException` | 409 |
| Upload acima do limite | `MaxUploadSizeExceededException` | 413 |
| Violação de regra de negócio | `RegraNegocioException` | 422 |
| Falha ao gerar PDF | `RelatorioException` | 500 |

## Padrões de projeto e boas práticas

- **DAO** — `GenericDao` (contrato) + `AbstractJpaDao` (*Template Method*) reaproveitado por todos os DAOs.
- **DTO + Mapper (Assembler)** — entidades nunca são serializadas; a conversão fica isolada em `dto.mapper`.
- **Service Layer / Facade** — regra de negócio fora do controller; `PainelExecutivoService` compõe os indicadores de vários serviços.
- **State** — `StatusPlanoAcao` conhece as próprias transições, evitando `if` espalhado pelos serviços.
- **Controller Advice** — tratamento de exceções centralizado, sem `try/catch` nos controllers.
- **Injeção de dependência por construtor** (`@RequiredArgsConstructor`), com campos `final`.
- **Transações explícitas** — leitura com `@Transactional(readOnly = true)`, escrita com `@Transactional`.
- **Consultas seguras** — parâmetros nomeados no JPQL e lista branca para os campos de ordenação.
- **`open-in-view` desativado** — as consultas usam `JOIN FETCH`, evitando *lazy loading* na camada web.
- **Nomenclatura** — pacotes e classes em português (domínio) com sufixos padrão (`Service`, `Dao`, `Controller`, `Requisicao`, `Resposta`), métodos em `camelCase` e constantes em `UPPER_SNAKE_CASE`.

## Testes

Os testes seguem o padrão adotado no projeto: classes com `main()` e `try/catch`, sem framework.

```bash
./mvnw clean package -DskipTests
./mvnw dependency:build-classpath -Dmdep.outputFile=cp.txt

# Windows (separador ";")
java -cp "target/test-classes;target/classes;$(cat cp.txt)" br.com.totvs.hermes.model.ReuniaoTest
java -cp "target/test-classes;target/classes;$(cat cp.txt)" br.com.totvs.hermes.util.TextoUtilsTest
java -cp "target/test-classes;target/classes;$(cat cp.txt)" br.com.totvs.hermes.service.DuplicidadeServiceTest
java -cp "target/test-classes;target/classes;$(cat cp.txt)" br.com.totvs.hermes.service.ImportacaoLoteServiceTest
java -cp "target/test-classes;target/classes;$(cat cp.txt)" br.com.totvs.hermes.service.ValorFinanceiroTest
java -cp "target/test-classes;target/classes;$(cat cp.txt)" br.com.totvs.hermes.service.ChurnServiceTest
java -cp "target/test-classes;target/classes;$(cat cp.txt)" br.com.totvs.hermes.service.TranscricaoArquivoServiceTest
java -cp "target/test-classes;target/classes;$(cat cp.txt)" br.com.totvs.hermes.ia.AplicadorAnaliseIaTest
```

`ValorFinanceiroTest` e `ChurnServiceTest` cobrem as regras de budget e de churn com trechos
reais que antes geravam falso positivo ("3 mil colaboradores", "cálculo de rescisão").

## Configuração

Principais propriedades em `src/main/resources/application.properties`:

| Propriedade | Padrão | Uso |
|-------------|--------|-----|
| `server.port` | `${PORT:8080}` | Porta da API (o Render informa a sua em `PORT`) |
| `spring.datasource.url` | `jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL` | Conexão Oracle (ver variáveis de ambiente acima) |
| `spring.datasource.hikari.maximum-pool-size` | `5` | Limite de conexões simultâneas |
| `spring.jpa.properties.hibernate.jdbc.fetch_size` | `100` | Linhas por ida ao banco nas consultas |
| `hermes.cors.origens` | `localhost:3000, 4200, 5173, 127.0.0.1:5500` | Origens liberadas para o Front-End (sobrescreva com `HERMES_CORS_ORIGENS`) |
| `hermes.relatorios.diretorio` | `./relatorios` | Pasta onde os PDFs são gerados |
| `spring.servlet.multipart.max-file-size` | `50MB` | Tamanho máximo do CSV importado |
| `spring.jpa.hibernate.ddl-auto` | `update` | Criação/atualização automática do schema |

## Limitações conhecidas

- **A API não tem autenticação por token.** O login valida a senha, mas os demais endpoints são
  abertos; com a API publicada, qualquer pessoa com a URL consegue ler e alterar dados.
- **Os PDFs executivo e de lote levam de 40 a 50 s em produção**, porque leem todas as reuniões
  com seus textos. O PDF de uma reunião sai em cerca de 3 s.
- **O feedback educativo é gerado sem acentos** ("orcamento", "decisao") pelos modelos de texto
  do `FeedbackReuniaoService`.
- **A detecção de codificação do CSV é frágil.** O importador (`CsvService.lerArquivo`) tenta
  UTF-8 e, se encontrar um único byte inválido em qualquer ponto do arquivo, relê o arquivo
  inteiro como ISO-8859-1, trocando todos os acentos. Essa é a causa provável dos textos
  corrigidos na base atual. Importe CSVs salvos em UTF-8 válido.

## Integrantes do grupo

| Nome | RM |
|------|----|
| Nicolas Barnabe da Cruz | 561997 |
| Luiz Antônio Moraes Santos | 562142 |
| Kevin Carvalho Venâncio | 561459 |
| Guilherme de Melo Sorrilha | 563825 |
| Yan Breno Barutti Conceição | 566412 |
