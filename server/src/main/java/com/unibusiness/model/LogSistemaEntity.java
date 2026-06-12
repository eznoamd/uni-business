package com.unibusiness.model;
import javax.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "logs_sistema")
public class LogSistemaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id", nullable = false) private UsuarioEntity usuario;
    @Column(nullable = false) private String acao;
    @Column(nullable = false, updatable = false) private LocalDateTime data = LocalDateTime.now();
    @Column(columnDefinition = "TEXT") private String detalhes;
    public LogSistemaEntity() {}
    public LogSistemaEntity(UsuarioEntity usuario, String acao) { this.usuario = usuario; this.acao = acao; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public UsuarioEntity getUsuario() { return usuario; }
    public void setUsuario(UsuarioEntity usuario) { this.usuario = usuario; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public LocalDateTime getData() { return data; }
    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
}