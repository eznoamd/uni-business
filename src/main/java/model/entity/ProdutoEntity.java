package model.entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "produtos")
public class ProdutoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private Integer quantidade = 0;

    @Column(nullable = false)
    private Float precoUnitario;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.REMOVE)
    private Set<MovimentacaoEstoqueEntity> movimentacoes = new HashSet<>();

    // Construtores
    public ProdutoEntity() {}

    public ProdutoEntity(String nome, Float precoUnitario) {
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = 0;
    }

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public Float getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(Float precoUnitario) { this.precoUnitario = precoUnitario; }
    public Set<MovimentacaoEstoqueEntity> getMovimentacoes() { return movimentacoes; }
    public void setMovimentacoes(Set<MovimentacaoEstoqueEntity> movimentacoes) { this.movimentacoes = movimentacoes; }
}
