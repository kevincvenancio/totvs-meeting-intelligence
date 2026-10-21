"""
Etapa de MODELO do pipeline — wrappers dos modelos do Hugging Face.

Dois modelos pré-treinados, usados sem fine-tuning (não existe base rotulada da TOTVS):

* **Sentimento** (`text-classification`): devolve P(positivo), P(neutro), P(negativo)
  para cada trecho.
* **Zero-shot** (`zero-shot-classification`, NLI): para cada trecho, devolve a
  probabilidade de cada hipótese em português ser verdadeira — usado para churn,
  oportunidade, negociação, concorrência e produto.

Os modelos são carregados sob demanda (a primeira chamada baixa os pesos para o
cache do Hugging Face, ~/.cache/huggingface) e reaproveitados entre análises.

Velocidade em CPU: as camadas lineares são quantizadas para int8 (quantização
dinâmica do PyTorch) e cada entrada é limitada a `max_tokens`. Medido na
arquitetura real, isso leva o zero-shot rápido de 33 para ~10 ms por par.
"""
from __future__ import annotations

import logging
import warnings
from functools import lru_cache
from typing import Protocol

log = logging.getLogger(__name__)


class ClassificadorSentimento(Protocol):
    def prever(self, textos: list[str]) -> list[dict[str, float]]: ...


class ClassificadorZeroShot(Protocol):
    def prever(self, textos: list[str], rotulos: list[str], template: str) -> list[dict[str, float]]: ...


def _dispositivo(dispositivo):
    if dispositivo is not None:
        return dispositivo
    try:
        import torch
        return 0 if torch.cuda.is_available() else -1
    except ImportError:  # pragma: no cover
        return -1


def _quantizar(pipe) -> None:
    """int8 dinâmico nas camadas Linear — só em CPU (em GPU não ajuda)."""
    try:
        import torch
        if pipe.model.device.type != "cpu":
            return
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            quant = getattr(torch.ao.quantization, "quantize_dynamic", None) or torch.quantization.quantize_dynamic
            pipe.model = quant(pipe.model, {torch.nn.Linear}, dtype=torch.qint8)
        log.info("Modelo quantizado para int8")
    except Exception as e:  # pragma: no cover — versão do torch sem suporte: segue em fp32
        log.warning("Quantização int8 indisponível (%s); usando fp32", e)


@lru_cache(maxsize=4)
def _carregar(tarefa: str, nome: str, dispositivo, quantizar: bool, max_tokens: int) -> object:
    try:
        from transformers import pipeline
    except ImportError as e:  # pragma: no cover
        raise RuntimeError(
            "Pacote 'transformers' não instalado. Rode: pip install -r requirements.txt"
        ) from e
    log.info("Carregando modelo %s (%s)…", nome, tarefa)
    pipe = pipeline(tarefa, model=nome, device=_dispositivo(dispositivo))
    # O zero-shot trunca pelo limite do tokenizador; reduzir o limite reduz o custo.
    pipe.tokenizer.model_max_length = max_tokens
    if quantizar:
        _quantizar(pipe)
    return pipe


# ──────────────────────────────────────────────────────────────────────────────
# Sentimento
# ──────────────────────────────────────────────────────────────────────────────

def _mapear_rotulo_sentimento(rotulo: str) -> str:
    r = rotulo.lower()
    if r.startswith("pos") or r in {"label_2", "4 stars", "5 stars"}:
        return "positivo"
    if r.startswith("neg") or r in {"label_0", "1 star", "2 stars"}:
        return "negativo"
    return "neutro"  # neutral, label_1, 3 stars


class SentimentoHF:
    def __init__(self, nome: str, dispositivo=None, batch_size: int = 32, quantizar: bool = True,
                 max_tokens: int = 160):
        self.nome = nome
        self.dispositivo = dispositivo
        self.batch_size = batch_size
        self.quantizar = quantizar
        self.max_tokens = max_tokens

    @property
    def _pipe(self):
        return _carregar("text-classification", self.nome, self.dispositivo, self.quantizar, self.max_tokens)

    def prever(self, textos: list[str]) -> list[dict[str, float]]:
        if not textos:
            return []
        saida = self._pipe(textos, top_k=None, truncation=True, max_length=self.max_tokens,
                           batch_size=self.batch_size)
        resultado = []
        for item in saida:
            if isinstance(item, dict):  # algumas versões devolvem só o top-1
                item = [item]
            probs = {"positivo": 0.0, "neutro": 0.0, "negativo": 0.0}
            for d in item:
                probs[_mapear_rotulo_sentimento(d["label"])] += float(d["score"])
            resultado.append(probs)
        return resultado


# ──────────────────────────────────────────────────────────────────────────────
# Zero-shot
# ──────────────────────────────────────────────────────────────────────────────

class ZeroShotHF:
    def __init__(self, nome: str, dispositivo=None, batch_size: int = 32, quantizar: bool = True,
                 max_tokens: int = 160):
        self.nome = nome
        self.dispositivo = dispositivo
        self.batch_size = batch_size
        self.quantizar = quantizar
        self.max_tokens = max_tokens

    @property
    def _pipe(self):
        return _carregar("zero-shot-classification", self.nome, self.dispositivo, self.quantizar, self.max_tokens)

    def prever(self, textos: list[str], rotulos: list[str], template: str,
               multi_label: bool = True) -> list[dict[str, float]]:
        """
        multi_label=True: cada hipótese é avaliada de forma independente (0..1).
        multi_label=False: as hipóteses competem entre si (softmax; somam 1 por trecho).
        """
        if not textos:
            return []
        saida = self._pipe(
            textos,
            candidate_labels=rotulos,
            hypothesis_template=template,
            multi_label=multi_label,
            batch_size=self.batch_size,
        )
        if isinstance(saida, dict):
            saida = [saida]
        return [dict(zip(s["labels"], map(float, s["scores"]))) for s in saida]


def carregar_modelos(nome_sentimento: str, nome_zero_shot: str, dispositivo=None, quantizar: bool = True,
                     max_tokens: int = 160, batch_size: int = 32):
    """Devolve (sentimento, zero_shot). Os pesos só são baixados na primeira previsão."""
    return (SentimentoHF(nome_sentimento, dispositivo, batch_size, quantizar, max_tokens),
            ZeroShotHF(nome_zero_shot, dispositivo, batch_size, quantizar, max_tokens))
