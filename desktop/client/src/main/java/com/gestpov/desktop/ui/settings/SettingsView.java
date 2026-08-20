package com.gestpov.desktop.ui.settings;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.AppSetting;
import com.gestpov.desktop.model.ClientConfiguration;
import com.gestpov.desktop.model.ReferenceValueOption;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.SettingsClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Paramètres généraux alignés sur le front web : groupes, types, listes de valeurs, save bulk.
 */
public final class SettingsView extends StackPane implements Reloadable {

    private static final List<SettingGroup> GROUPS = List.of(
            new SettingGroup("Entreprise & affichage", List.of(
                    "company.name", "company.logo", "company.address", "company.city", "company.country",
                    "company.phone", "company.email", "company.tax_id",
                    "app.currency", "app.language", "app.timezone", "app.date_format"
            )),
            new SettingGroup("Stock & alertes", List.of(
                    "stock.allow_negative", "stock.low_threshold_default", "stock.valuation_method",
                    "alert.expiry_days_default"
            )),
            new SettingGroup("Numérotation documents", List.of(
                    "numbering.entry_prefix", "numbering.exit_prefix", "numbering.inventory_prefix",
                    "numbering.movement_prefix", "numbering.sale_prefix"
            )),
            new SettingGroup("Point de vente (POS)", List.of(
                    "pos_sales_flow_mode",
                    "pos.allow_seller_cash_collection",
                    "pos.allow_partial_payment",
                    "pos.allow_split_payment",
                    "pos.max_pending_payment_duration",
                    "pos.alert.pending_payment_minutes",
                    "pos.alert.cash_difference_threshold",
                    "pos.require_manager_validation_for_cash_difference",
                    "pos.register_name",
                    "pos.default_warehouse_code",
                    "pos.tax_rate_default",
                    "pos.ticket_footer",
                    "pos.ticket_show_logo"
            )),
            new SettingGroup("Fidélité", List.of(
                    "loyalty.enabled", "loyalty.points_per_currency_unit", "loyalty.currency_unit_amount",
                    "loyalty.point_value", "loyalty.minimum_points_to_redeem", "loyalty.maximum_discount_percent",
                    "loyalty.points_expiration_enabled", "loyalty.points_expiration_days",
                    "loyalty.earn_points_on_discounted_sales", "loyalty.earn_points_on_tax_included_amount",
                    "loyalty.allow_points_redemption", "loyalty.tiers_config"
            ))
    );

    private final SessionContext session;
    private final SettingsClient client;
    private final boolean canUpdate;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final Label installIdValue = new Label("—");
    private final Label logoStatus = new Label("Logo : —");
    private final VBox groupsBox = new VBox(16);
    private final ClientConfigurationView clientConfigView;
    private final Map<String, Supplier<String>> editors = new LinkedHashMap<>();
    private Map<String, AppSetting> byKey = Map.of();
    private Map<String, List<ReferenceValueOption>> referenceValues = Map.of();
    private final Runnable openLicense;

    public SettingsView(SessionContext session) {
        this(session, null);
    }

    public SettingsView(SessionContext session, Runnable openLicense) {
        this.session = session;
        this.client = new SettingsClient(session.api());
        this.canUpdate = session.hasPermission("settings.update");
        this.openLicense = openLicense;
        this.clientConfigView = new ClientConfigurationView(session, loading);
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Paramètres");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Configuration centralisée (entreprise, stock, POS, fidélité)");
        sub.getStyleClass().add("page-sub");

