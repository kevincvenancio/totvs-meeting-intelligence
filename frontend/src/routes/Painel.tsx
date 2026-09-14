/* Painel executivo — GET /api/v1/dashboard.

   A ordem da tela é a ordem da pergunta que um gestor faz ao abrir isto de
   manhã: quanto tem, o que está em risco, o que fazer hoje, e só depois a
   composição da carteira. */

import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';

import { painel, relatorios } from '../lib/api';
import {
  corRisco,
  corSentimento,
  corStatusPlano,
  n,
  rotuloRisco,
  rotuloSentimento,
  rotuloStatusPlano,
} from '../lib/format';
import type { RiscoChurn, Sentimento, StatusPlanoAcao } from '../lib/types';
import { Erro, Esqueleto, Selo, Tela } from '../components/app/Shell';
import { Grafico, Painel as ValorHeroi } from '../components/viz/primitives';
import { Rosca } from '../components/viz/Rosca';
import { Ranking } from '../components/viz/Ranking';
import { Contador } from '../components/fx/primitives';

export function Painel() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard'],
    queryFn: painel.obter,
  });

  return (
    <Tela
      chapeu="Visão consolidada"
      titulo="Painel executivo"
      descricao="Os indicadores da análise automática somados aos do acompanhamento comercial."
      acoes={
        <>
          <a
            href={relatorios.executivo()}
            className="botao botao--contorno"
            target="_blank"
            rel="noreferrer"
          >
            Baixar PDF executivo
          </a>
          <Link to="/reunioes?riscoChurn=ALTO" className="botao botao--acento">
            Fila de risco
          </Link>
        </>
      }
    >
      {isLoading && <Esqueleto linhas={4} altura={104} />}
      {error && <Erro mensagem={(error as Error).message} />}

      {data && (
        <div className="painel-grade">
          {/* Linha 1 — o volume */}
          <section className="painel-kpis" aria-label="Indicadores principais">
            <ValorHeroi
              rotulo="Reuniões analisadas"
              valor={<Contador valor={data.totalReunioes} />}
              apoio={`${n(data.totalClientesCitados)} clientes citados`}
            />
            <ValorHeroi
              rotulo="Risco de churn alto"
              tom="var(--color-state-critical)"
              valor={<Contador valor={data.totalChurnAlto} />}
              apoio={`${n(data.totalChurnMedio)} em risco médio`}
            />
            <ValorHeroi
              rotulo="Oportunidades"
              tom="var(--color-data-2)"
              valor={<Contador valor={data.totalOportunidades} />}
              apoio={`score comercial médio ${data.scoreComercialMedia}`}
            />
            <ValorHeroi
              rotulo="Planos em aberto"
              tom="var(--color-state-warn)"
              valor={<Contador valor={data.planosAcao.emAberto} />}
              apoio={`${n(data.planosAcao.atrasados)} atrasados`}
            />
          </section>

          {/* Linha 2 — composição */}
          <Rosca
            titulo="Sentimento das reuniões"
            descricao="Classificação automática, seis classes possíveis."
            className="painel-rosca"
            fatias={Object.entries(data.sentimentos)
              .map(([chave, valor]) => ({
                rotulo: rotuloSentimento[chave as Sentimento] ?? chave,
                valor,
                cor: corSentimento[chave as Sentimento] ?? 'var(--color-state-idle)',
              }))
              .sort((a, b) => b.valor - a.valor)}
            miolo={{
              valor: rotuloSentimento[data.sentimentoPredominante as Sentimento] ?? '—',
              rotulo: 'predominante',
            }}
            nota="Toda classificação vem acompanhada de justificativa no detalhe da reunião."
          />

          <Grafico
            titulo="Situação dos planos de ação"
            descricao={`${n(data.planosAcao.total)} planos no total.`}
            className="painel-planos"
            nota="O ciclo de vida é do próprio domínio: o status conhece as transições permitidas."
            tabela={{
              colunas: ['Status', 'Planos'],
              linhas: Object.entries(data.planosAcao.totalPorStatus).map(([k, v]) => [
                rotuloStatusPlano[k as StatusPlanoAcao] ?? k,
                n(v),
              ]),
            }}
          >
            <ul className="barras-estado">
              {Object.entries(data.planosAcao.totalPorStatus)
                .sort((a, b) => b[1] - a[1])
                .map(([chave, valor]) => {
                  const maximo = Math.max(...Object.values(data.planosAcao.totalPorStatus), 1);
                  return (
                    <li key={chave}>
                      <span className="barras-estado-rotulo">
                        <span
                          className="viz-marca"
                          style={{ background: corStatusPlano[chave as StatusPlanoAcao] }}
                          aria-hidden="true"
                        />
                        {rotuloStatusPlano[chave as StatusPlanoAcao] ?? chave}
                      </span>
                      <span className="barras-estado-trilho">
                        <span
                          style={{
                            width: `${(valor / maximo) * 100}%`,
                            background: corStatusPlano[chave as StatusPlanoAcao],
                          }}
                        />
                      </span>
                      <span className="numeric barras-estado-valor">{n(valor)}</span>
                    </li>
                  );
                })}
            </ul>

            {data.planosAcao.atrasados > 0 && (
              <p className="painel-alerta">
                <Selo texto={`${data.planosAcao.atrasados} atrasados`} tom="var(--color-state-critical)" />
                <Link to="/planos-acao?somenteAtrasados=true">Ver planos vencidos →</Link>
              </p>
            )}
          </Grafico>

          {/* Linha 3 — rankings */}
          <Ranking
            titulo="Produtos mais citados"
            descricao="Menções extraídas das transcrições."
            className="painel-produtos"
            itens={Object.entries(data.topProdutos)
              .map(([rotulo, valor]) => ({ rotulo, valor }))
              .sort((a, b) => b.valor - a.valor)
              .slice(0, 7)}
          />

          <Ranking
            titulo="Concorrentes mais citados"
            descricao="Quando aparecem, a conversa muda de assunto."
            className="painel-concorrentes"
            cor="var(--color-data-6)"
            itens={Object.entries(data.topConcorrentes)
              .map(([rotulo, valor]) => ({ rotulo, valor }))
              .sort((a, b) => b.valor - a.valor)
              .slice(0, 6)}
          />

          {/* Linha 4 — as duas filas */}
          <FilaDeReunioes
            titulo="Maior risco de churn"
            descricao="Ordenadas pelo risco calculado na análise."
            className="painel-fila-risco"
            reunioes={data.topRiscoChurn}
            destaque="risco"
          />

          <FilaDeReunioes
            titulo="Maiores oportunidades"
            descricao="Ordenadas pelo score comercial."
            className="painel-fila-oportunidade"
            reunioes={data.topOportunidades}
            destaque="score"
          />
        </div>
      )}
    </Tela>
  );
}

