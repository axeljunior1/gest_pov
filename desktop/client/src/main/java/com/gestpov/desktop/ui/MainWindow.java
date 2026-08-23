package com.gestpov.desktop.ui;

import com.gestpov.desktop.net.DashboardClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.admin.AlertsView;
import com.gestpov.desktop.ui.admin.ImportExportView;
import com.gestpov.desktop.ui.admin.RolesView;
import com.gestpov.desktop.ui.admin.UsersView;
import com.gestpov.desktop.ui.analytics.AnalyticsView;
import com.gestpov.desktop.ui.attributes.AttributesView;
import com.gestpov.desktop.ui.brands.BrandsView;
import com.gestpov.desktop.ui.categories.CategoriesView;
import com.gestpov.desktop.ui.customers.CustomersView;
import com.gestpov.desktop.ui.dashboard.DashboardView;
import com.gestpov.desktop.ui.license.LicenseView;
import com.gestpov.desktop.ui.pos.PosHistoryView;
import com.gestpov.desktop.ui.pos.PosReportsView;
import com.gestpov.desktop.ui.pos.PosReturnsView;
import com.gestpov.desktop.ui.pos.PosView;
import com.gestpov.desktop.ui.products.ProductBarcodePrintView;
import com.gestpov.desktop.ui.products.ProductWorkspace;
import com.gestpov.desktop.ui.sales.SalesListView;
import com.gestpov.desktop.ui.settings.SettingsView;
import com.gestpov.desktop.ui.stock.InventoriesView;
import com.gestpov.desktop.ui.stock.PurchaseOrdersView;
import com.gestpov.desktop.ui.stock.StockEntriesExitsView;
import com.gestpov.desktop.ui.stock.StockTransfersView;
import com.gestpov.desktop.ui.stock.StockValuationView;
import com.gestpov.desktop.ui.stock.StockView;
import com.gestpov.desktop.ui.stock.WarehousesView;
import com.gestpov.desktop.ui.suppliers.SuppliersView;
import com.gestpov.desktop.ui.units.UnitsView;
import com.gestpov.desktop.util.FxAsync;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Fenêtre principale après login. Navigation par permissions + raccourcis clavier.
 */
public final class MainWindow extends BorderPane {

    private final SessionContext session;
    private final StackPane center = new StackPane();
    private final List<Button> navButtons = new ArrayList<>();
    private final Label healthDot = new Label("●");
    private final Label healthLabel = new Label("Serveur…");
    private final Button dashboardNav = nav("Tableau de bord");
    private final Button productsNav = nav("Produits");
    private final Button categoriesNav = nav("Catégories");
    private final Button brandsNav = nav("Marques");
    private final Button suppliersNav = nav("Fournisseurs");
    private final Button unitsNav = nav("Unités");
    private final Button attributesNav = nav("Attributs");
    private final Button barcodePrintNav = nav("Étiquettes codes-barres");
    private final Button stockNav = nav("Stock");
    private final Button warehousesNav = nav("Entrepôts");
    private final Button entriesExitsNav = nav("Entrées / Sorties");
    private final Button inventoriesNav = nav("Inventaires");
    private final Button valuationNav = nav("Valorisation");
    private final Button transfersNav = nav("Transferts");
    private final Button purchaseOrdersNav = nav("Bons commande");
    private final Button posNav = nav("Caisse POS");
    private final Button posHistoryNav = nav("Historique caisse");
    private final Button posReportsNav = nav("Rapports caisse");
    private final Button posReturnsNav = nav("Retours POS");
    private final Button customersNav = nav("Clients");
    private final Button salesNav = nav("Ventes BO");
    private final Button analyticsNav = nav("Analytics");
    private final Button usersNav = nav("Utilisateurs");
    private final Button rolesNav = nav("Rôles");
    private final Button alertsNav = nav("Alertes");
    private final Button importExportNav = nav("Import / Export");
    private final Button licenseNav = nav("Licence");
    private final Button settingsNav = nav("Paramètres");

    private DashboardView dashboardView;
    private ProductWorkspace productsView;
    private BrandsView brandsView;
    private CategoriesView categoriesView;
    private SuppliersView suppliersView;
    private UnitsView unitsView;
    private AttributesView attributesView;
    private ProductBarcodePrintView barcodePrintView;
    private StockView stockView;
    private WarehousesView warehousesView;
    private StockEntriesExitsView entriesExitsView;
    private InventoriesView inventoriesView;
    private StockValuationView valuationView;
    private StockTransfersView transfersView;
    private PurchaseOrdersView purchaseOrdersView;
    private PosView posView;
    private PosHistoryView posHistoryView;
    private PosReportsView posReportsView;
    private PosReturnsView posReturnsView;
    private CustomersView customersView;
    private SalesListView salesListView;
    private AnalyticsView analyticsView;
    private UsersView usersView;
    private RolesView rolesView;
    private AlertsView alertsView;
    private ImportExportView importExportView;
    private LicenseView licenseView;
    private SettingsView settingsView;
    private Timeline healthTimeline;

