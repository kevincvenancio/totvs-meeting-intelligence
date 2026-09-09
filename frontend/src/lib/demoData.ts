/* ============================================================================
   Conjunto de demonstração.

   Usado quando a API Spring não está no ar (o backend depende do Oracle da
   FIAP). Os números são coerentes entre si — o painel soma o que as listas
   mostram — para que a vitrine nunca se contradiga. Tudo é determinístico:
   um gerador congruente linear com semente fixa, sem Math.random, para que
   duas cargas da página mostrem exatamente a mesma base.
   ========================================================================== */

import type {
  Cliente,
  Comentario,
  Dashboard,
  Feedback,
  ImportacaoLote,
  Insight,
  PaginaResposta,
  PlanoAcao,
  ReuniaoDetalhe,
  ReuniaoResumo,
  RiscoChurn,
  Sentimento,
  StatusCliente,
  StatusCompletude,
  StatusPlanoAcao,
  Usuario,
} from './types';

/* ── Gerador determinístico ─────────────────────────────────────────────── */

function semente(inicial: number) {
  let estado = inicial;
  return () => {
    estado = (estado * 1664525 + 1013904223) % 4294967296;
    return estado / 4294967296;
  };
}

const aleatorio = semente(360360);
const entre = (min: number, max: number) => min + Math.floor(aleatorio() * (max - min + 1));

/* ── Vocabulário do domínio ─────────────────────────────────────────────── */

const RAZOES = [
  'Metalúrgica Aurora', 'Distribuidora Vale Norte', 'Frigorífico Pampa Sul',
  'Têxtil Mirassol', 'Agro Cerrado Participações', 'Construtora Ponte Nova',
  'Rede Farma Vida', 'Laticínios Serra Azul', 'Logística Trilho Certo',
  'Indústria Química Bandeirante', 'Calçados Passo Firme', 'Móveis Araucária',
  'Usina Canavieira Boa Vista', 'Rede Supermercados Lumiar', 'Autopeças Diesel Master',
  'Cooperativa Vale do Café', 'Editora Papel & Tinta', 'Hospital Santa Clara',
  'Plásticos Polinorte', 'Bebidas Fonte Clara', 'Transportadora Rota 40',
  'Cimento Rocha Firme', 'Eletro Center Brasil', 'Papelaria Escriba',
] as const;

const VENDEDORES = [
  'Marina Coutinho', 'Rafael Bastos', 'Juliana Prado', 'Thiago Menezes',
  'Camila Rezende', 'Anderson Lopes', 'Patrícia Nogueira', 'Eduardo Vasques',
] as const;

const PRODUTOS = [
  'Protheus', 'TOTVS RM', 'Datasul', 'Fluig', 'TOTVS Techfin',
  'TOTVS Consignado', 'Winthor', 'TOTVS Assinatura Eletrônica', 'Carol / Analytics',
  'TOTVS Cloud', 'Gestão de Estoque', 'Folha de Pagamento',
] as const;

const CONCORRENTES = [
  'SAP', 'Oracle', 'Sankhya', 'Senior', 'Linx', 'Omie', 'Bling', 'Microsiga legado',
] as const;

const CATEGORIAS = [
  'Renovação contratual', 'Upsell de módulos', 'Suporte crítico', 'Implantação',
  'Descoberta comercial', 'Revisão de faturamento', 'Migração de versão',
  'Treinamento de equipe',
] as const;

const SEGMENTOS = [
  'Manufatura', 'Distribuição', 'Varejo', 'Agronegócio', 'Serviços',
  'Saúde', 'Construção', 'Logística',
] as const;

const UFS = ['SP', 'MG', 'PR', 'RS', 'SC', 'BA', 'GO', 'PE', 'CE', 'RJ'] as const;

const FAIXAS = [
  'Até R$ 10 mi', 'R$ 10 mi a R$ 50 mi', 'R$ 50 mi a R$ 200 mi', 'Acima de R$ 200 mi',
] as const;

