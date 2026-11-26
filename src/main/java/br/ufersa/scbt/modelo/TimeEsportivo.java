package br.ufersa.scbt.modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.ufersa.scbt.db.ConexaoBD;

public class TimeEsportivo {
  private static final String SELECT_TIME = """
      SELECT t.id AS time_id, t.nome AS time_nome, t.media_habilidade, t.grupo_id,
             u.id AS usuario_id, u.nome AS usuario_nome, u.username, u.senha, u.ativo
      FROM time_esportivo t
      LEFT JOIN time_jogador tj ON tj.time_id = t.id
      LEFT JOIN usuario u ON u.id = tj.usuario_id
      LEFT JOIN habilidade h ON h.usuario_id = u.id AND h.grupo_id = t.grupo_id
      """;
  private static final String ORDEM = " ORDER BY t.grupo_id, t.nome, h.valor DESC, u.nome";

  private int id;
  private String nome;
  private double mediaHabilidade;
  private List<Usuario> jogadores;
  private Grupo grupo;

  TimeEsportivo(int id, String nome, double mediaHabilidade, List<Usuario> jogadores, Grupo grupo) {
    this.id = id;
    this.nome = nome;
    this.mediaHabilidade = mediaHabilidade;
    this.jogadores = new ArrayList<>(jogadores);
    this.grupo = grupo;
  }

  public int getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public double getMediaHabilidade() {
    return mediaHabilidade;
  }

  public List<Usuario> getJogadores() {
    return List.copyOf(jogadores);
  }

  public Grupo getGrupo() {
    return grupo;
  }

  public void atualizarNome(String nome) {
    validarNome(nome);

    if (id > 0) {
      String sql = "UPDATE time_esportivo SET nome = ? WHERE id = ?";

      try (Connection conexao = ConexaoBD.conectar();
           PreparedStatement comando = conexao.prepareStatement(sql)) {
        comando.setString(1, nome);
        comando.setInt(2, id);
        comando.executeUpdate();
      } catch (SQLException e) {
        if ("23505".equals(e.getSQLState())) {
          throw new IllegalArgumentException("Já existe um time chamado \"" + nome + "\" neste grupo.");
        }

        throw new RuntimeException("Erro ao atualizar o nome do time.", e);
      }
    }

    this.nome = nome;
  }

  public double calcularMediaHabilidade() {
    if (jogadores.isEmpty() || grupo == null) {
      mediaHabilidade = 0;
      return mediaHabilidade;
    }

    double soma = 0;

    for (Usuario jogador : jogadores) {
      Habilidade habilidade = Habilidade.consultar(jogador, grupo.getEsporte());
      soma += habilidade == null ? 0 : habilidade.getValor();
    }

    mediaHabilidade = Math.round(soma / jogadores.size() * 100) / 100.0;
    return mediaHabilidade;
  }

  public static TimeEsportivo criar(String nome, List<Usuario> jogadores) {
    validarNome(nome);

    if (jogadores == null || jogadores.isEmpty()) {
      throw new IllegalArgumentException("O time precisa ter pelo menos um jogador.");
    }

    return new TimeEsportivo(0, nome, 0, jogadores, null);
  }

  public static TimeEsportivo consultar(String nome) {
    List<TimeEsportivo> times = buscarLista(SELECT_TIME + " WHERE t.nome = ?" + ORDEM, nome);

    return times.isEmpty() ? null : times.get(0);
  }

  public static void excluir(String nome, Grupo grupo) {
    if (grupo == null) {
      throw new IllegalArgumentException("Informe o grupo.");
    }

    String sql = "DELETE FROM time_esportivo WHERE nome = ? AND grupo_id = ?";

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      comando.setString(1, nome);
      comando.setInt(2, grupo.getId());

      if (comando.executeUpdate() == 0) {
        throw new IllegalArgumentException("Time \"" + nome + "\" não encontrado no grupo.");
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao excluir time.", e);
    }
  }

  public static List<TimeEsportivo> listar() {
    return buscarLista(SELECT_TIME + ORDEM, null);
  }

