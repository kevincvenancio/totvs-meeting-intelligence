/* ============================================================================
   Página inicial — o material do pitch.

   Cinco blocos de tela cheia, na ordem do roteiro da apresentação:
   problema → por que vale dinheiro → o que construímos → três níveis →
   impacto e entrada no painel. O passador de slides avança item a item
   dentro de cada bloco (ver ProvedorApresentacao); rolando com o mouse, a
   página funciona como uma landing comum.
   ========================================================================== */

import { TrilhoDeIndice, type Marco } from '../components/chrome/ScrollRail';
import { ProvedorApresentacao } from '../components/chrome/Apresentacao';
import { ROTEIRO } from '../lib/roteiro';
import { Problema } from '../sections/pitch/Problema';
import { Valor } from '../sections/pitch/Valor';
import { Pipeline } from '../sections/pitch/Pipeline';
import { Niveis } from '../sections/pitch/Niveis';
import { Impacto } from '../sections/pitch/Impacto';
import { Rodape } from '../sections/Rodape';

const MARCOS: Marco[] = [
  ...ROTEIRO.map(({ id, rotulo, ref }) => ({ id, rotulo, ref })),
  { id: 'contato', rotulo: 'Contato', ref: '06' },
];

export function Landing() {
  return (
    <ProvedorApresentacao>
      <TrilhoDeIndice marcos={MARCOS} />

      <main id="conteudo">
        <div id="pitch">
          <Problema />
          <Valor />
          <Pipeline />
          <Niveis />
          <Impacto />
        </div>
      </main>

      <Rodape />
    </ProvedorApresentacao>
  );
}
