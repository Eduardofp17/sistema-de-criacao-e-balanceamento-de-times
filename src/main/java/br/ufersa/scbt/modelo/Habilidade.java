package br.ufersa.scbt.modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.ufersa.scbt.db.ConexaoBD;

public class Habilidade {
  private static final String SELECT_HABILIDADE = """
      SELECT h.valor,
             u.id AS usuario_id, u.nome AS usuario_nome, u.username, u.senha, u.ativo,
             e.id AS esporte_id, e.nome AS esporte_nome, e.tamanho_equipe
      FROM habilidade h
      JOIN usuario u ON u.id = h.usuario_id
      JOIN grupo g ON g.id = h.grupo_id
      JOIN esporte e ON e.id = g.esporte_id
      """;

  private Usuario usuario;
  private Esporte esporte;
  private double valor;

  Habilidade(Usuario usuario, Esporte esporte, double valor) {
    this.usuario = usuario;
    this.esporte = esporte;
    this.valor = valor;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public Esporte getEsporte() {
    return esporte;
  }

  public double getValor() {
    return valor;
  }

  public void editar(double valor) {
    if (valor < 1 || valor > 5) {
      throw new IllegalArgumentException("A habilidade deve ser um valor de 1 a 5.");
    }

    String sql = """
        UPDATE habilidade SET valor = ?
        WHERE usuario_id = ?
          AND grupo_id = (SELECT id FROM grupo WHERE esporte_id = ?)
        """;

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setDouble(1, valor);
      comando.setInt(2, usuario.getId());
      comando.setInt(3, esporte.getId());
      comando.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao editar habilidade.", e);
    }

    this.valor = valor;
  }

  public static Habilidade criar(Usuario usuario, Esporte esporte, double valor) {
    validar(usuario, esporte);

    if (valor < 0 || valor > 5) {
      throw new IllegalArgumentException("A habilidade deve ser um valor de 0 a 5.");
    }

    if (consultar(usuario, esporte) != null) {
      throw new IllegalArgumentException(usuario.getNome() + " já tem habilidade nessa modalidade.");
    }

    String sql = """
        INSERT INTO habilidade (usuario_id, grupo_id, valor)
        SELECT p.usuario_id, p.grupo_id, ?
        FROM participacao p
        JOIN grupo g ON g.id = p.grupo_id
        WHERE p.usuario_id = ? AND g.esporte_id = ?
        """;

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setDouble(1, valor);
      comando.setInt(2, usuario.getId());
      comando.setInt(3, esporte.getId());

      if (comando.executeUpdate() == 0) {
        throw new IllegalArgumentException(usuario.getNome() + " não participa do grupo dessa modalidade.");
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao criar habilidade.", e);
    }

    return new Habilidade(usuario, esporte, valor);
  }

  public static Habilidade consultar(Usuario usuario, Esporte esporte) {
    validar(usuario, esporte);

    String sql = SELECT_HABILIDADE + " WHERE h.usuario_id = ? AND e.id = ?";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setInt(1, usuario.getId());
      comando.setInt(2, esporte.getId());

      try (ResultSet resultado = comando.executeQuery()) {
        if (resultado.next()) {
          return lerHabilidade(resultado);
        }

        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao consultar habilidade.", e);
    }
  }

  public static void excluir(Usuario usuario, Esporte esporte) {
    validar(usuario, esporte);

    String sql = """
        DELETE FROM habilidade
        WHERE usuario_id = ?
          AND grupo_id = (SELECT id FROM grupo WHERE esporte_id = ?)
        """;

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setInt(1, usuario.getId());
      comando.setInt(2, esporte.getId());
      comando.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao excluir habilidade.", e);
    }
  }

  public static List<Habilidade> listar() {
    return buscarLista(SELECT_HABILIDADE + " ORDER BY e.nome, u.nome", null);
  }

  public static List<Habilidade> listarPorUsuario(Usuario usuario) {
    if (usuario == null) {
      throw new IllegalArgumentException("Informe o usuário.");
    }

    return buscarLista(SELECT_HABILIDADE + " WHERE h.usuario_id = ? ORDER BY e.nome", usuario.getId());
  }

  private static List<Habilidade> buscarLista(String sql, Integer usuarioId) {
    List<Habilidade> habilidades = new ArrayList<>();

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      if (usuarioId != null) {
        comando.setInt(1, usuarioId);
      }

      try (ResultSet resultado = comando.executeQuery()) {
        while (resultado.next()) {
          habilidades.add(lerHabilidade(resultado));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao listar habilidades.", e);
    }

    return habilidades;
  }

  private static Habilidade lerHabilidade(ResultSet resultado) throws SQLException {
    Usuario usuario = new Usuario(
      resultado.getInt("usuario_id"),
      resultado.getString("usuario_nome"),
      resultado.getString("username"),
      resultado.getString("senha"),
      resultado.getBoolean("ativo")
    );

    Esporte esporte = new Esporte(
      resultado.getInt("esporte_id"),
      resultado.getString("esporte_nome"),
      resultado.getInt("tamanho_equipe")
    );

    return new Habilidade(usuario, esporte, resultado.getDouble("valor"));
  }

  private static void validar(Usuario usuario, Esporte esporte) {
    if (usuario == null) {
      throw new IllegalArgumentException("Informe o usuário.");
    }

    if (esporte == null) {
      throw new IllegalArgumentException("Informe a modalidade.");
    }
  }
}
