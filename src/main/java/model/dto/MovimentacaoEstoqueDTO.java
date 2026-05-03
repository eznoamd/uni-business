package model.dto;

import java.time.LocalDateTime;

public class MovimentacaoEstoqueDTO {
    private Integer id;
    private Integer produtoId;
    private String produtoNome;
    private String tipo;
    private Integer quantidade;
    private LocalDateTime data;
    private Integer usuarioId;
    private String usuarioNome;

    // Construtores
    public MovimentacaoEstoqueDTO() {}

    public MovimentacaoEstoqueDTO(Integer id, Integer produtoId, String produtoNome, String tipo,
                                  Integer quantidade, LocalDateTime data, Integer usuarioId, String usuarioNome) {
        this.id = id;
        this.produtoId = produtoId;
        this.produtoNome = produtoNome;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.data = data;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
    }

    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Integer produtoId) {
        this.produtoId = produtoId;
    }

    public String getProdutoNome() {
        return produtoNome;
    }

    public void setProdutoNome(String produtoNome) {
        this.produtoNome = produtoNome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }
}
