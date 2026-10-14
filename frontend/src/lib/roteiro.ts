/* ============================================================================
   Roteiro do pitch — a fonte única da ordem, do tempo e dos passos.

   Cada bloco é uma seção de tela cheia da página inicial. `passos` é quantas
   vezes o passador (ou a seta do teclado) revela algo novo dentro do bloco
   antes de seguir para o próximo. Mudou a fala, muda aqui — a seção, o
   trilho lateral, a navegação e o modo apresentador leem desta lista.
   ========================================================================== */

export interface BlocoRoteiro {
  id: string;
  ref: string;
  rotulo: string;
  tempo: string;
  apresentador: string;
  passos: number;
}

export const ROTEIRO: BlocoRoteiro[] = [
  { id: 'problema', ref: '01', rotulo: 'O problema', tempo: '0:00–0:40', apresentador: 'Apresentador 1', passos: 4 },
  { id: 'valor', ref: '02', rotulo: 'Por que vale dinheiro', tempo: '0:40–1:15', apresentador: 'Apresentador 1', passos: 3 },
  { id: 'pipeline', ref: '03', rotulo: 'O que construímos', tempo: '1:15–2:30', apresentador: 'Apresentador 2', passos: 5 },
  { id: 'niveis', ref: '04', rotulo: 'Três níveis', tempo: '2:30–3:20', apresentador: 'Apresentador 2', passos: 5 },
  { id: 'impacto', ref: '05', rotulo: 'Impacto financeiro', tempo: '3:20–4:00', apresentador: 'Apresentador 3', passos: 5 },
];
