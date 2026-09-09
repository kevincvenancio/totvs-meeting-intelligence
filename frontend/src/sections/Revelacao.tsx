/* ============================================================================
   A virada de material.

   Até aqui o site inteiro é obsidiana. Nesta seção uma lâmina de papel abre a
   partir do centro — clip-path escalado pelo scroll — e por dentro dela está o
   produto de verdade, no modo claro. Da virada em diante a página continua em
   papel: a mudança é permanente, não um piscar.

   É o argumento da seção dito em material: a análise sai do escuro e vira algo
   que se leva para a mesa da diretoria.

   Nada aqui rola por dentro. Uma área rolável dentro de uma seção presa rouba
   a roda do mouse, o scroll da página congela e a animação trava no meio —
   por isso o conteúdo é dimensionado para caber na tela, não para rolar.
   ========================================================================== */

import { Link } from 'react-router-dom';
import { gsap, useAnimacao } from '../lib/motion';
import { dashboard } from '../lib/demoData';
import { corRisco, corSentimento, rotuloRisco, rotuloSentimento } from '../lib/format';
import type { RiscoChurn, Sentimento } from '../lib/types';

const KPIS = [
  { rotulo: 'Reuniões', valor: '1.126', apoio: 'analisadas' },
  { rotulo: 'Churn alto', valor: '143', apoio: '12,7% da base', tom: 'var(--color-state-critical)' },
  { rotulo: 'Oportunidades', valor: '419', apoio: 'score > 60' },
  { rotulo: 'Planos abertos', valor: '105', apoio: '23 atrasados', tom: 'var(--color-state-warn)' },
];

export function Revelacao() {
  const raiz = useAnimacao<HTMLElement>(({ escopo, reduzido }) => {
    if (reduzido) {
      gsap.set('.revelacao-papel', { clipPath: 'inset(0% round 0px)' });
      gsap.set('.revelacao-escuro', { opacity: 0 });
      return;
    }

    const linha = gsap.timeline({
      scrollTrigger: {
        trigger: escopo,
        start: 'top top',
        end: () => `+=${window.innerHeight * 2.2}`,
        pin: '.revelacao-quadro',
        scrub: 0.65,
        anticipatePin: 1,
        invalidateOnRefresh: true,
      },
    });

    linha
      // A lâmina abre do centro; o raio some junto com a borda.
      .fromTo(
        '.revelacao-papel',
        { clipPath: 'inset(46% 42% round 18px)' },
        { clipPath: 'inset(0% 0% round 0px)', duration: 1.6, ease: 'power2.inOut' },
        0,
      )
      .to('.revelacao-escuro', { opacity: 0, duration: 0.9 }, 0.5)
      .fromTo('.revelacao-painel', { scale: 1.12 }, { scale: 1, duration: 1.6, ease: 'power2.inOut' }, 0)
      .fromTo(
        '.revelacao-anima',
        { opacity: 0, y: 30 },
        { opacity: 1, y: 0, duration: 0.7, stagger: 0.11, ease: 'expo.out' },
        0.95,
      )
      .to({}, { duration: 0.45 });
  }, []);

  const sentimentos = Object.entries(dashboard.sentimentos)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 4);
  const totalSentimentos = sentimentos.reduce((soma, [, v]) => soma + v, 0) || 1;

  return (
    <section ref={raiz} id="revelacao" className="revelacao" aria-labelledby="revelacao-titulo">
      <div className="revelacao-quadro">
        {/* Camada escura — o que estava valendo até aqui */}
        <div className="revelacao-escuro grain">
          <p className="ref">REF: I360 — 06</p>
          <h2 className="display revelacao-chamada">
            E então isso
            <br />
            vai para a mesa.
          </h2>
        </div>

        {/* Camada de papel — abre por cima */}
        <div className="revelacao-papel" data-mode="light">
          <div className="revelacao-painel">
            <div className="revelacao-conteudo">
              <div className="revelacao-coluna">
                <header className="revelacao-cabecalho revelacao-anima">
                  <span className="ref">Painel executivo · /api/v1/dashboard</span>
                  <h2 id="revelacao-titulo" className="display revelacao-titulo">
                    O mesmo dado,
                    <br />
                    agora <em>legível</em>.
                  </h2>
                  <p className="lead">
                    Modo claro não é o escuro invertido: os passos de cor dos dados são outros,
                    escolhidos e validados contra o papel. Contraste, daltonismo e ordem das séries
                    passam nos dois modos.
                  </p>
                </header>

                <div className="revelacao-acoes revelacao-anima">
                  <Link to="/painel" className="botao botao--acento botao--grande" data-cursor="Abrir">
                    Entrar no painel
                  </Link>
                  <a
                    href="/swagger-ui.html"
                    className="botao botao--contorno botao--grande"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Ver a API no Swagger
                  </a>
                </div>
              </div>

              {/* Recorte fiel do painel real */}
              <div className="revelacao-vitrine revelacao-anima">
                <div className="revelacao-kpis">
                  {KPIS.map((kpi) => (
                    <div
                      key={kpi.rotulo}
                      className="revelacao-kpi"
                      style={{ '--tom': kpi.tom } as React.CSSProperties}
                    >
                      <span className="revelacao-kpi-rotulo">{kpi.rotulo}</span>
                      <span className="revelacao-kpi-valor numeric">{kpi.valor}</span>
                      <span className="revelacao-kpi-apoio">{kpi.apoio}</span>
                    </div>
                  ))}
                </div>

                <div className="revelacao-barra-empilhada">
                  <span className="revelacao-vitrine-titulo">Sentimento predominante</span>
                  <div className="revelacao-empilhada" role="img" aria-label="Distribuição de sentimento">
                    {sentimentos.map(([chave, valor]) => (
                      <span
                        key={chave}
                        style={{
                          width: `${(valor / totalSentimentos) * 100}%`,
                          background: corSentimento[chave as Sentimento],
                        }}
                        title={`${rotuloSentimento[chave as Sentimento]}: ${valor}`}
                      />
                    ))}
                  </div>
                  <ul className="revelacao-legenda">
                    {sentimentos.map(([chave, valor]) => (
                      <li key={chave}>
                        <span
                          className="revelacao-marca"
                          style={{ background: corSentimento[chave as Sentimento] }}
                          aria-hidden="true"
                        />
                        {rotuloSentimento[chave as Sentimento]}
                        <span className="numeric">{valor}</span>
                      </li>
                    ))}
                  </ul>
                </div>

                <ul className="revelacao-fila">
                  <li className="revelacao-fila-titulo">
                    <span className="revelacao-vitrine-titulo">Fila de risco</span>
                    <span className="ref">ordenada por score</span>
                  </li>
                  {dashboard.topRiscoChurn.slice(0, 3).map((reuniao) => (
                    <li key={reuniao.id} className="revelacao-fila-linha">
                      <span
                        className="revelacao-selo"
                        style={{ '--tom': corRisco[reuniao.riscoChurn as RiscoChurn] } as React.CSSProperties}
                      >
                        {rotuloRisco[reuniao.riscoChurn as RiscoChurn]}
                      </span>
                      <span className="revelacao-fila-cliente">{reuniao.cliente}</span>
                      <span className="numeric revelacao-fila-score">{reuniao.scoreComercial}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
