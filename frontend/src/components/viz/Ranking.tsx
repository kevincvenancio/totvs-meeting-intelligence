/* Ranking em barras horizontais.

   A forma certa para "quem aparece mais": o rótulo fica na horizontal, legível
   sem inclinar a cabeça, e a ordem é a informação. Série única, então uma cor
   só — pintar cada barra de uma cor seria codificar a posição, e a posição já
   está codificada pela posição.

   Barras crescem ao entrar na viewport e cada uma mostra seu próprio valor:
   com poucas linhas, rótulo direto vence eixo. */

import { useEffect, useRef } from 'react';
import { gsap, movimentoReduzido } from '../../lib/motion';
import { Grafico } from './primitives';

export interface ItemRanking {
  rotulo: string;
  valor: number;
}

interface Props {
  titulo: string;
  descricao?: string;
  nota?: string;
  itens: ItemRanking[];
  cor?: string;
  unidade?: string;
  maximo?: number;
  className?: string;
}

export function Ranking({
  titulo,
  descricao,
  nota,
  itens,
  cor = 'var(--color-data-1)',
  unidade = 'menções',
  maximo,
  className,
}: Props) {
  const raiz = useRef<HTMLOListElement>(null);
  const teto = maximo ?? Math.max(...itens.map((i) => i.valor), 1);

  useEffect(() => {
    const elemento = raiz.current;
    if (!elemento || movimentoReduzido()) return;

    const barras = elemento.querySelectorAll('.ranking-preenchimento');
    const animacao = gsap.fromTo(
      barras,
      { scaleX: 0 },
      {
        scaleX: 1,
        duration: 1.1,
        ease: 'expo.out',
        stagger: 0.07,
        scrollTrigger: { trigger: elemento, start: 'top 88%', once: true },
      },
    );

    return () => {
      animacao.scrollTrigger?.kill();
      animacao.kill();
    };
  }, [itens]);

  return (
    <Grafico
      titulo={titulo}
      descricao={descricao}
      nota={nota}
      className={className}
      tabela={{
        colunas: ['Item', unidade.charAt(0).toUpperCase() + unidade.slice(1)],
        linhas: itens.map((i) => [i.rotulo, i.valor.toLocaleString('pt-BR')]),
      }}
    >
      <ol ref={raiz} className="ranking">
        {itens.map((item, indice) => (
          <li key={item.rotulo} className="ranking-linha">
            <span className="ranking-posicao numeric">{String(indice + 1).padStart(2, '0')}</span>
            <span className="ranking-rotulo">{item.rotulo}</span>
            <span className="ranking-trilho">
              <span
                className="ranking-preenchimento"
                style={{ width: `${(item.valor / teto) * 100}%`, background: cor }}
              />
            </span>
            <span className="ranking-valor numeric">{item.valor.toLocaleString('pt-BR')}</span>
          </li>
        ))}
      </ol>
    </Grafico>
  );
}
