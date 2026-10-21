"""
VALIDAÇÃO COM NOVAS TRANSCRIÇÕES

Roda o pipeline nos três modos sobre um conjunto rotulado que o motor nunca viu e
compara as previsões com os rótulos dados por uma pessoa:

    heuristico  → baseline (regras do back-end Java, o "modelo" antes da Sprint 4)
    modelo      → só os modelos do Hugging Face
    hibrido     → modelo + regras (modelo final proposto)

Métricas: acurácia e F1-macro por tarefa (churn, oportunidade, sentimento),
recall do churn ALTO (a métrica de negócio mais cara de errar) e precisão / recall /
F1 dos produtos identificados.

Uso (dentro da pasta ciencia-de-dados):
    python validacao/validar.py                        # os três modos
    python validacao/validar.py --modos heuristico     # sem baixar modelos
    python validacao/validar.py --perfil preciso            # compara com o mDeBERTa
"""
from __future__ import annotations

import argparse
import csv
import json
import statistics
import sys
import time
from collections import Counter
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(RAIZ))

from hermes_ia import ConfigPipeline, HermesPipeline  # noqa: E402
from hermes_ia import calibracao as C  # noqa: E402
from hermes_ia.config import PERFIS_ZERO_SHOT  # noqa: E402

TAREFAS = {
    "churn": ["ALTO", "MEDIO", "BAIXO"],
    "oportunidade": ["ALTA", "MEDIA", "BAIXA"],
    "sentimento": ["POSITIVO", "NEUTRO", "NEGATIVO", "MISTO"],
}


# ──────────────────────────────────────────────────────────────────────────────
# Métricas (implementadas aqui para não exigir scikit-learn)
# ──────────────────────────────────────────────────────────────────────────────

def matriz_confusao(y_true: list[str], y_pred: list[str], classes: list[str]) -> dict[str, dict[str, int]]:
    m = {c: {p: 0 for p in classes} for c in classes}
    for t, p in zip(y_true, y_pred):
        if t in m and p in m[t]:
            m[t][p] += 1
    return m


def f1_por_classe(y_true, y_pred, classe) -> tuple[float, float, float]:
    tp = sum(1 for t, p in zip(y_true, y_pred) if t == classe and p == classe)
    fp = sum(1 for t, p in zip(y_true, y_pred) if t != classe and p == classe)
    fn = sum(1 for t, p in zip(y_true, y_pred) if t == classe and p != classe)
    prec = tp / (tp + fp) if tp + fp else 0.0
    rec = tp / (tp + fn) if tp + fn else 0.0
    f1 = 2 * prec * rec / (prec + rec) if prec + rec else 0.0
    return prec, rec, f1


def metricas_classificacao(y_true, y_pred, classes) -> dict:
    presentes = [c for c in classes if c in y_true]  # F1-macro só sobre classes que existem no gabarito
    f1s = [f1_por_classe(y_true, y_pred, c)[2] for c in presentes]
    return {
        "acuracia": sum(t == p for t, p in zip(y_true, y_pred)) / len(y_true),
        "f1_macro": sum(f1s) / len(f1s) if f1s else 0.0,
        "por_classe": {c: dict(zip(("precisao", "recall", "f1"), f1_por_classe(y_true, y_pred, c))) for c in presentes},
        "confusao": matriz_confusao(y_true, y_pred, classes),
    }


def metricas_produto(esperados: list[list[str]], previstos: list[list[str]]) -> dict:
    tp = fp = fn = 0
    for e, p in zip(esperados, previstos):
        e, p = set(e), set(p)
        tp += len(e & p)
        fp += len(p - e)
        fn += len(e - p)
    prec = tp / (tp + fp) if tp + fp else 0.0
    rec = tp / (tp + fn) if tp + fn else 0.0
    return {"precisao": prec, "recall": rec, "f1": 2 * prec * rec / (prec + rec) if prec + rec else 0.0}


# ──────────────────────────────────────────────────────────────────────────────
# Execução
# ──────────────────────────────────────────────────────────────────────────────

