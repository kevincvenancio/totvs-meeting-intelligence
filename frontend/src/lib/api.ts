/* ============================================================================
   Cliente da API REST do Hermes (Spring Boot, /api/v1).

   Regra de ouro do projeto: o front nunca fica em branco. Se a API não estiver
   no ar — o backend depende do Oracle da FIAP — cada consulta cai num conjunto
   de dados de demonstração e a interface avisa que está em modo vitrine.
   ========================================================================== */

import type {
  Cliente,
  Comentario,
  Dashboard,
  ErroResposta,
  Feedback,
  ImportacaoLote,
  IndicadoresPlanoAcao,
  Insight,
  PaginaResposta,
  PlanoAcao,
  ResultadoImportacao,
  ReuniaoDetalhe,
  ReuniaoResumo,
  StatusPlanoAcao,
  Usuario,
} from './types';
import * as demo from './demoData';

export const BASE = '/api/v1';

/** Erro devolvido pela API com corpo padronizado. */
export class ApiError extends Error {
  readonly status: number;
  readonly corpo: ErroResposta;

  constructor(status: number, corpo: ErroResposta) {
    super(corpo.mensagem || corpo.erro || `Falha ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.corpo = corpo;
  }
}

/** A API respondeu? `null` enquanto ninguém perguntou ainda. */
let apiViva: boolean | null = null;
const ouvintes = new Set<(viva: boolean) => void>();

export function assinarStatusApi(fn: (viva: boolean) => void) {
  ouvintes.add(fn);
  if (apiViva !== null) fn(apiViva);
  return () => {
    ouvintes.delete(fn);
  };
}

function marcar(viva: boolean) {
  if (apiViva === viva) return;
  apiViva = viva;
  ouvintes.forEach((fn) => fn(viva));
}

export const modoDemonstracao = () => apiViva === false;

type Opcoes = RequestInit & { query?: Record<string, unknown> };

/** Os filtros são interfaces fechadas; a montagem da query e a demonstração
    precisam deles como mapa. A conversão fica num lugar só. */
const mapa = (filtro: object) => filtro as Record<string, unknown>;

function montarUrl(caminho: string, query?: Record<string, unknown>) {
  const url = new URL(BASE + caminho, window.location.origin);
  if (query) {
    for (const [chave, valor] of Object.entries(query)) {
      if (valor === undefined || valor === null || valor === '') continue;
      url.searchParams.set(chave, String(valor));
    }
  }
  return url.pathname + url.search;
}

/* "A API não está no ar" tem mais de uma cara. Direto no navegador, o fetch
   estoura (status 0). Atrás do proxy do Vite, o servidor de desenvolvimento
   responde 502 no lugar do back-end derrubado. Nenhum dos dois é resposta da
   aplicação — os dois levam à demonstração. */
const INDISPONIVEL = new Set([0, 502, 503, 504]);

/** Requisição crua: propaga erro de negócio, sinaliza back-end fora do ar. */
async function bruto<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  const { query, ...init } = opcoes;
  let resposta: Response;

  try {
    resposta = await fetch(montarUrl(caminho, query), {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(init.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
        ...init.headers,
      },
    });
  } catch {
    marcar(false);
    throw new ApiError(0, { mensagem: 'API indisponível.' });
  }

  if (INDISPONIVEL.has(resposta.status)) {
    marcar(false);
    throw new ApiError(resposta.status, { mensagem: 'API indisponível.' });
  }

  marcar(true);

  if (resposta.status === 204) return undefined as T;

  if (!resposta.ok) {
    const corpo = (await resposta.json().catch(() => ({}))) as ErroResposta;
    throw new ApiError(resposta.status, corpo);
  }

  return (await resposta.json()) as T;
}

/** Requisição de leitura: cai na demonstração quando a API não responde. */
async function ler<T>(caminho: string, alternativa: () => T, opcoes: Opcoes = {}): Promise<T> {
  try {
    return await bruto<T>(caminho, opcoes);
  } catch (erro) {
    // Só o silêncio do back-end justifica a demonstração; 404 e 422 são resposta.
    if (erro instanceof ApiError && INDISPONIVEL.has(erro.status)) return alternativa();
    throw erro;
  }
}

/* ── Autenticação ───────────────────────────────────────────────────────── */

export const autenticacao = {
  login: (email: string, senha: string) =>
    ler<Usuario>('/autenticacao/login', () => demo.usuarioDemo(email), {
      method: 'POST',
      body: JSON.stringify({ email, senha }),
    }),
};

/* ── Painel ─────────────────────────────────────────────────────────────── */

export const painel = {
  obter: () => ler<Dashboard>('/dashboard', () => demo.dashboard),
};

/* ── Reuniões ───────────────────────────────────────────────────────────── */

export interface FiltroReuniao {
  termo?: string;
  clienteId?: number;
  loteId?: number;
  statusCompletude?: string;
  sentimento?: string;
  riscoChurn?: string;
  prioridade?: string;
  dataInicial?: string;
  dataFinal?: string;
  somenteAnalisadas?: boolean;
  incluirDuplicadas?: boolean;
  ordenarPor?: string;
  direcao?: 'asc' | 'desc';
  pagina?: number;
  tamanho?: number;
}

export const reunioes = {
  listar: (filtro: FiltroReuniao = {}) =>
    ler<PaginaResposta<ReuniaoResumo>>('/reunioes', () => demo.paginaReunioes(mapa(filtro)), {
      query: mapa(filtro),
    }),

  buscar: (id: number) => ler<ReuniaoDetalhe>(`/reunioes/${id}`, () => demo.reuniaoDetalhe(id)),

  insights: (id: number) => ler<Insight[]>(`/reunioes/${id}/insights`, () => demo.insights(id)),

  feedback: (id: number) =>
    ler<Feedback | null>(`/reunioes/${id}/feedback`, () => demo.feedback(id)),

  planos: (id: number) =>
    ler<PlanoAcao[]>(`/reunioes/${id}/planos-acao`, () => demo.planosDaReuniao(id)),

  comentarios: (id: number, pagina = 0, tamanho = 20) =>
    ler<PaginaResposta<Comentario>>(`/reunioes/${id}/comentarios`, () => demo.comentarios(id), {
      query: { pagina, tamanho },
    }),

  comentar: (id: number, texto: string, usuarioId: number) =>
    bruto<Comentario>(`/reunioes/${id}/comentarios`, {
      method: 'POST',
      body: JSON.stringify({ texto, usuarioId }),
    }),

  reanalisar: (id: number) => bruto<ReuniaoDetalhe>(`/reunioes/${id}/analise`, { method: 'POST' }),
};

/* ── Clientes ───────────────────────────────────────────────────────────── */

export interface FiltroCliente {
  termo?: string;
  status?: string;
  uf?: string;
  pagina?: number;
  tamanho?: number;
}

export const clientes = {
  listar: (filtro: FiltroCliente = {}) =>
    ler<PaginaResposta<Cliente>>('/clientes', () => demo.paginaClientes(mapa(filtro)), {
      query: mapa(filtro),
    }),

  buscar: (id: number) => ler<Cliente>(`/clientes/${id}`, () => demo.cliente(id)),

  reunioesDo: (id: number) =>
    ler<ReuniaoResumo[]>(`/clientes/${id}/reunioes`, () => demo.reunioesDoCliente(id)),
};

/* ── Planos de ação ─────────────────────────────────────────────────────── */

export interface FiltroPlano {
  termo?: string;
  reuniaoId?: number;
  responsavelId?: number;
  status?: StatusPlanoAcao;
  prioridade?: string;
  somenteAtrasados?: boolean;
  prazoAte?: string;
  ordenarPor?: string;
  direcao?: 'asc' | 'desc';
  pagina?: number;
  tamanho?: number;
}

export const planosAcao = {
  listar: (filtro: FiltroPlano = {}) =>
    ler<PaginaResposta<PlanoAcao>>('/planos-acao', () => demo.paginaPlanos(mapa(filtro)), {
      query: mapa(filtro),
    }),

  indicadores: () =>
    ler<IndicadoresPlanoAcao>('/planos-acao/indicadores', () => demo.dashboard.planosAcao),

  atrasados: () => ler<PlanoAcao[]>('/planos-acao/atrasados', () => demo.planosAtrasados()),

  transicionar: (id: number, status: StatusPlanoAcao, resultado?: string) =>
    bruto<PlanoAcao>(`/planos-acao/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status, resultado }),
    }),
};

/* ── Importações ────────────────────────────────────────────────────────── */

export const importacoes = {
  listar: () => ler<ImportacaoLote[]>('/importacoes', () => demo.lotes),

  enviar: (arquivo: File) => {
    const corpo = new FormData();
    corpo.append('arquivo', arquivo);
    return bruto<ResultadoImportacao>('/importacoes', { method: 'POST', body: corpo });
  },
};

/* ── Relatórios (PDF — download direto, sem passar por JSON) ────────────── */

export const relatorios = {
  executivo: () => `${BASE}/relatorios/executivo`,
  reuniao: (id: number) => `${BASE}/relatorios/reunioes/${id}`,
  lote: (id: number) => `${BASE}/relatorios/lotes/${id}`,
};
