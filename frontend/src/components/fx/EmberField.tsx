/* ============================================================================
   Campo de brasa — WebGL puro, sem three.js.

   Um único quad com um fragment shader: ruído fBm em duas escalas, deformado
   por um campo de fluxo lento, colorido pela rampa da marca. É o fundo do
   herói e a camada mais profunda do parallax.

   Escolhas de custo:
   · resolução interna limitada a 1× (nunca 2× em telas retina) — o ruído é
     suave, ninguém percebe, e o custo cai pela metade;
   · o laço para quando o elemento sai da viewport;
   · com "movimento reduzido" desenha um quadro e congela;
   · sem WebGL o CSS por baixo já entrega um degradê aceitável.
   ========================================================================== */

import { useEffect, useRef } from 'react';
import { movimentoReduzido } from '../../lib/motion';

const VERTEX = `#version 300 es
in vec2 posicao;
void main() { gl_Position = vec4(posicao, 0.0, 1.0); }`;

const FRAGMENT = `#version 300 es
precision highp float;

uniform vec2  uResolucao;
uniform float uTempo;
uniform vec2  uPonteiro;
uniform float uIntensidade;

out vec4 corSaida;

/* Ruído por gradiente (Perlin simplificado em 2D) */
vec2 hash(vec2 p) {
  p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
  return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float ruido(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  vec2 u = f * f * (3.0 - 2.0 * f);
  return mix(
    mix(dot(hash(i + vec2(0.0, 0.0)), f - vec2(0.0, 0.0)),
        dot(hash(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0)), u.x),
    mix(dot(hash(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0)),
        dot(hash(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0)), u.x), u.y);
}

/* Soma de oitavas — cinco bastam para a escala em que isso é visto */
float fbm(vec2 p) {
  float valor = 0.0;
  float amplitude = 0.5;
  mat2 rotacao = mat2(0.8, 0.6, -0.6, 0.8);
  for (int i = 0; i < 5; i++) {
    valor += amplitude * ruido(p);
    p = rotacao * p * 2.02;
    amplitude *= 0.5;
  }
  return valor;
}

void main() {
  vec2 uv = gl_FragCoord.xy / uResolucao.xy;
  vec2 p = (gl_FragCoord.xy * 2.0 - uResolucao.xy) / min(uResolucao.x, uResolucao.y);

  float t = uTempo * 0.055;

  /* Deformação de domínio: o ruído alimenta a coordenada do próprio ruído.
     É o que dá o aspecto de fumaça em vez de manchas estáticas. */
  vec2 q = vec2(fbm(p + vec2(0.0, t)), fbm(p + vec2(5.2, 1.3 - t)));
  vec2 r = vec2(
    fbm(p + 3.5 * q + vec2(1.7, 9.2) + 0.15 * t),
    fbm(p + 3.5 * q + vec2(8.3, 2.8) - 0.12 * t)
  );
  float f = fbm(p + 3.0 * r);

  /* O ponteiro do mouse aquece a região onde está, com queda suave */
  float calorPonteiro = 1.0 - smoothstep(0.0, 1.05, length(p - uPonteiro * 1.15));

  /* Rampa da marca: obsidiana -> vinho -> brasa -> laranja quente */
  vec3 obsidiana = vec3(0.031, 0.035, 0.043);
  vec3 vinho     = vec3(0.117, 0.071, 0.047);
  vec3 brasa     = vec3(0.851, 0.384, 0.173);
  vec3 quente    = vec3(1.000, 0.478, 0.239);

  float intensidade = clamp(f * 0.72 + 0.42, 0.0, 1.0);
  intensidade = pow(intensidade, 1.75);
  intensidade += calorPonteiro * 0.16;
  intensidade *= uIntensidade;

  vec3 cor = mix(obsidiana, vinho, smoothstep(0.05, 0.42, intensidade));
  cor = mix(cor, brasa, smoothstep(0.40, 0.78, intensidade) * 0.72);
  cor = mix(cor, quente, smoothstep(0.74, 1.0, intensidade) * 0.55);

  /* Vinheta: o centro respira, as bordas voltam para a obsidiana */
  float vinheta = 1.0 - smoothstep(0.30, 1.25, length(p));
  cor = mix(obsidiana, cor, vinheta * 0.94 + 0.06);

  /* Dithering: mata o banding nos degradês largos, custo de uma linha */
  float ruidoTela = fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453);
  cor += (ruidoTela - 0.5) / 255.0;

  corSaida = vec4(cor, 1.0);
}`;

function compilar(gl: WebGL2RenderingContext, tipo: number, fonte: string) {
  const shader = gl.createShader(tipo)!;
  gl.shaderSource(shader, fonte);
  gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    console.warn('[EmberField]', gl.getShaderInfoLog(shader));
    gl.deleteShader(shader);
    return null;
  }
  return shader;
}

interface Props {
  className?: string;
  /** 0 a 1 — o herói baixa isso conforme a página rola. */
  intensidade?: number;
}

