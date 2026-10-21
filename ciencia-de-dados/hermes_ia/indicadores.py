"""
Etapa de INDICADORES — o sistema de scoring.

Recebe os sinais do léxico (regras) e as probabilidades do modelo (por trecho) e
devolve indicadores de 0 a 100 com faixas, sempre acompanhados da evidência (trecho
da transcrição) que os justifica.

Nos modos "hibrido" e "modelo", o NÍVEL de cada indicador (ALTO/MÉDIO/BAIXO…) sai
das regras calibradas em hermes_ia/calibracao.py, com limiares aprendidos por
validação cruzada (validacao/calibrar.py). A nota 0-100 acompanha o nível: o limiar
médio vira 35 e o alto vira 65.

No modo "heuristico" tudo vem das regras do motor Java original (baseline).
"""
from __future__ import annotations

import logging
from dataclasses import dataclass, field

from . import calibracao as C
from .config import CONTRASTE_CHURN, CONTRASTE_OPORT, PRODUTOS, ConfigPipeline
from .processamento import Trecho

log = logging.getLogger(__name__)


@dataclass
class SinaisLexico:
    churn_alto: str | None = None
    churn_medio: str | None = None
    concorrentes: dict[str, str] = field(default_factory=dict)
    produtos_citados: dict[str, str] = field(default_factory=dict)
    venda_cruzada: dict[str, str] = field(default_factory=dict)
    interesse: list[str] = field(default_factory=list)
    positivas: list[str] = field(default_factory=list)
    negativas: list[str] = field(default_factory=list)
    dores: list[tuple[str, str, str]] = field(default_factory=list)  # (termo, descrição, criticidade)
    budget: str | None = None


@dataclass
class SaidaModelo:
    """Probabilidades do modelo para cada trecho avaliado."""
    trechos_sentimento: list[Trecho] = field(default_factory=list)
    sentimento: list[dict[str, float]] = field(default_factory=list)
    trechos_zero_shot: list[Trecho] = field(default_factory=list)
    zs_churn: list[dict[str, float]] = field(default_factory=list)   # contraste; chave = hipótese
    zs_oport: list[dict[str, float]] = field(default_factory=list)   # contraste; chave = hipótese
    trechos_produto: list[Trecho] = field(default_factory=list)
    zero_shot_produto: list[dict[str, float]] = field(default_factory=list)

    @property
    def disponivel(self) -> bool:
        return bool(self.sentimento or self.zs_churn)


# ──────────────────────────────────────────────────────────────────────────────
# Apoio
# ──────────────────────────────────────────────────────────────────────────────

def _clamp(x: float, lo: float = 0, hi: float = 100) -> float:
    return max(lo, min(hi, x))


def _resumir(texto: str, limite: int = 260) -> str:
    texto = " ".join(texto.split())
    return texto if len(texto) <= limite else texto[: limite - 1].rsplit(" ", 1)[0] + "…"


def _trecho_ao_redor(texto: str, termo: str, raio: int = 110) -> str:
    i = texto.lower().find(termo.lower())
    if i < 0:
        return ""
    ini, fim = max(0, i - raio), min(len(texto), i + len(termo) + raio)
    return ("…" if ini > 0 else "") + " ".join(texto[ini:fim].split()) + ("…" if fim < len(texto) else "")


def _ranking(probs: list[dict[str, float]], trechos: list[Trecho], hipotese: str) -> list[tuple[float, Trecho]]:
    return sorted(((p.get(hipotese, 0.0), t) for p, t in zip(probs, trechos)), key=lambda x: -x[0])


def _evidencias(pares: list[tuple[float, Trecho]], minimo: float, qtd: int = 2) -> list[dict]:
    return [{"trecho": _resumir(t.texto), "probabilidade": round(p, 3), "origem": "modelo"}
            for p, t in pares[:qtd] if p >= minimo]


def sinais(lex: SinaisLexico, saida: SaidaModelo) -> dict[str, float]:
    """Os mesmos sinais que validacao/diagnostico.py exporta para a calibração."""
    def maximo(probs, hip):
        return max((p.get(hip, 0.0) for p in probs), default=0.0)

    cli = [p for p, t in zip(saida.sentimento, saida.trechos_sentimento) if t.do_cliente]
    return {
        "lex_churn_alto": float(bool(lex.churn_alto)), "lex_churn_medio": float(bool(lex.churn_medio)),
        "lex_concorrente": float(len(lex.concorrentes)), "lex_interesse": float(len(lex.interesse)),
        "lex_budget": float(bool(lex.budget)), "lex_venda_cruzada": float(len(lex.venda_cruzada)),
        "lex_pos": float(len(lex.positivas)), "lex_neg": float(len(lex.negativas)), "lex_dores": float(len(lex.dores)),
        "c_reclama_max": maximo(saida.zs_churn, CONTRASTE_CHURN["c_reclama"]),
        "o_negocia_max": maximo(saida.zs_oport, CONTRASTE_OPORT["o_negocia"]),
        "o_interesse_max": maximo(saida.zs_oport, CONTRASTE_OPORT["o_interesse"]),
        "s_saldo_cliente": (sum(p["positivo"] - p["negativo"] for p in cli) / len(cli)) if cli else 0.0,
    }


