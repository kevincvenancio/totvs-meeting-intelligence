/* ============================================================================
   Galeria horizontal presa.

   Quatro painéis do tamanho da tela que atravessam na horizontal enquanto a
   página fica presa na vertical. Dentro de cada painel, o texto e o desenho
   andam em velocidades diferentes — sem esse contra-movimento interno a
   travessia horizontal fica plana, parece um carrossel.

   Os desenhos são o mecanismo real de cada etapa, não ilustração: o que
   entra, o que o sistema faz com aquilo, o que sai.
   ========================================================================== */

import { gsap, useAnimacao } from '../lib/motion';

interface Pilar {
  ref: string;
  titulo: string;
  resumo: string;
  itens: string[];
  desenho: React.ReactNode;
}

/* ── Desenhos ───────────────────────────────────────────────────────────── */

function DesenhoImportacao() {
  return (
    <svg viewBox="0 0 320 220" fill="none" className="pilar-svg" aria-hidden="true">
      {/* Linhas do CSV entrando */}
      <g className="pilar-anima">
        {[0, 1, 2, 3, 4].map((i) => (
          <rect
            key={i}
            x="6"
            y={22 + i * 22}
            width={96 - i * 6}
            height="10"
            rx="2"
            fill="currentColor"
            opacity={0.14 + i * 0.04}
          />
        ))}
      </g>
      {/* Funil de validação */}
      <path d="M132 24 L206 24 L176 96 L176 156 L162 166 L162 96 Z" stroke="var(--accent)" strokeWidth="1.5" fill="none" />
      <text x="169" y="188" textAnchor="middle" className="pilar-legenda" fill="currentColor">
        validação
      </text>
      {/* Saídas classificadas */}
      <g>
        <rect x="230" y="30" width="84" height="34" rx="4" stroke="var(--color-state-good)" strokeWidth="1.2" fill="none" />
        <text x="272" y="51" textAnchor="middle" className="pilar-legenda" fill="currentColor">válidas</text>

        <rect x="230" y="76" width="84" height="34" rx="4" stroke="var(--color-state-warn)" strokeWidth="1.2" fill="none" />
        <text x="272" y="97" textAnchor="middle" className="pilar-legenda" fill="currentColor">incompletas</text>

        <rect x="230" y="122" width="84" height="34" rx="4" stroke="var(--color-state-idle)" strokeWidth="1.2" fill="none" strokeDasharray="3 3" />
        <text x="272" y="143" textAnchor="middle" className="pilar-legenda" fill="currentColor">duplicadas</text>
      </g>
      <g stroke="currentColor" strokeWidth="1" opacity=".4">
        <path d="M106 46 L130 40" />
        <path d="M184 60 L228 47" />
        <path d="M184 78 L228 93" />
        <path d="M180 120 L228 139" />
      </g>
    </svg>
  );
}

function DesenhoAnalise() {
  return (
    <svg viewBox="0 0 320 220" fill="none" className="pilar-svg" aria-hidden="true">
      {/* Três mostradores: completude, sentimento, churn */}
      {[
        { cx: 62, valor: 0.78, cor: 'var(--color-state-good)', rotulo: 'completude' },
        { cx: 160, valor: 0.54, cor: 'var(--color-data-5)', rotulo: 'sentimento' },
        { cx: 258, valor: 0.86, cor: 'var(--color-state-critical)', rotulo: 'churn' },
      ].map((mostrador) => {
        const raio = 34;
        const circunferencia = 2 * Math.PI * raio;
        return (
          <g key={mostrador.rotulo}>
            <circle cx={mostrador.cx} cy="82" r={raio} stroke="currentColor" strokeWidth="6" opacity=".14" />
            <circle
              className="pilar-arco"
              cx={mostrador.cx}
              cy="82"
              r={raio}
              stroke={mostrador.cor}
              strokeWidth="6"
              strokeLinecap="round"
              strokeDasharray={`${circunferencia * mostrador.valor} ${circunferencia}`}
              transform={`rotate(-90 ${mostrador.cx} 82)`}
            />
            <text x={mostrador.cx} y="88" textAnchor="middle" className="pilar-valor" fill="currentColor">
              {Math.round(mostrador.valor * 100)}
            </text>
            <text x={mostrador.cx} y="138" textAnchor="middle" className="pilar-legenda" fill="currentColor">
              {mostrador.rotulo}
            </text>
          </g>
        );
      })}
      <path d="M20 168 H300" stroke="currentColor" strokeWidth="1" opacity=".2" />
      <text x="20" y="192" className="pilar-legenda" fill="currentColor">
        score de qualidade · score comercial · prioridade
      </text>
    </svg>
  );
}

