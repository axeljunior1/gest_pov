package com.gestpov.desktop.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class UiTheme {

    private UiTheme() {
    }

    public static Scene scene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        var css = UiTheme.class.getResource("/com/gestpov/desktop/ui/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        return scene;
    }

    public static void apply(Stage stage, Parent root, double width, double height) {
        stage.setScene(scene(root, width, height));
    }
}
