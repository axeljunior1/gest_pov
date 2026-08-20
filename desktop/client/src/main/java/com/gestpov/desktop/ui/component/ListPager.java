package com.gestpov.desktop.ui.component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pagination client pour TableView : affiche une page (10/20/50) sur un jeu déjà chargé.
 */
public final class ListPager<T> {

    public static final int DEFAULT_PAGE_SIZE = 20;

    private final TableView<T> table;
    private final Label info = new Label();
    private final ComboBox<Integer> sizeBox = new ComboBox<>();
    private final Button prev = new Button("←");
    private final Button next = new Button("→");
    private final HBox bar = new HBox(10);
    private final List<T> all = new ArrayList<>();
    private int page;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private Consumer<String> onInfo;

    public ListPager(TableView<T> table) {
        this.table = table;
        sizeBox.getItems().addAll(10, 20, 50);
        sizeBox.getSelectionModel().select(Integer.valueOf(DEFAULT_PAGE_SIZE));
        sizeBox.setOnAction(e -> {
            Integer v = sizeBox.getValue();
            pageSize = v == null ? DEFAULT_PAGE_SIZE : v;
            page = 0;
            refresh();
        });
        prev.getStyleClass().add("button-ghost");
        next.getStyleClass().add("button-ghost");
        prev.setOnAction(e -> {
            if (page > 0) {
                page--;
                refresh();
            }
        });
        next.setOnAction(e -> {
            if ((page + 1) * pageSize < all.size()) {
                page++;
                refresh();
            }
        });
        info.getStyleClass().add("page-sub");
        Label sizeLbl = new Label("Par page");
        sizeLbl.getStyleClass().add("page-sub");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.getChildren().addAll(info, spacer, sizeLbl, sizeBox, prev, next);
        bar.setAlignment(Pos.CENTER_LEFT);
        refresh();
    }

    public HBox bar() {
        return bar;
    }

    public void setOnInfo(Consumer<String> onInfo) {
        this.onInfo = onInfo;
    }

    public void setItems(List<T> items) {
        all.clear();
        if (items != null) {
            all.addAll(items);
        }
        page = 0;
        refresh();
    }

    public List<T> allItems() {
        return List.copyOf(all);
    }

    public int totalCount() {
        return all.size();
    }

    private void refresh() {
        int total = all.size();
        int from = Math.min(page * pageSize, total);
        int to = Math.min(from + pageSize, total);
        ObservableList<T> pageItems = FXCollections.observableArrayList(all.subList(from, to));
        table.setItems(pageItems);
        int pages = total == 0 ? 1 : (int) Math.ceil(total / (double) pageSize);
        String text = total == 0
                ? "0 élément"
                : (from + 1) + "–" + to + " / " + total + " · page " + (page + 1) + "/" + pages;
        info.setText(text);
        if (onInfo != null) {
            onInfo.accept(text);
        }
        prev.setDisable(page <= 0);
        next.setDisable(to >= total);
    }
}
