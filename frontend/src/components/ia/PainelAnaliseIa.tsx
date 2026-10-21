/* Painel da análise de IA (modelos do Hugging Face).

   Mostra as seis saídas pedidas para a área comercial — produto, sentimento,
   risco de churn, oportunidade, termos e recomendação — sempre com o trecho da
   transcrição que justificou cada indicador. Usado na tela "Análise IA" e no
   detalhe da reunião. */

import type { AnaliseIa, EvidenciaIa, RiscoChurn } from '../../lib/types';
import { corRisco, rotuloRisco } from '../../lib/format';
import { Selo } from '../app/Shell';

const corNivel: Record<string, string> = {
  ALTA: 'var(--color-state-good)',
  MEDIA: 'var(--color-state-warn)',
  BAIXA: 'var(--color-state-idle)',
};

const corPrioridade: Record<string, string> = {
  ALTA: 'var(--color-state-critical)',
  MEDIA: 'var(--color-state-warn)',
  BAIXA: 'var(--color-state-idle)',
  Alta: 'var(--color-state-critical)',
  Média: 'var(--color-state-warn)',
  Baixa: 'var(--color-state-idle)',
};

const corSentimentoIa: Record<string, string> = {
  POSITIVO: 'var(--color-data-3)',
  NEUTRO: 'var(--color-state-idle)',
  MISTO: 'var(--color-data-5)',
  NEGATIVO: 'var(--color-data-2)',
};

