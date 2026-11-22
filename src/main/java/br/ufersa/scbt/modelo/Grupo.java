package br.ufersa.scbt.modelo;

import java.sql.Date;
import java.sql.Time;

public class Grupo {
  private int id;
  private String nome;
  private String descricao;
  private Date dataCriacao;
  private Time horaCriacao;
  private Esporte esporte;
  private Usuario administrador;
  private Usuario criador;

  Grupo(int id, String nome, String descricao, Date dataCriacao, Time horaCriacao,
        Esporte esporte, Usuario administrador, Usuario criador) {
    this.id = id;
    this.nome = nome;
    this.descricao = descricao;
    this.dataCriacao = dataCriacao;
    this.horaCriacao = horaCriacao;
    this.esporte = esporte;
    this.administrador = administrador;
    this.criador = criador;
  }
}
