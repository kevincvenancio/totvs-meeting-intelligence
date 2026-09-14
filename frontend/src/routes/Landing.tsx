/* ============================================================================
   Página inicial — a sequência inteira de camadas.

   A ordem não é arbitrária: herói (promessa) → faixa (respiro) → escuta
   (demonstração) → camadas (profundidade) → pilares (plataforma) → risco
   (consequência) → números (prova) → revelação (virada de material) →
   rodapé (convite). Cada seção prepara a próxima.
   ========================================================================== */

import { Faixa } from '../components/fx/Marquee';
import { TrilhoDeIndice, type Marco } from '../components/chrome/ScrollRail';
import { Heroi } from '../sections/Hero';
import { Escuta } from '../sections/Escuta';
import { Camadas } from '../sections/Camadas';
import { Pilares } from '../sections/Pilares';
import { Risco } from '../sections/Risco';
import { Numeros } from '../sections/Numeros';
import { Revelacao } from '../sections/Revelacao';
import { Rodape } from '../sections/Rodape';

const MARCOS: Marco[] = [
  { id: 'heroi', rotulo: 'Abertura', ref: '00' },
  { id: 'escuta', rotulo: 'A escuta', ref: '01' },
  { id: 'camadas', rotulo: 'As camadas', ref: '02' },
  { id: 'pilares', rotulo: 'A plataforma', ref: '03' },
  { id: 'risco', rotulo: 'O risco', ref: '04' },
  { id: 'numeros', rotulo: 'A escala', ref: '05' },
  { id: 'revelacao', rotulo: 'O painel', ref: '06' },
  { id: 'contato', rotulo: 'Contato', ref: '07' },
];

/* Vocabulário do domínio, correndo. Serve de respiro entre a promessa do
   herói e a demonstração — e já introduz os termos que as próximas seções
   vão usar sem explicar de novo. */
const LEXICO = [
  'completude',
  'sentimento',
  'risco de churn',
  'score comercial',
  'plano de ação',
  'insight',
  'feedback educativo',
  'carteira',
  'lote de importação',
  'trecho de origem',
];

export function Landing() {
  return (
    <>
      <TrilhoDeIndice marcos={MARCOS} />

      <main id="conteudo">
        <Heroi />

        <div className="lexico" aria-hidden="true">
          <Faixa velocidade={38}>
            <span className="lexico-linha">
              {LEXICO.map((termo) => (
                <span key={termo} className="lexico-termo">
                  {termo}
                  <span className="lexico-separador">✳</span>
                </span>
              ))}
            </span>
          </Faixa>
          <Faixa velocidade={-26} className="lexico-inversa">
            <span className="lexico-linha lexico-linha--fina">
              {['/api/v1/reunioes', '/api/v1/dashboard', '/api/v1/planos-acao', '/api/v1/importacoes', '/api/v1/relatorios'].map(
                (rota) => (
                  <span key={rota} className="lexico-rota numeric">
                    {rota}
                    <span className="lexico-separador">·</span>
                  </span>
                ),
              )}
            </span>
          </Faixa>
        </div>

        <Escuta />
        <Camadas />
        <Pilares />
        <Risco />
        <Numeros />
        <Revelacao />
      </main>

      <Rodape />
    </>
  );
}