const SENTIMENTOS: readonly Sentimento[] = [
  'POSITIVO', 'NEGATIVO', 'CRITICO', 'MISTO', 'OPORTUNIDADE_COMERCIAL', 'NEUTRO',
];

const ROTULO_SENTIMENTO: Record<Sentimento, string> = {
  POSITIVO: 'Positivo',
  NEGATIVO: 'Negativo',
  CRITICO: 'Crítico',
  MISTO: 'Misto',
  OPORTUNIDADE_COMERCIAL: 'Oportunidade comercial',
  NEUTRO: 'Neutro',
};

const ROTULO_RISCO: Record<RiscoChurn, string> = {
  ALTO: 'Alto',
  MEDIO: 'Médio',
  BAIXO: 'Baixo',
};

const DORES = [
  'Fechamento contábil leva 11 dias úteis e trava o time fiscal.',
  'Integração com o WMS do parceiro quebra a cada atualização de versão.',
  'Relatórios gerenciais são montados fora do sistema, em planilha.',
  'Três chamados críticos reabertos no último trimestre sem RCA.',
  'Custo por usuário subiu acima do reajuste previsto em contrato.',
  'Equipe nova não recebeu treinamento no módulo fiscal.',
  'Divergência de estoque entre a filial e o centro de distribuição.',
  'Aprovação de compras depende de e-mail fora do fluxo.',
];

const OPORTUNIDADES = [
  'Cliente citou expansão para duas filiais no segundo semestre.',
  'Abertura para discutir o módulo de Techfin no próximo ciclo.',
  'Diretoria pediu comparativo de TCO contra a solução atual.',
  'Interesse explícito em automação de assinatura eletrônica.',
  'Projeto de migração para nuvem aprovado no orçamento.',
  'Demanda por painel analítico consolidado entre unidades.',
];

const TEMAS = [
  'Revisão do escopo de implantação e cronograma de virada',
  'Alinhamento sobre chamados críticos em aberto',
  'Apresentação do módulo fiscal e próximos passos',
  'Negociação de renovação e reajuste contratual',
  'Diagnóstico de performance no fechamento mensal',
  'Descoberta: mapeamento de processos da controladoria',
];

/* ── Clientes ───────────────────────────────────────────────────────────── */

function digitosCnpj(indice: number) {
  const base = String(10000000 + indice * 137).padStart(8, '0');
  return `${base.slice(0, 2)}.${base.slice(2, 5)}.${base.slice(5, 8)}/0001-${String(
    (indice * 17) % 90 + 10,
  )}`;
}

const STATUS_CLIENTE: readonly StatusCliente[] = ['ATIVO', 'ATIVO', 'ATIVO', 'EM_RISCO', 'PROSPECT', 'INATIVO'];

const DESCRICAO_STATUS: Record<StatusCliente, string> = {
  PROSPECT: 'Prospect',
  ATIVO: 'Ativo',
  EM_RISCO: 'Em risco',
  INATIVO: 'Inativo',
};

export const listaClientes: Cliente[] = RAZOES.map((razao, i) => {
  const status = STATUS_CLIENTE[i % STATUS_CLIENTE.length];
  return {
    id: i + 1,
    razaoSocial: `${razao} LTDA`,
    nomeFantasia: razao,
    nomeExibicao: razao,
    cnpj: digitosCnpj(i + 1).replace(/\D/g, ''),
    cnpjFormatado: digitosCnpj(i + 1),
    segmento: SEGMENTOS[i % SEGMENTOS.length],
    uf: UFS[i % UFS.length],
    cidade: ['Campinas', 'Uberlândia', 'Curitiba', 'Caxias do Sul', 'Joinville', 'Feira de Santana'][i % 6],
    faixaFaturamento: FAIXAS[i % FAIXAS.length],
    notaNps: Number((4 + aleatorio() * 6).toFixed(1)),
    status,
    statusDescricao: DESCRICAO_STATUS[status],
    contatoPrincipal: ['Sandra Vieira', 'Marcelo Antunes', 'Leila Fontes', 'Otávio Bittencourt'][i % 4],
    emailContato: `contato@${razao.toLowerCase().replace(/[^a-z]/g, '')}.com.br`,
    telefoneContato: `(${11 + (i % 80)}) 9${String(80000000 + i * 4321).slice(0, 8)}`,
    totalReunioes: entre(3, 28),
    dataCadastro: new Date(2023, i % 12, 1 + (i % 27)).toISOString(),
  };
});

