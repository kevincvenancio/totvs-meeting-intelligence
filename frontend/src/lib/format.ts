/* Formatação pt-BR e mapeamentos de domínio para rótulo/cor. */

import type {
  NivelCriticidade,
  RiscoChurn,
  Sentimento,
  StatusCliente,
  StatusCompletude,
  StatusPlanoAcao,
} from './types';

const numero = new Intl.NumberFormat('pt-BR');
const numeroCompacto = new Intl.NumberFormat('pt-BR', { notation: 'compact', maximumFractionDigits: 1 });
const dataCurta = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' });
const dataLonga = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long' });
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export const n = (valor: number | undefined | null) => (valor == null ? '—' : numero.format(valor));
export const nc = (valor: number | undefined | null) =>
  valor == null ? '—' : numeroCompacto.format(valor);

function paraData(valor?: string | null) {
  if (!valor) return null;
  // A API manda LocalDate (2025-09-08) e LocalDateTime (2025-09-08T14:31:08).
  const d = new Date(valor.length === 10 ? `${valor}T12:00:00` : valor);
  return Number.isNaN(d.getTime()) ? null : d;
}

export const dt = (valor?: string | null) => {
  const d = paraData(valor);
  return d ? dataCurta.format(d) : '—';
};

export const dtLonga = (valor?: string | null) => {
  const d = paraData(valor);
  return d ? dataLonga.format(d) : '—';
};

export const dth = (valor?: string | null) => {
  const d = paraData(valor);
  return d ? dataHora.format(d) : '—';
};

/** "há 3 dias" / "em 5 dias" — para prazos de plano de ação. */
export function relativo(dias?: number | null) {
  if (dias == null) return '—';
  if (dias === 0) return 'hoje';
  const rtf = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto' });
  return rtf.format(dias, 'day');
}

/* ── Domínio → rótulo ───────────────────────────────────────────────────── */

export const rotuloSentimento: Record<Sentimento, string> = {
  POSITIVO: 'Positivo',
  NEGATIVO: 'Negativo',
  CRITICO: 'Crítico',
  MISTO: 'Misto',
  OPORTUNIDADE_COMERCIAL: 'Oportunidade',
  NEUTRO: 'Neutro',
};

export const rotuloRisco: Record<RiscoChurn, string> = {
  ALTO: 'Alto',
  MEDIO: 'Médio',
  BAIXO: 'Baixo',
};

export const rotuloCompletude: Record<StatusCompletude, string> = {
  COMPLETA: 'Completa',
  PARCIAL: 'Parcial',
  INCOMPLETA: 'Incompleta',
};

export const rotuloStatusPlano: Record<StatusPlanoAcao, string> = {
  PENDENTE: 'Pendente',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
};

export const rotuloStatusCliente: Record<StatusCliente, string> = {
  PROSPECT: 'Prospect',
  ATIVO: 'Ativo',
  EM_RISCO: 'Em risco',
  INATIVO: 'Inativo',
};

export const rotuloCriticidade: Record<NivelCriticidade, string> = {
  ALTA: 'Alta',
  MEDIA: 'Média',
  BAIXA: 'Baixa',
};

/* ── Domínio → cor de dado ──────────────────────────────────────────────
   A ordem dos slots é o mecanismo de segurança para daltonismo — cada
   entidade tem um slot fixo, que não muda quando a lista é filtrada.      */

export const corSentimento: Record<Sentimento, string> = {
  POSITIVO: 'var(--color-data-3)',
  OPORTUNIDADE_COMERCIAL: 'var(--color-data-2)',
  MISTO: 'var(--color-data-5)',
  NEUTRO: 'var(--color-state-idle)',
  NEGATIVO: 'var(--color-data-1)',
  CRITICO: 'var(--color-data-6)',
};

/** Risco e criticidade são estado, não identidade: paleta reservada. */
export const corRisco: Record<RiscoChurn, string> = {
  BAIXO: 'var(--color-state-good)',
  MEDIO: 'var(--color-state-warn)',
  ALTO: 'var(--color-state-critical)',
};

export const corCompletude: Record<StatusCompletude, string> = {
  COMPLETA: 'var(--color-state-good)',
  PARCIAL: 'var(--color-state-warn)',
  INCOMPLETA: 'var(--color-state-critical)',
};

export const corStatusPlano: Record<StatusPlanoAcao, string> = {
  PENDENTE: 'var(--color-state-warn)',
  EM_ANDAMENTO: 'var(--color-data-2)',
  CONCLUIDO: 'var(--color-state-good)',
  CANCELADO: 'var(--color-state-idle)',
};

export const corStatusCliente: Record<StatusCliente, string> = {
  ATIVO: 'var(--color-state-good)',
  EM_RISCO: 'var(--color-state-critical)',
  PROSPECT: 'var(--color-data-2)',
  INATIVO: 'var(--color-state-idle)',
};

/** Slots de dado por posição — para séries sem semântica própria. */
export const SLOTS_DADO = [
  'var(--color-data-1)',
  'var(--color-data-2)',
  'var(--color-data-3)',
  'var(--color-data-4)',
  'var(--color-data-5)',
  'var(--color-data-6)',
] as const;

export const slot = (indice: number) => SLOTS_DADO[indice % SLOTS_DADO.length];

/** Quebra o texto multi-linha que a análise devolve em itens de lista. */
export const linhas = (texto?: string | null) =>
  (texto ?? '')
    .split(/\r?\n|(?<=\.)\s{2,}/)
    .map((l) => l.trim())
    .filter(Boolean);

export const iniciais = (nome?: string) =>
  (nome ?? '?')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase())
    .join('');
