package model.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_cargo",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "cargo_id")
    )
    private Set<CargoEntity> cargos = new HashSet<>();

    @OneToMany(mappedBy = "criadoPor", cascade = CascadeType.REMOVE)
    private Set<TarefaEntity> tarefas = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tarefa_usuario",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "tarefa_id")
    )
    private Set<TarefaEntity> tarefasAtribuidas = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "equipe_usuario",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "equipe_id")
    )
    private Set<EquipeEntity> equipes = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "conversa_participantes",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "conversa_id")
    )
    private Set<ConversaEntity> conversas = new HashSet<>();

    @OneToMany(mappedBy = "remetente", cascade = CascadeType.REMOVE)
    private Set<MensagemEntity> mensagens = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.REMOVE)
    private Set<RegistroPontoEntity> registrosPonto = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.REMOVE)
    private Set<MovimentacaoEstoqueEntity> movimentacoesEstoque = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.REMOVE)
    private Set<MovimentacaoCaixaEntity> movimentacoesCaixa = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.REMOVE)
    private Set<LogSistemaEntity> logs = new HashSet<>();

    // Construtores
    public UsuarioEntity() {}

    public UsuarioEntity(String nome, String email, String senhaHash) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.ativo = true;
    }

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public Set<CargoEntity> getCargos() { return cargos; }
    public void setCargos(Set<CargoEntity> cargos) { this.cargos = cargos; }
    public Set<TarefaEntity> getTarefas() { return tarefas; }
    public void setTarefas(Set<TarefaEntity> tarefas) { this.tarefas = tarefas; }
    public Set<TarefaEntity> getTarefasAtribuidas() { return tarefasAtribuidas; }
    public void setTarefasAtribuidas(Set<TarefaEntity> tarefasAtribuidas) { this.tarefasAtribuidas = tarefasAtribuidas; }
    public Set<EquipeEntity> getEquipes() { return equipes; }
    public void setEquipes(Set<EquipeEntity> equipes) { this.equipes = equipes; }
    public Set<ConversaEntity> getConversas() { return conversas; }
    public void setConversas(Set<ConversaEntity> conversas) { this.conversas = conversas; }
    public Set<MensagemEntity> getMensagens() { return mensagens; }
    public void setMensagens(Set<MensagemEntity> mensagens) { this.mensagens = mensagens; }
    public Set<RegistroPontoEntity> getRegistrosPonto() { return registrosPonto; }
    public void setRegistrosPonto(Set<RegistroPontoEntity> registrosPonto) { this.registrosPonto = registrosPonto; }
    public Set<MovimentacaoEstoqueEntity> getMovimentacoesEstoque() { return movimentacoesEstoque; }
    public void setMovimentacoesEstoque(Set<MovimentacaoEstoqueEntity> movimentacoesEstoque) { this.movimentacoesEstoque = movimentacoesEstoque; }
    public Set<MovimentacaoCaixaEntity> getMovimentacoesCaixa() { return movimentacoesCaixa; }
    public void setMovimentacoesCaixa(Set<MovimentacaoCaixaEntity> movimentacoesCaixa) { this.movimentacoesCaixa = movimentacoesCaixa; }
    public Set<LogSistemaEntity> getLogs() { return logs; }
    public void setLogs(Set<LogSistemaEntity> logs) { this.logs = logs; }
}
