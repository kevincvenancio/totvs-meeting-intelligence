"""
Etapa de RECOMENDAÇÃO — transforma indicadores em próximas ações para a área comercial.

Motor de regras explícito (não é "caixa-preta"): cada recomendação diz o motivo,
o prazo sugerido e o responsável, para o vendedor confiar e agir.
"""
from __future__ import annotations

_ORDEM = {"ALTA": 0, "MEDIA": 1, "BAIXA": 2}


def _acao(acao: str, prioridade: str, prazo: str, responsavel: str, motivo: str) -> dict:
    return {"acao": acao, "prioridade": prioridade, "prazo": prazo, "responsavel": responsavel, "motivo": motivo}


def gerar(resultado: dict, max_acoes: int = 5) -> list[dict]:
    churn = resultado["risco_churn"]
    oport = resultado["oportunidade_comercial"]
    sent = resultado["sentimento"]
    produtos = resultado["produtos_identificados"]
    concorrentes = resultado["concorrentes"]
    dores = resultado["dores"]
    palavras = resultado["processamento"]["palavras"]
    acoes: list[dict] = []

    revisar = _acao("Revisar a gravação ou reagendar a reunião", "MEDIA", "3 dias", "Vendedor",
                    f"Transcrição com apenas {palavras} palavras — conteúdo insuficiente para análise confiável.")
    if palavras < 50:  # mesmo corte do back-end Java para reunião INCOMPLETA
        return [revisar]

    # ── Retenção ──────────────────────────────────────────────────────────────
    if churn["nivel"] == "ALTO":
        acoes.append(_acao(
            "Acionar Customer Success e o gerente da conta com um plano de retenção",
            "ALTA", "24 horas", "Customer Success",
            f"Risco de churn {churn['score']}/100" + (f" — sinal: “{churn['sinais'][0]}”" if churn["sinais"] else "") + "."))
    elif churn["nivel"] == "MEDIO":
        acoes.append(_acao(
            "Agendar reunião de acompanhamento (health check) com o decisor",
            "MEDIA", "7 dias", "Customer Success",
            f"Risco de churn moderado ({churn['score']}/100): sinais de insatisfação ou avaliação de alternativas."))

    if concorrentes:
        acoes.append(_acao(
            f"Preparar comparativo de diferenciais TOTVS x {', '.join(concorrentes)}",
            "ALTA" if churn["nivel"] != "BAIXO" else "MEDIA", "5 dias", "Pré-vendas",
            "Concorrente citado na reunião."))

    dores_suporte = [d for d in dores if "suporte" in d["descricao"].lower() or "resposta" in d["descricao"].lower()]
    if dores_suporte:
        acoes.append(_acao(
            "Escalonar os chamados em aberto e dar retorno formal ao cliente",
            "ALTA", "48 horas", "Suporte",
            "Reclamação de atendimento: " + ", ".join(d["descricao"] for d in dores_suporte) + "."))

    # ── Venda ────────────────────────────────────────────────────────────────
    principal = produtos[0]["produto"] if produtos else None
    if oport["nivel"] == "ALTA":
        if oport["budget"]:
            acoes.append(_acao(
                f"Enviar proposta comercial{' de ' + principal if principal else ''}",
                "ALTA", "48 horas", "Vendedor",
                f"Oportunidade {oport['score']}/100 e orçamento citado ({oport['budget']})."))
        else:
            acoes.append(_acao(
                "Qualificar orçamento, decisor e prazo (BANT) e preparar proposta",
                "ALTA", "3 dias", "Vendedor",
                f"Interesse claro (oportunidade {oport['score']}/100), mas sem orçamento citado."))
    elif oport["nivel"] == "MEDIA":
        acoes.append(_acao(
            f"Agendar demonstração focada em {principal or 'nas dores levantadas'}",
            "MEDIA", "7 dias", "Pré-vendas",
            f"Interesse moderado (oportunidade {oport['score']}/100)."))

    ja_citados = {p["produto"] for p in produtos if p["origem"] == "citado na reunião"}
    for vc in oport["venda_cruzada"]:
        if vc["produto"] not in ja_citados:
            acoes.append(_acao(
                f"Apresentar {vc['produto']} como venda cruzada",
                "MEDIA" if churn["nivel"] == "BAIXO" else "BAIXA", "15 dias", "Vendedor",
                f"O cliente falou de “{vc['sinal']}”."))

    if sent["rotulo"] == "POSITIVO" and churn["nivel"] == "BAIXO":
        acoes.append(_acao(
            "Pedir indicação ou depoimento (case de sucesso)", "BAIXA", "30 dias", "Marketing / CS",
            f"Cliente satisfeito (sentimento {sent['score']:+.2f}) e sem risco de churn."))

    if palavras < 100:
        revisar["prioridade"] = "BAIXA"
        revisar["acao"] = "Conferir se a gravação está completa"
        acoes.append(revisar)

    if not acoes:
        acoes.append(_acao(
            "Fazer novo contato para mapear dores e orçamento", "BAIXA", "15 dias", "Vendedor",
            "Pouca informação comercial na reunião."))

    acoes.sort(key=lambda a: _ORDEM[a["prioridade"]])
    return acoes[:max_acoes]
