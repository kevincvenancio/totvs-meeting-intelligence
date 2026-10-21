"""
Linha de comando: analisa uma ou mais transcrições e imprime (ou grava) o resultado.

    python analisar.py reuniao.txt
    python analisar.py pasta/*.txt --modo heuristico --json saida.json
    python analisar.py --csv transcricoes.csv --saida resultados.csv   # CSV no formato da importação (ANON_TRANSCRICAO)
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path

from hermes_ia import ConfigPipeline, HermesPipeline
from hermes_ia.config import PERFIS_ZERO_SHOT


def resumo(nome: str, r: dict) -> str:
    c, o, s = r["risco_churn"], r["oportunidade_comercial"], r["sentimento"]
    linhas = [
        f"══ {nome} ({r['processamento']['palavras']} palavras, {r['tempo_ms']} ms, modo {r['modo']})",
        f"  Produto:       {r['produto_principal'] or '—'}"
        + (f"  (+{len(r['produtos_identificados']) - 1})" if len(r["produtos_identificados"]) > 1 else ""),
        f"  Sentimento:    {s['rotulo']} ({s['score']:+.2f})",
        f"  Risco churn:   {c['nivel']} ({c['score']}/100)",
        f"  Oportunidade:  {o['nivel']} ({o['score']}/100)" + (f" · budget {o['budget']}" if o["budget"] else ""),
        f"  Saúde:         {r['indicadores']['saude_cliente']}/100 · prioridade {r['indicadores']['prioridade']}",
        f"  Termos:        {', '.join(t['termo'] for t in r['termos_principais'][:6])}",
        "  Recomendações:",
    ]
    linhas += [f"    [{a['prioridade']}] {a['acao']} ({a['prazo']}, {a['responsavel']})" for a in r["recomendacoes"]]
    return "\n".join(linhas)


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("arquivos", nargs="*", help=".txt com uma transcrição cada")
    ap.add_argument("--csv", help="CSV de transcrições (coluna ANON_TRANSCRICAO ou transcricao)")
    ap.add_argument("--saida", help="CSV de saída (com --csv)")
    ap.add_argument("--json", help="grava a saída completa em JSON")
    ap.add_argument("--modo", default="hibrido", choices=["hibrido", "modelo", "heuristico"])
    ap.add_argument("--perfil", choices=list(PERFIS_ZERO_SHOT), default="rapido")
    ap.add_argument("--limite", type=int, default=0, help="analisa só as N primeiras linhas do CSV")
    a = ap.parse_args()

    cfg = ConfigPipeline(modo=a.modo)
    cfg.modelo_zero_shot = PERFIS_ZERO_SHOT[a.perfil]
    pipe = HermesPipeline(cfg)

    entradas: list[tuple[str, str]] = []
    for arq in a.arquivos:
        entradas.append((arq, Path(arq).read_text(encoding="utf-8", errors="replace")))
    if a.csv:
        csv.field_size_limit(sys.maxsize)
        with open(a.csv, encoding="utf-8", errors="replace") as f:
            amostra = f.read(4096)
            f.seek(0)
            dialeto = csv.Sniffer().sniff(amostra, delimiters=",;\t")
            for i, linha in enumerate(csv.DictReader(f, dialect=dialeto)):
                if a.limite and i >= a.limite:
                    break
                texto = linha.get("ANON_TRANSCRICAO") or linha.get("transcricao") or ""
                ident = linha.get("ANON_ID") or linha.get("id") or f"linha-{i + 1}"
                if texto.strip():
                    entradas.append((ident, texto))
    if not entradas:
        ap.error("informe arquivos .txt ou --csv")

    resultados = {}
    for i in range(0, len(entradas), 16):  # lotes de 16: mais rápido por reunião
        bloco = entradas[i:i + 16]
        for (nome, _), r in zip(bloco, pipe.analisar_lote([t for _, t in bloco])):
            resultados[nome] = r
            print(resumo(nome, r), flush=True)

    if a.json:
        Path(a.json).write_text(json.dumps(resultados, ensure_ascii=False, indent=2), encoding="utf-8")
    if a.saida:
        with open(a.saida, "w", encoding="utf-8", newline="") as f:
            w = csv.writer(f, delimiter=";")
            w.writerow(["id", "produto", "sentimento", "sentimento_score", "churn", "churn_score",
                        "oportunidade", "oportunidade_score", "budget", "saude", "prioridade",
                        "termos", "recomendacao_principal"])
            for nome, r in resultados.items():
                w.writerow([nome, r["produto_principal"] or "", r["sentimento"]["rotulo"], r["sentimento"]["score"],
                            r["risco_churn"]["nivel"], r["risco_churn"]["score"],
                            r["oportunidade_comercial"]["nivel"], r["oportunidade_comercial"]["score"],
                            r["oportunidade_comercial"]["budget"] or "", r["indicadores"]["saude_cliente"],
                            r["indicadores"]["prioridade"], ", ".join(t["termo"] for t in r["termos_principais"]),
                            r["recomendacoes"][0]["acao"] if r["recomendacoes"] else ""])


if __name__ == "__main__":
    main()
