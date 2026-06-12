package com.unibusiness.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.time.LocalDate;

public class TaskCard extends VBox {

    private final VBox infoContainer;
    private final HBox actionContainer;

    public TaskCard( String titulo, String descricao, String prioridade, LocalDate dataFim, String corDataFim ) {

        setSpacing(10);

        setStyle(
                "-fx-padding: 15;" +
                "-fx-background-color: white;" +
                "-fx-background-radius: 7;" +
                "-fx-border-radius: 7;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),8,0,0,2);"
        );

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane prioridadeBadge =
                criarBadgePrioridade(prioridade);

        Label tituloLabel =
                new Label(titulo);

        tituloLabel.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #111827;"
        );

        header.getChildren().addAll(
                prioridadeBadge,
                tituloLabel
        );

        infoContainer = new VBox(4);

        Label descricaoLabel =
                new Label(descricao);
        descricaoLabel.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-text-fill: #374151;"
        );

        descricaoLabel.setWrapText(true);

        Label prazoLabel =
                new Label(dataFim != null ? "Prazo: " + dataFim : "Sem prazo definido");
        prazoLabel.setStyle(
                "-fx-font-size: 12px;" +
                corDataFim
        );

        infoContainer.getChildren().addAll(
                descricaoLabel,
                prazoLabel
        );

        actionContainer = new HBox();
        actionContainer.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(
                header,
                infoContainer,
                actionContainer
        );
    }

    private StackPane criarBadgePrioridade(String prioridade) {
        String cor = "#374151";

        String texto = switch (prioridade.toLowerCase()) {
            case "alta" -> "!!!";
            case "media" -> "!!";
            default -> "!";
        };

        Label label = new Label(texto);

        label.setStyle(
                "-fx-text-fill: " + cor + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;"
        );

        StackPane badge = new StackPane(label);

        badge.setMinSize(28, 28);
        badge.setPrefSize(28, 28);
        badge.setMaxSize(28, 28);

        badge.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 999;" +
                "-fx-border-radius: 999;" +
                "-fx-border-color: " + cor + ";" +
                "-fx-border-width: 1.5;"
        );

        return badge;
    }

    public void adicionarBotao(
            Button button
    ) {
        actionContainer
                .getChildren()
                .add(button);
    }

    public void adicionarInformacao(
            String texto
    ) {
        Label infoLabel = new Label(texto);
        infoLabel.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: #6B7280;"
        );
        infoContainer.getChildren().add(infoLabel);
    }
}