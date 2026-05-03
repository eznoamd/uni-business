package model.dto;

import java.time.LocalDateTime;

public class TarefaDTO {
    private Integer id;
    private String titulo;
    private String descricao;
    private String status;
    private String prioridade;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Integer criadoPorId;
    private String criadoPorNome;

    // Construtores
    public TarefaDTO() {}

    public TarefaDTO(Integer id, String titulo, String descricao, String status, String prioridade,
                     LocalDateTime dataInicio, LocalDateTime dataFim, Integer criadoPorId, String criadoPorNome) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.status = status;
        this.prioridade = prioridade;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.criadoPorId = criadoPorId;
        this.criadoPorNome = criadoPorNome;
    }

    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }

    public Integer getCriadoPorId() {
        return criadoPorId;
    }

    public void setCriadoPorId(Integer criadoPorId) {
        this.criadoPorId = criadoPorId;
    }

    public String getCriadoPorNome() {
        return criadoPorNome;
    }

    public void setCriadoPorNome(String criadoPorNome) {
        this.criadoPorNome = criadoPorNome;
    }
}
