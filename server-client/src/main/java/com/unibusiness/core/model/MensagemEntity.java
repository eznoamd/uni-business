package com.unibusiness.core.model;
import javax.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "mensagens")
public class MensagemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversa_id", nullable = false) private ConversaEntity conversa;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "remetente_id", nullable = false) private UsuarioEntity remetente;
    @Column(nullable = false, columnDefinition = "TEXT") private String conteudo;
    @Column(nullable = false, updatable = false) private LocalDateTime enviadoEm = LocalDateTime.now();
    public MensagemEntity() {}
    public MensagemEntity(ConversaEntity conversa, UsuarioEntity remetente, String conteudo) { this.conversa = conversa; this.remetente = remetente; this.conteudo = conteudo; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public ConversaEntity getConversa() { return conversa; }
    public void setConversa(ConversaEntity conversa) { this.conversa = conversa; }
    public UsuarioEntity getRemetente() { return remetente; }
    public void setRemetente(UsuarioEntity remetente) { this.remetente = remetente; }
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    public LocalDateTime getEnviadoEm() { return enviadoEm; }
}