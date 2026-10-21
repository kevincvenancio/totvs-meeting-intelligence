"""
Etapa de PROCESSAMENTO do pipeline:

    Transcrição → [Processamento] → Modelo → Indicadores → Recomendação

- normaliza o texto (acentuação corrompida, espaços, cabeçalho opcional);
- separa as falas por locutor e infere quem é cliente e quem é TOTVS;
- quebra a reunião em trechos que cabem no limite de tokens dos modelos;
- escolhe quais trechos vale a pena mandar ao modelo (custo de CPU);
- extrai os termos principais (n-gramas mais relevantes, sem stopwords).
"""
from __future__ import annotations

import math
import re
import unicodedata
from collections import Counter
from dataclasses import dataclass, field

from . import lexico

# ──────────────────────────────────────────────────────────────────────────────
# Estruturas
# ──────────────────────────────────────────────────────────────────────────────


@dataclass
class Fala:
    locutor: str
    texto: str
    papel: str = "desconhecido"  # "cliente" | "totvs" | "desconhecido"


@dataclass
class Trecho:
    indice: int
    texto: str
    locutores: list[str] = field(default_factory=list)
    do_cliente: bool = True  # trecho majoritariamente falado pelo cliente (ou papel desconhecido)

    @property
    def palavras(self) -> int:
        return len(self.texto.split())


@dataclass
class TranscricaoProcessada:
    texto: str
    texto_lower: str
    falas: list[Fala]
    trechos: list[Trecho]
    qtd_palavras: int
    qtd_locutores: int
    metadados: dict[str, str]


# ──────────────────────────────────────────────────────────────────────────────
# Normalização
# ──────────────────────────────────────────────────────────────────────────────

_MOJIBAKE = re.compile(r"[ÃÂ][\x80-\xbf¡-¿]")


def corrigir_codificacao(texto: str) -> str:
    """Desfaz o 'gestÃ£o' → 'gestão' (UTF-8 lido como Latin-1), problema real da base."""
    if not _MOJIBAKE.search(texto):
        return texto
    try:
        return texto.encode("latin-1").decode("utf-8")
    except (UnicodeEncodeError, UnicodeDecodeError):
        return texto


def separar_cabecalho(texto: str) -> tuple[dict[str, str], str]:
    """Cabeçalho opcional entre linhas '---' (mesmo formato aceito pela importação .txt/.docx)."""
    meta: dict[str, str] = {}
    m = re.match(r"\s*---\s*\n(.*?)\n---\s*\n", texto, re.DOTALL)
    if not m:
        return meta, texto
    for linha in m.group(1).splitlines():
        if ":" in linha:
            k, v = linha.split(":", 1)
            meta[k.strip().lower()] = v.strip()
    return meta, texto[m.end():]


def normalizar(texto: str) -> str:
    texto = corrigir_codificacao(texto or "")
    texto = unicodedata.normalize("NFC", texto)
    texto = texto.replace("\r\n", "\n").replace("\r", "\n")
    texto = re.sub(r"[ \t ]+", " ", texto)
    texto = re.sub(r"\n{3,}", "\n\n", texto)
    return texto.strip()


# ──────────────────────────────────────────────────────────────────────────────
# Locutores
# ──────────────────────────────────────────────────────────────────────────────

# "[LOCUTOR 1]: ..." (formato da base TOTVS)
_RE_LOCUTOR = re.compile(r"\[LOCUTOR\s*(\d+)\]\s*:?\s*", re.IGNORECASE)
# "Vendedor: ..." / "Cliente: ..." / "João Silva: ..." no começo da linha
_RE_NOME = re.compile(r"^\s*([A-ZÀ-Úa-zà-ú][\wÀ-ú .]{0,30}?)\s*:\s+(.+)$")

# Frases típicas de quem vende (peso 2) e palavras que os dois lados usam, mas o vendedor mais (peso 1)
_KW_TOTVS_FORTE = ["nossa solução", "nosso sistema", "nosso produto", "nosso módulo", "aqui na totvs",
                   "posso te mostrar", "vou te mostrar", "vou mostrar", "vou compartilhar a tela",
                   "a gente oferece", "a plataforma permite", "te mando a proposta", "envio a proposta",
                   "mando a proposta", "posso preparar", "posso montar"]
_KW_TOTVS = ["licença", "consultor", "te envio", "envio a", "envio o", "envio até", "fico feliz",
             "posso pedir", "vou levar", "vou agendar", "vou envolver", "vou acompanhar", "vou montar"]
