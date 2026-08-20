package com.gestpov.desktop.ui.admin;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.ImportJob;
import com.gestpov.desktop.model.ImportPreview;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.ImportExportClient;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ImportExportView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final ImportExportClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<ImportJob> history = new TableView<>();
    private final Label previewLabel = new Label("Aucun aperçu");

    public ImportExportView(SessionContext session) {
        this.session = session;
        this.client = new ImportExportClient(session.api());
        getChildren().addAll(build(), loading);
        reloadHistory();
    }

    private VBox build() {
        Label title = new Label("Import / Export");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Templates, import produits, exports CSV");
        sub.getStyleClass().add("page-sub");
        previewLabel.getStyleClass().add("page-sub");

        Button template = new Button("Template produits CSV");
        template.getStyleClass().add("button-secondary");
        template.setDisable(!session.hasPermission("import.read"));
        template.setOnAction(e -> download("template-produits.csv", () -> client.productTemplate("CSV")));

        Button preview = new Button("Aperçu import produits…");
        preview.getStyleClass().add("button-secondary");
        preview.setDisable(!session.hasPermission("import.read"));
        preview.setOnAction(e -> pickAndPreview());

        Button validate = new Button("Valider import produits…");
        validate.getStyleClass().add("button-primary");
        validate.setDisable(!session.hasPermission("import.create"));
        validate.setOnAction(e -> pickAndValidate());

        Button expProducts = new Button("Exporter produits");
        expProducts.getStyleClass().add("button-secondary");
        expProducts.setDisable(!session.hasPermission("export.read"));
        expProducts.setOnAction(e -> download("products.csv", () -> client.exportProducts("CSV")));

        Button expStock = new Button("Exporter stock");
        expStock.getStyleClass().add("button-secondary");
        expStock.setDisable(!session.hasPermission("export.read"));
        expStock.setOnAction(e -> download("stock.csv", () -> client.exportStock("CSV")));

        Button expAlerts = new Button("Exporter alertes");
        expAlerts.getStyleClass().add("button-secondary");
        expAlerts.setDisable(!session.hasPermission("export.read"));
        expAlerts.setOnAction(e -> download("alerts.csv", () -> client.exportAlerts("CSV")));

        Button refresh = new Button("Historique");
        refresh.getStyleClass().add("button-ghost");
        refresh.setOnAction(e -> reloadHistory());

        HBox actions = new HBox(8, template, preview, validate, expProducts, expStock, expAlerts, refresh);
        actions.setAlignment(Pos.CENTER_LEFT);

        history.setPlaceholder(new EmptyState("Aucun job d'import"));
        history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        history.getColumns().addAll(
                col("Id", j -> String.valueOf(j.id())),
                col("Type", ImportJob::importType),
                col("Statut", ImportJob::status),
                col("Fichier", ImportJob::fileName),
                col("Lignes", j -> j.successRows() + "/" + j.totalRows()),
                col("Créé", j -> j.createdAt() == null ? "" : j.createdAt())
        );
        VBox.setVgrow(history, Priority.ALWAYS);

        VBox page = new VBox(16, title, sub, error, actions, previewLabel, history);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private void pickAndPreview() {
        Path file = chooseFile();
        if (file == null) {
            return;
        }
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> {
            byte[] bytes = Files.readAllBytes(file);
            return client.previewProducts(file.getFileName().toString(), bytes, "REJECT");
        }, this::showPreview, this::fail);
    }

    private void pickAndValidate() {
        Path file = chooseFile();
        if (file == null) {
            return;
        }
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> {
            byte[] bytes = Files.readAllBytes(file);
            return client.validateProducts(file.getFileName().toString(), bytes, "REJECT");
        }, preview -> {
            showPreview(preview);
            reloadHistory();
        }, this::fail);
    }

    private void showPreview(ImportPreview preview) {
        loading.setLoading(false);
        previewLabel.setText("Aperçu : " + preview.totalRows() + " lignes, "
                + preview.validRows() + " OK, " + preview.errorRows() + " erreurs");
    }

    private void download(String suggested, ThrowingBytesSupplier supplier) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer");
        chooser.setInitialFileName(suggested);
        var file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        error.hide();
        loading.setLoading(true);
        FxAsync.run(supplier::get, bytes -> {
            try {
                Files.write(file.toPath(), bytes);
                loading.setLoading(false);
                previewLabel.setText("Fichier enregistré : " + file.getAbsolutePath());
            } catch (Exception ex) {
                fail(ex);
            }
        }, this::fail);
    }

    private Path chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Fichier d'import");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV / Excel", "*.csv", "*.xlsx", "*.xls"));
        var file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        return file == null ? null : file.toPath();
    }

    @Override
    public void reload() {
        reloadHistory();
    }

    private void reloadHistory() {
        if (!session.hasPermission("import.read")) {
            return;
        }
        error.hide();
        loading.setLoading(true);
        FxAsync.run(client::history, list -> {
            loading.setLoading(false);
            history.setItems(FXCollections.observableArrayList(list));
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

    private static TableColumn<ImportJob, String> col(String title, java.util.function.Function<ImportJob, String> fn) {
        TableColumn<ImportJob, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(fn.apply(cd.getValue()) == null ? "" : fn.apply(cd.getValue())));
        return c;
    }

    @FunctionalInterface
    private interface ThrowingBytesSupplier {
        byte[] get() throws Exception;
    }
}
