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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ImportExportView extends StackPane implements Reloadable {

    private static final String TEMPLATE_UNAVAILABLE =
            "Template d'import indisponible. Redémarrez ou mettez à jour le serveur Gest POV.";

    private static final Map<String, String> IMPORT_TYPES = new LinkedHashMap<>();
    private static final Map<String, String> EXPORT_TYPES = new LinkedHashMap<>();

    static {
        IMPORT_TYPES.put("products", "Produits");
        IMPORT_TYPES.put("brands", "Marques");
        IMPORT_TYPES.put("categories", "Catégories");
        IMPORT_TYPES.put("suppliers", "Fournisseurs");
        IMPORT_TYPES.put("units", "Unités");
        IMPORT_TYPES.put("warehouses", "Entrepôts");
        IMPORT_TYPES.put("packagings", "Conditionnements");
        IMPORT_TYPES.put("initial-stock", "Stock initial");

        EXPORT_TYPES.put("products", "Produits");
        EXPORT_TYPES.put("brands", "Marques");
        EXPORT_TYPES.put("categories", "Catégories");
        EXPORT_TYPES.put("suppliers", "Fournisseurs");
        EXPORT_TYPES.put("units", "Unités");
        EXPORT_TYPES.put("warehouses", "Entrepôts");
        EXPORT_TYPES.put("stock", "Stock");
        EXPORT_TYPES.put("alerts", "Alertes");
    }

    private final SessionContext session;
    private final ImportExportClient client;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<ImportJob> history = new TableView<>();
    private final Label previewLabel = new Label("Aucun aperçu");
    private final ComboBox<TypeOption> importType = new ComboBox<>();
    private final ComboBox<String> duplicateMode = new ComboBox<>();

    public ImportExportView(SessionContext session) {
        this.session = session;
        this.client = new ImportExportClient(session.api());
        getChildren().addAll(build(), loading);
        reloadHistory();
    }

    private VBox build() {
        Label title = new Label("Import / Export");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Templates, import référentiels / produits, exports CSV");
        sub.getStyleClass().add("page-sub");
        previewLabel.getStyleClass().add("page-sub");

        for (Map.Entry<String, String> entry : IMPORT_TYPES.entrySet()) {
            importType.getItems().add(new TypeOption(entry.getKey(), entry.getValue()));
        }
        importType.getSelectionModel().selectFirst();

        duplicateMode.getItems().addAll("REJECT", "UPDATE", "SKIP");
        duplicateMode.setValue("REJECT");
        duplicateMode.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String mode) {
                if ("UPDATE".equals(mode)) return "Existant → mettre à jour";
                if ("SKIP".equals(mode)) return "Existant → ignorer";
                return "Existant → refuser";
            }

            @Override
            public String fromString(String string) {
                if (string != null && string.contains("mettre à jour")) return "UPDATE";
                if (string != null && string.contains("ignorer")) return "SKIP";
                return "REJECT";
            }
        });
        importType.valueProperty().addListener((obs, o, n) ->
                duplicateMode.setDisable(!supportsDuplicateMode(selectedImportKey())));
        duplicateMode.setDisable(false);

        Button template = new Button("Télécharger template");
        template.getStyleClass().add("button-secondary");
        template.setDisable(!session.hasPermission("import.read"));
        template.setOnAction(e -> downloadTemplate());

        Button preview = new Button("Aperçu import…");
        preview.getStyleClass().add("button-secondary");
        preview.setDisable(!session.hasPermission("import.read"));
        preview.setOnAction(e -> pickAndPreview());

        Button validate = new Button("Valider import…");
        validate.getStyleClass().add("button-primary");
        validate.setDisable(!session.hasPermission("import.create"));
        validate.setOnAction(e -> pickAndValidate());

        HBox importRow = new HBox(8, importType, duplicateMode, template, preview, validate);
        importRow.setAlignment(Pos.CENTER_LEFT);

        FlowPane exports = new FlowPane(8, 8);
        for (Map.Entry<String, String> entry : EXPORT_TYPES.entrySet()) {
            Button btn = new Button("Exporter " + entry.getValue());
            btn.getStyleClass().add("button-secondary");
            btn.setDisable(!session.hasPermission("export.read"));
            String type = entry.getKey();
            btn.setOnAction(e -> downloadExport(type + ".csv", () -> client.export(type, "CSV")));
            exports.getChildren().add(btn);
        }

        Button refresh = new Button("Historique");
        refresh.getStyleClass().add("button-ghost");
        refresh.setOnAction(e -> reloadHistory());
        exports.getChildren().add(refresh);

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

        VBox page = new VBox(16, title, sub, error, importRow, exports, previewLabel, history);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private String selectedImportKey() {
        TypeOption selected = importType.getValue();
        return selected == null ? "products" : selected.key();
    }

    private static boolean supportsDuplicateMode(String type) {
        return type != null
                && !"packagings".equals(type)
                && !"initial-stock".equals(type);
    }

    private String duplicateModeOrNull() {
        String type = selectedImportKey();
        return supportsDuplicateMode(type) ? duplicateMode.getValue() : null;
    }

    private void pickAndPreview() {
        Path file = chooseFile();
        if (file == null) {
            return;
        }
        error.hide();
        loading.setLoading(true);
        String type = selectedImportKey();
        String mode = duplicateModeOrNull();
        FxAsync.run(() -> {
            byte[] bytes = Files.readAllBytes(file);
            return client.preview(type, file.getFileName().toString(), bytes, mode);
        }, this::showPreview, this::fail);
    }

    private void pickAndValidate() {
        Path file = chooseFile();
        if (file == null) {
            return;
        }
        error.hide();
        loading.setLoading(true);
        String type = selectedImportKey();
        String mode = duplicateModeOrNull();
        FxAsync.run(() -> {
            byte[] bytes = Files.readAllBytes(file);
            return client.validate(type, file.getFileName().toString(), bytes, mode);
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

    /** Télécharge le template API avant le FileChooser pour afficher 404/403 sans dialogue. */
    private void downloadTemplate() {
        String type = selectedImportKey();
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> client.template(type, "CSV"), bytes -> {
            loading.setLoading(false);
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Enregistrer");
            chooser.setInitialFileName("template-" + type + ".csv");
            var file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
            if (file == null) {
                return;
            }
            try {
                Files.write(file.toPath(), bytes);
                previewLabel.setText("Fichier enregistré : " + file.getAbsolutePath());
            } catch (Exception ex) {
                fail(ex);
            }
        }, this::failTemplate);
    }

    private void downloadExport(String suggested, ThrowingBytesSupplier supplier) {
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

    private void failTemplate(Throwable t) {
        loading.setLoading(false);
        if (t instanceof ApiException api) {
            if (api.statusCode() == 404) {
                error.show(TEMPLATE_UNAVAILABLE);
                return;
            }
            if (api.statusCode() == 403) {
                error.show(ApiException.userMessage(api));
                return;
            }
            error.show(ApiException.userMessage(api));
            return;
        }
        error.show(t.getMessage() == null ? "Erreur" : t.getMessage());
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

    /** Clé API + libellé FR — évite d'envoyer « Marques » au lieu de « brands ». */
    static final class TypeOption {
        private final String key;
        private final String label;

        TypeOption(String key, String label) {
            this.key = key;
            this.label = label;
        }

        String key() {
            return key;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @FunctionalInterface
    private interface ThrowingBytesSupplier {
        byte[] get() throws Exception;
    }
}