/* ── Reuniões ───────────────────────────────────────────────────────────── */

const TOTAL_REUNIOES = 48;

function riscoDe(sentimento: Sentimento): RiscoChurn {
  if (sentimento === 'CRITICO') return 'ALTO';
  if (sentimento === 'NEGATIVO') return aleatorio() > 0.4 ? 'ALTO' : 'MEDIO';
  if (sentimento === 'MISTO') return 'MEDIO';
  return aleatorio() > 0.85 ? 'MEDIO' : 'BAIXO';
}

function completudeDe(pontos: number): StatusCompletude {
  if (pontos >= 75) return 'COMPLETA';
  if (pontos >= 45) return 'PARCIAL';
  return 'INCOMPLETA';
}

export const listaReunioes: ReuniaoResumo[] = Array.from({ length: TOTAL_REUNIOES }, (_, i) => {
  const cliente = listaClientes[i % listaClientes.length];
  const sentimento = SENTIMENTOS[Math.floor(aleatorio() * SENTIMENTOS.length)];
  const risco = riscoDe(sentimento);
  const pontuacao = entre(28, 98);
  const data = new Date(2025, 8 - Math.floor(i / 8), 28 - (i % 27));

  return {
    id: i + 1,
    idExterno: `MTG-2025-${String(1000 + i * 7)}`,
    data: data.toISOString().slice(0, 10),
    cliente: cliente.nomeExibicao,
    vendedor: VENDEDORES[i % VENDEDORES.length],
    categoriaPrincipal: CATEGORIAS[i % CATEGORIAS.length],
    statusCompletude: completudeDe(pontuacao),
    sentimento,
    sentimentoFormatado: ROTULO_SENTIMENTO[sentimento],
    riscoChurn: risco,
    riscoChurnLabel: ROTULO_RISCO[risco],
    prioridade: risco === 'ALTO' ? 'Alta' : risco === 'MEDIO' ? 'Média' : 'Baixa',
    scoreComercial: entre(18, 97),
    scoreQualidade: pontuacao,
    duracao: `${entre(22, 78)} min`,
    analisada: true,
    duplicada: i % 17 === 16,
    codigoLote: 'LOTE-2025-0001',
    clienteVinculado: {
      id: cliente.id,
      nomeExibicao: cliente.nomeExibicao,
      cnpjFormatado: cliente.cnpjFormatado,
      status: cliente.status,
    },
  };
});

/* ── Transcrição de vitrine (usada também na seção de scroll da home) ───── */

export const TRANSCRICAO_VITRINE: { locutor: string; fala: string; marca?: string }[] = [
  { locutor: 'Vendedor', fala: 'Obrigado pelo tempo. Queria entender como foi o fechamento de agosto.' },
  {
    locutor: 'Cliente',
    fala: 'Olha, foi difícil. O time fiscal virou dois fins de semana pra fechar.',
    marca: 'dor',
  },
  { locutor: 'Vendedor', fala: 'Dois fins de semana. Isso já vinha acontecendo antes?' },
  {
    locutor: 'Cliente',
    fala: 'Desde a virada de versão. E a diretoria começou a perguntar quanto isso custa.',
    marca: 'risco',
  },
  { locutor: 'Vendedor', fala: 'Entendi. E o chamado que vocês abriram, teve retorno?' },
  {
    locutor: 'Cliente',
    fala: 'Foi reaberto três vezes. Sinceramente, a gente pediu proposta de outro fornecedor.',
    marca: 'churn',
  },
  { locutor: 'Vendedor', fala: 'Certo. Vamos tratar isso como crítico.' },
  {
    locutor: 'Cliente',
    fala: 'Se resolver o fechamento, a gente até topa olhar o módulo de Techfin no ano que vem.',
    marca: 'oportunidade',
  },
];

