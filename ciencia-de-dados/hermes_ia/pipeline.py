"""
Pipeline completo do Hermes IA:

    Transcrição → Processamento → Modelo → Indicadores → Recomendação

Uso:
    from hermes_ia import HermesPipeline
    resultado = HermesPipeline().analisar(texto_da_transcricao)
    resultados = HermesPipeline().analisar_lote([texto1, texto2, ...])   # mais rápido por reunião

Velocidade: cada reunião gera no máximo
    8 trechos × 4 hipóteses de churn + 8 trechos × 4 de oportunidade = 64 pares no zero-shot
    + 8 trechos no sentimento,
não importa o tamanho da transcrição. No lote, os trechos de todas as reuniões passam
pelo modelo juntos, em batches grandes, o que aproveita melhor a CPU.
"""
from __future__ import annotations

import time
from dataclasses import dataclass

from . import indicadores as ind
from . import lexico, processamento, recomendacao
from .config import CONTRASTE_CHURN, CONTRASTE_OPORT, PRODUTOS, TEMPLATE_HIPOTESE, ConfigPipeline
from .modelos import ClassificadorSentimento, ClassificadorZeroShot, carregar_modelos

VERSAO = "1.2.0"


@dataclass
class _Preparo:
    proc: processamento.TranscricaoProcessada
    lex: ind.SinaisLexico
    saida: ind.SaidaModelo


