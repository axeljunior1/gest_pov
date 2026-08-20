package com.gestpov.desktop.ui;

import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.brands.BrandsView;
import com.gestpov.desktop.ui.categories.CategoriesView;
import com.gestpov.desktop.ui.customers.CustomersView;
import com.gestpov.desktop.ui.pos.PosView;
import com.gestpov.desktop.ui.products.ProductWorkspace;
import com.gestpov.desktop.ui.settings.SettingsView;
import com.gestpov.desktop.ui.stock.StockView;
import com.gestpov.desktop.ui.suppliers.SuppliersView;
import com.gestpov.desktop.ui.units.UnitsView;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Fenêtre principale après login. Navigation par permissions + raccourcis clavier.
 */
public final class MainWindow extends BorderPane {

    private final SessionContext session;
    private final StackPane center = new StackPane();
    private final List<Button> navButtons = new ArrayList<>();
    private final Button productsNav = nav("Produits");
    private final Button categoriesNav = nav("Catégories");
    private final Button brandsNav = nav("Marques");
    private final Button suppliersNav = nav("Fournisseurs");
    private final Button unitsNav = nav("Unités");
    private final Button stockNav = nav("Stock");
    private final Button posNav = nav("Caisse POS");
    private final Button customersNav = nav("Clients");
    private final Button settingsNav = nav("Paramètres");

    private ProductWorkspace productsView;
    private BrandsView brandsView;
    private CategoriesView categoriesView;
    private SuppliersView suppliersView;
    private UnitsView unitsView;
    private StockView stockView;
    private PosView posView;
    private CustomersView customersView;
    private SettingsView settingsView;

    public MainWindow(SessionContext session, Runnable logout) {
        this.session = session;

        Label brand = new Label("Gest POV");
        brand.getStyleClass().add("brand-title");
        Label server = new Label(session.serverName() + "  ·  v" + session.serverVersion());
        server.getStyleClass().add("brand-sub");
        server.setWrapText(true);

        productsNav.setOnAction(e -> showProducts());
        categoriesNav.setOnAction(e -> showCategories());
        brandsNav.setOnAction(e -> showBrands());
        suppliersNav.setOnAction(e -> showSuppliers());
        unitsNav.setOnAction(e -> showUnits());
        stockNav.setOnAction(e -> showStock());
        posNav.setOnAction(e -> showPos());
        customersNav.setOnAction(e -> showCustomers());
        settingsNav.setOnAction(e -> showSettings());

        VBox navItems = new VBox(4);
        boolean any = false;
        if (session.hasPermission("products.read")) {
            navItems.getChildren().addAll(section("CATALOGUE"), productsNav, categoriesNav, brandsNav,
                    suppliersNav, unitsNav);
            any = true;
        }
        if (session.hasPermission("stock.read")) {
            navItems.getChildren().addAll(section("STOCK"), stockNav);
            any = true;
        }
        if (session.hasPermission("pos.sale.read") || session.hasPermission("customer.read")) {
            navItems.getChildren().add(section("VENTES"));
            if (session.hasPermission("pos.sale.read")) {
                navItems.getChildren().add(posNav);
            }
            if (session.hasPermission("customer.read")) {
                navItems.getChildren().add(customersNav);
            }
            any = true;
        }
        if (session.hasPermission("settings.read")) {
            navItems.getChildren().addAll(section("PARAMÈTRES"), settingsNav);
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
        scroll.setStyle("-fx-background-color: transparent;");

        VBox sidebar = new VBox(12, brand, server, scroll, hints);
        sidebar.getStyleClass().add("sidebar");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Label user = new Label(session.displayName() + "\n" + session.email());
        user.getStyleClass().add("user-label");
        user.setWrapText(true);
        Button logoutBtn = new Button("Déconnexion");
        logoutBtn.getStyleClass().add("button-ghost");
        logoutBtn.setOnAction(e -> logout.run());

        HBox top = new HBox(16, user, logoutBtn);
        top.setAlignment(Pos.CENTER_RIGHT);
        top.getStyleClass().add("top-bar");
        HBox.setHgrow(user, Priority.ALWAYS);

        center.setPadding(new Insets(16));
        if (session.hasPermission("products.read")) {
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

        addEventFilter(KeyEvent.KEY_PRESSED, this::onShortcut);

        setLeft(sidebar);
        setTop(top);
        setCenter(center);
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

    private void showProducts() {
        if (productsView == null) {
            productsView = new ProductWorkspace(session);
        }
        setNavActive(productsNav);
        center.getChildren().setAll(productsView);
    }

    private void showBrands() {
        if (brandsView == null) {
            brandsView = new BrandsView(session);
        }
        setNavActive(brandsNav);
        center.getChildren().setAll(brandsView);
    }

    private void showCategories() {
        if (categoriesView == null) {
            categoriesView = new CategoriesView(session);
        }
        setNavActive(categoriesNav);
        center.getChildren().setAll(categoriesView);
    }

    private void showSuppliers() {
        if (suppliersView == null) {
            suppliersView = new SuppliersView(session);
        }
        setNavActive(suppliersNav);
        center.getChildren().setAll(suppliersView);
    }

    private void showUnits() {
        if (unitsView == null) {
            unitsView = new UnitsView(session);
        }
        setNavActive(unitsNav);
        center.getChildren().setAll(unitsView);
    }

    private void showStock() {
        if (stockView == null) {
            stockView = new StockView(session);
        }
        setNavActive(stockNav);
        center.getChildren().setAll(stockView);
    }

    private void showPos() {
        if (posView == null) {
            posView = new PosView(session);
        }
        setNavActive(posNav);
        center.getChildren().setAll(posView);
    }

    private void showCustomers() {
        if (customersView == null) {
            customersView = new CustomersView(session);
        }
        setNavActive(customersNav);
        center.getChildren().setAll(customersView);
    }

    private void showSettings() {
        if (settingsView == null) {
            settingsView = new SettingsView(session);
        }
        setNavActive(settingsNav);
        center.getChildren().setAll(settingsView);
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
