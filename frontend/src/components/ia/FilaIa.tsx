/* Andamento da análise da base pelo modelo (GET/POST /api/v1/ia/fila).

   A importação grava as reuniões na hora e o modelo analisa em segundo plano;
   este cartão mostra quanto falta e deixa mandar a base inteira para a IA. */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ia } from '../../lib/api';
import { n } from '../../lib/format';
import { Erro, Selo } from '../app/Shell';

const ROTULO = {
  DESLIGADA: ['Modelo não configurado', 'var(--color-state-critical)'],
  PARADA: ['Todas analisadas', 'var(--color-state-good)'],
  PROCESSANDO: ['Modelo analisando', 'var(--accent)'],
  AGUARDANDO_IA: ['Aguardando a IA voltar', 'var(--color-state-critical)'],
} as const;

export function FilaIa() {
  const cliente = useQueryClient();
  const fila = useQuery({
    queryKey: ['ia-fila'],
    queryFn: ia.fila,
    // Atualiza sozinho enquanto há reuniões na fila
    refetchInterval: (q) => (q.state.data && q.state.data.pendentes > 0 ? 3000 : false),
  });
  const enviar = useMutation({
    mutationFn: (todas: boolean) => ia.analisarBase(todas),
    onSuccess: (dados) => cliente.setQueryData(['ia-fila'], dados),
  });

  const f = fila.data;
  if (!f) return null;
  const total = f.pendentes + f.processadas;
  const pct = total > 0 ? Math.round((f.processadas / total) * 100) : 0;
  const [rotulo, tom] = ROTULO[f.estado];

  return (
    <section className="ia-entrada ia-fila" aria-live="polite">
      <header className="ia-fila-topo">
        <div>
          <span className="eyebrow">Base de reuniões</span>
          <h2 className="bloco-titulo">Análise da base pelo modelo</h2>
        </div>
        <Selo texto={rotulo} tom={tom} />
      </header>

      {f.habilitada ? (
        <>
          <p className="ia-nota">
            Toda reunião importada é analisada pelo modelo do Hugging Face. A importação termina na
            hora e o modelo trabalha em segundo plano, em lotes, sem travar o sistema.
          </p>
          {total > 0 && (
            <div className="ia-fila-progresso">
              <span className="ia-medidor" aria-hidden="true">
                <span style={{ width: `${pct}%`, background: 'var(--accent)' }} />
              </span>
              <span className="ref">
                {n(f.processadas)} analisadas · {n(f.pendentes)} na fila
                {f.falhas > 0 && ` · ${n(f.falhas)} falhas`}
                {f.segundosPorReuniao != null && ` · ${f.segundosPorReuniao.toFixed(2)} s por reunião`}
                {f.minutosRestantes != null && ` · faltam ~${f.minutosRestantes} min`}
              </span>
            </div>
          )}
          {f.ultimoErro && <p className="ref">Último erro: {f.ultimoErro}</p>}
          <div className="ia-controles">
            <button
              type="button"
              className="botao botao--acento"
              disabled={enviar.isPending}
              onClick={() => enviar.mutate(false)}
            >
              Analisar com o modelo as que faltam
            </button>
            <button
              type="button"
              className="botao botao--contorno"
              disabled={enviar.isPending}
              onClick={() => enviar.mutate(true)}
            >
              Reanalisar todas com o modelo
            </button>
          </div>
        </>
      ) : (
        <p className="ia-nota">
          O serviço do modelo não está configurado. Suba-o e defina <code>HERMES_IA_URL</code> no
          back-end para o modelo analisar as reuniões.
        </p>
      )}
      {enviar.error && <Erro mensagem={(enviar.error as Error).message} />}
    </section>
  );
}