def _params(cfg: ConfigPipeline, tarefa: str) -> dict | None:
    """Parâmetros calibrados da tarefa para o modo atual; None = usar as regras."""
    if cfg.modo == "heuristico":
        return None
    calib = cfg.calibracao if cfg.calibracao is not None else C.carregar(cfg.modelo_zero_shot)
    params = calib.get(tarefa, {}).get(cfg.modo)
    if params is None:
        log.warning("Sem calibração de %s (%s, modo %s) — usando as regras. Rode validacao/diagnostico.py "
                    "e validacao/calibrar.py.", cfg.modelo_zero_shot, tarefa, cfg.modo)
    return params


# ──────────────────────────────────────────────────────────────────────────────
# Sentimento
# ──────────────────────────────────────────────────────────────────────────────

def calcular_sentimento(lex: SinaisLexico, saida: SaidaModelo, cfg: ConfigPipeline) -> dict:
    x = sinais(lex, saida)
    p, n = len(lex.positivas), len(lex.negativas) + len(lex.dores) // 2
    params = _params(cfg, "sentimento") if saida.sentimento else None

    dist = None
    if saida.sentimento:
        cont = {"positivo": 0, "neutro": 0, "negativo": 0}
        for probs in saida.sentimento:
            cont[max(probs, key=probs.get)] += 1
        total = sum(cont.values()) or 1
        dist = {k: round(v / total, 3) for k, v in cont.items()}

    if params is None:  # regras (baseline)
        score = 0.0 if p + n == 0 else (p - n) / (p + n) * min(1.0, (p + n) / 3)
        rotulo = C.regras_sozinhas(x, "sentimento")
    else:
        rotulo = C.prever(x, "sentimento", params)
        score = C.nota(x, "sentimento", params) / 50 - 1  # limiar médio → −0,3; alto → +0,3
    if rotulo == "NEUTRO" and lex.positivas and (lex.negativas or lex.dores):
        rotulo = "MISTO"

    partes = []
    if params is not None:
        partes.append(f"Modelo: saldo {x['s_saldo_cliente']:+.2f} na fala do cliente ({len(saida.sentimento)} trechos)")
    if lex.positivas or lex.negativas:
        partes.append("Expressões: " + ", ".join((lex.positivas + lex.negativas)[:4]))
    return {
        "rotulo": rotulo,
        "score": round(max(-1.0, min(1.0, score)), 3),
        "distribuicao_trechos": dist,
        "justificativa": ". ".join(partes) or "Sem expressões emocionais claras — reunião informativa ou técnica.",
    }


# ──────────────────────────────────────────────────────────────────────────────
# Churn
# ──────────────────────────────────────────────────────────────────────────────

def _score_lexico_churn(lex: SinaisLexico) -> float:
    if lex.churn_alto:
        return 90.0
    if lex.churn_medio:
        return 55.0
    if lex.concorrentes:
        return 40.0
    return 10.0


def calcular_churn(lex: SinaisLexico, saida: SaidaModelo, cfg: ConfigPipeline, texto: str) -> dict:
    x = sinais(lex, saida)
    score_lex = _score_lexico_churn(lex)
    params = _params(cfg, "churn") if saida.zs_churn else None

    evid: list[dict] = []
    if params is None:
        nivel = C.regras_sozinhas(x, "churn")
        score = score_lex
    else:
        nivel = C.prever(x, "churn", params)
        score = C.nota(x, "churn", params)
        if nivel != "BAIXO":
            evid = _evidencias(_ranking(saida.zs_churn, saida.trechos_zero_shot, CONTRASTE_CHURN["c_reclama"]), 0.25)

    sinais_txt = []
    for termo in filter(None, [lex.churn_alto, lex.churn_medio]):
        sinais_txt.append(termo)
        if cfg.modo != "modelo":
            evid.insert(0, {"trecho": _trecho_ao_redor(texto, termo), "probabilidade": None,
                            "origem": f"léxico: “{termo}”"})
    if lex.concorrentes:
        sinais_txt.append("concorrente citado: " + ", ".join(lex.concorrentes))
    return {
        "nivel": nivel,
        "score": round(_clamp(score)),
        "score_modelo": None if params is None else round(100 * x["c_reclama_max"]),
        "score_lexico": round(score_lex),
        "sinais": sinais_txt,
        "evidencias": evid[:3],
    }


# ──────────────────────────────────────────────────────────────────────────────
# Oportunidade comercial
# ──────────────────────────────────────────────────────────────────────────────

