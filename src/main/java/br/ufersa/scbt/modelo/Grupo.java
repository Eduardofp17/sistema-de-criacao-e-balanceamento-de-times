package br.ufersa.scbt.modelo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import br.ufersa.scbt.db.ConexaoBD;

public class Grupo {
  private static final String SELECT_GRUPO = """
      SELECT g.id, g.nome, g.descricao, g.data_criacao, g.hora_criacao,
             e.id AS esporte_id, e.nome AS esporte_nome, e.tamanho_equipe,
             a.id AS admin_id, a.nome AS admin_nome, a.username AS admin_username,
             a.senha AS admin_senha, a.ativo AS admin_ativo,
             c.id AS criador_id, c.nome AS criador_nome, c.username AS criador_username,
             c.senha AS criador_senha, c.ativo AS criador_ativo
      FROM grupo g
      JOIN esporte e ON e.id = g.esporte_id
      JOIN usuario a ON a.id = g.administrador_id
      JOIN usuario c ON c.id = g.criador_id
      """;

  private int id;
  private String nome;
  private String descricao;
  private Date dataCriacao;
  private Time horaCriacao;
  private Esporte esporte;
  private Usuario administrador;
  private Usuario criador;

  Grupo(int id, String nome, String descricao, Date dataCriacao, Time horaCriacao,
        Esporte esporte, Usuario administrador, Usuario criador) {
    this.id = id;
    this.nome = nome;
    this.descricao = descricao;
    this.dataCriacao = dataCriacao;
    this.horaCriacao = horaCriacao;
    this.esporte = esporte;
    this.administrador = administrador;
    this.criador = criador;
  }

  public int getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getDescricao() {
    return descricao;
  }

  public Date getDataCriacao() {
    return dataCriacao;
  }

  public Time getHoraCriacao() {
    return horaCriacao;
  }

  public Esporte getEsporte() {
    return esporte;
  }

  public Usuario getAdministrador() {
    return administrador;
  }

  public Usuario getCriador() {
    return criador;
  }

  public void editar(String nome, String descricao) {
    validarNome(nome);

    if (!nome.equals(this.nome) && consultar(nome) != null) {
      throw new IllegalArgumentException("Já existe um grupo com o nome \"" + nome + "\".");
    }

    String sql = "UPDATE grupo SET nome = ?, descricao = ? WHERE id = ?";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setString(1, nome);
      comando.setString(2, descricao);
      comando.setInt(3, id);
      comando.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao editar grupo.", e);
    }

