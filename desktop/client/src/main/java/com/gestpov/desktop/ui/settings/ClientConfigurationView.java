package com.gestpov.desktop.ui.settings;

import com.gestpov.desktop.config.ClientConfig;
import com.gestpov.desktop.config.ClientConfigStore;
import com.gestpov.desktop.model.ClientConfiguration;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.SettingsClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.ui.pos.EscPosPrinter;
import com.gestpov.desktop.util.FxAsync;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration client complète (POS, stock, taxes) — alignée sur le module web.
 */
public final class ClientConfigurationView extends VBox {

    private final SessionContext session;
    private final SettingsClient client;
    private final boolean canUpdate;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading;

    private final TextField registerName = new TextField();
    private final TextField salePrefix = new TextField();
    private final TextArea ticketFooter = new TextArea();
    private final CheckBox ticketShowLogo = new CheckBox();
    private final CheckBox autoPrint = new CheckBox();
    private final CheckBox allowPartial = new CheckBox();
    private final CheckBox allowSplit = new CheckBox();
    private final CheckBox changeGiving = new CheckBox();
    private final VBox paymentBox = new VBox(8);
    private final List<CheckBox> paymentChecks = new ArrayList<>();

    private final CheckBox allowNegative = new CheckBox();
    private final CheckBox lowStockAlerts = new CheckBox();
    private final CheckBox multiWarehouse = new CheckBox();
    private final TextField lowThreshold = new TextField();
    private final ComboBox<String> valuation = new ComboBox<>();

    private final CheckBox taxEnabled = new CheckBox();
    private final TextField taxName = new TextField();
    private final TextField taxRate = new TextField();
    private final CheckBox pricesIncludeTax = new CheckBox();
    private final CheckBox autoApplyTax = new CheckBox();

    private final ComboBox<String> ticketPrinter = new ComboBox<>();
    private final ComboBox<String> ticketPaperWidth = new ComboBox<>();
    private final Label printerStatus = new Label();
    private final ClientConfigStore configStore = ClientConfigStore.userDefault();

    private ClientConfiguration current = ClientConfiguration.empty();

    public ClientConfigurationView(SessionContext session, LoadingOverlay sharedLoading) {
        this.session = session;
        this.client = new SettingsClient(session.api());
        this.canUpdate = session.hasPermission("settings.update");
        this.loading = sharedLoading;
        build();
    }

