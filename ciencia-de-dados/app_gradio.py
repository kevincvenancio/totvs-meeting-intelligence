"""
PROTÓTIPO FUNCIONAL — interface web do pipeline Hermes IA.

Cole (ou envie em .txt) uma transcrição nova e veja, em segundos, o produto
identificado, o sentimento, o risco de churn, a oportunidade comercial, os termos
principais e as recomendações de ação.

    python app_gradio.py            → http://localhost:7860
    python app_gradio.py --share    → link público temporário (para a apresentação)
"""
from __future__ import annotations

import argparse
import html
import json
from pathlib import Path

import gradio as gr

from hermes_ia import ConfigPipeline, HermesPipeline
from hermes_ia.config import MODELO_ZERO_SHOT_PRECISO

_PIPES: dict[str, HermesPipeline] = {}
_EXEMPLOS = Path(__file__).with_name("validacao") / "novas_transcricoes.jsonl"

COR = {"ALTO": "#c0392b", "ALTA": "#1e8449", "MEDIO": "#d68910", "MEDIA": "#d68910",
       "BAIXO": "#1e8449", "BAIXA": "#7f8c8d", "POSITIVO": "#1e8449", "NEGATIVO": "#c0392b",
       "MISTO": "#d68910", "NEUTRO": "#7f8c8d", "Alta": "#c0392b", "Média": "#d68910", "Baixa": "#7f8c8d"}


def _pipeline(modo: str, preciso: bool) -> HermesPipeline:
    """Um pipeline por combinação; os pesos dos modelos ficam em cache (lru_cache em modelos.py)."""
    chave = f"{modo}-{preciso}"
    if chave not in _PIPES:
        cfg = ConfigPipeline(modo=modo)
        if preciso:
            cfg.modelo_zero_shot = MODELO_ZERO_SHOT_PRECISO
        _PIPES[chave] = HermesPipeline(cfg)
    return _PIPES[chave]


def _selo(txt: str) -> str:
    return (f'<span style="background:{COR.get(txt, "#34495e")};color:#fff;padding:2px 10px;'
            f'border-radius:12px;font-weight:600">{html.escape(str(txt))}</span>')


def _cartao(titulo: str, corpo: str) -> str:
    return (f'<div style="border:1px solid #ddd;border-radius:10px;padding:12px 14px;margin:6px 0">'
            f'<div style="font-size:12px;text-transform:uppercase;letter-spacing:.06em;opacity:.7">{titulo}</div>'
            f'<div style="margin-top:6px">{corpo}</div></div>')


def _evid(lista: list[dict]) -> str:
    if not lista:
        return ""
    itens = []
    for e in lista:
        prob = "" if e["probabilidade"] is None else f', p={e["probabilidade"]:.2f}'
        itens.append(f'<li style="margin:4px 0"><i>“{html.escape(e["trecho"])}”</i> '
                     f'<small>({html.escape(e["origem"])}{prob})</small></li>')
    itens = "".join(itens)
    return f'<ul style="margin:6px 0 0 18px;padding:0">{itens}</ul>'


