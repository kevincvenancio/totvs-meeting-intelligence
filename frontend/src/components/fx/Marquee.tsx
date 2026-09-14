/* Faixa infinita cuja velocidade é a velocidade do scroll.

   Anda sozinha devagar; quando a página rola, acelera na direção do gesto e
   inclina ligeiramente. Ao parar, volta ao passo base. O efeito só funciona
   porque a base é lenta: se a faixa já corresse rápido, o empurrão do scroll
   não seria legível. */

import { useEffect, useRef, type ReactNode } from 'react';
import { gsap, movimentoReduzido, ScrollTrigger } from '../../lib/motion';

interface Props {
  children: ReactNode;
  /** Pixels por segundo no repouso. Negativo inverte o sentido. */
  velocidade?: number;
  /** Quantas cópias empilhar. 4 cobre telas ultrawide sem buraco. */
  copias?: number;
  className?: string;
}

export function Faixa({ children, velocidade = 42, copias = 4, className }: Props) {
  const raiz = useRef<HTMLDivElement>(null);
  const trilha = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const container = raiz.current;
    const conteudo = trilha.current;
    if (!container || !conteudo) return;
    if (movimentoReduzido()) return;

    const primeira = conteudo.firstElementChild as HTMLElement | null;
    if (!primeira) return;

    let largura = primeira.offsetWidth;
    let deslocamento = 0;
    let fator = 1; // multiplicador vindo da velocidade do scroll
    let inclinacao = 0;
    let ultimoQuadro = performance.now();

    const gatilho = ScrollTrigger.create({
      trigger: container,
      start: 'top bottom',
      end: 'bottom top',
      onUpdate: (self) => {
        // getVelocity vem em px/s; 1200 px/s é um scroll enérgico.
        const v = self.getVelocity();
        fator = 1 + Math.min(Math.abs(v) / 620, 7) * Math.sign(v || 1);
        inclinacao = gsap.utils.clamp(-7, 7, -v / 340);
      },
    });

    function passo(agora: number) {
      const delta = Math.min((agora - ultimoQuadro) / 1000, 0.05);
      ultimoQuadro = agora;

      // Volta suave ao passo base quando o scroll para.
      fator += (1 - fator) * 0.055;
      inclinacao += (0 - inclinacao) * 0.075;

      deslocamento -= velocidade * fator * delta;
      if (largura > 0) deslocamento = ((deslocamento % largura) + largura) % largura - largura;

      gsap.set(conteudo, { x: deslocamento, skewX: inclinacao });
      quadro = requestAnimationFrame(passo);
    }

    let quadro = requestAnimationFrame(passo);

    const observador = new ResizeObserver(() => {
      largura = primeira.offsetWidth;
    });
    observador.observe(primeira);

    return () => {
      cancelAnimationFrame(quadro);
      observador.disconnect();
      gatilho.kill();
    };
  }, [velocidade]);

  return (
    <div ref={raiz} className={`faixa ${className ?? ''}`} aria-hidden="true">
      <div ref={trilha} className="faixa-trilha">
        {Array.from({ length: copias }, (_, i) => (
          <div key={i} className="faixa-copia">
            {children}
          </div>
        ))}
      </div>
    </div>
  );
}
