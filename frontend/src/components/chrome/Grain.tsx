/* Grão de filme.

   Gera uma vez um bloco de ruído 128×128 num canvas e publica como data-URL
   em `--grain-url`. Um único PNG minúsculo, reaproveitado por toda a página,
   é mais barato que qualquer filtro SVG animado — e é o que costura tudo:
   sem ele os degradês do shader e as superfícies chapadas parecem de
   materiais diferentes. */

import { useEffect } from 'react';

const TAMANHO = 128;

export function Grao() {
  useEffect(() => {
    if (document.documentElement.style.getPropertyValue('--grain-url')) return;

    const canvas = document.createElement('canvas');
    canvas.width = TAMANHO;
    canvas.height = TAMANHO;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const imagem = ctx.createImageData(TAMANHO, TAMANHO);
    const dados = imagem.data;

    for (let i = 0; i < dados.length; i += 4) {
      // Ruído monocromático em alfa baixo: o CSS controla a opacidade final.
      const valor = (Math.random() * 255) | 0;
      dados[i] = valor;
      dados[i + 1] = valor;
      dados[i + 2] = valor;
      dados[i + 3] = 22;
    }

    ctx.putImageData(imagem, 0, 0);
    document.documentElement.style.setProperty('--grain-url', `url(${canvas.toDataURL('image/png')})`);
  }, []);

  return null;
}
