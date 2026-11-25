package br.ufersa.scbt;

import java.io.Console;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import br.ufersa.scbt.db.ConexaoBD;
import br.ufersa.scbt.modelo.Esporte;
import br.ufersa.scbt.modelo.Grupo;
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
      System.out.println("3 - Meus grupos");
      System.out.println("4 - Criar grupo");
      System.out.println("0 - Sair da conta");

      switch (lerOpcao()) {
        case 1 -> verPerfil(usuario);
        case 2 -> editarPerfil(usuario);
        case 3 -> meusGrupos(usuario);
        case 4 -> criarGrupo(usuario);
        case 0 -> {
          return;
        }
        default -> System.out.println("Opção inválida.");
      }
    }
  }

  private static void criarGrupo(Usuario usuario) {
    System.out.println();
    System.out.println("--- Criar grupo ---");
    String nome = lerTexto("Nome do grupo: ");
    String descricao = lerTextoOpcional("Descrição (opcional): ");
    String nomeEsporte = lerTexto("Modalidade esportiva (ex.: Futsal): ");
    int tamanhoEquipe = lerNumero("Jogadores por time: ");

    Esporte esporte = null;

    try {
      if (Grupo.consultar(nome) != null) {
        System.out.println("Erro: Já existe um grupo com o nome \"" + nome + "\".");
        return;
      }

      esporte = Esporte.criar(nomeEsporte, tamanhoEquipe);
      Grupo grupo = Grupo.criar(nome, descricao.isEmpty() ? null : descricao, esporte, usuario);
      System.out.println("Grupo \"" + grupo.getNome() + "\" criado! Você é o administrador.");
    } catch (RuntimeException e) {
      if (esporte != null) {
        Esporte.excluir(esporte.getNome());
      }

      mostrarErro(e);
    }
  }

  private static void meusGrupos(Usuario usuario) {
    try {
      List<Grupo> grupos = Grupo.listarPorUsuario(usuario);

      System.out.println();
      System.out.println("--- Meus grupos ---");

      if (grupos.isEmpty()) {
        System.out.println("Você ainda não participa de nenhum grupo.");
        return;
      }

      for (int i = 0; i < grupos.size(); i++) {
        Grupo grupo = grupos.get(i);
        System.out.println((i + 1) + " - " + grupo.getNome() + " (" + grupo.getEsporte().getNome() + ")");
      }

      System.out.println("0 - Voltar");
      int opcao = lerOpcao();

      if (opcao >= 1 && opcao <= grupos.size()) {
        menuGrupo(usuario, grupos.get(opcao - 1));
      } else if (opcao != 0) {
        System.out.println("Opção inválida.");
      }
    } catch (RuntimeException e) {
      mostrarErro(e);
    }
  }

  private static void menuGrupo(Usuario usuario, Grupo grupo) {
    while (true) {
      boolean administrador = grupo.getAdministrador().getId() == usuario.getId();

      System.out.println();
      System.out.println("===== Grupo: " + grupo.getNome() + " =====");
      System.out.println("Descrição: " + (grupo.getDescricao() == null ? "-" : grupo.getDescricao()));
      System.out.println("Modalidade: " + grupo.getEsporte().getNome()
          + " (" + grupo.getEsporte().getTamanhoEquipe() + " jogadores por time)");
      System.out.println("Administrador: " + grupo.getAdministrador().getNome());
      System.out.println("Criador: " + grupo.getCriador().getNome());
      System.out.println("Criado em: " + grupo.getDataCriacao() + " às " + grupo.getHoraCriacao());

      if (!administrador) {
        System.out.println();
        System.out.println("Apenas o administrador pode alterar o grupo.");
        System.out.println("0 - Voltar");

        if (lerOpcao() == 0) {
          return;
        }

        continue;
      }

      System.out.println();
      System.out.println("1 - Editar grupo");
      System.out.println("2 - Editar modalidade");
      System.out.println("3 - Transferir administração");
      System.out.println("4 - Excluir grupo");
      System.out.println("0 - Voltar");

      try {
        switch (lerOpcao()) {
          case 1 -> editarGrupo(grupo);
          case 2 -> editarModalidade(grupo);
          case 3 -> transferirAdministracao(grupo);
          case 4 -> {
            if (excluirGrupo(grupo)) {
              return;
            }
          }
          case 0 -> {
            return;
          }
          default -> System.out.println("Opção inválida.");
        }
      } catch (RuntimeException e) {
        mostrarErro(e);
      }
    }
  }

  private static void editarGrupo(Grupo grupo) {
    System.out.println("--- Editar grupo (deixe em branco para manter) ---");
    String nome = lerTextoOpcional("Nome [" + grupo.getNome() + "]: ");
    String descricao = lerTextoOpcional("Descrição [" + (grupo.getDescricao() == null ? "-" : grupo.getDescricao()) + "]: ");

    grupo.editar(
      nome.isEmpty() ? grupo.getNome() : nome,
      descricao.isEmpty() ? grupo.getDescricao() : descricao
    );
    System.out.println("Grupo atualizado!");
  }

  private static void editarModalidade(Grupo grupo) {
    Esporte esporte = grupo.getEsporte();

    System.out.println("--- Editar modalidade (deixe em branco para manter) ---");
    String nome = lerTextoOpcional("Modalidade [" + esporte.getNome() + "]: ");
    String tamanho = lerTextoOpcional("Jogadores por time [" + esporte.getTamanhoEquipe() + "]: ");

    esporte.editar(
      nome.isEmpty() ? esporte.getNome() : nome,
      tamanho.isEmpty() ? esporte.getTamanhoEquipe() : converterNumero(tamanho)
    );
    System.out.println("Modalidade atualizada!");
  }

  private static void transferirAdministracao(Grupo grupo) {
    String username = lerTexto("Username do novo administrador: ");
    Usuario novoAdministrador = Usuario.consultar(username);

    if (novoAdministrador == null) {
      System.out.println("Usuário não encontrado.");
      return;
    }

    grupo.transferirAdministracao(novoAdministrador);
    System.out.println("Agora " + novoAdministrador.getNome() + " é o administrador do grupo.");
  }

  private static boolean excluirGrupo(Grupo grupo) {
    System.out.println("Isso apaga o grupo, seus participantes, habilidades e times.");
    String confirmacao = lerTextoOpcional("Digite o nome do grupo para confirmar: ");

    if (!confirmacao.equals(grupo.getNome())) {
      System.out.println("Exclusão cancelada.");
      return false;
    }

    Grupo.excluir(grupo.getNome());
    System.out.println("Grupo excluído.");
    return true;
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

  private static int lerNumero(String rotulo) {
    while (true) {
      try {
        return converterNumero(lerTexto(rotulo));
      } catch (IllegalArgumentException e) {
        System.out.println(e.getMessage());
      }
    }
  }

  private static int converterNumero(String texto) {
    try {
      return Integer.parseInt(texto.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Digite um número inteiro.");
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
