package com.unibusiness;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import com.unibusiness.manager.ViewManager;


public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        ViewManager.setPrimaryStage(stage);
        ViewManager.switchPrimaryTo("/views/login.fxml");
    }

    public static void main(String[] args) {
        launch();
    }
}