    public MainWindow(SessionContext session, Runnable logout) {
        this.session = session;

        Label brand = new Label("Gest POV");
        brand.getStyleClass().add("brand-title");
        Label server = new Label(session.serverName() + "  ·  v" + session.serverVersion());
        server.getStyleClass().add("brand-sub");
        server.setWrapText(true);

        dashboardNav.setOnAction(e -> showDashboard());
        productsNav.setOnAction(e -> showProducts());
        categoriesNav.setOnAction(e -> showCategories());
        brandsNav.setOnAction(e -> showBrands());
        suppliersNav.setOnAction(e -> showSuppliers());
        unitsNav.setOnAction(e -> showUnits());
        attributesNav.setOnAction(e -> showAttributes());
        barcodePrintNav.setOnAction(e -> showBarcodePrint());
        stockNav.setOnAction(e -> showStock());
        warehousesNav.setOnAction(e -> showWarehouses());
        entriesExitsNav.setOnAction(e -> showEntriesExits());
        inventoriesNav.setOnAction(e -> showInventories());
        valuationNav.setOnAction(e -> showValuation());
        transfersNav.setOnAction(e -> showTransfers());
        purchaseOrdersNav.setOnAction(e -> showPurchaseOrders());
        posNav.setOnAction(e -> showPos());
        posHistoryNav.setOnAction(e -> showPosHistory());
        posReportsNav.setOnAction(e -> showPosReports());
        posReturnsNav.setOnAction(e -> showPosReturns());
        customersNav.setOnAction(e -> showCustomers());
        salesNav.setOnAction(e -> showSales());
        analyticsNav.setOnAction(e -> showAnalytics());
        usersNav.setOnAction(e -> showUsers());
        rolesNav.setOnAction(e -> showRoles());
        alertsNav.setOnAction(e -> showAlerts());
        importExportNav.setOnAction(e -> showImportExport());
        licenseNav.setOnAction(e -> showLicense());
        settingsNav.setOnAction(e -> showSettings());

        VBox navItems = new VBox(4);
        boolean any = false;
        if (session.hasPermission("dashboard.read")) {
            navItems.getChildren().addAll(section("ACCUEIL"), dashboardNav);
            any = true;
        }
        if (session.hasPermission("products.read")) {
            navItems.getChildren().addAll(section("CATALOGUE"), productsNav, categoriesNav, brandsNav,
                    suppliersNav, unitsNav, attributesNav);
            if (session.hasPermission("products.update")) {
                navItems.getChildren().add(barcodePrintNav);
            }
            any = true;
        }
        boolean stockSection = session.hasPermission("stock.read")
                || session.hasPermission("stock_entry.read")
                || session.hasPermission("stock_exit.read")
                || session.hasPermission("inventory.read");
        if (stockSection) {
            navItems.getChildren().add(section("STOCK"));
            if (session.hasPermission("stock.read")) {
                navItems.getChildren().addAll(stockNav, warehousesNav, valuationNav, transfersNav);
            }
            if (session.hasPermission("stock_entry.read") || session.hasPermission("stock_exit.read")) {
                navItems.getChildren().add(entriesExitsNav);
            }
            if (session.hasPermission("inventory.read")) {
                navItems.getChildren().add(inventoriesNav);
            }
            if (session.hasPermission("stock_entry.read")) {
                navItems.getChildren().add(purchaseOrdersNav);
            }
            any = true;
        }
        if (session.hasPermission("pos.sale.read") || session.hasPermission("customer.read")
                || session.hasPermission("pos.report.read")
                || session.hasPermission("pos.return.create")
                || session.hasPermission("pos.sale.refund")
                || session.hasPermission("pos.ticket.print")
                || session.hasPermission("pos.ticket.reprint")
                || session.hasPermission("pos.sale.read_own")
                || session.hasPermission("analytics.sales.read")) {
            navItems.getChildren().add(section("VENTES"));
            if (session.hasPermission("pos.sale.read")) {
                navItems.getChildren().add(posNav);
            }
            if (session.hasPermission("pos.ticket.print") || session.hasPermission("pos.ticket.reprint")
                    || session.hasPermission("pos.report.read")) {
                navItems.getChildren().add(posHistoryNav);
            }
            if (session.hasPermission("pos.report.read")) {
                navItems.getChildren().add(posReportsNav);
            }
            if (session.hasPermission("pos.return.create") || session.hasPermission("pos.sale.refund")
                    || session.hasPermission("pos.return.read")) {
                navItems.getChildren().add(posReturnsNav);
            }
            if (session.hasPermission("customer.read")) {
                navItems.getChildren().add(customersNav);
            }
            if (session.hasPermission("pos.sale.read") || session.hasPermission("pos.sale.read_own")
                    || session.hasPermission("analytics.sales.read") || session.hasPermission("pos.report.read")) {
                navItems.getChildren().add(salesNav);
            }
            any = true;
        }
        if (session.hasPermission("analytics.read") || session.hasPermission("analytics.sales.read")
                || session.hasPermission("sales.cancellations.read")) {
            navItems.getChildren().addAll(section("ANALYTICS"), analyticsNav);
            any = true;
        }
        boolean adminSection = session.hasPermission("users.read")
                || session.hasPermission("roles.read")
                || session.hasPermission("alerts.read")
                || session.hasPermission("import.read")
                || session.hasPermission("export.read");
        if (adminSection) {
            navItems.getChildren().add(section("ADMIN"));
            if (session.hasPermission("users.read")) {
                navItems.getChildren().add(usersNav);
            }
            if (session.hasPermission("roles.read")) {
                navItems.getChildren().add(rolesNav);
            }
            if (session.hasPermission("alerts.read")) {
                navItems.getChildren().add(alertsNav);
            }
            if (session.hasPermission("import.read") || session.hasPermission("export.read")) {
                navItems.getChildren().add(importExportNav);
            }
            any = true;
        }
        if (session.hasPermission("settings.read")) {
            navItems.getChildren().addAll(section("PARAMÈTRES"), settingsNav, licenseNav);
            any = true;
        } else {
            navItems.getChildren().addAll(section("PARAMÈTRES"), licenseNav);
            any = true;
        }
        if (!any) {
            Label none = new Label("Aucun module disponible pour votre profil.");
            none.getStyleClass().add("nav-coming");
            none.setWrapText(true);
            navItems.getChildren().add(none);
        }

        Label hints = new Label("F4 Caisse  ·  F2 Recherche POS\nCtrl+1…8 écrans");
        hints.getStyleClass().add("nav-coming");
        hints.setWrapText(true);

        ScrollPane scroll = new ScrollPane(navItems);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("sidebar-scroll");
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setBackground(null);
        navItems.setStyle("-fx-background-color: transparent;");

        VBox sidebar = new VBox(12, brand, server, scroll, hints);
        sidebar.getStyleClass().add("sidebar");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        healthDot.getStyleClass().add("health-dot");
        healthDot.getStyleClass().add("health-unknown");
        healthLabel.getStyleClass().add("health-label");
        HBox health = new HBox(6, healthDot, healthLabel);
        health.setAlignment(Pos.CENTER_LEFT);

        Label user = new Label(session.displayName() + "\n" + session.email());
        user.getStyleClass().add("user-label");
        user.setWrapText(true);
        Button logoutBtn = new Button("Déconnexion");
        logoutBtn.getStyleClass().add("button-ghost");
        logoutBtn.setOnAction(e -> {
            stopHealth();
            logout.run();
        });

        HBox top = new HBox(16, health, user, logoutBtn);
        top.setAlignment(Pos.CENTER_RIGHT);
        top.getStyleClass().add("top-bar");
        HBox.setHgrow(user, Priority.ALWAYS);

        center.setPadding(new Insets(16));
        openDefaultScreen();

        addEventFilter(KeyEvent.KEY_PRESSED, this::onShortcut);
        startHealthPolling();

        setLeft(sidebar);
        setTop(top);
        setCenter(center);
    }

