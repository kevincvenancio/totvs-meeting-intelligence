"""
Regras de decisão calibradas — usadas pelo pipeline (indicadores.py) e pela calibração
(validacao/calibrar.py), para as duas nunca divergirem.

Os sinais vêm do modelo em formulação de CONTRASTE (as hipóteses competem entre si,
softmax), que a validação mostrou ser a que separa as classes:

    c_reclama_max    — maior P("o cliente reclama de um problema") contra "quer cancelar",
                       "está satisfeito" e "assunto técnico"            (AUC churn ALTO 0,86)
    o_negocia_max    — maior P("discutem preço, proposta ou orçamento")  (AUC oport. ALTA 0,83)
    o_interesse_max  — maior P("interesse em contratar ou ampliar")
    s_saldo_cliente  — média de P(positivo) − P(negativo) na fala do cliente (AUC pos×neg 0,90)

Os limiares ficam em calibracao.json, gerado por validacao/calibrar.py com LOOCV.
"""
from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path

ARQUIVO = Path(__file__).with_name("calibracao.json")
NIVEIS_CHURN = ("BAIXO", "MEDIO", "ALTO")
CLASSES = {
    "churn": ("BAIXO", "MEDIO", "ALTO"),
    "oportunidade": ("BAIXA", "MEDIA", "ALTA"),
    "sentimento": ("NEGATIVO", "NEUTRO", "POSITIVO"),
}


@lru_cache(maxsize=1)
def _arquivo() -> dict:
    if not ARQUIVO.exists():
        return {}
    return json.loads(ARQUIVO.read_text(encoding="utf-8"))


def carregar(modelo_zero_shot: str | None = None) -> dict:
    """
    Calibração do modelo zero-shot informado. Cada modelo tem a sua: os limiares do
    MiniLM não valem para o mDeBERTa (as probabilidades têm escalas diferentes).
    """
    modelos = _arquivo().get("modelos", {})
    if modelo_zero_shot is None:
        return next(iter(modelos.values()), {})
    return modelos.get(modelo_zero_shot, {})


# ── Sinais das regras ────────────────────────────────────────────────────────

def regras_churn(x: dict) -> float:
    return x["lex_churn_alto"] + 0.5 * x["lex_churn_medio"] + 0.3 * min(1.0, x["lex_concorrente"])


def regras_oport(x: dict) -> float:
    return min(1.0, x["lex_interesse"]) + 0.5 * x["lex_budget"] + 0.2 * min(1.0, x["lex_venda_cruzada"])


def regras_sent(x: dict) -> float:
    p, n = x["lex_pos"], x["lex_neg"] + x["lex_dores"] / 2
    return 0.0 if p + n == 0 else (p - n) / (p + n)


SINAIS_MODELO = {
    "churn": lambda x: [x["c_reclama_max"]],
    "oportunidade": lambda x: [x["o_negocia_max"], x["o_interesse_max"]],
    "sentimento": lambda x: [x["s_saldo_cliente"]],
}
SINAL_REGRAS = {"churn": regras_churn, "oportunidade": regras_oport, "sentimento": regras_sent}


def pontuar(x: dict, tarefa: str, pesos_modelo, peso_regras) -> float:
    return sum(w * v for w, v in zip(pesos_modelo, SINAIS_MODELO[tarefa](x))) + peso_regras * SINAL_REGRAS[tarefa](x)


def classificar(s: float, t1: float, t2: float, classes) -> str:
    return classes[2] if s >= t2 else classes[1] if s >= t1 else classes[0]


# ── Churn: regras dão o nível; o modelo sobe ou desce um nível ───────────────

def nivel_regras_churn(x: dict) -> int:
    if x["lex_churn_alto"]:
        return 2
    if x["lex_churn_medio"] or x["lex_concorrente"]:
        return 1
    return 0


def churn_ajustado(x: dict, t_sobe: float, t_desce: float) -> str:
    n = nivel_regras_churn(x)
    ins = x["c_reclama_max"]
    if ins >= t_sobe:
        n = min(2, n + 1)
    elif n > 0 and ins < t_desce:
        n -= 1
    return NIVEIS_CHURN[n]


def prever(x: dict, tarefa: str, params: dict) -> str:
    if params["formulacao"] == "ajuste_regras":
        return churn_ajustado(x, params["limiar_sobe"], params["limiar_desce"])
    s = pontuar(x, tarefa, params["pesos_modelo"], params["peso_regras"])
    return classificar(s, params["limiar_medio"], params["limiar_alto"], CLASSES[tarefa])


def regras_sozinhas(x: dict, tarefa: str) -> str:
    """O que o motor heurístico conclui sozinho (baseline)."""
    if tarefa == "churn":
        return NIVEIS_CHURN[nivel_regras_churn(x)]
    if tarefa == "oportunidade":
        sc = 5 + 25 * min(2, x["lex_interesse"]) + 25 * x["lex_budget"] + 10 * min(2, x["lex_venda_cruzada"])
        return "ALTA" if sc >= 60 else "MEDIA" if sc >= 35 else "BAIXA"
    p, n = x["lex_pos"], x["lex_neg"] + x["lex_dores"] // 2
    sc = 0.0 if p + n == 0 else (p - n) / (p + n) * min(1.0, (p + n) / 3)
    return "POSITIVO" if sc >= 0.2 else "NEGATIVO" if sc <= -0.2 else "NEUTRO"


# ── Escala 0-100 para a tela (o nível vem da calibração; a nota acompanha) ───

def escala_0_100(s: float, t1: float, t2: float) -> float:
    """Linear por partes: limiar médio → 35, limiar alto → 65."""
    vao = max(t2 - t1, 0.05)
    if s < t1:
        return max(0.0, 35 - 35 * (t1 - s) / vao)
    if s < t2:
        return 35 + 30 * (s - t1) / vao
    return min(100.0, 65 + 35 * (s - t2) / vao)


def nota(x: dict, tarefa: str, params: dict) -> float:
    if params["formulacao"] == "ajuste_regras":
        nivel = NIVEIS_CHURN.index(churn_ajustado(x, params["limiar_sobe"], params["limiar_desce"]))
        t_d, t_s = params["limiar_desce"], params["limiar_sobe"]
        frac = min(1.0, max(0.0, (x["c_reclama_max"] - t_d) / max(t_s - t_d, 0.05)))
        return (15, 50, 82)[nivel] + 15 * frac
    s = pontuar(x, tarefa, params["pesos_modelo"], params["peso_regras"])
    return escala_0_100(s, params["limiar_medio"], params["limiar_alto"])
