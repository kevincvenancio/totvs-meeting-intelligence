/* ============================================================================
   Modo apresentação.

   A página inicial é o material do pitch: cada bloco do roteiro é uma seção
   de tela cheia, e o passador de slides (que envia setas / Page Up / Page
   Down) avança dentro do bloco antes de ir para o próximo — os itens surgem
   no ritmo da fala, como num deck.

   Dois jeitos de chegar a um bloco, dois comportamentos:
   - pelo teclado/passador, o bloco começa no passo 0 e revela item a item;
   - rolando com o mouse (ou clicando no trilho), o bloco já chega completo,
     porque quem rola está lendo, não apresentando.

   Atalhos: → ↓ espaço PgDn avançam · ← ↑ PgUp voltam · 1–5 pulam para o
   bloco · Home volta ao início · P abre o painel do apresentador · F tela
   cheia.
   ========================================================================== */

import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { ScrollTrigger, irPara } from '../../lib/motion';
import { ROTEIRO } from '../../lib/roteiro';
import { ContextoPassos } from './passos';

const TECLAS_AVANCAR = new Set(['ArrowRight', 'ArrowDown', 'PageDown', ' ']);
const TECLAS_VOLTAR = new Set(['ArrowLeft', 'ArrowUp', 'PageUp']);

function emCampoDeTexto(alvo: EventTarget | null) {
  const el = alvo as HTMLElement | null;
  if (!el) return false;
  return el.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(el.tagName);
}

/** Um bloco "está na tela" quando cobre o centro da viewport. */
function blocoEmVista(id: string) {
  const el = document.getElementById(id);
  if (!el) return false;
  const r = el.getBoundingClientRect();
  return r.top < window.innerHeight * 0.5 && r.bottom > window.innerHeight * 0.5;
}

function segundos(marca: string) {
  const [m, s] = marca.split(':').map(Number);
  return m * 60 + s;
}

const formatarTempo = (total: number) =>
  `${Math.floor(total / 60)}:${String(Math.floor(total % 60)).padStart(2, '0')}`;

