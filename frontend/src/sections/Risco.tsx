/* ============================================================================
   "Onde o churn começa."

   A seção prende e o scroll faz a nuvem de 1.126 reuniões se separar em três
   grupos de risco. O texto entra em três tempos, cada um quando o grupo
   correspondente termina de se formar. É a única seção do site em que o
   número aparece antes da frase — aqui o dado é o argumento.
   ========================================================================== */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { gsap, useAnimacao } from '../lib/motion';
import { CampoDeChurn } from '../components/fx/ChurnField';
import { Contador } from '../components/fx/primitives';

const GRUPOS = [
  {
    chave: 'BAIXO',
    rotulo: 'Risco baixo',
    total: 719,
    cor: 'var(--color-state-good)',
    texto: 'Conversas com próximo passo combinado e sem menção a concorrente.',
  },
  {
    chave: 'MEDIO',
    rotulo: 'Risco médio',
    total: 264,
    cor: 'var(--color-state-warn)',
    texto: 'Sinal isolado: uma queixa operacional, ou uma decisão que subiu de nível.',
  },
  {
    chave: 'ALTO',
    rotulo: 'Risco alto',
    total: 143,
    cor: 'var(--color-state-critical)',
    texto: 'Dois ou mais sinais na mesma reunião. É a fila que precisa de ligação amanhã.',
  },
];

export function Risco() {
  const [progresso, setProgresso] = useState(0);

  const raiz = useAnimacao<HTMLElement>(({ escopo, reduzido }) => {
    if (reduzido) {
      setProgresso(1);
      return;
    }

    const linha = gsap.timeline({
      scrollTrigger: {
        trigger: escopo,
        start: 'top top',
        end: () => `+=${window.innerHeight * 2.6}`,
        pin: '.risco-quadro',
        scrub: 0.7,
        anticipatePin: 1,
        onUpdate: ({ progress }) => setProgresso(progress),
      },
    });

    linha
      .from('.risco-titulo', { opacity: 0, y: 46, duration: 0.7 }, 0.1)
      .from('.risco-apoio', { opacity: 0, y: 30, duration: 0.5 }, 0.35)
      .from(
        '.risco-grupo',
        { opacity: 0, y: 40, duration: 0.6, stagger: 0.55 },
        1.1,
      )
      .from('.risco-acao', { opacity: 0, y: 24, duration: 0.5 }, 3.1)
      .to({}, { duration: 0.6 });
  }, []);

  return (
    <section ref={raiz} id="risco" className="risco" aria-labelledby="risco-titulo">
      <div className="risco-quadro grain">
        <CampoDeChurn progresso={progresso} className="risco-campo" />

        <div className="risco-conteudo">
          <header className="risco-cabecalho">
            <span className="ref">REF: HMS — 04</span>
            <h2 id="risco-titulo" className="display risco-titulo">
              O churn não
              <br />
              chega de <em>surpresa</em>.
            </h2>
            <p className="lead risco-apoio">
              Cada ponto é uma reunião analisada. Antes da leitura, é ruído. Depois, é uma fila
              de trabalho com nome, prazo e responsável.
            </p>
          </header>

          <ul className="risco-grupos">
            {GRUPOS.map((grupo) => (
              <li
                key={grupo.chave}
                className="risco-grupo"
                style={{ '--cor-grupo': grupo.cor } as React.CSSProperties}
              >
                <span className="risco-marca" aria-hidden="true" />
                <Contador valor={grupo.total} className="risco-numero" />
                <span className="risco-rotulo">{grupo.rotulo}</span>
                <p className="risco-texto">{grupo.texto}</p>
              </li>
            ))}
          </ul>

          <div className="risco-acao">
            <Link to="/reunioes?riscoChurn=ALTO" className="botao botao--contorno" data-cursor="Abrir">
              Ver as 143 reuniões de risco alto
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
