"""
CALIBRAÇÃO DO MODELO — escolhe pesos e limiares a partir do diagnóstico, com
validação cruzada leave-one-out (LOOCV).

Por que calibrar: as probabilidades do zero-shot não são frequências reais. A primeira
validação mostrou que, com limiares escolhidos "no olho", o modelo marcava churn em
quase todas as reuniões. Aqui os limiares são aprendidos com dados rotulados.

Como evitar "decorar" as 20 transcrições: para cada reunião, a calibração é feita nas
outras 19 e testada nela (LOOCV). As métricas impressas são essas — de reuniões que a
calibração não viu. Os parâmetros finais (ajustados nas 20) vão para
hermes_ia/calibracao.json, que o pipeline lê. As fórmulas ficam em hermes_ia/calibracao.py,
o mesmo código que o pipeline executa.

Três colunas, como na validação:
    regras  — motor heurístico sozinho (baseline);
    modelo  — só os sinais do modelo, com limiares calibrados;
    hibrido — modelo + regras.

Uso:
    python validacao/calibrar.py validacao/resultados/diagnostico-rapido.csv
"""
from __future__ import annotations

import csv
import itertools
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(RAIZ))

from hermes_ia import calibracao as C  # noqa: E402

PESOS_MODELO = {"churn": [(1.0,)], "oportunidade": [(1.0, 0.0), (0.0, 1.0), (1.0, 0.5), (1.0, 1.0)],
                "sentimento": [(1.0,)]}
PESOS_REGRAS = {"churn": (0.05, 0.1, 0.2, 0.4), "oportunidade": (0.1, 0.2, 0.4), "sentimento": (0.1, 0.2, 0.4, 1.0)}


def acuracia(y, p):
    return sum(a == b for a, b in zip(y, p)) / len(y)


def f1_macro(y, p):
    f1s = []
    for c in sorted(set(y)):
        tp = sum(a == c and b == c for a, b in zip(y, p))
        fp = sum(a != c and b == c for a, b in zip(y, p))
        fn = sum(a == c and b != c for a, b in zip(y, p))
        pr = tp / (tp + fp) if tp + fp else 0
        rc = tp / (tp + fn) if tp + fn else 0
        f1s.append(2 * pr * rc / (pr + rc) if pr + rc else 0)
    return sum(f1s) / len(f1s)


def recall(y, p, c):
    pos = [b for a, b in zip(y, p) if a == c]
    return sum(b == c for b in pos) / len(pos) if pos else 0


def _cortes(valores):
    v = sorted(set(valores))
    return [v[0] - 1e-6] + [(a + b) / 2 for a, b in zip(v, v[1:])] + [v[-1] + 1e-6]


def _melhor(linhas, tarefa, candidatos):
    y = [r["y"][tarefa] for r in linhas]
    melhor = None
    for params, desempate in candidatos:
        p = [C.prever(r["x"], tarefa, params) for r in linhas]
        chave = (acuracia(y, p), f1_macro(y, p), desempate)
        if melhor is None or chave > melhor[0]:
            melhor = (chave, params)
    return melhor[1]


def ajustar(linhas, tarefa, variante):
    """Melhores parâmetros no conjunto (acurácia, depois F1-macro)."""
    candidatos = []
    if tarefa == "churn" and variante == "hibrido":
        # Formulação fixada por desenho: regras decidem os casos explícitos e o modelo
        # ajusta um nível. Com 3 exemplos de churn ALTO, escolher entre formulações
        # pelo próprio treino só troca de vencedor a cada rodada (sobreajuste).
        cortes = _cortes([r["x"]["c_reclama_max"] for r in linhas])
        for t_sobe in cortes:
            for t_desce in (c for c in cortes if c <= t_sobe):
                candidatos.append(({"formulacao": "ajuste_regras", "limiar_sobe": round(t_sobe, 4),
                                    "limiar_desce": round(t_desce, 4)}, 0))
        return _melhor(linhas, tarefa, candidatos)
    pesos_regras = (0.0,) if variante == "modelo" else PESOS_REGRAS[tarefa]
    for pm in PESOS_MODELO[tarefa]:
        for pr in pesos_regras:
            sc = [C.pontuar(r["x"], tarefa, pm, pr) for r in linhas]
            for t1, t2 in itertools.combinations_with_replacement(_cortes(sc), 2):
                candidatos.append(({"formulacao": "pontuacao", "pesos_modelo": list(pm), "peso_regras": pr,
                                    "limiar_medio": round(t1, 4), "limiar_alto": round(t2, 4)}, -(t2 - t1)))
    return _melhor(linhas, tarefa, candidatos)


