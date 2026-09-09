/* Planos de ação — quadro por status.

   As colunas são o próprio enum StatusPlanoAcao, e os botões de transição em
   cada cartão vêm de `transicoesPermitidas`, que a API devolve pronto. O front
   não reimplementa a regra: ele desenha o que o domínio autorizou. */

import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { planosAcao } from '../lib/api';
import {
  corStatusPlano,
  dt,
  n,
  relativo,
  rotuloStatusPlano,
} from '../lib/format';
import type { PlanoAcao, StatusPlanoAcao } from '../lib/types';
import { Erro, Esqueleto, Selo, Tela } from '../components/app/Shell';

const COLUNAS: StatusPlanoAcao[] = ['PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO'];

const COR_PRIORIDADE: Record<string, string> = {
  ALTA: 'var(--color-state-critical)',
  MEDIA: 'var(--color-state-warn)',
  BAIXA: 'var(--color-state-idle)',
};

export function PlanosAcao() {
  const [parametros, definirParametros] = useSearchParams();
  const somenteAtrasados = parametros.get('somenteAtrasados') === 'true';
  const clienteConsulta = useQueryClient();
  const [transicionando, setTransicionando] = useState<number | null>(null);

  const filtro = {
    termo: parametros.get('termo') ?? undefined,
    somenteAtrasados: somenteAtrasados || undefined,
    tamanho: 100,
  };

  const { data, isLoading, error } = useQuery({
    queryKey: ['planos', filtro],
    queryFn: () => planosAcao.listar(filtro),
  });

  const indicadores = useQuery({
    queryKey: ['planos', 'indicadores'],
    queryFn: planosAcao.indicadores,
  });

  const mover = useMutation({
    mutationFn: ({ id, status }: { id: number; status: StatusPlanoAcao }) =>
      planosAcao.transicionar(
        id,
        status,
        status === 'CONCLUIDO' || status === 'CANCELADO'
          ? 'Registrado pelo painel Insight360.'
          : undefined,
      ),
    onSettled: () => {
      setTransicionando(null);
      clienteConsulta.invalidateQueries({ queryKey: ['planos'] });
    },
  });

  function atualizar(chave: string, valor: string) {
    const proximos = new URLSearchParams(parametros);
    if (valor) proximos.set(chave, valor);
    else proximos.delete(chave);
    definirParametros(proximos, { replace: true });
  }

  const porStatus = (status: StatusPlanoAcao) =>
    (data?.conteudo ?? []).filter((plano) => plano.status === status);

  return (
    <Tela
      chapeu="Execução"
      titulo="Planos de ação"
      descricao="O que sai da análise e vira trabalho: responsável, prazo e um ciclo de vida que o domínio controla."
      largo
      acoes={
        <button
          type="button"
          className={`botao ${somenteAtrasados ? 'botao--acento' : 'botao--contorno'}`}
          onClick={() => atualizar('somenteAtrasados', somenteAtrasados ? '' : 'true')}
        >
          {somenteAtrasados ? 'Mostrando atrasados' : 'Só os atrasados'}
        </button>
      }
    >
      {indicadores.data && (
        <ul className="indicadores-linha">
          <li>
            <span className="ref">Total</span>
            <span className="numeric">{n(indicadores.data.total)}</span>
          </li>
          <li>
            <span className="ref">Em aberto</span>
            <span className="numeric">{n(indicadores.data.emAberto)}</span>
          </li>
          <li data-alerta={indicadores.data.atrasados > 0 || undefined}>
            <span className="ref">Atrasados</span>
            <span className="numeric">{n(indicadores.data.atrasados)}</span>
          </li>
        </ul>
      )}

      <div className="filtros">
        <label className="filtro filtro--busca">
          <span className="sr-only">Buscar plano</span>
          <input
            type="search"
            placeholder="Buscar pelo título do plano…"
            defaultValue={filtro.termo ?? ''}
            onChange={(evento) => atualizar('termo', evento.target.value)}
          />
        </label>
      </div>

      {isLoading && <Esqueleto linhas={4} altura={180} />}
      {error && <Erro mensagem={(error as Error).message} />}
      {mover.error && <Erro mensagem={(mover.error as Error).message} />}

      {data && (
        <div className="quadro">
          {COLUNAS.map((status) => {
            const itens = porStatus(status);
            return (
              <section
                key={status}
                className="quadro-coluna"
                style={{ '--tom': corStatusPlano[status] } as React.CSSProperties}
                aria-label={rotuloStatusPlano[status]}
              >
                <header className="quadro-cabecalho">
                  <h2>
                    <span className="quadro-ponto" aria-hidden="true" />
                    {rotuloStatusPlano[status]}
                  </h2>
                  <span className="numeric quadro-contagem">{itens.length}</span>
                </header>

                <ul className="quadro-lista">
                  {itens.map((plano) => (
                    <li key={plano.id}>
                      <Cartao
                        plano={plano}
                        ocupado={transicionando === plano.id}
                        aoMover={(proximo) => {
                          setTransicionando(plano.id);
                          mover.mutate({ id: plano.id, status: proximo });
                        }}
                      />
                    </li>
                  ))}
                  {itens.length === 0 && <li className="quadro-vazio">Nada aqui.</li>}
                </ul>
              </section>
            );
          })}
        </div>
      )}
    </Tela>
  );
}

function Cartao({
  plano,
  aoMover,
  ocupado,
}: {
  plano: PlanoAcao;
  aoMover: (status: StatusPlanoAcao) => void;
  ocupado: boolean;
}) {
  return (
    <article className="cartao-plano" data-ocupado={ocupado || undefined}>
      <header className="cartao-plano-topo">
        <Selo
          texto={plano.prioridadeDescricao}
          tom={COR_PRIORIDADE[plano.prioridade] ?? 'var(--color-state-idle)'}
        />
        {plano.atrasado && <Selo texto="Atrasado" tom="var(--color-state-critical)" />}
      </header>

      <h3 className="cartao-plano-titulo">{plano.titulo}</h3>

      {plano.reuniao && (
        <Link to={`/reunioes/${plano.reuniao.id}`} className="cartao-plano-reuniao ref">
          {plano.reuniao.cliente ?? plano.reuniao.idExterno} ↗
        </Link>
      )}

      <dl className="cartao-plano-meta">
        <div>
          <dt>Responsável</dt>
          <dd>{plano.responsavel?.nome ?? '—'}</dd>
        </div>
        <div>
          <dt>Prazo</dt>
          <dd>
            {dt(plano.prazo)}
            {plano.diasRestantes != null && plano.status !== 'CONCLUIDO' && (
              <span className="cartao-plano-relativo"> · {relativo(plano.diasRestantes)}</span>
            )}
          </dd>
        </div>
      </dl>

      {plano.resultado && <p className="cartao-plano-resultado">{plano.resultado}</p>}

      {plano.transicoesPermitidas.length > 0 && (
        <footer className="cartao-plano-acoes">
          {plano.transicoesPermitidas.map((proximo) => (
            <button
              key={proximo}
              type="button"
              disabled={ocupado}
              onClick={() => aoMover(proximo)}
              style={{ '--tom': corStatusPlano[proximo] } as React.CSSProperties}
            >
              {rotuloStatusPlano[proximo]}
            </button>
          ))}
        </footer>
      )}
    </article>
  );
}
