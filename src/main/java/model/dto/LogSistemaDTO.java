package model.dto;

import java.time.LocalDateTime;

public class LogSistemaDTO {
    private Integer id;
    private Integer usuarioId;
    private String usuarioNome;
    private String acao;
    private LocalDateTime data;
    private String detalhes;

    // Construtores
    public LogSistemaDTO() {}

    public LogSistemaDTO(Integer id, Integer usuarioId, String usuarioNome, String acao,
                         LocalDateTime data, String detalhes) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
        this.acao = acao;
        this.data = data;
        this.detalhes = detalhes;
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

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }
}
