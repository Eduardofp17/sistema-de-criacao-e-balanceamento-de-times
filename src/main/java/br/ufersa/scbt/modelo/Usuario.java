package br.ufersa.scbt.modelo;

public class Usuario {
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
}
