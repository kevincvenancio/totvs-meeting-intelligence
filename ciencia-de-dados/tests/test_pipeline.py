"""
Testes do pipeline. Rodam sem baixar nada: os modelos do Hugging Face são trocados
por modelos falsos determinísticos, que respondem pelo conteúdo do trecho.

    pytest -q
"""
from __future__ import annotations

import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from hermes_ia import ConfigPipeline, HermesPipeline  # noqa: E402
from hermes_ia import lexico, processamento  # noqa: E402
from hermes_ia.config import CONTRASTE_CHURN, CONTRASTE_OPORT, INTENCOES, PRODUTOS  # noqa: E402

#: Calibração fixa para os testes (independente do calibracao.json real)
CALIB_TESTE = {
    "churn": {
        "hibrido": {"formulacao": "ajuste_regras", "limiar_sobe": 0.4, "limiar_desce": 0.3},
        "modelo": {"formulacao": "pontuacao", "pesos_modelo": [1.0], "peso_regras": 0.0,
                   "limiar_medio": 0.3, "limiar_alto": 0.6},
    },
    "oportunidade": {
        "hibrido": {"formulacao": "pontuacao", "pesos_modelo": [1.0, 1.0], "peso_regras": 0.2,
                    "limiar_medio": 0.5, "limiar_alto": 0.9},
        "modelo": {"formulacao": "pontuacao", "pesos_modelo": [1.0, 1.0], "peso_regras": 0.0,
                   "limiar_medio": 0.5, "limiar_alto": 0.9},
    },
    "sentimento": {
        "hibrido": {"formulacao": "pontuacao", "pesos_modelo": [1.0], "peso_regras": 0.2,
                    "limiar_medio": -0.3, "limiar_alto": 0.3},
        "modelo": {"formulacao": "pontuacao", "pesos_modelo": [1.0], "peso_regras": 0.0,
                   "limiar_medio": -0.3, "limiar_alto": 0.3},
    },
}


def cfg(**kw) -> ConfigPipeline:
    return ConfigPipeline(calibracao=CALIB_TESTE, **kw)


class SentimentoFalso:
    def prever(self, textos):
        out = []
        for t in textos:
            low = t.lower()
            if any(w in low for w in ("péssim", "frustr", "decepcion", "vai embora", "não funciona")):
                out.append({"positivo": 0.05, "neutro": 0.15, "negativo": 0.80})
            elif any(w in low for w in ("ótim", "excelente", "gostei", "satisfeit")):
                out.append({"positivo": 0.85, "neutro": 0.10, "negativo": 0.05})
            else:
                out.append({"positivo": 0.10, "neutro": 0.80, "negativo": 0.10})
        return out


class ZeroShotFalso:
    """Simula um NLI: alta probabilidade quando o trecho contém pistas da hipótese."""
    PISTAS = {
        INTENCOES["cancelamento"]: ("multa para encerrar", "vai embora", "não vamos renovar"),
        INTENCOES["insatisfacao"]: ("péssim", "erro", "ninguém responde"),
        INTENCOES["interesse"]: ("quero contratar", "gostaria de levar", "interesse"),
        INTENCOES["negociacao"]: ("proposta", "orçamento", "r$"),
        INTENCOES["concorrencia"]: ("outras duas apresentações", "concorrente"),
        CONTRASTE_CHURN["c_reclama"]: ("péssim", "erro", "ninguém responde", "multa para encerrar", "vai embora"),
        CONTRASTE_OPORT["o_negocia"]: ("proposta", "orçamento", "r$"),
        PRODUTOS["TOTVS RH / Folha"]: ("folha",),
        PRODUTOS["TOTVS Agora (Ponto)"]: ("jornada", "horas extras"),
    }

    def __init__(self):
        self.chamadas = 0

    def prever(self, textos, rotulos, template, multi_label=True):
        self.chamadas += 1
        assert "{}" in template
        out = []
        for t in textos:
            low = t.lower()
            d = {r: (0.93 if any(p in low for p in self.PISTAS.get(r, ())) else 0.04) for r in rotulos}
            if not multi_label:  # contraste: softmax entre as hipóteses
                soma = sum(d.values())
                d = {k: v / soma for k, v in d.items()}
            out.append(d)
        return out


CHURN_IMPLICITO = """
[LOCUTOR 1]: Oi, Fernanda, obrigado por receber a gente. Como estão as coisas com a folha?
[LOCUTOR 2]: Péssimas. Em outubro a folha saiu com erro para trezentos funcionários. Meu diretor pediu para
eu levantar quanto custaria a multa para encerrar o relacionamento com vocês.
[LOCUTOR 1]: Nossa, eu não sabia.
[LOCUTOR 2]: A gente já recebeu outras duas apresentações. Se não resolver até o fim do mês, a gente vai embora.
"""

