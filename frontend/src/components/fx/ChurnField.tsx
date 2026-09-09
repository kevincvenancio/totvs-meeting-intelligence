/* ============================================================================
   Campo de churn — 1.126 partículas, uma por reunião analisada.

   Começam como uma nuvem sem forma: é a base recém-importada, antes de
   qualquer leitura. Conforme o scroll avança, cada partícula viaja até o
   grupo do seu risco. O que era ruído vira três colunas com tamanhos
   diferentes — que é exatamente o que a classificação faz com a base.

   Custo: as posições de origem e destino são calculadas uma vez; o laço só
   interpola e desenha. Sem alocação por quadro, sem sombra, sem gradiente
   por partícula.
   ========================================================================== */

import { useEffect, useRef } from 'react';
import { movimentoReduzido } from '../../lib/motion';

const TOTAL = 1126;

const GRUPOS = [
  { chave: 'BAIXO', proporcao: 0.64, cor: '42, 151, 112', centro: 0.2 },
  { chave: 'MEDIO', proporcao: 0.235, cor: '181, 132, 18', centro: 0.5 },
  { chave: 'ALTO', proporcao: 0.125, cor: '207, 67, 64', centro: 0.8 },
] as const;

interface Particula {
  origemX: number;
  origemY: number;
  destinoX: number;
  destinoY: number;
  x: number;
  y: number;
  raio: number;
  cor: string;
  fase: number;
}

export function CampoDeChurn({
  progresso,
  className,
}: {
  progresso: number;
  className?: string;
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const progressoRef = useRef(progresso);

  /* O progresso chega a cada quadro de scroll; o laço de desenho lê o ref
     para não ser recriado. A escrita fica num efeito próprio. */
  useEffect(() => {
    progressoRef.current = progresso;
  }, [progresso]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d', { alpha: true });
    if (!ctx) return;

    let particulas: Particula[] = [];
    let largura = 0;
    let altura = 0;
    let dpr = 1;

    /* Gerador determinístico: a mesma nuvem em toda carga da página. */
    let estado = 20250909;
    const rnd = () => {
      estado = (estado * 1664525 + 1013904223) % 4294967296;
      return estado / 4294967296;
    };

    function construir() {
      if (!canvas) return;
      const caixa = canvas.getBoundingClientRect();
      dpr = Math.min(window.devicePixelRatio || 1, 2);
      largura = caixa.width;
      altura = caixa.height;
      canvas.width = Math.floor(largura * dpr);
      canvas.height = Math.floor(altura * dpr);
      ctx!.setTransform(dpr, 0, 0, dpr, 0, 0);

      estado = 20250909;
      particulas = [];

      // Em telas pequenas menos partículas: a leitura é a mesma, o custo não.
      const quantidade = largura < 700 ? Math.round(TOTAL * 0.42) : TOTAL;

      for (const grupo of GRUPOS) {
        const doGrupo = Math.round(quantidade * grupo.proporcao);
        const colunaX = largura * grupo.centro;
        // Coluna proporcional ao tamanho do grupo — a forma já conta a história.
        const larguraColuna = Math.min(largura * 0.2, 40 + grupo.proporcao * largura * 0.34);
        const alturaColuna = altura * (0.28 + grupo.proporcao * 0.62);

        for (let i = 0; i < doGrupo; i++) {
          // Distribuição em disco para a coluna não ter cantos duros.
          const angulo = rnd() * Math.PI * 2;
          const distancia = Math.sqrt(rnd());

          particulas.push({
            origemX: rnd() * largura,
            origemY: altura * 0.12 + rnd() * altura * 0.76,
            destinoX: colunaX + Math.cos(angulo) * distancia * larguraColuna * 0.5,
            destinoY: altura * 0.58 + Math.sin(angulo) * distancia * alturaColuna * 0.5,
            x: 0,
            y: 0,
            raio: 0.7 + rnd() * 1.1,
            cor: grupo.cor,
            fase: rnd() * Math.PI * 2,
          });
        }
      }
    }

    /* Suavização: as partículas não chegam todas juntas — cada uma tem seu
       próprio atraso, derivado da fase, e o grupo se forma como enxame. */
    function desenhar(tempo: number) {
      if (!ctx) return;
      const p = progressoRef.current;
      ctx.clearRect(0, 0, largura, altura);

      for (let i = 0; i < particulas.length; i++) {
        const part = particulas[i];
        const atraso = (i % 90) / 90 * 0.28;
        const local = Math.max(0, Math.min(1, (p - atraso) / (1 - atraso)));
        // Curva de saída: rápido no começo, assentando no fim.
        const t = 1 - Math.pow(1 - local, 3);

        const deriva = movimentoReduzido() ? 0 : Math.sin(tempo * 0.0006 + part.fase) * (3 - t * 1.6);

        part.x = part.origemX + (part.destinoX - part.origemX) * t + deriva;
        part.y = part.origemY + (part.destinoY - part.origemY) * t + deriva * 0.6;

        ctx.beginPath();
        ctx.arc(part.x, part.y, part.raio, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(${part.cor}, ${0.2 + t * 0.62})`;
        ctx.fill();
      }
    }

    let quadro = 0;
    let visivel = true;

    function laco(tempo: number) {
      desenhar(tempo);
      quadro = visivel ? requestAnimationFrame(laco) : 0;
    }

    const observador = new IntersectionObserver(
      ([entrada]) => {
        visivel = entrada.isIntersecting;
        if (visivel && !quadro) quadro = requestAnimationFrame(laco);
      },
      { threshold: 0 },
    );

    construir();
    observador.observe(canvas);
    quadro = requestAnimationFrame(laco);

    const aoRedimensionar = () => {
      construir();
      desenhar(performance.now());
    };
    window.addEventListener('resize', aoRedimensionar);

    return () => {
      cancelAnimationFrame(quadro);
      observador.disconnect();
      window.removeEventListener('resize', aoRedimensionar);
    };
  }, []);

  return <canvas ref={canvasRef} className={className} aria-hidden="true" />;
}
