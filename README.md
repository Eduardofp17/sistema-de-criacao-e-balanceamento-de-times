# Sistema de Criação e Balanceamento de Times

O **SCBT** forma times esportivos equilibrados com base na habilidade dos jogadores de cada grupo.

- Java 17
- PostgreSQL 17 (rodando no Docker)
- Maven (baixa o driver do PostgreSQL)

---

## Setup do projeto

### 1. Pré-requisitos

Instale apenas:

| Ferramenta | Para que serve | Como conferir |
|---|---|---|
| [Java 17+ (JDK)](https://adoptium.net/) | compilar e rodar o código | `java -version` |
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | rodar o banco de dados | `docker --version` |
| Git | baixar o projeto | `git --version` |
| [Maven](https://maven.apache.org/install.html) *(só para rodar pelo terminal)* | compilar e rodar pelo terminal | `mvn -version` |

Não é preciso instalar o PostgreSQL: o Docker cuida disso.

Quem rodar só pelo editor (IntelliJ ou VS Code) não precisa instalar o Maven, porque os editores já vêm com ele. Para rodar pelo terminal, instale:

```bash
brew install maven            # Mac
sudo apt install maven        # Linux (Ubuntu/Debian)
choco install maven           # Windows (com Chocolatey)
```

No Windows sem Chocolatey: baixe o `.zip` em [maven.apache.org](https://maven.apache.org/download.cgi), extraia e adicione a pasta `bin` ao `PATH`.

### 2. Clonar o repositório

```bash
git clone git@github.com:Eduardofp17/sistema-de-criacao-e-balanceamento-de-times.git
cd sistema-de-criacao-e-balanceamento-de-times
```

### 3. Subir o banco de dados

Com o Docker Desktop aberto:

```bash
docker compose up -d
```

Na primeira vez, o Docker baixa o PostgreSQL e cria todas as tabelas automaticamente (a partir de `db/init/01-schema.sql`).

### 4. Rodar o projeto

**Pelo editor:** abra a pasta do projeto, espere ele carregar o `pom.xml` e clique no ▶️ ao lado do `main` em `App.java`.

**Pelo terminal** (com o Maven instalado), na pasta do projeto:

```bash
mvn compile exec:java
```

- `compile`: compila o código (os `.class` vão para a pasta `target/`)
- `exec:java`: roda a classe `App`

Se aparecer `Conectado ao banco: PostgreSQL 17...`, está tudo certo.

---

## Banco de dados

### Dados de conexão

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Porta | `5432` |
| Banco | `balanceamento_times` |
| Usuário | `times` |
| Senha | `times` |

Use esses dados para abrir o banco em ferramentas visuais (DBeaver, pgAdmin, IntelliJ).

### Comandos úteis

```bash
docker compose up -d        # liga o banco
docker compose down         # desliga (os dados continuam salvos)
docker compose down -v      # desliga e APAGA todos os dados
docker exec -it times-postgres psql -U times -d balanceamento_times   # abre o terminal SQL
```

### Tabelas

| Tabela | Classe do diagrama | Observação |
|---|---|---|
| `usuario` | `Usuario` | `senha` guarda o **hash**, nunca a senha pura |
| `esporte` | `Esporte` | cada esporte pertence a um único grupo |
| `grupo` | `Grupo` | referencia esporte, administrador e criador |
| `participacao` | `Participacao` | chave: usuário + grupo |
| `habilidade` | `Habilidade` | valor de 0 a 5 (padrão 0), uma por participação |
| `time_esportivo` | `TimeEsportivo` | nome único dentro do grupo |
| `time_jogador` | lista `jogadores` do `TimeEsportivo` | uma linha por jogador do time |

O que o banco já faz sozinho:

- Excluir um **grupo** apaga suas participações, habilidades, times e jogadores dos times.
- Excluir uma **participação** apaga a habilidade daquele usuário naquele grupo.
- Não deixa criar habilidade para quem não participa do grupo.
- Não deixa excluir um esporte que ainda pertence a um grupo.
- Não aceita habilidade fora da faixa de 0 a 5.

## Estrutura do projeto

```
├── db/init/01-schema.sql          # criação das tabelas
├── docker-compose.yml             # configuração do PostgreSQL
├── pom.xml                        # dependências do projeto (driver do PostgreSQL)
└── src/main/java/br/ufersa/scbt/
    ├── App.java                   # ponto de entrada do programa
    └── db/ConexaoBD.java          # abre conexões com o banco
```

---

## Guia de implementação

### Como acessar o banco

Sempre com `try-with-resources` (fecha a conexão sozinho) e `PreparedStatement` (os `?` evitam SQL injection):

```java
String sql = "SELECT id, nome, username, senha, ativo FROM usuario WHERE username = ?";

try (Connection conexao = ConexaoBD.conectar();
     PreparedStatement comando = conexao.prepareStatement(sql)) {
  comando.setString(1, username);

  try (ResultSet resultado = comando.executeQuery()) {
    if (resultado.next()) {
      return new Usuario(
        resultado.getInt("id"),
        resultado.getString("nome"),
        resultado.getString("username"),
        resultado.getString("senha"),
        resultado.getBoolean("ativo")
      );
    }
  }
}
```