FALSO_POSITIVO_LEXICO = """
[LOCUTOR 1]: Bom dia, Ricardo. Vou te mostrar como ficaria o faturamento no Protheus.
[LOCUTOR 2]: Bom dia. A gente já decidiu: vamos cancelar o contrato com o fornecedor atual no fim do ano.
Gostei muito da demonstração. O orçamento aprovado é de R$ 40 mil. Quero contratar, manda a proposta até sexta.
[LOCUTOR 1]: Perfeito, envio na quinta.
"""


@pytest.fixture
def hibrido():
    return HermesPipeline(cfg(modo="hibrido"), SentimentoFalso(), ZeroShotFalso())


def test_saida_tem_todos_os_campos_pedidos(hibrido):
    r = hibrido.analisar(CHURN_IMPLICITO)
    for campo in ("produtos_identificados", "sentimento", "risco_churn", "oportunidade_comercial",
                  "termos_principais", "recomendacoes", "indicadores"):
        assert campo in r
    assert 0 <= r["indicadores"]["saude_cliente"] <= 100
    assert r["recomendacoes"], "sempre há ao menos uma recomendação"


def test_modelo_pega_churn_que_o_lexico_nao_pega(hibrido):
    """Sem frase explícita, as regras dizem BAIXO; o modelo vê a insatisfação e sobe um nível."""
    heur = HermesPipeline(ConfigPipeline(modo="heuristico")).analisar(CHURN_IMPLICITO)
    hib = hibrido.analisar(CHURN_IMPLICITO)
    assert heur["risco_churn"]["nivel"] == "BAIXO"
    assert hib["risco_churn"]["nivel"] == "MEDIO"
    assert hib["risco_churn"]["evidencias"][0]["origem"] == "modelo"
    assert any(r["responsavel"] == "Customer Success" for r in hib["recomendacoes"])


def test_hibrido_corrige_falso_positivo_do_lexico(hibrido):
    heur = HermesPipeline(ConfigPipeline(modo="heuristico")).analisar(FALSO_POSITIVO_LEXICO)
    hib = hibrido.analisar(FALSO_POSITIVO_LEXICO)
    assert heur["risco_churn"]["nivel"] == "ALTO"          # léxico vê "cancelar o contrato"
    assert hib["risco_churn"]["nivel"] != "ALTO"           # modelo não vê insatisfação: desce um nível
    assert hib["oportunidade_comercial"]["nivel"] == "ALTA"
    assert hib["oportunidade_comercial"]["budget"] == "R$ 40 mil"


def test_produto_inferido_pelo_modelo_so_quando_ligado():
    padrao = HermesPipeline(cfg(), SentimentoFalso(), ZeroShotFalso()).analisar(CHURN_IMPLICITO)
    assert all(p["origem"] != "inferido pelo modelo" for p in padrao["produtos_identificados"])
    ligado = HermesPipeline(cfg(inferir_produto_modelo=True), SentimentoFalso(), ZeroShotFalso())
    produtos = {p["produto"]: p for p in ligado.analisar(CHURN_IMPLICITO)["produtos_identificados"]}
    assert produtos["TOTVS RH / Folha"]["origem"] == "inferido pelo modelo"


def test_produto_citado_tem_prioridade(hibrido):
    r = hibrido.analisar(FALSO_POSITIVO_LEXICO)
    assert r["produto_principal"] == "TOTVS Protheus (ERP)"
    assert r["produtos_identificados"][0]["origem"] == "citado na reunião"


def test_modo_heuristico_nao_carrega_modelo():
    r = HermesPipeline(ConfigPipeline(modo="heuristico")).analisar(CHURN_IMPLICITO)
    assert r["modelos"] is None
    assert r["risco_churn"]["score_modelo"] is None


def test_transcricao_curta_pede_revisao(hibrido):
    r = hibrido.analisar("[LOCUTOR 1]: Alô? [LOCUTOR 2]: Caiu a ligação, vamos remarcar.")
    assert "Revisar" in r["recomendacoes"][0]["acao"]


def test_trechos_longos_respeitam_limite_e_teto():
    fala = "A folha de pagamento tem muitos problemas. " * 400  # ~2.800 palavras sem quebra de locutor
    proc = processamento.processar("[LOCUTOR 1]: oi\n[LOCUTOR 2]: " + fala, palavras_por_trecho=180)
    assert all(t.palavras <= 180 for t in proc.trechos)
    sel = processamento.selecionar_trechos(proc.trechos, 16)
    assert len(sel) == 16
    assert [t.indice for t in sel] == sorted(t.indice for t in sel)


