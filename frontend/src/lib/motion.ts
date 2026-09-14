/* ============================================================================
   Infraestrutura de movimento.

   Um único lugar registra os plugins do GSAP, conhece a preferência de
   movimento reduzido e conecta o Lenis ao ticker — assim o scroll suave e o
   ScrollTrigger andam no mesmo relógio e nada fica meio quadro atrasado.
   ========================================================================== */

import { useLayoutEffect, useRef } from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { SplitText } from 'gsap/SplitText';
import { DrawSVGPlugin } from 'gsap/DrawSVGPlugin';
import Lenis from 'lenis';

gsap.registerPlugin(ScrollTrigger, SplitText, DrawSVGPlugin);

/* O ScrollTrigger não deve tentar corrigir a barra de endereço no mobile a
   cada pixel: só quando ela realmente termina de recolher. */
ScrollTrigger.config({ ignoreMobileResize: true });
ScrollTrigger.defaults({ markers: false });

export { gsap, ScrollTrigger, SplitText, DrawSVGPlugin };

/* Em desenvolvimento, o console consegue inspecionar os gatilhos:
   `__gsap.ScrollTrigger.getAll()` mostra start, end e progresso de cada um.
   Não existe no build de produção. */
if (import.meta.env.DEV && typeof window !== 'undefined') {
  (window as unknown as Record<string, unknown>).__gsap = { gsap, ScrollTrigger };
}

/** Curvas da casa — as mesmas do CSS, para que JS e CSS combinem. */
export const EASE = {
  expo: 'expo.out',
  quart: 'power4.inOut',
  soft: 'power2.out',
} as const;

export function movimentoReduzido() {
  return (
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

/* ── Lenis ──────────────────────────────────────────────────────────────── */

let lenis: Lenis | null = null;

export function iniciarScrollSuave() {
  if (lenis || movimentoReduzido()) return null;

  lenis = new Lenis({
    duration: 1.15,
    // Curva de saída exponencial: a página "assenta" em vez de parar seco.
    easing: (t: number) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
    wheelMultiplier: 0.9,
    touchMultiplier: 1.6,
    // Em toque o scroll nativo é melhor: mantém o momentum do sistema.
    syncTouch: false,
  });

  lenis.on('scroll', ScrollTrigger.update);

  const passo = (tempo: number) => lenis?.raf(tempo * 1000);
  gsap.ticker.add(passo);
  gsap.ticker.lagSmoothing(0);

  return () => {
    gsap.ticker.remove(passo);
    lenis?.destroy();
    lenis = null;
  };
}

export const obterLenis = () => lenis;

export function travarScroll(travado: boolean) {
  document.body.dataset.locked = String(travado);
  if (travado) lenis?.stop();
  else lenis?.start();
}

export function irPara(alvo: string | HTMLElement, deslocamento = 0) {
  if (lenis) {
    lenis.scrollTo(alvo, { offset: deslocamento, duration: 1.4 });
    return;
  }
  const el = typeof alvo === 'string' ? document.querySelector(alvo) : alvo;
  el?.scrollIntoView({ behavior: movimentoReduzido() ? 'auto' : 'smooth' });
}

/* ── Hook de contexto ───────────────────────────────────────────────────
   Todo componente animado usa este hook: o gsap.context recolhe sozinho
   tudo o que foi criado dentro dele quando o componente sai de cena.      */

export function useAnimacao<T extends HTMLElement = HTMLDivElement>(
  montar: (contexto: { escopo: T; reduzido: boolean }) => void,
  dependencias: unknown[] = [],
) {
  const referencia = useRef<T>(null);

  useLayoutEffect(() => {
    const escopo = referencia.current;
    if (!escopo) return;

    const reduzido = movimentoReduzido();
    const contexto = gsap.context(() => montar({ escopo, reduzido }), escopo);

    // Depois que fontes e imagens assentam, as medidas mudam.
    const refazer = () => ScrollTrigger.refresh();
    document.fonts?.ready.then(refazer).catch(() => {});

    return () => contexto.revert();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencias);

  return referencia;
}

/** Interpola valor por breakpoint sem sair do JS de animação. */
export const responsivo = <T,>(mobile: T, desktop: T) =>
  typeof window !== 'undefined' && window.innerWidth >= 1024 ? desktop : mobile;