    this.nome = nome;
    this.descricao = descricao;
  }

  public void transferirAdministracao(Usuario usuario) {
    if (usuario == null) {
      throw new IllegalArgumentException("Informe o novo administrador.");
    }

    if (usuario.getId() == administrador.getId()) {
      throw new IllegalArgumentException(usuario.getNome() + " já é o administrador do grupo.");
    }

    String sqlParticipa = "SELECT 1 FROM participacao WHERE usuario_id = ? AND grupo_id = ?";
    String sqlTransferir = "UPDATE grupo SET administrador_id = ? WHERE id = ?";

    try (Connection conexao = ConexaoBD.conectar()) {
      try (PreparedStatement comando = conexao.prepareStatement(sqlParticipa)) {
        comando.setInt(1, usuario.getId());
        comando.setInt(2, id);

        try (ResultSet resultado = comando.executeQuery()) {
          if (!resultado.next()) {
            throw new IllegalArgumentException(usuario.getNome() + " não participa do grupo.");
          }
        }
      }

      try (PreparedStatement comando = conexao.prepareStatement(sqlTransferir)) {
        comando.setInt(1, usuario.getId());
        comando.setInt(2, id);
        comando.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao transferir a administração.", e);
    }

    this.administrador = usuario;
  }

  public static Grupo criar(String nome, String descricao, Esporte esporte, Usuario criador) {
    validarNome(nome);

    if (esporte == null) {
      throw new IllegalArgumentException("Informe a modalidade esportiva do grupo.");
    }

    if (criador == null || !criador.isAtivo()) {
      throw new IllegalStateException("Apenas usuários ativos podem criar grupos.");
    }

    if (consultar(nome) != null) {
      throw new IllegalArgumentException("Já existe um grupo com o nome \"" + nome + "\".");
    }

    String sqlGrupo = """
        INSERT INTO grupo (nome, descricao, esporte_id, administrador_id, criador_id)
        VALUES (?, ?, ?, ?, ?)
        RETURNING id, data_criacao, hora_criacao
        """;
    String sqlParticipacao = "INSERT INTO participacao (usuario_id, grupo_id) VALUES (?, ?)";
    String sqlHabilidade = "INSERT INTO habilidade (usuario_id, grupo_id) VALUES (?, ?)";

    try (Connection conexao = ConexaoBD.conectar()) {
      conexao.setAutoCommit(false);

      try {
        int id;
        Date dataCriacao;
        Time horaCriacao;

        try (PreparedStatement comando = conexao.prepareStatement(sqlGrupo)) {
          comando.setString(1, nome);
          comando.setString(2, descricao);
          comando.setInt(3, esporte.getId());
          comando.setInt(4, criador.getId());
          comando.setInt(5, criador.getId());

          try (ResultSet resultado = comando.executeQuery()) {
            resultado.next();
            id = resultado.getInt("id");
            dataCriacao = resultado.getDate("data_criacao");
            horaCriacao = resultado.getTime("hora_criacao");
          }
        }

        for (String sql : List.of(sqlParticipacao, sqlHabilidade)) {
          try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, criador.getId());
            comando.setInt(2, id);
            comando.executeUpdate();
          }
        }

        conexao.commit();

        return new Grupo(id, nome, descricao, dataCriacao, horaCriacao, esporte, criador, criador);
      } catch (SQLException e) {
        conexao.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao criar grupo.", e);
    }
  }

  public static Grupo consultar(String nome) {
    String sql = SELECT_GRUPO + " WHERE g.nome = ?";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setString(1, nome);

      try (ResultSet resultado = comando.executeQuery()) {
        if (resultado.next()) {
          return lerGrupo(resultado);
        }

        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao consultar grupo.", e);
    }
  }

  public static void excluir(String nome) {
    String sqlGrupo = "DELETE FROM grupo WHERE nome = ? RETURNING esporte_id";
    String sqlEsporte = "DELETE FROM esporte WHERE id = ?";

    try (Connection conexao = ConexaoBD.conectar()) {
      conexao.setAutoCommit(false);

      try {
        int esporteId;

        try (PreparedStatement comando = conexao.prepareStatement(sqlGrupo)) {
          comando.setString(1, nome);

          try (ResultSet resultado = comando.executeQuery()) {
            if (!resultado.next()) {
              conexao.rollback();
              throw new IllegalArgumentException("Grupo \"" + nome + "\" não encontrado.");
            }

            esporteId = resultado.getInt("esporte_id");
          }
        }

        try (PreparedStatement comando = conexao.prepareStatement(sqlEsporte)) {
          comando.setInt(1, esporteId);
          comando.executeUpdate();
        }

        conexao.commit();
      } catch (SQLException e) {
        conexao.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao excluir grupo.", e);
    }
  }

  public static List<Grupo> listar() {
    return buscarLista(SELECT_GRUPO + " ORDER BY g.nome", null);
  }

  public static List<Grupo> listarPorUsuario(Usuario usuario) {
    String sql = SELECT_GRUPO
        + " JOIN participacao p ON p.grupo_id = g.id WHERE p.usuario_id = ? ORDER BY g.nome";

    return buscarLista(sql, usuario.getId());
  }

  private static List<Grupo> buscarLista(String sql, Integer usuarioId) {
    List<Grupo> grupos = new ArrayList<>();

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      if (usuarioId != null) {
        comando.setInt(1, usuarioId);
      }

      try (ResultSet resultado = comando.executeQuery()) {
        while (resultado.next()) {
          grupos.add(lerGrupo(resultado));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao listar grupos.", e);
    }

    return grupos;
  }

  private static Grupo lerGrupo(ResultSet resultado) throws SQLException {
    Esporte esporte = new Esporte(
      resultado.getInt("esporte_id"),
      resultado.getString("esporte_nome"),
      resultado.getInt("tamanho_equipe")
    );

    Usuario administrador = new Usuario(
      resultado.getInt("admin_id"),
      resultado.getString("admin_nome"),
      resultado.getString("admin_username"),
      resultado.getString("admin_senha"),
      resultado.getBoolean("admin_ativo")
    );

    Usuario criador = new Usuario(
      resultado.getInt("criador_id"),
      resultado.getString("criador_nome"),
      resultado.getString("criador_username"),
      resultado.getString("criador_senha"),
      resultado.getBoolean("criador_ativo")
    );

    return new Grupo(
      resultado.getInt("id"),
      resultado.getString("nome"),
      resultado.getString("descricao"),
      resultado.getDate("data_criacao"),
      resultado.getTime("hora_criacao"),
      esporte,
      administrador,
      criador
    );
  }

  private static void validarNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("O campo nome do grupo é obrigatório.");
    }
  }
}
