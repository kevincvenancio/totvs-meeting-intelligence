/* Carteira — GET /api/v1/clientes.

   Cartões em vez de tabela: aqui a unidade de leitura é a conta inteira, não
   um campo isolado, e o status precisa ser visível de longe para varrer a
   carteira à procura de quem está em risco. */

import { Link, useSearchParams } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { clientes } from '../lib/api';
import { corStatusCliente, n, rotuloStatusCliente } from '../lib/format';
import type { StatusCliente } from '../lib/types';
import { Erro, Esqueleto, Selo, Tela, Vazio } from '../components/app/Shell';
import { Paginacao } from './Reunioes';

const STATUS: StatusCliente[] = ['ATIVO', 'EM_RISCO', 'PROSPECT', 'INATIVO'];

export function Clientes() {
  const [parametros, definirParametros] = useSearchParams();

  const filtro = {
    termo: parametros.get('termo') ?? undefined,
    status: parametros.get('status') ?? undefined,
    uf: parametros.get('uf') ?? undefined,
    pagina: Number(parametros.get('pagina') ?? 0),
    tamanho: 24,
  };

  const { data, isLoading, error } = useQuery({
    queryKey: ['clientes', filtro],
    queryFn: () => clientes.listar(filtro),
    placeholderData: keepPreviousData,
  });

  function atualizar(chave: string, valor: string) {
    const proximos = new URLSearchParams(parametros);
    if (valor) proximos.set(chave, valor);
    else proximos.delete(chave);
    if (chave !== 'pagina') proximos.delete('pagina');
    definirParametros(proximos, { replace: true });
  }

  return (
    <Tela
      chapeu="Acompanhamento comercial"
      titulo="Carteira de clientes"
      descricao="Cada conta com CNPJ validado, status e o total de reuniões já analisadas."
      largo
    >
      <div className="filtros" role="search">
        <label className="filtro filtro--busca">
          <span className="sr-only">Buscar cliente</span>
          <input
            type="search"
            placeholder="Buscar por razão social ou CNPJ…"
            defaultValue={filtro.termo ?? ''}
            onChange={(evento) => atualizar('termo', evento.target.value)}
          />
        </label>

        <div className="filtro-pilulas" role="group" aria-label="Filtrar por status">
          <button
            type="button"
            data-ativo={!filtro.status || undefined}
            onClick={() => atualizar('status', '')}
          >
            Todos
          </button>
          {STATUS.map((status) => (
            <button
              key={status}
              type="button"
              data-ativo={filtro.status === status || undefined}
              style={{ '--tom': corStatusCliente[status] } as React.CSSProperties}
              onClick={() => atualizar('status', filtro.status === status ? '' : status)}
            >
              {rotuloStatusCliente[status]}
            </button>
          ))}
        </div>
      </div>

      {isLoading && !data && <Esqueleto linhas={6} altura={140} />}
      {error && <Erro mensagem={(error as Error).message} />}

      {data && data.conteudo.length === 0 && (
        <Vazio titulo="Nenhum cliente encontrado" texto="Ajuste a busca ou o filtro de status." />
      )}

      {data && data.conteudo.length > 0 && (
        <>
          <ul className="cartoes">
            {data.conteudo.map((cliente) => (
              <li key={cliente.id}>
                <article
                  className="cartao-cliente"
                  style={{ '--tom': corStatusCliente[cliente.status] } as React.CSSProperties}
                >
                  <header className="cartao-cliente-topo">
                    <Selo
                      texto={rotuloStatusCliente[cliente.status]}
                      tom={corStatusCliente[cliente.status]}
                    />
                    {cliente.notaNps != null && (
                      <span className="cartao-nps">
                        <span className="ref">NPS</span>
                        <span className="numeric">{cliente.notaNps.toFixed(1)}</span>
                      </span>
                    )}
                  </header>

                  <h2 className="cartao-cliente-nome">{cliente.nomeExibicao}</h2>
                  <p className="cartao-cliente-cnpj ref">{cliente.cnpjFormatado}</p>

                  <dl className="cartao-cliente-dados">
                    <div>
                      <dt>Segmento</dt>
                      <dd>{cliente.segmento ?? '—'}</dd>
                    </div>
                    <div>
                      <dt>Praça</dt>
                      <dd>
                        {cliente.cidade ?? '—'}
                        {cliente.uf ? ` · ${cliente.uf}` : ''}
                      </dd>
                    </div>
                    <div>
                      <dt>Faturamento</dt>
                      <dd>{cliente.faixaFaturamento ?? '—'}</dd>
                    </div>
                    <div>
                      <dt>Reuniões</dt>
                      <dd className="numeric">{n(cliente.totalReunioes ?? 0)}</dd>
                    </div>
                  </dl>

                  <Link
                    to={`/reunioes?clienteId=${cliente.id}`}
                    className="cartao-cliente-link"
                    data-cursor="Abrir"
                  >
                    Ver reuniões
                    <span aria-hidden="true">→</span>
                  </Link>
                </article>
              </li>
            ))}
          </ul>

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
