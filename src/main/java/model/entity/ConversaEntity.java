package model.entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "conversas")
public class ConversaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String tipo;

    @ManyToMany(mappedBy = "conversas", fetch = FetchType.LAZY)
    private Set<UsuarioEntity> participantes = new HashSet<>();

    @OneToMany(mappedBy = "conversa", cascade = CascadeType.REMOVE)
    private Set<MensagemEntity> mensagens = new HashSet<>();

    // Construtores
    public ConversaEntity() {}

    public ConversaEntity(String tipo) {
        this.tipo = tipo;
    }

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Set<UsuarioEntity> getParticipantes() { return participantes; }
    public void setParticipantes(Set<UsuarioEntity> participantes) { this.participantes = participantes; }
    public Set<MensagemEntity> getMensagens() { return mensagens; }
    public void setMensagens(Set<MensagemEntity> mensagens) { this.mensagens = mensagens; }
}