_KW_CLIENTE = ["nossa empresa", "nosso processo", "nosso erp", "hoje usamos", "hoje a gente",
               "precisamos de", "temos dificuldade", "a gente precisa", "aqui na empresa",
               "nossa equipe", "nosso time", "nossos funcionários", "nosso contrato", "meu diretor",
               "a diretoria", "o orçamento aprovado", "a gente já decidiu", "manda a proposta",
               "mande a proposta", "quanto custa", "qual o valor", "o pessoal daqui", "quero contratar",
               "temos interesse", "tenho interesse", "pode preparar", "pode mandar", "pode marcar",
               "gostei", "a gente quer", "queremos"]
_NOMES_TOTVS = {"vendedor", "consultor", "totvs", "executivo", "executiva", "gerente de contas", "cs", "esn"}
_NOMES_CLIENTE = {"cliente", "prospect", "comprador", "diretor", "gestor", "gestora"}


def extrair_falas(texto: str) -> list[Fala]:
    falas: list[Fala] = []
    partes = _RE_LOCUTOR.split(texto)
    if len(partes) > 1:  # formato [LOCUTOR n]
        for i in range(1, len(partes) - 1, 2):
            fala = partes[i + 1].strip()
            if fala:
                falas.append(Fala(locutor=f"LOCUTOR {partes[i]}", texto=fala))
    else:
        atual: Fala | None = None
        for linha in texto.splitlines():
            m = _RE_NOME.match(linha)
            if m and len(m.group(1).split()) <= 4:
                atual = Fala(locutor=m.group(1).strip(), texto=m.group(2).strip())
                falas.append(atual)
            elif linha.strip():
                if atual is None:
                    atual = Fala(locutor="?", texto="")
                    falas.append(atual)
                atual.texto = (atual.texto + " " + linha.strip()).strip()
    _inferir_papeis(falas)
    return falas


def _inferir_papeis(falas: list[Fala]) -> None:
    """
    Papel de cada locutor. Primeiro pelo nome ("Vendedor:", "Cliente:"); depois pelo
    vocabulário: d = 2·(frases de vendedor) + (termos de TOTVS) − 2·(termos de cliente). Numa reunião com vários
    locutores, quem tem o maior d é o TOTVS e quem tem d menor é cliente.
    """
    por_loc: dict[str, list[str]] = {}
    for f in falas:
        por_loc.setdefault(f.locutor, []).append(f.texto.lower())
    papeis: dict[str, str] = {}
    saldo: dict[str, int] = {}
    for loc, textos in por_loc.items():
        nome = loc.lower()
        if any(n in nome for n in _NOMES_TOTVS):
            papeis[loc] = "totvs"
        elif any(n in nome for n in _NOMES_CLIENTE):
            papeis[loc] = "cliente"
        else:
            junto = " ".join(textos)
            saldo[loc] = (2 * sum(junto.count(k) for k in _KW_TOTVS_FORTE) + sum(junto.count(k) for k in _KW_TOTVS)
                          - 2 * sum(junto.count(k) for k in _KW_CLIENTE))

    if saldo:
        # desempate: em reunião comercial quem abre costuma ser o vendedor
        primeiro = next((f.locutor for f in falas if f.locutor in saldo), None)
        if primeiro and len(saldo) > 1 and list(saldo.values()).count(max(saldo.values())) > 1 \
                and saldo[primeiro] == max(saldo.values()):
            saldo[primeiro] += 1
        maior = max(saldo.values())
        tem_totvs = "totvs" in papeis.values()
        for loc, d in saldo.items():
            if (not tem_totvs and d == maior and d > min(saldo.values())
                    and list(saldo.values()).count(maior) == 1):
                papeis[loc] = "totvs"
            elif d < 0 or d < maior or tem_totvs:
                papeis[loc] = "cliente"
            else:
                papeis[loc] = "desconhecido"
    for f in falas:
        f.papel = papeis.get(f.locutor, "desconhecido")


# ──────────────────────────────────────────────────────────────────────────────
# Sentenças e trechos
# ──────────────────────────────────────────────────────────────────────────────

_RE_SENTENCA = re.compile(r"(?<=[.!?…])\s+")


def sentencas(texto: str) -> list[str]:
    return [s.strip() for s in _RE_SENTENCA.split(texto) if len(s.strip()) > 1]


