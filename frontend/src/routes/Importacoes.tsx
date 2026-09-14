/* Importações — POST /api/v1/importacoes (multipart) e listagem de lotes.

   A área de soltar arquivo é o único componente do sistema que aceita algo
   de fora, então ela valida antes de mandar: extensão e tamanho, com a mesma
   regra do back-end (.csv, 50 MB). Assim o erro chega em milissegundos em vez
   de depois do upload. */

import { useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { importacoes, relatorios } from '../lib/api';
import { dth, n } from '../lib/format';
import { Erro, Esqueleto, Tela, Vazio } from '../components/app/Shell';

const LIMITE_BYTES = 50 * 1024 * 1024;

export function Importacoes() {
  const [sobre, setSobre] = useState(false);
  const [recusa, setRecusa] = useState<string | null>(null);
  const entrada = useRef<HTMLInputElement>(null);
  const clienteConsulta = useQueryClient();

  const lotes = useQuery({ queryKey: ['importacoes'], queryFn: importacoes.listar });

  const enviar = useMutation({
    mutationFn: importacoes.enviar,
    onSuccess: () => clienteConsulta.invalidateQueries({ queryKey: ['importacoes'] }),
  });

  function validarEEnviar(arquivo?: File | null) {
    setRecusa(null);
    if (!arquivo) return;

    if (!arquivo.name.toLowerCase().endsWith('.csv')) {
      setRecusa('O arquivo precisa ter extensão .csv.');
      return;
    }
    if (arquivo.size === 0) {
      setRecusa('O arquivo está vazio.');
      return;
    }
    if (arquivo.size > LIMITE_BYTES) {
      setRecusa(`O limite é 50 MB — este tem ${(arquivo.size / 1024 / 1024).toFixed(1)} MB.`);
      return;
    }

    enviar.mutate(arquivo);
  }

  return (
    <Tela
      chapeu="Entrada de dados"
      titulo="Importações"
      descricao="Envie o CSV de transcrições. O lote registra brutos, válidos, incompletos, duplicados e erros."
    >
      <div
        className="soltar"
        data-sobre={sobre || undefined}
        data-ocupado={enviar.isPending || undefined}
        onDragOver={(evento) => {
          evento.preventDefault();
          setSobre(true);
        }}
        onDragLeave={() => setSobre(false)}
        onDrop={(evento) => {
          evento.preventDefault();
          setSobre(false);
          validarEEnviar(evento.dataTransfer.files?.[0]);
        }}
      >
        <input
          ref={entrada}
          type="file"
          accept=".csv,text/csv"
          className="sr-only"
          onChange={(evento) => validarEEnviar(evento.target.files?.[0])}
        />

        <span className="soltar-marca" aria-hidden="true">
          <svg viewBox="0 0 48 48" fill="none">
            <path d="M24 34V12m0 0-8 8m8-8 8 8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            <path d="M8 32v4a4 4 0 0 0 4 4h24a4 4 0 0 0 4-4v-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
        </span>

        <p className="soltar-titulo">
          {enviar.isPending ? 'Processando o lote…' : 'Arraste o CSV até aqui'}
        </p>
        <p className="soltar-apoio">
          Máximo 50 MB · extensão <code>.csv</code> · duplicidade detectada dentro do lote e contra
          os anteriores
        </p>

        <button
          type="button"
          className="botao botao--acento"
          disabled={enviar.isPending}
          onClick={() => entrada.current?.click()}
        >
          {enviar.isPending ? 'Enviando…' : 'Escolher arquivo'}
        </button>

        {enviar.isPending && <span className="soltar-barra" aria-hidden="true" />}
      </div>

      {recusa && <Erro mensagem={recusa} />}
      {enviar.error && <Erro mensagem={(enviar.error as Error).message} />}

      {enviar.data && (
        <section className="resultado" aria-live="polite">
          <h2 className="resultado-titulo">
            Lote <span className="numeric">{enviar.data.lote.codigoLote}</span> processado
          </h2>
          <ul className="resultado-numeros">
            {[
              ['Encontradas', enviar.data.totalEncontradas, 'var(--text-primary)'],
              ['Processadas', enviar.data.processadas, 'var(--color-state-good)'],
              ['Incompletas', enviar.data.incompletas, 'var(--color-state-warn)'],
              ['Duplicadas', enviar.data.duplicadas, 'var(--color-state-idle)'],
              ['Erros', enviar.data.erros, 'var(--color-state-critical)'],
            ].map(([rotulo, valor, tom]) => (
              <li key={String(rotulo)} style={{ '--tom': tom } as React.CSSProperties}>
                <span className="ref">{rotulo}</span>
                <span className="numeric">{n(Number(valor))}</span>
              </li>
            ))}
          </ul>

          {enviar.data.errosDetalhe && enviar.data.errosDetalhe.length > 0 && (
            <details className="resultado-erros">
              <summary>{enviar.data.errosDetalhe.length} linhas com erro</summary>
              <ul>
                {enviar.data.errosDetalhe.slice(0, 30).map((erro, i) => (
                  <li key={i}>{erro}</li>
                ))}
              </ul>
            </details>
          )}
        </section>
      )}

      <h2 className="secao-titulo">Lotes importados</h2>

      {lotes.isLoading && <Esqueleto linhas={3} altura={92} />}
      {lotes.error && <Erro mensagem={(lotes.error as Error).message} />}

      {lotes.data && lotes.data.length === 0 && (
        <Vazio titulo="Nenhum lote ainda" texto="Envie o primeiro CSV de transcrições acima." />
      )}

      {lotes.data && lotes.data.length > 0 && (
        <ul className="lotes">
          {lotes.data.map((lote) => {
            const barras = [
              { rotulo: 'Válidas', valor: lote.totalReunioesValidas, cor: 'var(--color-state-good)' },
              { rotulo: 'Incompletas', valor: lote.totalReunioesIncompletas, cor: 'var(--color-state-warn)' },
              { rotulo: 'Duplicadas', valor: lote.totalReunioesDuplicadas, cor: 'var(--color-state-idle)' },
              { rotulo: 'Erros', valor: lote.totalReunioesComErro, cor: 'var(--color-state-critical)' },
            ];
            const total = barras.reduce((soma, b) => soma + b.valor, 0) || 1;

            return (
              <li key={lote.id}>
                <article className="lote">
                  <header className="lote-topo">
                    <div>
                      <h3 className="lote-codigo numeric">{lote.codigoLote}</h3>
                      <p className="lote-arquivo ref">
                        {lote.nomeArquivoOriginal} · {dth(lote.dataHoraImportacao)}
                      </p>
                    </div>
                    <a
                      href={relatorios.lote(lote.id)}
                      className="botao botao--contorno"
                      target="_blank"
                      rel="noreferrer"
                    >
                      PDF do lote
                    </a>
                  </header>

                  <div
                    className="lote-barra"
                    role="img"
                    aria-label={barras.map((b) => `${b.rotulo}: ${b.valor}`).join('. ')}
                  >
                    {barras.map((barra) => (
                      <span
                        key={barra.rotulo}
                        style={{ width: `${(barra.valor / total) * 100}%`, background: barra.cor }}
                      />
                    ))}
                  </div>

                  <ul className="lote-legenda">
                    {barras.map((barra) => (
                      <li key={barra.rotulo}>
                        <span className="viz-marca" style={{ background: barra.cor }} aria-hidden="true" />
                        {barra.rotulo}
                        <span className="numeric">{n(barra.valor)}</span>
                      </li>
                    ))}
                  </ul>

                  {lote.observacoes && <p className="lote-observacoes">{lote.observacoes}</p>}
                </article>
              </li>
            );
          })}
        </ul>
      )}
    </Tela>
  );
}
