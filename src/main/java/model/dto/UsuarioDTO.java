package model.dto;

import java.time.LocalDateTime;

public class UsuarioDTO {
    private Integer id;
    private String nome;
    private String email;
    private Boolean ativo;
    private LocalDateTime criadoEm;

    // Construtores
    public UsuarioDTO() {}

    public UsuarioDTO(Integer id, String nome, String email, Boolean ativo, LocalDateTime criadoEm) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
    }

    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
