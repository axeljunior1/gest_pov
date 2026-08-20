package com.gestpov.desktop.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class ErrorBanner extends HBox {

    private final Label text = new Label();

    public ErrorBanner() {
        getStyleClass().add("error-banner");
        text.getStyleClass().add("error-banner-text");
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);
        Button dismiss = new Button("Fermer");
        dismiss.getStyleClass().add("button-ghost");
        dismiss.setOnAction(e -> hide());
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        getChildren().addAll(text, dismiss);
        hide();
    }

    public void show(String message) {
        text.setText(message == null ? "" : message);
        setVisible(true);
        setManaged(true);
    }

    public void hide() {
        text.setText("");
        setVisible(false);
        setManaged(false);
    }
}
