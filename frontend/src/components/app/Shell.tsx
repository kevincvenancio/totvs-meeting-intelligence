/* ============================================================================
   Moldura das telas do aplicativo.

   Barra secundária fixa logo abaixo da navegação do site, título de página e
   o aviso de vitrine — quando a API não responde, o usuário precisa saber que
   está vendo um conjunto de demonstração, não a base dele.
   ========================================================================== */

import { useEffect, useState, type ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { assinarStatusApi } from '../../lib/api';

const ROTAS = [
  { para: '/painel', rotulo: 'Painel' },
  { para: '/reunioes', rotulo: 'Reuniões' },
  { para: '/clientes', rotulo: 'Clientes' },
  { para: '/planos-acao', rotulo: 'Planos de ação' },
  { para: '/importacoes', rotulo: 'Importações' },
];

export function AvisoDeVitrine() {
  const [viva, setViva] = useState<boolean | null>(null);

  useEffect(() => assinarStatusApi(setViva), []);

  if (viva !== false) return null;

  return (
    <div className="aviso-vitrine" role="status">
      <span className="aviso-ponto" aria-hidden="true" />
      <p>
        <strong>Modo demonstração.</strong> A API em <code>localhost:8080</code> não respondeu — os
        dados abaixo são um conjunto de exemplo. Suba o back-end com{' '}
        <code>mvn spring-boot:run</code> para ver a base real.
      </p>
    </div>
  );
}

interface Props {
  titulo: string;
  chapeu: string;
  descricao?: string;
  acoes?: ReactNode;
  children: ReactNode;
  /** Ocupa a largura toda (quadro kanban, tabelas largas). */
  largo?: boolean;
}

export function Tela({ titulo, chapeu, descricao, acoes, children, largo }: Props) {
  useEffect(() => {
    document.title = `${titulo} — Insight360`;
    return () => {
      document.title = 'Insight360 — Meeting Intelligence · TOTVS';
    };
  }, [titulo]);

  return (
    <div className="tela">
      <nav className="tela-barra" aria-label="Seções da plataforma">
        <div className="tela-barra-interior">
          <ul>
            {ROTAS.map((rota) => (
              <li key={rota.para}>
                <NavLink
                  to={rota.para}
                  className={({ isActive }) => (isActive ? 'ativa' : undefined)}
                >
                  {rota.rotulo}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      </nav>

      <main id="conteudo" className={`tela-corpo ${largo ? 'tela-corpo--largo' : ''}`}>
        <header className="tela-cabecalho">
          <div className="tela-titulo-caixa">
            <span className="eyebrow">{chapeu}</span>
            <h1 className="display tela-titulo">{titulo}</h1>
            {descricao && <p className="lead tela-descricao">{descricao}</p>}
          </div>
          {acoes && <div className="tela-acoes">{acoes}</div>}
        </header>

        <AvisoDeVitrine />

        {children}
      </main>
    </div>
  );
}

/* ── Estados de carregamento e vazio ────────────────────────────────────── */

export function Esqueleto({ linhas = 5, altura = 58 }: { linhas?: number; altura?: number }) {
  return (
    <div className="esqueleto" aria-hidden="true">
      {Array.from({ length: linhas }, (_, i) => (
        <span key={i} style={{ height: altura, animationDelay: `${i * 90}ms` }} />
      ))}
    </div>
  );
}

export function Vazio({ titulo, texto, acao }: { titulo: string; texto: string; acao?: ReactNode }) {
  return (
    <div className="vazio">
      <span className="vazio-marca" aria-hidden="true" />
      <h2>{titulo}</h2>
      <p>{texto}</p>
      {acao}
    </div>
  );
}

export function Erro({ mensagem }: { mensagem: string }) {
  return (
    <div className="erro-caixa" role="alert">
      <span className="erro-marca" aria-hidden="true" />
      <p>{mensagem}</p>
    </div>
  );
}

/* ── Selo de estado ─────────────────────────────────────────────────────
   Cor nunca sozinha: o selo sempre carrega o rótulo por escrito.          */

export function Selo({
  texto,
  tom,
  className,
}: {
  texto: string;
  tom: string;
  className?: string;
}) {
  return (
    <span className={`selo ${className ?? ''}`} style={{ '--tom': tom } as React.CSSProperties}>
      <span className="selo-ponto" aria-hidden="true" />
      {texto}
    </span>
  );
}
