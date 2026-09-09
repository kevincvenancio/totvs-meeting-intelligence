import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import './styles/index.css';
import App from './App.tsx';

/* O modo escolhido é aplicado antes da primeira pintura para não haver
   piscada de tema entre o HTML e o React. */
try {
  const salvo = localStorage.getItem('insight360:modo');
  if (salvo === 'light' || salvo === 'dark') document.documentElement.dataset.mode = salvo;
} catch {
  // Sem localStorage, vale o padrão do index.html (obsidiana).
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
