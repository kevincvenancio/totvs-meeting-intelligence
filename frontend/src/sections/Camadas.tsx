/* ============================================================================
   Baralho empilhado.

   Quatro cartões que se sobrepõem: cada um prende na tela, encolhe e escurece
   enquanto o próximo desliza por cima. O que fica visível da pilha embaixo é
   proposital — a leitura anterior continua ali, é sobre ela que a próxima
   camada é construída.

   As quatro camadas são as quatro leituras que o motor faz da mesma conversa.
   ========================================================================== */

import { gsap, useAnimacao } from '../lib/motion';

interface Camada {
  ref: string;
  chapeu: string;
  titulo: string;
  texto: string;
  campo: string;
  cor: string;
  exemplo: React.ReactNode;
}

const CAMADAS: Camada[] = [
  {
    ref: '01',
    chapeu: 'Camada literal',
    titulo: 'O que foi dito',
    texto:
      'A transcrição vira tema, resumo, pontos principais, dores, oportunidades, produtos citados e concorrentes mencionados. Nada de interpretação ainda — só o que está escrito, organizado.',
    campo: 'resumoReuniao · pontosPrincipais · doresIdentificadas',
    cor: 'var(--color-data-2)',
    exemplo: (
      <p className="camada-citacao">
        “Desde a virada de versão o time fiscal vira dois fins de semana, e a diretoria começou a
        perguntar quanto isso custa.”
      </p>
    ),
  },
  {
    ref: '02',
    chapeu: 'Camada afetiva',
    titulo: 'O que foi sentido',
    texto:
      'Seis sentimentos possíveis, e nenhum deles vem sozinho: o campo de justificativa obriga o sistema a apontar onde, na conversa, aquele sentimento aparece. Um rótulo sem prova não serve para decidir nada.',
    campo: 'sentimento · sentimentoJustificativa',
    cor: 'var(--color-data-5)',
    exemplo: (
      <div className="camada-chips">
        {['Positivo', 'Negativo', 'Crítico', 'Misto', 'Oportunidade', 'Neutro'].map((s, i) => (
          <span key={s} data-ativo={i === 3 || undefined}>
            {s}
          </span>
        ))}
      </div>
    ),
  },
  {
    ref: '03',
    chapeu: 'Camada preditiva',
    titulo: 'O que vai acontecer',
    texto:
      'Risco de churn em três faixas, prioridade derivada e score comercial. É o campo que muda a ordem da fila do time: quem liga para quem, amanhã de manhã.',
    campo: 'riscoChurn · prioridade · scoreComercial',
    cor: 'var(--color-state-critical)',
    exemplo: (
      <div className="camada-medidores">
        {[
          { r: 'Churn', v: 86, c: 'var(--color-state-critical)' },
          { r: 'Comercial', v: 61, c: 'var(--color-data-2)' },
          { r: 'Qualidade', v: 44, c: 'var(--color-state-warn)' },
        ].map((m) => (
          <div key={m.r} className="camada-medidor">
            <span className="camada-medidor-rotulo">{m.r}</span>
            <span className="camada-medidor-trilho">
              <span style={{ width: `${m.v}%`, background: m.c }} />
            </span>
            <span className="numeric camada-medidor-valor">{m.v}</span>
          </div>
        ))}
      </div>
    ),
  },
  {
    ref: '04',
    chapeu: 'Camada educativa',
    titulo: 'O que aprender',
    texto:
      'A camada que quase nenhum sistema entrega: por que o sinal não foi percebido na hora, quais eram as pistas, e que perguntas teriam aberto a conversa. O vendedor sai da análise sabendo mais do que entrou.',
    campo: 'feedbackReuniao · perguntasRecomendadas',
    cor: 'var(--color-data-3)',
    exemplo: (
      <ul className="camada-perguntas">
        <li>“O que precisaria acontecer para essa conversa sair da mesa da diretoria?”</li>
        <li>“Quando você diz que pediram proposta, isso já é um processo formal?”</li>
        <li>“Se o fechamento voltasse para 3 dias, o que mudaria na sua rotina?”</li>
      </ul>
    ),
  },
];

export function Camadas() {
  /* O empilhamento em si é `position: sticky` no CSS — quatro elementos
     grudando em alturas escalonadas. Sticky é muito mais estável que prender
     quatro cartões sobrepostos com ScrollTrigger, e sobrevive a qualquer
     mudança de altura da página. Ao GSAP sobra só o recuo: encolher e
     escurecer o cartão de baixo no exato trecho em que o de cima o cobre. */
  const raiz = useAnimacao<HTMLElement>(({ reduzido }) => {
    if (reduzido) return;

    const cartoes = gsap.utils.toArray<HTMLElement>('.camada-cartao');

    cartoes.forEach((cartao, indice) => {
      const proximo = cartoes[indice + 1];
      if (!proximo) return;

      gsap.fromTo(
        cartao,
        { scale: 1, filter: 'brightness(1)' },
        {
          scale: 0.94 - indice * 0.014,
          filter: 'brightness(0.55)',
          ease: 'none',
          scrollTrigger: {
            // O recuo acompanha a chegada do próximo, não a entrada dele na
            // viewport — começar cedo demais apagava o cartão ainda em leitura.
            trigger: proximo,
            start: 'top 78%',
            end: 'top 22%',
            scrub: 0.5,
            invalidateOnRefresh: true,
          },
        },
      );
    });
  }, []);

  return (
    <section ref={raiz} id="camadas" className="camadas" aria-labelledby="camadas-titulo">
      <header className="camadas-cabecalho">
        <span className="ref">REF: I360 — 02</span>
        <h2 id="camadas-titulo" className="display camadas-titulo">
          Uma conversa,
          <br />
          quatro <em>leituras</em>.
        </h2>
        <p className="lead">
          O motor não classifica a reunião uma vez. Ele a atravessa quatro vezes, e cada passagem
          responde a uma pergunta diferente sobre a mesma transcrição.
        </p>
      </header>

      <div className="camadas-pilha">
        {CAMADAS.map((camada, indice) => (
          <article
            key={camada.ref}
            className="camada-cartao"
            style={
              {
                '--cor-camada': camada.cor,
                // Degrau do sticky e ordem de empilhamento vêm do índice.
                '--indice': indice,
                zIndex: indice + 1,
              } as React.CSSProperties
            }
          >
            <div className="camada-conteudo">
              <div className="camada-coluna-texto">
                <div className="camada-topo">
                  <span className="camada-indice numeric">{camada.ref}</span>
                  <span className="eyebrow">{camada.chapeu}</span>
                </div>

                <h3 className="display camada-titulo">{camada.titulo}</h3>
                <p className="camada-texto">{camada.texto}</p>
                <code className="camada-campo">{camada.campo}</code>
              </div>

              <div className="camada-coluna-exemplo">{camada.exemplo}</div>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
