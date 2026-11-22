package br.ufersa.scbt.modelo;

public class Esporte {
  private int id;
  private String nome;
  private int tamanhoEquipe;

  Esporte(int id, String nome, int tamanhoEquipe) {
    this.id = id;
    this.nome = nome;
    this.tamanhoEquipe = tamanhoEquipe;
  }
}