def calcular_oportunidade(lex: SinaisLexico, saida: SaidaModelo, cfg: ConfigPipeline, texto: str) -> dict:
    x = sinais(lex, saida)
    score_lex = _clamp(5 + 25 * min(2, len(lex.interesse)) + (25 if lex.budget else 0)
                       + 10 * min(2, len(lex.venda_cruzada)))
    params = _params(cfg, "oportunidade") if saida.zs_oport else None

    evid: list[dict] = []
    if params is None:
        nivel = C.regras_sozinhas(x, "oportunidade")
        score = score_lex
    else:
        nivel = C.prever(x, "oportunidade", params)
        score = C.nota(x, "oportunidade", params)
        if nivel != "BAIXA":
            par_neg = _ranking(saida.zs_oport, saida.trechos_zero_shot, CONTRASTE_OPORT["o_negocia"])
            par_int = _ranking(saida.zs_oport, saida.trechos_zero_shot, CONTRASTE_OPORT["o_interesse"])
            evid = _evidencias(par_neg, 0.25, 1) + _evidencias(par_int, 0.25, 1)

    for termo in (lex.interesse[:1] if cfg.modo != "modelo" else []):
        evid.insert(0, {"trecho": _trecho_ao_redor(texto, termo), "probabilidade": None, "origem": f"léxico: “{termo}”"})
    return {
        "nivel": nivel,
        "score": round(_clamp(score)),
        "score_modelo": None if params is None else round(100 * max(x["o_negocia_max"], x["o_interesse_max"])),
        "score_lexico": round(score_lex),
        "budget": lex.budget,
        "venda_cruzada": [{"produto": p, "sinal": t} for p, t in lex.venda_cruzada.items()],
        "evidencias": evid[:3],
    }


# ──────────────────────────────────────────────────────────────────────────────
# Produto
# ──────────────────────────────────────────────────────────────────────────────

def identificar_produtos(lex: SinaisLexico, saida: SaidaModelo, cfg: ConfigPipeline, texto: str) -> list[dict]:
    """
    Nome citado (confiança 0,99) + contexto da conversa (sinais de venda cruzada, 0,6).
    Inferência pelo modelo só com cfg.inferir_produto_modelo — desligada por padrão porque
    acertou o produto em só 6 de 19 reuniões na validação.
    """
    achados: dict[str, dict] = {}
    for prod, termo in lex.produtos_citados.items():
        achados[prod] = {"produto": prod, "confianca": 0.99, "origem": "citado na reunião",
                         "evidencia": _trecho_ao_redor(texto, termo, 80)}

    if saida.zero_shot_produto and cfg.inferir_produto_modelo and cfg.modo != "heuristico":
        hip_para_prod = {h: p for p, h in PRODUTOS.items()}
        melhor: dict[str, tuple[float, Trecho]] = {}
        venceu: set[str] = set()
        for probs, t in zip(saida.zero_shot_produto, saida.trechos_produto):
            so_prod = {hip_para_prod[h]: v for h, v in probs.items() if h in hip_para_prod}
            if not so_prod:
                continue
            venceu.add(max(so_prod, key=so_prod.get))
            for prod, v in so_prod.items():
                if v > melhor.get(prod, (0.0, t))[0]:
                    melhor[prod] = (v, t)
        for prod, (v, t) in melhor.items():
            if prod in achados or v < cfg.limiar_produto or prod not in venceu:
                continue
            achados[prod] = {"produto": prod, "confianca": round(v, 3), "origem": "inferido pelo modelo",
                             "evidencia": _resumir(t.texto, 200)}

    for prod, termo in lex.venda_cruzada.items():
        achados.setdefault(prod, {"produto": prod, "confianca": 0.6, "origem": "contexto da conversa",
                                  "evidencia": _trecho_ao_redor(texto, termo, 80)})

    return sorted(achados.values(), key=lambda d: -d["confianca"])


# ──────────────────────────────────────────────────────────────────────────────
# Indicadores compostos
# ──────────────────────────────────────────────────────────────────────────────

def indicadores_compostos(sent: dict, churn: dict, oport: dict) -> dict:
    """
    Saúde do cliente (0-100) = 50% (100 − churn) + 30% sentimento reescalado + 20% oportunidade.
    Churn pesa mais porque perder a conta custa mais do que deixar de vender um módulo.
    """
    sent_0_100 = (sent["score"] + 1) * 50
    saude = round(_clamp(0.5 * (100 - churn["score"]) + 0.3 * sent_0_100 + 0.2 * oport["score"]))
    if churn["nivel"] == "ALTO" or oport["nivel"] == "ALTA":
        prioridade = "Alta"
    elif churn["nivel"] == "MEDIO" or oport["nivel"] == "MEDIA" or sent["rotulo"] == "NEGATIVO":
        prioridade = "Média"
    else:
        prioridade = "Baixa"

    # Compatível com o enum Sentimento do back-end Java
    if churn["nivel"] == "ALTO":
        hermes = "CRITICO"
    elif oport["nivel"] == "ALTA" and sent["rotulo"] != "NEGATIVO":
        hermes = "OPORTUNIDADE_COMERCIAL"
    else:
        hermes = sent["rotulo"]
    return {"saude_cliente": saude, "prioridade": prioridade, "sentimento_hermes": hermes}
