## 1. Linha de base

- [x] 1.1 Rodar o teste de mutação (PIT) a partir de um build limpo, guardar uma cópia do relatório (`mutations.xml`) nos temporários do Claude (`.claude/tmp/`) e anotar aqui quantos mutantes morreram. Esta task não mexe em código. Morreram 337.
- [x] 1.2 Rodar a suíte inteira e anotar aqui o total de testes e quantos são dos testes de conta (`AccountApiTest`), de autenticação (`AuthApiTest`) e da raiz (`RootApiTest`). Esta task não mexe em código. São 356 no total: 15 de conta, 22 de autenticação e 6 da raiz.

## 2. Fixture no teste de autenticação

- [x] 2.1 Criar as três classes de suporte que o design descreve: a fixture de conta (`AccountFixture`), a de caminhos da API (`ApiPaths`) e a que conta linhas de tabela (`TableRows`). No teste de autenticação, os caminhos passam a vir da classe de caminhos; a conta da Ana (`ANA` e `ANA_LOGIN`) e os helpers de criar conta e pegar token (`createAccount`, `createAna` e `tokenOf`) passam a vir da fixture; e a contagem de tokens (`countTokens`) passa a usar a contadora de linhas. As duas classes que precisam de instância são criadas na preparação de cada teste (`@BeforeEach`), logo depois do cliente da API (`ApiClient`).

```diff
+++ b/src/test/java/com/william/kanban/support/ApiPaths.java
+public final class ApiPaths {
+
+	public static final String ACCOUNTS = "/accounts";
+
+	public static final String ME = "/accounts/me";
+
+	public static final String LOGIN = "/auth/login";
+
+	public static final String LOGOUT = "/auth/logout";
+
+	private ApiPaths() {
+	}
+
+}
```

```diff
+++ b/src/test/java/com/william/kanban/support/TableRows.java
+public final class TableRows {
+
+	private final JdbcTemplate jdbcTemplate;
+
+	public TableRows(JdbcTemplate jdbcTemplate) {
...
+	public int count(String table) {
...
+}
```

```diff
+++ b/src/test/java/com/william/kanban/support/AccountFixture.java
+public final class AccountFixture {
+
+	public static final CreateAccountRequest ANA =
+		new CreateAccountRequest("ana@exemplo.com", "Ana", "segredo");
+
+	public static final LoginRequest ANA_LOGIN =
+		new LoginRequest("ana@exemplo.com", "segredo");
+
+	private final ApiClient api;
+
+	private final JdbcTemplate jdbcTemplate;
+
+	public AccountFixture(ApiClient api, JdbcTemplate jdbcTemplate) {
...
+	public String createAccount(CreateAccountRequest account) {
...
+	public String createAna() {
...
+	public String tokenOf(LoginRequest login) {
...
+}
```

```diff
+++ b/src/test/java/com/william/kanban/auth/AuthApiTest.java
 	@BeforeEach
 	void setUp() {
 		api = new ApiClient(mockMvc);
+		accounts = new AccountFixture(api, jdbcTemplate);
+		rows = new TableRows(jdbcTemplate);
 		jdbcTemplate.execute("delete from accounts");
 	}
```

- [x] 2.2 Rodar só o teste de autenticação, sem medir cobertura, e conferir que ele passa com o mesmo número de testes anotado na 1.2.

## 3. Fixture no teste de conta

- [x] 3.1 No teste de conta, fazer a mesma troca: caminhos pela classe de caminhos, a conta da Ana (`ANA`) e os helpers de criar a Ana e fazer o login (`createAna` e `loginAsAna`) pela fixture, e a contagem de contas (`countAccounts`) pela contadora de linhas.
- [x] 3.2 Rodar só o teste de conta, sem medir cobertura, e conferir que ele passa com o mesmo número de testes anotado na 1.2.

## 4. Fixture no teste da raiz

- [x] 4.1 No teste da raiz, trocar o helper que cria a Ana e pega o token dela (`tokenOfAna`) pelas chamadas da fixture.
- [x] 4.2 Rodar só o teste da raiz, sem medir cobertura, e conferir que ele passa com o mesmo número de testes anotado na 1.2.

## 5. Convenção e formatação

- [x] 5.1 No arquivo de instruções do projeto (`CLAUDE.md`), em Convenções, logo depois do item do cliente da API, registrar as três classes de suporte: a fixture cria conta e emite token pela API e traz a conta da Ana; a classe de caminhos guarda os caminhos que mais de um teste usa; e a contadora de linhas conta as linhas de uma tabela. As duas com instância nascem na preparação de cada teste (`@BeforeEach`).
- [x] 5.2 Rodar a suíte inteira e conferir o total anotado na 1.2. Depois, confirmar que nenhum dos três testes ainda declara helper próprio de conta, de contagem ou constante de caminho, e que nenhuma linha dos arquivos tocados passa de noventa caracteres.

## 6. Mutação

- [x] 6.1 Rodar de novo o teste de mutação a partir de um build limpo e conferir que morreram pelo menos tantos mutantes quanto na 1.1. Esta task não mexe em código. Morreram os mesmos 337.
