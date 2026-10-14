/* ============================================================================
   Bloco 1 · O problema (0:00–0:40)

   O herói com o nome e a tese, e três trechos de transcrição que acendem um
   a um no ritmo da fala: uma reclamação, uma oportunidade, um sinal de churn.
   Antes de acender, cada trecho está lá, apagado — o dado já existe, só não
   foi lido. O último passo mostra o que ninguém viu: a reclamação e o churn
   são da mesma conta, com cinco meses de distância.
   ========================================================================== */

import { gsap, useAnimacao } from '../../lib/motion';
import { Orbitas } from '../../components/brand/Logo';
import { CampoDeBrasa } from '../../components/fx/EmberField';
import { revelar, usePasso } from '../../components/chrome/passos';

const TRECHOS = [
  {
    tipo: 'Reclamação',
    cor: 'var(--color-data-2)',
    reuniao: 'MTG-2025-0318',
    quando: 'mar · 2025',
    conta: 'Distribuidora Vale Norte',
    fala: 'Desde a atualização, o fechamento fiscal vira dois fins de semana. O chamado já foi reaberto três vezes.',
  },
  {
    tipo: 'Oportunidade',
    cor: 'var(--color-data-1)',
    reuniao: 'MTG-2025-0577',
    quando: 'mai · 2025',
    conta: 'Rede Farma Vida',
    fala: 'Se a integração com o e-commerce sair, a gente quer olhar o módulo de crédito ainda este ano.',
  },
  {
    tipo: 'Sinal de churn',
    cor: 'var(--color-state-critical)',
    reuniao: 'MTG-2025-0941',
    quando: 'ago · 2025',
    conta: 'Distribuidora Vale Norte',
    fala: 'A diretoria pediu proposta de outro fornecedor. Do jeito que está, não vamos renovar.',
  },
];

export function Problema() {
  const passo = usePasso('problema');

  const raiz = useAnimacao<HTMLElement>(({ reduzido }) => {
    if (reduzido) return;

    // Entra depois da cortina do preloader.
    gsap
      .timeline({ delay: 2.5 })
      .from('.problema-etiqueta', { opacity: 0, y: 20, duration: 0.9, ease: 'expo.out' })
      .from(
        '.problema-titulo .heroi-linha span',
        { yPercent: 115, duration: 1.4, ease: 'expo.out', stagger: 0.11 },
        '-=0.6',
      )
      .from('.problema-apoio', { opacity: 0, y: 26, duration: 1, ease: 'expo.out' }, '-=0.85')
      // O contêiner, não os cartões: o estado aceso/apagado de cada cartão é
      // do CSS, e um estilo inline deixado pelo tween passaria por cima dele.
      .from('.problema-grade', { opacity: 0, y: 30, duration: 1, ease: 'expo.out', clearProps: 'all' }, '-=0.7')
      .from('.heroi-orbitas', { opacity: 0, scale: 0.86, duration: 2.2, ease: 'expo.out' }, '-=1.9');
  }, []);

  return (
    <section ref={raiz} id="problema" className="pitch-bloco problema grain" aria-labelledby="problema-titulo">
      <CampoDeBrasa className="heroi-brasa" intensidade={0.85} />
      <div className="heroi-malha blueprint" aria-hidden="true" />
      <div className="heroi-orbitas" aria-hidden="true">
        <Orbitas />
      </div>

      <div className="pitch-interior problema-interior">
        <p className="problema-etiqueta eyebrow">
          <span className="ponto-vivo" aria-hidden="true" />
          Hermes · inteligência de reuniões para a TOTVS
        </p>

        <h1 id="problema-titulo" className="display problema-titulo">
          <span className="heroi-linha">
            <span>Toda reunião já</span>
          </span>
          <span className="heroi-linha">
            <span>
              contém a <em>resposta</em>.
            </span>
          </span>
        </h1>

        <p className="lead problema-apoio">
          A TOTVS já tem, por escrito, o que o cliente reclama, o que ele quer comprar e o que o faz
          pensar em sair. O problema é que isso fica <strong>preso dentro de cada conversa</strong>.
        </p>

        <div className="problema-trechos">
          <ul className="problema-grade">
            {TRECHOS.map((trecho, i) => (
              <li
                key={trecho.reuniao}
                className="problema-trecho"
                data-aceso={revelar(passo, i + 1)}
                style={{ '--cor-trecho': trecho.cor } as React.CSSProperties}
              >
                <div className="problema-trecho-topo">
                  <span className="problema-trecho-tipo">
                    <span className="problema-trecho-ponto" aria-hidden="true" />
                    {trecho.tipo}
                  </span>
                  <span className="ref numeric">{trecho.quando}</span>
                </div>
                <p className="problema-trecho-fala">“{trecho.fala}”</p>
                <div className="problema-trecho-base">
                  <span>{trecho.conta}</span>
                  <span className="ref numeric">{trecho.reuniao}</span>
                </div>
              </li>
            ))}
          </ul>

          <div className="problema-elo" data-visivel={revelar(passo, 4)}>
            <span className="problema-elo-linha" aria-hidden="true" />
            <span className="problema-elo-rotulo">
              Mesma conta, cinco meses depois — <strong>e ninguém ligou os pontos.</strong>
            </span>
          </div>
        </div>
      </div>
    </section>
  );
}
