package com.gestpov.desktop.ui.admin;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.Role;
import com.gestpov.desktop.model.UserAccount;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.RoleClient;
import com.gestpov.desktop.net.UserClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.ConfirmationDialog;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.ListPager;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public final class UsersView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final UserClient users;
    private final RoleClient roles;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<UserAccount> table = new TableView<>();
    private final ListPager<UserAccount> pager = new ListPager<>(table);
    private final TextField firstName = new TextField();
    private final TextField lastName = new TextField();
    private final TextField email = new TextField();
    private final PasswordField password = new PasswordField();
    private final TextField badgeCode = new TextField();
    private final PasswordField pin = new PasswordField();
    private final CheckBox active = new CheckBox("Actif");
    private final ComboBox<Role> rolePick = new ComboBox<>();
    private final Button save = new Button("Créer");
    private Long editingId;
    private List<Role> allRoles = List.of();

    public UsersView(SessionContext session) {
        this.session = session;
        this.users = new UserClient(session.api());
        this.roles = new RoleClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Utilisateurs");
        title.getStyleClass().add("page-title");
        Label sub = new Label("CRUD comptes et rôles");
        sub.getStyleClass().add("page-sub");

        firstName.setPromptText("Prénom *");
        lastName.setPromptText("Nom *");
        email.setPromptText("Email *");
        password.setPromptText("Mot de passe");
        badgeCode.setPromptText("Code badge (optionnel)");
        pin.setPromptText("PIN (optionnel)");
        active.setSelected(true);
        rolePick.setPromptText("Rôle");
        rolePick.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Role r) {
                return r == null ? "" : r.name() + " (" + r.code() + ")";
            }

            @Override
            public Role fromString(String s) {
                return null;
            }
        });

        save.getStyleClass().add("button-primary");
        save.setOnAction(e -> save());
        Button cancel = new Button("Annuler");
        cancel.getStyleClass().add("button-ghost");
        cancel.setOnAction(e -> reset());
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        HBox row1 = new HBox(8, firstName, lastName, email, password);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox row2 = new HBox(8, badgeCode, pin, rolePick, active, save, cancel, refresh);
        row2.setAlignment(Pos.CENTER_LEFT);
        VBox form = new VBox(8, row1, row2);
        boolean canWrite = session.hasPermission("users.create") || session.hasPermission("users.update");
        form.setVisible(canWrite);
        form.setManaged(canWrite);

        table.setPlaceholder(new EmptyState("Aucun utilisateur"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("Nom", UserAccount::displayName),
                col("Email", UserAccount::email),
                col("Badge", u -> u.badgeCode() == null || u.badgeCode().isBlank() ? "—" : u.badgeCode()),
                col("Actif", u -> Boolean.TRUE.equals(u.isActive()) ? "Oui" : "Non"),
                col("Rôles", u -> String.join(", ", u.roles() == null ? List.of() : u.roles()))
        );
        TableColumn<UserAccount, Void> actions = new TableColumn<>();
        actions.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                UserAccount u = getTableRow().getItem();
                HBox box = new HBox(6);
                if (session.hasPermission("users.update")) {
                    Button edit = new Button("Modifier");
                    edit.getStyleClass().add("button-ghost");
                    edit.setOnAction(e -> beginEdit(u));
                    box.getChildren().add(edit);
                }
                if (session.hasPermission("users.delete")) {
                    Button del = new Button("Suppr.");
                    del.getStyleClass().add("button-danger");
                    del.setOnAction(e -> delete(u));
                    box.getChildren().add(del);
                }
                setGraphic(box.getChildren().isEmpty() ? null : box);
            }
        });
        table.getColumns().add(actions);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox page = new VBox(16, title, sub, error, form, table, pager.bar());
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void beginEdit(UserAccount u) {
        editingId = u.id();
        firstName.setText(u.firstName());
        lastName.setText(u.lastName());
        email.setText(u.email());
        password.clear();
        password.setPromptText("Laisser vide = inchangé");
        badgeCode.setText(u.badgeCode() == null ? "" : u.badgeCode());
        pin.clear();
        pin.setPromptText("Laisser vide = inchangé");
        active.setSelected(Boolean.TRUE.equals(u.isActive()));
        if (u.roles() != null && !u.roles().isEmpty()) {
            String codeOrName = u.roles().get(0);
            allRoles.stream()
                    .filter(r -> codeOrName.equals(r.code()) || codeOrName.equals(r.name()))
                    .findFirst()
                    .ifPresent(rolePick::setValue);
        }
        save.setText("Enregistrer");
    }

    private void reset() {
        editingId = null;
        firstName.clear();
        lastName.clear();
        email.clear();
        password.clear();
        password.setPromptText("Mot de passe");
        badgeCode.clear();
        badgeCode.setPromptText("Code badge (optionnel)");
        pin.clear();
        pin.setPromptText("PIN (optionnel)");
        active.setSelected(true);
        rolePick.getSelectionModel().clearSelection();
        save.setText("Créer");
    }

    private void save() {
        error.hide();
        String fn = firstName.getText() == null ? "" : firstName.getText().trim();
        String ln = lastName.getText() == null ? "" : lastName.getText().trim();
        String em = email.getText() == null ? "" : email.getText().trim();
        if (fn.isBlank() || ln.isBlank() || em.isBlank()) {
            error.show("Prénom, nom et email obligatoires.");
            return;
        }
        Role role = rolePick.getValue();
        if (role == null || role.id() == null) {
            error.show("Sélectionnez un rôle.");
            return;
        }
        List<Long> roleIds = List.of(role.id());
        String pwd = password.getText();
        String badge = badgeCode.getText();
        String pinValue = pin.getText();
        loading.setLoading(true);
        if (editingId == null) {
            if (pwd == null || pwd.isBlank()) {
                loading.setLoading(false);
                error.show("Mot de passe obligatoire à la création.");
                return;
            }
            FxAsync.run(() -> users.create(fn, ln, em, pwd, badge, pinValue, active.isSelected(), roleIds),
                    ignored -> {
                        loading.setLoading(false);
                        reset();
                        reload();
                    }, this::fail);
        } else {
            Long id = editingId;
            FxAsync.run(() -> users.update(id, fn, ln, em, pwd, badge, pinValue, active.isSelected(), roleIds),
                    ignored -> {
                        loading.setLoading(false);
                        reset();
                        reload();
                    }, this::fail);
        }
    }

    private void delete(UserAccount u) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer", "Supprimer " + u.displayName() + " ?")) {
            return;
        }
        loading.setLoading(true);
        FxAsync.runVoid(() -> users.delete(u.id()), this::reload, this::fail);
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> {
            List<Role> roleList = roles.list();
            List<UserAccount> userList = users.list();
            return new Object[]{roleList, userList};
        }, data -> {
            loading.setLoading(false);
            @SuppressWarnings("unchecked")
            List<Role> roleList = (List<Role>) data[0];
            @SuppressWarnings("unchecked")
            List<UserAccount> userList = (List<UserAccount>) data[1];
            allRoles = roleList == null ? List.of() : roleList;
            rolePick.setItems(FXCollections.observableArrayList(allRoles));
            pager.setItems(userList == null ? List.of() : userList);
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

    private static TableColumn<UserAccount, String> col(String title, java.util.function.Function<UserAccount, String> fn) {
        TableColumn<UserAccount, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(fn.apply(cd.getValue()) == null ? "" : fn.apply(cd.getValue())));
        return c;
    }
}