def loocv(linhas, tarefa, variante):
    y, p = [], []
    for i, r in enumerate(linhas):
        params = ajustar(linhas[:i] + linhas[i + 1:], tarefa, variante)
        y.append(r["y"][tarefa])
        p.append(C.prever(r["x"], tarefa, params))
    return y, p


def carregar(arq: Path) -> list[dict]:
    linhas = []
    for r in csv.DictReader(arq.open(encoding="utf-8-sig"), delimiter=";"):
        x = {k: float(v) for k, v in r.items() if k.startswith(("lex_", "m_", "c_", "o_", "s_"))}
        y = {"churn": r["churn"], "oportunidade": r["oportunidade"],
             # MISTO não cabe num eixo só; na calibração conta como NEUTRO
             "sentimento": "NEUTRO" if r["sentimento"] == "MISTO" else r["sentimento"]}
        linhas.append({"id": r["id"], "x": x, "y": y, "modelo": r.get("modelo_zero_shot", "?")})
    return linhas


def main() -> None:
    arq = Path(sys.argv[1] if len(sys.argv) > 1 else RAIZ / "validacao" / "resultados" / "diagnostico-rapido.csv")
    linhas = carregar(arq)
    modelo_zs = linhas[0]["modelo"]
    print(f"{len(linhas)} transcrições · {arq.name} · zero-shot {modelo_zs}")
    print("Acurácia / F1-macro em LOOCV (cada reunião avaliada por uma calibração que não a viu).\n")
    print("| Tarefa | Regras | Modelo | Híbrido |")
    print("|---|---|---|---|")
    final = {"fonte": arq.name, "modelo_zero_shot": modelo_zs, "n": len(linhas)}
    notas = []
    for tarefa in C.CLASSES:
        y = [r["y"][tarefa] for r in linhas]
        pb = [C.regras_sozinhas(r["x"], tarefa) for r in linhas]
        ym, pm = loocv(linhas, tarefa, "modelo")
        yh, ph = loocv(linhas, tarefa, "hibrido")
        cel = lambda yy, pp: f"{acuracia(yy, pp):.2f} / {f1_macro(yy, pp):.2f}"
        print(f"| {tarefa} | {cel(y, pb)} | {cel(ym, pm)} | {cel(yh, ph)} |")
        if tarefa == "churn":
            notas.append(f"Recall do churn ALTO: regras {recall(y, pb, 'ALTO'):.2f} · modelo "
                         f"{recall(ym, pm, 'ALTO'):.2f} · híbrido {recall(yh, ph, 'ALTO'):.2f}")
        final[tarefa] = {
            "hibrido": ajustar(linhas, tarefa, "hibrido"),
            "modelo": ajustar(linhas, tarefa, "modelo"),
            "loocv": {"regras": [round(acuracia(y, pb), 3), round(f1_macro(y, pb), 3)],
                      "modelo": [round(acuracia(ym, pm), 3), round(f1_macro(ym, pm), 3)],
                      "hibrido": [round(acuracia(yh, ph), 3), round(f1_macro(yh, ph), 3)]},
        }
    print("\n" + "\n".join(notas))
    # Uma calibração por modelo zero-shot, no mesmo arquivo
    todos = json.loads(C.ARQUIVO.read_text(encoding="utf-8")) if C.ARQUIVO.exists() else {}
    todos.setdefault("modelos", {})[modelo_zs] = final
    C.ARQUIVO.write_text(json.dumps(todos, ensure_ascii=False, indent=2), encoding="utf-8")
    C._arquivo.cache_clear()
    print(f"\nParâmetros finais (ajustados nas {len(linhas)}) gravados em {C.ARQUIVO.relative_to(RAIZ)}")


if __name__ == "__main__":
    main()
