/* ============================================================================
   Hermes Chat — botão flutuante + painel de conversa com dois agentes.

   - Reuniões (azul): consulta o banco — reuniões, clientes, planos de ação.
   - Suporte técnico (jade): explica como usar as telas; não lê dados.

   Só consulta. A conversa vive em memória (estado do componente), uma por
   agente; nada é salvo no navegador.
   ========================================================================== */

import { useEffect, useId, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ApiError, assinarStatusApi, enviarMensagemChat } from '../../lib/api';
import type { AgenteChat, ChatMensagem, FonteChat, MetaChat } from '../../lib/types';
import { Markdown } from './Markdown';

const MAX_CARACTERES = 600;
const MAX_HISTORICO = 6;

interface Bolha extends ChatMensagem {
  id: number;
  fontes?: FonteChat[];
  meta?: MetaChat;
  /** Mensagem de erro local: aparece na tela, mas não vai no histórico. */
  erro?: boolean;
}

interface ConfigAgente {
  nome: string;
  resumo: string;
  status: string;
  saudacao: string;
  placeholder: (reuniaoId: number | null) => string;
  sugestoes: (reuniaoId: number | null) => string[];
  marcaSugestao: string;
  rodape: string;
}

const AGENTES: Record<AgenteChat, ConfigAgente> = {
  REUNIOES: {
    nome: 'Reuniões',
    resumo: 'dados · clientes · planos',
    status: 'consulta o banco · só leitura',
    saudacao:
      'Oi, sou o Hermes. Pergunte sobre **reuniões**, **clientes** ou **planos de ação** — respondo com os dados reais do sistema.',
    placeholder: (id) => (id ? `Pergunte sobre a reunião #${id}…` : 'Pergunte sobre reuniões, clientes ou planos…'),
    sugestoes: (id) => [
      ...(id ? ['Resuma a reunião atual'] : []),
      'Quais clientes têm risco de churn alto?',
      'Quais planos de ação estão atrasados?',
      'Quais reuniões têm as maiores oportunidades?',
    ],
    marcaSugestao: '→',
    rodape: 'só leitura · responde apenas com dados do sistema',
  },
  SUPORTE: {
    nome: 'Suporte técnico',
    resumo: 'telas · importação · erros',
    status: 'modo suporte · sem dados de clientes',
    saudacao:
      'Modo **suporte técnico**. Tire dúvidas sobre as telas, a importação de arquivos, os relatórios em PDF e as mensagens de erro.',
    placeholder: () => 'Qual a sua dúvida sobre o sistema?',
    sugestoes: () => [
      'Como importo um CSV?',
      'Quais campos o CSV precisa ter?',
      'Onde baixo o relatório em PDF?',
      'Como mudo o status de um plano?',
    ],
    marcaSugestao: '?',
    rodape: 'dúvidas de uso · não altera dados',
  },
};

let proximoId = 1;

function reuniaoDaRota(pathname: string): number | null {
  const m = /^\/reunioes\/(\d+)\/?$/.exec(pathname);
  return m ? Number(m[1]) : null;
}

function mensagemDeErro(erro: unknown): string {
  if (erro instanceof ApiError) {
    if (erro.status === 429) return 'Muitas perguntas seguidas. Aguarde um minuto e tente de novo.';
    if (erro.status === 503)
      return erro.corpo.mensagem || 'O Hermes está indisponível no momento. Tente novamente em instantes.';
    if (erro.status === 0 || erro.status === 502 || erro.status === 504)
      return 'A API não respondeu. Verifique se o back-end está no ar.';
    if (erro.corpo.campos?.length) return erro.corpo.campos[0].mensagem;
    if (erro.corpo.mensagem) return erro.corpo.mensagem;
  }
  return 'Não consegui responder agora. Tente novamente.';
}

function rotuloFonte(f: FonteChat) {
  if (f.tipo === 'REUNIAO') return { texto: `Reunião #${f.id}`, para: `/reunioes/${f.id}` };
  if (f.tipo === 'PLANO_ACAO') return { texto: `Plano #${f.id}`, para: '/planos-acao' };
  return { texto: `Cliente #${f.id}`, para: '/clientes' };
}