    private void openDefaultScreen() {
        if (session.hasPermission("dashboard.read")) {
            showDashboard();
        } else if (session.hasPermission("products.read")) {
            showProducts();
        } else if (session.hasPermission("pos.sale.read")) {
            showPos();
        } else if (session.hasPermission("stock.read")) {
            showStock();
        } else if (session.hasPermission("customer.read")) {
            showCustomers();
        } else if (session.hasPermission("settings.read")) {
            showSettings();
        } else {
            Label empty = new Label("Connecté. Aucun écran métier n'est disponible pour vos droits.");
            empty.getStyleClass().add("empty-state");
            center.getChildren().add(empty);
        }
    }

    private void startHealthPolling() {
        pingHealth();
        healthTimeline = new Timeline(new KeyFrame(Duration.seconds(20), e -> pingHealth()));
        healthTimeline.setCycleCount(Timeline.INDEFINITE);
        healthTimeline.play();
    }

    private void stopHealth() {
        if (healthTimeline != null) {
            healthTimeline.stop();
        }
    }

    private void pingHealth() {
        DashboardClient client = new DashboardClient(session.api());
        FxAsync.run(() -> {
            try {
                var health = client.health();
                String status = health.path("status").asText("UP");
                return "UP".equalsIgnoreCase(status) ? "ok" : "degraded";
            } catch (Exception ignored) {
                try {
                    client.discovery();
                    return "ok";
                } catch (Exception e2) {
                    return "down";
                }
            }
        }, state -> {
            healthDot.getStyleClass().removeAll("health-ok", "health-down", "health-unknown");
            switch (state) {
                case "ok" -> {
                    healthDot.getStyleClass().add("health-ok");
                    healthLabel.setText("Serveur OK");
                }
                case "degraded" -> {
                    healthDot.getStyleClass().add("health-unknown");
                    healthLabel.setText("Serveur dégradé");
                }
                default -> {
                    healthDot.getStyleClass().add("health-down");
                    healthLabel.setText("Serveur hors ligne");
                }
            }
        }, ignored -> {
            healthDot.getStyleClass().removeAll("health-ok", "health-down", "health-unknown");
            healthDot.getStyleClass().add("health-down");
            healthLabel.setText("Serveur hors ligne");
        });
    }