def carregar(caminho: Path) -> list[dict]:
    if caminho.suffix == ".jsonl":
        return [json.loads(l) for l in caminho.read_text(encoding="utf-8").splitlines() if l.strip()]
    # CSV: id;transcricao;churn;oportunidade;sentimento;produtos (produtos separados por "|")
    with caminho.open(encoding="utf-8") as f:
        linhas = list(csv.DictReader(f, delimiter=";"))
    return [{
        "id": l["id"], "descricao": l.get("descricao", ""), "transcricao": l["transcricao"],
        "rotulos": {"churn": l["churn"], "oportunidade": l["oportunidade"], "sentimento": l["sentimento"],
                    "produtos": [p for p in l.get("produtos", "").split("|") if p]},
    } for l in linhas]


def prever(pipe: HermesPipeline, casos: list[dict]) -> tuple[list[dict], list[float]]:
    saidas, tempos = [], []
    for c in casos:
        t0 = time.perf_counter()
        r = pipe.analisar(c["transcricao"])
        tempos.append(time.perf_counter() - t0)
        saidas.append({
            "churn": r["risco_churn"]["nivel"],
            "churn_score": r["risco_churn"]["score"],
            "oportunidade": r["oportunidade_comercial"]["nivel"],
            "oportunidade_score": r["oportunidade_comercial"]["score"],
            "sentimento": r["sentimento"]["rotulo"],
            "sentimento_score": r["sentimento"]["score"],
            "produtos": [p["produto"] for p in r["produtos_identificados"]],
        })
        print(f"  {c['id']}: churn={saidas[-1]['churn']:<5} oport={saidas[-1]['oportunidade']:<5} "
              f"sent={saidas[-1]['sentimento']:<8} ({tempos[-1]:.1f}s)", flush=True)
    return saidas, tempos


def avaliar(casos: list[dict], saidas: list[dict]) -> dict:
    res = {}
    for tarefa, classes in TAREFAS.items():
        res[tarefa] = metricas_classificacao([c["rotulos"][tarefa] for c in casos], [s[tarefa] for s in saidas], classes)
    res["churn_alto"] = dict(zip(("precisao", "recall", "f1"), f1_por_classe(
        [c["rotulos"]["churn"] for c in casos], [s["churn"] for s in saidas], "ALTO")))
    res["produtos"] = metricas_produto([c["rotulos"]["produtos"] for c in casos], [s["produtos"] for s in saidas])
    return res