function DesenhoAcompanhamento() {
  return (
    <svg viewBox="0 0 320 220" fill="none" className="pilar-svg" aria-hidden="true">
      {/* Ciclo de vida do plano de ação — o enum StatusPlanoAcao, desenhado */}
      {[
        { x: 8, rotulo: 'PENDENTE', cor: 'var(--color-state-warn)' },
        { x: 116, rotulo: 'EM ANDAM.', cor: 'var(--color-data-2)' },
        { x: 224, rotulo: 'CONCLUÍDO', cor: 'var(--color-state-good)' },
      ].map((estado) => (
        <g key={estado.rotulo}>
          <rect x={estado.x} y="48" width="88" height="40" rx="5" stroke={estado.cor} strokeWidth="1.4" fill="none" />
          <text x={estado.x + 44} y="72" textAnchor="middle" className="pilar-legenda" fill="currentColor">
            {estado.rotulo}
          </text>
        </g>
      ))}

      <g stroke="currentColor" strokeWidth="1.2" markerEnd="url(#seta)">
        <path d="M100 68 H112" />
        <path d="M208 68 H220" />
        <path d="M52 92 V128 H160" />
        <path d="M160 92 V128" />
      </g>

      <rect x="116" y="132" width="88" height="38" rx="5" stroke="var(--color-state-idle)" strokeWidth="1.4" strokeDasharray="4 4" fill="none" />
      <text x="160" y="155" textAnchor="middle" className="pilar-legenda" fill="currentColor">
        CANCELADO
      </text>

      <defs>
        <marker id="seta" viewBox="0 0 8 8" refX="7" refY="4" markerWidth="6" markerHeight="6" orient="auto">
          <path d="M0 0 L8 4 L0 8 z" fill="currentColor" />
        </marker>
      </defs>

      <text x="8" y="200" className="pilar-legenda" fill="currentColor" opacity=".62">
        a API devolve as transições permitidas — o front só habilita o que é válido
      </text>
    </svg>
  );
}

function DesenhoRelatorio() {
  return (
    <svg viewBox="0 0 320 220" fill="none" className="pilar-svg" aria-hidden="true">
      {/* Três folhas empilhadas com profundidade */}
      {[
        { x: 40, y: 40, o: 0.2 },
        { x: 56, y: 28, o: 0.4 },
        { x: 72, y: 16, o: 1 },
      ].map((folha, i) => (
        <g key={i} opacity={folha.o}>
          <rect x={folha.x} y={folha.y} width="128" height="164" rx="4" fill="var(--surface-1)" stroke="currentColor" strokeWidth="1" />
          {i === 2 && (
            <>
              <rect x={folha.x + 14} y={folha.y + 18} width="62" height="7" rx="1.5" fill="var(--accent)" />
              {[0, 1, 2, 3, 4, 5].map((l) => (
                <rect
                  key={l}
                  x={folha.x + 14}
                  y={folha.y + 38 + l * 12}
                  width={l % 3 === 2 ? 58 : 100}
                  height="4"
                  rx="1"
                  fill="currentColor"
                  opacity=".3"
                />
              ))}
              {/* Mini gráfico de barras dentro do relatório */}
              {[22, 40, 30, 52, 36].map((h, b) => (
                <rect
                  key={b}
                  x={folha.x + 14 + b * 20}
                  y={folha.y + 140 - h}
                  width="12"
                  height={h}
                  rx="2"
                  fill="var(--accent)"
                  opacity={0.35 + b * 0.13}
                />
              ))}
            </>
          )}
        </g>
      ))}
      <g stroke="currentColor" strokeWidth="1" opacity=".35">
        <path d="M212 96 H286" markerEnd="url(#seta2)" />
      </g>
      <text x="212" y="86" className="pilar-legenda" fill="currentColor">
        PDF
      </text>
      <text x="212" y="120" className="pilar-legenda" fill="currentColor" opacity=".6">
        executivo · reunião · lote
      </text>
      <defs>
        <marker id="seta2" viewBox="0 0 8 8" refX="7" refY="4" markerWidth="6" markerHeight="6" orient="auto">
          <path d="M0 0 L8 4 L0 8 z" fill="currentColor" />
        </marker>
      </defs>
    </svg>
  );
}

