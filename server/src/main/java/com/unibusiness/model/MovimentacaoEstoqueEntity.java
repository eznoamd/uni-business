package com.unibusiness.model;
import javax.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "movimentacoes_estoque")
public class MovimentacaoEstoqueEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "produto_id", nullable = false) private ProdutoEntity produto;
    @Column(nullable = false) private String tipo;
    @Column(nullable = false) private Integer quantidade;
    @Column(nullable = false, updatable = false) private LocalDateTime data = LocalDateTime.now();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id", nullable = false) private UsuarioEntity usuario;
    public MovimentacaoEstoqueEntity() {}
    public MovimentacaoEstoqueEntity(ProdutoEntity produto, String tipo, Integer quantidade, UsuarioEntity usuario) { this.produto = produto; this.tipo = tipo; this.quantidade = quantidade; this.usuario = usuario; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public ProdutoEntity getProduto() { return produto; }
    public void setProduto(ProdutoEntity produto) { this.produto = produto; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public LocalDateTime getData() { return data; }
    public UsuarioEntity getUsuario() { return usuario; }
    public void setUsuario(UsuarioEntity usuario) { this.usuario = usuario; }
}