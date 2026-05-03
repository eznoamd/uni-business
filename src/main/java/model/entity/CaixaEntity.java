package model.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "caixa")
public class CaixaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataAbertura = LocalDateTime.now();

    private LocalDateTime dataFechamento;

    @Column(nullable = false)
    private Float saldoInicial = 0F;

    private Float saldoFinal;

    @OneToMany(mappedBy = "caixa", cascade = CascadeType.REMOVE)
    private Set<MovimentacaoCaixaEntity> movimentacoes = new HashSet<>();

    // Construtores
    public CaixaEntity() {}

    public CaixaEntity(Float saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public LocalDateTime getDataFechamento() { return dataFechamento; }
    public void setDataFechamento(LocalDateTime dataFechamento) { this.dataFechamento = dataFechamento; }
    public Float getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(Float saldoInicial) { this.saldoInicial = saldoInicial; }
    public Float getSaldoFinal() { return saldoFinal; }
    public void setSaldoFinal(Float saldoFinal) { this.saldoFinal = saldoFinal; }
    public Set<MovimentacaoCaixaEntity> getMovimentacoes() { return movimentacoes; }
    public void setMovimentacoes(Set<MovimentacaoCaixaEntity> movimentacoes) { this.movimentacoes = movimentacoes; }
}
