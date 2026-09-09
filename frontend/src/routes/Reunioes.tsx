/* Listagem de reuniões — GET /api/v1/reunioes.

   Os filtros ficam numa linha só, acima da grade, e vivem na URL: um recorte
   interessante é um link que se manda para alguém. */

import { Link, useSearchParams } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { reunioes, type FiltroReuniao } from '../lib/api';
import {
  corCompletude,
  corRisco,
  corSentimento,
  dt,
  n,
  rotuloCompletude,
  rotuloRisco,
  rotuloSentimento,
} from '../lib/format';
import type { RiscoChurn, Sentimento, StatusCompletude } from '../lib/types';
import { Erro, Esqueleto, Selo, Tela, Vazio } from '../components/app/Shell';

const SENTIMENTOS: Sentimento[] = [
  'POSITIVO',
  'OPORTUNIDADE_COMERCIAL',
  'MISTO',
  'NEUTRO',
  'NEGATIVO',
  'CRITICO',
];

const ORDENACOES = [
  { valor: 'data', rotulo: 'Data' },
  { valor: 'scoreComercial', rotulo: 'Score comercial' },
  { valor: 'scoreQualidade', rotulo: 'Score de qualidade' },
  { valor: 'cliente', rotulo: 'Cliente' },
];

export function Reunioes() {
  const [parametros, definirParametros] = useSearchParams();

  const filtro: FiltroReuniao = {
    termo: parametros.get('termo') ?? undefined,
    sentimento: parametros.get('sentimento') ?? undefined,
    riscoChurn: parametros.get('riscoChurn') ?? undefined,
    statusCompletude: parametros.get('statusCompletude') ?? undefined,
    ordenarPor: parametros.get('ordenarPor') ?? 'data',
    direcao: (parametros.get('direcao') as 'asc' | 'desc') ?? 'desc',
    pagina: Number(parametros.get('pagina') ?? 0),
    tamanho: 20,
  };

  const { data, isLoading, error } = useQuery({
    queryKey: ['reunioes', filtro],
    queryFn: () => reunioes.listar(filtro),
    placeholderData: keepPreviousData,
  });

  function atualizar(chave: string, valor: string) {
    const proximos = new URLSearchParams(parametros);
    if (valor) proximos.set(chave, valor);
    else proximos.delete(chave);
    // Trocar o filtro sempre volta para a primeira página.
    if (chave !== 'pagina') proximos.delete('pagina');
    definirParametros(proximos, { replace: true });
  }

  const temFiltro = ['termo', 'sentimento', 'riscoChurn', 'statusCompletude'].some((c) =>
    parametros.get(c),
  );

  return (
    <Tela
      chapeu="Base analisada"
      titulo="Reuniões"
      descricao="Cada linha é uma transcrição lida, classificada e pontuada pelo motor de análise."
      largo
      acoes={
        temFiltro ? (
          <button
            type="button"
            className="botao botao--fantasma"
            onClick={() => definirParametros(new URLSearchParams(), { replace: true })}
          >
            Limpar filtros
          </button>
        ) : undefined
      }
    >
      <div className="filtros" role="search">
        <label className="filtro filtro--busca">
          <span className="sr-only">Buscar por cliente, vendedor ou identificador</span>
          <input
            type="search"
            placeholder="Buscar por cliente, vendedor, identificador…"
            defaultValue={filtro.termo ?? ''}
            onChange={(evento) => atualizar('termo', evento.target.value)}
          />
        </label>

        <label className="filtro">
          <span>Sentimento</span>
          <select
            value={filtro.sentimento ?? ''}
            onChange={(evento) => atualizar('sentimento', evento.target.value)}
          >
            <option value="">Todos</option>
            {SENTIMENTOS.map((s) => (
              <option key={s} value={s}>
                {rotuloSentimento[s]}
              </option>
            ))}
          </select>
        </label>

        <label className="filtro">
          <span>Risco de churn</span>
          <select
            value={filtro.riscoChurn ?? ''}
            onChange={(evento) => atualizar('riscoChurn', evento.target.value)}
          >
            <option value="">Todos</option>
            <option value="ALTO">Alto</option>
            <option value="MEDIO">Médio</option>
            <option value="BAIXO">Baixo</option>
          </select>
        </label>

        <label className="filtro">
          <span>Completude</span>
          <select
            value={filtro.statusCompletude ?? ''}
            onChange={(evento) => atualizar('statusCompletude', evento.target.value)}
          >
            <option value="">Todas</option>
            <option value="COMPLETA">Completa</option>
            <option value="PARCIAL">Parcial</option>
            <option value="INCOMPLETA">Incompleta</option>
          </select>
        </label>

        <label className="filtro">
          <span>Ordenar por</span>
          <select
            value={filtro.ordenarPor}
            onChange={(evento) => atualizar('ordenarPor', evento.target.value)}
          >
            {ORDENACOES.map((o) => (
              <option key={o.valor} value={o.valor}>
                {o.rotulo}
              </option>
            ))}
          </select>
        </label>

        <button
          type="button"
          className="filtro-direcao"
          onClick={() => atualizar('direcao', filtro.direcao === 'asc' ? 'desc' : 'asc')}
          aria-label={filtro.direcao === 'asc' ? 'Ordem crescente' : 'Ordem decrescente'}
          title={filtro.direcao === 'asc' ? 'Crescente' : 'Decrescente'}
        >
          {filtro.direcao === 'asc' ? '↑' : '↓'}
        </button>
      </div>

      {isLoading && !data && <Esqueleto linhas={8} />}
      {error && <Erro mensagem={(error as Error).message} />}

      {data && data.conteudo.length === 0 && (
        <Vazio
          titulo="Nenhuma reunião com esse recorte"
          texto="Ajuste os filtros ou limpe a busca para ver a base completa."
        />
      )}

      {data && data.conteudo.length > 0 && (
        <>
          <div className="grade-caixa">
            <table className="grade">
              <caption className="sr-only">
                Reuniões analisadas, {n(data.totalElementos)} no total.
              </caption>
              <thead>
                <tr>
                  <th scope="col">Cliente</th>
                  <th scope="col">Data</th>
                  <th scope="col">Categoria</th>
                  <th scope="col">Completude</th>
                  <th scope="col">Sentimento</th>
                  <th scope="col">Churn</th>
                  <th scope="col" className="alinha-direita">Comercial</th>
                  <th scope="col" className="alinha-direita">Qualidade</th>
                </tr>
              </thead>
              <tbody>
                {data.conteudo.map((reuniao) => (
                  <tr key={reuniao.id}>
                    <th scope="row">
                      <Link to={`/reunioes/${reuniao.id}`} className="grade-principal">
                        {reuniao.cliente ?? 'Sem cliente'}
                        <span className="grade-secundario ref">{reuniao.idExterno}</span>
                      </Link>
                    </th>
                    <td className="numeric">{dt(reuniao.data)}</td>
                    <td>{reuniao.categoriaPrincipal ?? '—'}</td>
                    <td>
                      {reuniao.statusCompletude && (
                        <Selo
                          texto={rotuloCompletude[reuniao.statusCompletude as StatusCompletude]}
                          tom={corCompletude[reuniao.statusCompletude as StatusCompletude]}
                        />
                      )}
                    </td>
                    <td>
                      {reuniao.sentimento && (
                        <span className="grade-sentimento">
                          <span
                            className="viz-marca"
                            style={{ background: corSentimento[reuniao.sentimento as Sentimento] }}
                            aria-hidden="true"
                          />
                          {rotuloSentimento[reuniao.sentimento as Sentimento]}
                        </span>
                      )}
                    </td>
                    <td>
                      {reuniao.riscoChurn && (
                        <Selo
                          texto={rotuloRisco[reuniao.riscoChurn as RiscoChurn]}
                          tom={corRisco[reuniao.riscoChurn as RiscoChurn]}
                        />
                      )}
                    </td>
                    <td className="numeric alinha-direita">
                      <Medidor valor={reuniao.scoreComercial ?? 0} cor="var(--color-data-2)" />
                    </td>
                    <td className="numeric alinha-direita">
                      <Medidor valor={reuniao.scoreQualidade ?? 0} cor="var(--color-data-3)" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Paginacao
            pagina={data.pagina}
            totalPaginas={data.totalPaginas}
            totalElementos={data.totalElementos}
            aoIr={(p) => atualizar('pagina', String(p))}
          />
        </>
      )}
    </Tela>
  );
}

/* Barra minúscula ao lado do número: a leitura relativa vem antes da exata. */
function Medidor({ valor, cor }: { valor: number; cor: string }) {
  return (
    <span className="medidor-celula">
      <span className="medidor-trilho" aria-hidden="true">
        <span style={{ width: `${Math.min(valor, 100)}%`, background: cor }} />
      </span>
      {valor}
    </span>
  );
}

export function Paginacao({
  pagina,
  totalPaginas,
  totalElementos,
  aoIr,
}: {
  pagina: number;
  totalPaginas: number;
  totalElementos: number;
  aoIr: (pagina: number) => void;
}) {
  if (totalPaginas <= 1) {
    return (
      <p className="paginacao-resumo ref">
        {n(totalElementos)} {totalElementos === 1 ? 'registro' : 'registros'}
      </p>
    );
  }

  return (
    <nav className="paginacao" aria-label="Paginação">
      <button type="button" disabled={pagina === 0} onClick={() => aoIr(pagina - 1)}>
        ← Anterior
      </button>
      <span className="ref numeric">
        Página {pagina + 1} de {totalPaginas} · {n(totalElementos)} registros
      </span>
      <button
        type="button"
        disabled={pagina >= totalPaginas - 1}
        onClick={() => aoIr(pagina + 1)}
      >
        Próxima →
      </button>
    </nav>
  );
}
