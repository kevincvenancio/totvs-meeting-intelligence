"""
Configuração central do pipeline de IA do Hermes.

Tudo o que é "decisão de modelagem" (quais modelos do Hugging Face, quais rótulos,
quais limiares) fica aqui, num lugar só, para ser fácil justificar e ajustar.
"""
from __future__ import annotations

import os
from dataclasses import dataclass, field

# ──────────────────────────────────────────────────────────────────────────────
# Modelos do Hugging Face
# ──────────────────────────────────────────────────────────────────────────────

#: Sentimento — DistilBERT multilíngue destilado (positivo / neutro / negativo).
#: Leve (~135 M parâmetros), roda em CPU e entende português.
MODELO_SENTIMENTO = os.getenv(
    "HERMES_MODELO_SENTIMENTO",
    "lxyuan/distilbert-base-multilingual-cased-sentiments-student",
)

#: Zero-shot (NLI) — classifica um trecho em rótulos escritos em linguagem natural,
#: sem precisar de base rotulada para treinar (o que o projeto não tem).
#: Dois perfis da mesma família (MoritzLaurer, treinados em MNLI + XNLI, com português):
#:
#:   rapido  — MiniLMv2-L6: 6 camadas de 384. ~10 ms por par em CPU (int8, 160 tokens).
#:             PADRÃO: é o que dá conta de analisar a base inteira.
#:   preciso — mDeBERTa-v3-base: 12 camadas de 768. ~13x mais lento, um pouco mais preciso.
MODELO_ZERO_SHOT_RAPIDO = "MoritzLaurer/multilingual-MiniLMv2-L6-mnli-xnli"
MODELO_ZERO_SHOT_PRECISO = "MoritzLaurer/mDeBERTa-v3-base-mnli-xnli"
PERFIS_ZERO_SHOT = {"rapido": MODELO_ZERO_SHOT_RAPIDO, "preciso": MODELO_ZERO_SHOT_PRECISO}
PERFIL_PADRAO = os.getenv("HERMES_IA_PERFIL", "rapido")
MODELO_ZERO_SHOT = os.getenv("HERMES_MODELO_ZERO_SHOT", PERFIS_ZERO_SHOT.get(PERFIL_PADRAO, MODELO_ZERO_SHOT_RAPIDO))

#: Frase-modelo do NLI. "{}" é substituído por cada rótulo.
TEMPLATE_HIPOTESE = "Neste trecho, {}."

# ──────────────────────────────────────────────────────────────────────────────
# Rótulos (hipóteses) do zero-shot
# ──────────────────────────────────────────────────────────────────────────────

#: Intenções comerciais avaliadas em cada trecho (multi-rótulo).
#: chave interna -> hipótese em português
INTENCOES: dict[str, str] = {
    "cancelamento": "o cliente quer cancelar o contrato, não renovar ou trocar de fornecedor",
    "insatisfacao": "o cliente reclama do sistema, do suporte ou do atendimento",
    "interesse": "o cliente demonstra interesse em contratar ou ampliar a solução",
    "negociacao": "as pessoas discutem preço, proposta comercial ou orçamento",
    "concorrencia": "o cliente compara com um sistema concorrente",
}

#: Hipóteses em CONTRASTE (softmax: competem entre si e somam 1 em cada trecho).
#: A validação mostrou que a pergunta isolada "o cliente quer cancelar?" não separa as
#: classes (AUC ≈ 0,5); obrigar o modelo a escolher entre opções concorrentes — incluindo
#: "satisfeito" e "assunto técnico" — separa (AUC 0,86 para churn ALTO).
CONTRASTE_CHURN: dict[str, str] = {
    "c_cancelar": INTENCOES["cancelamento"],
    "c_reclama": "o cliente reclama de um problema, mas não fala em sair",
    "c_satisfeito": "o cliente está satisfeito e quer continuar",
    "c_tecnico": "as pessoas tratam de detalhes técnicos ou operacionais",
}
CONTRASTE_OPORT: dict[str, str] = {
    "o_interesse": INTENCOES["interesse"],
    "o_negocia": INTENCOES["negociacao"],
    "o_sem_interesse": "o cliente não tem interesse agora ou adia a decisão",
    "o_tecnico": "as pessoas tratam de detalhes técnicos ou operacionais",
}