function linhaMeta(meta?: MetaChat) {
  if (!meta) return null;
  const partes: string[] = [];
  if (!meta.chamouModelo) {
    partes.push('resposta direta · sem consumir cota');
  } else if (meta.agente === 'SUPORTE') {
    partes.push('guia do sistema');
  } else {
    const r = meta.reunioesConsultadas;
    const p = meta.planosConsultados;
    partes.push(r || p ? `${r} reuniões · ${p} planos consultados` : 'indicadores gerais consultados');
  }
  partes.push(`${(meta.duracaoMs / 1000).toFixed(1).replace('.', ',')} s`);
  return partes.join(' · ');
}

function formatarTokens(t: number) {
  return t >= 1000 ? `${(t / 1000).toFixed(1).replace('.', ',')}k` : String(t);
}

/* Ícones derivados da marca do Hermes: o anel aberto (por onde o sinal entra)
   com o anel interno. Reuniões = núcleo aceso (o insight dos dados);
   Suporte = "?" no lugar do núcleo. O arco usa a cor do agente. */
function IconeAgente({ agente, tamanho = 18 }: { agente: AgenteChat; tamanho?: number }) {
  return (
    <svg
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      className="hc-icone"
    >
      <path
        className="hc-icone-arco"
        d="M12 3.5a8.5 8.5 0 1 1-6.01 2.49"
        stroke="var(--hc-icone, currentColor)"
        strokeWidth="2.2"
        strokeLinecap="round"
      />
      {agente === 'REUNIOES' ? (
        <>
          <circle cx="12" cy="12" r="4.4" stroke="currentColor" strokeWidth="1.2" opacity=".45" />
          <circle cx="12" cy="12" r="1.9" fill="var(--hc-icone, currentColor)" />
        </>
      ) : (
        <>
          <path
            d="M10.1 10.2a1.95 1.95 0 1 1 2.75 1.78c-.52.24-.85.63-.85 1.2v.32"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinecap="round"
          />
          <circle cx="12" cy="15.6" r=".95" fill="var(--hc-icone, currentColor)" />
        </>
      )}
    </svg>
  );
}