    private void build() {
        Label h = new Label("Configuration POS, stock & taxes");
        h.getStyleClass().add("settings-group-title");
        Label hint = new Label("Miroir de /api/settings/client-config — mêmes options que le module web.");
        hint.getStyleClass().add("page-sub");
        hint.setWrapText(true);

        registerName.setPromptText("Nom de caisse");
        salePrefix.setPromptText("Préfixe ticket (ex. TK)");
        ticketFooter.setPromptText("Pied de ticket");
        ticketFooter.setPrefRowCount(3);
        ticketFooter.setWrapText(true);
        lowThreshold.setPromptText("Seuil stock faible");
        taxName.setPromptText("Nom taxe");
        taxRate.setPromptText("Taux %");
        valuation.getItems().addAll("WEIGHTED_AVERAGE", "FIFO", "LIFO", "LAST_PURCHASE");
        valuation.getSelectionModel().select("WEIGHTED_AVERAGE");

        setEditable(registerName, salePrefix, ticketFooter, lowThreshold, taxName, taxRate);
        valuation.setDisable(!canUpdate);
        setCheckEditable(ticketShowLogo, autoPrint, allowPartial, allowSplit, changeGiving,
                allowNegative, lowStockAlerts, multiWarehouse, taxEnabled, pricesIncludeTax, autoApplyTax);

        GridPane posGrid = new GridPane();
        posGrid.setHgap(12);
        posGrid.setVgap(10);
        posGrid.add(labeled("Nom de caisse", registerName), 0, 0);
        posGrid.add(labeled("Préfixe ticket", salePrefix), 1, 0);
        posGrid.add(labeled("Pied de ticket", ticketFooter), 0, 1, 2, 1);
        VBox posFlags = new VBox(8,
                checkRow(ticketShowLogo, "Afficher le logo sur le ticket"),
                checkRow(autoPrint, "Impression automatique après vente"),
                checkRow(changeGiving, "Rendu de monnaie activé"),
                checkRow(allowPartial, "Paiement partiel"),
                checkRow(allowSplit, "Paiement fractionné"));
        posGrid.add(posFlags, 0, 2, 2, 1);
        ColumnGrow(posGrid);

        Label printerTitle = new Label("Imprimante ticket (ce poste)");
        printerTitle.getStyleClass().add("settings-group-title");
        Label printerHint = new Label("Réglage local à cet ordinateur — pas synchronisé entre postes. "
                + "Imprimante thermique ESC/POS installée en « Générique / Texte seul » dans Windows.");
        printerHint.getStyleClass().add("page-sub");
        printerHint.setWrapText(true);

        ticketPrinter.setPromptText("Aucune (dialogue Windows classique)");
        ticketPrinter.setMaxWidth(Double.MAX_VALUE);
        ticketPaperWidth.getItems().addAll("80mm (48 car.)", "58mm (32 car.)");
        ticketPaperWidth.setMaxWidth(Double.MAX_VALUE);
        Button refreshPrinters = new Button("Détecter les imprimantes");
        refreshPrinters.getStyleClass().add("button-secondary");
        refreshPrinters.setOnAction(e -> refreshPrinterList());
        Button testPrint = new Button("Imprimer un ticket test");
        testPrint.getStyleClass().add("button-secondary");
        testPrint.setOnAction(e -> testPrint());
        Button savePrinter = new Button("Enregistrer l'imprimante");
        savePrinter.getStyleClass().add("button-primary");
        savePrinter.setOnAction(e -> savePrinterConfig());
        printerStatus.getStyleClass().add("page-sub");
        printerStatus.setWrapText(true);

        GridPane printerGrid = new GridPane();
        printerGrid.setHgap(12);
        printerGrid.setVgap(10);
        printerGrid.add(labeled("Imprimante Windows", ticketPrinter), 0, 0);
        printerGrid.add(labeled("Largeur papier", ticketPaperWidth), 1, 0);
        printerGrid.add(new HBox(8, refreshPrinters, testPrint, savePrinter), 0, 1, 2, 1);
        printerGrid.add(printerStatus, 0, 2, 2, 1);
        ColumnGrow(printerGrid);

        Label payTitle = new Label("Moyens de paiement");
        payTitle.getStyleClass().add("form-label");
        paymentBox.getStyleClass().add("config-check-list");

        Label stockTitle = new Label("Stock");
        stockTitle.getStyleClass().add("settings-group-title");
        GridPane stockGrid = new GridPane();
        stockGrid.setHgap(12);
        stockGrid.setVgap(10);
        stockGrid.add(checkRow(allowNegative, "Autoriser vente si stock insuffisant"), 0, 0);
        stockGrid.add(checkRow(lowStockAlerts, "Alertes stock faible"), 1, 0);
        stockGrid.add(checkRow(multiWarehouse, "Multi-entrepôt"), 0, 1);
        stockGrid.add(labeled("Seuil stock faible", lowThreshold), 0, 2);
        stockGrid.add(labeled("Valorisation", valuation), 1, 2);
        ColumnGrow(stockGrid);

        Label taxTitle = new Label("Taxes");
        taxTitle.getStyleClass().add("settings-group-title");
        GridPane taxGrid = new GridPane();
        taxGrid.setHgap(12);
        taxGrid.setVgap(10);
        taxGrid.add(checkRow(taxEnabled, "Activer les taxes"), 0, 0, 2, 1);
        taxGrid.add(labeled("Nom taxe", taxName), 0, 1);
        taxGrid.add(labeled("Taux par défaut (%)", taxRate), 1, 1);
        taxGrid.add(checkRow(pricesIncludeTax, "Prix affichés TTC"), 0, 2);
        taxGrid.add(checkRow(autoApplyTax, "Application auto sur les ventes"), 1, 2);
        ColumnGrow(taxGrid);

        Button save = new Button("Enregistrer config");
        save.getStyleClass().add("button-primary");
        save.setDisable(!canUpdate);
        save.setOnAction(e -> save());
        Button refresh = new Button("Recharger");
        refresh.getStyleClass().add("button-ghost");
        refresh.setOnAction(e -> reload());
        HBox actions = new HBox(8, save, refresh);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label posTitle = new Label("Caisse & tickets");
        posTitle.getStyleClass().add("settings-group-title");

        setSpacing(14);
        getChildren().addAll(
                h, hint, error,
                posTitle, posGrid, payTitle, paymentBox,
                printerTitle, printerHint, printerGrid,
                stockTitle, stockGrid,
                taxTitle, taxGrid,
                actions);
        getStyleClass().add("card");
        setPadding(new Insets(16));

        bindPrinterConfig();
        refreshPrinterList();
    }

    private void bindPrinterConfig() {
        ClientConfig local = configStore.load();
        if (local.hasTicketPrinter() && !ticketPrinter.getItems().contains(local.ticketPrinterName())) {
            ticketPrinter.getItems().add(local.ticketPrinterName());
        }
        ticketPrinter.setValue(local.hasTicketPrinter() ? local.ticketPrinterName() : null);
        ticketPaperWidth.getSelectionModel().select(local.ticketPaperWidthChars() <= 32 ? 1 : 0);
    }

