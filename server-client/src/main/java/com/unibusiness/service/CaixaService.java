package com.unibusiness.service;

import com.unibusiness.core.model.CaixaEntity;
import com.unibusiness.core.model.MovimentacaoCaixaEntity;
import com.unibusiness.core.service.impl.CaixaServiceImpl;
import com.unibusiness.dto.Dto;
import com.unibusiness.session.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de caixa — antes via TCP (CAIXA_*), agora via CaixaService (JPA) direto.
 */
public class CaixaService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final com.unibusiness.core.service.CaixaService caixaService = new CaixaServiceImpl();

    /** Id do caixa aberto atualmente — preenchido por getCaixaAtual(). */
    private Integer caixaAtualId;

    public Dto.Caixa getCaixaAtual() {
        return caixaService.getAtual().map(c -> {
            caixaAtualId = c.getId();
            List<MovimentacaoCaixaEntity> movs = caixaService.listarMovimentacoes(c.getId());
            return toDto(c, movs);
        }).orElseGet(() -> {
            caixaAtualId = null;
            return null;
        });
    }

    /**
     * Quando NÃO há caixa aberto, retorna o último caixa fechado (com o
     * saldoFinal já calculado no momento do fechamento) — usado pela tela
     * pra mostrar "qual foi o saldo de fechamento" em vez de "—".
     */
    public Dto.Caixa getUltimoFechado() {
        return caixaService.getUltimoFechado()
            .map(c -> toDto(c, List.of()))
            .orElse(null);
    }

    public boolean abrirCaixa(double saldoInicial) {
        try {
            CaixaEntity c = caixaService.abrir((float) saldoInicial);
            caixaAtualId = c.getId();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean fecharCaixa() {
        if (caixaAtualId == null) return false;
        try {
            caixaService.fechar(caixaAtualId, null);
            caixaAtualId = null;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean movimentar(String tipo, double valor, String descricao) {
        if (caixaAtualId == null) return false;
        try {
            caixaService.movimentar(caixaAtualId, tipo, (float) valor, descricao,
                SessionManager.getInstance().getUsuario());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Dto.MovimentacaoCaixa> listarMovimentacoes() {
        if (caixaAtualId == null) return List.of();
        return caixaService.listarMovimentacoes(caixaAtualId).stream()
            .map(CaixaService::toDto)
            .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Dto.Caixa toDto(CaixaEntity c, List<MovimentacaoCaixaEntity> movs) {
        Dto.Caixa dto = new Dto.Caixa();
        dto.id = c.getId();
        dto.status = c.getDataFechamento() == null ? "ABERTO" : "FECHADO";
        dto.saldoInicial = c.getSaldoInicial() != null ? c.getSaldoInicial().doubleValue() : 0.0;
        dto.saldoAtual = c.getDataFechamento() != null
            ? (c.getSaldoFinal() != null ? c.getSaldoFinal().doubleValue() : 0.0)
            : calcularSaldoAtual(c, movs);
        dto.aberturaEm = c.getDataAbertura() != null ? c.getDataAbertura().format(ISO) : null;
        dto.fechamentoEm = c.getDataFechamento() != null ? c.getDataFechamento().format(ISO) : null;
        return dto;
    }

    private static double calcularSaldoAtual(CaixaEntity c, List<MovimentacaoCaixaEntity> movs) {
        double saldo = c.getSaldoInicial() != null ? c.getSaldoInicial() : 0.0;
        for (MovimentacaoCaixaEntity m : movs) {
            if ("ENTRADA".equals(m.getTipo())) saldo += m.getValor();
            else if ("SAIDA".equals(m.getTipo())) saldo -= m.getValor();
        }
        return saldo;
    }

    private static Dto.MovimentacaoCaixa toDto(MovimentacaoCaixaEntity m) {
        Dto.MovimentacaoCaixa dto = new Dto.MovimentacaoCaixa();
        dto.id = m.getId();
        dto.tipo = m.getTipo();
        dto.valor = m.getValor() != null ? m.getValor().doubleValue() : 0.0;
        dto.descricao = m.getDescricao();
        dto.realizadaEm = m.getData() != null ? m.getData().format(ISO) : null;
        return dto;
    }
}
