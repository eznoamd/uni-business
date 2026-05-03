package model.entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "equipes")
public class EquipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nome;

    @ManyToMany(mappedBy = "equipes", fetch = FetchType.LAZY)
    private Set<UsuarioEntity> usuarios = new HashSet<>();

    @ManyToMany(mappedBy = "equipesAtribuidas", fetch = FetchType.LAZY)
    private Set<TarefaEntity> tarefas = new HashSet<>();

    // Construtores
    public EquipeEntity() {}

    public EquipeEntity(String nome) {
        this.nome = nome;
    }

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Set<UsuarioEntity> getUsuarios() { return usuarios; }
    public void setUsuarios(Set<UsuarioEntity> usuarios) { this.usuarios = usuarios; }
    public Set<TarefaEntity> getTarefas() { return tarefas; }
    public void setTarefas(Set<TarefaEntity> tarefas) { this.tarefas = tarefas; }
}