    private void refreshPrinterList() {
        String selected = ticketPrinter.getValue();
        FxAsync.run(EscPosPrinter::listPrinters, names -> {
            ticketPrinter.getItems().setAll(names);
            if (selected != null) {
                if (!ticketPrinter.getItems().contains(selected)) {
                    ticketPrinter.getItems().add(selected);
                }
                ticketPrinter.setValue(selected);
            }
            printerStatus.setText(ticketPrinter.getItems().isEmpty()
                    ? "Aucune imprimante détectée par Windows."
                    : ticketPrinter.getItems().size() + " imprimante(s) détectée(s).");
        }, e -> printerStatus.setText("Détection impossible : " + e.getMessage()));
    }

    private void savePrinterConfig() {
        String name = ticketPrinter.getValue();
        int width = ticketPaperWidth.getSelectionModel().getSelectedIndex() == 1 ? 32 : 48;
        try {
            configStore.save(configStore.load().withTicketPrinter(name == null ? "" : name, width));
            printerStatus.setText(name == null || name.isBlank()
                    ? "Aucune imprimante ESC/POS — dialogue d'impression Windows classique."
                    : "Imprimante enregistrée pour ce poste : " + name);
        } catch (Exception e) {
            printerStatus.setText("Échec de l'enregistrement : " + e.getMessage());
        }
    }

    private void testPrint() {
        String name = ticketPrinter.getValue();
        if (name == null || name.isBlank()) {
            printerStatus.setText("Sélectionnez d'abord une imprimante.");
            return;
        }
        int width = ticketPaperWidth.getSelectionModel().getSelectedIndex() == 1 ? 32 : 48;
        printerStatus.setText("Impression du ticket test…");
        FxAsync.runVoid(() -> EscPosPrinter.printTestPage(name, width),
                () -> printerStatus.setText("Ticket test envoyé à " + name + "."),
                e -> printerStatus.setText("Échec de l'impression test : " + e.getMessage()));
    }

    public void reload() {
        error.hide();
        if (loading != null) {
            loading.setLoading(true);
        }
        FxAsync.run(client::getClientConfig, cfg -> {
            if (loading != null) {
                loading.setLoading(false);
            }
            bind(cfg);
        }, this::fail);
    }

    private void bind(ClientConfiguration cfg) {
        current = cfg == null ? ClientConfiguration.empty() : cfg;
        ClientConfiguration.Pos pos = current.pos();
        ClientConfiguration.Stock stock = current.stock();
        ClientConfiguration.Tax tax = current.tax();

        registerName.setText(nullToEmpty(pos.registerName()));
        salePrefix.setText(nullToEmpty(pos.salePrefix()));
        ticketFooter.setText(nullToEmpty(pos.ticketFooter()));
        ticketShowLogo.setSelected(pos.ticketShowLogo());
        autoPrint.setSelected(pos.autoPrintAfterSale());
        allowPartial.setSelected(pos.allowPartialPayment());
        allowSplit.setSelected(pos.allowSplitPayment());
        changeGiving.setSelected(pos.changeGivingEnabled());

        paymentChecks.clear();
        paymentBox.getChildren().clear();
        List<ClientConfiguration.PaymentMethodSetting> methods = pos.paymentMethods();
        if (methods == null || methods.isEmpty()) {
            paymentBox.getChildren().add(new Label("Aucune méthode de paiement"));
        } else {
            for (ClientConfiguration.PaymentMethodSetting m : methods) {
                CheckBox cb = new CheckBox();
                cb.setSelected(m.enabled());
                cb.setDisable(!canUpdate);
                cb.setUserData(m);
                paymentChecks.add(cb);
                String label = (m.label() == null || m.label().isBlank()) ? m.code() : m.label();
                paymentBox.getChildren().add(checkRow(cb, label + " (" + m.code() + ")"));
            }
        }

        allowNegative.setSelected(stock.allowNegativeStock());
        lowStockAlerts.setSelected(stock.lowStockAlertsEnabled());
        multiWarehouse.setSelected(stock.multiWarehouseEnabled());
        lowThreshold.setText(stock.lowStockThresholdDefault() == null
                ? "10" : stock.lowStockThresholdDefault().stripTrailingZeros().toPlainString());
        if (stock.valuationMethod() != null && !stock.valuationMethod().isBlank()) {
            if (!valuation.getItems().contains(stock.valuationMethod())) {
                valuation.getItems().add(stock.valuationMethod());
            }
            valuation.getSelectionModel().select(stock.valuationMethod());
        }

        taxEnabled.setSelected(tax.enabled());
        taxName.setText(nullToEmpty(tax.name()));
        taxRate.setText(tax.defaultRate() == null
                ? "0" : tax.defaultRate().stripTrailingZeros().toPlainString());
        pricesIncludeTax.setSelected(tax.pricesIncludeTax());
        autoApplyTax.setSelected(tax.autoApplyOnSales());
    }