const PILARES: Pilar[] = [
  {
    ref: 'I360 — 01',
    titulo: 'Importação',
    resumo:
      'Um CSV de até 50 MB entra e sai classificado. A duplicidade é detectada dentro do lote e contra tudo o que já foi importado antes.',
    itens: [
      'Leitura por página, sem carregar tudo em memória',
      'Transcrições de 188 mil caracteres em CLOB',
      'Lote rastreável: brutos, válidos, incompletos, duplicados, erros',
    ],
    desenho: <DesenhoImportacao />,
  },
  {
    ref: 'I360 — 02',
    titulo: 'Análise',
    resumo:
      'Cada transcrição recebe pontuação de completude, sentimento com justificativa, risco de churn e dois scores — qualidade e comercial.',
    itens: [
      'Seis sentimentos, do positivo ao crítico',
      'Risco de churn em três faixas, com prioridade derivada',
      'Insights com trecho de origem e grau de confiança',
    ],
    desenho: <DesenhoAnalise />,
  },
  {
    ref: 'I360 — 03',
    titulo: 'Acompanhamento',
    resumo:
      'O insight vira plano de ação com responsável e prazo. O ciclo de vida é do próprio domínio: o enum conhece as próprias transições.',
    itens: [
      'Responsável ativo, prazo não retroativo',
      'Conclusão e cancelamento exigem resultado',
      'Comentários por reunião, com autoria preservada',
    ],
    desenho: <DesenhoAcompanhamento />,
  },
  {
    ref: 'I360 — 04',
    titulo: 'Relatório',
    resumo:
      'O que a diretoria lê. PDF executivo consolidado, por reunião ou por lote — gerado a partir dos mesmos dados que alimentam o painel.',
    itens: [
      'Executivo consolidado da carteira',
      'Individual por reunião analisada',
      'Fechamento por lote de importação',
    ],
    desenho: <DesenhoRelatorio />,
  },
];

export function Pilares() {
  const raiz = useAnimacao<HTMLElement>(({ escopo, reduzido }) => {
    if (reduzido) return;

    const trilha = escopo.querySelector<HTMLElement>('.pilares-trilha');
    if (!trilha) return;

    const distancia = () => trilha.scrollWidth - window.innerWidth;

    const travessia = gsap.to(trilha, {
      x: () => -distancia(),
      ease: 'none',
      scrollTrigger: {
        trigger: escopo,
        start: 'top top',
        end: () => `+=${distancia()}`,
        pin: true,
        scrub: 0.85,
        anticipatePin: 1,
        invalidateOnRefresh: true,
      },
    });

    /* Contra-movimento interno: o texto anda um pouco contra a travessia e o
       desenho um pouco a favor. É o que dá volume ao deslocamento lateral —
       mas com amplitude curta, senão o texto sai pela borda do painel antes
       de o painel sair da tela. */
    const trilhos = (pilar: HTMLElement) => ({
      trigger: pilar,
      containerAnimation: travessia,
      start: 'left right',
      end: 'right left',
      scrub: true,
    });

    gsap.utils.toArray<HTMLElement>('.pilar').forEach((pilar) => {
      const texto = pilar.querySelector('.pilar-texto');
      const figura = pilar.querySelector('.pilar-figura');
      const numero = pilar.querySelector('.pilar-numero');

      if (texto) {
        gsap.fromTo(texto, { x: 42 }, { x: -30, ease: 'none', scrollTrigger: trilhos(pilar) });
      }

      // O painel de abertura não tem desenho nem numeral: só o texto anda.
      if (figura) {
        gsap.fromTo(
          figura,
          { x: -46, scale: 0.95 },
          { x: 46, scale: 1.03, ease: 'none', scrollTrigger: trilhos(pilar) },
        );
      }

      if (numero) {
        gsap.fromTo(
          numero,
          { opacity: 0.05, x: 110 },
          { opacity: 0.14, x: -110, ease: 'none', scrollTrigger: trilhos(pilar) },
        );
      }
    });
  }, []);

  return (
    <section ref={raiz} id="pilares" className="pilares" aria-labelledby="pilares-titulo">
      <h2 id="pilares-titulo" className="sr-only">
        A plataforma em quatro etapas
      </h2>

      <div className="pilares-trilha">
        {/* Painel de abertura da travessia */}
        <article className="pilar pilar--abertura">
          <div className="pilar-texto">
            <span className="ref">REF: I360 — PLATAFORMA</span>
            <h3 className="display pilar-abertura-titulo">
              Quatro etapas
              <br />
              entre o áudio
              <br />e a <em>decisão</em>.
            </h3>
            <p className="lead">
              Role para a direita. Cada etapa é uma camada do sistema — e todas existem na API
              pública, em <span className="numeric">/api/v1</span>.
            </p>
            <span className="pilar-dica ref">→ arraste ou continue rolando</span>
          </div>
        </article>

        {PILARES.map((pilar, indice) => (
          <article key={pilar.ref} className="pilar">
            <span className="pilar-numero numeric" aria-hidden="true">
              {String(indice + 1).padStart(2, '0')}
            </span>

            <div className="pilar-texto">
              <span className="ref">REF: {pilar.ref}</span>
              <h3 className="display pilar-titulo">{pilar.titulo}</h3>
              <p className="lead pilar-resumo">{pilar.resumo}</p>
              <ul className="pilar-itens">
                {pilar.itens.map((item) => (
                  <li key={item}>
                    <span className="pilar-marcador" aria-hidden="true" />
                    {item}
                  </li>
                ))}
              </ul>
            </div>

            <figure className="pilar-figura">{pilar.desenho}</figure>
          </article>
        ))}
      </div>
    </section>
  );
}
