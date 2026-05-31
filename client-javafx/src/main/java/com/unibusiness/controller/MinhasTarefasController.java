package com.unibusiness.controller;

import com.unibusiness.component.TaskCard;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;

public class MinhasTarefasController {

    @FXML
    private VBox emAndamentoContainer;

    @FXML
    private VBox atribuidasContainer;

    @FXML
    private void initialize() {
        adicionarTarefaEmAndamento(
                "Corrigir bug no login",
                "Usuário não consegue autenticar após troca de senha.",
                "alta",
                LocalDate.now().plusDays(1)
        );

        adicionarTarefaEmAndamento(
                "Implementar dashboard",
                "Criar os gráficos financeiros da página inicial.",
                "media",
                LocalDate.now().plusDays(5)
        );

        adicionarTarefaAtribuida(
                "Atualizar documentação",
                "Atualizar o manual do sistema.",
                "baixa",
                "Equipe Desenvolvimento",
                LocalDate.now().plusDays(7)
        );

        adicionarTarefaAtribuida(
                "Corrigir API de autenticação",
                "Problema na renovação do token JWT.",
                "alta",
                "Diretamente para você",
                LocalDate.now().minusDays(1)
        );
    }

    private void adicionarTarefaEmAndamento(
            String titulo,
            String descricao,
            String prioridade,
            LocalDate dataFim
    ) {

        TaskCard card =
                new TaskCard(
                        titulo,
                        descricao,
                        prioridade,
                        dataFim,
                        getCorDataFim(dataFim)
                );

        Button concluir =
                new Button("Concluir");
        concluir.getStyleClass().addAll("btn", "btn-success");

        concluir.setOnAction(event ->
                emAndamentoContainer
                        .getChildren()
                        .remove(card)
        );

        card.adicionarBotao(concluir);

        emAndamentoContainer
                .getChildren()
                .add(card);
    }

    private void adicionarTarefaAtribuida(
            String titulo,
            String descricao,
            String prioridade,
            String origem,
            LocalDate dataFim
    ) {

        TaskCard card =
                new TaskCard(
                        titulo,
                        descricao,
                        prioridade,
                        dataFim,
                        getCorDataFim(dataFim)
                );

        card.adicionarInformacao(
                "Origem: " + origem
        );

        Button iniciar =
                new Button("Iniciar");
        iniciar.getStyleClass().addAll("btn", "btn-primary");

        iniciar.setOnAction(event -> {

            atribuidasContainer
                    .getChildren()
                    .remove(card);

            adicionarTarefaEmAndamento(
                    titulo,
                    descricao,
                    prioridade,
                    dataFim
            );
        });

        card.adicionarBotao(iniciar);

        atribuidasContainer
                .getChildren()
                .add(card);
    }

    private String getCorDataFim(
            LocalDate dataFim
    ) {

        long dias =
                ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        dataFim
                );

        if (dias < 0) {
            return "-fx-text-fill: #e64c4c;";
        }

        if (dias <= 2) {
            return "-fx-text-fill: #ffa341;";
        }

        return "-fx-text-fill: #374151;";
    }
}