export function ProvedorApresentacao({ children }: { children: ReactNode }) {
  const [indice, setIndice] = useState(0);
  const [passos, setPassos] = useState(() => ROTEIRO.map(() => 0));
  const [dentro, setDentro] = useState(true);
  const [painelAberto, setPainelAberto] = useState(false);
  const [apresentando, setApresentando] = useState(false);
  const [inicio, setInicio] = useState<number | null>(null);
  const [agora, setAgora] = useState(() => Date.now());

  /* O teclado lê o estado por referência: o ouvinte é registrado uma vez só. */
  const estado = useRef({ indice, passos });
  useLayoutEffect(() => {
    estado.current = { indice, passos };
  }, [indice, passos]);
  const modo = useRef<'teclado' | 'manual'>('teclado');
  const usouTeclado = useRef(false);

  const completar = useCallback((i: number) => {
    setPassos((atual) =>
      atual[i] >= ROTEIRO[i].passos ? atual : atual.map((p, j) => (j === i ? ROTEIRO[i].passos : p)),
    );
  }, []);

  /* ── Qual bloco está na tela ─────────────────────────────────────────── */
  useEffect(() => {
    const gatilhos = ROTEIRO.map((bloco, i) =>
      ScrollTrigger.create({
        trigger: `#${bloco.id}`,
        start: 'top 50%',
        end: 'bottom 50%',
        onToggle: ({ isActive }) => {
          if (!isActive) return;
          setIndice(i);
          if (modo.current === 'manual') completar(i);
        },
      }),
    );

    const geral = ScrollTrigger.create({
      trigger: '#pitch',
      start: 'top 50%',
      end: 'bottom 50%',
      onToggle: ({ isActive }) => setDentro(isActive),
    });

    return () => {
      gatilhos.forEach((g) => g.kill());
      geral.kill();
    };
  }, [completar]);

  /* ── Rolagem com mouse ou toque: modo leitura ────────────────────────── */
  useEffect(() => {
    function aoRolarManualmente() {
      setApresentando(false);
      if (modo.current === 'manual') return;
      modo.current = 'manual';
      // Quem só está navegando pela página não fica com o herói pela metade.
      if (!usouTeclado.current) completar(estado.current.indice);
    }

    window.addEventListener('wheel', aoRolarManualmente, { passive: true });
    window.addEventListener('touchstart', aoRolarManualmente, { passive: true });
    window.addEventListener('pointerdown', aoRolarManualmente, { passive: true });
    return () => {
      window.removeEventListener('wheel', aoRolarManualmente);
      window.removeEventListener('touchstart', aoRolarManualmente);
      window.removeEventListener('pointerdown', aoRolarManualmente);
    };
  }, [completar]);

  /* ── Teclado e passador ──────────────────────────────────────────────── */
  useEffect(() => {
    function irParaBloco(i: number, passo: number) {
      modo.current = 'teclado';
      setPassos((atual) => atual.map((p, j) => (j === i ? passo : p)));
      setIndice(i);
      irPara(`#${ROTEIRO[i].id}`);
    }

    /* Se a página parou no meio de um bloco, o passo seguinte também o
       reenquadra — a banca nunca vê meia seção. */
    function enquadrar(i: number) {
      const el = document.getElementById(ROTEIRO[i].id);
      if (el && Math.abs(el.getBoundingClientRect().top) > 4) irPara(el);
    }

    function avancar() {
      const { indice: i, passos: p } = estado.current;
      if (p[i] < ROTEIRO[i].passos) {
        modo.current = 'teclado';
        setPassos((atual) => atual.map((v, j) => (j === i ? v + 1 : v)));
        enquadrar(i);
        return true;
      }
      if (i < ROTEIRO.length - 1) {
        irParaBloco(i + 1, 0);
        return true;
      }
      return false;
    }

    function voltar() {
      const { indice: i, passos: p } = estado.current;
      if (p[i] > 0) {
        modo.current = 'teclado';
        setPassos((atual) => atual.map((v, j) => (j === i ? v - 1 : v)));
        enquadrar(i);
        return true;
      }
      if (i > 0) {
        irParaBloco(i - 1, ROTEIRO[i - 1].passos);
        return true;
      }
      return false;
    }

    function aoTeclar(evento: KeyboardEvent) {
      if (evento.ctrlKey || evento.metaKey || evento.altKey || emCampoDeTexto(evento.target)) return;

      const tecla = evento.key;

      if (tecla === 'p' || tecla === 'P') {
        setPainelAberto((aberto) => !aberto);
        return;
      }
      if (tecla === 'f' || tecla === 'F') {
        if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
        else document.documentElement.requestFullscreen().catch(() => {});
        return;
      }

      const numero = Number(tecla);
      if (Number.isInteger(numero) && numero >= 1 && numero <= ROTEIRO.length) {
        evento.preventDefault();
        usouTeclado.current = true;
        setApresentando(true);
        irParaBloco(numero - 1, 0);
        return;
      }
      if (tecla === 'Home') {
        evento.preventDefault();
        usouTeclado.current = true;
        setApresentando(true);
        irParaBloco(0, 0);
        return;
      }

      // Fora dos blocos (no rodapé, por exemplo) as setas voltam a rolar a página.
      if (!blocoEmVista(ROTEIRO[estado.current.indice].id)) return;

      const tratado = TECLAS_AVANCAR.has(tecla) ? avancar() : TECLAS_VOLTAR.has(tecla) ? voltar() : null;
      if (tratado === null) return;
      if (tratado) {
        evento.preventDefault();
        usouTeclado.current = true;
        setApresentando(true);
        setInicio((atual) => atual ?? Date.now());
      }
    }

    window.addEventListener('keydown', aoTeclar);
    return () => window.removeEventListener('keydown', aoTeclar);
  }, []);

  /* ── Navegação do topo some enquanto se apresenta pelo teclado ───────── */
  useEffect(() => {
    const raiz = document.documentElement;
    if (apresentando && dentro) raiz.dataset.apresentando = 'true';
    else delete raiz.dataset.apresentando;
    return () => {
      delete raiz.dataset.apresentando;
    };
  }, [apresentando, dentro]);

  /* ── Cronômetro do ensaio ────────────────────────────────────────────── */
  useEffect(() => {
    if (!painelAberto) return;
    const id = window.setInterval(() => setAgora(Date.now()), 500);
    return () => window.clearInterval(id);
  }, [painelAberto]);

  const valor = useMemo(() => ({ passos }), [passos]);
  const bloco = ROTEIRO[indice];
  const decorrido = inicio ? (agora - inicio) / 1000 : 0;
  const fimDoBloco = segundos(bloco.tempo.split('–')[1]);
  const atrasado = inicio !== null && decorrido > fimDoBloco;

  return (
    <ContextoPassos.Provider value={valor}>
      {children}

      <div className="pitch-hud" data-visivel={dentro || undefined} aria-hidden={!dentro}>
        <div className="pitch-hud-linha">
          <span className="pitch-hud-ref numeric">
            {bloco.ref} / {String(ROTEIRO.length).padStart(2, '0')}
          </span>
          <span className="pitch-hud-rotulo">{bloco.rotulo}</span>
          <span className="pitch-hud-passos" aria-hidden="true">
            {Array.from({ length: bloco.passos }, (_, k) => (
              <span key={k} data-feito={passos[indice] > k || undefined} />
            ))}
          </span>
        </div>

        {painelAberto && (
          <div className="pitch-hud-apresentador">
            <span>{bloco.apresentador}</span>
            <span className="numeric">{bloco.tempo}</span>
            <span className="numeric pitch-hud-cronometro" data-atrasado={atrasado || undefined}>
              {inicio ? formatarTempo(decorrido) : 'cronômetro inicia no 1º avanço'}
            </span>
            <span className="pitch-hud-atalhos">→ avança · ← volta · 1–5 bloco · F tela cheia · P fecha</span>
          </div>
        )}
      </div>
    </ContextoPassos.Provider>
  );
}