export function HermesChat() {
  const { pathname } = useLocation();
  const reuniaoId = reuniaoDaRota(pathname);

  const [aberto, setAberto] = useState(false);
  const [agente, setAgente] = useState<AgenteChat>('REUNIOES');
  const [conversas, setConversas] = useState<Record<AgenteChat, Bolha[]>>({ REUNIOES: [], SUPORTE: [] });
  const [texto, setTexto] = useState('');
  const [carregando, setCarregando] = useState(false);
  const [apiViva, setApiViva] = useState<boolean | null>(null);

  const listaRef = useRef<HTMLDivElement>(null);
  const campoRef = useRef<HTMLTextAreaElement>(null);
  const botaoRef = useRef<HTMLButtonElement>(null);
  const idPainel = useId();
  const idTitulo = useId();

  const config = AGENTES[agente];
  const bolhas = conversas[agente];
  const ultimaMeta = [...bolhas].reverse().find((b) => b.meta?.chamouModelo)?.meta;

  useEffect(() => assinarStatusApi(setApiViva), []);
  const vitrine = apiViva === false;

  useEffect(() => {
    const lista = listaRef.current;
    if (lista) lista.scrollTop = lista.scrollHeight;
  }, [bolhas, carregando, aberto, agente]);

  useEffect(() => {
    if (aberto) campoRef.current?.focus();
  }, [aberto, agente]);

  function fechar() {
    setAberto(false);
    botaoRef.current?.focus();
  }

  function adicionar(alvo: AgenteChat, bolha: Bolha) {
    setConversas((atual) => ({ ...atual, [alvo]: [...atual[alvo], bolha] }));
  }

  async function enviar(pergunta: string) {
    const mensagem = pergunta.trim();
    if (!mensagem || carregando || vitrine) return;

    const alvo = agente; // a resposta volta para o agente que recebeu a pergunta
    const historico: ChatMensagem[] = conversas[alvo]
      .filter((b) => !b.erro)
      .slice(-MAX_HISTORICO)
      .map(({ papel, texto }) => ({ papel, texto }));

    adicionar(alvo, { id: proximoId++, papel: 'user', texto: mensagem });
    setTexto('');
    setCarregando(true);

    try {
      const r = await enviarMensagemChat({
        mensagem,
        historico,
        agente: alvo,
        reuniaoId: alvo === 'REUNIOES' ? reuniaoId : null,
        tela: /^\/[a-z0-9/-]*$/.test(pathname) ? pathname : undefined,
      });
      adicionar(alvo, { id: proximoId++, papel: 'assistant', texto: r.resposta, fontes: r.fontes, meta: r.meta });
    } catch (erro) {
      adicionar(alvo, { id: proximoId++, papel: 'assistant', texto: mensagemDeErro(erro), erro: true });
    } finally {
      setCarregando(false);
      campoRef.current?.focus();
    }
  }

  function aoEnviar(e: FormEvent) {
    e.preventDefault();
    void enviar(texto);
  }

  function aoTeclar(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      void enviar(texto);
    }
  }

  return (
    <>
      <button
        ref={botaoRef}
        type="button"
        className={`hermes-chat-gatilho ${aberto ? 'hermes-chat-gatilho--aberto' : ''}`}
        aria-expanded={aberto}
        aria-controls={idPainel}
        aria-label={aberto ? 'Fechar o chat do Hermes' : 'Abrir o chat do Hermes'}
        onClick={() => (aberto ? fechar() : setAberto(true))}
      >
        <span className="hermes-chat-gatilho-marca" aria-hidden="true" />
        <span>{aberto ? 'Fechar' : 'Pergunte ao Hermes'}</span>
      </button>

      {aberto && (
        <section
          id={idPainel}
          className={`hermes-chat hermes-chat--${agente.toLowerCase()}`}
          role="dialog"
          aria-modal="false"
          aria-labelledby={idTitulo}
          onKeyDown={(e) => e.key === 'Escape' && fechar()}
        >
          <header className="hc-cab">
            <div className="hc-cab-linha">
              <div className="hc-tit">
                <span className="hc-avatar" aria-hidden="true">
                  <IconeAgente agente={agente} tamanho={24} />
                </span>
                <div>
                  <h2 id={idTitulo}>Hermes</h2>
                  <p className="hc-status">
                    <span className="hc-vivo" aria-hidden="true" />
                    {config.status}
                  </p>
                </div>
              </div>
              <div className="hc-acoes">
                {bolhas.length > 0 && (
                  <button
                    type="button"
                    className="hc-limpar"
                    onClick={() => setConversas((atual) => ({ ...atual, [agente]: [] }))}
                    disabled={carregando}
                  >
                    Nova conversa
                  </button>
                )}
                <button type="button" className="hc-fechar" aria-label="Fechar o chat" onClick={fechar}>
                  ×
                </button>
              </div>
            </div>

            <div className="hc-agentes" role="tablist" aria-label="Escolha o agente">
              {(Object.keys(AGENTES) as AgenteChat[]).map((a) => (
                <button
                  key={a}
                  type="button"
                  role="tab"
                  aria-selected={agente === a}
                  className={`hc-ag hc-ag--${a.toLowerCase()} ${agente === a ? 'hc-ag--on' : ''}`}
                  onClick={() => setAgente(a)}
                  disabled={carregando}
                >
                  <span className="hc-ag-icone">
                    <IconeAgente agente={a} tamanho={16} />
                  </span>
                  <span>
                    <b>{AGENTES[a].nome}</b>
                    <small>{AGENTES[a].resumo}</small>
                  </span>
                </button>
              ))}
            </div>
          </header>

          <div className="hc-ctx" aria-label="Contexto">
            {agente === 'REUNIOES' ? (
              <span className="hc-pill hc-pill--acc">◉ {reuniaoId ? `Reunião #${reuniaoId}` : 'Base completa'}</span>
            ) : (
              <span className="hc-pill hc-pill--acc">◉ Tela: {pathname}</span>
            )}
            <span className="hc-pill">{agente === 'REUNIOES' ? 'Dados de agora' : 'Guia do sistema'}</span>
            {ultimaMeta && <span className="hc-ctx-tok">ctx {formatarTokens(ultimaMeta.tokensContexto)} tok</span>}
          </div>

          <div
            ref={listaRef}
            className="hc-lista"
            role="log"
            aria-live="polite"
            aria-busy={carregando}
            data-lenis-prevent
          >
            <div className="hc-ia">
              <span className="hc-mini" aria-hidden="true">
                <IconeAgente agente={agente} tamanho={14} />
              </span>
              <div className="hc-bolha">
                <Markdown texto={config.saudacao} />
              </div>
            </div>

            {bolhas.map((b) =>
              b.papel === 'user' ? (
                <p key={b.id} className="hc-eu">
                  {b.texto}
                </p>
              ) : (
                <div key={b.id} className="hc-ia">
                  <span className="hc-mini" aria-hidden="true">
                <IconeAgente agente={agente} tamanho={14} />
              </span>
                  <div className={`hc-bolha ${b.erro ? 'hc-bolha--erro' : ''}`}>
                    {b.meta && <p className="hc-meta">✓ {linhaMeta(b.meta)}</p>}
                    <Markdown
                      texto={b.texto}
                      reunioes={(b.fontes ?? []).filter((f) => f.tipo === 'REUNIAO').map((f) => f.id)}
                    />
                    {b.fontes && b.fontes.length > 0 && (
                      <ul className="hc-fontes" aria-label="Fontes">
                        <li className="hc-fontes-rot" aria-hidden="true">
                          Fontes
                        </li>
                        {b.fontes.map((f) => {
                          const { texto: rotulo, para } = rotuloFonte(f);
                          return (
                            <li key={`${f.tipo}-${f.id}`}>
                              <Link to={para}>↗ {rotulo}</Link>
                            </li>
                          );
                        })}
                      </ul>
                    )}
                  </div>
                </div>
              ),
            )}

            {carregando && (
              <div className="hc-ia">
                <span className="hc-mini" aria-hidden="true">
                <IconeAgente agente={agente} tamanho={14} />
              </span>
                <p className="hc-bolha hc-digitando">
                  <span className="sr-only">Hermes está consultando…</span>
                  <span aria-hidden="true" />
                  <span aria-hidden="true" />
                  <span aria-hidden="true" />
                </p>
              </div>
            )}
          </div>

          {!vitrine && bolhas.length === 0 && (
            <div className="hc-sugestoes" role="group" aria-label="Sugestões de pergunta">
              {config.sugestoes(reuniaoId).map((s) => (
                <button key={s} type="button" className="hc-chip" onClick={() => enviar(s)}>
                  <b aria-hidden="true">{config.marcaSugestao}</b>
                  {s}
                </button>
              ))}
            </div>
          )}

          {vitrine ? (
            <p className="hc-aviso" role="status">
              <strong>Modo demonstração.</strong> O Hermes precisa da API no ar para responder.
            </p>
          ) : (
            <form className="hc-form" onSubmit={aoEnviar}>
              <div className="hc-campo">
                <label className="sr-only" htmlFor={`${idPainel}-campo`}>
                  Sua pergunta
                </label>
                <textarea
                  id={`${idPainel}-campo`}
                  ref={campoRef}
                  rows={2}
                  value={texto}
                  maxLength={MAX_CARACTERES}
                  placeholder={config.placeholder(reuniaoId)}
                  onChange={(e) => setTexto(e.target.value)}
                  onKeyDown={aoTeclar}
                  disabled={carregando}
                />
                <div className="hc-campo-pe">
                  <span className="hc-teclas" aria-hidden="true">
                    <kbd>Enter</kbd> enviar · <kbd>Shift</kbd>+<kbd>Enter</kbd> quebra
                  </span>
                  <button type="submit" className="hc-enviar" disabled={carregando || !texto.trim()}>
                    Enviar ↵
                  </button>
                </div>
              </div>
              <div className="hc-rodape">
                <span>{config.rodape}</span>
                <span>
                  {texto.length} / {MAX_CARACTERES}
                </span>
              </div>
            </form>
          )}
        </section>
      )}
    </>
  );
}
