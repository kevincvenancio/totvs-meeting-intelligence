/* Estado de passos do modo apresentação, compartilhado com as seções do
   pitch. Fica fora de Apresentacao.tsx para o Fast Refresh continuar
   funcionando naquele arquivo (ele só exporta componentes). */

import { createContext, useContext } from 'react';
import { ROTEIRO } from '../../lib/roteiro';

export const ContextoPassos = createContext<{ passos: number[] }>({ passos: ROTEIRO.map(() => 0) });

/** Passo atual do bloco: 0 é só o título; cada número revela mais um item. */
export function usePasso(id: string) {
  const { passos } = useContext(ContextoPassos);
  const indice = ROTEIRO.findIndex((bloco) => bloco.id === id);
  return passos[indice] ?? 0;
}

/** `data-visivel` quando o passo do bloco chegou em `k`. */
export const revelar = (passo: number, k: number) => (passo >= k ? true : undefined);
