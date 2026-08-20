package com.gestpov.desktop.ui.component;

import javafx.scene.control.Label;

public final class EmptyState extends Label {

    public EmptyState(String message) {
        super(message);
        getStyleClass().add("empty-state");
        setWrapText(true);
        setMaxWidth(Double.MAX_VALUE);
    }

    public void setMessage(String message) {
        setText(message);
    }
}