  static List<TimeEsportivo> balancear(Grupo grupo, List<Usuario> jogadores) {
    if (grupo == null) {
      throw new IllegalArgumentException("Informe o grupo.");
    }

    List<Usuario> selecionados = removerRepetidos(jogadores);

    if (selecionados.size() < 2) {
      throw new IllegalArgumentException("Selecione pelo menos 2 jogadores para balancear.");
    }

    Map<Integer, Integer> antiguidade = new HashMap<>();
    List<Participacao> participacoes = Participacao.listarPorGrupo(grupo);

    for (int i = 0; i < participacoes.size(); i++) {
      antiguidade.put(participacoes.get(i).getUsuario().getId(), i);
    }

    Map<Integer, Double> habilidades = new HashMap<>();

    for (Usuario jogador : selecionados) {
      if (!antiguidade.containsKey(jogador.getId())) {
        throw new IllegalArgumentException(jogador.getNome() + " não participa do grupo.");
      }

      Habilidade habilidade = Habilidade.consultar(jogador, grupo.getEsporte());
      habilidades.put(jogador.getId(), habilidade == null ? 0 : habilidade.getValor());
    }

    selecionados.sort(
      Comparator.comparingDouble((Usuario jogador) -> habilidades.get(jogador.getId())).reversed()
        .thenComparingInt(jogador -> antiguidade.get(jogador.getId()))
    );

    int quantidadeTimes = Math.max(2, selecionados.size() / grupo.getEsporte().getTamanhoEquipe());
    List<List<Usuario>> distribuicao = new ArrayList<>();

    for (int i = 0; i < quantidadeTimes; i++) {
      distribuicao.add(new ArrayList<>());
    }

    for (int i = 0; i < selecionados.size(); i++) {
      int rodada = i / quantidadeTimes;
      int posicao = i % quantidadeTimes;

      if (rodada % 2 == 1) {
        posicao = quantidadeTimes - 1 - posicao;
      }

      distribuicao.get(posicao).add(selecionados.get(i));
    }

    List<TimeEsportivo> times = new ArrayList<>();

    for (int i = 0; i < quantidadeTimes; i++) {
      TimeEsportivo time = criar("Time " + (i + 1), distribuicao.get(i));
      time.grupo = grupo;
      time.calcularMediaHabilidade();
      times.add(time);
    }

    salvarTimes(grupo, times);

    return times;
  }

  private static void salvarTimes(Grupo grupo, List<TimeEsportivo> times) {
    String sqlApagar = "DELETE FROM time_esportivo WHERE grupo_id = ?";
    String sqlTime = "INSERT INTO time_esportivo (grupo_id, nome, media_habilidade) VALUES (?, ?, ?) RETURNING id";
    String sqlJogador = "INSERT INTO time_jogador (time_id, usuario_id) VALUES (?, ?)";

    try (Connection conexao = ConexaoBD.conectar()) {
      conexao.setAutoCommit(false);

      try {
        try (PreparedStatement comando = conexao.prepareStatement(sqlApagar)) {
          comando.setInt(1, grupo.getId());
          comando.executeUpdate();
        }

        for (TimeEsportivo time : times) {
          try (PreparedStatement comando = conexao.prepareStatement(sqlTime)) {
            comando.setInt(1, grupo.getId());
            comando.setString(2, time.nome);
            comando.setDouble(3, time.mediaHabilidade);

            try (ResultSet resultado = comando.executeQuery()) {
              resultado.next();
              time.id = resultado.getInt("id");
            }
          }

          for (Usuario jogador : time.jogadores) {
            try (PreparedStatement comando = conexao.prepareStatement(sqlJogador)) {
              comando.setInt(1, time.id);
              comando.setInt(2, jogador.getId());
              comando.executeUpdate();
            }
          }
        }

        conexao.commit();
      } catch (SQLException e) {
        conexao.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao salvar os times.", e);
    }
  }

  private static List<TimeEsportivo> buscarLista(String sql, String nome) {
    Map<Integer, Grupo> grupos = new HashMap<>();

    for (Grupo grupo : Grupo.listar()) {
      grupos.put(grupo.getId(), grupo);
    }

    Map<Integer, TimeEsportivo> times = new LinkedHashMap<>();

    try (Connection conexao = ConexaoBD.conectar();
         PreparedStatement comando = conexao.prepareStatement(sql)) {
      if (nome != null) {
        comando.setString(1, nome);
      }

      try (ResultSet resultado = comando.executeQuery()) {
        while (resultado.next()) {
          int timeId = resultado.getInt("time_id");
          TimeEsportivo time = times.get(timeId);

          if (time == null) {
            time = new TimeEsportivo(
              timeId,
              resultado.getString("time_nome"),
              resultado.getDouble("media_habilidade"),
              List.of(),
              grupos.get(resultado.getInt("grupo_id"))
            );
            times.put(timeId, time);
          }

          if (resultado.getObject("usuario_id") != null) {
            time.jogadores.add(new Usuario(
              resultado.getInt("usuario_id"),
              resultado.getString("usuario_nome"),
              resultado.getString("username"),
              resultado.getString("senha"),
              resultado.getBoolean("ativo")
            ));
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao buscar times.", e);
    }

    return new ArrayList<>(times.values());
  }

  private static List<Usuario> removerRepetidos(List<Usuario> jogadores) {
    if (jogadores == null) {
      return new ArrayList<>();
    }

    Map<Integer, Usuario> unicos = new LinkedHashMap<>();

    for (Usuario jogador : jogadores) {
      if (jogador != null) {
        unicos.putIfAbsent(jogador.getId(), jogador);
      }
    }

    return new ArrayList<>(unicos.values());
  }

  private static void validarNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("O nome do time é obrigatório.");
    }
  }
}
