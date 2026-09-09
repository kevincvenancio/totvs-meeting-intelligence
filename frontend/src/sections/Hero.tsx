/* ============================================================================
   Herói — seis camadas com profundidades diferentes.

   Do fundo para a frente: campo de brasa (WebGL) · malha · órbitas · título ·
   apoio · rodapé de dados. O ponteiro move cada uma numa proporção distinta,
   e o scroll faz a câmera atravessar o conjunto: o título sobe e desfoca, as
   órbitas se abrem, a brasa esfria. Nada disso é decorativo — é a mesma ideia
   do produto, camadas de leitura sobre uma conversa.
   ========================================================================== */

import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { gsap, irPara, useAnimacao } from '../lib/motion';
import { Orbitas } from '../components/brand/Logo';
import { CampoDeBrasa } from '../components/fx/EmberField';
import { Magnetico } from '../components/fx/primitives';

const DADOS_RODAPE = [
  { valor: '1.126', rotulo: 'reuniões analisadas' },
  { valor: '2.819', rotulo: 'insights extraídos' },
  { valor: '172 s', rotulo: 'para ler 86,3 MB' },
];

export function Heroi() {
  const [intensidade, setIntensidade] = useState(1);
  const palco = useRef<HTMLDivElement>(null);

  const raiz = useAnimacao<HTMLElement>(({ escopo, reduzido }) => {
    if (reduzido) return;

    /* ── Entrada ──────────────────────────────────────────────────────── */
    const entrada = gsap.timeline({ delay: 2.5 });

    entrada
      .from('.heroi-etiqueta', { opacity: 0, y: 20, duration: 0.9, ease: 'expo.out' })
      .from(
        '.heroi-titulo .heroi-linha span',
        { yPercent: 115, duration: 1.4, ease: 'expo.out', stagger: 0.11 },
        '-=0.6',
      )
      .from('.heroi-apoio', { opacity: 0, y: 26, duration: 1, ease: 'expo.out' }, '-=0.85')
      .from('.heroi-acoes > *', { opacity: 0, y: 22, duration: 0.9, ease: 'expo.out', stagger: 0.09 }, '-=0.7')
      .from('.heroi-rodape', { opacity: 0, duration: 1.1, ease: 'power2.out' }, '-=0.6')
      .from('.heroi-orbitas', { opacity: 0, scale: 0.86, duration: 2.2, ease: 'expo.out' }, '-=1.9');

    /* ── Parallax de ponteiro ─────────────────────────────────────────
       Cada camada tem seu próprio quickTo: interromper e redirecionar é
       muito mais barato do que criar um tween por evento de mouse.      */
    const camadas = gsap.utils.toArray<HTMLElement>('[data-profundidade]');
    const movedores = camadas.map((camada) => ({
      camada,
      profundidade: Number(camada.dataset.profundidade),
      x: gsap.quickTo(camada, 'x', { duration: 1.1, ease: 'power3' }),
      y: gsap.quickTo(camada, 'y', { duration: 1.1, ease: 'power3' }),
    }));

    function aoMover(evento: PointerEvent) {
      const nx = evento.clientX / window.innerWidth - 0.5;
      const ny = evento.clientY / window.innerHeight - 0.5;
      movedores.forEach(({ profundidade, x, y }) => {
        x(-nx * profundidade * 62);
        y(-ny * profundidade * 44);
      });
    }

    if (window.matchMedia('(pointer: fine)').matches) {
      window.addEventListener('pointermove', aoMover, { passive: true });
    }

    /* ── Travessia no scroll ──────────────────────────────────────────
       A câmera empurra para dentro: o título cresce e sai de foco, as
       órbitas se abrem, a brasa esfria. Tudo scrubado no mesmo trecho.  */
    const travessia = gsap.timeline({
      scrollTrigger: {
        trigger: escopo,
        start: 'top top',
        end: 'bottom top',
        scrub: 0.6,
        onUpdate: ({ progress }) => setIntensidade(1 - progress * 0.72),
      },
    });

    travessia
      .to('.heroi-titulo', { yPercent: -34, scale: 1.16, filter: 'blur(9px)', opacity: 0 }, 0)
      .to('.heroi-apoio, .heroi-acoes', { yPercent: -50, opacity: 0 }, 0)
      .to('.heroi-orbitas', { scale: 1.55, opacity: 0.12, rotate: 24 }, 0)
      .to('.heroi-malha', { yPercent: 16, opacity: 0 }, 0)
      .to('.heroi-rodape', { yPercent: 130, opacity: 0 }, 0)
      .to('.heroi-veu', { opacity: 1 }, 0.35);

    return () => window.removeEventListener('pointermove', aoMover);
  }, []);

  return (
    <section ref={raiz} id="heroi" className="heroi grain" aria-label="Insight360">
      {/* Camada 0 — brasa */}
      <CampoDeBrasa className="heroi-brasa" intensidade={intensidade} />

      {/* Camada 1 — malha */}
      <div className="heroi-malha blueprint" data-profundidade="0.15" aria-hidden="true" />

      {/* Camada 2 — órbitas */}
      <div className="heroi-orbitas" data-profundidade="0.4" aria-hidden="true">
        <Orbitas />
      </div>

      <div ref={palco} className="heroi-palco">
        <p className="heroi-etiqueta eyebrow" data-profundidade="0.62">
          <span className="ponto-vivo" aria-hidden="true" />
          TOTVS · Meeting Intelligence · v3.0
        </p>

        {/* Camada 3 — título */}
        <h1 className="heroi-titulo display" data-profundidade="0.9">
          <span className="heroi-linha">
            <span>Toda reunião já</span>
          </span>
          <span className="heroi-linha">
            <span>contém a <em>resposta</em>.</span>
          </span>
        </h1>

        {/* Camada 4 — apoio */}
        <p className="heroi-apoio lead" data-profundidade="1.05">
          O Insight360 lê a transcrição inteira, classifica completude, mede sentimento,
          calcula risco de churn e devolve o plano de ação — antes que o cliente peça a
          rescisão.
        </p>

        <div className="heroi-acoes" data-profundidade="1.2">
          <Magnetico>
            <Link to="/painel" className="botao botao--acento botao--grande" data-cursor="Ver dados">
              Abrir painel executivo
            </Link>
          </Magnetico>
          <Magnetico forca={0.2}>
            <button
              type="button"
              className="botao botao--fantasma botao--grande"
              onClick={() => irPara('#escuta', -20)}
              data-cursor="Descer"
            >
              Ver o método
            </button>
          </Magnetico>
        </div>
      </div>

      {/* Camada 5 — rodapé de dados */}
      <div className="heroi-rodape">
        <span className="ref">REF: I360 — 00</span>

        <ul className="heroi-dados">
          {DADOS_RODAPE.map((dado) => (
            <li key={dado.rotulo}>
              <span className="numeric heroi-dado-valor">{dado.valor}</span>
              <span className="heroi-dado-rotulo">{dado.rotulo}</span>
            </li>
          ))}
        </ul>

        <button
          type="button"
          className="heroi-descer"
          onClick={() => irPara('#escuta', -20)}
          aria-label="Ir para a próxima seção"
        >
          <span className="ref">Role</span>
          <span className="heroi-descer-linha" aria-hidden="true" />
        </button>
      </div>

      <div className="heroi-veu" aria-hidden="true" />
    </section>
  );
}
