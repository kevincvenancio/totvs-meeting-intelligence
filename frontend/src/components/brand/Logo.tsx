/* Marca do Hermes.

   O símbolo é uma abertura: um anel interrompido exatamente onde o sinal
   entra. O ponto quente no centro é o insight — o que sobrou depois que a
   conversa inteira foi lida. */

interface Props {
  tamanho?: number;
  className?: string;
  /** Anima a abertura ao passar o mouse (usado na navegação). */
  vivo?: boolean;
}

export function Marca({ tamanho = 28, className, vivo = false }: Props) {
  return (
    <svg
      width={tamanho}
      height={tamanho}
      viewBox="0 0 64 64"
      fill="none"
      className={className}
      aria-hidden="true"
      data-vivo={vivo || undefined}
    >
      <defs>
        <linearGradient id="marca-brasa" x1="14" y1="8" x2="52" y2="58" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="var(--color-ember-hot)" />
          <stop offset="1" stopColor="var(--color-ember-deep)" />
        </linearGradient>
      </defs>
      <path
        className="marca-arco"
        d="M32 12a20 20 0 1 1-14.14 5.86"
        stroke="url(#marca-brasa)"
        strokeWidth="4.5"
        strokeLinecap="round"
      />
      <circle cx="32" cy="32" r="11" stroke="currentColor" strokeWidth="2" opacity=".45" />
      <circle className="marca-nucleo" cx="32" cy="32" r="3.5" fill="var(--color-ember-hot)" />
    </svg>
  );
}

export function Logotipo({ tamanho = 26, className }: { tamanho?: number; className?: string }) {
  return (
    <span className={`logotipo ${className ?? ''}`}>
      <Marca tamanho={tamanho} vivo />
      <span className="logotipo-texto">
        Hermes
      </span>
    </span>
  );
}

/* Anéis de órbita do herói: puro SVG, girando em velocidades diferentes.
   Cada anel é uma camada de profundidade distinta no parallax. */
export function Orbitas({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 800 800" className={className} fill="none" aria-hidden="true">
      <defs>
        <radialGradient id="orbita-luz" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0.55" stopColor="var(--color-ember)" stopOpacity="0" />
          <stop offset="0.82" stopColor="var(--color-ember)" stopOpacity="0.5" />
          <stop offset="1" stopColor="var(--color-ember)" stopOpacity="0" />
        </radialGradient>
      </defs>

      <g stroke="currentColor" strokeWidth="1">
        <circle className="orbita orbita--1" cx="400" cy="400" r="150" opacity=".16" />
        <circle
          className="orbita orbita--2"
          cx="400"
          cy="400"
          r="238"
          opacity=".13"
          strokeDasharray="2 9"
        />
        <circle className="orbita orbita--3" cx="400" cy="400" r="326" opacity=".1" />
        <circle
          className="orbita orbita--4"
          cx="400"
          cy="400"
          r="399"
          opacity=".08"
          strokeDasharray="1 14"
        />
      </g>

      {/* Marcadores que percorrem as órbitas — os "sinais" em trânsito */}
      <g className="orbita-marcadores">
        <circle className="sinal sinal--a" cx="400" cy="250" r="3" fill="var(--color-ember-hot)" />
        <circle className="sinal sinal--b" cx="638" cy="400" r="2" fill="var(--color-data-2)" />
        <circle className="sinal sinal--c" cx="400" cy="726" r="2.5" fill="var(--color-ember)" />
      </g>

      <circle cx="400" cy="400" r="400" fill="url(#orbita-luz)" opacity=".35" />
    </svg>
  );
}
