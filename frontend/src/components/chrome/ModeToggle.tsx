/* Alternador obsidiana ⇄ papel.

   O modo claro não é o escuro invertido: os passos de cor de dado são outros,
   validados contra a superfície de papel (ver tokens.css). A escolha fica no
   localStorage; sem escolha, o site abre em obsidiana — que é o modo em que a
   identidade foi desenhada. */

import { useEffect, useState } from 'react';

const CHAVE = 'insight360:modo';
type Modo = 'dark' | 'light';

function modoInicial(): Modo {
  if (typeof window === 'undefined') return 'dark';
  const salvo = localStorage.getItem(CHAVE);
  return salvo === 'light' || salvo === 'dark' ? salvo : 'dark';
}

export function AlternadorDeModo() {
  const [modo, setModo] = useState<Modo>(modoInicial);

  useEffect(() => {
    document.documentElement.dataset.mode = modo;
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute('content', modo === 'dark' ? '#08090b' : '#f4f1ea');
    try {
      localStorage.setItem(CHAVE, modo);
    } catch {
      // Navegação privada pode recusar a escrita; o modo da sessão continua valendo.
    }
  }, [modo]);

  return (
    <button
      type="button"
      className="alternador"
      onClick={() => setModo((m) => (m === 'dark' ? 'light' : 'dark'))}
      aria-label={modo === 'dark' ? 'Mudar para o modo claro' : 'Mudar para o modo escuro'}
      title={modo === 'dark' ? 'Modo claro' : 'Modo escuro'}
    >
      <span className="alternador-corpo" data-modo={modo}>
        <span className="alternador-disco" />
      </span>
    </button>
  );
}
