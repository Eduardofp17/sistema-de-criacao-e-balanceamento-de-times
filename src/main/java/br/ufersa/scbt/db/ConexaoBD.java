package br.ufersa.scbt.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoBD {
  private static final String URL = valorOuPadrao("DB_URL", "jdbc:postgresql://localhost:5432/balanceamento_times");
  private static final String USUARIO = valorOuPadrao("DB_USER", "times");
  private static final String SENHA = valorOuPadrao("DB_PASSWORD", "times");

  private ConexaoBD() {
  }

  public static Connection conectar() throws SQLException {
    return DriverManager.getConnection(URL, USUARIO, SENHA);
  }

  private static String valorOuPadrao(String variavel, String padrao) {
    String valor = System.getenv(variavel);

    return valor != null ? valor : padrao;
  }
}