        Button save = new Button("Enregistrer tout");
        save.getStyleClass().add("button-primary");
        save.setDisable(!canUpdate);
        save.setOnAction(e -> saveAll());
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, title, spacer, refresh, save);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox installCard = buildInstallationCard();
        VBox logoCard = buildLogoCard();
        groupsBox.getStyleClass().add("settings-groups");

        ScrollPane scroll = new ScrollPane(new VBox(16, installCard, logoCard, clientConfigView, groupsBox));
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("settings-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox page = new VBox(12, header, sub, error, scroll);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        return page;
    }

    private VBox buildLogoCard() {
        Label h = new Label("Logo entreprise");
        h.getStyleClass().add("settings-group-title");
        logoStatus.getStyleClass().add("page-sub");
        Button choose = new Button("Choisir un fichier…");
        choose.getStyleClass().add("button-secondary");
        choose.setDisable(!canUpdate);
        choose.setOnAction(e -> chooseLogo());
        Label hint = new Label("Upload multipart /api/settings/company/logo (sinon chemin stocké dans company.logo).");
        hint.getStyleClass().add("page-sub");
        VBox card = new VBox(10, h, logoStatus, choose, hint);
        card.getStyleClass().add("card");
        return card;
    }

    private void chooseLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Logo entreprise");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.svg"));
        java.io.File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> {
            byte[] bytes = Files.readAllBytes(file.toPath());
            try {
                return client.uploadCompanyLogo(file.getName(), bytes);
            } catch (ApiException uploadEx) {
                // fallback : stocker le chemin absolu dans company.logo
                client.update("company.logo", file.getAbsolutePath());
                return null;
            }
        }, cfg -> {
            loading.setLoading(false);
            if (cfg != null) {
                updateLogoStatus(cfg);
            } else {
                logoStatus.setText("Logo : chemin enregistré → " + file.getAbsolutePath());
            }
            reload();
        }, this::fail);
    }

    private void updateLogoStatus(ClientConfiguration cfg) {
        if (cfg == null) {
            logoStatus.setText("Logo : —");
            return;
        }
        String path = cfg.logoPath();
        String url = cfg.logoUrl();
        if (url != null && !url.isBlank()) {
            logoStatus.setText("Logo : " + url);
        } else if (path != null && !path.isBlank()) {
            logoStatus.setText("Logo : " + path);
        } else {
            logoStatus.setText("Logo : —");
        }
    }

    private VBox buildInstallationCard() {
        Label h = new Label("Identifiant d'installation (licence)");
        h.getStyleClass().add("settings-group-title");
        installIdValue.getStyleClass().add("settings-install-id");
        installIdValue.setWrapText(true);
        Button copy = new Button("Copier");
        copy.getStyleClass().add("button-ghost");
        copy.setOnAction(e -> {
            String id = installIdValue.getText();
            if (id == null || id.isBlank() || "—".equals(id)) {
                return;
            }
            ClipboardContent content = new ClipboardContent();
            content.putString(id);
            Clipboard.getSystemClipboard().setContent(content);
        });
        HBox row = new HBox(12, installIdValue, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(installIdValue, Priority.ALWAYS);
        Label hint = new Label("La licence .lic doit être émise pour cet ID, puis importée sur le serveur.");
        hint.getStyleClass().add("page-sub");
        VBox card = new VBox(10, h, row, hint);
        if (openLicense != null) {
            Button open = new Button("Ouvrir l'écran Licence");
            open.getStyleClass().add("button-secondary");
            open.setOnAction(e -> openLicense.run());
            card.getChildren().add(open);
        }
        card.getStyleClass().add("card");
        return card;
    }

    private void rebuildEditors() {
        editors.clear();
        groupsBox.getChildren().clear();

        Set<String> placed = new LinkedHashSet<>();
        for (SettingGroup group : GROUPS) {
            List<AppSetting> items = new ArrayList<>();
            for (String key : group.keys()) {
                AppSetting s = byKey.get(key);
                if (s != null) {
                    items.add(s);
                    placed.add(key);
                }
            }
            if (!items.isEmpty()) {
                groupsBox.getChildren().add(buildGroupCard(group.title(), items));
            }
        }

        List<AppSetting> others = new ArrayList<>();
        for (AppSetting s : byKey.values()) {
            if (!placed.contains(s.key())) {
                others.add(s);
            }
        }
        if (!others.isEmpty()) {
            groupsBox.getChildren().add(buildGroupCard("Autres", others));
        }

        if (groupsBox.getChildren().isEmpty()) {
            Label empty = new Label("Aucun paramètre disponible.");
            empty.getStyleClass().add("empty-state");
            groupsBox.getChildren().add(empty);
        }
    }

    private VBox buildGroupCard(String title, List<AppSetting> items) {
        Label h = new Label(title);
        h.getStyleClass().add("settings-group-title");
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        int col = 0;
        int row = 0;
        for (AppSetting setting : items) {
            Node field = buildEditor(setting);
            VBox cell = new VBox(4, fieldLabel(setting), field);
            boolean wide = setting.isJson();
            if (wide && col == 1) {
                col = 0;
                row++;
            }
            grid.add(cell, col, row);
            if (wide) {
                GridPane.setColumnSpan(cell, 2);
                col = 0;
                row++;
            } else {
                col++;
                if (col > 1) {
                    col = 0;
                    row++;
                }
            }
        }

        VBox card = new VBox(12, h, grid);
        card.getStyleClass().add("card");
        return card;
    }

    private Label fieldLabel(AppSetting setting) {
        Label label = new Label(setting.label());
        label.getStyleClass().add("form-label");
        if (setting.key() != null && !setting.key().equals(setting.label())) {
            label.setTooltip(new Tooltip(setting.key()));
        }
        return label;
    }

    private Node buildEditor(AppSetting setting) {
        String key = setting.key();
        String current = setting.value() == null ? "" : setting.value();

        if (setting.hasReferenceList()) {
            ComboBox<ReferenceValueOption> combo = new ComboBox<>();
            combo.setMaxWidth(Double.MAX_VALUE);
            List<ReferenceValueOption> options = referenceValues.getOrDefault(
                    setting.referenceCategory(), List.of());
            combo.getItems().setAll(options);
            combo.setDisable(!canUpdate);
            ReferenceValueOption selected = options.stream()
                    .filter(o -> o.code().equalsIgnoreCase(current))
                    .findFirst()
                    .orElse(null);
            if (selected != null) {
                combo.getSelectionModel().select(selected);
            } else if (!current.isBlank()) {
                ReferenceValueOption orphan = new ReferenceValueOption(current, current + " (valeur actuelle)");
                combo.getItems().add(0, orphan);
                combo.getSelectionModel().select(orphan);
            } else if (!options.isEmpty()) {
                combo.getSelectionModel().selectFirst();
            }
            editors.put(key, () -> {
                ReferenceValueOption opt = combo.getSelectionModel().getSelectedItem();
                return opt == null ? "" : opt.code();
            });
            return combo;
        }

        if (setting.isBoolean()) {
            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().setAll("true", "false");
            combo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(String object) {
                    if ("true".equalsIgnoreCase(object)) {
                        return "Oui";
                    }
                    if ("false".equalsIgnoreCase(object)) {
                        return "Non";
                    }
                    return object == null ? "" : object;
                }

                @Override
                public String fromString(String string) {
                    if ("Oui".equalsIgnoreCase(string)) {
                        return "true";
                    }
                    if ("Non".equalsIgnoreCase(string)) {
                        return "false";
                    }
                    return string;
                }
            });
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setDisable(!canUpdate);
            String norm = "true".equalsIgnoreCase(current) ? "true" : "false";
            combo.getSelectionModel().select(norm);
            editors.put(key, () -> {
                String v = combo.getSelectionModel().getSelectedItem();
                return v == null ? "false" : v;
            });
            return combo;
        }

        if (setting.isJson()) {
            TextArea area = new TextArea(current);
            area.setPrefRowCount(5);
            area.setWrapText(true);
            area.getStyleClass().add("settings-json");
            area.setDisable(!canUpdate);
            editors.put(key, area::getText);
            return area;
        }

        TextField field = new TextField(current);
        field.setDisable(!canUpdate);
        if (setting.isNumber()) {
            field.setPromptText("Nombre");
        }
        editors.put(key, field::getText);
        return field;
    }

    private void saveAll() {
        if (!canUpdate) {
            return;
        }
        error.hide();
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, Supplier<String>> e : editors.entrySet()) {
            values.put(e.getKey(), e.getValue().get());
        }
        loading.setLoading(true);
        FxAsync.run(() -> {
            client.updateBulk(values);
            ClientConfiguration cfg = null;
            try {
                cfg = client.getClientConfig();
            } catch (ApiException ignored) {
            }
            return new LoadPayload(client.getAll(), client.getReferenceValues(), safeInstallId(), cfg);
        }, payload -> {
            loading.setLoading(false);
            applyPayload(payload);
        }, this::fail);
    }

    private String safeInstallId() {
        try {
            return client.installationId();
        } catch (ApiException ex) {
            return installIdValue.getText();
        }
    }

    private void applyPayload(LoadPayload payload) {
        Map<String, AppSetting> map = new LinkedHashMap<>();
        for (AppSetting s : payload.settings()) {
            map.put(s.key(), s);
        }
        byKey = map;
        referenceValues = payload.refs() == null ? Map.of() : payload.refs();
        String id = payload.installId();
        installIdValue.setText(id == null || id.isBlank() ? "—" : id);
        updateLogoStatus(payload.clientConfig());
        if (payload.clientConfig() == null) {
            AppSetting logo = byKey.get("company.logo");
            if (logo != null && logo.value() != null && !logo.value().isBlank()) {
                logoStatus.setText("Logo : " + logo.value());
            }
        }
        rebuildEditors();
    }

    @Override
    public void reload() {
        error.hide();
        loading.setLoading(true);
        FxAsync.run(() -> {
            List<AppSetting> settings = client.getAll();
            Map<String, List<ReferenceValueOption>> refs;
            try {
                refs = client.getReferenceValues();
            } catch (ApiException ex) {
                refs = Map.of();
            }
            String installId;
            try {
                installId = client.installationId();
            } catch (ApiException ex) {
                installId = "";
            }
            ClientConfiguration cfg = null;
            try {
                cfg = client.getClientConfig();
            } catch (ApiException ex) {
                // optionnel
            }
            return new LoadPayload(settings, refs, installId, cfg);
        }, payload -> {
            loading.setLoading(false);
            applyPayload(payload);
            clientConfigView.reload();
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

    private record SettingGroup(String title, List<String> keys) {
    }

    private record LoadPayload(
            List<AppSetting> settings,
            Map<String, List<ReferenceValueOption>> refs,
            String installId,
            ClientConfiguration clientConfig
    ) {
    }
}
