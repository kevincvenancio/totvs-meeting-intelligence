/* ============================================================================
   Peças compartilhadas dos gráficos.

   Regras que valem para todos e por isso moram aqui:
   · o texto usa tokens de texto, nunca a cor da série — quem carrega a
     identidade é a marca colorida ao lado do rótulo;
   · duas séries ou mais sempre têm legenda; até quatro também são rotuladas
     direto no desenho, para que a identidade nunca dependa só da cor;
   · todo gráfico tem uma tabela equivalente, aberta por um botão.
   ========================================================================== */

import { useId, useState, type ReactNode } from 'react';

/* ── Moldura ────────────────────────────────────────────────────────────── */

interface MolduraProps {
  titulo: string;
  descricao?: string;
  /** Rodapé: fonte do dado, recorte, unidade. */
  nota?: string;
  children: ReactNode;
  /** Linhas da tabela equivalente: [rótulo, valor formatado]. */
  tabela?: { colunas: string[]; linhas: (string | number)[][] };
  className?: string;
  acao?: ReactNode;
}

export function Grafico({
  titulo,
  descricao,
  nota,
  children,
  tabela,
  className,
  acao,
}: MolduraProps) {
  const [mostrandoTabela, setMostrandoTabela] = useState(false);
  const id = useId();

  return (
    <figure className={`viz ${className ?? ''}`}>
      <figcaption className="viz-cabecalho">
        <div>
          <h3 className="viz-titulo">{titulo}</h3>
          {descricao && <p className="viz-descricao">{descricao}</p>}
        </div>
        <div className="viz-acoes">
          {acao}
          {tabela && (
            <button
              type="button"
              className="viz-alternar"
              aria-expanded={mostrandoTabela}
              aria-controls={id}
              onClick={() => setMostrandoTabela((v) => !v)}
            >
              {mostrandoTabela ? 'Gráfico' : 'Tabela'}
            </button>
          )}
        </div>
      </figcaption>

      <div className="viz-corpo">
        {mostrandoTabela && tabela ? (
          <div className="viz-tabela-caixa" id={id}>
            <table className="viz-tabela">
              <thead>
                <tr>
                  {tabela.colunas.map((coluna) => (
                    <th key={coluna} scope="col">
                      {coluna}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {tabela.linhas.map((linha, i) => (
                  <tr key={i}>
                    {linha.map((celula, j) => (
                      <td key={j} className={j > 0 ? 'numeric' : undefined}>
                        {celula}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          children
        )}
      </div>

      {nota && <p className="viz-nota">{nota}</p>}
    </figure>
  );
}

/* ── Legenda ────────────────────────────────────────────────────────────── */

export interface ItemLegenda {
  rotulo: string;
  cor: string;
  valor?: string;
}

export function Legenda({
  itens,
  aoDestacar,
  destacado,
}: {
  itens: ItemLegenda[];
  aoDestacar?: (rotulo: string | null) => void;
  destacado?: string | null;
}) {
  return (
    <ul className="viz-legenda">
      {itens.map((item) => (
        <li
          key={item.rotulo}
          data-apagado={destacado && destacado !== item.rotulo ? 'true' : undefined}
          onPointerEnter={() => aoDestacar?.(item.rotulo)}
          onPointerLeave={() => aoDestacar?.(null)}
        >
          <span className="viz-marca" style={{ background: item.cor }} aria-hidden="true" />
          <span className="viz-legenda-rotulo">{item.rotulo}</span>
          {item.valor && <span className="viz-legenda-valor numeric">{item.valor}</span>}
        </li>
      ))}
    </ul>
  );
}

/* ── Balão de contexto ──────────────────────────────────────────────────── */

export function Balao({
  x,
  y,
  children,
  visivel,
}: {
  x: number;
  y: number;
  children: ReactNode;
  visivel: boolean;
}) {
  return (
    <div
      className="viz-balao"
      role="tooltip"
      data-visivel={visivel || undefined}
      style={{ left: x, top: y }}
    >
      {children}
    </div>
  );
}

/* ── Número herói ───────────────────────────────────────────────────────
   Quando a resposta é um valor só, um gráfico é ruído. Isto é o gráfico.  */

export function Painel({
  valor,
  rotulo,
  apoio,
  tom,
  className,
}: {
  valor: ReactNode;
  rotulo: string;
  apoio?: ReactNode;
  /** Cor de estado — sempre acompanhada do rótulo, nunca sozinha. */
  tom?: string;
  className?: string;
}) {
  return (
    <div className={`painel-valor ${className ?? ''}`} style={{ '--tom': tom } as React.CSSProperties}>
      <span className="painel-rotulo">{rotulo}</span>
      <span className="painel-numero numeric">{valor}</span>
      {apoio && <span className="painel-apoio">{apoio}</span>}
    </div>
  );
}
