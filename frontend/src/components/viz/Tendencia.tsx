/* Série temporal em área.

   Um eixo só, sempre. Duas medidas de escalas diferentes viram dois gráficos,
   nunca dois eixos y no mesmo desenho — é a forma mais confiável de fazer uma
   correlação aparecer onde não existe.

   A linha tem 2px, a área é a mesma cor a 12% e o grid recua para quase nada.
   O cruzamento com balão é padrão: um gráfico em HTML é interativo por
   natureza, e esconder o valor exato atrás de "passe o mouse" só é aceitável
   porque o botão de tabela está do lado. */

import { useEffect, useMemo, useRef, useState } from 'react';
import { gsap, movimentoReduzido, ScrollTrigger } from '../../lib/motion';
import { Grafico } from './primitives';

export interface PontoSerie {
  rotulo: string;
  valor: number;
}

interface Props {
  titulo: string;
  descricao?: string;
  nota?: string;
  pontos: PontoSerie[];
  cor?: string;
  unidade?: string;
  altura?: number;
}

const LARGURA = 640;

export function Tendencia({
  titulo,
  descricao,
  nota,
  pontos,
  cor = 'var(--color-data-1)',
  unidade = '',
  altura = 220,
}: Props) {
  const svgRef = useRef<SVGSVGElement>(null);
  const [ativo, setAtivo] = useState<number | null>(null);

  const { caminhoLinha, caminhoArea, coordenadas, teto, marcas } = useMemo(() => {
    const valores = pontos.map((p) => p.valor);
    const maximo = Math.max(...valores, 1);
    // Teto "redondo": eixos que terminam em 137 dão a impressão de precisão falsa.
    const passo = Math.pow(10, Math.floor(Math.log10(maximo))) / 2;
    const tetoRedondo = Math.ceil(maximo / passo) * passo;

    const margemEsquerda = 46;
    const margemBaixo = 28;
    const margemTopo = 12;
    const largura = LARGURA - margemEsquerda - 8;
    const alturaUtil = altura - margemBaixo - margemTopo;

    const coords = pontos.map((ponto, i) => ({
      x: margemEsquerda + (i / Math.max(pontos.length - 1, 1)) * largura,
      y: margemTopo + alturaUtil - (ponto.valor / tetoRedondo) * alturaUtil,
      ...ponto,
    }));

    /* Curva monotônica: suaviza sem inventar picos entre os pontos, que é o
       defeito clássico da spline cardinal em série de negócio. */
    const linha = coords
      .map((c, i) => {
        if (i === 0) return `M ${c.x} ${c.y}`;
        const anterior = coords[i - 1];
        const meio = (anterior.x + c.x) / 2;
        return `C ${meio} ${anterior.y}, ${meio} ${c.y}, ${c.x} ${c.y}`;
      })
      .join(' ');

    const base = margemTopo + alturaUtil;
    const area = `${linha} L ${coords[coords.length - 1]?.x ?? 0} ${base} L ${coords[0]?.x ?? 0} ${base} Z`;

    const linhasGrade = [0, 0.25, 0.5, 0.75, 1].map((fracao) => ({
      y: margemTopo + alturaUtil - fracao * alturaUtil,
      valor: Math.round(tetoRedondo * fracao),
    }));

    return { caminhoLinha: linha, caminhoArea: area, coordenadas: coords, teto: tetoRedondo, marcas: linhasGrade };
  }, [pontos, altura]);

  useEffect(() => {
    const svg = svgRef.current;
    if (!svg || movimentoReduzido()) return;

    const linha = svg.querySelector('.tendencia-linha');
    const area = svg.querySelector('.tendencia-area');
    if (!linha) return;

    /* Duas decisões deliberadas aqui.

       Primeira: `fromTo`, nunca `from`. Em desenvolvimento o React monta o
       componente duas vezes, e um `from` na segunda montagem leria como estado
       final o estado inicial deixado pela primeira — a linha animaria de 0%
       para 0% e o gráfico ficaria em branco.

       Segunda: a linha do tempo nasce pausada e quem a toca é um
       ScrollTrigger explícito. Passar `scrollTrigger` direto no construtor de
       uma timeline não-scrubada não a faz avançar neste projeto (o gatilho
       fica ativo, a timeline destravada, e o tempo dela nunca anda). Com o
       `onEnter` explícito não há dúvida sobre quem dá o play. */
    const animacao = gsap
      .timeline({ paused: true })
      .fromTo(linha, { drawSVG: '0%' }, { drawSVG: '100%', duration: 1.6, ease: 'power2.inOut' })
      .fromTo(area, { opacity: 0 }, { opacity: 1, duration: 1.1 }, '-=1.1')
      .fromTo(
        svg.querySelectorAll('.tendencia-ponto'),
        { scale: 0, transformOrigin: 'center' },
        { scale: 1, duration: 0.5, stagger: 0.04, ease: 'back.out(2)' },
        '-=0.9',
      );

    const gatilho = ScrollTrigger.create({
      trigger: svg,
      start: 'top 88%',
      once: true,
      onEnter: () => animacao.play(),
    });

    // Chegou pelo meio da página (link direto, recarga com scroll restaurado):
    // não há entrada para esperar, o gráfico já deveria estar desenhado.
    if (gatilho.progress > 0) animacao.play();

    return () => {
      gatilho.kill();
      animacao.kill();
    };
  }, [caminhoLinha]);

  function aoMover(evento: React.PointerEvent<SVGSVGElement>) {
    const svg = svgRef.current;
    if (!svg || !coordenadas.length) return;
    const caixa = svg.getBoundingClientRect();
    const x = ((evento.clientX - caixa.left) / caixa.width) * LARGURA;

    let maisProximo = 0;
    let menorDistancia = Infinity;
    coordenadas.forEach((c, i) => {
      const distancia = Math.abs(c.x - x);
      if (distancia < menorDistancia) {
        menorDistancia = distancia;
        maisProximo = i;
      }
    });
    setAtivo(maisProximo);
  }

  const ponto = ativo != null ? coordenadas[ativo] : null;

  return (
    <Grafico
      titulo={titulo}
      descricao={descricao}
      nota={nota}
      tabela={{
        colunas: ['Período', unidade || 'Valor'],
        linhas: pontos.map((p) => [p.rotulo, p.valor.toLocaleString('pt-BR')]),
      }}
    >
      <div className="tendencia">
        <svg
          ref={svgRef}
          viewBox={`0 0 ${LARGURA} ${altura}`}
          className="tendencia-svg"
          role="img"
          aria-label={`${titulo}. De ${pontos[0]?.rotulo} a ${
            pontos[pontos.length - 1]?.rotulo
          }, variando entre ${Math.min(...pontos.map((p) => p.valor))} e ${Math.max(
            ...pontos.map((p) => p.valor),
          )} ${unidade}.`}
          onPointerMove={aoMover}
          onPointerLeave={() => setAtivo(null)}
        >
          <defs>
            <linearGradient id="tendencia-fundo" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={cor} stopOpacity="0.24" />
              <stop offset="100%" stopColor={cor} stopOpacity="0" />
            </linearGradient>
          </defs>

          {/* Grade recessiva: presente para medir, ausente para ler */}
          <g className="tendencia-grade">
            {marcas.map((marca) => (
              <g key={marca.valor}>
                <line x1="46" y1={marca.y} x2={LARGURA - 8} y2={marca.y} />
                <text x="38" y={marca.y + 4} textAnchor="end" className="tendencia-marca numeric">
                  {marca.valor >= 1000 ? `${(marca.valor / 1000).toFixed(0)}k` : marca.valor}
                </text>
              </g>
            ))}
          </g>

          <path className="tendencia-area" d={caminhoArea} fill="url(#tendencia-fundo)" />
          <path
            className="tendencia-linha"
            d={caminhoLinha}
            stroke={cor}
            strokeWidth="2"
            fill="none"
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {ponto && (
            <line
              className="tendencia-cruz"
              x1={ponto.x}
              y1={12}
              x2={ponto.x}
              y2={altura - 28}
            />
          )}

          {coordenadas.map((c, i) => (
            <circle
              key={c.rotulo}
              className="tendencia-ponto"
              cx={c.x}
              cy={c.y}
              r={ativo === i ? 5.5 : 3.5}
              fill={cor}
              // Anel na cor da superfície: separa o marcador da área por baixo.
              stroke="var(--surface-1)"
              strokeWidth="2"
            />
          ))}

          <g className="tendencia-eixo">
            {coordenadas.map((c, i) =>
              i % Math.ceil(coordenadas.length / 6) === 0 ? (
                <text key={c.rotulo} x={c.x} y={altura - 8} textAnchor="middle">
                  {c.rotulo}
                </text>
              ) : null,
            )}
          </g>
        </svg>

        {ponto && (
          <div
            className="tendencia-balao"
            style={{ left: `${(ponto.x / LARGURA) * 100}%`, top: `${(ponto.y / altura) * 100}%` }}
          >
            <span className="tendencia-balao-rotulo">{ponto.rotulo}</span>
            <span className="tendencia-balao-valor numeric">
              {ponto.valor.toLocaleString('pt-BR')} {unidade}
            </span>
          </div>
        )}
      </div>
      <span className="sr-only">Máximo do eixo: {teto}.</span>
    </Grafico>
  );
}
