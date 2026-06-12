package com.unibusiness.core.model;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
@Entity
@Table(name = "conversas")
public class ConversaEntity {
    public enum Tipo { PRIVADA, GRUPO }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Tipo tipo;
    @Column(length = 120) private String nome;
    @ManyToMany(mappedBy = "conversas", fetch = FetchType.LAZY) private Set<UsuarioEntity> participantes = new HashSet<>();
    @OneToMany(mappedBy = "conversa", cascade = CascadeType.REMOVE) private Set<MensagemEntity> mensagens = new HashSet<>();
    public ConversaEntity() {}
    public ConversaEntity(Tipo tipo, String nome) { this.tipo = tipo; this.nome = nome; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Set<UsuarioEntity> getParticipantes() { return participantes; }
    public void setParticipantes(Set<UsuarioEntity> participantes) { this.participantes = participantes; }
    public Set<MensagemEntity> getMensagens() { return mensagens; }
    public void setMensagens(Set<MensagemEntity> mensagens) { this.mensagens = mensagens; }
}