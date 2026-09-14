/* ============================================================================
   "A reunião fala. O sistema escuta."

   A seção que demonstra o produto em vez de descrevê-lo. Fica presa na tela
   enquanto o scroll faz o papel do tempo: a transcrição avança fala por fala,
   os trechos que importam acendem, e cada insight entra pela direita no exato
   momento em que a frase que o originou é dita.

   Três camadas em velocidades diferentes: onda ao fundo (lenta), transcrição
   (média), cartões de insight (rápida). É a mesma conversa lida em três
   profundidades — que é literalmente o que o motor de análise faz.
   ========================================================================== */

import { useRef, useState } from 'react';
import { gsap, useAnimacao } from '../lib/motion';
import { INSIGHTS_VITRINE, TRANSCRICAO_VITRINE } from '../lib/demoData';

/* Amplitudes determinísticas: a "onda" precisa ser a mesma a cada carga. */
const ONDA = Array.from({ length: 96 }, (_, i) => {
  const base = Math.sin(i * 0.31) * 0.5 + Math.sin(i * 0.13 + 1.7) * 0.32 + Math.sin(i * 0.71) * 0.18;
  return 0.18 + Math.abs(base) * 0.82;
});

/* Posições das falas e dos cartões na linha do tempo. Ficam aqui fora para
   que o contador da coluna direita use exatamente os mesmos números da
   animação — um "02 / 04" que não bate com o que está na tela é pior do que
   não ter contador. */
const INICIO = 0.5;
const PASSO_FALA = 0.85;
const SOBRA_FINAL = 1.4;
const DURACAO_TOTAL = INICIO + TRANSCRICAO_VITRINE.length * PASSO_FALA + SOBRA_FINAL;

const ENTRADA_CARTAO = INSIGHTS_VITRINE.map(
  (insight) => (INICIO + insight.slot * PASSO_FALA + 0.35) / DURACAO_TOTAL,
);

const COR_MARCA: Record<string, string> = {
  dor: 'var(--color-data-1)',
  risco: 'var(--color-data-5)',
  churn: 'var(--color-state-critical)',
  oportunidade: 'var(--color-data-2)',
};

const COR_INSIGHT: Record<string, string> = {
  DOR: 'var(--color-data-1)',
  RISCO: 'var(--color-data-5)',
  CHURN: 'var(--color-state-critical)',
  OPORTUNIDADE: 'var(--color-data-2)',
};

