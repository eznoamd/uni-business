package com.unibusiness.controller.aba;

import com.unibusiness.component.TaskCard;
import com.unibusiness.dto.Dto;
import com.unibusiness.service.TarefaService;
import com.unibusiness.session.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tela "Minhas Tarefas" — antes mockada, agora carrega as tarefas atribuídas
 * ao usuário logado via TarefaService (mesma fachada usada pela aba
 * "Tarefas" administrativa).
 *
 *   - "Tarefas em andamento"   → status EM_ANDAMENTO, botão "Concluir"
 *   - "Tarefas atribuídas a mim" → status PENDENTE, botão "Iniciar"
 *
 * Tarefas CONCLUIDA não aparecem aqui (mesmo comportamento do mock antigo).
 */
public class MinhasTarefasController {

    @FXML private VBox emAndamentoContainer;
    @FXML private VBox atribuidasContainer;

    private final TarefaService tarefaService = new TarefaService();

    @FXML
    private void initialize() {
        carregarTarefas();
    }

    private void carregarTarefas() {
        Integer usuarioId = SessionManager.getInstance().getUsuarioId();

        Task<List<Dto.Tarefa>> task = new Task<>() {
            @Override protected List<Dto.Tarefa> call() {
                return tarefaService.listar().stream()
                    .filter(t -> usuarioId != null && usuarioId.equals(t.getResponsavelId()))
                    .collect(Collectors.toList());
            }
        };

        task.setOnSucceeded(e -> renderizar(task.getValue()));
        new Thread(task, "load-minhas-tarefas").start();
    }

    private void renderizar(List<Dto.Tarefa> tarefas) {
        Platform.runLater(() -> {
            emAndamentoContainer.getChildren().clear();
            atribuidasContainer.getChildren().clear();

            List<Dto.Tarefa> emAndamento = tarefas.stream()
                .filter(t -> "EM_ANDAMENTO".equals(t.getStatus())).collect(Collectors.toList());
            List<Dto.Tarefa> pendentes = tarefas.stream()
                .filter(t -> "PENDENTE".equals(t.getStatus())).collect(Collectors.toList());

            if (emAndamento.isEmpty()) {
                emAndamentoContainer.getChildren().add(mensagemVazia("Nenhuma tarefa em andamento."));
            } else {
                emAndamento.forEach(this::adicionarTarefaEmAndamento);
            }

            if (pendentes.isEmpty()) {
                atribuidasContainer.getChildren().add(mensagemVazia("Nenhuma tarefa pendente atribuída a você."));
            } else {
                pendentes.forEach(this::adicionarTarefaAtribuida);
            }
        });
    }

    private Label mensagemVazia(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        return l;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void adicionarTarefaEmAndamento(Dto.Tarefa t) {
        LocalDate dataFim = parseDataFim(t.getDataFim());
        TaskCard card = new TaskCard(t.getTitulo(), descricaoOuVazio(t), prioridadeOuPadrao(t),
            dataFim, getCorDataFim(dataFim));

        Button concluir = new Button("Concluir");
        concluir.getStyleClass().addAll("btn", "btn-success");
        concluir.setOnAction(e -> alterarStatus(t, "CONCLUIDA"));

        card.adicionarBotao(concluir);
        emAndamentoContainer.getChildren().add(card);
    }

    private void adicionarTarefaAtribuida(Dto.Tarefa t) {
        LocalDate dataFim = parseDataFim(t.getDataFim());
        TaskCard card = new TaskCard(t.getTitulo(), descricaoOuVazio(t), prioridadeOuPadrao(t),
            dataFim, getCorDataFim(dataFim));

        Button iniciar = new Button("Iniciar");
        iniciar.getStyleClass().addAll("btn", "btn-primary");
        iniciar.setOnAction(e -> alterarStatus(t, "EM_ANDAMENTO"));

        card.adicionarBotao(iniciar);
        atribuidasContainer.getChildren().add(card);
    }

    private void alterarStatus(Dto.Tarefa t, String novoStatus) {
        new Thread(() -> {
            tarefaService.atualizar(t.getId(), novoStatus, prioridadeOuPadrao(t));
            Platform.runLater(this::carregarTarefas);
        }, "atualizar-tarefa").start();
    }

    private String descricaoOuVazio(Dto.Tarefa t) {
        return t.getDescricao() != null ? t.getDescricao() : "";
    }

    private String prioridadeOuPadrao(Dto.Tarefa t) {
        return t.getPrioridade() != null ? t.getPrioridade() : "BAIXA";
    }

    /** Converte "AAAA-MM-DDTHH:mm:ss" (ou null) em LocalDate. */
    private LocalDate parseDataFim(String dataFim) {
        if (dataFim == null || dataFim.isBlank()) return null;
        try {
            return LocalDate.parse(dataFim.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String getCorDataFim(LocalDate dataFim) {
        if (dataFim == null) return "-fx-text-fill: #9CA3AF;";
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), dataFim);
        if (dias < 0)  return "-fx-text-fill: #e64c4c;";
        if (dias <= 2) return "-fx-text-fill: #ffa341;";
        return "-fx-text-fill: #374151;";
    }
}