function FilaDeReunioes({
  titulo,
  descricao,
  reunioes,
  destaque,
  className,
}: {
  titulo: string;
  descricao: string;
  reunioes: { id: number; cliente?: string; categoriaPrincipal?: string; riscoChurn?: string; scoreComercial?: number }[];
  destaque: 'risco' | 'score';
  className?: string;
}) {
  return (
    <Grafico titulo={titulo} descricao={descricao} className={className}>
      <ol className="fila">
        {reunioes.map((reuniao, indice) => (
          <li key={reuniao.id}>
            <Link to={`/reunioes/${reuniao.id}`} className="fila-linha">
              <span className="fila-posicao numeric">{String(indice + 1).padStart(2, '0')}</span>
              <span className="fila-cliente">{reuniao.cliente ?? 'Sem cliente vinculado'}</span>
              <span className="fila-categoria">{reuniao.categoriaPrincipal}</span>
              {destaque === 'risco' ? (
                <Selo
                  texto={rotuloRisco[reuniao.riscoChurn as RiscoChurn] ?? '—'}
                  tom={corRisco[reuniao.riscoChurn as RiscoChurn] ?? 'var(--color-state-idle)'}
                />
              ) : (
                <span className="numeric fila-score">{reuniao.scoreComercial}</span>
              )}
            </Link>
          </li>
        ))}
      </ol>
    </Grafico>
  );
}
