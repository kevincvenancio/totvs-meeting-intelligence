/* ============================================================================
   Primitivas de movimento reutilizadas em toda a página.

   Cada uma respeita "movimento reduzido" entregando o estado final, nunca um
   estado intermediário: quem desliga animação recebe o conteúdo pronto, não
   um texto invisível esperando um gatilho que não vem.
   ========================================================================== */

import {
  createElement,
  useEffect,
  useRef,
  type ElementType,
  type ReactNode,
} from 'react';
import { gsap, movimentoReduzido, ScrollTrigger, SplitText } from '../../lib/motion';

/* ── Texto que sobe por linha, com máscara ──────────────────────────────── */

interface TextoProps {
  children: ReactNode;
  como?: ElementType;
  className?: string;
  /** 'linhas' para blocos de display, 'palavras' para parágrafos longos. */
  por?: 'linhas' | 'palavras' | 'caracteres';
  atraso?: number;
  escalonamento?: number;
  /** Dispara na entrada da viewport (padrão) ou imediatamente. */
  aoEntrar?: boolean;
}

export function TextoRevelado({
  children,
  como = 'div',
  className,
  por = 'linhas',
  atraso = 0,
  escalonamento = 0.09,
  aoEntrar = true,
}: TextoProps) {
  const referencia = useRef<HTMLElement>(null);

  useEffect(() => {
    const elemento = referencia.current;
    if (!elemento || movimentoReduzido()) return;

    let divisao: SplitText | null = null;
    let animacao: gsap.core.Tween | null = null;
    let gatilho: ScrollTrigger | null = null;

    // As fontes precisam estar carregadas: dividir antes gera linhas erradas.
    const preparar = () => {
      divisao = new SplitText(elemento, {
        type: por === 'caracteres' ? 'chars,words' : por === 'palavras' ? 'words' : 'lines',
        linesClass: 'linha-dividida',
        mask: por === 'linhas' ? 'lines' : undefined,
        autoSplit: true,
      });

      const partes =
        por === 'caracteres' ? divisao.chars : por === 'palavras' ? divisao.words : divisao.lines;
      if (!partes?.length) return;

      gsap.set(partes, { yPercent: 118, opacity: por === 'linhas' ? 1 : 0 });

      animacao = gsap.to(partes, {
        yPercent: 0,
        opacity: 1,
        duration: 1.15,
        ease: 'expo.out',
        stagger: escalonamento,
        delay: atraso,
        paused: aoEntrar,
      });

      if (aoEntrar) {
        gatilho = ScrollTrigger.create({
          trigger: elemento,
          start: 'top 86%',
          once: true,
          onEnter: () => animacao?.play(),
        });
      }
    };

    document.fonts?.ready.then(preparar).catch(preparar);

    return () => {
      animacao?.kill();
      gatilho?.kill();
      divisao?.revert();
    };
  }, [por, atraso, escalonamento, aoEntrar]);

  /* O ref é repassado como prop, não lido: o elemento é dinâmico (`como`), e
     createElement é a forma correta de montá-lo. A regra não distingue as duas
     coisas. */
  // oxlint-disable-next-line react/refs
  return createElement(como, { ref: referencia, className }, children);
}

/* ── Bloco que entra deslizando ─────────────────────────────────────────── */

interface EntradaProps {
  children: ReactNode;
  className?: string;
  como?: ElementType;
  /** Deslocamento inicial em pixels. */
  de?: number;
  atraso?: number;
  /** Escalona os filhos diretos em vez do bloco inteiro. */
  filhos?: boolean;
}

export function Entrada({
  children,
  className,
  como = 'div',
  de = 44,
  atraso = 0,
  filhos = false,
}: EntradaProps) {
  const referencia = useRef<HTMLElement>(null);

  useEffect(() => {
    const elemento = referencia.current;
    if (!elemento || movimentoReduzido()) return;

    const alvos = filhos ? Array.from(elemento.children) : [elemento];
    gsap.set(alvos, { y: de, opacity: 0 });

    const animacao = gsap.to(alvos, {
      y: 0,
      opacity: 1,
      duration: 1.05,
      ease: 'expo.out',
      stagger: filhos ? 0.085 : 0,
      delay: atraso,
      scrollTrigger: { trigger: elemento, start: 'top 88%', once: true },
    });

    return () => {
      animacao.scrollTrigger?.kill();
      animacao.kill();
      gsap.set(alvos, { clearProps: 'all' });
    };
  }, [de, atraso, filhos]);

  /* O ref é repassado como prop, não lido: o elemento é dinâmico (`como`), e
     createElement é a forma correta de montá-lo. A regra não distingue as duas
     coisas. */
  // oxlint-disable-next-line react/refs
  return createElement(como, { ref: referencia, className }, children);
}

