package com.unibusiness.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import java.util.function.UnaryOperator;

public class HomeController {

    @FXML
    private TextField entradaField;

    @FXML
    private TextField saidaField;

    @FXML
    private TextArea observacoesField;

    @FXML
    private void initialize() {
        hourFix(entradaField);
        hourFix(saidaField);
    }

    private void hourFix(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {

            String numeros = newValue.replaceAll("[^0-9]", "");

            if (numeros.length() > 4) {
                numeros = numeros.substring(0, 4);
            }

            String formatado = numeros;

            if (numeros.length() > 2) {
                formatado = numeros.substring(0, 2) + ":" + numeros.substring(2);
            }

            if (!newValue.equals(formatado)) {
                field.setText(formatado);
            }
        });
    }
}