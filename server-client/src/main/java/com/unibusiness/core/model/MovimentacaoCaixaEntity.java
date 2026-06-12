package com.unibusiness.core.model;
import javax.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "movimentacoes_caixa")
public class MovimentacaoCaixaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "caixa_id", nullable = false) private CaixaEntity caixa;
    @Column(nullable = false) private String tipo;
    @Column(nullable = false) private Float valor;
    private String descricao;
    @Column(nullable = false, updatable = false) private LocalDateTime data = LocalDateTime.now();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id", nullable = false) private UsuarioEntity usuario;
    public MovimentacaoCaixaEntity() {}
    public MovimentacaoCaixaEntity(CaixaEntity caixa, String tipo, Float valor, UsuarioEntity usuario) { this.caixa = caixa; this.tipo = tipo; this.valor = valor; this.usuario = usuario; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public CaixaEntity getCaixa() { return caixa; }
    public void setCaixa(CaixaEntity caixa) { this.caixa = caixa; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Float getValor() { return valor; }
    public void setValor(Float valor) { this.valor = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDateTime getData() { return data; }
    public UsuarioEntity getUsuario() { return usuario; }
    public void setUsuario(UsuarioEntity usuario) { this.usuario = usuario; }
}