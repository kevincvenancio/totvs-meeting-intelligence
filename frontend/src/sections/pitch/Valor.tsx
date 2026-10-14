/* ============================================================================
   Bloco 2 · Por que isso vale dinheiro (0:40–1:15)

   A ponte entre o problema e a solução: dois números grandes, cada um com a
   fonte embaixo, e a conclusão que leva ao Hermes.
   ========================================================================== */

import { revelar, usePasso } from '../../components/chrome/passos';
import { CabecalhoBloco } from './comum';

const DADOS = [
  {
    valor: '5–25×',
    texto: 'é quanto custa a mais conquistar um cliente novo do que manter um que já está na base.',
    fonte:
      'Harvard Business Review, “The Value of Keeping the Right Customers” (2014), a partir de pesquisas da Bain & Company.',
  },
  {
    valor: '~30%',
    texto: 'de melhora na retenção reportada por empresas que usam inteligência conversacional.',
    fonte: 'Talkmap, estudo com operações de telecom, bancos e seguros nos EUA (dado reportado pelo fornecedor).',
  },
];

export function Valor() {
  const passo = usePasso('valor');

  return (
    <section id="valor" className="pitch-bloco valor" aria-labelledby="valor-titulo">
      <div className="pitch-interior">
        <CabecalhoBloco
          id="valor"
          titulo={
            <>
              Esses sinais valem <em>dinheiro</em>.
            </>
          }
        />

        <div className="valor-dados">
          {DADOS.map((dado, i) => (
            <figure key={dado.valor} className="valor-dado revela" data-visivel={revelar(passo, i + 1)}>
              <span className="valor-numero display">{dado.valor}</span>
              <p className="valor-texto">{dado.texto}</p>
              <figcaption className="valor-fonte">Fonte: {dado.fonte}</figcaption>
            </figure>
          ))}
        </div>

        <p className="valor-conclusao revela" data-visivel={revelar(passo, 3)}>
          O caminho mais barato para a TOTVS crescer é a <em>base que ela já tem</em> — e o que essa
          base já está dizendo nas reuniões.
        </p>
      </div>
    </section>
  );
}
