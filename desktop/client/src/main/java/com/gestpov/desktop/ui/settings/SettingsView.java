package com.gestpov.desktop.ui.settings;

import com.gestpov.desktop.model.AppSetting;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.SettingsClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class SettingsView extends StackPane {

    private final SessionContext session;
    private final SettingsClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<AppSetting> table = new TableView<>();
    private final TextField value = new TextField();
    private AppSetting selected;

    public SettingsView(SessionContext session) {
        this.session = session;
        this.client = new SettingsClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Paramètres");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Réglages serveur (entreprise, stock, POS, fidélité)");
        sub.getStyleClass().add("page-sub");
        value.setPromptText("Valeur");
        Button save = new Button("Enregistrer");
        save.getStyleClass().add("button-primary");
        save.setOnAction(e -> save());
        save.setVisible(session.hasPermission("settings.update"));
        save.setManaged(session.hasPermission("settings.update"));
        HBox edit = new HBox(8, value, save);
        edit.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(value, Priority.ALWAYS);
        VBox card = new VBox(edit);
        card.getStyleClass().add("card");
        table.setPlaceholder(new EmptyState("Aucun paramètre"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(FXCollections.observableArrayList());
        table.getColumns().add(col("Clé", AppSetting::key));
        table.getColumns().add(col("Valeur", AppSetting::value));
        table.getColumns().add(col("Description", s -> s.description() == null ? "" : s.description()));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            selected = b;
            value.setText(b == null ? "" : b.value());
        });
        VBox page = new VBox(16, title, sub, error, card, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void save() {
        if (selected == null) {
            error.show("Sélectionnez un paramètre.");
            return;
        }
        loading.setLoading(true);
        String key = selected.key();
        String val = value.getText();
        FxAsync.run(() -> client.update(key, val), updated -> reload(), this::fail);
    }

    public void reload() {
        loading.setLoading(true);
        FxAsync.run(client::getAll, list -> {
            loading.setLoading(false);
            table.getItems().setAll(list);
        }, this::fail);
    }

    private void fail(Throwable t) {
        loading.setLoading(false);
        if (t instanceof ApiException api && !api.isUnauthorized()) {
            error.show(ApiException.userMessage(api));
        } else if (!(t instanceof ApiException a && a.isUnauthorized())) {
            error.show("Une erreur est survenue.");
        }
    }

    private static TableColumn<AppSetting, String> col(String title, java.util.function.Function<AppSetting, String> fn) {
        TableColumn<AppSetting, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return col;
    }
}
