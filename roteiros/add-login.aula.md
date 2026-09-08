# O login do Kanban
Tudo o que mudou no código para uma requisição saber quem a fez.

## A troca de fundação
figura: figuras/fundacao.svg
- Um starter no lugar da biblioteca :: O projeto guardava senha em hash com uma biblioteca solta de criptografia. Agora usa o starter de segurança inteiro, que traz junto a cadeia de filtros.
- A tabela de tokens :: Uma migração nova cria a tabela de tokens, com chave estrangeira para contas e apagamento em cascata.
- O prazo em propriedade :: A validade virou uma propriedade do arquivo de configuração, lida como duração, com vinte e quatro horas de padrão.

## O que a tabela guarda
figura: figuras/tabela.svg
- Uma linha por login :: Cada login insere uma linha, e cada logout apaga a linha que aquela requisição usou. Não existe coluna de estado para revogar.
- Só o resumo do token :: A coluna guarda o resumo criptográfico, nunca o valor em claro. Um índice único sobre ela atende toda busca por requisição.
- A entidade e o repositório :: Do lado do código entram uma entidade com identificador gerado no construtor e um repositório que busca e apaga pelo resumo.

## A conferência da senha
figura: figuras/credencial.svg
- O serviço de conta abriu uma porta :: A classe de serviço de conta virou pública, e um único método novo saiu do pacote: o de autenticar. A senha em hash continua sem atravessar a fronteira.
- Busca pelo email normalizado :: O repositório ganhou uma busca por email, e o método baixa a caixa das letras antes de procurar.
- Custo igual nos dois erros :: Quando o email não existe, o código ainda compara a senha contra um hash fixo. O tempo de resposta fica igual, e o erro não entrega quais emails existem.

## Como o token nasce
figura: figuras/emissao.svg
- Duzentos e cinquenta e seis bits sorteados :: O serviço de autenticação sorteia trinta e dois bytes de uma fonte criptográfica e codifica em base sessenta e quatro, na variante segura para endereços.
- O resumo vai para o banco :: Do valor sorteado, só o resumo é gravado. O valor em claro aparece numa única resposta da interface, a do login.
- Um registro carrega os dois :: Como a entidade guarda o resumo, o valor em claro sai do serviço num registro próprio, com o token e a data de expiração.

## Login e logout na interface
figura: figuras/endpoints.svg
- Duzentos e um na emissão :: O controlador responde duzentos e um no login, com o token, o tipo e a data de expiração, em nomes com sublinhado.
- Quatrocentos e um indistinguível :: Uma exceção nova de credencial inválida vira quatrocentos e um no tradutor global de erros, com o mesmo corpo para senha errada e para email inexistente.
- Duzentos e quatro no logout :: O logout apaga a linha do token da própria chamada e responde duzentos e quatro, sem corpo nenhum.

## A cadeia de filtros
figura: figuras/cadeia.svg
- O filtro resolve o token :: Um filtro que roda uma vez por requisição lê o cabeçalho de autorização, resolve o token e põe o identificador da conta no contexto de segurança.
- O erro sai no formato da casa :: Sem um ponto de entrada próprio, a cadeia responderia quatrocentos e três com corpo vazio. O ponto de entrada novo escreve o mesmo formato de problema do resto da interface.
- Três rotas abertas :: Só o cadastro, o login e a documentação atendem sem token. Toda rota fora dessa lista nasce bloqueada.

## A conta do token
figura: figuras/conta.svg
- A consulta por identificador saiu :: O endpoint que buscava conta por identificador foi removido, porque expunha qualquer conta a qualquer chamador.
- Entrou a conta autenticada :: No lugar dele entrou o endpoint da própria conta, que lê o identificador do principal da requisição e nunca do endereço.
- O cadastro perdeu um cabeçalho :: A criação de conta continua respondendo duzentos e um, mas sem o cabeçalho de localização, que apontava para o endpoint removido.

## O ajuste do logout
figura: figuras/logout.svg
- Um campo a mais na documentação :: Ler o cabeçalho dentro do controlador fazia a documentação declarar um parâmetro de autorização, e a interface de testes mostrava um campo extra para preencher à mão.
- O token virou credencial :: Agora o filtro guarda o token nas credenciais da autenticação, e o controlador o lê de lá. Ele não conhece mais o formato do cabeçalho.
- Um teste guarda a porta :: Um teste confere que a operação de logout não declara parâmetro nenhum, para o campo não voltar sem ninguém perceber.

## O que só apareceu rodando
figura: figuras/achados.svg
- O usuário fantasma :: Desligar autenticação básica e formulário não impediu a subida de criar um usuário com senha aleatória. Foi preciso excluir a autoconfiguração que o gera.
- O erro sem instância :: Os dois caminhos de erro devolviam formatos diferentes. O ponto de entrada passou a preencher o campo de instância, como o tradutor global já fazia.
- Duas contas no teste :: Com uma conta só, um defeito que devolvesse sempre a primeira conta da tabela passaria despercebido. O teste novo cadastra duas.

## Onde tudo fechou
- Quarenta e um testes verdes :: A suíte terminou com quarenta e um testes, cobrindo cada cenário escrito nas especificações da mudança.
- Cobertura e mutação acima do limite :: A cobertura passou do mínimo de oitenta por cento, e o teste de mutação matou noventa por cento dos mutantes.
- O desenho voltou a bater :: O documento de desenho foi corrigido em três pontos onde a implementação tinha divergido dele.