class HermesPipeline:
    def __init__(
        self,
        config: ConfigPipeline | None = None,
        sentimento: ClassificadorSentimento | None = None,
        zero_shot: ClassificadorZeroShot | None = None,
    ):
        self.cfg = config or ConfigPipeline()
        if self.cfg.modo != "heuristico" and (sentimento is None or zero_shot is None):
            s, z = carregar_modelos(self.cfg.modelo_sentimento, self.cfg.modelo_zero_shot, self.cfg.dispositivo,
                                    self.cfg.quantizar, self.cfg.max_tokens, self.cfg.batch_size)
            sentimento = sentimento or s
            zero_shot = zero_shot or z
        self.sentimento = sentimento
        self.zero_shot = zero_shot

    # ── Etapa 2: sinais de regra ──────────────────────────────────────────────
    @staticmethod
    def sinais_lexico(proc: processamento.TranscricaoProcessada) -> ind.SinaisLexico:
        low = proc.texto_lower
        dores, vistos = [], set()
        for termo, (desc, crit) in lexico.DORES.items():
            if termo in low and desc not in vistos:
                vistos.add(desc)
                dores.append((termo, desc, crit))
        return ind.SinaisLexico(
            churn_alto=lexico.achar(low, lexico.CHURN_ALTO),
            churn_medio=lexico.achar(low, lexico.CHURN_MEDIO),
            concorrentes=lexico.achar_dicionario(low, lexico.CONCORRENTES),
            produtos_citados=lexico.achar_dicionario(low, lexico.PRODUTOS_TOTVS),
            venda_cruzada=lexico.achar_dicionario(low, lexico.VENDA_CRUZADA),
            interesse=[s for s in lexico.SINAIS_INTERESSE if lexico.contem(low, s)],
            positivas=[s for s in lexico.POSITIVAS if lexico.contem(low, s)],
            negativas=[s for s in lexico.NEGATIVAS if lexico.contem(low, s)],
            dores=dores,
            budget=lexico.encontrar_valor_financeiro(proc.texto),
        )

    # ── Etapa 3: modelo (várias reuniões numa passada só) ─────────────────────
    def _selecionar(self, proc: processamento.TranscricaoProcessada) -> ind.SaidaModelo:
        saida = ind.SaidaModelo()
        if self.cfg.modo == "heuristico" or not proc.trechos:
            return saida
        saida.trechos_sentimento = processamento.selecionar_trechos(
            proc.trechos, self.cfg.max_trechos_sentimento, somente_cliente=True)
        saida.trechos_zero_shot = processamento.selecionar_trechos(proc.trechos, self.cfg.max_trechos_zero_shot)
        if self.cfg.inferir_produto_modelo:
            saida.trechos_produto = processamento.selecionar_trechos_produto(
                proc.trechos, self.cfg.max_trechos_produto)
        return saida

    def rodar_modelos(self, saidas: list[ind.SaidaModelo]) -> None:
        """Preenche as probabilidades de todas as reuniões com uma chamada por tarefa."""
        if self.cfg.modo == "heuristico":
            return

        def em_lote(campo_trechos: str, prever) -> list[list]:
            textos, limites = [], []
            for s in saidas:
                ts = getattr(s, campo_trechos)
                limites.append((len(textos), len(textos) + len(ts)))
                textos.extend(t.texto for t in ts)
            probs = prever(textos) if textos else []
            return [probs[a:b] for a, b in limites]

        for s, p in zip(saidas, em_lote("trechos_sentimento", self.sentimento.prever)):
            s.sentimento = p
        # Contraste (softmax): as hipóteses competem entre si — ver config.CONTRASTE_CHURN
        rot_churn = list(CONTRASTE_CHURN.values())
        for s, p in zip(saidas, em_lote("trechos_zero_shot", lambda tx: self.zero_shot.prever(
                tx, rot_churn, TEMPLATE_HIPOTESE, multi_label=False))):
            s.zs_churn = p
        rot_oport = list(CONTRASTE_OPORT.values())
        for s, p in zip(saidas, em_lote("trechos_zero_shot", lambda tx: self.zero_shot.prever(
                tx, rot_oport, TEMPLATE_HIPOTESE, multi_label=False))):
            s.zs_oport = p
        if self.cfg.inferir_produto_modelo:
            rot_prod = list(PRODUTOS.values())
            for s, p in zip(saidas, em_lote("trechos_produto",
                                            lambda tx: self.zero_shot.prever(tx, rot_prod, TEMPLATE_HIPOTESE))):
                s.zero_shot_produto = p

    # ── Pipeline completo ─────────────────────────────────────────────────────
    def analisar(self, transcricao: str) -> dict:
        return self.analisar_lote([transcricao])[0]

    def analisar_lote(self, transcricoes: list[str]) -> list[dict]:
        t0 = time.perf_counter()

        preparos = []                                                                      # 1. processamento
        for texto in transcricoes:
            proc = processamento.processar(texto, self.cfg.palavras_por_trecho)
            preparos.append(_Preparo(proc, self.sinais_lexico(proc), self._selecionar(proc)))

        self.rodar_modelos([p.saida for p in preparos])                                    # 2. modelo

        resultados = [self._montar(p) for p in preparos]                                   # 3 e 4
        ms = round((time.perf_counter() - t0) * 1000 / max(1, len(transcricoes)))
        for r in resultados:
            r["tempo_ms"] = ms
        return resultados

    def _montar(self, p: _Preparo) -> dict:
        proc, lex, saida = p.proc, p.lex, p.saida
        sent = ind.calcular_sentimento(lex, saida, self.cfg)                               # 3. indicadores
        churn = ind.calcular_churn(lex, saida, self.cfg, proc.texto)
        oport = ind.calcular_oportunidade(lex, saida, self.cfg, proc.texto)
        produtos = ind.identificar_produtos(lex, saida, self.cfg, proc.texto)
        compostos = ind.indicadores_compostos(sent, churn, oport)

        resultado = {
            "versao": VERSAO,
            "modo": self.cfg.modo,
            "modelos": None if self.cfg.modo == "heuristico" else {
                "sentimento": self.cfg.modelo_sentimento,
                "zero_shot": self.cfg.modelo_zero_shot,
            },
            "processamento": {
                "palavras": proc.qtd_palavras,
                "locutores": proc.qtd_locutores,
                "papeis": {f.locutor: f.papel for f in proc.falas},
                "trechos": len(proc.trechos),
                "trechos_avaliados_sentimento": len(saida.trechos_sentimento),
                "trechos_avaliados_zero_shot": len(saida.trechos_zero_shot),
                "trechos_avaliados_produto": len(saida.trechos_produto),
                "metadados": proc.metadados,
            },
            "produtos_identificados": produtos,
            "produto_principal": produtos[0]["produto"] if produtos else None,
            "concorrentes": list(lex.concorrentes),
            "sentimento": sent,
            "risco_churn": churn,
            "oportunidade_comercial": oport,
            "dores": [{"descricao": d, "criticidade": c, "termo": t} for t, d, c in lex.dores],
            "termos_principais": processamento.termos_principais(proc.texto, self.cfg.qtd_termos),
            "indicadores": compostos,
        }
        resultado["recomendacoes"] = recomendacao.gerar(resultado)                         # 4. recomendação
        return resultado
