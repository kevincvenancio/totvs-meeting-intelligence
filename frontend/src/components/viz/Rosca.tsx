/* Rosca de composição.

   Usada só onde a pergunta é mesmo "de que isto é feito" e as fatias são
   poucas. Cada arco tem 2px de folga contra o fundo — sem esse respiro dois
   tons vizinhos viram uma mancha só. O valor dominante ocupa o miolo, porque
   é a resposta que quase todo mundo quer e ninguém deveria ter que calcular
   passando o mouse. */

import { useState } from 'react';
import { Balao, Grafico, Legenda, type ItemLegenda } from './primitives';

export interface Fatia {
  rotulo: string;
  valor: number;
  cor: string;
}

interface Props {
  titulo: string;
  descricao?: string;
  nota?: string;
  fatias: Fatia[];
  /** Texto do miolo. Sem ele, mostra o total. */
  miolo?: { valor: string; rotulo: string };
  tamanho?: number;
  className?: string;
}

const TAU = Math.PI * 2;

export function Rosca({
  titulo,
  descricao,
  nota,
  fatias,
  miolo,
  tamanho = 210,
  className,
}: Props) {
  const [destacado, setDestacado] = useState<string | null>(null);
  const [balao, setBalao] = useState({ x: 0, y: 0, texto: '', visivel: false });

  const total = fatias.reduce((soma, f) => soma + f.valor, 0) || 1;
  const centro = tamanho / 2;
  const raio = centro - 12;
  const espessura = 26;

  /* Um arco por fatia, desenhado como path para poder ter ponta arredondada
     e folga — stroke-dasharray num círculo não dá controle sobre as duas.
     O ângulo de partida de cada fatia é a soma das anteriores: calculado por
     varredura acumulada, sem acumulador mutável durante a renderização. */
  const raioMedio = raio - espessura / 2;

  const arcos = fatias.map((fatia, indice) => {
    const proporcao = fatia.valor / total;
    const varredura = proporcao * TAU;
    const anguloAtual =
      -Math.PI / 2 +
      fatias.slice(0, indice).reduce((soma, anterior) => soma + (anterior.valor / total) * TAU, 0);

    // 2px de folga convertidos em ângulo, limitados para fatias mínimas.
    const folga = Math.min(varredura * 0.28, 2.4 / raio);
    const inicio = anguloAtual + folga / 2;
    const fim = anguloAtual + varredura - folga / 2;

    const x1 = centro + Math.cos(inicio) * raioMedio;
    const y1 = centro + Math.sin(inicio) * raioMedio;
    const x2 = centro + Math.cos(fim) * raioMedio;
    const y2 = centro + Math.sin(fim) * raioMedio;
    const maior = fim - inicio > Math.PI ? 1 : 0;

    return {
      ...fatia,
      proporcao,
      d: `M ${x1} ${y1} A ${raioMedio} ${raioMedio} 0 ${maior} 1 ${x2} ${y2}`,
      // Ponto médio do arco: onde o balão aparece.
      meioX: centro + Math.cos((inicio + fim) / 2) * raioMedio,
      meioY: centro + Math.sin((inicio + fim) / 2) * raioMedio,
    };
  });

  const dominante = [...fatias].sort((a, b) => b.valor - a.valor)[0];

  const legenda: ItemLegenda[] = fatias.map((f) => ({
    rotulo: f.rotulo,
    cor: f.cor,
    valor: `${Math.round((f.valor / total) * 100)}%`,
  }));

  return (
    <Grafico
      titulo={titulo}
      descricao={descricao}
      nota={nota}
      className={className}
      tabela={{
        colunas: ['Categoria', 'Reuniões', 'Participação'],
        linhas: fatias.map((f) => [
          f.rotulo,
          f.valor.toLocaleString('pt-BR'),
          `${((f.valor / total) * 100).toFixed(1)}%`,
        ]),
      }}
    >
      <div className="rosca">
        <div className="rosca-desenho">
          <svg
            viewBox={`0 0 ${tamanho} ${tamanho}`}
            width={tamanho}
            height={tamanho}
            role="img"
            aria-label={`${titulo}. ${fatias
              .map((f) => `${f.rotulo}: ${Math.round((f.valor / total) * 100)}%`)
              .join('. ')}.`}
          >
            {arcos.map((arco) => (
              <path
                key={arco.rotulo}
                d={arco.d}
                stroke={arco.cor}
                strokeWidth={espessura}
                strokeLinecap="butt"
                fill="none"
                className="rosca-arco"
                data-apagado={destacado && destacado !== arco.rotulo ? 'true' : undefined}
                onPointerEnter={() =>
                  setBalao({
                    x: arco.meioX,
                    y: arco.meioY,
                    texto: `${arco.rotulo} · ${arco.valor.toLocaleString('pt-BR')} (${Math.round(
                      arco.proporcao * 100,
                    )}%)`,
                    visivel: true,
                  })
                }
                onPointerLeave={() => setBalao((b) => ({ ...b, visivel: false }))}
              />
            ))}
          </svg>

          <div className="rosca-miolo">
            <span className="rosca-miolo-valor numeric">
              {miolo?.valor ?? `${Math.round((dominante.valor / total) * 100)}%`}
            </span>
            <span className="rosca-miolo-rotulo">{miolo?.rotulo ?? dominante.rotulo}</span>
          </div>

          <Balao x={balao.x} y={balao.y} visivel={balao.visivel}>
            {balao.texto}
          </Balao>
        </div>

        <Legenda itens={legenda} destacado={destacado} aoDestacar={setDestacado} />
      </div>
    </Grafico>
  );
}
