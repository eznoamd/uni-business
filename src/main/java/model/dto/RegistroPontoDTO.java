package model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RegistroPontoDTO {
    private Integer id;
    private Integer usuarioId;
    private String usuarioNome;
    private LocalDate data;
    private LocalDateTime horaEntrada;
    private LocalDateTime horaSaida;
    private String observacao;

    // Construtores
    public RegistroPontoDTO() {}

    public RegistroPontoDTO(Integer id, Integer usuarioId, String usuarioNome, LocalDate data,
                            LocalDateTime horaEntrada, LocalDateTime horaSaida, String observacao) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
        this.data = data;
        this.horaEntrada = horaEntrada;
        this.horaSaida = horaSaida;
        this.observacao = observacao;
    }

    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalDateTime getHoraEntrada() {
        return horaEntrada;
    }

    public void setHoraEntrada(LocalDateTime horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public LocalDateTime getHoraSaida() {
        return horaSaida;
    }

    public void setHoraSaida(LocalDateTime horaSaida) {
        this.horaSaida = horaSaida;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
