package br.ufersa.scbt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import br.ufersa.scbt.db.ConexaoBD;

public class App {
  public static void main(String[] args) {
    try (Connection conexao = ConexaoBD.conectar();
         Statement comando = conexao.createStatement();
         ResultSet resultado = comando.executeQuery("SELECT version()")) {
      resultado.next();

      System.out.println("Conectado ao banco: " + resultado.getString(1));
    } catch (SQLException e) {
      System.out.println("Erro ao conectar no banco: " + e.getMessage());
    }
  }
}
