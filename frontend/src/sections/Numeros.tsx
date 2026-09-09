/* ============================================================================
   Prova.

   Depois de três seções de argumento, a página precisa mostrar volume. Os
   números aqui são os da migração real registrada no README: 1.126 reuniões,
   2.819 insights, 86,3 MB de transcrição em 172 segundos.

   O movimento é contido de propósito — números que dançam demais viram
   decoração e param de ser lidos como números.
   ========================================================================== */

import { dashboard } from '../lib/demoData';
import { Contador, Entrada, Filete } from '../components/fx/primitives';
import { Ranking } from '../components/viz/Ranking';
import { Tendencia } from '../components/viz/Tendencia';
import { Rosca } from '../components/viz/Rosca';
import { corSentimento, rotuloSentimento } from '../lib/format';
import type { Sentimento } from '../lib/types';

const MARCOS = [
  { valor: 1126, rotulo: 'reuniões analisadas', apoio: 'em um único lote de importação' },
  { valor: 2819, rotulo: 'insights extraídos', apoio: 'cada um com trecho de origem' },
  { valor: 86.3, rotulo: 'MB de transcrição', apoio: 'a maior com 188 mil caracteres', decimais: 1 },
  { valor: 172, rotulo: 'segundos de migração', apoio: 'H2 → Oracle, sem registro órfão' },
];

/* Volume por mês do recorte analisado. */
const SERIE = [
  { rotulo: 'jan', valor: 62 },
  { rotulo: 'fev', valor: 78 },
  { rotulo: 'mar', valor: 96 },
  { rotulo: 'abr', valor: 88 },
  { rotulo: 'mai', valor: 117 },
  { rotulo: 'jun', valor: 134 },
  { rotulo: 'jul', valor: 128 },
  { rotulo: 'ago', valor: 165 },
  { rotulo: 'set', valor: 158 },
];

export function Numeros() {
  const fatias = Object.entries(dashboard.sentimentos)
    .map(([chave, valor]) => ({
      rotulo: rotuloSentimento[chave as Sentimento] ?? chave,
      valor,
      cor: corSentimento[chave as Sentimento] ?? 'var(--color-state-idle)',
    }))
    .sort((a, b) => b.valor - a.valor);

  const produtos = Object.entries(dashboard.topProdutos)
    .map(([rotulo, valor]) => ({ rotulo, valor }))
    .sort((a, b) => b.valor - a.valor)
    .slice(0, 6);

  return (
    <section id="numeros" className="numeros" aria-labelledby="numeros-titulo">
      <div className="numeros-interior">
        <header className="numeros-cabecalho">
          <span className="ref">REF: I360 — 05</span>
          <h2 id="numeros-titulo" className="display numeros-titulo">
            Escala <em>medida</em>,
            <br />
            não prometida.
          </h2>
        </header>

        <Filete className="numeros-filete" />

        <ul className="numeros-marcos">
          {MARCOS.map((marco) => (
            <li key={marco.rotulo}>
              <Contador
                valor={marco.valor}
                decimais={marco.decimais ?? 0}
                className="numeros-valor"
              />
              <span className="numeros-rotulo">{marco.rotulo}</span>
              <span className="numeros-apoio">{marco.apoio}</span>
            </li>
          ))}
        </ul>

        <Entrada className="numeros-graficos" filhos>
          <Tendencia
            titulo="Reuniões analisadas por mês"
            descricao="Recorte de 2025, base consolidada."
            pontos={SERIE}
            unidade="reuniões"
            nota="Fonte: lote LOTE-2025-0001. Setembro parcial."
            cor="var(--color-data-1)"
          />

          <Rosca
            titulo="Distribuição de sentimento"
            descricao="Classificação automática das reuniões analisadas."
            fatias={fatias}
            miolo={{
              valor: `${Math.round(
                ((fatias[0]?.valor ?? 0) /
                  (fatias.reduce((soma, f) => soma + f.valor, 0) || 1)) *
                  100,
              )}%`,
              rotulo: fatias[0]?.rotulo ?? '',
            }}
            nota="Seis classes possíveis; nenhuma reunião fica sem classificação."
          />

          <Ranking
            titulo="Produtos mais citados"
            descricao="Menções extraídas das transcrições."
            itens={produtos}
            nota="Contagem por reunião, não por ocorrência no texto."
          />
        </Entrada>
      </div>
    </section>
  );
}
