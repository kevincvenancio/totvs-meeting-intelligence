/* Cortina de entrada.

   Conta até 100 enquanto as fontes carregam, depois sobe em duas lâminas
   defasadas — a de trás sai antes, e por meio segundo se vê profundidade
   entre elas. É a primeira demonstração da gramática do site: camadas. */

import { useEffect, useRef, useState } from 'react';
import { gsap, movimentoReduzido, travarScroll } from '../../lib/motion';
import { Marca } from '../brand/Logo';

export function Preloader({ aoTerminar }: { aoTerminar?: () => void }) {
  const raiz = useRef<HTMLDivElement>(null);
  // Quem pediu menos movimento não vê cortina nenhuma: o estado já nasce no fim.
  const [contagem, setContagem] = useState(() => (movimentoReduzido() ? 100 : 0));
  const [fora, setFora] = useState(movimentoReduzido);

  useEffect(() => {
    if (movimentoReduzido()) {
      aoTerminar?.();
      return;
    }

    travarScroll(true);

    const progresso = { valor: 0 };
    const linha = gsap.timeline();

    linha
      .to(progresso, {
        valor: 100,
        duration: 1.9,
        ease: 'power2.inOut',
        onUpdate: () => setContagem(Math.round(progresso.valor)),
      })
      .to('.preloader-marca', { scale: 1.06, duration: 0.35, ease: 'expo.out' }, '-=0.25')
      .to('.preloader-conteudo', { opacity: 0, y: -18, duration: 0.45, ease: 'power3.in' })
      .to(
        '.preloader-lamina--tras',
        { yPercent: -100, duration: 1.05, ease: 'expo.inOut' },
        '-=0.2',
      )
      .to(
        '.preloader-lamina--frente',
        { yPercent: -100, duration: 1.15, ease: 'expo.inOut' },
        '<0.14',
      )
      .add(() => {
        travarScroll(false);
        setFora(true);
        aoTerminar?.();
      });

    return () => {
      linha.kill();
      travarScroll(false);
    };
  }, [aoTerminar]);

  if (fora) return null;

  return (
    <div ref={raiz} className="preloader" role="status" aria-live="polite">
      <div className="preloader-lamina preloader-lamina--tras" />
      <div className="preloader-lamina preloader-lamina--frente">
        <div className="preloader-conteudo">
          <div className="preloader-marca">
            <Marca tamanho={34} />
          </div>

          <div className="preloader-meio">
            <span className="eyebrow">Insight360 · Meeting Intelligence</span>
            <span className="preloader-contador numeric">{String(contagem).padStart(3, '0')}</span>
          </div>

          <div className="preloader-barra">
            <span style={{ transform: `scaleX(${contagem / 100})` }} />
          </div>
        </div>
      </div>
      <span className="sr-only">Carregando: {contagem}%</span>
    </div>
  );
}