export function Escuta() {
  const [progresso, setProgresso] = useState(0);
  const palco = useRef<HTMLDivElement>(null);

  const raiz = useAnimacao<HTMLElement>(({ escopo, reduzido }) => {
    if (reduzido) {
      // Sem movimento: tudo já visível, nada preso.
      gsap.set('.escuta-fala, .insight-cartao', { opacity: 1, x: 0, y: 0 });
      gsap.set('.escuta-fala', { '--realce': 1 });
      return;
    }

    const falas = gsap.utils.toArray<HTMLElement>('.escuta-fala');
    const cartoes = gsap.utils.toArray<HTMLElement>('.insight-cartao');

    gsap.set(falas, { opacity: 0.12 });
    gsap.set(cartoes, { opacity: 0, x: 70, scale: 0.94 });

    const linha = gsap.timeline({
      scrollTrigger: {
        trigger: escopo,
        start: 'top top',
        end: () => `+=${window.innerHeight * 3.4}`,
        pin: '.escuta-quadro',
        scrub: 0.75,
        anticipatePin: 1,
        onUpdate: ({ progress }) => setProgresso(progress),
      },
    });

    /* O cabeçalho sai de cena para dar o palco à conversa — e o palco sobe
       exatamente o que ele ocupava, senão fica um buraco no topo do quadro. */
    const cabecalho = escopo.querySelector<HTMLElement>('.escuta-cabecalho');
    linha
      .to(cabecalho, { opacity: 0, y: -50, duration: 0.55 }, 0)
      .to(
        '.escuta-palco',
        { y: () => -(cabecalho?.offsetHeight ?? 0) * 0.66, duration: 0.7, ease: 'power2.inOut' },
        0,
      );

    falas.forEach((fala, indice) => {
      const posicao = INICIO + indice * PASSO_FALA;

      linha
        .to(fala, { opacity: 1, duration: 0.4 }, posicao)
        .to(fala, { '--realce': 1, duration: 0.5 }, posicao + 0.1);

      // A coluna sobe o suficiente para manter a fala corrente no centro.
      linha.to(
        '.escuta-rolo',
        { y: -indice * 0.42 * fala.offsetHeight - indice * 9, duration: 0.85, ease: 'power2.inOut' },
        posicao,
      );

      // As falas antigas recuam, sem sumir: o contexto continua legível.
      if (indice > 0) {
        linha.to(falas[indice - 1], { opacity: 0.3, duration: 0.4 }, posicao + 0.15);
      }
    });

    // Cada cartão entra quando a fala que o originou é dita.
    INSIGHTS_VITRINE.forEach((insight, indice) => {
      const cartao = cartoes[indice];
      if (!cartao) return;
      const posicao = INICIO + insight.slot * PASSO_FALA + 0.35;

      linha
        .to(cartao, { opacity: 1, x: 0, scale: 1, duration: 0.7, ease: 'expo.out' }, posicao)
        .fromTo(
          cartao.querySelector('.insight-medidor span'),
          { scaleX: 0 },
          { scaleX: insight.confianca / 100, duration: 0.9, ease: 'expo.out' },
          posicao + 0.2,
        );
    });

    // Sobra de tempo no fim: o conjunto fica montado alguns quadros antes de sair.
    linha.to({}, { duration: SOBRA_FINAL });
  }, []);

  const faixaAtiva = Math.round(progresso * ONDA.length);

  return (
    <section ref={raiz} id="escuta" className="escuta" aria-labelledby="escuta-titulo">
      <div className="escuta-quadro grain">
        {/* Camada 0 — a onda da conversa */}
        <div className="escuta-onda" aria-hidden="true">
          {ONDA.map((amplitude, i) => (
            <span
              key={i}
              style={{
                height: `${amplitude * 100}%`,
                opacity: i <= faixaAtiva ? 0.5 : 0.12,
                background: i <= faixaAtiva ? 'var(--accent)' : 'currentColor',
              }}
            />
          ))}
        </div>

        <header className="escuta-cabecalho">
          <span className="ref">REF: HMS — 01</span>
          <h2 id="escuta-titulo" className="display escuta-titulo">
            A reunião fala.
            <br />
            O sistema <em>escuta</em>.
          </h2>
          <p className="lead">
            Role para acompanhar. À esquerda, a transcrição como ela chega. À direita, o que o
            motor de análise extrai enquanto ela acontece.
          </p>
        </header>

        <div ref={palco} className="escuta-palco">
          {/* Camada 1 — transcrição */}
          <div className="escuta-coluna">
            <div className="escuta-cabecalho-coluna">
              <span className="eyebrow">Transcrição bruta</span>
              <span className="ref">MTG-2025-1043</span>
            </div>

            <div className="escuta-janela">
              <div className="escuta-rolo">
                {TRANSCRICAO_VITRINE.map((item, i) => (
                  <p
                    key={i}
                    className="escuta-fala"
                    data-locutor={item.locutor}
                    data-marcada={item.marca ? 'true' : undefined}
                    style={
                      {
                        '--realce': 0,
                        '--cor-marca': item.marca ? COR_MARCA[item.marca] : 'transparent',
                      } as React.CSSProperties
                    }
                  >
                    <span className="escuta-locutor">{item.locutor}</span>
                    <span className="escuta-texto">{item.fala}</span>
                  </p>
                ))}
              </div>
            </div>
          </div>

          {/* Camada 2 — insights */}
          <div className="escuta-coluna escuta-coluna--insights">
            <div className="escuta-cabecalho-coluna">
              <span className="eyebrow">Extração automática</span>
              <span className="ref numeric">
                {String(ENTRADA_CARTAO.filter((entrada) => progresso >= entrada).length).padStart(2, '0')}
                {' / '}
                {String(INSIGHTS_VITRINE.length).padStart(2, '0')}
              </span>
            </div>

            <ul className="escuta-insights">
              {INSIGHTS_VITRINE.map((insight) => (
                <li
                  key={insight.tipo}
                  className="insight-cartao"
                  style={{ '--cor-insight': COR_INSIGHT[insight.tipo] } as React.CSSProperties}
                >
                  <div className="insight-topo">
                    <span className="insight-tipo">
                      <span className="insight-ponto" aria-hidden="true" />
                      {insight.rotulo}
                    </span>
                    <span className="ref numeric">{insight.confianca}%</span>
                  </div>

                  <p className="insight-descricao">{insight.descricao}</p>

                  <div className="insight-medidor" aria-hidden="true">
                    <span />
                  </div>
                  <span className="sr-only">Confiança da extração: {insight.confianca}%.</span>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* Régua de tempo */}
        <div className="escuta-regua" aria-hidden="true">
          <span className="ref">00:00</span>
          <span className="escuta-regua-trilho">
            <span style={{ transform: `scaleX(${progresso})` }} />
          </span>
          <span className="ref">41:12</span>
        </div>
      </div>
    </section>
  );
}
