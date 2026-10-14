/* Peças pequenas que os cinco blocos do pitch compartilham. */

import { useEffect, useRef } from 'react';
import { gsap, movimentoReduzido } from '../../lib/motion';
import { ROTEIRO } from '../../lib/roteiro';

/** Cabeçalho comum: referência do bloco, título e apoio opcional. */
export function CabecalhoBloco({
  id,
  titulo,
  apoio,
}: {
  id: string;
  titulo: React.ReactNode;
  apoio?: React.ReactNode;
}) {
  const bloco = ROTEIRO.find((b) => b.id === id);
  return (
    <header className="pitch-cabecalho">
      <span className="ref">
        {bloco?.ref} — {bloco?.rotulo}
      </span>
      <h2 id={`${id}-titulo`} className="display pitch-titulo">
        {titulo}
      </h2>
      {apoio && <p className="lead pitch-apoio">{apoio}</p>}
    </header>
  );
}

/** Número que conta até o valor no momento em que é revelado — não ao rolar. */
export function NumeroRevelado({
  valor,
  visivel,
  decimais = 0,
  prefixo = '',
  sufixo = '',
  className,
}: {
  valor: number;
  visivel: boolean;
  decimais?: number;
  prefixo?: string;
  sufixo?: string;
  className?: string;
}) {
  const referencia = useRef<HTMLSpanElement>(null);
  const formatar = (n: number) =>
    prefixo +
    n.toLocaleString('pt-BR', { minimumFractionDigits: decimais, maximumFractionDigits: decimais }) +
    sufixo;

  useEffect(() => {
    const elemento = referencia.current;
    if (!elemento) return;
    if (!visivel || movimentoReduzido()) {
      elemento.textContent = formatar(visivel ? valor : 0);
      return;
    }
    const estado = { n: 0 };
    const animacao = gsap.to(estado, {
      n: valor,
      duration: 1.6,
      ease: 'expo.out',
      onUpdate: () => {
        elemento.textContent = formatar(estado.n);
      },
    });
    return () => {
      animacao.kill();
    };
    // formatar depende só das props listadas
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visivel, valor, decimais, prefixo, sufixo]);

  return (
    <span ref={referencia} className={`numeric ${className ?? ''}`}>
      {formatar(valor)}
    </span>
  );
}
