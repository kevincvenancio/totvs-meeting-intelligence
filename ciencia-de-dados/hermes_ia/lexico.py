"""
Léxico do motor heurístico (portado do back-end Java: AnaliseTranscricaoService,
ChurnService, SentimentoService e InsightExtratorService).

Serve a dois propósitos:
  1. **Baseline** — é o "modelo" que existia antes da Sprint 4, usado na validação
     para mostrar quanto o modelo do Hugging Face acrescenta;
  2. **Regras de alta precisão** dentro do modelo híbrido: quando o cliente diz
     literalmente "não vamos renovar" ou cita "Protheus", não há o que adivinhar.
"""
from __future__ import annotations

import re
from functools import lru_cache

PRODUTOS_TOTVS: dict[str, list[str]] = {
    "TOTVS Protheus (ERP)": ["protheus", "erp totvs", "backoffice", "módulo financeiro", "módulo contábil", "datasul"],
    "TOTVS RH / Folha": ["totvs rm", "meu rh", "portal rh", "totvs rh", "rh totvs", "folha de pagamento totvs",
                         "departamento pessoal totvs", "payroll totvs"],
    "TOTVS Agora (Ponto)": ["totvs agora", "ponto eletrônico totvs", "registro de ponto totvs", "agora totvs"],
    "TOTVS Fluig": ["fluig", "bpm totvs"],
    "TOTVS BI / Analytics": ["totvs bi", "bi totvs", "analytics totvs", "dashboard totvs"],
    "TOTVS CRM": ["totvs crm", "crm totvs"],
    "TOTVS Techfin": ["techfin", "totvs pay"],
    "TOTVS Carol (IA e Dados)": ["carol totvs", "totvs carol", "ia totvs"],
    "TOTVS Varejo": ["totvs varejo", "pdv totvs", "frente de caixa totvs"],
    "TOTVS Saúde": ["totvs saúde", "prontuário totvs"],
    "TOTVS Educacional": ["totvs educacional", "rm educacional"],
    "TOTVS Logística": ["wms totvs", "tms totvs", "logística totvs"],
}

CONCORRENTES: dict[str, list[str]] = {
    "Senior Sistemas": ["senior sistemas", "sênior sistemas", "senior rh", "da senior"],
    "SAP": ["sap s/4", "s4hana", "sap hana", "sap erp", "sap business one", "da sap"],
    "Oracle": ["oracle erp", "jd edwards", "netsuite"],
    "Linx": ["linx sistemas", "linx pos", "da linx"],
    "Omie": ["omie"],
    "Sankhya": ["sankhya"],
    "Microsoft Dynamics": ["dynamics 365", "microsoft dynamics", "d365"],
    "Salesforce": ["salesforce"],
    "Domínio Sistemas": ["dominio sistemas", "domínio sistemas"],
    "CIGAM": ["cigam"],
    "Mega Sistemas": ["mega sistemas", "mega erp"],
    "Ahgora": ["ahgora"],
}

CHURN_ALTO = [
    "cancelar o contrato", "cancelar contrato", "cancelar nosso contrato",
    "cancelamento do contrato", "pedido de cancelamento", "rescindir o contrato",
    "encerrar o contrato", "rescisão do contrato com",
    "não vamos renovar", "não renovar o contrato", "não vou renovar",
    "sair da totvs", "trocar de fornecedor",
    "migrar para outro fornecedor", "migrar para outro sistema", "migrar para outro erp",
]
CHURN_MEDIO = [
    "avaliando outra solução", "insatisfeito com", "não está atendendo",
    "procurar alternativa", "proposta do concorrente",
    "estamos olhando para", "pensando em trocar",
]

POSITIVAS = [
    "ótimo", "gostei muito", "excelente", "muito bom", "resolveu",
    "melhorou bastante", "estamos satisfeitos", "ótima apresentação",
    "muito interessante", "faz todo sentido", "adorei", "funciona muito bem",
    "boa experiência", "muito satisfeito", "bacana", "maravilhoso",
]
NEGATIVAS = [
    "problema sério", "sofrendo muito", "ruim demais", "muito difícil",
    "retrabalho constante", "erro grave", "demora absurda", "insatisfeito",
    "péssimo", "não funciona", "falha constante", "impossível usar",
    "atraso grande", "frustrado", "decepcionante", "não atende",
]