def tabela_markdown(resultados: dict[str, dict], tempos: dict[str, list[float]]) -> str:
    modos = list(resultados)
    linhas = ["| Métrica | " + " | ".join(modos) + " |", "|---|" + "---|" * len(modos)]

    def lin(nome, f):
        valores = [f(resultados[m]) for m in modos]
        melhor = max(valores)
        cel = [f"**{v:.2f}**" if v == melhor and valores.count(melhor) < len(valores) else f"{v:.2f}" for v in valores]
        linhas.append(f"| {nome} | " + " | ".join(cel) + " |")

    lin("Churn — acurácia", lambda r: r["churn"]["acuracia"])
    lin("Churn — F1-macro", lambda r: r["churn"]["f1_macro"])
    lin("Churn ALTO — recall", lambda r: r["churn_alto"]["recall"])
    lin("Churn ALTO — precisão", lambda r: r["churn_alto"]["precisao"])
    lin("Oportunidade — acurácia", lambda r: r["oportunidade"]["acuracia"])
    lin("Oportunidade — F1-macro", lambda r: r["oportunidade"]["f1_macro"])
    lin("Sentimento — acurácia", lambda r: r["sentimento"]["acuracia"])
    lin("Sentimento — F1-macro", lambda r: r["sentimento"]["f1_macro"])
    lin("Produto — F1", lambda r: r["produtos"]["f1"])
    linhas.append("| Tempo médio por reunião (s) | " + " | ".join(f"{statistics.mean(tempos[m]):.2f}" for m in modos) + " |")
    return "\n".join(linhas)


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--arquivo", default=str(Path(__file__).with_name("novas_transcricoes.jsonl")))
    ap.add_argument("--modos", nargs="+", default=["heuristico", "modelo", "hibrido"],
                    choices=["heuristico", "modelo", "hibrido"])
    ap.add_argument("--perfil", choices=list(PERFIS_ZERO_SHOT), default="rapido",
                    help="rapido = MiniLMv2-L6 (padrão); preciso = mDeBERTa-v3-base (~10x mais lento)")
    ap.add_argument("--saida", default=str(Path(__file__).with_name("resultados")))
    args = ap.parse_args()

    casos = carregar(Path(args.arquivo))
    print(f"{len(casos)} transcrições rotuladas em {args.arquivo}")
    print("Distribuição do gabarito:", {t: dict(Counter(c['rotulos'][t] for c in casos)) for t in TAREFAS})

    saida = Path(args.saida)
    saida.mkdir(parents=True, exist_ok=True)
    resultados, tempos, previsoes = {}, {}, {}
    modelos_compartilhados = None
    for modo in args.modos:
        print(f"\n== modo {modo} ==")
        cfg = ConfigPipeline(modo=modo)
        cfg.modelo_zero_shot = PERFIS_ZERO_SHOT[args.perfil]
        if modo == "heuristico":
            pipe = HermesPipeline(cfg)
        else:
            if modelos_compartilhados is None:  # carrega os modelos uma vez só
                modelos_compartilhados = HermesPipeline(cfg)
            pipe = HermesPipeline(cfg, modelos_compartilhados.sentimento, modelos_compartilhados.zero_shot)
            pipe.analisar(casos[0]["transcricao"])  # aquece (download + primeira inferência fora do cronômetro)
        previsoes[modo], tempos[modo] = prever(pipe, casos)
        if modo != "heuristico":
            t0 = time.perf_counter()
            pipe.analisar_lote([c["transcricao"] for c in casos])
            print(f"  em lote: {(time.perf_counter() - t0) / len(casos):.2f} s por reunião")
        resultados[modo] = avaliar(casos, previsoes[modo])

    tabela = tabela_markdown(resultados, tempos)
    print("\n" + tabela)

    # Calibração: a tabela acima usa limiares ajustados NESTAS transcrições (otimista).
    # A medida honesta é a da validação cruzada (LOOCV), gravada pela calibração.
    calib = C.carregar(PERFIS_ZERO_SHOT[args.perfil])
    bloco_loocv = ""
    if calib and any(m != "heuristico" for m in args.modos):
        linhas_cv = ["| Tarefa | Regras | Modelo | Híbrido |", "|---|---|---|---|"]
        for tarefa in ("churn", "oportunidade", "sentimento"):
            cv = calib[tarefa]["loocv"]
            cel = lambda par: f"{par[0]:.2f} / {par[1]:.2f}"
            linhas_cv.append(f"| {tarefa} | {cel(cv['regras'])} | {cel(cv['modelo'])} | {cel(cv['hibrido'])} |")
        bloco_loocv = (
            f"\n\n## Validação cruzada (LOOCV) — a medida honesta\n\n"
            f"Os limiares do modelo foram calibrados com estas {calib['n']} transcrições, então a tabela acima "
            f"é otimista para as colunas `modelo` e `hibrido`. Abaixo, acurácia / F1-macro em leave-one-out: "
            f"cada reunião avaliada por uma calibração feita sem ela (sentimento em 3 classes, MISTO = NEUTRO).\n\n"
            + "\n".join(linhas_cv))
        print("\nATENÇÃO: modelo e híbrido foram calibrados nestas mesmas transcrições (tabela otimista).")
        print("Medida honesta (LOOCV), acurácia / F1-macro:\n" + "\n".join(linhas_cv))

    (saida / "metricas.json").write_text(json.dumps(resultados, ensure_ascii=False, indent=2), encoding="utf-8-sig")
    (saida / "metricas.md").write_text(
        f"# Validação com {len(casos)} novas transcrições\n\n{tabela}\n\n"
        "Melhor valor de cada linha em negrito. Matrizes de confusão em `metricas.json`." + bloco_loocv + "\n",
        encoding="utf-8-sig")  # BOM: PowerShell e Excel mostram os acentos certos
    with (saida / "predicoes.csv").open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f, delimiter=";")
        w.writerow(["id", "descricao", "modo", "churn_real", "churn_previsto", "churn_score",
                    "oport_real", "oport_prevista", "oport_score", "sent_real", "sent_previsto",
                    "sent_score", "produtos_reais", "produtos_previstos"])
        for modo, prev in previsoes.items():
            for c, p in zip(casos, prev):
                r = c["rotulos"]
                w.writerow([c["id"], c.get("descricao", ""), modo, r["churn"], p["churn"], p["churn_score"],
                            r["oportunidade"], p["oportunidade"], p["oportunidade_score"],
                            r["sentimento"], p["sentimento"], p["sentimento_score"],
                            "|".join(r["produtos"]), "|".join(p["produtos"])])
    print(f"\nResultados gravados em {saida}/ (metricas.md, metricas.json, predicoes.csv)")


if __name__ == "__main__":
    main()