def montar_trechos(falas: list[Fala], palavras_por_trecho: int) -> list[Trecho]:
    """Agrupa falas consecutivas em trechos de até N palavras (quebra falas longas por sentença)."""
    unidades: list[tuple[Fala, str]] = []
    for f in falas:
        sents = sentencas(f.texto) or [f.texto]
        buf: list[str] = []
        n = 0
        for s in sents:
            w = len(s.split())
            if buf and n + w > palavras_por_trecho:
                unidades.append((f, " ".join(buf)))
                buf, n = [], 0
            # sentença gigante (transcrição sem pontuação): fatia por palavras
            if w > palavras_por_trecho:
                pal = s.split()
                for i in range(0, len(pal), palavras_por_trecho):
                    unidades.append((f, " ".join(pal[i:i + palavras_por_trecho])))
                continue
            buf.append(s)
            n += w
        if buf:
            unidades.append((f, " ".join(buf)))

    trechos: list[Trecho] = []
    buf_txt: list[str] = []
    buf_loc: list[str] = []
    buf_cli = 0
    buf_tot = 0
    n = 0

    def fechar() -> None:
        nonlocal buf_txt, buf_loc, buf_cli, buf_tot, n
        if buf_txt:
            trechos.append(Trecho(
                indice=len(trechos),
                texto=" ".join(buf_txt),
                locutores=sorted(set(buf_loc)),
                do_cliente=buf_cli >= buf_tot,
            ))
        buf_txt, buf_loc, buf_cli, buf_tot, n = [], [], 0, 0, 0

    for fala, txt in unidades:
        w = len(txt.split())
        if buf_txt and n + w > palavras_por_trecho:
            fechar()
        buf_txt.append(txt)
        buf_loc.append(fala.locutor)
        if fala.papel == "totvs":
            buf_tot += w
        else:
            buf_cli += w
        n += w
    fechar()
    return trechos


