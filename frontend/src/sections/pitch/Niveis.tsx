/* ============================================================================
   Bloco 4 · Três níveis e valor por área (2:30–3:20)

   Reunião → Conta → Carteira, ligados por setas. Cada cartão tem um desenho
   mínimo do que aquele nível mostra no painel. Depois, a frase do
   diferencial e a faixa das áreas que usam.
   ========================================================================== */

import { revelar, usePasso } from '../../components/chrome/passos';
import { CabecalhoBloco } from './comum';

function DesenhoReuniao() {
  return (
    <div className="nivel-medidores" aria-hidden="true">
      {[
        { r: 'Completude', v: 78, c: 'var(--color-state-good)' },
        { r: 'Sentimento', v: 42, c: 'var(--color-data-5)' },
        { r: 'Churn', v: 86, c: 'var(--color-state-critical)' },
      ].map((m) => (
        <div key={m.r} className="nivel-medidor">
          <span>{m.r}</span>
          <span className="nivel-medidor-trilho">
            <span style={{ width: `${m.v}%`, background: m.c }} />
          </span>
        </div>
      ))}
    </div>
  );
}

/* Uma conta ao longo do tempo: cada ponto é uma reunião, a cor é o sentimento. */
function DesenhoConta() {
  const pontos = [
    { y: 30, c: 'var(--color-state-good)' },
    { y: 26, c: 'var(--color-state-good)' },
    { y: 38, c: 'var(--color-data-5)' },
    { y: 52, c: 'var(--color-data-2)' },
    { y: 70, c: 'var(--color-state-critical)' },
  ];
  return (
    <svg viewBox="0 0 220 90" className="nivel-svg" aria-hidden="true">
      <polyline
        points={pontos.map((p, i) => `${20 + i * 45},${p.y}`).join(' ')}
        fill="none"
        stroke="currentColor"
        strokeOpacity=".35"
        strokeWidth="1.5"
      />
      {pontos.map((p, i) => (
        <circle key={i} cx={20 + i * 45} cy={p.y} r="6" fill={p.c} />
      ))}
    </svg>
  );
}

function DesenhoCarteira() {
  return (
    <div className="nivel-ranking" aria-hidden="true">
      {[92, 74, 61, 48, 33].map((v, i) => (
        <span key={i} style={{ width: `${v}%`, opacity: 1 - i * 0.14 }} />
      ))}
    </div>
  );
}

const NIVEIS = [
  {
    nome: 'Reunião',
    pergunta: 'O que aconteceu naquela conversa?',
    itens: ['Completude, sentimento e risco de churn', 'Sinais com o trecho de origem', 'Feedback e perguntas para o vendedor'],
    desenho: <DesenhoReuniao />,
  },
  {
    nome: 'Conta',
    pergunta: 'A relação está melhorando ou piorando?',
    itens: ['Todas as reuniões do cliente, em ordem', 'Status da conta: ativa, em risco…', 'Planos de ação com responsável e prazo'],
    desenho: <DesenhoConta />,
  },
  {
    nome: 'Carteira',
    pergunta: 'Onde estão os riscos e as oportunidades?',
    itens: ['Distribuição de risco e sentimento', 'Produtos e concorrentes mais citados', 'Fila de risco e de oportunidade'],
    desenho: <DesenhoCarteira />,
  },
];

const AREAS = [
  { nome: 'Comercial', uso: 'próximo passo e oportunidades' },
  { nome: 'Customer Success', uso: 'fila de risco antes da rescisão' },
  { nome: 'Produto', uso: 'dores que se repetem por módulo' },
  { nome: 'Marketing', uso: 'concorrentes e argumentos' },
  { nome: 'Liderança', uso: 'a carteira em uma tela' },
];

export function Niveis() {
  const passo = usePasso('niveis');

  return (
    <section id="niveis" className="pitch-bloco niveis" aria-labelledby="niveis-titulo">
      <div className="pitch-interior">
        <CabecalhoBloco
          id="niveis"
          titulo={
            <>
              Três níveis de leitura, <em>ligados</em>.
            </>
          }
        />

        <ol className="niveis-grade">
          {NIVEIS.map((nivel, i) => (
            <li key={nivel.nome} className="nivel revela" data-visivel={revelar(passo, i + 1)}>
              <div className="nivel-topo">
                <span className="numeric nivel-numero">{String(i + 1).padStart(2, '0')}</span>
                <h3 className="display nivel-nome">{nivel.nome}</h3>
              </div>
              <p className="nivel-pergunta">{nivel.pergunta}</p>
              <div className="nivel-desenho">{nivel.desenho}</div>
              <ul className="nivel-itens">
                {nivel.itens.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
              {i < NIVEIS.length - 1 && <span className="nivel-seta" aria-hidden="true" />}
            </li>
          ))}
        </ol>

        <p className="niveis-diferencial revela" data-visivel={revelar(passo, 4)}>
          Um sinal isolado parece pequeno. Quando dezenas de reuniões apontam para o mesmo problema,
          deixa de ser uma conversa e <em>vira decisão de negócio</em>.
        </p>

        <ul className="niveis-areas revela" data-visivel={revelar(passo, 5)}>
          {AREAS.map((area) => (
            <li key={area.nome}>
              <span className="niveis-area-nome">{area.nome}</span>
              <span className="niveis-area-uso">{area.uso}</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
