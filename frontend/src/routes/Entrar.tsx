/* Entrada — POST /api/v1/autenticacao/login.

   A API valida e-mail e senha em BCrypt e devolve o usuário; não há token,
   e por isso o front guarda apenas a identificação em memória de sessão para
   preencher autoria de comentário e plano. Nenhuma senha é persistida.

   Duas metades: à esquerda o formulário, à direita o campo de brasa. É o único
   lugar do aplicativo que herda o clima da home, porque é a fronteira entre o
   site e a ferramenta. */

import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { autenticacao, ApiError } from '../lib/api';
import { CampoDeBrasa } from '../components/fx/EmberField';
import { Marca } from '../components/brand/Logo';

const CHAVE_SESSAO = 'hermes:usuario';

export function Entrar() {
  const navegar = useNavigate();
  const [email, setEmail] = useState('admin@hermes.com.br');
  const [senha, setSenha] = useState('admin123');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault();
    setErro(null);
    setEnviando(true);

    try {
      const usuario = await autenticacao.login(email, senha);
      try {
        sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(usuario));
      } catch {
        // Sem sessionStorage a navegação continua; só a autoria fica anônima.
      }
      navegar('/painel');
    } catch (falha) {
      setErro(
        falha instanceof ApiError
          ? falha.message
          : 'Não foi possível entrar. Tente novamente.',
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main id="conteudo" className="entrar">
      <div className="entrar-formulario">
        <div className="entrar-interior">
          <Link to="/" className="entrar-marca">
            <Marca tamanho={30} />
            <span>Voltar ao site</span>
          </Link>

          <header className="entrar-cabecalho">
            <span className="ref">REF: HMS — ACESSO</span>
            <h1 className="display entrar-titulo">Entrar</h1>
            <p className="lead">
              A API não usa token: o login valida as credenciais em BCrypt e devolve o usuário
              autenticado.
            </p>
          </header>

          <form onSubmit={aoEnviar} className="formulario">
            <label className="campo">
              <span>E-mail</span>
              <input
                type="email"
                autoComplete="username"
                required
                value={email}
                onChange={(evento) => setEmail(evento.target.value)}
              />
            </label>

            <label className="campo">
              <span>Senha</span>
              <input
                type="password"
                autoComplete="current-password"
                required
                minLength={6}
                value={senha}
                onChange={(evento) => setSenha(evento.target.value)}
              />
            </label>

            {erro && (
              <p className="campo-erro" role="alert">
                {erro}
              </p>
            )}

            <button type="submit" className="botao botao--acento botao--grande" disabled={enviando}>
              {enviando ? 'Verificando…' : 'Entrar no painel'}
            </button>
          </form>

          <p className="entrar-dica">
            Credencial padrão criada no primeiro start:{' '}
            <code>admin@hermes.com.br</code> / <code>admin123</code>. Troque em{' '}
            <code>PATCH /api/v1/usuarios/{'{id}'}/senha</code>.
          </p>
        </div>
      </div>

      <aside className="entrar-arte" aria-hidden="true">
        <CampoDeBrasa className="entrar-brasa" />
        <blockquote className="entrar-citacao display">
          Toda reunião já contém a <em>resposta</em>.
        </blockquote>
      </aside>
    </main>
  );
}