def selecionar_trechos(trechos: list[Trecho], k: int, somente_cliente: bool = False) -> list[Trecho]:
    """
    Escolhe até k trechos para o modelo: primeiro os de maior densidade de sinais
    comerciais (léxico), depois completa com trechos espaçados ao longo da reunião
    para não perder o contexto geral. Devolve na ordem original.
    """
    base = [t for t in trechos if t.do_cliente] if somente_cliente else list(trechos)
    if not base:
        base = list(trechos)
    if len(base) <= k:
        return base

    sinais = (lexico.CHURN_ALTO + lexico.CHURN_MEDIO + lexico.SINAIS_INTERESSE
              + lexico.POSITIVAS + lexico.NEGATIVAS + list(lexico.DORES)
              + [t for v in lexico.CONCORRENTES.values() for t in v]
              + ["contrato", "proposta", "renovação", "suporte", "concorrente", "preço", "valor"])

    def densidade(t: Trecho) -> float:
        low = t.texto.lower()
        return sum(low.count(s) for s in sinais) / max(1, math.sqrt(t.palavras))

    ordenados = sorted(base, key=densidade, reverse=True)
    escolhidos = {t.indice for t in ordenados[: k // 2] if densidade(t) > 0}
    passo = len(base) / max(1, k - len(escolhidos))
    i = 0.0
    while len(escolhidos) < k and i < len(base):
        escolhidos.add(base[int(i)].indice)
        i += passo
    for t in base:  # completa se o espaçamento colidiu com os já escolhidos
        if len(escolhidos) >= k:
            break
        escolhidos.add(t.indice)
    return [t for t in base if t.indice in escolhidos]


_PISTAS_PRODUTO = sorted({t for v in lexico.VENDA_CRUZADA.values() for t in v}
                        | {t for v in lexico.PRODUTOS_TOTVS.values() for t in v}
                        | {"folha", "ponto", "jornada", "escala", "estoque", "faturamento", "financeiro",
                           "relatório", "indicador", "dashboard", "crm", "funil", "loja", "pdv", "caixa",
                           "hospital", "clínica", "prontuário", "escola", "faculdade", "matrícula", "aluno",
                           "transporte", "frota", "frete", "armazém", "aprovação", "fluxo", "integração",
                           "erp", "compras", "contábil", "fiscal", "nota fiscal", "cobrança", "boleto",
                           "rh", "funcionários", "colaboradores", "sistema"})


def selecionar_trechos_produto(trechos: list[Trecho], k: int) -> list[Trecho]:
    """Os k trechos que mais falam de processos/áreas — onde o produto aparece."""
    if len(trechos) <= k:
        return list(trechos)

    def densidade(t: Trecho) -> float:
        low = t.texto.lower()
        return sum(low.count(p) for p in _PISTAS_PRODUTO) / max(1, math.sqrt(t.palavras))

    escolhidos = sorted(trechos, key=densidade, reverse=True)[:k]
    return sorted(escolhidos, key=lambda t: t.indice)


# ──────────────────────────────────────────────────────────────────────────────
# Termos principais
# ──────────────────────────────────────────────────────────────────────────────

STOPWORDS = set("""
a à ao aos as às até com como da das de dela dele deles do dos e é ela elas ele eles em entre era eram
essa essas esse esses esta está estão estas este estes eu foi fomos for foram há isso isto já la lá
lhe lhes mais mas me mesmo meu meus minha minhas muito muitos na nas nem no nos nós nossa nossas nosso
nossos num numa o os ou para pela pelas pelo pelos por qual quando que quem se sem ser seu seus si só
sua suas também te tem têm tenho ter teu tu tua um uma umas uns você vocês vos ser são sou vai vamos
vou fazer faz feito pode podem posso aqui ali então né tá ta aí ai assim tipo coisa coisas gente
sim não nao ok okay bom boa bem certo claro beleza tudo todo toda todos todas cada outro outra outros
outras porque pra pro pras pros dá dar daí agora hoje ainda onde depois antes sobre até lo la isso
ah eh hum uhum ahn é ser estar estou estava tinha tem ter vai ia ir vem vou fica ficar acho olha
deixa deixar sei saber falar falando fala disse dizer entendi entendeu exatamente perfeito legal
obrigado obrigada tchau dia tarde noite oi olá senhor senhora locutor mostrar mostra vamos ver
""".split())

_RE_TOKEN = re.compile(r"[a-zà-ú][a-zà-ú\-]+", re.IGNORECASE)


def termos_principais(texto: str, n: int = 10) -> list[dict]:
    """
    Unigramas e bigramas mais relevantes. Pontuação = frequência × peso, com
    bigramas valendo 2,5× (carregam mais significado: "folha de pagamento" > "folha").
    """
    tokens = [t.lower() for t in _RE_TOKEN.findall(texto)]
    uni = Counter(t for t in tokens if t not in STOPWORDS and len(t) > 3)
    bi: Counter[str] = Counter()
    # bigramas sobre a sequência sem stopwords "de/da/do" no meio (folha de pagamento → folha pagamento)
    conteudo = [t for t in tokens if t not in STOPWORDS and len(t) > 2]
    for a, b in zip(conteudo, conteudo[1:]):
        if a != b:
            bi[f"{a} {b}"] += 1

    candidatos: dict[str, float] = {}
    for termo, f in uni.items():
        if f >= 2:
            candidatos[termo] = float(f)
    for termo, f in bi.items():
        if f >= 2:
            candidatos[termo] = f * 2.5
    # remove unigramas totalmente "cobertos" por um bigrama mais forte
    for termo in list(candidatos):
        if " " in termo:
            for parte in termo.split():
                if parte in candidatos and candidatos[parte] <= candidatos[termo] / 2.5 + 0.5:
                    candidatos.pop(parte, None)
    top = sorted(candidatos.items(), key=lambda kv: (-kv[1], kv[0]))[:n]
    return [{"termo": t, "relevancia": round(s, 1)} for t, s in top]


# ──────────────────────────────────────────────────────────────────────────────
# Ponto de entrada
# ──────────────────────────────────────────────────────────────────────────────


def processar(texto_bruto: str, palavras_por_trecho: int = 180) -> TranscricaoProcessada:
    meta, corpo = separar_cabecalho(normalizar(texto_bruto))
    falas = extrair_falas(corpo)
    if not falas:
        falas = [Fala(locutor="?", texto=corpo)]
    texto = "\n".join(f.texto for f in falas)
    trechos = montar_trechos(falas, palavras_por_trecho)
    return TranscricaoProcessada(
        texto=texto,
        texto_lower=texto.lower(),
        falas=falas,
        trechos=trechos,
        qtd_palavras=len(texto.split()),
        qtd_locutores=len({f.locutor for f in falas if f.locutor != "?"}),
        metadados=meta,
    )