def renderizar(r: dict) -> str:
    ind = r["indicadores"]
    s, c, o = r["sentimento"], r["risco_churn"], r["oportunidade_comercial"]
    topo = (f'<div style="display:flex;gap:18px;flex-wrap:wrap;align-items:center">'
            f'<div><b>Saúde do cliente</b><div style="font-size:30px;font-weight:700">{ind["saude_cliente"]}/100</div></div>'
            f'<div><b>Prioridade</b><br>{_selo(ind["prioridade"])}</div>'
            f'<div><b>Classificação Hermes</b><br>{_selo(ind["sentimento_hermes"])}</div>'
            f'<div><small>{r["processamento"]["palavras"]} palavras · {r["processamento"]["locutores"]} locutores · '
            f'modo {r["modo"]} · {r["tempo_ms"]} ms</small></div></div>')

    prods = "".join(f'<li><b>{html.escape(p["produto"])}</b> — {p["confianca"]:.0%} <small>({p["origem"]})</small></li>'
                    for p in r["produtos_identificados"]) or "<i>Nenhum produto identificado</i>"
    conc = (f'<div style="margin-top:6px">Concorrentes citados: <b>{", ".join(r["concorrentes"])}</b></div>'
            if r["concorrentes"] else "")
    termos = " ".join(f'<span style="background:#eef2f7;color:#223;border-radius:6px;padding:2px 8px;margin:2px;'
                      f'display:inline-block">{html.escape(t["termo"])}</span>' for t in r["termos_principais"])
    acoes = "".join(
        f'<tr><td>{_selo(a["prioridade"].capitalize() if a["prioridade"] != "MEDIA" else "Média")}</td>'
        f'<td><b>{html.escape(a["acao"])}</b><br><small>{html.escape(a["motivo"])}</small></td>'
        f'<td>{a["prazo"]}</td><td>{a["responsavel"]}</td></tr>' for a in r["recomendacoes"])
    dist = s["distribuicao_trechos"]
    dist_txt = f' · trechos: {dist["positivo"]:.0%} pos / {dist["neutro"]:.0%} neu / {dist["negativo"]:.0%} neg' if dist else ""

    return (
        _cartao("Resumo", topo)
        + _cartao("Produto identificado", f'<ul style="margin:0 0 0 18px;padding:0">{prods}</ul>{conc}')
        + _cartao("Sentimento", f'{_selo(s["rotulo"])} score {s["score"]:+.2f}{dist_txt}<br><small>{html.escape(s["justificativa"])}</small>')
        + _cartao("Risco de churn", f'{_selo(c["nivel"])} {c["score"]}/100'
                  f'<small> (modelo {c["score_modelo"] if c["score_modelo"] is not None else "–"} · léxico {c["score_lexico"]})</small>'
                  + _evid(c["evidencias"]))
        + _cartao("Oportunidade comercial", f'{_selo(o["nivel"])} {o["score"]}/100'
                  + (f' · orçamento citado: <b>{html.escape(o["budget"])}</b>' if o["budget"] else "")
                  + _evid(o["evidencias"]))
        + _cartao("Principais termos", termos or "<i>—</i>")
        + _cartao("Recomendação de ação",
                  f'<table style="width:100%;border-collapse:collapse">'
                  f'<tr style="text-align:left"><th></th><th>Ação</th><th>Prazo</th><th>Responsável</th></tr>{acoes}</table>')
    )


def analisar(texto: str, arquivo, modo: str, preciso: bool):
    if arquivo is not None:
        caminho = arquivo if isinstance(arquivo, str) else arquivo.name
        texto = Path(caminho).read_text(encoding="utf-8", errors="replace")
    if not texto or len(texto.split()) < 5:
        raise gr.Error("Cole uma transcrição (ou envie um .txt) com pelo menos algumas frases.")
    r = _pipeline(modo, preciso).analisar(texto)
    return renderizar(r), r


def exemplos() -> list[list[str]]:
    if not _EXEMPLOS.exists():
        return []
    casos = [json.loads(l) for l in _EXEMPLOS.read_text(encoding="utf-8").splitlines() if l.strip()]
    return [[c["transcricao"]] for c in casos[:8]]


def montar_app() -> gr.Blocks:
    with gr.Blocks(title="Hermes IA — análise de transcrições") as app:
        gr.Markdown("## Hermes IA — Transcrição → Processamento → Modelo → Indicadores → Recomendação\n"
                    "Modelos do Hugging Face: sentimento (DistilBERT multilíngue) e zero-shot NLI (MiniLMv2 / mDeBERTa-v3, XNLI).")
        with gr.Row():
            with gr.Column(scale=5):
                texto = gr.Textbox(label="Transcrição", lines=16,
                                   placeholder="[LOCUTOR 1]: Bom dia...\n[LOCUTOR 2]: ...")
                arquivo = gr.File(label="…ou envie um .txt", file_types=[".txt"])
                with gr.Row():
                    modo = gr.Dropdown(["hibrido", "modelo", "heuristico"], value="hibrido", label="Modo")
                    preciso = gr.Checkbox(label="Zero-shot preciso (mDeBERTa, ~10x mais lento)", value=False)
                botao = gr.Button("Analisar", variant="primary")
                ex = exemplos()
                if ex:
                    gr.Examples(ex, inputs=[texto], label="Transcrições de validação")
            with gr.Column(scale=6):
                painel = gr.HTML()
                bruto = gr.JSON(label="Saída completa (JSON)")
        botao.click(analisar, [texto, arquivo, modo, preciso], [painel, bruto])
    return app


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--share", action="store_true")
    ap.add_argument("--porta", type=int, default=7860)
    a = ap.parse_args()
    montar_app().launch(server_port=a.porta, share=a.share)
