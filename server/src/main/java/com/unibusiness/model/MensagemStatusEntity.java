package com.unibusiness.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Rastreia o status de leitura de cada mensagem por usuário.
 *
 * Para cada mensagem enviada, é criada uma linha desta entidade
 * para cada participante da conversa (exceto o próprio remetente,
 * que já "leu" ao enviar).
 *
 * Isso permite:
 *  - Contar não lidas por conversa por usuário
 *  - Saber exatamente quando cada um leu
 *  - Enviar push de "mensagens perdidas" no login
 */
@Entity
@Table(
    name = "mensagem_status",
    uniqueConstraints = @UniqueConstraint(columnNames = {"mensagem_id", "usuario_id"})
)
public class MensagemStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mensagem_id", nullable = false)
    private MensagemEntity mensagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private Boolean lida = false;

    private LocalDateTime lidaEm;

    // ── Construtores ──────────────────────────────────────────────────────────

    public MensagemStatusEntity() {}

    public MensagemStatusEntity(MensagemEntity mensagem, UsuarioEntity usuario) {
        this.mensagem = mensagem;
        this.usuario  = usuario;
        this.lida     = false;
    }

    // ── Métodos de negócio ────────────────────────────────────────────────────

    public void marcarComoLida() {
        this.lida   = true;
        this.lidaEm = LocalDateTime.now();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Integer getId()             { return id; }
    public MensagemEntity getMensagem(){ return mensagem; }
    public UsuarioEntity getUsuario()  { return usuario; }
    public Boolean getLida()           { return lida; }
    public LocalDateTime getLidaEm()   { return lidaEm; }

    public void setMensagem(MensagemEntity mensagem) { this.mensagem = mensagem; }
    public void setUsuario(UsuarioEntity usuario)    { this.usuario  = usuario; }
    public void setLida(Boolean lida)                { this.lida     = lida; }
    public void setLidaEm(LocalDateTime lidaEm)      { this.lidaEm   = lidaEm; }
}