#: Produtos TOTVS descritos pela *função* (o cliente raramente fala o nome do produto).
#: produto -> hipótese
PRODUTOS: dict[str, str] = {
    "TOTVS Protheus (ERP)": "o assunto é ERP, faturamento, compras, estoque ou contabilidade",
    "TOTVS RH / Folha": "o assunto é folha de pagamento, departamento pessoal ou recursos humanos",
    "TOTVS Agora (Ponto)": "o assunto é ponto eletrônico, jornada, escalas ou banco de horas",
    "TOTVS Fluig": "o assunto é fluxo de aprovação, automação de processos ou integração entre sistemas",
    "TOTVS BI / Analytics": "o assunto é relatórios gerenciais, indicadores ou dashboards",
    "TOTVS CRM": "o assunto é funil de vendas, leads ou gestão da equipe comercial",
    "TOTVS Techfin": "o assunto é contas a pagar e receber, fluxo de caixa, cobrança ou crédito",
    "TOTVS Carol (IA e Dados)": "o assunto é inteligência artificial ou plataforma de dados",
    "TOTVS Varejo": "o assunto é loja, PDV ou frente de caixa",
    "TOTVS Saúde": "o assunto é hospital, clínica ou prontuário",
    "TOTVS Educacional": "o assunto é escola, faculdade, matrícula ou gestão acadêmica",
    "TOTVS Logística": "o assunto é transporte, frota, armazém ou WMS",
}


# ──────────────────────────────────────────────────────────────────────────────
# Parâmetros do pipeline
# ──────────────────────────────────────────────────────────────────────────────

@dataclass
class ConfigPipeline:
    #: "hibrido" (modelo + léxico, padrão), "modelo" (só Hugging Face) ou "heuristico" (só léxico).
    modo: str = "hibrido"
    modelo_sentimento: str = MODELO_SENTIMENTO
    modelo_zero_shot: str = MODELO_ZERO_SHOT
    #: Tamanho de cada trecho enviado ao modelo, em palavras (≈ 1,4 token/palavra em PT).
    #: 90 palavras ≈ 128 tokens: o custo do transformer cresce mais que linearmente com o
    #: tamanho, e trechos curtos também localizam melhor a evidência.
    palavras_por_trecho: int = 90
    #: Limite de tokens por entrada do modelo (trecho + hipótese).
    max_tokens: int = 160
    #: Trechos avaliados nas 5 intenções (churn, insatisfação, interesse, negociação, concorrência).
    max_trechos_zero_shot: int = 8
    #: Trechos avaliados nos 12 produtos — produto aparece cedo e se repete; 3 trechos bastam.
    max_trechos_produto: int = 3
    #: Teto de trechos avaliados pelo sentimento.
    max_trechos_sentimento: int = 8
    #: Inferir produto pelo modelo quando o cliente não cita o nome. Desligado: na validação
    #: o produto mais provável estava certo em só 6 de 19 reuniões (o nome citado é melhor).
    inferir_produto_modelo: bool = False
    #: Parâmetros calibrados (None = lê hermes_ia/calibracao.json, gerado por validacao/calibrar.py).
    calibracao: dict | None = None
    #: Quantização dinâmica int8 em CPU (≈ 1,5–2x mais rápido, perda de qualidade desprezível).
    quantizar: bool = True
    #: Pares (trecho, hipótese) processados de uma vez pelo modelo.
    batch_size: int = 32
    #: Probabilidade mínima para um produto entrar na saída.
    limiar_produto: float = 0.80
    #: Quantos termos principais devolver.
    qtd_termos: int = 10
    #: "cpu", "cuda" ou índice da GPU. None = automático.
    dispositivo: str | int | None = None
    #: Faixas do risco de churn (0-100).
    faixa_churn_alto: int = 65
    faixa_churn_medio: int = 35
    extras: dict = field(default_factory=dict)

    def __post_init__(self) -> None:
        if self.modo not in {"hibrido", "modelo", "heuristico"}:
            raise ValueError(f"modo inválido: {self.modo!r}")


def config_perfil(perfil: str = PERFIL_PADRAO, **kw) -> ConfigPipeline:
    """Atalho: ConfigPipeline com o zero-shot do perfil ("rapido" ou "preciso")."""
    if perfil not in PERFIS_ZERO_SHOT:
        raise ValueError(f"perfil inválido: {perfil!r} (use rapido ou preciso)")
    return ConfigPipeline(modelo_zero_shot=PERFIS_ZERO_SHOT[perfil], **kw)