#: dor -> (descrição, criticidade)
DORES: dict[str, tuple[str, str]] = {
    "processo manual": ("Processo manual", "ALTA"),
    "planilha": ("Uso excessivo de planilhas", "MEDIA"),
    "retrabalho": ("Retrabalho operacional", "ALTA"),
    "não integra": ("Falta de integração entre sistemas", "ALTA"),
    "falta integração": ("Falta de integração entre sistemas", "ALTA"),
    "não conseguimos medir": ("Falta de indicadores e visibilidade", "ALTA"),
    "sem dashboard": ("Ausência de dashboard gerencial", "MEDIA"),
    "processo lento": ("Processos lentos", "MEDIA"),
    "demora muito": ("Demora operacional", "MEDIA"),
    "não atende": ("Sistema atual insuficiente", "ALTA"),
    "suporte demora": ("Demora no suporte", "ALTA"),
    "chamado em aberto": ("Chamado de suporte pendente", "MEDIA"),
    "ninguém responde": ("Falta de resposta do suporte", "ALTA"),
    "não funciona": ("Funcionalidade com problema", "ALTA"),
    "perda de dados": ("Perda ou inconsistência de dados", "ALTA"),
    "lentidão": ("Lentidão do sistema", "ALTA"),
    "banco de horas": ("Controle complexo de banco de horas", "MEDIA"),
    "falta automação": ("Falta de automação", "ALTA"),
}

#: Sinal de oportunidade -> produto sugerido para venda cruzada
VENDA_CRUZADA: dict[str, list[str]] = {
    "TOTVS RH / Folha": ["folha de pagamento", "departamento pessoal", "holerite"],
    "TOTVS BI / Analytics": ["relatório gerencial", "dashboard", "business intelligence", "indicador de desempenho"],
    "TOTVS Fluig": ["integração entre sistemas", "api de integração", "fluxo de aprovação"],
    "TOTVS CRM": ["gestão comercial", "funil de vendas", "pipeline de vendas", "leads"],
    "TOTVS Techfin": ["fluxo de caixa", "contas a pagar", "contas a receber", "cobrança"],
    "TOTVS Carol (IA e Dados)": ["inteligência artificial", "machine learning", "plataforma de dados"],
    "TOTVS Agora (Ponto)": ["ponto eletrônico", "escala de trabalho", "banco de horas", "jornada de trabalho"],
}

SINAIS_INTERESSE = [
    "manda a proposta", "mande a proposta", "enviar a proposta", "envia a proposta",
    "quero contratar", "vamos fechar", "podemos fechar", "tenho interesse",
    "temos interesse", "faz todo sentido", "quanto custa", "qual o valor",
    "próximos passos", "agendar uma demonstração", "agenda uma demo",
]

# ── Budget (mesma regra do Java: AnaliseTranscricaoService.encontrarValorFinanceiro) ──
_VALOR = re.compile(
    r"R\$\s*\d+(?:[.,]\d+)*(?:\s*(?:milhões|milhão|mil|k)\b)?"
    r"|\b\d+(?:[.,]\d+)*\s*(?:milhões|milhão|mil|k)\b(?:\s+(?:de\s+)?reais)?",
    re.IGNORECASE,
)
_CONTEXTO_FIN = re.compile(
    r"(valor|custo|custa|investimento|investir|orçamento|budget|preço|mensalidade|pagar|paga|pago|"
    r"gasto|gastar|gasta|economia|economizar|faturamento|fatura|ticket|desconto|proposta|"
    r"receita|prejuízo|multa|reais)\b[^.?!]{0,25}$",
    re.IGNORECASE,
)


def encontrar_valor_financeiro(texto: str) -> str | None:
    """Primeiro valor em dinheiro citado ("3 mil colaboradores" não conta)."""
    for m in _VALOR.finditer(texto or ""):
        val = m.group().strip()
        low = val.lower()
        antes = texto[max(0, m.start() - 40): m.start()]
        if low.startswith("r$") or low.endswith("reais") or _CONTEXTO_FIN.search(antes):
            return val
    return None


@lru_cache(maxsize=1024)
def _padrao(termo: str) -> re.Pattern:
    return re.compile(r"(?<!\w)" + re.escape(termo) + r"(?!\w)")


def contem(texto_lower: str, termo: str) -> bool:
    """Termo como palavra(s) inteira(s): "quanto custa" não casa com "quanto custaria"."""
    # Filtro barato antes da regex: na maioria dos termos a substring nem aparece.
    return termo in texto_lower and _padrao(termo).search(texto_lower) is not None


def achar(texto_lower: str, termos: list[str]) -> str | None:
    """Primeiro termo da lista presente no texto (já em minúsculas)."""
    for t in termos:
        if contem(texto_lower, t):
            return t
    return None


def achar_dicionario(texto_lower: str, dic: dict[str, list[str]]) -> dict[str, str]:
    """{chave: termo encontrado} para cada entrada do dicionário presente no texto."""
    out: dict[str, str] = {}
    for chave, termos in dic.items():
        t = achar(texto_lower, termos)
        if t:
            out[chave] = t
    return out
