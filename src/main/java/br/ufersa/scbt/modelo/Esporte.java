package br.ufersa.scbt.modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.ufersa.scbt.db.ConexaoBD;

public class Esporte {
  private int id;
  private String nome;
  private int tamanhoEquipe;

  Esporte(int id, String nome, int tamanhoEquipe) {
    this.id = id;
    this.nome = nome;
    this.tamanhoEquipe = tamanhoEquipe;
  }

  public int getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public int getTamanhoEquipe() {
    return tamanhoEquipe;
  }

  public void editar(String nome, int tamanhoEquipe) {
    validar(nome, tamanhoEquipe);

    String sql = "UPDATE esporte SET nome = ?, tamanho_equipe = ? WHERE id = ?";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setString(1, nome);
      comando.setInt(2, tamanhoEquipe);
      comando.setInt(3, id);
      comando.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao editar esporte.", e);
    }

    this.nome = nome;
    this.tamanhoEquipe = tamanhoEquipe;
  }

  public static Esporte criar(String nome, int tamanhoEquipe) {
    validar(nome, tamanhoEquipe);

    String sql = "INSERT INTO esporte (nome, tamanho_equipe) VALUES (?, ?) RETURNING id";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setString(1, nome);
      comando.setInt(2, tamanhoEquipe);

      try (ResultSet resultado = comando.executeQuery()) {
        resultado.next();

        return new Esporte(resultado.getInt("id"), nome, tamanhoEquipe);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao criar esporte.", e);
    }
  }

  public static Esporte consultar(String nome) {
    String sql = "SELECT id, nome, tamanho_equipe FROM esporte WHERE nome = ? ORDER BY id LIMIT 1";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setString(1, nome);

      try (ResultSet resultado = comando.executeQuery()) {
        if (resultado.next()) {
          return lerEsporte(resultado);
        }

        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao consultar esporte.", e);
    }
  }

  public static void excluir(String nome) {
    String sql = "DELETE FROM esporte e WHERE e.nome = ? AND NOT EXISTS (SELECT 1 FROM grupo g WHERE g.esporte_id = e.id)";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setString(1, nome);
      comando.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao excluir esporte.", e);
    }
  }

  public static List<Esporte> listar() {
    String sql = "SELECT id, nome, tamanho_equipe FROM esporte ORDER BY nome";
    List<Esporte> esportes = new ArrayList<>();

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql);
         ResultSet resultado = comando.executeQuery()) {
      while (resultado.next()) {
        esportes.add(lerEsporte(resultado));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao listar esportes.", e);
    }

    return esportes;
  }

  private static Esporte lerEsporte(ResultSet resultado) throws SQLException {
    return new Esporte(
      resultado.getInt("id"),
      resultado.getString("nome"),
      resultado.getInt("tamanho_equipe")
    );
  }

  private static void validar(String nome, int tamanhoEquipe) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("O campo nome do esporte é obrigatório.");
    }

    if (tamanhoEquipe <= 0) {
      throw new IllegalArgumentException("O tamanho da equipe deve ser maior que zero.");
    }
  }
}
