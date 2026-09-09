/* ============================================================================
   Espelho TypeScript dos DTOs de resposta da API (br.com.totvs.insight360.dto).
   Mantido manualmente e alinhado a /v3/api-docs.
   ========================================================================== */

export type StatusCompletude = 'COMPLETA' | 'PARCIAL' | 'INCOMPLETA';

export type Sentimento =
  | 'POSITIVO'
  | 'NEGATIVO'
  | 'CRITICO'
  | 'MISTO'
  | 'OPORTUNIDADE_COMERCIAL'
  | 'NEUTRO';

export type RiscoChurn = 'ALTO' | 'MEDIO' | 'BAIXO';
export type NivelCriticidade = 'ALTA' | 'MEDIA' | 'BAIXA';
export type PerfilUsuario = 'ADMIN' | 'GESTOR' | 'VENDEDOR' | 'ANALISTA';
export type StatusCliente = 'PROSPECT' | 'ATIVO' | 'EM_RISCO' | 'INATIVO';
export type StatusPlanoAcao = 'PENDENTE' | 'EM_ANDAMENTO' | 'CONCLUIDO' | 'CANCELADO';
export type PrioridadePlanoAcao = 'ALTA' | 'MEDIA' | 'BAIXA';

export interface PaginaResposta<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
  primeira: boolean;
  ultima: boolean;
}

export interface UsuarioResumo {
  id: number;
  nome: string;
  email: string;
  perfil: PerfilUsuario;
}

export interface Usuario extends UsuarioResumo {
  perfilDescricao: string;
  cargo?: string;
  ativo: boolean;
  dataCadastro?: string;
  dataAtualizacao?: string;
}

export interface ClienteResumo {
  id: number;
  nomeExibicao: string;
  cnpjFormatado?: string;
  status: StatusCliente;
}

export interface Cliente extends ClienteResumo {
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  segmento?: string;
  uf?: string;
  cidade?: string;
  faixaFaturamento?: string;
  notaNps?: number;
  statusDescricao: string;
  contatoPrincipal?: string;
  emailContato?: string;
  telefoneContato?: string;
  observacoes?: string;
  totalReunioes?: number;
  dataCadastro?: string;
  dataAtualizacao?: string;
}

export interface ReuniaoResumo {
  id: number;
  idExterno?: string;
  data: string;
  cliente?: string;
  vendedor?: string;
  categoriaPrincipal?: string;
  statusCompletude?: StatusCompletude;
  sentimento?: Sentimento;
  sentimentoFormatado?: string;
  riscoChurn?: RiscoChurn;
  riscoChurnLabel?: string;
  prioridade?: string;
  scoreComercial?: number;
  scoreQualidade?: number;
  duracao?: string;
  analisada: boolean;
  duplicada: boolean;
  codigoLote?: string;
  clienteVinculado?: ClienteResumo;
}

export interface ReuniaoDetalhe extends ReuniaoResumo {
  formato?: string;
  duracaoMinutos?: number;
  uf?: string;
  cnae?: string;
  nomeUnidade?: string;
  segmento?: string;
  faturamento?: string;
  notaNps?: number;
  pontuacaoCompletude?: number;
  motivoIncompletude?: string;
  insightParcial?: string;
  sentimentoJustificativa?: string;
  categoriasPrincipais?: string;
  temaReuniao?: string;
  resumoReuniao?: string;
  pontosPrincipais?: string;
  doresIdentificadas?: string;
  oportunidades?: string;
  recomendacaoFinal?: string;
  produtosIdentificados?: string;
  concorrentesIdentificados?: string;
  personasIdentificadas?: string;
  empresasIdentificadas?: string;
  areasInternas?: string;
  budgetIdentificado?: string;
  locutoresIdentificados?: string;
  quantidadePalavras?: number;
  quantidadeLocutores?: number;
  transcricaoTratada?: string;
  motivoDuplicidade?: string;
  idReuniaoOriginalDuplicada?: number;
  nomeArquivoOrigem?: string;
  dataImportacao?: string;
  totalInsights: number;
  totalComentarios: number;
  totalPlanosAcao: number;
}

export interface Insight {
  id: number;
  reuniaoId: number;
  tipo?: string;
  descricao: string;
  prioridade?: string;
  trechoOrigem?: string;
  confianca?: number;
}

export interface Feedback {
  id: number;
  reuniaoId: number;
  problemaIdentificado?: string;
  categoriaProblema?: string;
  motivoNaoIdentificadoAntes?: string;
  sinaisNaConversa?: string;
  comoIdentificarAntes?: string;
  perguntasRecomendadas?: string;
  acaoDeMelhoria?: string;
  nivelCriticidade?: NivelCriticidade;
  mensagemEducativa?: string;
}

export interface ReuniaoReferencia {
  id: number;
  idExterno?: string;
  cliente?: string;
  data?: string;
}

export interface PlanoAcao {
  id: number;
  titulo: string;
  descricao?: string;
  prioridade: PrioridadePlanoAcao;
  prioridadeDescricao: string;
  status: StatusPlanoAcao;
  statusDescricao: string;
  prazo?: string;
  atrasado: boolean;
  diasRestantes?: number;
  resultado?: string;
  responsavel?: UsuarioResumo;
  reuniao?: ReuniaoReferencia;
  transicoesPermitidas: StatusPlanoAcao[];
  dataCriacao?: string;
  dataAtualizacao?: string;
  dataConclusao?: string;
}

export interface Comentario {
  id: number;
  reuniaoId: number;
  texto: string;
  editado: boolean;
  autor?: UsuarioResumo;
  dataCriacao?: string;
  dataAtualizacao?: string;
}

export interface ImportacaoLote {
  id: number;
  codigoLote: string;
  nomeArquivoOriginal?: string;
  dataHoraImportacao?: string;
  totalRegistrosBrutos: number;
  totalReunioesValidas: number;
  totalReunioesIncompletas: number;
  totalReunioesDuplicadas: number;
  totalReunioesComErro: number;
  statusProcessamento?: string;
  observacoes?: string;
}

export interface ResultadoImportacao {
  lote: ImportacaoLote;
  totalEncontradas: number;
  processadas: number;
  incompletas: number;
  duplicadas: number;
  erros: number;
  errosDetalhe?: string[];
}

export interface IndicadoresPlanoAcao {
  totalPorStatus: Record<string, number>;
  total: number;
  emAberto: number;
  atrasados: number;
}

export interface IndicadoresCliente {
  totalPorStatus: Record<string, number>;
  total: number;
  emRisco: number;
}

export interface Dashboard {
  totalReunioes: number;
  totalClientesCitados: number;
  totalChurnAlto: number;
  totalChurnMedio: number;
  totalOportunidades: number;
  scoreQualidadeMedia: string;
  scoreComercialMedia: string;
  sentimentos: Record<string, number>;
  sentimentoPredominante?: string;
  topProdutos: Record<string, number>;
  topConcorrentes: Record<string, number>;
  topCategorias: Record<string, number>;
  topOportunidades: ReuniaoResumo[];
  topRiscoChurn: ReuniaoResumo[];
  planosAcao: IndicadoresPlanoAcao;
  clientes: IndicadoresCliente;
}

/** Corpo de erro padronizado do ManipuladorGlobalExcecoes. */
export interface ErroResposta {
  instante?: string;
  status?: number;
  erro?: string;
  mensagem?: string;
  caminho?: string;
  campos?: { campo: string; mensagem: string }[];
}