export const INSIGHTS_VITRINE = [
  {
    tipo: 'DOR',
    rotulo: 'Dor identificada',
    descricao: 'Fechamento fiscal exige trabalho em fim de semana desde a virada de versão.',
    confianca: 94,
    slot: 6,
  },
  {
    tipo: 'RISCO',
    rotulo: 'Sinal de risco',
    descricao: 'Diretoria questiona o custo — a decisão subiu de nível na hierarquia.',
    confianca: 88,
    slot: 4,
  },
  {
    tipo: 'CHURN',
    rotulo: 'Risco de churn: alto',
    descricao: 'Proposta concorrente solicitada. Chamado crítico reaberto três vezes.',
    confianca: 97,
    slot: 6,
  },
  {
    tipo: 'OPORTUNIDADE',
    rotulo: 'Oportunidade',
    descricao: 'Abertura declarada para Techfin condicionada à resolução do fechamento.',
    confianca: 81,
    slot: 5,
  },
];

/* ── Painel ─────────────────────────────────────────────────────────────── */

function contar<T extends string>(lista: readonly T[]): Record<string, number> {
  return lista.reduce<Record<string, number>>((mapa, chave) => {
    mapa[chave] = (mapa[chave] ?? 0) + 1;
    return mapa;
  }, {});
}

const sentimentos = contar(listaReunioes.map((r) => r.sentimento!).filter(Boolean));

const topProdutos = Object.fromEntries(
  PRODUTOS.slice(0, 7)
    .map((p, i) => [p, 340 - i * 41 - (i % 3) * 7] as const)
    .sort((a, b) => b[1] - a[1]),
);

const topConcorrentes = Object.fromEntries(
  CONCORRENTES.slice(0, 6).map((c, i) => [c, 128 - i * 19 - (i % 2) * 5] as const),
);

const topCategorias = Object.fromEntries(
  CATEGORIAS.slice(0, 6).map((c, i) => [c, 214 - i * 28] as const),
);

const porScore = [...listaReunioes].sort((a, b) => (b.scoreComercial ?? 0) - (a.scoreComercial ?? 0));
const porRisco = listaReunioes
  .filter((r) => r.riscoChurn === 'ALTO')
  .sort((a, b) => (b.scoreComercial ?? 0) - (a.scoreComercial ?? 0));

export const dashboard: Dashboard = {
  totalReunioes: 1126,
  totalClientesCitados: 287,
  totalChurnAlto: 143,
  totalChurnMedio: 264,
  totalOportunidades: 419,
  scoreQualidadeMedia: '68,4',
  scoreComercialMedia: '54,9',
  sentimentos,
  sentimentoPredominante: 'MISTO',
  topProdutos,
  topConcorrentes,
  topCategorias,
  topOportunidades: porScore.slice(0, 6),
  topRiscoChurn: porRisco.slice(0, 6),
  planosAcao: {
    totalPorStatus: { PENDENTE: 61, EM_ANDAMENTO: 44, CONCLUIDO: 128, CANCELADO: 12 },
    total: 245,
    emAberto: 105,
    atrasados: 23,
  },
  clientes: {
    totalPorStatus: { ATIVO: 168, EM_RISCO: 41, PROSPECT: 57, INATIVO: 21 },
    total: 287,
    emRisco: 41,
  },
};

/* ── Planos de ação ─────────────────────────────────────────────────────── */

const TITULOS_PLANO = [
  'Agendar RCA do chamado crítico com o time de suporte',
  'Enviar comparativo de TCO para a diretoria',
  'Revisar cronograma de virada com o gerente de projeto',
  'Preparar proposta de renovação com desconto escalonado',
  'Treinar equipe fiscal no novo módulo',
  'Levantar divergência de estoque entre filial e CD',
  'Apresentar roadmap de Techfin ao controller',
  'Formalizar plano de recuperação de conta',
  'Reunir squad de integração com o WMS do parceiro',
  'Consolidar painel analítico entre as unidades',
];

