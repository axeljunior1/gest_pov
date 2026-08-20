package com.gestpov.desktop.ui.admin;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.Permission;
import com.gestpov.desktop.model.Role;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.RoleClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RolesView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final RoleClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<Role> table = new TableView<>();
    private final ListView<CheckBox> permList = new ListView<>();
    private final Label detailTitle = new Label("Sélectionnez un rôle");
    private final Button savePerms = new Button("Enregistrer permissions");
    private List<Permission> allPerms = List.of();
    private Role selected;

    public RolesView(SessionContext session) {
        this.session = session;
        this.client = new RoleClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Rôles");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Matrice de permissions par rôle");
        sub.getStyleClass().add("page-sub");

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        table.setItems(FXCollections.observableArrayList());
        table.setPlaceholder(new EmptyState("Aucun rôle"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("Nom", Role::name),
                col("Code", Role::code),
                col("Système", r -> Boolean.TRUE.equals(r.isSystem()) ? "Oui" : "Non"),
                col("Permissions", r -> String.valueOf(r.permissions() == null ? 0 : r.permissions().size()))
        );
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                showRole(b);
            }
        });
        HBox.setHgrow(table, Priority.ALWAYS);

        detailTitle.getStyleClass().add("settings-group-title");
        permList.setPrefWidth(360);
        savePerms.getStyleClass().add("button-primary");
        savePerms.setDisable(!session.hasPermission("roles.update"));
        savePerms.setOnAction(e -> savePermissions());
        VBox detail = new VBox(10, detailTitle, permList, savePerms);
        detail.getStyleClass().add("card");
        detail.setPadding(new Insets(12));
        VBox.setVgrow(permList, Priority.ALWAYS);

        HBox split = new HBox(16, table, detail);
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox page = new VBox(16, title, sub, error, refresh, split);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void showRole(Role role) {
        selected = role;
        detailTitle.setText(role.name() + " (" + role.code() + ")");
        Set<String> current = new HashSet<>(role.permissions() == null ? List.of() : role.permissions());
        List<CheckBox> boxes = new ArrayList<>();
        for (Permission p : allPerms) {
            CheckBox cb = new CheckBox(p.code() + (p.name() == null || p.name().isBlank() ? "" : " — " + p.name()));
            cb.setSelected(current.contains(p.code()));
            cb.setUserData(p.code());
            cb.setDisable(!session.hasPermission("roles.update"));
            boxes.add(cb);
        }
        permList.setItems(FXCollections.observableArrayList(boxes));
    }

    private void savePermissions() {
        if (selected == null || selected.id() == null) {
            return;
        }
        List<String> codes = new ArrayList<>();
        for (CheckBox cb : permList.getItems()) {
            if (cb.isSelected() && cb.getUserData() instanceof String code) {
                codes.add(code);
            }
        }
        error.hide();
        loading.setLoading(true);
        Long id = selected.id();
        FxAsync.run(() -> client.updatePermissions(id, codes), updated -> {
            loading.setLoading(false);
            reload();
        }, this::fail);
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> {
            List<Role> roles = client.list();
            List<Permission> perms = client.listPermissions();
            return new Object[]{roles, perms};
        }, data -> {
            loading.setLoading(false);
            @SuppressWarnings("unchecked")
            List<Role> roles = (List<Role>) data[0];
            @SuppressWarnings("unchecked")
            List<Permission> perms = (List<Permission>) data[1];
            allPerms = perms == null ? List.of() : perms;
            table.setItems(FXCollections.observableArrayList(roles == null ? List.of() : roles));
            if (selected != null && selected.id() != null) {
                roles.stream().filter(r -> selected.id().equals(r.id())).findFirst().ifPresent(this::showRole);
            }
        }, this::fail);
    }

    private void fail(Throwable t) {
        loading.setLoading(false);
        if (t instanceof ApiException api) {
            error.show(ApiException.userMessage(api));
        } else {
            error.show(t.getMessage() == null ? "Erreur" : t.getMessage());
        }
    }

    private static TableColumn<Role, String> col(String title, java.util.function.Function<Role, String> fn) {
        TableColumn<Role, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(fn.apply(cd.getValue()) == null ? "" : fn.apply(cd.getValue())));
        return c;
    }
}
