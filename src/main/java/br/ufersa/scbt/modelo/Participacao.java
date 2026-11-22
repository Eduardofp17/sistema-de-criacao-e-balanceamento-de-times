package br.ufersa.scbt.modelo;

import java.sql.Date;
import java.sql.Time;

public class Participacao {
  private Usuario usuario;
  private Grupo grupo;
  private Date dataEntrada;
  private Time horaEntrada;

  Participacao(Usuario usuario, Grupo grupo, Date dataEntrada, Time horaEntrada) {
    this.usuario = usuario;
    this.grupo = grupo;
    this.dataEntrada = dataEntrada;
    this.horaEntrada = horaEntrada;
  }
}
