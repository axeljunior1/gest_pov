package com.gestpov.desktop;

import com.gestpov.desktop.ui.AppFlow;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Client Desktop Gest POV — discovery, login, module Marques.
 */
public class GestPovDesktopApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        new AppFlow(primaryStage).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
