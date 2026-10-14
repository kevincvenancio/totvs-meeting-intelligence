import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import './styles/index.css';
import App from './App.tsx';

/* O modo escolhido é aplicado antes da primeira pintura para não haver
   piscada de tema entre o HTML e o React. */
try {
  const salvo = localStorage.getItem('hermes:modo');
  if (salvo === 'light' || salvo === 'dark') document.documentElement.dataset.mode = salvo;
} catch {
  // Sem localStorage, vale o padrão do index.html (obsidiana).
}

/* F5 recomeça do zero: sem isso o navegador devolve a posição de scroll
   antiga depois que o Lenis e o preloader já montaram, e a página abre no
   meio da apresentação. Uma âncora (#conteudo) no endereço também puxaria
   a tela para baixo, então ela sai antes da primeira pintura. */
if ('scrollRestoration' in history) history.scrollRestoration = 'manual';
if (location.hash) history.replaceState(history.state, '', location.pathname + location.search);
window.scrollTo(0, 0);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
