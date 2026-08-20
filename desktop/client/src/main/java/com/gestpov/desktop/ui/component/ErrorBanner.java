package com.gestpov.desktop.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Bandeau d'erreur avec texte scrollable (messages API longs).
 */
public final class ErrorBanner extends HBox {

    private static final double MAX_TEXT_HEIGHT = 140;

    private final Label text = new Label();
    private final ScrollPane scroll = new ScrollPane();

    public ErrorBanner() {
        getStyleClass().add("error-banner");
        text.getStyleClass().add("error-banner-text");
        text.setWrapText(true);
        text.setMaxWidth(Double.MAX_VALUE);

        scroll.setContent(text);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxHeight(MAX_TEXT_HEIGHT);
        scroll.setPrefHeight(USE_COMPUTED_SIZE);
        scroll.getStyleClass().add("error-banner-scroll");
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        HBox.setHgrow(scroll, Priority.ALWAYS);

        Button dismiss = new Button("Fermer");
        dismiss.getStyleClass().add("button-ghost");
        dismiss.setOnAction(e -> hide());
        dismiss.setMinWidth(Button.USE_PREF_SIZE);

        setAlignment(Pos.TOP_LEFT);
        setSpacing(8);
        getChildren().addAll(scroll, dismiss);
        hide();
    }

    public void show(String message) {
        String msg = message == null ? "" : message;
        text.setText(msg);
        // Hauteur dynamique plafonnée pour forcer l'ascenseur si besoin
        text.applyCss();
        double needed = Math.min(MAX_TEXT_HEIGHT, Math.max(40, estimateHeight(msg)));
        scroll.setPrefHeight(needed);
        scroll.setVvalue(0);
        setVisible(true);
        setManaged(true);
    }

    public void hide() {
        text.setText("");
        setVisible(false);
        setManaged(false);
    }

    private static double estimateHeight(String msg) {
        int lines = 1;
        for (int i = 0; i < msg.length(); i++) {
            if (msg.charAt(i) == '\n') {
                lines++;
            }
        }
        // approx wrap: ~80 chars / line
        lines = Math.max(lines, (msg.length() / 80) + 1);
        return 18 + lines * 18.0;
    }
}
