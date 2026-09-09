/* Cursor de dois corpos: um ponto que gruda no ponteiro e um anel que chega
   atrasado. Sobre elementos interativos o anel cresce e ganha rótulo — é como
   o site "fala" o que aquele alvo faz sem precisar de tooltip.

   Só existe em ponteiro fino (mouse/trackpad). Em toque, nada é criado. */

import { useEffect, useRef } from 'react';
import { gsap, movimentoReduzido } from '../../lib/motion';

export function Cursor() {
  const anel = useRef<HTMLDivElement>(null);
  const ponto = useRef<HTMLDivElement>(null);
  const rotulo = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const fino = window.matchMedia('(pointer: fine)').matches;
    if (!fino || movimentoReduzido()) return;

    document.documentElement.classList.add('cursor-proprio');

    const xAnel = gsap.quickTo(anel.current, 'x', { duration: 0.5, ease: 'power3' });
    const yAnel = gsap.quickTo(anel.current, 'y', { duration: 0.5, ease: 'power3' });
    const xPonto = gsap.quickTo(ponto.current, 'x', { duration: 0.09, ease: 'power2' });
    const yPonto = gsap.quickTo(ponto.current, 'y', { duration: 0.09, ease: 'power2' });

    function aoMover(evento: PointerEvent) {
      // Antes do primeiro movimento o cursor estaria em (0,0), desenhando um
      // arco solto no canto da tela.
      anel.current?.removeAttribute('data-oculto');
      ponto.current?.removeAttribute('data-oculto');
      xAnel(evento.clientX);
      yAnel(evento.clientY);
      xPonto(evento.clientX);
      yPonto(evento.clientY);
    }

    function alvoInterativo(elemento: Element | null) {
      return elemento?.closest<HTMLElement>(
        'a, button, [role="button"], input, select, textarea, [data-cursor]',
      );
    }

    function aoEntrar(evento: PointerEvent) {
      const alvo = alvoInterativo(evento.target as Element);
      if (!alvo) return;
      const texto = alvo.dataset.cursor ?? '';
      if (rotulo.current) rotulo.current.textContent = texto;
      anel.current?.setAttribute('data-estado', texto ? 'rotulo' : 'ativo');
    }

    function aoSair(evento: PointerEvent) {
      if (alvoInterativo(evento.relatedTarget as Element)) return;
      if (!alvoInterativo(evento.target as Element)) return;
      anel.current?.setAttribute('data-estado', 'ocioso');
      if (rotulo.current) rotulo.current.textContent = '';
    }

    const aoPressionar = () => anel.current?.setAttribute('data-pressionado', 'true');
    const aoSoltar = () => anel.current?.removeAttribute('data-pressionado');
    const aoSairDaJanela = () => anel.current?.setAttribute('data-oculto', 'true');
    const aoEntrarNaJanela = () => anel.current?.removeAttribute('data-oculto');

    window.addEventListener('pointermove', aoMover, { passive: true });
    document.addEventListener('pointerover', aoEntrar, true);
    document.addEventListener('pointerout', aoSair, true);
    window.addEventListener('pointerdown', aoPressionar);
    window.addEventListener('pointerup', aoSoltar);
    document.addEventListener('pointerleave', aoSairDaJanela);
    document.addEventListener('pointerenter', aoEntrarNaJanela);

    return () => {
      document.documentElement.classList.remove('cursor-proprio');
      window.removeEventListener('pointermove', aoMover);
      document.removeEventListener('pointerover', aoEntrar, true);
      document.removeEventListener('pointerout', aoSair, true);
      window.removeEventListener('pointerdown', aoPressionar);
      window.removeEventListener('pointerup', aoSoltar);
      document.removeEventListener('pointerleave', aoSairDaJanela);
      document.removeEventListener('pointerenter', aoEntrarNaJanela);
    };
  }, []);

  return (
    <>
      <div ref={anel} className="cursor-anel" data-estado="ocioso" data-oculto="true" aria-hidden="true">
        <span ref={rotulo} className="cursor-rotulo" />
      </div>
      <div ref={ponto} className="cursor-ponto" data-oculto="true" aria-hidden="true" />
    </>
  );
}
