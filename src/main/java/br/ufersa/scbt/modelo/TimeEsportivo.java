package br.ufersa.scbt.modelo;

import java.util.ArrayList;
import java.util.List;

public class TimeEsportivo {
  private int id;
  private String nome;
  private double mediaHabilidade;
  private List<Usuario> jogadores;

  TimeEsportivo(int id, String nome, double mediaHabilidade, List<Usuario> jogadores) {
    this.id = id;
    this.nome = nome;
    this.mediaHabilidade = mediaHabilidade;
    this.jogadores = new ArrayList<>(jogadores);
  }
}