    private void save() {
        if (!canUpdate) {
            return;
        }
        BigDecimal threshold;
        BigDecimal rate;
        try {
            threshold = parseDecimal(lowThreshold.getText(), BigDecimal.TEN);
            rate = parseDecimal(taxRate.getText(), BigDecimal.ZERO);
        } catch (NumberFormatException e) {
            error.show("Seuil stock ou taux taxe invalide.");
            return;
        }
        if (paymentChecks.stream().noneMatch(CheckBox::isSelected)) {
            error.show("Activez au moins un moyen de paiement.");
            return;
        }

        Map<String, Object> pos = new LinkedHashMap<>();
        pos.put("registerName", trim(registerName));
        pos.put("salePrefix", trim(salePrefix));
        pos.put("ticketFooter", ticketFooter.getText() == null ? "" : ticketFooter.getText());
        pos.put("ticketShowLogo", ticketShowLogo.isSelected());
        pos.put("autoPrintAfterSale", autoPrint.isSelected());
        pos.put("changeGivingEnabled", changeGiving.isSelected());
        pos.put("allowPartialPayment", allowPartial.isSelected());
        pos.put("allowSplitPayment", allowSplit.isSelected());
        List<Map<String, Object>> methods = new ArrayList<>();
        for (CheckBox cb : paymentChecks) {
            if (cb.getUserData() instanceof ClientConfiguration.PaymentMethodSetting m) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("code", m.code());
                row.put("label", m.label());
                row.put("enabled", cb.isSelected());
                methods.add(row);
            }
        }
        pos.put("paymentMethods", methods);

        Map<String, Object> stock = new LinkedHashMap<>();
        stock.put("allowNegativeStock", allowNegative.isSelected());
        stock.put("lowStockAlertsEnabled", lowStockAlerts.isSelected());
        stock.put("multiWarehouseEnabled", multiWarehouse.isSelected());
        stock.put("lowStockThresholdDefault", threshold);
        stock.put("valuationMethod", valuation.getValue() == null ? "WEIGHTED_AVERAGE" : valuation.getValue());

        Map<String, Object> tax = new LinkedHashMap<>();
        tax.put("enabled", taxEnabled.isSelected());
        tax.put("name", trim(taxName));
        tax.put("defaultRate", rate);
        tax.put("pricesIncludeTax", pricesIncludeTax.isSelected());
        tax.put("autoApplyOnSales", autoApplyTax.isSelected());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pos", pos);
        body.put("stock", stock);
        body.put("tax", tax);

        error.hide();
        if (loading != null) {
            loading.setLoading(true);
        }
        FxAsync.run(() -> client.updateClientConfig(body), cfg -> {
            if (loading != null) {
                loading.setLoading(false);
            }
            bind(cfg);
        }, this::fail);
    }

    private void fail(Throwable t) {
        if (loading != null) {
            loading.setLoading(false);
        }
        if (t instanceof ApiException api && !api.isUnauthorized()) {
            error.show(ApiException.userMessage(api));
        } else if (!(t instanceof ApiException a && a.isUnauthorized())) {
            error.show("Une erreur est survenue.");
        }
    }

    private void setEditable(javafx.scene.control.Control... nodes) {
        for (javafx.scene.control.Control n : nodes) {
            n.setDisable(!canUpdate);
        }
    }

    private void setCheckEditable(CheckBox... boxes) {
        for (CheckBox b : boxes) {
            b.setDisable(!canUpdate);
        }
    }

    private static HBox checkRow(CheckBox box, String text) {
        Label label = new Label(text);
        label.getStyleClass().add("check-row-label");
        label.setWrapText(true);
        label.setOnMouseClicked(e -> {
            if (!box.isDisabled()) {
                box.setSelected(!box.isSelected());
            }
        });
        HBox row = new HBox(10, box, label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("check-row");
        HBox.setHgrow(label, Priority.ALWAYS);
        return row;
    }

    private static VBox labeled(String title, javafx.scene.Node node) {
        Label l = new Label(title);
        l.getStyleClass().add("form-label");
        VBox box = new VBox(4, l, node);
        GridPane.setHgrow(box, Priority.ALWAYS);
        if (node instanceof TextField || node instanceof TextArea || node instanceof ComboBox) {
            ((javafx.scene.layout.Region) node).setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    private static void ColumnGrow(GridPane grid) {
        javafx.scene.layout.ColumnConstraints c0 = new javafx.scene.layout.ColumnConstraints();
        c0.setPercentWidth(50);
        javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints();
        c1.setPercentWidth(50);
        grid.getColumnConstraints().setAll(c0, c1);
        grid.setMaxWidth(Double.MAX_VALUE);
    }

    private static String trim(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private static BigDecimal parseDecimal(String text, BigDecimal fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return new BigDecimal(text.trim().replace(',', '.'));
    }
}
