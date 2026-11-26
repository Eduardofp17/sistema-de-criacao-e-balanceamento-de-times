package br.ufersa.scbt.modelo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import br.ufersa.scbt.db.ConexaoBD;

public class Usuario {
  private static final int ITERACOES_HASH = 210_000;

  private int id;
  private String nome;
  private String username;
  private String senha;
  private boolean ativo;

  Usuario(int id, String nome, String username, String senha, boolean ativo) {
    this.id = id;
    this.nome = nome;
    this.username = username;
    this.senha = senha;
    this.ativo = ativo;
  }

  public int getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getUsername() {
    return username;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void editar(String nome, String username, String senha) {
    if (!ativo) {
      throw new IllegalStateException("Conta inativa: reative a conta para editar.");
    }

    validarTexto(nome, "nome");
    validarTexto(username, "username");

    if (!username.equals(this.username) && consultar(username) != null) {
      throw new IllegalArgumentException("O username \"" + username + "\" já está em uso.");
    }

    String novoHash = (senha == null || senha.isBlank()) ? this.senha : gerarHash(senha);

    String sql = "UPDATE usuario SET nome = ?, username = ?, senha = ? WHERE id = ?";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setString(1, nome);
      comando.setString(2, username);
      comando.setString(3, novoHash);
      comando.setInt(4, id);
      comando.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao editar usuário.", e);
    }

    this.nome = nome;
    this.username = username;
    this.senha = novoHash;
  }

  public void desativar() {
    if (!ativo) {
      throw new IllegalStateException("A conta já está inativa.");
    }

    for (Grupo grupo : Grupo.listarPorUsuario(this)) {
      Participacao.excluirParticipacao(this, grupo);
    }

    alterarAtivo(false);
  }

  public void reativar() {
    alterarAtivo(true);
  }

  public static Usuario criar(String nome, String username, String senha) {
    validarTexto(nome, "nome");
    validarTexto(username, "username");
    validarTexto(senha, "senha");

    if (consultar(username) != null) {
      throw new IllegalArgumentException("O username \"" + username + "\" já está em uso.");
    }

    String hash = gerarHash(senha);
    String sql = "INSERT INTO usuario (nome, username, senha) VALUES (?, ?, ?) RETURNING id";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setString(1, nome);
      comando.setString(2, username);
      comando.setString(3, hash);

      try (ResultSet resultado = comando.executeQuery()) {
        resultado.next();

        return new Usuario(resultado.getInt("id"), nome, username, hash, true);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao criar usuário.", e);
    }
  }

  public static Usuario consultar(String username) {
    String sql = "SELECT id, nome, username, senha, ativo FROM usuario WHERE username = ?";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setString(1, username);

      try (ResultSet resultado = comando.executeQuery()) {
        if (resultado.next()) {
          return lerUsuario(resultado);
        }

        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao consultar usuário.", e);
    }
  }

  public static boolean autenticar(String username, String senha) {
    Usuario usuario = consultar(username);

    if (usuario == null || senha == null) {
      return false;
    }

    return conferirSenha(senha, usuario.senha);
  }

  public static List<Usuario> listar() {
    String sql = "SELECT id, nome, username, senha, ativo FROM usuario ORDER BY nome";
    List<Usuario> usuarios = new ArrayList<>();

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql);
         ResultSet resultado = comando.executeQuery()) {
      while (resultado.next()) {
        usuarios.add(lerUsuario(resultado));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao listar usuários.", e);
    }

    return usuarios;
  }

  private void alterarAtivo(boolean ativo) {
    String sql = "UPDATE usuario SET ativo = ? WHERE id = ?";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setBoolean(1, ativo);
      comando.setInt(2, id);
      comando.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao alterar a situação do usuário.", e);
    }

    this.ativo = ativo;
  }

  private static Usuario lerUsuario(ResultSet resultado) throws SQLException {
    return new Usuario(
      resultado.getInt("id"),
      resultado.getString("nome"),
      resultado.getString("username"),
      resultado.getString("senha"),
      resultado.getBoolean("ativo")
    );
  }

  private static void validarTexto(String valor, String campo) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException("O campo " + campo + " é obrigatório.");
    }
  }

  private static String gerarHash(String senha) {
    byte[] sal = new byte[16];
    new SecureRandom().nextBytes(sal);

    byte[] hash = calcularPbkdf2(senha, sal, ITERACOES_HASH);
    Base64.Encoder base64 = Base64.getEncoder();

    return ITERACOES_HASH + ":" + base64.encodeToString(sal) + ":" + base64.encodeToString(hash);
  }

  private static boolean conferirSenha(String senha, String hashGuardado) {
    String[] partes = hashGuardado.split(":");
    int iteracoes = Integer.parseInt(partes[0]);
    byte[] sal = Base64.getDecoder().decode(partes[1]);
    byte[] hashEsperado = Base64.getDecoder().decode(partes[2]);

    byte[] hashDigitado = calcularPbkdf2(senha, sal, iteracoes);

    return MessageDigest.isEqual(hashEsperado, hashDigitado);
  }

  private static byte[] calcularPbkdf2(String senha, byte[] sal, int iteracoes) {
    PBEKeySpec especificacao = new PBEKeySpec(senha.toCharArray(), sal, iteracoes, 256);

    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacao).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("Erro ao gerar o hash da senha.", e);
    } finally {
      especificacao.clearPassword();
    }
  }
}
