package model.dto;

import java.time.LocalDateTime;

public class MensagemDTO {
    private Integer id;
    private Integer conversaId;
    private Integer remetenteId;
    private String remetenteNome;
    private String conteudo;
    private LocalDateTime enviadoEm;

    // Construtores
    public MensagemDTO() {}

    public MensagemDTO(Integer id, Integer conversaId, Integer remetenteId, String remetenteNome,
                       String conteudo, LocalDateTime enviadoEm) {
        this.id = id;
        this.conversaId = conversaId;
        this.remetenteId = remetenteId;
        this.remetenteNome = remetenteNome;
        this.conteudo = conteudo;
        this.enviadoEm = enviadoEm;
    }

    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getConversaId() {
        return conversaId;
    }

    public void setConversaId(Integer conversaId) {
        this.conversaId = conversaId;
    }

    public Integer getRemetenteId() {
        return remetenteId;
    }

    public void setRemetenteId(Integer remetenteId) {
        this.remetenteId = remetenteId;
    }

    public String getRemetenteNome() {
        return remetenteNome;
    }

    public void setRemetenteNome(String remetenteNome) {
        this.remetenteNome = remetenteNome;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public LocalDateTime getEnviadoEm() {
        return enviadoEm;
    }

    public void setEnviadoEm(LocalDateTime enviadoEm) {
        this.enviadoEm = enviadoEm;
    }
}