const rotuloNivel: Record<string, string> = { ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' };
const rotuloSent: Record<string, string> = {
  POSITIVO: 'Positivo',
  NEUTRO: 'Neutro',
  MISTO: 'Misto',
  NEGATIVO: 'Negativo',
};

function corSaude(valor: number) {
  if (valor >= 65) return 'var(--color-state-good)';
  if (valor >= 40) return 'var(--color-state-warn)';
  return 'var(--color-state-critical)';
}

function Evidencias({ lista }: { lista: EvidenciaIa[] }) {
  if (lista.length === 0) return null;
  return (
    <ul className="ia-evidencias">
      {lista.map((e, i) => (
        <li key={i}>
          <blockquote className="detalhe-trecho">{e.trecho}</blockquote>
          <span className="ref">
            {e.origem}
            {e.probabilidade != null && ` · p = ${e.probabilidade.toFixed(2)}`}
          </span>
        </li>
      ))}
    </ul>
  );
}

function Medidor({ valor, cor }: { valor: number; cor: string }) {
  return (
    <span className="ia-medidor" aria-hidden="true">
      <span style={{ width: `${Math.max(2, Math.min(100, valor))}%`, background: cor }} />
    </span>
  );
}

export function PainelAnaliseIa({ analise, compacto }: { analise: AnaliseIa; compacto?: boolean }) {
  const { indicadores: ind, sentimento: sent, risco_churn: churn, oportunidade_comercial: oport } = analise;

  return (
    <div className="ia-painel" data-compacto={compacto || undefined}>
      {/* Faixa de indicadores */}
      <section className="ia-faixa" aria-label="Indicadores da IA">
        <div className="ia-kpi">
          <span className="detalhe-selo-rotulo">Saúde do cliente</span>
          <span className="ia-kpi-valor numeric" style={{ color: corSaude(ind.saude_cliente) }}>
            {ind.saude_cliente}
            <small>/100</small>
          </span>
          <Medidor valor={ind.saude_cliente} cor={corSaude(ind.saude_cliente)} />
        </div>
        <div className="ia-kpi">
          <span className="detalhe-selo-rotulo">Risco de churn</span>
          <span className="ia-kpi-linha">
            <Selo texto={rotuloRisco[churn.nivel as RiscoChurn]} tom={corRisco[churn.nivel as RiscoChurn]} />
            <span className="numeric">{churn.score}/100</span>
          </span>
          <Medidor valor={churn.score} cor={corRisco[churn.nivel as RiscoChurn]} />
        </div>
        <div className="ia-kpi">
          <span className="detalhe-selo-rotulo">Oportunidade</span>
          <span className="ia-kpi-linha">
            <Selo texto={rotuloNivel[oport.nivel]} tom={corNivel[oport.nivel]} />
            <span className="numeric">{oport.score}/100</span>
          </span>
          <Medidor valor={oport.score} cor={corNivel[oport.nivel]} />
        </div>
        <div className="ia-kpi">
          <span className="detalhe-selo-rotulo">Sentimento</span>
          <span className="ia-kpi-linha">
            <Selo texto={rotuloSent[sent.rotulo]} tom={corSentimentoIa[sent.rotulo]} />
            <span className="numeric">{sent.score >= 0 ? '+' : ''}{sent.score.toFixed(2)}</span>
          </span>
          <Medidor valor={(sent.score + 1) * 50} cor={corSentimentoIa[sent.rotulo]} />
        </div>
        <div className="ia-kpi">
          <span className="detalhe-selo-rotulo">Prioridade</span>
          <Selo texto={ind.prioridade} tom={corPrioridade[ind.prioridade] ?? 'var(--hairline)'} />
        </div>
      </section>

      <div className="ia-grade">
        <section className="ia-cartao">
          <h3 className="eyebrow">Produto identificado</h3>
          {analise.produtos_identificados.length === 0 ? (
            <p className="detalhe-vazio">Nenhum produto identificado.</p>
          ) : (
            <ul className="ia-produtos">
              {analise.produtos_identificados.map((p) => (
                <li key={p.produto}>
                  <strong>{p.produto}</strong>
                  <span className="ref">
                    {Math.round(p.confianca * 100)}% · {p.origem}
                  </span>
                </li>
              ))}
            </ul>
          )}
          {analise.concorrentes.length > 0 && (
            <p className="ia-nota">
              Concorrentes citados: <strong>{analise.concorrentes.join(', ')}</strong>
            </p>
          )}
        </section>

        <section className="ia-cartao">
          <h3 className="eyebrow">Principais termos</h3>
          <ul className="detalhe-etiquetas">
            {analise.termos_principais.map((t) => (
              <li key={t.termo}>{t.termo}</li>
            ))}
          </ul>
          {sent.justificativa && <p className="ia-nota">{sent.justificativa}</p>}
        </section>

        <section className="ia-cartao">
          <h3 className="eyebrow">Por que este risco de churn</h3>
          {churn.evidencias.length > 0 ? (
            <Evidencias lista={churn.evidencias} />
          ) : (
            <p className="detalhe-vazio">Nenhum sinal de cancelamento na conversa.</p>
          )}
          <p className="ia-nota ref">
            modelo {churn.score_modelo ?? '—'} · regras {churn.score_lexico}
          </p>
        </section>

        <section className="ia-cartao">
          <h3 className="eyebrow">Por que esta oportunidade</h3>
          {oport.budget && (
            <p className="ia-nota">
              Orçamento citado: <strong>{oport.budget}</strong>
            </p>
          )}
          {oport.evidencias.length > 0 ? (
            <Evidencias lista={oport.evidencias} />
          ) : (
            <p className="detalhe-vazio">Sem sinal claro de interesse em compra.</p>
          )}
          {oport.venda_cruzada.length > 0 && (
            <p className="ia-nota">
              Venda cruzada: {oport.venda_cruzada.map((v) => v.produto).join(', ')}
            </p>
          )}
        </section>
      </div>

      <section className="ia-cartao">
        <h3 className="eyebrow">Recomendação de ação</h3>
        <ol className="ia-acoes">
          {analise.recomendacoes.map((a, i) => (
            <li key={i}>
              <Selo texto={rotuloNivel[a.prioridade]} tom={corPrioridade[a.prioridade]} />
              <div>
                <p className="ia-acao">{a.acao}</p>
                <p className="ref">
                  {a.prazo} · {a.responsavel} — {a.motivo}
                </p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <p className="ia-rodape ref">
        {analise.modelos
          ? `Modelos: ${analise.modelos.sentimento} · ${analise.modelos.zero_shot}`
          : 'Modo heurístico (só regras)'}{' '}
        · modo {analise.modo} · {analise.processamento.palavras} palavras · {analise.tempo_ms} ms
      </p>
    </div>
  );
}
