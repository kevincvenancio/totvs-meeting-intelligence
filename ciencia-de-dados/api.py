"""
API HTTP do Hermes IA — consumida pelo back-end Java (HermesIaClient).

    uvicorn api:app --port 8000
    curl -X POST localhost:8000/analisar -H "Content-Type: application/json" \
         -d '{"transcricao": "[LOCUTOR 1]: ...", "modo": "hibrido"}'

Documentação interativa em http://localhost:8000/docs

Na subida, os modelos do Hugging Face são carregados (e baixados, na primeira vez)
antes de o servidor aceitar requisições — assim a primeira análise pedida pelo Java
não esgota o tempo de espera. HERMES_IA_PRECARREGAR=0 desliga isso.
"""
from __future__ import annotations

import logging
import os
from contextlib import asynccontextmanager
from functools import lru_cache

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from hermes_ia import VERSAO, ConfigPipeline, HermesPipeline

log = logging.getLogger("hermes_ia.api")
MODO_PADRAO = os.getenv("HERMES_IA_MODO", "hibrido")


@lru_cache(maxsize=3)
def _pipeline(modo: str) -> HermesPipeline:
    return HermesPipeline(ConfigPipeline(modo=modo))


@asynccontextmanager
async def _ciclo_de_vida(_app: FastAPI):
    if os.getenv("HERMES_IA_PRECARREGAR", "1") != "0" and MODO_PADRAO != "heuristico":
        log.warning("Carregando modelos do Hugging Face (na primeira vez, baixa ~1 GB)…")
        _pipeline(MODO_PADRAO).analisar(
            "[LOCUTOR 1]: Bom dia, vou te mostrar a proposta. [LOCUTOR 2]: Ótimo, a folha de pagamento é manual.")
        log.warning("Modelos prontos.")
    yield


app = FastAPI(title="Hermes IA", version=VERSAO, lifespan=_ciclo_de_vida,
              description="Transcrição → Processamento → Modelo → Indicadores → Recomendação")


class Requisicao(BaseModel):
    transcricao: str = Field(..., min_length=20, description="Texto da reunião ([LOCUTOR n]: ... ou Nome: ...)")
    modo: str = Field(MODO_PADRAO, pattern="^(hibrido|modelo|heuristico)$")


class RequisicaoLote(BaseModel):
    transcricoes: list[str] = Field(..., min_length=1, max_length=32)
    modo: str = Field(MODO_PADRAO, pattern="^(hibrido|modelo|heuristico)$")


@app.get("/saude")
def saude() -> dict:
    cfg = _pipeline(MODO_PADRAO).cfg
    return {"status": "ok", "versao": VERSAO, "modo_padrao": MODO_PADRAO,
            "modelo_zero_shot": cfg.modelo_zero_shot, "modelo_sentimento": cfg.modelo_sentimento}


@app.post("/analisar")
def analisar(req: Requisicao) -> dict:
    try:
        return _pipeline(req.modo).analisar(req.transcricao)
    except RuntimeError as e:  # transformers não instalado / sem internet para baixar os pesos
        raise HTTPException(status_code=503, detail=str(e)) from e


@app.post("/analisar-lote")
def analisar_lote(req: RequisicaoLote) -> dict:
    """Várias reuniões numa chamada: os trechos de todas passam pelo modelo juntos (mais rápido)."""
    try:
        return {"resultados": _pipeline(req.modo).analisar_lote(req.transcricoes)}
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
