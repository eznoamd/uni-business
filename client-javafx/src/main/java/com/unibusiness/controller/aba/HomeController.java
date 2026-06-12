package com.unibusiness.controller.aba;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class HomeController {

    @FXML private TextField entradaField;
    @FXML private TextField saidaField;
    @FXML private TextArea  observacoesField;

    @FXML
    private void initialize() {
        aplicarMascaraHora(entradaField);
        aplicarMascaraHora(saidaField);
    }

    /**
     * Formata o campo para exibir hora no padrão HH:mm conforme o usuário digita.
     * Aceita apenas dígitos e insere o ":" automaticamente após os dois primeiros.
     */
    private void aplicarMascaraHora(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            String numeros = newValue.replaceAll("[^0-9]", "");

            if (numeros.length() > 4) numeros = numeros.substring(0, 4);

            String formatado = numeros.length() > 2
                ? numeros.substring(0, 2) + ":" + numeros.substring(2)
                : numeros;

            if (!newValue.equals(formatado)) {
                field.setText(formatado);
            }
        });
    }
}