    private void onShortcut(KeyEvent e) {
        if (e.getCode() == KeyCode.F4 && session.hasPermission("pos.sale.read")) {
            showPos();
            e.consume();
            return;
        }
        if (e.getCode() == KeyCode.F2 && posView != null && center.getChildren().contains(posView)) {
            posView.focusSearch();
            e.consume();
            return;
        }
        if (!e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
            return;
        }
        switch (e.getCode()) {
            case DIGIT1, NUMPAD1 -> {
                if (session.hasPermission("products.read")) {
                    showProducts();
                    e.consume();
                }
            }
            case DIGIT2, NUMPAD2 -> {
                if (session.hasPermission("products.read")) {
                    showCategories();
                    e.consume();
                }
            }
            case DIGIT3, NUMPAD3 -> {
                if (session.hasPermission("products.read")) {
                    showBrands();
                    e.consume();
                }
            }
            case DIGIT4, NUMPAD4 -> {
                if (session.hasPermission("products.read")) {
                    showSuppliers();
                    e.consume();
                }
            }
            case DIGIT5, NUMPAD5 -> {
                if (session.hasPermission("products.read")) {
                    showUnits();
                    e.consume();
                }
            }
            case DIGIT6, NUMPAD6 -> {
                if (session.hasPermission("stock.read")) {
                    showStock();
                    e.consume();
                }
            }
            case DIGIT7, NUMPAD7 -> {
                if (session.hasPermission("customer.read")) {
                    showCustomers();
                    e.consume();
                }
            }
            case DIGIT8, NUMPAD8 -> {
                if (session.hasPermission("settings.read")) {
                    showSettings();
                    e.consume();
                }
            }
            default -> {
            }
        }
    }


    private void present(javafx.scene.Node view, Button nav, boolean createdNow) {
        setNavActive(nav);
        center.getChildren().setAll(view);
        if (!createdNow && view instanceof Reloadable reloadable) {
            reloadable.reload();
        }
    }

    private void showDashboard() {
        boolean created = dashboardView == null;
        if (created) {
            dashboardView = new DashboardView(session);
        }
        present(dashboardView, dashboardNav, created);
    }

    private void showProducts() {
        boolean created = productsView == null;
        if (created) {
            productsView = new ProductWorkspace(session);
        }
        present(productsView, productsNav, created);
    }

    private void showBrands() {
        boolean created = brandsView == null;
        if (created) {
            brandsView = new BrandsView(session);
        }
        present(brandsView, brandsNav, created);
    }

    private void showCategories() {
        boolean created = categoriesView == null;
        if (created) {
            categoriesView = new CategoriesView(session);
        }
        present(categoriesView, categoriesNav, created);
    }

    private void showSuppliers() {
        boolean created = suppliersView == null;
        if (created) {
            suppliersView = new SuppliersView(session);
        }
        present(suppliersView, suppliersNav, created);
    }

    private void showUnits() {
        boolean created = unitsView == null;
        if (created) {
            unitsView = new UnitsView(session);
        }
        present(unitsView, unitsNav, created);
    }

