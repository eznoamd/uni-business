package com.unibusiness.core.model;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
@Entity
@Table(name = "tarefas")
public class TarefaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(nullable = false) private String titulo;
    @Column(columnDefinition = "TEXT") private String descricao;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private String prioridade;
    @Column(nullable = false) private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "criado_por", nullable = false) private UsuarioEntity criadoPor;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tarefa_usuario", joinColumns = @JoinColumn(name = "tarefa_id"), inverseJoinColumns = @JoinColumn(name = "usuario_id"))
    private Set<UsuarioEntity> usuariosAtribuidos = new HashSet<>();
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tarefa_equipe", joinColumns = @JoinColumn(name = "tarefa_id"), inverseJoinColumns = @JoinColumn(name = "equipe_id"))
    private Set<EquipeEntity> equipesAtribuidas = new HashSet<>();
    public TarefaEntity() {}
    public TarefaEntity(String titulo, String status, String prioridade, LocalDateTime dataInicio, UsuarioEntity criadoPor) { this.titulo = titulo; this.status = status; this.prioridade = prioridade; this.dataInicio = dataInicio; this.criadoPor = criadoPor; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }
    public LocalDateTime getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDateTime dataInicio) { this.dataInicio = dataInicio; }
    public LocalDateTime getDataFim() { return dataFim; }
    public void setDataFim(LocalDateTime dataFim) { this.dataFim = dataFim; }
    public UsuarioEntity getCriadoPor() { return criadoPor; }
    public void setCriadoPor(UsuarioEntity criadoPor) { this.criadoPor = criadoPor; }
    public Set<UsuarioEntity> getUsuariosAtribuidos() { return usuariosAtribuidos; }
    public void setUsuariosAtribuidos(Set<UsuarioEntity> usuariosAtribuidos) { this.usuariosAtribuidos = usuariosAtribuidos; }
    public Set<EquipeEntity> getEquipesAtribuidas() { return equipesAtribuidas; }
    public void setEquipesAtribuidas(Set<EquipeEntity> equipesAtribuidas) { this.equipesAtribuidas = equipesAtribuidas; }
}