/* ── Filete que se desenha ──────────────────────────────────────────────── */

export function Filete({ className }: { className?: string }) {
  const referencia = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const elemento = referencia.current;
    if (!elemento || movimentoReduzido()) return;

    const animacao = gsap.fromTo(
      elemento,
      { scaleX: 0 },
      {
        scaleX: 1,
        duration: 1.5,
        ease: 'expo.inOut',
        scrollTrigger: { trigger: elemento, start: 'top 92%', once: true },
      },
    );

    return () => {
      animacao.scrollTrigger?.kill();
      animacao.kill();
    };
  }, []);

  return <div ref={referencia} className={`rule ${className ?? ''}`} />;
}

/* ── Botão magnético ────────────────────────────────────────────────────── */

export function Magnetico({
  children,
  className,
  forca = 0.32,
}: {
  children: ReactNode;
  className?: string;
  forca?: number;
}) {
  const referencia = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const elemento = referencia.current;
    if (!elemento || movimentoReduzido()) return;
    if (!window.matchMedia('(pointer: fine)').matches) return;

    const x = gsap.quickTo(elemento, 'x', { duration: 0.55, ease: 'elastic.out(1, 0.55)' });
    const y = gsap.quickTo(elemento, 'y', { duration: 0.55, ease: 'elastic.out(1, 0.55)' });

    function aoMover(evento: PointerEvent) {
      const caixa = elemento!.getBoundingClientRect();
      x((evento.clientX - (caixa.left + caixa.width / 2)) * forca);
      y((evento.clientY - (caixa.top + caixa.height / 2)) * forca);
    }

    const aoSair = () => {
      x(0);
      y(0);
    };

    elemento.addEventListener('pointermove', aoMover);
    elemento.addEventListener('pointerleave', aoSair);

    return () => {
      elemento.removeEventListener('pointermove', aoMover);
      elemento.removeEventListener('pointerleave', aoSair);
    };
  }, [forca]);

  return (
    <span ref={referencia} className={`magnetico ${className ?? ''}`}>
      {children}
    </span>
  );
}

/* ── Parallax por camada ────────────────────────────────────────────────
   `profundidade` negativa move contra o scroll (parece mais longe),
   positiva move junto e mais rápido (parece mais perto).                  */

export function Camada({
  children,
  profundidade = 0.2,
  className,
  como = 'div',
}: {
  children: ReactNode;
  profundidade?: number;
  className?: string;
  como?: ElementType;
}) {
  const referencia = useRef<HTMLElement>(null);

  useEffect(() => {
    const elemento = referencia.current;
    if (!elemento || movimentoReduzido()) return;

    const animacao = gsap.to(elemento, {
      yPercent: profundidade * 100,
      ease: 'none',
      scrollTrigger: {
        trigger: elemento.parentElement ?? elemento,
        start: 'top bottom',
        end: 'bottom top',
        scrub: 0.8,
      },
    });

    return () => {
      animacao.scrollTrigger?.kill();
      animacao.kill();
    };
  }, [profundidade]);

  /* O ref é repassado como prop, não lido: o elemento é dinâmico (`como`), e
     createElement é a forma correta de montá-lo. A regra não distingue as duas
     coisas. */
  // oxlint-disable-next-line react/refs
  return createElement(como, { ref: referencia, className }, children);
}

/* ── Contador ───────────────────────────────────────────────────────────── */

export function Contador({
  valor,
  duracao = 2.1,
  className,
  sufixo = '',
  decimais = 0,
}: {
  valor: number;
  duracao?: number;
  className?: string;
  sufixo?: string;
  decimais?: number;
}) {
  const referencia = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const elemento = referencia.current;
    if (!elemento) return;

    const formatar = (n: number) =>
      n.toLocaleString('pt-BR', {
        minimumFractionDigits: decimais,
        maximumFractionDigits: decimais,
      }) + sufixo;

    if (movimentoReduzido()) {
      elemento.textContent = formatar(valor);
      return;
    }

    const estado = { n: 0 };
    const animacao = gsap.to(estado, {
      n: valor,
      duration: duracao,
      ease: 'expo.out',
      onUpdate: () => {
        elemento.textContent = formatar(estado.n);
      },
      scrollTrigger: { trigger: elemento, start: 'top 90%', once: true },
    });

    return () => {
      animacao.scrollTrigger?.kill();
      animacao.kill();
    };
  }, [valor, duracao, sufixo, decimais]);

  return (
    <span ref={referencia} className={`numeric ${className ?? ''}`}>
      0{sufixo}
    </span>
  );
}
