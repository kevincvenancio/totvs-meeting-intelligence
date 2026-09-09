/* ============================================================================
   Fecho.

   Tipo em escala máxima, uma cortina que sobe revelando o rodapé por baixo, e
   o gesto de copiar o e-mail — o único momento do site em que a interface
   responde com uma palavra em vez de um movimento.
   ========================================================================== */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { gsap, useAnimacao } from '../lib/motion';
import { Faixa } from '../components/fx/Marquee';
import { Magnetico, TextoRevelado } from '../components/fx/primitives';
import { Marca } from '../components/brand/Logo';

const EMAIL = 'admin@insight360.com.br';

const COLUNAS = [
  {
    titulo: 'Plataforma',
    itens: [
      { rotulo: 'Painel executivo', para: '/painel' },
      { rotulo: 'Reuniões', para: '/reunioes' },
      { rotulo: 'Carteira de clientes', para: '/clientes' },
      { rotulo: 'Planos de ação', para: '/planos-acao' },
      { rotulo: 'Importações', para: '/importacoes' },
    ],
  },
  {
    titulo: 'Desenvolvedor',
    externos: [
      { rotulo: 'Swagger UI', para: '/swagger-ui.html' },
      { rotulo: 'OpenAPI 3 (JSON)', para: '/v3/api-docs' },
      { rotulo: 'Console H2', para: '/h2-console' },
    ],
  },
];

export function Rodape() {
  const [copiado, setCopiado] = useState(false);

  const raiz = useAnimacao<HTMLElement>(({ escopo, reduzido }) => {
    if (reduzido) return;

    gsap.from('.rodape-corpo', {
      yPercent: -28,
      ease: 'none',
      scrollTrigger: {
        trigger: escopo,
        start: 'top bottom',
        end: 'bottom bottom',
        scrub: 0.8,
      },
    });

    gsap.fromTo(
      '.rodape-assinatura-texto',
      { yPercent: 42, opacity: 0 },
      {
        yPercent: 0,
        opacity: 1,
        duration: 1.5,
        ease: 'expo.out',
        scrollTrigger: { trigger: '.rodape-assinatura', start: 'top 94%', once: true },
      },
    );
  }, []);

  async function copiar() {
    try {
      await navigator.clipboard.writeText(EMAIL);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2200);
    } catch {
      // Sem permissão de área de transferência o link mailto abaixo resolve.
      window.location.href = `mailto:${EMAIL}`;
    }
  }

  return (
    <footer ref={raiz} id="contato" className="rodape" data-mode="light">
      <Faixa velocidade={34} className="rodape-faixa">
        <span className="rodape-faixa-texto display">
          Toda reunião já contém a resposta&nbsp;&nbsp;·&nbsp;&nbsp;Insight360&nbsp;&nbsp;·&nbsp;&nbsp;
          Meeting Intelligence&nbsp;&nbsp;·&nbsp;&nbsp;
        </span>
      </Faixa>

      <div className="rodape-corpo">
        <div className="rodape-chamada">
          <TextoRevelado como="h2" className="display rodape-titulo" por="linhas">
            Vamos ler as suas reuniões.
          </TextoRevelado>

          <div className="rodape-acoes">
            <Magnetico>
              <button
                type="button"
                className="botao botao--acento botao--grande"
                onClick={copiar}
                data-cursor={copiado ? 'Copiado' : 'Copiar'}
              >
                {copiado ? 'E-mail copiado' : EMAIL}
              </button>
            </Magnetico>
            <Magnetico forca={0.2}>
              <Link to="/entrar" className="botao botao--contorno botao--grande">
                Entrar na plataforma
              </Link>
            </Magnetico>
          </div>
        </div>

        <div className="rodape-grade">
          <div className="rodape-marca">
            <Marca tamanho={30} />
            <p className="rodape-descricao">
              Insight360 — inteligência comercial sobre transcrições de reunião. API REST em{' '}
              <span className="numeric">/api/v1</span>, 31 recursos e 44 operações documentadas.
            </p>
          </div>

          {COLUNAS.map((coluna) => (
            <nav key={coluna.titulo} className="rodape-coluna" aria-label={coluna.titulo}>
              <h3 className="eyebrow">{coluna.titulo}</h3>
              <ul>
                {coluna.itens?.map((item) => (
                  <li key={item.para}>
                    <Link to={item.para}>{item.rotulo}</Link>
                  </li>
                ))}
                {coluna.externos?.map((item) => (
                  <li key={item.para}>
                    <a href={item.para} target="_blank" rel="noreferrer">
                      {item.rotulo} ↗
                    </a>
                  </li>
                ))}
              </ul>
            </nav>
          ))}

          <div className="rodape-coluna">
            <h3 className="eyebrow">Ficha técnica</h3>
            <ul className="rodape-ficha">
              <li>
                <span>Back-end</span>
                <span>Spring Boot · Java 17</span>
              </li>
              <li>
                <span>Persistência</span>
                <span>Oracle 19c · H2</span>
              </li>
              <li>
                <span>Front-end</span>
                <span>React 19 · GSAP · Vite</span>
              </li>
              <li>
                <span>Tipografia</span>
                <span>Instrument Serif · Inter Tight</span>
              </li>
            </ul>
          </div>
        </div>

        {/* Assinatura em escala máxima */}
        <div className="rodape-assinatura" aria-hidden="true">
          <svg viewBox="0 0 1000 150" preserveAspectRatio="xMidYMid meet">
            <text
              x="500"
              y="118"
              textAnchor="middle"
              className="rodape-assinatura-texto"
              fill="currentColor"
            >
              INSIGHT360
            </text>
          </svg>
        </div>

        <div className="rodape-base">
          <span className="ref">© {new Date().getFullYear()} TOTVS · Insight360 v3.0</span>
          <span className="ref">REF: I360 — FIM</span>
        </div>
      </div>
    </footer>
  );
}
