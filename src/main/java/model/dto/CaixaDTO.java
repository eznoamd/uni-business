package model.dto;

import java.time.LocalDateTime;

public class CaixaDTO {
    private Integer id;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataFechamento;
    private Float saldoInicial;
    private Float saldoFinal;

    // Construtores
    public CaixaDTO() {}

    public CaixaDTO(Integer id, LocalDateTime dataAbertura, LocalDateTime dataFechamento,
                    Float saldoInicial, Float saldoFinal) {
        this.id = id;
        this.dataAbertura = dataAbertura;
        this.dataFechamento = dataFechamento;
        this.saldoInicial = saldoInicial;
        this.saldoFinal = saldoFinal;
    }

    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public Float getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(Float saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public Float getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(Float saldoFinal) {
        this.saldoFinal = saldoFinal;
    }
}
