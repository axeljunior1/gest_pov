package com.gestpov.desktop.ui.license;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.LicenseStatus;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.LicenseClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.nio.file.Files;

public final class LicenseView extends StackPane implements Reloadable {

    private final LicenseClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final Label statusValue = new Label("—");
    private final Label reasonValue = new Label("—");
    private final Label clientValue = new Label("—");
    private final Label siteValue = new Label("—");
    private final Label expiresValue = new Label("—");
    private final Label daysValue = new Label("—");
    private final Label maxUsersValue = new Label("—");
    private final Label installIdValue = new Label("—");
    private final Label heroIdValue = new Label("—");
    private final Label copyFeedback = new Label("");
    private final VBox needLicenseBanner = new VBox(10);
    private final Runnable onActivated;
    private String currentInstallId = "";

    public LicenseView(SessionContext session) {
        this(session, null);
    }

    public LicenseView(SessionContext session, Runnable onActivated) {
        this.client = new LicenseClient(session.api());
        this.onActivated = onActivated;
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Licence");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Statut serveur et import de fichier .lic");
        sub.getStyleClass().add("page-sub");

        needLicenseBanner.getStyleClass().add("license-need-banner");
        needLicenseBanner.setPadding(new Insets(14));
        Label needTitle = new Label("Licence requise — identifiant serveur (server.id)");
        needTitle.getStyleClass().add("license-need-title");
        Label needHint = new Label(
                "Copiez cet identifiant et transmettez-le pour générer le fichier .lic, puis importez-le ci-dessous.");
        needHint.getStyleClass().add("page-sub");
        needHint.setWrapText(true);
        heroIdValue.getStyleClass().add("license-hero-id");
        heroIdValue.setWrapText(true);
        Button copyHero = new Button("Copier server.id");
        copyHero.getStyleClass().add("button-primary");
        copyHero.setOnAction(e -> copyId());
        copyFeedback.getStyleClass().add("page-sub");
        HBox heroRow = new HBox(12, heroIdValue, copyHero);
        heroRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(heroIdValue, Priority.ALWAYS);
        needLicenseBanner.getChildren().setAll(needTitle, needHint, heroRow, copyFeedback);
        needLicenseBanner.setVisible(false);
        needLicenseBanner.setManaged(false);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        addRow(grid, 0, "Statut", statusValue);
        addRow(grid, 1, "Raison", reasonValue);
        addRow(grid, 2, "Client", clientValue);
        addRow(grid, 3, "Site", siteValue);
        addRow(grid, 4, "Expiration", expiresValue);
        addRow(grid, 5, "Jours restants", daysValue);
        addRow(grid, 6, "Utilisateurs max", maxUsersValue);
        addRow(grid, 7, "server.id / installation", installIdValue);

        Button copy = new Button("Copier ID");
        copy.getStyleClass().add("button-ghost");
        copy.setOnAction(e -> copyId());

        Button importBtn = new Button("Importer .lic…");
        importBtn.getStyleClass().add("button-primary");
        importBtn.setOnAction(e -> importLic());

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        HBox actions = new HBox(8, importBtn, refresh, copy);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, grid, actions);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        VBox page = new VBox(16, title, sub, error, needLicenseBanner, card);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void copyId() {
        String id = currentInstallId;
        if (id == null || id.isBlank()) {
            id = installIdValue.getText();
        }
        if (id == null || id.isBlank() || "—".equals(id)) {
            copyFeedback.setText("Identifiant indisponible — actualisez.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(id);
        Clipboard.getSystemClipboard().setContent(content);
        copyFeedback.setText("Copié dans le presse-papiers.");
    }

    private void importLic() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Fichier licence");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Licence", "*.lic", "*.*"));
        var file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> client.importLicense(file.getName(), Files.readAllBytes(file.toPath())), status -> {
            loading.setLoading(false);
            apply(status);
            if (status != null && status.valid() && onActivated != null) {
                onActivated.run();
            }
        }, this::fail);
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> {
            LicenseStatus status = client.status();
            String id = status != null && status.installationId() != null ? status.installationId() : "";
            if (id.isBlank()) {
                try {
                    id = client.installationId();
                } catch (ApiException ignored) {
                    // status peut deja contenir l'id
                }
            }
            return new Loaded(status, id);
        }, loaded -> {
            loading.setLoading(false);
            apply(loaded.status());
            if (loaded.installId() != null && !loaded.installId().isBlank()) {
                currentInstallId = loaded.installId().trim();
                installIdValue.setText(currentInstallId);
                heroIdValue.setText(currentInstallId);
            }
        }, this::fail);
    }

    private void apply(LicenseStatus status) {
        if (status == null) {
            return;
        }
        boolean ok = status.valid();
        statusValue.setText(ok ? "Valide" : (status.activated() ? "Activée (invalide)" : "Non activée"));
        reasonValue.setText(dash(status.reason()));
        clientValue.setText(dash(status.client()));
        siteValue.setText(dash(status.site()));
        expiresValue.setText(dash(status.expiresAt()));
        daysValue.setText(status.daysRemaining() == null ? "—" : String.valueOf(status.daysRemaining()));
        maxUsersValue.setText(status.maxUsers() == null ? "—" : String.valueOf(status.maxUsers()));
        if (status.installationId() != null && !status.installationId().isBlank()) {
            currentInstallId = status.installationId().trim();
        }
        installIdValue.setText(dash(currentInstallId.isBlank() ? status.installationId() : currentInstallId));
        heroIdValue.setText(dash(currentInstallId.isBlank() ? status.installationId() : currentInstallId));
        needLicenseBanner.setVisible(!ok);
        needLicenseBanner.setManaged(!ok);
        if (ok) {
            copyFeedback.setText("");
        }
    }

    private void fail(Throwable t) {
        loading.setLoading(false);
        needLicenseBanner.setVisible(true);
        needLicenseBanner.setManaged(true);
        if (t instanceof ApiException api) {
            String id = api.installationId();
            if (id != null && !id.isBlank()) {
                currentInstallId = id;
                installIdValue.setText(id);
                heroIdValue.setText(id);
            }
            error.show(ApiException.userMessage(api));
            // Fallback ID via endpoint public
            if (currentInstallId.isBlank()) {
                FxAsync.run(client::installationId, fetched -> {
                    if (fetched != null && !fetched.isBlank()) {
                        currentInstallId = fetched.trim();
                        installIdValue.setText(currentInstallId);
                        heroIdValue.setText(currentInstallId);
                    }
                }, ignored -> {
                });
            }
        } else {
            error.show(t.getMessage() == null ? "Erreur" : t.getMessage());
        }
    }

    private static void addRow(GridPane grid, int row, String label, Label value) {
        Label l = new Label(label);
        l.getStyleClass().add("page-sub");
        value.getStyleClass().add("settings-install-id");
        value.setWrapText(true);
        grid.add(l, 0, row);
        grid.add(value, 1, row);
    }

    private static String dash(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    private record Loaded(LicenseStatus status, String installId) {
    }
}