def test_teto_de_trechos_no_modelo():
    zs = ZeroShotFalso()
    pipe = HermesPipeline(cfg(max_trechos_zero_shot=5), SentimentoFalso(), zs)
    r = pipe.analisar("[LOCUTOR 1]: " + "Precisamos melhorar a jornada de trabalho. " * 600)
    assert r["processamento"]["trechos_avaliados_zero_shot"] == 5


def test_corrige_acentuacao_corrompida():
    assert processamento.corrigir_codificacao("gestÃ£o de folha") == "gestão de folha"
    assert processamento.corrigir_codificacao("gestão") == "gestão"


def test_cabecalho_e_formato_nome():
    proc = processamento.processar("---\nCliente: Alfa\nData: 2025-03-10\n---\nVendedor: Oi, tudo bem?\nCliente: Tudo.")
    assert proc.metadados["cliente"] == "Alfa"
    assert {f.papel for f in proc.falas} == {"totvs", "cliente"}


def test_budget_igual_ao_back_end_java():
    assert lexico.encontrar_valor_financeiro("temos 3 mil colaboradores") is None
    assert lexico.encontrar_valor_financeiro("o orçamento é de 3 milhões") == "3 milhões"
    assert lexico.encontrar_valor_financeiro("custa R$ 1.500,00 por mês") == "R$ 1.500,00"


def test_termos_principais_sem_stopwords():
    termos = [t["termo"] for t in processamento.termos_principais(
        "A folha de pagamento atrasou. A folha de pagamento tem erro. Então, né, a gente precisa resolver a folha de pagamento.")]
    assert termos[0] == "folha pagamento"
    assert "então" not in termos and "gente" not in termos


def test_conjunto_de_validacao_roda_inteiro(hibrido):
    import json
    arq = Path(__file__).resolve().parents[1] / "validacao" / "novas_transcricoes.jsonl"
    casos = [json.loads(l) for l in arq.read_text(encoding="utf-8").splitlines() if l.strip()]
    assert len(casos) >= 20
    for c in casos:
        r = hibrido.analisar(c["transcricao"])
        assert r["risco_churn"]["nivel"] in {"ALTO", "MEDIO", "BAIXO"}


def test_lote_igual_a_individual_e_chama_modelo_poucas_vezes():
    import json
    arq = Path(__file__).resolve().parents[1] / "validacao" / "novas_transcricoes.jsonl"
    textos = [json.loads(l)["transcricao"] for l in arq.read_text(encoding="utf-8").splitlines() if l.strip()]
    zs = ZeroShotFalso()
    pipe = HermesPipeline(cfg(), SentimentoFalso(), zs)
    lote = pipe.analisar_lote(textos)
    assert zs.chamadas == 2  # contraste de churn + contraste de oportunidade, para as 20 reuniões juntas
    individuais = [HermesPipeline(cfg(), SentimentoFalso(), ZeroShotFalso()).analisar(t) for t in textos]
    for a, b in zip(lote, individuais):
        a.pop("tempo_ms"), b.pop("tempo_ms")
        assert a == b


def test_custo_por_reuniao_e_limitado():
    """Uma reunião de ~12 mil palavras não pode gerar mais pares do que o teto."""
    class Contador(ZeroShotFalso):
        pares = 0
        def prever(self, textos, rotulos, template, multi_label=True):
            Contador.pares += len(textos) * len(rotulos)
            return super().prever(textos, rotulos, template, multi_label)
    c = cfg()
    pipe = HermesPipeline(c, SentimentoFalso(), Contador())
    pipe.analisar("[LOCUTOR 1]: oi\n[LOCUTOR 2]: " + "A folha de pagamento atrasou de novo este mês. " * 1500)
    teto = c.max_trechos_zero_shot * (len(CONTRASTE_CHURN) + len(CONTRASTE_OPORT))
    assert Contador.pares <= teto == 64


def test_calibracao_e_por_modelo():
    from hermes_ia import calibracao as C
    real = C.carregar("MoritzLaurer/multilingual-MiniLMv2-L6-mnli-xnli")
    assert set(real) >= {"churn", "oportunidade", "sentimento"}
    assert C.carregar("modelo-que-nao-foi-calibrado") == {}


def test_sem_calibracao_cai_nas_regras():
    pipe = HermesPipeline(ConfigPipeline(calibracao={}), SentimentoFalso(), ZeroShotFalso())
    r = pipe.analisar(FALSO_POSITIVO_LEXICO)
    assert r["risco_churn"]["nivel"] == "ALTO"  # igual às regras
