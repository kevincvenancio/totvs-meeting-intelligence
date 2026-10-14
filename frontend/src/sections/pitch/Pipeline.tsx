/* ============================================================================
   Bloco 3 · O que construímos: o pipeline (1:15–2:30)

   O bloco que mais pesa para a banca. As quatro etapas ficam todas na tela
   desde o início, apagadas, e cada uma acende quando é citada. O último passo
   mostra a stack e o volume real já processado.

   O texto de cada etapa descreve o que o código faz hoje
   (ImportacaoArquivoService, AnaliseTranscricaoService, ChurnService,
   SentimentoService, FeedbackReuniaoService, DashboardService). Se o motor
   mudar, mude aqui.
   ========================================================================== */

import { revelar, usePasso } from '../../components/chrome/passos';
import { CabecalhoBloco } from './comum';

interface Etapa {
  nome: string;
  frase: string;
  detalhe: string;
  saidas: string[];
  selo?: string;
  exemplo?: React.ReactNode;
}

const ETAPAS: Etapa[] = [
  {
    nome: 'Ingestão',
    frase: 'A transcrição entra e é padronizada.',
    detalhe:
      'Arquivos de até 50 MB, lidos por página. Cada linha é validada, e o lote separa reuniões válidas, incompletas e duplicadas — inclusive contra o que já foi importado.',
    saidas: ['lote rastreável', 'texto tratado'],
  },
  {
    nome: 'Extração',
    frase: 'O texto vira sinais estruturados.',
    detalhe:
      'Dicionários do domínio TOTVS reconhecem produtos, concorrentes, dores, áreas e orçamento citado. Cada sinal guarda o trecho da conversa que o comprova.',
    saidas: ['produtos', 'dores', 'concorrentes', 'oportunidades'],
    exemplo: (
      <p className="etapa-exemplo">
        <span>
          “…do jeito que está, <mark>não vamos renovar</mark>.”
        </span>
        <span className="etapa-exemplo-sinal">churn · alto</span>
      </p>
    ),
  },
  {
    nome: 'Classificação',
    frase: 'Cada reunião recebe notas.',
    detalhe:
      'Completude de 0 a 100 — cliente identificado, os dois lados falando, dor e próximo passo. Sentimento em seis classes, sempre com justificativa. Risco de churn em três faixas, score comercial e prioridade.',
    saidas: ['completude', 'sentimento', 'risco de churn', 'score'],
    selo: 'regras auditáveis · custo zero por análise',
  },
  {
    nome: 'Agregação',
    frase: 'Os sinais sobem para a conta e a carteira.',
    detalhe:
      'Indicadores por cliente e pela carteira inteira, feedback educativo para o vendedor, plano de ação com responsável e prazo, e relatório executivo em PDF.',
    saidas: ['painel', 'plano de ação', 'relatório'],
  },
];

const STACK = ['Java 17 · Spring Boot', 'Oracle 19c', 'React 19 · Vite', 'Vercel + Render'];

const PROVA = [
  { valor: '1.126', rotulo: 'reuniões reais analisadas' },
  { valor: '2.819', rotulo: 'sinais extraídos, cada um com trecho' },
  { valor: '22', rotulo: 'contas na carteira de demonstração' },
];

export function Pipeline() {
  const passo = usePasso('pipeline');

  return (
    <section id="pipeline" className="pitch-bloco pipeline blueprint"aria-labelledby="pipeline-titulo">
      <div className="pitch-interior">
        <CabecalhoBloco
          id="pipeline"
          titulo={
            <>
              Da transcrição à ação, em <em>quatro etapas</em>.
            </>
          }
        />

        <ol className="pipeline-etapas">
          {ETAPAS.map((etapa, i) => (
            <li
              key={etapa.nome}
              className="pipeline-etapa"
              data-acesa={revelar(passo, i + 1)}
              data-atual={passo === i + 1 || undefined}
            >
              <div className="etapa-topo">
                <span className="etapa-numero numeric">{String(i + 1).padStart(2, '0')}</span>
                <h3 className="etapa-nome">{etapa.nome}</h3>
              </div>
              <p className="etapa-frase">{etapa.frase}</p>
              <p className="etapa-detalhe">{etapa.detalhe}</p>
              {etapa.exemplo}
              <ul className="etapa-saidas">
                {etapa.saidas.map((saida) => (
                  <li key={saida}>{saida}</li>
                ))}
              </ul>
              {etapa.selo && <span className="etapa-selo">{etapa.selo}</span>}
              {i < ETAPAS.length - 1 && <span className="etapa-seta" aria-hidden="true" />}
            </li>
          ))}
        </ol>

        <div className="pipeline-base revela" data-visivel={revelar(passo, 5)}>
          <div className="pipeline-api">
            <span className="eyebrow">Tudo exposto numa API REST</span>
            <span className="numeric pipeline-rota">/api/v1</span>
            <span className="pipeline-api-apoio">31 recursos · 44 operações · OpenAPI</span>
          </div>

          <ul className="pipeline-prova">
            {PROVA.map((item) => (
              <li key={item.rotulo}>
                <span className="numeric pipeline-prova-valor">{item.valor}</span>
                <span className="pipeline-prova-rotulo">{item.rotulo}</span>
              </li>
            ))}
          </ul>

          <ul className="pipeline-stack">
            {STACK.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}
