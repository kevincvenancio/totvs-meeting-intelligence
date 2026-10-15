/* ============================================================================
   Markdown mínimo para as respostas do Hermes.

   Cobre só o que o prompt pede ao modelo: parágrafos, **negrito**, `código`,
   listas ("-" e "1."), tabelas com "|" e links internos de reunião (#123).
   Nada de innerHTML: tudo vira elemento React, então texto do modelo nunca
   é interpretado como HTML.
   ========================================================================== */

import { Fragment, type ReactNode } from 'react';
import { Link } from 'react-router-dom';

type Bloco =
  | { tipo: 'p'; linhas: string[] }
  | { tipo: 'ul' | 'ol'; itens: string[] }
  | { tipo: 'tabela'; cabecalho: string[]; linhas: string[][] };

const ITEM_UL = /^\s*[-*•]\s+(.*)$/;
const ITEM_OL = /^\s*\d+[.)]\s+(.*)$/;
const LINHA_TABELA = /^\s*\|.*\|\s*$/;
const SEPARADOR_TABELA = /^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$/;

function celulas(linha: string) {
  return linha.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map((c) => c.trim());
}

function separarBlocos(texto: string): Bloco[] {
  const linhas = texto.replace(/\r\n?/g, '\n').split('\n');
  const blocos: Bloco[] = [];
  let i = 0;

  while (i < linhas.length) {
    const linha = linhas[i];

    if (!linha.trim()) {
      i++;
      continue;
    }

    // Tabela: linha com |, seguida do separador |---|
    if (LINHA_TABELA.test(linha) && i + 1 < linhas.length && SEPARADOR_TABELA.test(linhas[i + 1])) {
      const cabecalho = celulas(linha);
      const corpo: string[][] = [];
      i += 2;
      while (i < linhas.length && LINHA_TABELA.test(linhas[i])) corpo.push(celulas(linhas[i++]));
      blocos.push({ tipo: 'tabela', cabecalho, linhas: corpo });
      continue;
    }

    const ul = ITEM_UL.exec(linha);
    const ol = ITEM_OL.exec(linha);
    if (ul || ol) {
      const tipo = ul ? 'ul' : 'ol';
      const padrao = ul ? ITEM_UL : ITEM_OL;
      const itens: string[] = [];
      while (i < linhas.length) {
        const m = padrao.exec(linhas[i]);
        if (m) {
          itens.push(m[1]);
        } else if (linhas[i].trim() && /^\s{2,}/.test(linhas[i]) && itens.length) {
          itens[itens.length - 1] += ' ' + linhas[i].trim(); // continuação do item
        } else {
          break;
        }
        i++;
      }
      blocos.push({ tipo, itens });
      continue;
    }

    const paragrafo: string[] = [];
    while (
      i < linhas.length &&
      linhas[i].trim() &&
      !ITEM_UL.test(linhas[i]) &&
      !ITEM_OL.test(linhas[i]) &&
      !LINHA_TABELA.test(linhas[i])
    ) {
      // Títulos (#) não são pedidos ao modelo; se vierem, viram texto em negrito
      paragrafo.push(linhas[i].replace(/^#{1,6}\s+(.*)$/, '**$1**'));
      i++;
    }
    blocos.push({ tipo: 'p', linhas: paragrafo });
  }
  return blocos;
}

/** **negrito**, `código` e "Reunião #123" / "#123" virando link para a reunião. */
const EM_LINHA = /(\*\*[^*]+\*\*|`[^`]+`|(?:Reuni[aã]o\s*)?#\d{1,9}\b)/g;

function emLinha(texto: string, reunioesValidas: Set<number>): ReactNode[] {
  return texto.split(EM_LINHA).map((parte, i) => {
    if (!parte) return null;
    if (parte.startsWith('**') && parte.endsWith('**') && parte.length > 4) {
      return <strong key={i}>{emLinha(parte.slice(2, -2), reunioesValidas)}</strong>;
    }
    if (parte.startsWith('`') && parte.endsWith('`') && parte.length > 2) {
      return <code key={i}>{parte.slice(1, -1)}</code>;
    }
    const id = /#(\d{1,9})$/.exec(parte);
    if (id && reunioesValidas.has(Number(id[1]))) {
      return (
        <Link key={i} to={`/reunioes/${id[1]}`} className="md-id">
          {parte}
        </Link>
      );
    }
    return <Fragment key={i}>{parte}</Fragment>;
  });
}

export function Markdown({ texto, reunioes = [] }: { texto: string; reunioes?: number[] }) {
  const validas = new Set(reunioes);

  return (
    <div className="md">
      {separarBlocos(texto).map((bloco, i) => {
        if (bloco.tipo === 'p') {
          return (
            <p key={i}>
              {bloco.linhas.map((l, j) => (
                <Fragment key={j}>
                  {j > 0 && <br />}
                  {emLinha(l, validas)}
                </Fragment>
              ))}
            </p>
          );
        }
        if (bloco.tipo === 'tabela') {
          return (
            <div key={i} className="md-tabela" role="region" aria-label="Tabela" tabIndex={0}>
              <table>
                <thead>
                  <tr>
                    {bloco.cabecalho.map((c, j) => (
                      <th key={j}>{emLinha(c, validas)}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {bloco.linhas.map((linha, j) => (
                    <tr key={j}>
                      {linha.map((c, k) => (
                        <td key={k}>{emLinha(c, validas)}</td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          );
        }
        const Lista = bloco.tipo;
        return (
          <Lista key={i} className={`md-${bloco.tipo}`}>
            {bloco.itens.map((item, j) => (
              <li key={j}>
                <span>{emLinha(item, validas)}</span>
              </li>
            ))}
          </Lista>
        );
      })}
    </div>
  );
}
