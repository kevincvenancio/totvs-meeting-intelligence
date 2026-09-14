/* Trilho de índice.

   Fica na borda direita e faz duas coisas ao mesmo tempo: mostra onde a
   leitura está (o filete que preenche) e serve de sumário clicável. As
   etiquetas só aparecem inteiras quando o ponteiro chega perto — o resto do
   tempo são traços, para não competir com o conteúdo. */

import { useEffect, useState } from 'react';
import { ScrollTrigger, irPara } from '../../lib/motion';

export interface Marco {
  id: string;
  rotulo: string;
  ref: string;
}

export function TrilhoDeIndice({ marcos }: { marcos: Marco[] }) {
  const [ativo, setAtivo] = useState(0);
  const [progresso, setProgresso] = useState(0);

  useEffect(() => {
    const gatilhos = marcos.map((marco, indice) =>
      ScrollTrigger.create({
        trigger: `#${marco.id}`,
        start: 'top 55%',
        end: 'bottom 55%',
        onToggle: ({ isActive }) => isActive && setAtivo(indice),
      }),
    );

    const geral = ScrollTrigger.create({
      trigger: document.body,
      start: 'top top',
      end: 'bottom bottom',
      onUpdate: ({ progress }) => setProgresso(progress),
    });

    return () => {
      gatilhos.forEach((g) => g.kill());
      geral.kill();
    };
  }, [marcos]);

  return (
    <nav className="trilho" aria-label="Índice da página">
      <span className="trilho-linha" aria-hidden="true">
        <span className="trilho-preenchimento" style={{ transform: `scaleY(${progresso})` }} />
      </span>

      <ol>
        {marcos.map((marco, indice) => (
          <li key={marco.id}>
            <button
              type="button"
              data-ativo={indice === ativo || undefined}
              onClick={() => irPara(`#${marco.id}`)}
              aria-current={indice === ativo ? 'true' : undefined}
            >
              <span className="trilho-ref numeric">{marco.ref}</span>
              <span className="trilho-rotulo">{marco.rotulo}</span>
              <span className="trilho-traco" aria-hidden="true" />
            </button>
          </li>
        ))}
      </ol>
    </nav>
  );
}
