/* Detalhe da reunião — a análise completa em uma tela.

   Duas colunas: à esquerda o que a reunião foi (análise, insights, feedback);
   à direita o que se faz com isso (planos, comentários, ações). O feedback
   educativo ganha destaque próprio porque é a parte que muda o comportamento
   de quem lê, não só o dado. */

import { Link, useParams } from 'react-router-dom';
import { useQueries } from '@tanstack/react-query';

import { relatorios, reunioes } from '../lib/api';
import {
  corCompletude,
  corRisco,
  corSentimento,
  corStatusPlano,
  dt,
  dth,
  iniciais,
  linhas,
  n,
  rotuloCompletude,
  rotuloCriticidade,
  rotuloRisco,
  rotuloSentimento,
  rotuloStatusPlano,
} from '../lib/format';
import type {
  NivelCriticidade,
  RiscoChurn,
  Sentimento,
  StatusCompletude,
  StatusPlanoAcao,
} from '../lib/types';
import { Erro, Esqueleto, Selo, Tela } from '../components/app/Shell';

export function ReuniaoDetalhe() {
  const { id } = useParams();
  const numero = Number(id);

  const [detalhe, listaInsights, feedback, planos, comentarios] = useQueries({
    queries: [
      { queryKey: ['reuniao', numero], queryFn: () => reunioes.buscar(numero) },
      { queryKey: ['reuniao', numero, 'insights'], queryFn: () => reunioes.insights(numero) },
      { queryKey: ['reuniao', numero, 'feedback'], queryFn: () => reunioes.feedback(numero) },
      { queryKey: ['reuniao', numero, 'planos'], queryFn: () => reunioes.planos(numero) },
      { queryKey: ['reuniao', numero, 'comentarios'], queryFn: () => reunioes.comentarios(numero) },
    ],
  });

  const r = detalhe.data;

  return (
    <Tela
      chapeu={r?.idExterno ? `Reunião ${r.idExterno}` : 'Reunião'}
      titulo={r?.cliente ?? 'Carregando…'}
      descricao={r?.temaReuniao}
      largo
      acoes={
        <>
          <Link to="/reunioes" className="botao botao--fantasma">
            ← Voltar
          </Link>
          {r && (
            <a
              href={relatorios.reuniao(r.id)}
              className="botao botao--contorno"
              target="_blank"
              rel="noreferrer"
            >
              PDF da reunião
            </a>
          )}
        </>
      }
    >
      {detalhe.isLoading && <Esqueleto linhas={6} altura={90} />}
      {detalhe.error && <Erro mensagem={(detalhe.error as Error).message} />}

      {r && (
        <>
          {r.duplicada && (
            <div className="aviso-vitrine aviso-vitrine--alerta" role="status">
              <span className="aviso-ponto" aria-hidden="true" />
              <p>
                <strong>Reunião duplicada.</strong> {r.motivoDuplicidade}
                {r.idReuniaoOriginalDuplicada && (
                  <>
                    {' '}
                    <Link to={`/reunioes/${r.idReuniaoOriginalDuplicada}`}>Ver a original →</Link>
                  </>
                )}
              </p>
            </div>
          )}

          {/* Faixa de classificação */}
          <section className="detalhe-classificacao" aria-label="Classificação automática">
            <div className="detalhe-selo-grupo">
              <span className="detalhe-selo-rotulo">Completude</span>
              {r.statusCompletude && (
                <Selo
                  texto={`${rotuloCompletude[r.statusCompletude as StatusCompletude]} · ${r.pontuacaoCompletude ?? 0}`}
                  tom={corCompletude[r.statusCompletude as StatusCompletude]}
                />
              )}
            </div>
            <div className="detalhe-selo-grupo">
              <span className="detalhe-selo-rotulo">Sentimento</span>
              {r.sentimento && (
                <Selo
                  texto={rotuloSentimento[r.sentimento as Sentimento]}
                  tom={corSentimento[r.sentimento as Sentimento]}
                />
              )}
            </div>
            <div className="detalhe-selo-grupo">
              <span className="detalhe-selo-rotulo">Risco de churn</span>
              {r.riscoChurn && (
                <Selo
                  texto={rotuloRisco[r.riscoChurn as RiscoChurn]}
                  tom={corRisco[r.riscoChurn as RiscoChurn]}
                />
              )}
            </div>
            <div className="detalhe-selo-grupo">
              <span className="detalhe-selo-rotulo">Score comercial</span>
              <span className="detalhe-score numeric">{r.scoreComercial ?? '—'}</span>
            </div>
            <div className="detalhe-selo-grupo">
              <span className="detalhe-selo-rotulo">Score de qualidade</span>
              <span className="detalhe-score numeric">{r.scoreQualidade ?? '—'}</span>
            </div>
          </section>

          <div className="detalhe-grade">
            <div className="detalhe-principal">
              {r.resumoReuniao && (
                <Bloco titulo="Resumo da conversa">
                  <p className="detalhe-texto">{r.resumoReuniao}</p>
                  {r.sentimentoJustificativa && (
                    <p className="detalhe-justificativa">
                      <span className="eyebrow">Por que este sentimento</span>
                      {r.sentimentoJustificativa}
                    </p>
                  )}
                </Bloco>
              )}

              {linhas(r.pontosPrincipais).length > 0 && (
                <Bloco titulo="Pontos principais">
                  <ul className="detalhe-lista">
                    {linhas(r.pontosPrincipais).map((ponto) => (
                      <li key={ponto}>{ponto}</li>
                    ))}
                  </ul>
                </Bloco>
              )}

              <div className="detalhe-par">
                {linhas(r.doresIdentificadas).length > 0 && (
                  <Bloco titulo="Dores identificadas" tom="var(--color-data-1)">
                    <ul className="detalhe-lista">
                      {linhas(r.doresIdentificadas).map((dor) => (
                        <li key={dor}>{dor}</li>
                      ))}
                    </ul>
                  </Bloco>
                )}
                {linhas(r.oportunidades).length > 0 && (
                  <Bloco titulo="Oportunidades" tom="var(--color-data-2)">
                    <ul className="detalhe-lista">
                      {linhas(r.oportunidades).map((op) => (
                        <li key={op}>{op}</li>
                      ))}
                    </ul>
                  </Bloco>
                )}
              </div>

              {listaInsights.data && listaInsights.data.length > 0 && (
                <Bloco titulo={`Insights extraídos (${listaInsights.data.length})`}>
                  <ul className="detalhe-insights">
                    {listaInsights.data.map((insight) => (
                      <li key={insight.id}>
                        <div className="detalhe-insight-topo">
                          <span className="detalhe-insight-tipo ref">{insight.tipo}</span>
                          <span className="detalhe-insight-confianca">
                            <span className="detalhe-insight-barra" aria-hidden="true">
                              <span style={{ width: `${insight.confianca ?? 0}%` }} />
                            </span>
                            <span className="numeric">{insight.confianca}%</span>
                          </span>
                        </div>
                        <p>{insight.descricao}</p>
                        {insight.trechoOrigem && (
                          <blockquote className="detalhe-trecho">{insight.trechoOrigem}</blockquote>
                        )}
                      </li>
                    ))}
                  </ul>
                </Bloco>
              )}

              {feedback.data && (
                <Bloco
                  titulo="Feedback educativo"
                  tom="var(--color-data-3)"
                  destaque
                  acao={
                    feedback.data.nivelCriticidade && (
                      <Selo
                        texto={`Criticidade ${rotuloCriticidade[
                          feedback.data.nivelCriticidade as NivelCriticidade
                        ].toLowerCase()}`}
                        tom={
                          feedback.data.nivelCriticidade === 'ALTA'
                            ? 'var(--color-state-critical)'
                            : feedback.data.nivelCriticidade === 'MEDIA'
                              ? 'var(--color-state-warn)'
                              : 'var(--color-state-good)'
                        }
                      />
                    )
                  }
                >
                  <dl className="detalhe-feedback">
                    <div>
                      <dt>Problema identificado</dt>
                      <dd>{feedback.data.problemaIdentificado}</dd>
                    </div>
                    <div>
                      <dt>Por que passou despercebido</dt>
                      <dd>{feedback.data.motivoNaoIdentificadoAntes}</dd>
                    </div>
                    <div>
                      <dt>Sinais que estavam na conversa</dt>
                      <dd>{feedback.data.sinaisNaConversa}</dd>
                    </div>
                    <div>
                      <dt>Como identificar antes</dt>
                      <dd>{feedback.data.comoIdentificarAntes}</dd>
                    </div>
                  </dl>

                  {linhas(feedback.data.perguntasRecomendadas).length > 0 && (
                    <>
                      <span className="eyebrow detalhe-subtitulo">Perguntas recomendadas</span>
                      <ul className="detalhe-perguntas">
                        {linhas(feedback.data.perguntasRecomendadas).map((pergunta) => (
                          <li key={pergunta}>{pergunta}</li>
                        ))}
                      </ul>
                    </>
                  )}

                  {feedback.data.mensagemEducativa && (
                    <p className="detalhe-mensagem">{feedback.data.mensagemEducativa}</p>
                  )}
                </Bloco>
              )}

              {r.recomendacaoFinal && (
                <Bloco titulo="Recomendação final" tom="var(--accent)">
                  <p className="detalhe-texto">{r.recomendacaoFinal}</p>
                </Bloco>
              )}
            </div>

            <aside className="detalhe-lateral">
              <Bloco titulo="Ficha">
                <dl className="detalhe-ficha">
                  {[
                    ['Data', dt(r.data)],
                    ['Vendedor', r.vendedor],
                    ['Formato', r.formato],
                    ['Duração', r.duracao],
                    ['Segmento', r.segmento],
                    ['UF', r.uf],
                    ['Faturamento', r.faturamento],
                    ['NPS', r.notaNps != null ? String(r.notaNps) : undefined],
                    ['Locutores', r.quantidadeLocutores != null ? String(r.quantidadeLocutores) : undefined],
                    ['Palavras', r.quantidadePalavras != null ? n(r.quantidadePalavras) : undefined],
                    ['Lote', r.codigoLote],
                    ['Importada em', dth(r.dataImportacao)],
                  ]
                    .filter(([, valor]) => valor)
                    .map(([rotulo, valor]) => (
                      <div key={rotulo}>
                        <dt>{rotulo}</dt>
                        <dd>{valor}</dd>
                      </div>
                    ))}
                </dl>
              </Bloco>

              {r.produtosIdentificados && (
                <Bloco titulo="Produtos citados">
                  <ul className="detalhe-etiquetas">
                    {r.produtosIdentificados.split(/,\s*/).map((produto) => (
                      <li key={produto}>{produto}</li>
                    ))}
                  </ul>
                </Bloco>
              )}

              {r.concorrentesIdentificados && (
                <Bloco titulo="Concorrentes citados" tom="var(--color-data-6)">
                  <ul className="detalhe-etiquetas">
                    {r.concorrentesIdentificados.split(/,\s*/).map((c) => (
                      <li key={c}>{c}</li>
                    ))}
                  </ul>
                </Bloco>
              )}

              <Bloco titulo={`Planos de ação (${planos.data?.length ?? 0})`}>
                {planos.data && planos.data.length > 0 ? (
                  <ul className="detalhe-planos">
                    {planos.data.map((plano) => (
                      <li key={plano.id}>
                        <div className="detalhe-plano-topo">
                          <Selo
                            texto={rotuloStatusPlano[plano.status as StatusPlanoAcao]}
                            tom={corStatusPlano[plano.status as StatusPlanoAcao]}
                          />
                          {plano.atrasado && (
                            <Selo texto="Atrasado" tom="var(--color-state-critical)" />
                          )}
                        </div>
                        <p className="detalhe-plano-titulo">{plano.titulo}</p>
                        <p className="detalhe-plano-meta ref">
                          {plano.responsavel?.nome} · prazo {dt(plano.prazo)}
                        </p>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="detalhe-vazio">Nenhum plano de ação criado para esta reunião.</p>
                )}
                <Link to="/planos-acao" className="botao botao--contorno detalhe-botao-largo">
                  Ver todos os planos
                </Link>
              </Bloco>

              <Bloco titulo={`Comentários (${comentarios.data?.totalElementos ?? 0})`}>
                {comentarios.data && comentarios.data.conteudo.length > 0 ? (
                  <ul className="detalhe-comentarios">
                    {comentarios.data.conteudo.map((comentario) => (
                      <li key={comentario.id}>
                        <div className="detalhe-comentario-autor">
                          <span className="avatar" aria-hidden="true">
                            {iniciais(comentario.autor?.nome)}
                          </span>
                          <span>
                            <strong>{comentario.autor?.nome}</strong>
                            <span className="ref">
                              {dth(comentario.dataCriacao)}
                              {comentario.editado && ' · editado'}
                            </span>
                          </span>
                        </div>
                        <p>{comentario.texto}</p>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="detalhe-vazio">Ainda sem comentários.</p>
                )}
              </Bloco>
            </aside>
          </div>
        </>
      )}
    </Tela>
  );
}

function Bloco({
  titulo,
  children,
  tom,
  destaque,
  acao,
}: {
  titulo: string;
  children: React.ReactNode;
  tom?: string;
  destaque?: boolean;
  acao?: React.ReactNode;
}) {
  return (
    <section
      className={`bloco ${destaque ? 'bloco--destaque' : ''}`}
      style={{ '--tom': tom ?? 'var(--hairline)' } as React.CSSProperties}
    >
      <header className="bloco-cabecalho">
        <h2 className="bloco-titulo">{titulo}</h2>
        {acao}
      </header>
      <div className="bloco-corpo">{children}</div>
    </section>
  );
}
