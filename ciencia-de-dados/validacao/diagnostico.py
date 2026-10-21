"""
DIAGNÓSTICO DO MODELO — exporta as probabilidades brutas do zero-shot e do sentimento
para cada transcrição rotulada, em duas formulações:

  multi    — cada hipótese avaliada sozinha (como o pipeline faz hoje);
  contraste — hipóteses que competem entre si (softmax), incluindo opções "neutras":
              o modelo precisa escolher entre "quer cancelar", "reclama mas fica",
              "está satisfeito" e "assunto técnico".

O arquivo gerado (validacao/resultados/diagnostico.csv) é usado por calibrar.py para
escolher a formulação e os limiares com validação cruzada.

Uso (dentro de ciencia-de-dados, com o .venv ativo):
    python validacao/diagnostico.py                    # zero-shot rápido (MiniLMv2)
    python validacao/diagnostico.py --perfil preciso   # zero-shot preciso (mDeBERTa-v3)

Depois:
    python validacao/calibrar.py validacao/resultados/diagnostico-rapido.csv
"""
from __future__ import annotations

import csv
import json
import sys
import time
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(RAIZ))

from hermes_ia import ConfigPipeline, HermesPipeline  # noqa: E402
from hermes_ia import processamento as P  # noqa: E402
from hermes_ia.config import (CONTRASTE_CHURN, CONTRASTE_OPORT, INTENCOES, PERFIS_ZERO_SHOT,  # noqa: E402
                              PRODUTOS, TEMPLATE_HIPOTESE)
CONTRASTE_PRODUTO_NENHUM = "o assunto não é nenhum sistema específico"


def resumo(valores: list[float]) -> tuple[float, float, float]:
    """(máximo, média, média dos 2 maiores)"""
    if not valores:
        return 0.0, 0.0, 0.0
    v = sorted(valores, reverse=True)
    return v[0], sum(v) / len(v), sum(v[:2]) / len(v[:2])


def main() -> None:
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--perfil", choices=list(PERFIS_ZERO_SHOT), default="rapido")
    args = ap.parse_args()
    arq = Path(__file__).with_name("novas_transcricoes.jsonl")
    casos = [json.loads(l) for l in arq.read_text(encoding="utf-8").splitlines() if l.strip()]
    pipe = HermesPipeline(ConfigPipeline(modelo_zero_shot=PERFIS_ZERO_SHOT[args.perfil]))
    cfg = pipe.cfg
    print(f"Modelos: {cfg.modelo_zero_shot} | {cfg.modelo_sentimento}")
    linhas = []
    t0 = time.perf_counter()
    for c in casos:
        proc = P.processar(c["transcricao"], cfg.palavras_por_trecho)
        lex = pipe.sinais_lexico(proc)
        tz = [t.texto for t in P.selecionar_trechos(proc.trechos, cfg.max_trechos_zero_shot)]
        tp = [t.texto for t in P.selecionar_trechos_produto(proc.trechos, cfg.max_trechos_produto)]
        ts = P.selecionar_trechos(proc.trechos, cfg.max_trechos_sentimento, somente_cliente=True)
        r = c["rotulos"]
        linha = {"id": c["id"], "modelo_zero_shot": cfg.modelo_zero_shot, "churn": r["churn"], "oportunidade": r["oportunidade"],
                 "sentimento": r["sentimento"], "produtos": "|".join(r["produtos"]),
                 "lex_churn_alto": int(bool(lex.churn_alto)), "lex_churn_medio": int(bool(lex.churn_medio)),
                 "lex_concorrente": len(lex.concorrentes), "lex_interesse": len(lex.interesse),
                 "lex_budget": int(bool(lex.budget)), "lex_venda_cruzada": len(lex.venda_cruzada),
                 "lex_pos": len(lex.positivas), "lex_neg": len(lex.negativas), "lex_dores": len(lex.dores),
                 "n_trechos": len(proc.trechos)}

        # 1) formulação atual (multi_label)
        multi = pipe.zero_shot.prever(tz, list(INTENCOES.values()), TEMPLATE_HIPOTESE)
        for chave, hip in INTENCOES.items():
            mx, md, t2 = resumo([p[hip] for p in multi])
            linha[f"m_{chave}_max"], linha[f"m_{chave}_media"], linha[f"m_{chave}_top2"] = mx, md, t2

        # 2) formulações por contraste (softmax entre opções concorrentes)
        for grupo in (CONTRASTE_CHURN, CONTRASTE_OPORT):
            soft = pipe.zero_shot.prever(tz, list(grupo.values()), TEMPLATE_HIPOTESE, multi_label=False)
            for chave, hip in grupo.items():
                mx, md, t2 = resumo([p[hip] for p in soft])
                linha[f"{chave}_max"], linha[f"{chave}_media"], linha[f"{chave}_top2"] = mx, md, t2

        # 3) produtos: multi e contraste (com a opção "nenhum")
        pm = pipe.zero_shot.prever(tp, list(PRODUTOS.values()), TEMPLATE_HIPOTESE)
        ps = pipe.zero_shot.prever(tp, list(PRODUTOS.values()) + [CONTRASTE_PRODUTO_NENHUM], TEMPLATE_HIPOTESE,
                                   multi_label=False)
        prod_multi, prod_soft = {}, {}
        for nome, hip in PRODUTOS.items():
            prod_multi[nome] = round(max((p[hip] for p in pm), default=0.0), 4)
            prod_soft[nome] = round(max((p[hip] for p in ps), default=0.0), 4)
        linha["produtos_multi"] = json.dumps(prod_multi, ensure_ascii=False)
        linha["produtos_contraste"] = json.dumps(prod_soft, ensure_ascii=False)
        linha["produtos_citados"] = "|".join(lex.produtos_citados)

        # 4) sentimento
        sp = pipe.sentimento.prever([t.texto for t in ts])
        for k in ("positivo", "neutro", "negativo"):
            linha[f"s_{k}_media"] = sum(p[k] for p in sp) / len(sp) if sp else 0.0
            linha[f"s_{k}_max"] = max((p[k] for p in sp), default=0.0)
        cli = [(p, t) for p, t in zip(sp, ts) if t.do_cliente]
        linha["s_saldo_cliente"] = (sum(p["positivo"] - p["negativo"] for p, _ in cli) / len(cli)) if cli else 0.0
        linhas.append(linha)
        print(f"  {c['id']} ok", flush=True)

    saida = Path(__file__).with_name("resultados")
    saida.mkdir(exist_ok=True)
    destino = saida / f"diagnostico-{args.perfil}.csv"
    with destino.open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(linhas[0]), delimiter=";")
        w.writeheader()
        for l in linhas:
            w.writerow({k: (round(v, 4) if isinstance(v, float) else v) for k, v in l.items()})
    print(f"\n{len(linhas)} transcrições em {time.perf_counter() - t0:.0f} s → {destino}")
    print(f"Próximo passo: python validacao/calibrar.py validacao/resultados/{destino.name}")


if __name__ == "__main__":
    main()