const RESPONSAVEIS: Usuario[] = VENDEDORES.slice(0, 6).map((nome, i) => ({
  id: i + 1,
  nome,
  email: `${nome.toLowerCase().split(' ')[0]}@insight360.com.br`,
  perfil: i === 0 ? 'GESTOR' : 'VENDEDOR',
  perfilDescricao: i === 0 ? 'Gestor Comercial' : 'Vendedor',
  cargo: i === 0 ? 'Gerente de contas' : 'Executivo de contas',
  ativo: true,
}));

const STATUS_PLANO: readonly StatusPlanoAcao[] = [
  'PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDO', 'PENDENTE', 'EM_ANDAMENTO', 'CANCELADO',
];

const TRANSICOES: Record<StatusPlanoAcao, StatusPlanoAcao[]> = {
  PENDENTE: ['EM_ANDAMENTO', 'CANCELADO'],
  EM_ANDAMENTO: ['CONCLUIDO', 'CANCELADO'],
  CONCLUIDO: [],
  CANCELADO: [],
};

const DESCRICAO_PLANO: Record<StatusPlanoAcao, string> = {
  PENDENTE: 'Pendente',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
};

export const listaPlanos: PlanoAcao[] = Array.from({ length: 22 }, (_, i) => {
  const status = STATUS_PLANO[i % STATUS_PLANO.length];
  const reuniao = listaReunioes[(i * 3) % listaReunioes.length];
  const prioridade = (['ALTA', 'MEDIA', 'BAIXA'] as const)[i % 3];
  const dias = entre(-9, 34);
  const prazo = new Date(2025, 8, 9 + dias);
  const aberto = status === 'PENDENTE' || status === 'EM_ANDAMENTO';

  return {
    id: i + 1,
    titulo: TITULOS_PLANO[i % TITULOS_PLANO.length],
    descricao:
      'Ação derivada automaticamente dos insights da reunião. O responsável confirma a execução e registra o resultado no encerramento.',
    prioridade,
    prioridadeDescricao: { ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' }[prioridade],
    status,
    statusDescricao: DESCRICAO_PLANO[status],
    prazo: prazo.toISOString().slice(0, 10),
    atrasado: aberto && dias < 0,
    diasRestantes: dias,
    resultado: status === 'CONCLUIDO' ? 'Ação executada e validada com o cliente.' : undefined,
    responsavel: RESPONSAVEIS[i % RESPONSAVEIS.length],
    reuniao: {
      id: reuniao.id,
      idExterno: reuniao.idExterno,
      cliente: reuniao.cliente,
      data: reuniao.data,
    },
    transicoesPermitidas: TRANSICOES[status],
    dataCriacao: new Date(2025, 7, 1 + (i % 28)).toISOString(),
  };
});

/* ── Lotes ──────────────────────────────────────────────────────────────── */

export const lotes: ImportacaoLote[] = [
  {
    id: 1,
    codigoLote: 'LOTE-2025-0001',
    nomeArquivoOriginal: 'transcricoes_q3_2025.csv',
    dataHoraImportacao: '2025-09-02T14:31:08',
    totalRegistrosBrutos: 1204,
    totalReunioesValidas: 1126,
    totalReunioesIncompletas: 187,
    totalReunioesDuplicadas: 61,
    totalReunioesComErro: 17,
    statusProcessamento: 'CONCLUIDO',
    observacoes: '86,3 MB de transcrições processadas em 172 s.',
  },
  {
    id: 2,
    codigoLote: 'LOTE-2025-0002',
    nomeArquivoOriginal: 'transcricoes_piloto_sul.csv',
    dataHoraImportacao: '2025-08-18T09:12:44',
    totalRegistrosBrutos: 240,
    totalReunioesValidas: 226,
    totalReunioesIncompletas: 38,
    totalReunioesDuplicadas: 9,
    totalReunioesComErro: 5,
    statusProcessamento: 'CONCLUIDO',
  },
];

/* ── Acesso paginado / por id ───────────────────────────────────────────── */

function paginar<T>(itens: T[], pagina = 0, tamanho = 20): PaginaResposta<T> {
  const inicio = pagina * tamanho;
  const conteudo = itens.slice(inicio, inicio + tamanho);
  const totalPaginas = tamanho > 0 ? Math.ceil(itens.length / tamanho) : 0;
  return {
    conteudo,
    pagina,
    tamanho,
    totalElementos: itens.length,
    totalPaginas,
    primeira: pagina === 0,
    ultima: totalPaginas === 0 || pagina >= totalPaginas - 1,
  };
}

export function paginaReunioes(filtro: Record<string, unknown> = {}) {
  let itens = listaReunioes;
  const termo = String(filtro.termo ?? '').toLowerCase();

  if (termo) {
    itens = itens.filter(
      (r) =>
        r.cliente?.toLowerCase().includes(termo) ||
        r.vendedor?.toLowerCase().includes(termo) ||
        r.idExterno?.toLowerCase().includes(termo) ||
        r.categoriaPrincipal?.toLowerCase().includes(termo),
    );
  }
  if (filtro.sentimento) itens = itens.filter((r) => r.sentimento === filtro.sentimento);
  if (filtro.riscoChurn) itens = itens.filter((r) => r.riscoChurn === filtro.riscoChurn);
  if (filtro.statusCompletude) {
    itens = itens.filter((r) => r.statusCompletude === filtro.statusCompletude);
  }
  if (filtro.clienteId) itens = itens.filter((r) => r.clienteVinculado?.id === Number(filtro.clienteId));
  if (!filtro.incluirDuplicadas) itens = itens.filter((r) => !r.duplicada);

  const campo = String(filtro.ordenarPor ?? 'data');
  const sinal = filtro.direcao === 'asc' ? 1 : -1;
  itens = [...itens].sort((a, b) => {
    const va = (a as unknown as Record<string, unknown>)[campo];
    const vb = (b as unknown as Record<string, unknown>)[campo];
    if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * sinal;
    return String(va).localeCompare(String(vb)) * sinal;
  });

  return paginar(itens, Number(filtro.pagina ?? 0), Number(filtro.tamanho ?? 20));
}

export function reuniaoDetalhe(id: number): ReuniaoDetalhe {
  const base = listaReunioes.find((r) => r.id === id) ?? listaReunioes[0];
  const cliente = listaClientes.find((c) => c.id === base.clienteVinculado?.id) ?? listaClientes[0];
  const indice = base.id;

  return {
    ...base,
    formato: indice % 3 === 0 ? 'Presencial' : 'Videoconferência',
    duracaoMinutos: Number(base.duracao?.replace(/\D/g, '') ?? 45),
    uf: cliente.uf,
    cnae: '2599-3/99',
    nomeUnidade: `${cliente.nomeExibicao} — Matriz`,
    segmento: cliente.segmento,
    faturamento: cliente.faixaFaturamento,
    notaNps: cliente.notaNps,
    pontuacaoCompletude: base.scoreQualidade,
    motivoIncompletude:
      base.statusCompletude === 'COMPLETA'
        ? undefined
        : 'Transcrição sem identificação de todos os locutores e sem encerramento formal.',
    sentimentoJustificativa:
      'A conversa alterna reconhecimento de valor com queixas operacionais recorrentes; o encerramento não confirma próximo passo.',
    categoriasPrincipais: `${base.categoriaPrincipal}, Suporte`,
    temaReuniao: TEMAS[indice % TEMAS.length],
    resumoReuniao:
      'O cliente relata atraso recorrente no fechamento fiscal desde a última virada de versão e menciona que a diretoria passou a acompanhar o custo do contrato. Um chamado crítico foi reaberto três vezes sem análise de causa raiz. Ao final, sinaliza abertura para novos módulos caso o problema operacional seja resolvido.',
    pontosPrincipais: [
      'Fechamento fiscal em 11 dias úteis, acima do combinado.',
      'Chamado crítico reaberto três vezes sem RCA.',
      'Diretoria passou a acompanhar o custo por usuário.',
      'Abertura para Techfin condicionada à estabilização.',
    ].join('\n'),
    doresIdentificadas: DORES.slice(indice % 3, (indice % 3) + 3).join('\n'),
    oportunidades: OPORTUNIDADES.slice(indice % 2, (indice % 2) + 2).join('\n'),
    recomendacaoFinal:
      'Escalar o chamado crítico para o time de engenharia com prazo formal, apresentar RCA em até 10 dias e só então retomar a conversa comercial sobre módulos adicionais.',
    produtosIdentificados: [PRODUTOS[indice % PRODUTOS.length], PRODUTOS[(indice + 4) % PRODUTOS.length]].join(', '),
    concorrentesIdentificados: CONCORRENTES[indice % CONCORRENTES.length],
    personasIdentificadas: 'Controller, Gerente de TI, Diretor Financeiro',
    empresasIdentificadas: cliente.razaoSocial,
    areasInternas: 'Suporte, Engenharia de produto, Customer Success',
    budgetIdentificado: indice % 4 === 0 ? 'R$ 180 mil / ano' : 'Não declarado',
    locutoresIdentificados: `${base.vendedor}, ${cliente.contatoPrincipal}`,
    quantidadePalavras: entre(1800, 9400),
    quantidadeLocutores: entre(2, 5),
    transcricaoTratada: TRANSCRICAO_VITRINE.map((l) => `${l.locutor}: ${l.fala}`).join('\n\n'),
    motivoDuplicidade: base.duplicada ? 'Mesmo identificador externo já importado no lote anterior.' : undefined,
    nomeArquivoOrigem: 'transcricoes_q3_2025.csv',
    dataImportacao: '2025-09-02T14:31:08',
    totalInsights: entre(3, 9),
    totalComentarios: entre(0, 5),
    totalPlanosAcao: entre(0, 4),
  };
}

export function insights(reuniaoId: number): Insight[] {
  const tipos = ['DOR', 'OPORTUNIDADE', 'RISCO', 'CONCORRENCIA', 'PROXIMO_PASSO'];
  return Array.from({ length: 5 }, (_, i) => ({
    id: reuniaoId * 100 + i,
    reuniaoId,
    tipo: tipos[i],
    descricao: [
      DORES[(reuniaoId + i) % DORES.length],
      OPORTUNIDADES[(reuniaoId + i) % OPORTUNIDADES.length],
      'Decisão subiu de nível: a diretoria passou a acompanhar o contrato.',
      `Menção direta a ${CONCORRENTES[(reuniaoId + i) % CONCORRENTES.length]} durante a conversa.`,
      'Cliente pediu retorno formal com prazo em até dez dias.',
    ][i],
    prioridade: (['Alta', 'Média', 'Baixa'] as const)[i % 3],
    trechoOrigem:
      '"...desde a virada de versão o time fiscal vira dois fins de semana, e a diretoria começou a perguntar quanto isso custa."',
    confianca: 72 + ((reuniaoId + i * 7) % 26),
  }));
}

export function feedback(reuniaoId: number): Feedback {
  return {
    id: reuniaoId,
    reuniaoId,
    problemaIdentificado: 'O sinal de churn apareceu no minuto 12 e não foi tratado na própria reunião.',
    categoriaProblema: 'Escuta ativa / tratamento de objeção',
    motivoNaoIdentificadoAntes:
      'A menção ao concorrente veio embutida numa frase sobre custo; a conversa seguiu para o tema seguinte sem aprofundar.',
    sinaisNaConversa:
      '"a gente pediu proposta de outro fornecedor" · "a diretoria começou a perguntar quanto isso custa" · chamado reaberto três vezes',
    comoIdentificarAntes:
      'Sempre que o cliente mover a decisão para um nível hierárquico acima, pare a agenda e explore. Custo mencionado por diretoria raramente é sobre preço — é sobre valor percebido.',
    perguntasRecomendadas: [
      'O que precisaria acontecer nos próximos 30 dias para essa conversa sair da mesa da diretoria?',
      'Quando você diz que pediram proposta, isso já virou um processo formal de avaliação?',
      'Se o fechamento voltasse para 3 dias, o que mudaria na sua rotina?',
    ].join('\n'),
    acaoDeMelhoria:
      'Registrar o gatilho de escalonamento no CRM e abrir plano de ação com prazo antes de encerrar a chamada.',
    nivelCriticidade: 'ALTA',
    mensagemEducativa:
      'Reuniões que terminam sem próximo passo formal têm 3,1× mais chance de virar churn no trimestre seguinte. O momento de marcar o retorno é enquanto a dor ainda está sendo dita.',
  };
}

export function comentarios(reuniaoId: number): PaginaResposta<Comentario> {
  const itens: Comentario[] = [
    {
      id: reuniaoId * 10 + 1,
      reuniaoId,
      texto: 'Escalei para a engenharia. RCA prometido para sexta.',
      editado: false,
      autor: RESPONSAVEIS[0],
      dataCriacao: '2025-09-03T10:22:00',
    },
    {
      id: reuniaoId * 10 + 2,
      reuniaoId,
      texto: 'Confirmei com o controller: proposta concorrente ainda não é processo formal. Janela aberta.',
      editado: true,
      autor: RESPONSAVEIS[2],
      dataCriacao: '2025-09-04T16:40:00',
      dataAtualizacao: '2025-09-04T17:02:00',
    },
  ];
  return paginar(itens, 0, 20);
}

export function planosDaReuniao(reuniaoId: number): PlanoAcao[] {
  return listaPlanos.filter((p) => p.reuniao?.id === reuniaoId).slice(0, 3);
}

export function planosAtrasados(): PlanoAcao[] {
  return listaPlanos.filter((p) => p.atrasado);
}

export function paginaPlanos(filtro: Record<string, unknown> = {}) {
  let itens = listaPlanos;
  const termo = String(filtro.termo ?? '').toLowerCase();
  if (termo) itens = itens.filter((p) => p.titulo.toLowerCase().includes(termo));
  if (filtro.status) itens = itens.filter((p) => p.status === filtro.status);
  if (filtro.prioridade) itens = itens.filter((p) => p.prioridade === filtro.prioridade);
  if (filtro.somenteAtrasados) itens = itens.filter((p) => p.atrasado);
  return paginar(itens, Number(filtro.pagina ?? 0), Number(filtro.tamanho ?? 50));
}

export function paginaClientes(filtro: Record<string, unknown> = {}) {
  let itens = listaClientes;
  const termo = String(filtro.termo ?? '').toLowerCase();
  if (termo) {
    itens = itens.filter(
      (c) => c.nomeExibicao.toLowerCase().includes(termo) || c.cnpjFormatado?.includes(termo),
    );
  }
  if (filtro.status) itens = itens.filter((c) => c.status === filtro.status);
  if (filtro.uf) itens = itens.filter((c) => c.uf === filtro.uf);
  return paginar(itens, Number(filtro.pagina ?? 0), Number(filtro.tamanho ?? 24));
}

export function cliente(id: number): Cliente {
  return listaClientes.find((c) => c.id === id) ?? listaClientes[0];
}

export function reunioesDoCliente(id: number): ReuniaoResumo[] {
  return listaReunioes.filter((r) => r.clienteVinculado?.id === id);
}

export function usuarioDemo(email: string): Usuario {
  return {
    id: 1,
    nome: 'Administrador Insight360',
    email: email || 'admin@insight360.com.br',
    perfil: 'ADMIN',
    perfilDescricao: 'Administrador',
    cargo: 'Administrador da plataforma',
    ativo: true,
    dataCadastro: '2025-01-12T08:00:00',
  };
}
