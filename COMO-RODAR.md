# Como rodar o TOTVS Hermes

Guia completo para subir o projeto do zero numa máquina Windows.
São **dois programas rodando ao mesmo tempo**, cada um na sua janela de terminal:

| Janela | O que roda | Endereço |
|--------|------------|----------|
| Terminal 1 | Back-end — API Java (Spring Boot) | http://localhost:8080 |
| Terminal 2 | Front-end — React (Vite) | http://localhost:5173 |

O front-end conversa com a API; se você abrir só o front, ele entra em "modo vitrine"
(dados falsos de demonstração) e avisa na tela. Para ver o sistema de verdade, **as duas
janelas precisam estar abertas**.

---

## Passo 0 — Instalar os pré-requisitos (só na primeira vez)

São só três, e os três têm instalador:

| Programa | Versão | Onde baixar |
|----------|--------|-------------|
| Git | qualquer | https://git-scm.com/download/win |
| JDK (Java) | 17 ou superior (testado no 21) | https://adoptium.net |
| Node.js | 20.19+ ou 22+ (testado no 22) | https://nodejs.org (versão LTS) |

> **Maven não precisa ser instalado.** O projeto vem com o *Maven Wrapper* (`mvnw`):
> na primeira vez que você rodar, ele mesmo baixa o Maven 3.9.16 para a sua pasta de
> usuário. Por isso os comandos deste guia usam `.\mvnw` e não `mvn`.

### Confira se deu certo

Abra o **PowerShell** (Iniciar → digite "PowerShell" → Enter) e rode:

```powershell
git --version
java -version
node -v
npm -v
```

Se algum comando responder *"não é reconhecido como nome de cmdlet"*, aquele programa
não foi instalado ou não está no `Path`. Resolva antes de continuar.

---

## Passo 1 — Baixar o código

No PowerShell, escolha uma pasta e clone:

```powershell
cd $env:USERPROFILE\Documents
git clone -b feature/frontend-premium https://github.com/kevincvenancio/totvs-meeting-intelligence.git
cd totvs-meeting-intelligence
```

> O `-b feature/frontend-premium` pega a versão atual (Hermes, com a interface nova).
> A branch `main` ainda está na versão anterior do projeto.

Agora você está na **raiz do projeto** — é daqui que o back-end sobe.
Para conferir, `dir` deve mostrar `pom.xml`, `mvnw.cmd`, `src` e `frontend`.

---

## Passo 2 — Terminal 1: subir o back-end (API)

Na **raiz do projeto** (onde está o `pom.xml`):

```powershell
.\mvnw clean package -DskipTests
```

> Baixa o Maven, baixa as dependências e compila. **Na primeira vez demora de 2 a 5
> minutos** e escreve centenas de linhas de `Downloading...` — é normal. Tem que
> terminar com `BUILD SUCCESS`.
>
> O `.\` na frente é obrigatório no PowerShell: sem ele o terminal não procura o
> programa na pasta atual. No `cmd` use `mvnw` e no Git Bash `./mvnw`.

Depois, ainda na mesma janela:

```powershell
.\mvnw spring-boot:run
```

Espere aparecer a linha:

```
Started HermesApplication in 18.301 seconds
```

**Leva uns 20 segundos** (conexão com o banco Oracle da FIAP). A partir daí a
janela fica "presa" mostrando os logs — **isso é o certo, não feche**. Essa janela
é o servidor rodando.

### Confira se a API está no ar

Abra no navegador: **http://localhost:8080/swagger-ui.html**
Deve aparecer a documentação interativa com todos os endpoints.

---

## Passo 3 — Terminal 2: subir o front-end

**Abra uma segunda janela do PowerShell** (a primeira continua ocupada com a API).
Vá até a pasta `frontend`:

```powershell
cd $env:USERPROFILE\Documents\totvs-meeting-intelligence\frontend
npm install
```

> Também demora alguns minutos na primeira vez. Avisos amarelos de `warn` podem ser
> ignorados; o que não pode é terminar em `ERR!`.

Em seguida:

```powershell
npm run dev
```

Vai aparecer:

```
  VITE v8.x  ready in 900 ms
  Local:   http://localhost:5173/
```

Abra **http://localhost:5173** no navegador. Essa janela também fica presa — não feche.

---

## Passo 4 — Entrar no sistema

Na tela de login (http://localhost:5173/entrar):

- **E-mail:** `admin@hermes.com.br`
- **Senha:** `admin123`

Esse usuário é criado automaticamente na primeira vez que a API sobe, se o banco
ainda não tiver nenhum usuário.

Rotas da aplicação:

| Tela | Endereço |
|------|----------|
| Apresentação (landing) | http://localhost:5173/ |
| Login | http://localhost:5173/entrar |
| Painel | http://localhost:5173/painel |
| Reuniões | http://localhost:5173/reunioes |
| Carteira de clientes | http://localhost:5173/clientes |
| Planos de ação | http://localhost:5173/planos-acao |
| Importação de CSV | http://localhost:5173/importacoes |

---

## Resumo — a ordem dos comandos

```powershell
# ---- TERMINAL 1 (raiz do projeto) - back-end ----
cd $env:USERPROFILE\Documents\totvs-meeting-intelligence
.\mvnw clean package -DskipTests
.\mvnw spring-boot:run
#  ... deixe rodando ...