    private void showAttributes() {
        boolean created = attributesView == null;
        if (created) {
            attributesView = new AttributesView(session);
        }
        present(attributesView, attributesNav, created);
    }

    private void showBarcodePrint() {
        boolean created = barcodePrintView == null;
        if (created) {
            barcodePrintView = new ProductBarcodePrintView(session);
        }
        present(barcodePrintView, barcodePrintNav, created);
    }

    private void showStock() {
        boolean created = stockView == null;
        if (created) {
            stockView = new StockView(session);
        }
        present(stockView, stockNav, created);
    }

    private void showWarehouses() {
        boolean created = warehousesView == null;
        if (created) {
            warehousesView = new WarehousesView(session);
        }
        present(warehousesView, warehousesNav, created);
    }

    private void showEntriesExits() {
        boolean created = entriesExitsView == null;
        if (created) {
            entriesExitsView = new StockEntriesExitsView(session);
        }
        present(entriesExitsView, entriesExitsNav, created);
    }

    private void showInventories() {
        boolean created = inventoriesView == null;
        if (created) {
            inventoriesView = new InventoriesView(session);
        }
        present(inventoriesView, inventoriesNav, created);
    }

    private void showValuation() {
        boolean created = valuationView == null;
        if (created) {
            valuationView = new StockValuationView(session);
        }
        present(valuationView, valuationNav, created);
    }

    private void showTransfers() {
        boolean created = transfersView == null;
        if (created) {
            transfersView = new StockTransfersView(session);
        }
        present(transfersView, transfersNav, created);
    }

    private void showPurchaseOrders() {
        boolean created = purchaseOrdersView == null;
        if (created) {
            purchaseOrdersView = new PurchaseOrdersView(session);
        }
        present(purchaseOrdersView, purchaseOrdersNav, created);
    }

    private void showPos() {
        boolean created = posView == null;
        if (created) {
            posView = new PosView(session);
        }
        present(posView, posNav, created);
    }

    private void showPosHistory() {
        boolean created = posHistoryView == null;
        if (created) {
            posHistoryView = new PosHistoryView(session);
        }
        present(posHistoryView, posHistoryNav, created);
    }

    private void showPosReports() {
        boolean created = posReportsView == null;
        if (created) {
            posReportsView = new PosReportsView(session);
        }
        present(posReportsView, posReportsNav, created);
    }

    private void showPosReturns() {
        boolean created = posReturnsView == null;
        if (created) {
            posReturnsView = new PosReturnsView(session);
        }
        present(posReturnsView, posReturnsNav, created);
    }

    private void showCustomers() {
        boolean created = customersView == null;
        if (created) {
            customersView = new CustomersView(session);
        }
        present(customersView, customersNav, created);
    }

    private void showSales() {
        boolean created = salesListView == null;
        if (created) {
            salesListView = new SalesListView(session);
        }
        present(salesListView, salesNav, created);
    }

    private void showAnalytics() {
        boolean created = analyticsView == null;
        if (created) {
            analyticsView = new AnalyticsView(session);
        }
        present(analyticsView, analyticsNav, created);
    }

    private void showUsers() {
        boolean created = usersView == null;
        if (created) {
            usersView = new UsersView(session);
        }
        present(usersView, usersNav, created);
    }

    private void showRoles() {
        boolean created = rolesView == null;
        if (created) {
            rolesView = new RolesView(session);
        }
        present(rolesView, rolesNav, created);
    }

    private void showAlerts() {
        boolean created = alertsView == null;
        if (created) {
            alertsView = new AlertsView(session);
        }
        present(alertsView, alertsNav, created);
    }

    private void showImportExport() {
        boolean created = importExportView == null;
        if (created) {
            importExportView = new ImportExportView(session);
        }
        present(importExportView, importExportNav, created);
    }

    private void showLicense() {
        boolean created = licenseView == null;
        if (created) {
            licenseView = new LicenseView(session);
        }
        present(licenseView, licenseNav, created);
    }

    private void showSettings() {
        boolean created = settingsView == null;
        if (created) {
            settingsView = new SettingsView(session, this::showLicense);
        }
        present(settingsView, settingsNav, created);
    }

    private void setNavActive(Button active) {
        for (Button button : navButtons) {
            button.getStyleClass().remove("nav-button-active");
        }
        if (!active.getStyleClass().contains("nav-button-active")) {
            active.getStyleClass().add("nav-button-active");
        }
    }

    private Button nav(String title) {
        Button button = new Button(title);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        navButtons.add(button);
        return button;
    }

    private static Label section(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("nav-section");
        return label;
    }
}
