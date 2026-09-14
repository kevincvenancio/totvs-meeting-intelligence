/* Navegação do site.

   Some ao descer e volta ao subir — o gesto de subir é sempre um gesto de
   "quero sair daqui", então é aí que a navegação precisa existir. Fora do
   topo ela ganha vidro e filete; no topo fica transparente sobre o herói. */

import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { gsap, irPara } from '../../lib/motion';
import { Logotipo } from '../brand/Logo';
import { AlternadorDeModo } from './ModeToggle';

const SECOES = [
  { rotulo: 'Método', alvo: '#escuta' },
  { rotulo: 'Plataforma', alvo: '#pilares' },
  { rotulo: 'Sinais', alvo: '#risco' },
  { rotulo: 'Números', alvo: '#numeros' },
];

export function Navegacao() {
  const barra = useRef<HTMLElement>(null);
  const [aberto, setAberto] = useState(false);
  const { pathname } = useLocation();
  const naHome = pathname === '/';

  useEffect(() => {
    const elemento = barra.current;
    if (!elemento) return;

    let ultimo = window.scrollY;
    let escondido = false;

    function aoRolar() {
      const atual = window.scrollY;
      const descendo = atual > ultimo;
      const passouDoTopo = atual > 90;

      elemento?.setAttribute('data-fixa', String(passouDoTopo));

      // Menu aberto nunca se esconde: seria perder o botão de fechar.
      if (!aberto) {
        if (descendo && passouDoTopo && !escondido) {
          escondido = true;
          gsap.to(elemento, { yPercent: -130, duration: 0.5, ease: 'power3.inOut' });
        } else if (!descendo && escondido) {
          escondido = false;
          gsap.to(elemento, { yPercent: 0, duration: 0.55, ease: 'power3.out' });
        }
      }

      ultimo = atual;
    }

    window.addEventListener('scroll', aoRolar, { passive: true });
    aoRolar();
    return () => window.removeEventListener('scroll', aoRolar);
  }, [aberto]);

  function navegarPara(alvo: string) {
    setAberto(false);
    irPara(alvo, -40);
  }

  return (
    <header ref={barra} className="nav" data-aberto={aberto || undefined}>
      <div className="nav-interior">
        <Link to="/" className="nav-marca" aria-label="Hermes — início">
          <Logotipo />
        </Link>

        <nav className="nav-secoes" aria-label="Seções">
          {naHome &&
            SECOES.map((secao) => (
              <button key={secao.alvo} type="button" onClick={() => navegarPara(secao.alvo)}>
                {secao.rotulo}
              </button>
            ))}
        </nav>

        <div className="nav-acoes">
          <AlternadorDeModo />
          <Link to="/painel" className="botao botao--acento" data-cursor="Entrar">
            Abrir painel
          </Link>
          <button
            type="button"
            className="nav-hamburguer"
            aria-expanded={aberto}
            aria-label={aberto ? 'Fechar menu' : 'Abrir menu'}
            onClick={() => setAberto((v) => !v)}
          >
            <span />
            <span />
          </button>
        </div>
      </div>

      {/* Painel móvel — a mesma navegação, empilhada */}
      <div className="nav-painel" hidden={!aberto}>
        <ul>
          {naHome &&
            SECOES.map((secao) => (
              <li key={secao.alvo}>
                <button type="button" onClick={() => navegarPara(secao.alvo)}>
                  {secao.rotulo}
                </button>
              </li>
            ))}
          <li>
            <Link to="/painel" onClick={() => setAberto(false)}>
              Painel executivo
            </Link>
          </li>
          <li>
            <Link to="/reunioes" onClick={() => setAberto(false)}>
              Reuniões
            </Link>
          </li>
          <li>
            <Link to="/entrar" onClick={() => setAberto(false)}>
              Entrar
            </Link>
          </li>
        </ul>
      </div>
    </header>
  );
}
