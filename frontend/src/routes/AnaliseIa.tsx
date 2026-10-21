/* Análise IA — POST /api/v1/ia/analises.

   Recebe uma transcrição nova e devolve, sem importar nada, o que interessa à
   área comercial: Transcrição → Processamento → Modelo → Indicadores →
   Recomendação, com os modelos do Hugging Face rodando no serviço Python. */

import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';

import { ia } from '../lib/api';
import type { ModoIa } from '../lib/types';
import { Erro, Selo, Tela } from '../components/app/Shell';
import { PainelAnaliseIa } from '../components/ia/PainelAnaliseIa';
import { FilaIa } from '../components/ia/FilaIa';

const EXEMPLO = `[LOCUTOR 1]: Oi, Fernanda, obrigado por receber a gente. Como estão as coisas com a folha?
[LOCUTOR 2]: Péssimas, para ser honesta. Em outubro a folha saiu com erro para trezentos funcionários, tivemos que pagar diferença na mão. O jurídico já está olhando as cláusulas de saída e meu diretor pediu para eu levantar quanto custaria a multa para encerrar o relacionamento com vocês.
[LOCUTOR 1]: Nossa, eu não sabia que tinha chegado nesse ponto.
[LOCUTOR 2]: Chegou. A gente já recebeu outras duas apresentações de empresas de folha e o pessoal gostou. Se até o fim do mês não tiver uma solução definitiva, a gente vai embora.
[LOCUTOR 1]: Entendido. Vou envolver o time de produto e o Customer Success agora.`;

const MODOS: { valor: ModoIa; rotulo: string }[] = [
  { valor: 'hibrido', rotulo: 'Híbrido (modelo + regras)' },
  { valor: 'modelo', rotulo: 'Só modelo (Hugging Face)' },
  { valor: 'heuristico', rotulo: 'Só regras (baseline)' },
];

export function AnaliseIa() {
  const [texto, setTexto] = useState('');
  const [modo, setModo] = useState<ModoIa>('hibrido');

  const status = useQuery({ queryKey: ['ia-status'], queryFn: ia.status, staleTime: 15_000 });
  const analisar = useMutation({ mutationFn: () => ia.analisar(texto, modo) });

  const palavras = texto.trim() ? texto.trim().split(/\s+/).length : 0;
  const pronta = status.data?.habilitada && status.data.online;

  return (
    <Tela
      chapeu="Inteligência artificial"
      titulo="Análise IA"
      descricao="Cole a transcrição de uma reunião nova. Os modelos do Hugging Face identificam produto, sentimento, risco de churn e oportunidade, e o Hermes recomenda a próxima ação."
      largo
      acoes={
        status.data && (
          <Selo
            texto={pronta ? `IA online · ${status.data.modo}` : status.data.habilitada ? 'IA fora do ar' : 'IA desligada'}
            tom={pronta ? 'var(--color-state-good)' : 'var(--color-state-critical)'}
          />
        )
      }
    >
      {status.data && !pronta && (
        <div className="aviso-vitrine aviso-vitrine--alerta" role="status">
          <span className="aviso-ponto" aria-hidden="true" />
          <p>
            <strong>{status.data.habilitada ? 'O serviço de IA não respondeu.' : 'A IA não está configurada.'}</strong>{' '}
            Suba o serviço Python com <code>uvicorn api:app --port 8000</code> na pasta{' '}
            <code>ciencia-de-dados</code> e defina <code>HERMES_IA_URL=http://localhost:8000</code> no back-end.
          </p>
        </div>
      )}

      <section className="ia-entrada">
        <label className="ia-campo">
          <span className="eyebrow">Transcrição</span>
          <textarea
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            rows={12}
            placeholder={'[LOCUTOR 1]: Bom dia...\n[LOCUTOR 2]: ...'}
          />
        </label>
        <div className="ia-controles">
          <label className="filtro">
            <span>Modo</span>
            <select value={modo} onChange={(e) => setModo(e.target.value as ModoIa)}>
              {MODOS.map((m) => (
                <option key={m.valor} value={m.valor}>
                  {m.rotulo}
                </option>
              ))}
            </select>
          </label>
          <span className="ref ia-contador">{palavras} palavras</span>
          <button type="button" className="botao botao--fantasma" onClick={() => setTexto(EXEMPLO)}>
            Usar exemplo
          </button>
          <button
            type="button"
            className="botao botao--acento"
            disabled={palavras < 5 || analisar.isPending}
            onClick={() => analisar.mutate()}
          >
            {analisar.isPending ? 'Analisando…' : 'Analisar'}
          </button>
        </div>
        {analisar.isPending && (
          <p className="ref">Rodando os modelos — em CPU leva alguns segundos (na primeira vez, mais).</p>
        )}
      </section>

      {analisar.error && <Erro mensagem={(analisar.error as Error).message} />}
      {analisar.data && <PainelAnaliseIa analise={analisar.data} />}

      <FilaIa />
    </Tela>
  );
}
