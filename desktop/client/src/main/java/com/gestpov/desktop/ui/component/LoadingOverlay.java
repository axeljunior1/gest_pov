package com.gestpov.desktop.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

public final class LoadingOverlay extends StackPane {

    public LoadingOverlay() {
        getStyleClass().add("overlay");
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(36, 36);
        setAlignment(Pos.CENTER);
        getChildren().add(indicator);
        setVisible(false);
        setManaged(false);
        setMouseTransparent(false);
    }

    public void setLoading(boolean loading) {
        setVisible(loading);
        setManaged(loading);
    }
}
