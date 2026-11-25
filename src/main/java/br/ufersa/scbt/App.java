package br.ufersa.scbt;

import java.io.Console;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

import br.ufersa.scbt.db.ConexaoBD;
import br.ufersa.scbt.modelo.Usuario;

public class App {
  private static final Scanner entrada = new Scanner(System.in);

  public static void main(String[] args) {
    if (!bancoDisponivel()) {
      System.out.println("Não foi possível conectar ao banco. Ele está ligado? (docker compose up -d)");
      return;
    }

    telaInicial();
  }

  private static void telaInicial() {
    while (true) {
      System.out.println();
      System.out.println("===== SCBT - Sistema de Criação e Balanceamento de Times =====");
      System.out.println("1 - Cadastrar");
      System.out.println("2 - Entrar");
      System.out.println("0 - Sair do sistema");

      switch (lerOpcao()) {
        case 1 -> cadastrar();
        case 2 -> entrar();
        case 0 -> {
          System.out.println("Até mais!");
          return;
        }
        default -> System.out.println("Opção inválida.");
      }
    }
  }

  private static void cadastrar() {
    System.out.println();
    System.out.println("--- Cadastro ---");
    String nome = lerTexto("Nome: ");
    String username = lerTexto("Username: ");
    String senha = lerSenha("Senha: ");
    String confirmacao = lerSenha("Confirme a senha: ");

    if (!senha.equals(confirmacao)) {
      System.out.println("As senhas não conferem.");
      return;
    }

    try {
      Usuario usuario = Usuario.criar(nome, username, senha);
      System.out.println("Cadastro realizado! Bem-vindo(a), " + usuario.getNome() + ". Agora é só entrar.");
    } catch (RuntimeException e) {
      mostrarErro(e);
    }
  }

  private static void entrar() {
    System.out.println();
    System.out.println("--- Entrar ---");
    String username = lerTexto("Username: ");
    String senha = lerSenha("Senha: ");

    try {
      if (!Usuario.autenticar(username, senha)) {
        System.out.println("Username ou senha inválidos.");
        return;
      }

      Usuario usuario = Usuario.consultar(username);

      if (!usuario.isAtivo() && !menuContaInativa(usuario)) {
        return;
      }

      menuUsuario(usuario);
    } catch (RuntimeException e) {
      mostrarErro(e);
    }
  }

  private static boolean menuContaInativa(Usuario usuario) {
    while (true) {
      System.out.println();
      System.out.println("Sua conta está inativa. A única ação disponível é reativá-la.");
      System.out.println("1 - Reativar conta");
      System.out.println("0 - Voltar");

      switch (lerOpcao()) {
        case 1 -> {
          usuario.reativar();
          System.out.println("Conta reativada!");
          return true;
        }
        case 0 -> {
          return false;
        }
        default -> System.out.println("Opção inválida.");
      }
    }
  }

  private static void menuUsuario(Usuario usuario) {
    while (true) {
      System.out.println();
      System.out.println("===== Olá, " + usuario.getNome() + " =====");
      System.out.println("1 - Meu perfil");
      System.out.println("2 - Editar perfil");
      System.out.println("0 - Sair da conta");

      switch (lerOpcao()) {
        case 1 -> verPerfil(usuario);
        case 2 -> editarPerfil(usuario);
        case 0 -> {
          return;
        }
        default -> System.out.println("Opção inválida.");
      }
    }
  }

  private static void verPerfil(Usuario usuario) {
    System.out.println();
    System.out.println("--- Meu perfil ---");
    System.out.println("Nome: " + usuario.getNome());
    System.out.println("Username: " + usuario.getUsername());
    System.out.println("Situação: " + (usuario.isAtivo() ? "ativa" : "inativa"));
  }

  private static void editarPerfil(Usuario usuario) {
    System.out.println();
    System.out.println("--- Editar perfil (deixe em branco para manter) ---");
    String nome = lerTextoOpcional("Nome [" + usuario.getNome() + "]: ");
    String username = lerTextoOpcional("Username [" + usuario.getUsername() + "]: ");
    String senha = lerSenha("Nova senha: ");

    if (!senha.isEmpty() && !senha.equals(lerSenha("Confirme a nova senha: "))) {
      System.out.println("As senhas não conferem.");
      return;
    }

    try {
      usuario.editar(
        nome.isEmpty() ? usuario.getNome() : nome,
        username.isEmpty() ? usuario.getUsername() : username,
        senha
      );
      System.out.println("Perfil atualizado!");
    } catch (RuntimeException e) {
      mostrarErro(e);
    }
  }

  private static int lerOpcao() {
    System.out.print("Escolha: ");
    String linha = entrada.nextLine().trim();

    try {
      return Integer.parseInt(linha);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private static String lerTexto(String rotulo) {
    while (true) {
      String valor = lerTextoOpcional(rotulo);

      if (!valor.isEmpty()) {
        return valor;
      }

      System.out.println("Campo obrigatório.");
    }
  }

  private static String lerTextoOpcional(String rotulo) {
    System.out.print(rotulo);

    return entrada.nextLine().trim();
  }

  private static String lerSenha(String rotulo) {
    Console console = System.console();

    if (console == null) {
      return lerTextoOpcional(rotulo);
    }

    char[] senha = console.readPassword(rotulo);

    return senha == null ? "" : new String(senha);
  }

  private static void mostrarErro(RuntimeException e) {
    System.out.println("Erro: " + e.getMessage());
  }

  private static boolean bancoDisponivel() {
    try (Connection conexao = ConexaoBD.conectar()) {
      return true;
    } catch (SQLException e) {
      return false;
    }
  }
}
