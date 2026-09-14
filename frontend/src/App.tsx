import { Suspense, lazy, useEffect, useState } from 'react';
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { iniciarScrollSuave, ScrollTrigger } from './lib/motion';
import { Navegacao } from './components/chrome/Nav';
import { Cursor } from './components/chrome/Cursor';
import { Grao } from './components/chrome/Grain';
import { Preloader } from './components/chrome/Preloader';
import { Landing } from './routes/Landing';

/* As telas do aplicativo só chegam ao navegador quando alguém entra nelas —
   a home carrega leve, que é onde a primeira impressão acontece. */
const Painel = lazy(() => import('./routes/Painel').then((m) => ({ default: m.Painel })));
const Reunioes = lazy(() => import('./routes/Reunioes').then((m) => ({ default: m.Reunioes })));
const ReuniaoDetalhe = lazy(() =>
  import('./routes/ReuniaoDetalhe').then((m) => ({ default: m.ReuniaoDetalhe })),
);
const Clientes = lazy(() => import('./routes/Clientes').then((m) => ({ default: m.Clientes })));
const PlanosAcao = lazy(() => import('./routes/PlanosAcao').then((m) => ({ default: m.PlanosAcao })));
const Importacoes = lazy(() =>
  import('./routes/Importacoes').then((m) => ({ default: m.Importacoes })),
);
const Entrar = lazy(() => import('./routes/Entrar').then((m) => ({ default: m.Entrar })));

const clienteConsulta = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

/* Ao trocar de rota: topo da página e recálculo dos gatilhos, senão o
   ScrollTrigger continua medindo a altura da tela anterior. */
function AoTrocarDeRota() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
    const id = window.setTimeout(() => ScrollTrigger.refresh(), 120);
    return () => window.clearTimeout(id);
  }, [pathname]);

  return null;
}

function Carregando() {
  return (
    <div className="carregando-rota" role="status">
      <span className="carregando-pulso" aria-hidden="true" />
      <span className="ref">Carregando</span>
    </div>
  );
}

export default function App() {
  const [pronto, setPronto] = useState(false);

  useEffect(() => iniciarScrollSuave() ?? undefined, []);

  return (
    <QueryClientProvider client={clienteConsulta}>
      <BrowserRouter>
        <AoTrocarDeRota />
        <Grao />
        <Cursor />
        {!pronto && <Preloader aoTerminar={() => setPronto(true)} />}

        <a className="skip-link" href="#conteudo">
          Pular para o conteúdo
        </a>

        <Navegacao />

        <Suspense fallback={<Carregando />}>
          <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/entrar" element={<Entrar />} />
            <Route path="/painel" element={<Painel />} />
            <Route path="/reunioes" element={<Reunioes />} />
            <Route path="/reunioes/:id" element={<ReuniaoDetalhe />} />
            <Route path="/clientes" element={<Clientes />} />
            <Route path="/planos-acao" element={<PlanosAcao />} />
            <Route path="/importacoes" element={<Importacoes />} />
            <Route path="*" element={<Landing />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