export function CampoDeBrasa({ className, intensidade = 1 }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const intensidadeRef = useRef(intensidade);

  /* A intensidade muda a cada quadro de scroll; guardá-la num ref evita
     recriar o contexto WebGL a cada mudança. A escrita fica num efeito
     próprio — escrever ref durante a renderização não é seguro no modo
     concorrente. */
  useEffect(() => {
    intensidadeRef.current = intensidade;
  }, [intensidade]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const gl = canvas.getContext('webgl2', {
      antialias: false,
      alpha: false,
      powerPreference: 'low-power',
      preserveDrawingBuffer: false,
    });
    // Sem WebGL2 o degradê CSS por baixo continua valendo.
    if (!gl) return;

    const vs = compilar(gl, gl.VERTEX_SHADER, VERTEX);
    const fs = compilar(gl, gl.FRAGMENT_SHADER, FRAGMENT);
    if (!vs || !fs) return;

    const programa = gl.createProgram()!;
    gl.attachShader(programa, vs);
    gl.attachShader(programa, fs);
    gl.linkProgram(programa);
    if (!gl.getProgramParameter(programa, gl.LINK_STATUS)) return;
    gl.useProgram(programa);

    const buffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
    const posicao = gl.getAttribLocation(programa, 'posicao');
    gl.enableVertexAttribArray(posicao);
    gl.vertexAttribPointer(posicao, 2, gl.FLOAT, false, 0, 0);

    const uResolucao = gl.getUniformLocation(programa, 'uResolucao');
    const uTempo = gl.getUniformLocation(programa, 'uTempo');
    const uPonteiro = gl.getUniformLocation(programa, 'uPonteiro');
    const uIntensidade = gl.getUniformLocation(programa, 'uIntensidade');

    const ponteiro = { x: 0, y: 0 };
    const alvo = { x: 0, y: 0 };

    function dimensionar() {
      if (!canvas || !gl) return;
      const { clientWidth: l, clientHeight: a } = canvas;
      // 1× de propósito: ruído suave não ganha nada com devicePixelRatio.
      const largura = Math.max(1, Math.floor(l));
      const altura = Math.max(1, Math.floor(a));
      if (canvas.width === largura && canvas.height === altura) return;
      canvas.width = largura;
      canvas.height = altura;
      gl.viewport(0, 0, largura, altura);
      gl.uniform2f(uResolucao, largura, altura);
    }

    function aoMover(evento: PointerEvent) {
      if (!canvas) return;
      const caixa = canvas.getBoundingClientRect();
      alvo.x = ((evento.clientX - caixa.left) / caixa.width) * 2 - 1;
      alvo.y = 1 - ((evento.clientY - caixa.top) / caixa.height) * 2;
    }

    const reduzido = movimentoReduzido();
    let visivel = true;
    let quadro = 0;
    let inicio = performance.now();

    const observador = new IntersectionObserver(
      ([entrada]) => {
        visivel = entrada.isIntersecting;
        if (visivel && !reduzido && !quadro) {
          // Retoma sem saltar no tempo do shader.
          inicio = performance.now() - decorrido;
          quadro = requestAnimationFrame(desenhar);
        }
      },
      { threshold: 0 },
    );
    observador.observe(canvas);

    let decorrido = 0;

    function desenhar(agora: number) {
      if (!gl) return;
      decorrido = agora - inicio;

      ponteiro.x += (alvo.x - ponteiro.x) * 0.045;
      ponteiro.y += (alvo.y - ponteiro.y) * 0.045;

      dimensionar();
      gl.uniform1f(uTempo, decorrido / 1000);
      gl.uniform2f(uPonteiro, ponteiro.x, ponteiro.y);
      gl.uniform1f(uIntensidade, intensidadeRef.current);
      gl.drawArrays(gl.TRIANGLES, 0, 3);

      quadro = visivel ? requestAnimationFrame(desenhar) : 0;
    }

    dimensionar();

    if (reduzido) {
      // Um quadro representativo e para: o visual continua, o movimento não.
      gl.uniform1f(uTempo, 12);
      gl.uniform2f(uPonteiro, 0, 0);
      gl.uniform1f(uIntensidade, intensidadeRef.current);
      gl.drawArrays(gl.TRIANGLES, 0, 3);
    } else {
      window.addEventListener('pointermove', aoMover, { passive: true });
      quadro = requestAnimationFrame(desenhar);
    }

    const aoRedimensionar = () => dimensionar();
    window.addEventListener('resize', aoRedimensionar);

    return () => {
      cancelAnimationFrame(quadro);
      observador.disconnect();
      window.removeEventListener('pointermove', aoMover);
      window.removeEventListener('resize', aoRedimensionar);
      gl.deleteProgram(programa);
      gl.deleteShader(vs);
      gl.deleteShader(fs);
      gl.deleteBuffer(buffer);
    };
  }, []);

  return <canvas ref={canvasRef} className={className} aria-hidden="true" />;
}
