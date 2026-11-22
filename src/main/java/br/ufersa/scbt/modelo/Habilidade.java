package br.ufersa.scbt.modelo;

public class Habilidade {
  private Usuario usuario;
  private Esporte esporte;
  private double valor;

  Habilidade(Usuario usuario, Esporte esporte, double valor) {
    this.usuario = usuario;
    this.esporte = esporte;
    this.valor = valor;
  }
}
