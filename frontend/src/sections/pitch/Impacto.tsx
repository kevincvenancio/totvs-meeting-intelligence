/* ============================================================================
   Bloco 5 · Impacto financeiro e entrada no painel (3:20–4:00)

   Três números, cada um com a premissa embaixo — a banca vai perguntar de
   onde vem cada um, e a resposta precisa estar na tela. Depois o total e o
   botão que leva à demonstração.

   Contas: 0,5% × R$ 5,5 bi = R$ 27,5 mi · 30 min × 220 dias × 1.000 pessoas
   = 110 mil h × R$ 75/h ≈ R$ 8,3 mi · soma com os R$ 23 mi de oportunidades
   ≈ R$ 58,8 mi.
   ========================================================================== */

import { Link } from 'react-router-dom';
import { revelar, usePasso } from '../../components/chrome/passos';
import { Magnetico } from '../../components/fx/primitives';
import { CabecalhoBloco, NumeroRevelado } from './comum';

const GANHOS = [
  {
    valor: 27.5,
    rotulo: 'em receita recorrente preservada',
    premissa: 'Churn 0,5 p.p. menor sobre um ARR de cerca de R$ 5,5 bi.',
    cor: 'var(--color-state-critical)',
  },
  {
    valor: 23,
    rotulo: 'em novas vendas na base',
    premissa: 'Oportunidades encontradas de forma sistemática: cerca de 0,4% do ARR.',
    cor: 'var(--color-data-1)',
  },
  {
    valor: 8.3,
    rotulo: 'em eficiência do time comercial',
    premissa: '30 min/dia × 220 dias úteis × ~1.000 pessoas = 110 mil horas, a ~R$ 75/hora.',
    cor: 'var(--color-data-3)',
  },
];

export function Impacto() {
  const passo = usePasso('impacto');

  return (
    <section id="impacto" className="pitch-bloco impacto" data-mode="light" aria-labelledby="impacto-titulo">
      <div className="pitch-interior">
        <CabecalhoBloco
          id="impacto"
          titulo={
            <>
              Quanto isso <em>vale</em>?
            </>
          }
          apoio="Números públicos da TOTVS e premissas conservadoras."
        />

        <ul className="impacto-ganhos">
          {GANHOS.map((ganho, i) => (
            <li
              key={ganho.rotulo}
              className="impacto-ganho revela"
              data-visivel={revelar(passo, i + 1)}
              style={{ '--cor-ganho': ganho.cor } as React.CSSProperties}
            >
              <span className="impacto-valor">
                <span className="impacto-moeda">R$</span>
                <NumeroRevelado
                  valor={ganho.valor}
                  decimais={ganho.valor % 1 ? 1 : 0}
                  visivel={passo >= i + 1}
                />
                <span className="impacto-unidade">mi</span>
              </span>
              <span className="impacto-rotulo">{ganho.rotulo}</span>
              <span className="impacto-premissa">{ganho.premissa}</span>
            </li>
          ))}
        </ul>

        <div className="impacto-fecho">
          <div className="impacto-total revela" data-visivel={revelar(passo, 4)}>
            <span className="eyebrow">Total por ano</span>
            <span className="impacto-total-valor display">
              ≈ R$ <NumeroRevelado valor={58.8} decimais={1} visivel={passo >= 4} /> mi
            </span>
            <span className="impacto-total-apoio">a partir de algo que a TOTVS já tem: as conversas.</span>
          </div>

          <div className="impacto-chamada revela" data-visivel={revelar(passo, 5)}>
            <p className="impacto-promessa">
              Mas número em tela é promessa. <em>Vamos mostrar como funciona.</em>
            </p>
            <Magnetico>
              <Link to="/painel" className="botao botao--acento botao--grande" data-cursor="Abrir">
                Abrir painel
              </Link>
            </Magnetico>
          </div>
        </div>
      </div>
    </section>
  );
}
