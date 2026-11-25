package br.ufersa.scbt.modelo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.ufersa.scbt.db.ConexaoBD;

public class Participacao {
  private static final String SELECT_PARTICIPACAO = """
      SELECT p.grupo_id, p.data_entrada, p.hora_entrada,
             u.id, u.nome, u.username, u.senha, u.ativo
      FROM participacao p
      JOIN usuario u ON u.id = p.usuario_id
      """;

  private Usuario usuario;
  private Grupo grupo;
  private Date dataEntrada;
  private Time horaEntrada;

  Participacao(Usuario usuario, Grupo grupo, Date dataEntrada, Time horaEntrada) {
    this.usuario = usuario;
    this.grupo = grupo;
    this.dataEntrada = dataEntrada;
    this.horaEntrada = horaEntrada;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public Grupo getGrupo() {
    return grupo;
  }

  public Date getDataEntrada() {
    return dataEntrada;
  }

  public Time getHoraEntrada() {
    return horaEntrada;
  }

  public static void adicionarParticipante(Usuario usuario, Grupo grupo) {
    validar(usuario, grupo);

    if (!usuario.isAtivo()) {
      throw new IllegalStateException(usuario.getNome() + " está com a conta inativa.");
    }

    if (consultar(usuario, grupo) != null) {
      throw new IllegalArgumentException(usuario.getNome() + " já participa do grupo.");
    }

    String sqlParticipacao = "INSERT INTO participacao (usuario_id, grupo_id) VALUES (?, ?)";
    String sqlHabilidade = "INSERT INTO habilidade (usuario_id, grupo_id, valor) VALUES (?, ?, 0)";

    try (Connection conexao = ConexaoBD.conectar()) {
      conexao.setAutoCommit(false);

      try {
        for (String sql : List.of(sqlParticipacao, sqlHabilidade)) {
          try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, usuario.getId());
            comando.setInt(2, grupo.getId());
            comando.executeUpdate();
          }
        }

        conexao.commit();
      } catch (SQLException e) {
        conexao.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao adicionar participante.", e);
    }
  }

  public static void excluirParticipacao(Usuario usuario, Grupo grupo) {
    validar(usuario, grupo);

    if (consultar(usuario, grupo) == null) {
      throw new IllegalArgumentException(usuario.getNome() + " não participa do grupo.");
    }

    String sqlTime = """
        DELETE FROM time_esportivo
        WHERE grupo_id = ?
          AND id IN (SELECT time_id FROM time_jogador WHERE usuario_id = ?)
        """;
    String sqlParticipacao = "DELETE FROM participacao WHERE grupo_id = ? AND usuario_id = ?";

    try (Connection conexao = ConexaoBD.conectar()) {
      conexao.setAutoCommit(false);

      try {
        for (String sql : List.of(sqlTime, sqlParticipacao)) {
          try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, grupo.getId());
            comando.setInt(2, usuario.getId());
            comando.executeUpdate();
          }
        }

        conexao.commit();
      } catch (SQLException e) {
        conexao.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao excluir participação.", e);
    }

    List<Participacao> restantes = listarPorGrupo(grupo);

    if (restantes.isEmpty()) {
      Grupo.excluir(grupo.getNome());
      return;
    }

    Grupo grupoAtual = Grupo.consultar(grupo.getNome());

    if (grupoAtual.getAdministrador().getId() == usuario.getId()) {
      grupoAtual.transferirAdministracao(restantes.get(0).getUsuario());
    }
  }

  public static Participacao consultar(Usuario usuario, Grupo grupo) {
    validar(usuario, grupo);

    String sql = SELECT_PARTICIPACAO + " WHERE p.usuario_id = ? AND p.grupo_id = ?";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setInt(1, usuario.getId());
      comando.setInt(2, grupo.getId());

      try (ResultSet resultado = comando.executeQuery()) {
        if (resultado.next()) {
          return lerParticipacao(resultado, grupo);
        }

        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao consultar participação.", e);
    }
  }

  public static List<Participacao> listar() {
    Map<Integer, Grupo> grupos = new HashMap<>();

    for (Grupo grupo : Grupo.listar()) {
      grupos.put(grupo.getId(), grupo);
    }

    String sql = SELECT_PARTICIPACAO + " ORDER BY p.grupo_id, p.data_entrada, p.hora_entrada";
    List<Participacao> participacoes = new ArrayList<>();

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql);
         ResultSet resultado = comando.executeQuery()) {
      while (resultado.next()) {
        participacoes.add(lerParticipacao(resultado, grupos.get(resultado.getInt("grupo_id"))));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao listar participações.", e);
    }

    return participacoes;
  }

  public static List<Participacao> listarPorGrupo(Grupo grupo) {
    if (grupo == null) {
      throw new IllegalArgumentException("Informe o grupo.");
    }

    String sql = SELECT_PARTICIPACAO + " WHERE p.grupo_id = ? ORDER BY p.data_entrada, p.hora_entrada";
    List<Participacao> participacoes = new ArrayList<>();

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setInt(1, grupo.getId());

      try (ResultSet resultado = comando.executeQuery()) {
        while (resultado.next()) {
          participacoes.add(lerParticipacao(resultado, grupo));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao listar participantes do grupo.", e);
    }

    return participacoes;
  }

  private static Participacao lerParticipacao(ResultSet resultado, Grupo grupo) throws SQLException {
    Usuario usuario = new Usuario(
      resultado.getInt("id"),
      resultado.getString("nome"),
      resultado.getString("username"),
      resultado.getString("senha"),
      resultado.getBoolean("ativo")
    );

    return new Participacao(usuario, grupo, resultado.getDate("data_entrada"), resultado.getTime("hora_entrada"));
  }

  private static void validar(Usuario usuario, Grupo grupo) {
    if (usuario == null) {
      throw new IllegalArgumentException("Informe o usuário.");
    }

    if (grupo == null) {
      throw new IllegalArgumentException("Informe o grupo.");
    }
  }
}
