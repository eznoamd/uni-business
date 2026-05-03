package model.dto;

import java.time.LocalDateTime;

public class MovimentacaoCaixaDTO {
    private Integer id;
    private Integer caixaId;
    private String tipo;
    private Float valor;
    private String descricao;
    private LocalDateTime data;
    private Integer usuarioId;
    private String usuarioNome;

    // Construtores
    public MovimentacaoCaixaDTO() {}

    public MovimentacaoCaixaDTO(Integer id, Integer caixaId, String tipo, Float valor, String descricao,
                                LocalDateTime data, Integer usuarioId, String usuarioNome) {
        this.id = id;
        this.caixaId = caixaId;
        this.tipo = tipo;
        this.valor = valor;
        this.descricao = descricao;
        this.data = data;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
    }

    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCaixaId() {
        return caixaId;
    }

    public void setCaixaId(Integer caixaId) {
        this.caixaId = caixaId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Float getValor() {
        return valor;
    }

    public void setValor(Float valor) {
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
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
}