# ---- TERMINAL 2 (pasta frontend) - front-end ----
cd $env:USERPROFILE\Documents\totvs-meeting-intelligence\frontend
npm install
npm run dev
#  ... deixe rodando ...

# Navegador: http://localhost:5173
```

Nas próximas vezes, `.\mvnw clean package` e `npm install` **não são mais necessários**:
basta `.\mvnw spring-boot:run` numa janela e `npm run dev` na outra.

Para **parar**: `Ctrl + C` em cada janela (se perguntar `Deseja encerrar (S/N)?`, digite `S`).

---

## Sobre o banco de dados

Por padrão o projeto conecta no **Oracle da FIAP** (`oracle.fiap.com.br:1521/ORCL`),
onde já estão as 1.126 reuniões analisadas. As credenciais estão em
`src/main/resources/application.properties` e podem ser trocadas por variáveis de
ambiente, sem mexer no código:

```powershell
$env:ORACLE_USUARIO = "rm999999"
$env:ORACLE_SENHA   = "minhaSenha"
.\mvnw spring-boot:run
```

> Atenção: no PowerShell é `$env:NOME = "valor"`. O `set NOME=valor` do README é
> sintaxe do `cmd` e **não funciona** no PowerShell.

### Perfis de execução

| Comando | Banco | O que sobe |
|---------|-------|------------|
| `.\mvnw spring-boot:run` | Oracle FIAP | API REST (padrão, com os dados reais) |
| `.\mvnw spring-boot:run "-Dspring-boot.run.profiles=h2"` | H2 local (`./data`) | API REST + console do H2 |
| `.\mvnw spring-boot:run "-Dspring-boot.run.profiles=cli"` | Oracle FIAP | Menu de linha de comando (sem servidor web) |
| `.\mvnw spring-boot:run "-Dspring-boot.run.profiles=cli,h2"` | H2 local | Menu de linha de comando |

> As aspas em volta do `-D...` são necessárias no PowerShell.

**Importante sobre o perfil `h2`:** ele funciona sem internet e sem a FIAP, mas o
arquivo do banco (`data/`) **não vem no repositório**. O banco sobe vazio — só com o
usuário admin, sem nenhuma reunião. Para ter dados, use o perfil padrão (Oracle) ou
importe um CSV de transcrições pela tela `/importacoes`.

Console do H2 (só no perfil `h2`): http://localhost:8080/h2-console —
JDBC `jdbc:h2:file:./data/hermes`, usuário `sa`, senha em branco.

---

## Problemas comuns

**`mvnw` não é reconhecido como nome de cmdlet**
Faltou o `.\` na frente (`.\mvnw`) ou você não está na raiz do projeto. Confira com
`dir` se o `mvnw.cmd` aparece na listagem.

**`JAVA_HOME is not defined correctly` ou `The JAVA_HOME environment variable is not defined`**
O wrapper achou o Java errado ou não achou nenhum. Aponte para a pasta do JDK:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

(Confirme o caminho real em `C:\Program Files\Java` ou `C:\Program Files\Eclipse Adoptium`.)

**`Web server failed to start. Port 8080 was already in use`**
Já tem alguma coisa usando a porta. Descubra e encerre:

```powershell
netstat -ano | findstr :8080
taskkill /PID <numero_que_apareceu> /F
```

**`ORA-01017: invalid username/password`**
Usuário/senha do Oracle inválidos ou expirados. Ajuste via `$env:ORACLE_USUARIO` /
`$env:ORACLE_SENHA`, ou use o perfil `h2`.

**`IO Error: The Network Adapter could not establish the connection`**
Sem acesso ao `oracle.fiap.com.br` (internet, VPN, firewall ou rede corporativa
bloqueando a porta 1521). Use o perfil `h2` para trabalhar offline.

**Faixa avisando "modo vitrine" / "dados de demonstração" no front**
A API não está respondendo. Confira se o Terminal 1 ainda está rodando e se
http://localhost:8080/swagger-ui.html abre.

**`npm run dev` reclama da versão do Node**
O Vite 8 exige Node 20.19+ ou 22+. Rode `node -v` e atualize pelo site do Node.js.

**`npm install` falha no meio**

```powershell
Remove-Item -Recurse -Force node_modules
npm cache clean --force
npm install
```

**Front abre mas fica branco**
Provavelmente ficou uma aba velha em cache. Atualize com `Ctrl + Shift + R`.

---

## Alternativa: rodar pelo IntelliJ IDEA

1. `File → Open` → selecione a pasta do projeto (a que tem o `pom.xml`).
2. Espere o IntelliJ baixar as dependências (barra de progresso no rodapé).
3. Abra `src/main/java/br/com/totvs/hermes/HermesApplication.java` e clique no ▶ verde
   ao lado da classe.
4. O front-end continua no terminal: `cd frontend`, `npm install`, `npm run dev`.

Para escolher um perfil pela IDE: `Run → Edit Configurations…` → campo
**Active profiles** → escreva `h2` (ou `cli`, `cli,h2`).

> Se você já tem o Maven instalado e prefere usar o comando `mvn`, funciona igual —
> é só trocar `.\mvnw` por `mvn` em todos os comandos deste guia.
