# TOTVS Meeting Intelligence

Sistema de inteligência comercial para análise automática de transcrições de reuniões TOTVS.
Importa arquivos CSV com transcrições, classifica completude, extrai sentimento, risco de churn,
oportunidades de venda cruzada e gera relatórios executivos em PDF.

## Pré-requisitos

- Java 17
- Maven 3.6+

## Como executar

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

## Estrutura de pacotes

| Pacote | Responsabilidade |
|--------|-----------------|
| `model` | Entidades JPA: `Reuniao`, `Insight`, `FeedbackReuniao`, `ImportacaoLote` e enums de domínio |
| `repository` | Interfaces Spring Data JPA |
| `service` | Lógica de negócio: análise, dashboard, PDF, CSV, churn, sentimento, insights |
| `util` | Utilitários estáticos: `TextoUtils` |
| `cli` | Interface de linha de comando (`MenuCli`) |

## Executando os testes manuais

```bash
# Compilar e rodar testes individualmente
mvn compile test-compile
java -cp target/test-classes:target/classes ReuniaoTest
java -cp target/test-classes:target/classes TextoUtilsTest
```
