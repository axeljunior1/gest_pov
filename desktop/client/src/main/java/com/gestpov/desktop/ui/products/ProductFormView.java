package com.gestpov.desktop.ui.products;

import com.gestpov.desktop.model.AuditLogEntry;
import com.gestpov.desktop.model.Brand;
import com.gestpov.desktop.model.Category;
import com.gestpov.desktop.model.PriceHistory;
import com.gestpov.desktop.model.Product;
import com.gestpov.desktop.model.ProductDraft;
import com.gestpov.desktop.model.ProductImage;
import com.gestpov.desktop.model.ProductPackaging;
import com.gestpov.desktop.model.ProductVariant;
import com.gestpov.desktop.model.Supplier;
import com.gestpov.desktop.model.Unit;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.BrandClient;
import com.gestpov.desktop.net.CategoryClient;
import com.gestpov.desktop.net.ProductClient;
import com.gestpov.desktop.net.SupplierClient;
import com.gestpov.desktop.net.UnitClient;
import com.gestpov.desktop.service.ProductValidator;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.ConfirmationDialog;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.util.FxAsync;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class ProductFormView extends StackPane {

    private final SessionContext session;
    private final ProductClient products;
    private final CategoryClient categoriesApi;
    private final BrandClient brandsApi;
    private final SupplierClient suppliersApi;
    private final UnitClient unitsApi;
    private final Long productId;
    private final Runnable onBack;
    private final java.util.function.LongConsumer onCreated;
    private final ErrorBanner errorBanner = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TextField nom = new TextField();
    private final TextField sku = new TextField();
    private final TextField codeBarre = new TextField();
    private final CheckBox generateBarcode = new CheckBox("Générer un EAN-13 à l'enregistrement (activé par défaut)");
    private final ComboBox<RefOption> marque = new ComboBox<>();
    private final ComboBox<RefOption> categorie = new ComboBox<>();
    private final ComboBox<RefOption> fournisseur = new ComboBox<>();
    private final ComboBox<RefOption> unite = new ComboBox<>();
    private final TextField prixAchat = new TextField();
    private final TextField prixVente = new TextField();
    private final ComboBox<RefOption> statut = new ComboBox<>();
    private final ComboBox<RefOption> cycleVie = new ComboBox<>();
    private final TextArea description = new TextArea();
    private final Label stockBadge = new Label();
    private final Label lifecycleBadge = new Label();
    private final Button saveButton = new Button("Enregistrer");
    private final VBox workflowBar = new VBox(8);
    private final ComboBox<String> priceType = new ComboBox<>();
    private final TextField newPrice = new TextField();
    private final TableView<PriceHistory> historyTable = new TableView<>();
    private final FlowPane imagePane = new FlowPane(8, 8);
    private final CheckBox uploadAsPrimary = new CheckBox("Définir comme image principale");
    private final TableView<ProductVariant> variantsTable = new TableView<>();
    private final TextField variantCouleur = new TextField();
    private final TextField variantTaille = new TextField();
    private final TextField variantSku = new TextField();
    private final TextField variantPrix = new TextField();
    private final CheckBox variantGenBarcode = new CheckBox("Générer code-barres");
    private final TableView<ProductPackaging> packagingTable = new TableView<>();
    private final TextField pkgNom = new TextField();
    private final TextField pkgSymbole = new TextField();
    private final TextField pkgQty = new TextField();
    private final TextField pkgPrix = new TextField();
    private final TableView<AuditLogEntry> auditTable = new TableView<>();
    private Product current;
    private final TabPane tabs = new TabPane();
    private final Tab pricesTab = new Tab("Prix");
    private final Tab imagesTab = new Tab("Images");
    private final Tab variantsTab = new Tab("Variantes");
    private final Tab packagingTab = new Tab("Conditionnements");
    private final Tab auditTab = new Tab("Audit");

    public ProductFormView(SessionContext session, Long productId, Runnable onBack,
                           java.util.function.LongConsumer onCreated) {
        this.session = session;
        this.productId = productId;
        this.onBack = onBack;
        this.onCreated = onCreated;
        this.products = new ProductClient(session.api());
        this.categoriesApi = new CategoryClient(session.api());
        this.brandsApi = new BrandClient(session.api());
        this.suppliersApi = new SupplierClient(session.api());
        this.unitsApi = new UnitClient(session.api());
        getChildren().addAll(buildContent(), loading);
        load();
    }

    private javafx.scene.Node buildContent() {
        boolean isNew = productId == null;
        Label title = new Label(isNew ? "Nouveau produit" : "Fiche produit");
        title.getStyleClass().add("page-title");
        Button back = new Button("← Retour à la liste");
        back.getStyleClass().add("button-ghost");
        back.setOnAction(e -> onBack.run());
        stockBadge.getStyleClass().add("badge");
        lifecycleBadge.getStyleClass().addAll("badge", "badge-neutral");
        lifecycleBadge.setVisible(false);
        lifecycleBadge.setManaged(false);
        stockBadge.setVisible(false);
        stockBadge.setManaged(false);

        HBox titleRow = new HBox(10, title, lifecycleBadge, stockBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        saveButton.getStyleClass().addAll("button-primary", "button-lg");
        saveButton.setOnAction(e -> save());
        boolean canWrite = isNew ? session.hasPermission("products.create") : session.hasPermission("products.update");
        saveButton.setVisible(canWrite);
        saveButton.setManaged(canWrite);

        HBox header = new HBox(14, back, titleRow, spacer, saveButton);
        header.setAlignment(Pos.CENTER_LEFT);

        workflowBar.getStyleClass().addAll("status-banner", "status-banner-draft");
        workflowBar.setVisible(false);
        workflowBar.setManaged(false);
        if (!isNew) {
            buildWorkflowButtons();
        }

        Tab general = new Tab("Général", generalForm());
        general.setClosable(false);
        pricesTab.setClosable(false);
        pricesTab.setContent(pricesPane());
        imagesTab.setClosable(false);
        imagesTab.setContent(imagesPane());
        variantsTab.setClosable(false);
        variantsTab.setContent(variantsPane());
        packagingTab.setClosable(false);
        packagingTab.setContent(packagingPane());
        auditTab.setClosable(false);
        auditTab.setContent(auditPane());
        tabs.getTabs().add(general);
        if (!isNew) {
            tabs.getTabs().addAll(pricesTab, imagesTab, variantsTab, packagingTab, auditTab);
        }
        tabs.getStyleClass().add("product-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox page = new VBox(18, header, errorBanner, workflowBar, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        return scroll;
    }

    private void buildWorkflowButtons() {
        workflowBar.getChildren().clear();
        String cycle = current == null ? null : current.cycleVie();
        boolean pending = "EN_ATTENTE_VALIDATION".equals(cycle);
        workflowBar.getStyleClass().removeAll("status-banner-draft", "status-banner-pending");
        workflowBar.getStyleClass().add(pending ? "status-banner-pending" : "status-banner-draft");

        Label status = new Label();
        status.getStyleClass().add("status-banner-text");
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (pending && session.hasPermission("products.validate")) {
            status.setText("⏳ En attente de validation — ce produit n'est pas encore actif au catalogue.");
            Button approve = new Button("✓ Approuver");
            approve.getStyleClass().add("button-primary");
            approve.setOnAction(e -> workflow(() -> products.approveLifecycle(productId)));
            Button reject = new Button("Rejeter");
            reject.getStyleClass().add("button-danger");
            reject.setOnAction(e -> workflow(() -> products.rejectLifecycle(productId, "Rejeté depuis Desktop")));
            actions.getChildren().addAll(approve, reject);
            workflowBar.getChildren().addAll(status, actions);
        } else if ("BROUILLON".equals(cycle) && session.hasPermission("products.update")) {
            status.setText("Ce produit est en brouillon — soumettez-le pour validation avant activation.");
            Button submit = new Button("Soumettre validation");
            submit.getStyleClass().add("button-secondary");
            submit.setOnAction(e -> workflow(() -> products.submitLifecycle(productId)));
            actions.getChildren().add(submit);
            workflowBar.getChildren().addAll(status, actions);
        }
        boolean show = !workflowBar.getChildren().isEmpty();
        workflowBar.setVisible(show);
        workflowBar.setManaged(show);
    }

    private void workflow(java.util.concurrent.Callable<Product> action) {
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(action, updated -> load(), this::showError);
    }

    private VBox generalForm() {
        nom.setPromptText("Nom");
        sku.setPromptText("Auto depuis le nom si vide");
        codeBarre.setPromptText("EAN-13 ou génération auto");
        generateBarcode.setSelected(productId == null);
        description.setPrefRowCount(3);
        for (String s : ProductLabels.STATUTS) {
            statut.getItems().add(new RefOption(s, ProductLabels.status(s)));
        }
        for (String s : ProductLabels.CYCLES) {
            cycleVie.getItems().add(new RefOption(s, ProductLabels.lifecycle(s)));
        }
        statut.getSelectionModel().selectFirst();
        // POS / vente : cycle Actif par défaut (sinon le produit n'apparaît pas en caisse)
        selectCode(cycleVie, "ACTIF");
        if (cycleVie.getSelectionModel().getSelectedItem() == null) {
            cycleVie.getSelectionModel().selectFirst();
        }

        VBox codeBarreBox = labeled("Code-barres (produit simple)", codeBarre);
        VBox identification = section("IDENTIFICATION",
                row(labeled("Nom", nom), labeled("SKU", sku)),
                codeBarreBox,
                generateBarcode,
                labeled("Description", description));

        VBox uniteBox = labeled("Unité de base (stock)", unite);
        Label unitHint = new Label("Le stock est toujours exprimé dans cette unité.");
        unitHint.getStyleClass().add("section-hint");
        uniteBox.getChildren().add(unitHint);
        VBox classification = section("CLASSIFICATION & APPROVISIONNEMENT",
                row(labeled("Marque", marque), labeled("Catégorie", categorie)),
                row(labeled("Fournisseur principal", fournisseur), uniteBox));

        VBox pricing = section("TARIFICATION",
                row(labeled("Prix achat", prixAchat), labeled("Prix vente", prixVente)));

        VBox lifecycleSection = section("STATUT & CYCLE DE VIE",
                row(labeled("Statut", statut), labeled("Cycle de vie", cycleVie)));

        VBox form = new VBox(16, identification, classification, pricing, lifecycleSection);
        return form;
    }

    private static VBox section(String titleText, javafx.scene.Node... rows) {
        Label label = new Label(titleText);
        label.getStyleClass().add("section-title");
        VBox box = new VBox(14, label);
        box.getChildren().addAll(rows);
        box.getStyleClass().add("card");
        return box;
    }

    private static HBox row(javafx.scene.Node a, javafx.scene.Node b) {
        HBox box = new HBox(16, a, b);
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);
        return box;
    }

    private VBox pricesPane() {
        priceType.getItems().setAll(ProductLabels.PRICE_TYPES);
        priceType.getSelectionModel().select("VENTE");
        newPrice.setPromptText("Nouveau prix");
        Button apply = new Button("Appliquer");
        apply.getStyleClass().add("button-primary");
        apply.setOnAction(e -> applyPrice());
        apply.setVisible(session.hasPermission("products.update"));
        apply.setManaged(session.hasPermission("products.update"));
        HBox form = new HBox(8, labeled("Type", priceType), labeled("Nouveau prix", newPrice), apply);
        form.setAlignment(Pos.BOTTOM_LEFT);
        form.getStyleClass().add("card");

        TableColumn<PriceHistory, String> date = col("Date", h -> ProductLabels.date(h.dateModification()));
        TableColumn<PriceHistory, String> type = col("Type", PriceHistory::type);
        TableColumn<PriceHistory, String> oldP = col("Ancien", h -> ProductLabels.price(h.ancienPrix()));
        TableColumn<PriceHistory, String> newP = col("Nouveau", h -> ProductLabels.price(h.nouveauPrix()));
        TableColumn<PriceHistory, String> user = col("Utilisateur", h -> h.utilisateur() == null ? "—" : h.utilisateur());
        historyTable.getColumns().addAll(date, type, oldP, newP, user);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.setPrefHeight(240);
        return new VBox(12, form, historyTable);
    }

    private VBox imagesPane() {
        Button upload = new Button("Ajouter une image");
        upload.getStyleClass().add("button-secondary");
        upload.setOnAction(e -> uploadImage());
        upload.setVisible(session.hasPermission("products.create"));
        upload.setManaged(session.hasPermission("products.create"));
        uploadAsPrimary.setSelected(true);
        uploadAsPrimary.setVisible(session.hasPermission("products.create"));
        uploadAsPrimary.setManaged(session.hasPermission("products.create"));
        imagePane.getStyleClass().add("card");
        return new VBox(12, new HBox(12, upload, uploadAsPrimary), imagePane);
    }

    private VBox variantsPane() {
        variantCouleur.setPromptText("Couleur");
        variantTaille.setPromptText("Taille");
        variantSku.setPromptText("SKU variante");
        variantPrix.setPromptText("Prix");
        variantGenBarcode.setSelected(true);
        Button add = new Button("Ajouter variante");
        add.getStyleClass().add("button-primary");
        add.setOnAction(e -> addVariant());
        boolean canCreate = session.hasPermission("product_variant.create")
                || session.hasPermission("products.create");
        HBox form = new HBox(8, variantCouleur, variantTaille, variantSku, variantPrix, variantGenBarcode, add);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setVisible(canCreate);
        form.setManaged(canCreate);

        variantsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        variantsTable.setPrefHeight(220);
        variantsTable.getColumns().add(colV("Libellé", v -> v.label() == null ? "" : v.label()));
        variantsTable.getColumns().add(colV("SKU", v -> v.sku() == null ? "" : v.sku()));
        variantsTable.getColumns().add(colV("Prix", v -> ProductLabels.price(v.prix())));
        variantsTable.getColumns().add(colV("Stock", v -> v.stock() == null ? "—" : String.valueOf(v.stock())));
        if (session.hasPermission("product_variant.delete") || session.hasPermission("products.delete")) {
            TableColumn<ProductVariant, Void> actions = new TableColumn<>();
            actions.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        return;
                    }
                    ProductVariant variant = getTableRow().getItem();
                    Button del = new Button("Suppr.");
                    del.getStyleClass().add("button-danger");
                    del.setOnAction(e -> deleteVariant(variant));
                    setGraphic(del);
                }
            });
            variantsTable.getColumns().add(actions);
        }
        return new VBox(12, form, variantsTable);
    }

    private VBox packagingPane() {
        pkgNom.setPromptText("Nom *");
        pkgSymbole.setPromptText("Symbole");
        pkgQty.setPromptText("Qté base *");
        pkgPrix.setPromptText("Prix vente");
        Button add = new Button("Ajouter");
        add.getStyleClass().add("button-primary");
        add.setOnAction(e -> addPackaging());
        boolean canCreate = session.hasPermission("products.create");
        HBox form = new HBox(8, pkgNom, pkgSymbole, pkgQty, pkgPrix, add);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setVisible(canCreate);
        form.setManaged(canCreate);

        packagingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        packagingTable.setPrefHeight(220);
        packagingTable.getColumns().add(colP("Nom", ProductPackaging::nom));
        packagingTable.getColumns().add(colP("Symbole", p -> p.symbole() == null ? "" : p.symbole()));
        packagingTable.getColumns().add(colP("Qté base", p ->
                p.quantiteBase() == null ? "" : p.quantiteBase().toPlainString()));
        packagingTable.getColumns().add(colP("Prix", p -> ProductLabels.price(p.prixVente())));
        if (session.hasPermission("products.delete")) {
            TableColumn<ProductPackaging, Void> actions = new TableColumn<>();
            actions.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        return;
                    }
                    ProductPackaging pkg = getTableRow().getItem();
                    Button del = new Button("Suppr.");
                    del.getStyleClass().add("button-danger");
                    del.setOnAction(e -> deletePackaging(pkg));
                    setGraphic(del);
                }
            });
            packagingTable.getColumns().add(actions);
        }
        return new VBox(12, form, packagingTable);
    }

    private VBox auditPane() {
        auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        auditTable.setPrefHeight(280);
        auditTable.getColumns().add(colA("Date", a -> a.dateAction() == null ? "" : a.dateAction()));
        auditTable.getColumns().add(colA("Action", a -> a.action() == null ? "" : a.action()));
        auditTable.getColumns().add(colA("Détails", a -> a.details() == null ? "" : a.details()));
        auditTable.getColumns().add(colA("Utilisateur", a -> a.utilisateur() == null ? "" : a.utilisateur()));
        Button refresh = new Button("Actualiser audit");
        refresh.getStyleClass().add("button-ghost");
        refresh.setOnAction(e -> load());
        return new VBox(12, refresh, auditTable);
    }

    private void load() {
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> {
            FormData data = new FormData();
            data.categories = categoriesApi.getTree();
            data.brands = brandsApi.findAll();
            data.suppliers = suppliersApi.findAll();
            data.units = unitsApi.findAll();
            if (productId != null) {
                data.product = products.getById(productId);
                data.history = products.priceHistory(productId);
                data.packagings = products.listPackagings(productId);
                data.audit = products.auditHistory(productId);
            }
            return data;
        }, this::bind, this::showError);
    }

    private void bind(FormData data) {
        setBusy(false);
        fillRefs(marque, data.brands.stream().map(b -> new RefOption(String.valueOf(b.id()), b.nom())).toList());
        List<RefOption> cats = new ArrayList<>();
        flatten(data.categories, "", cats);
        fillRefs(categorie, cats);
        fillRefs(fournisseur, data.suppliers.stream()
                .map(s -> new RefOption(String.valueOf(s.id()), s.nom())).toList());
        fillRefs(unite, data.units.stream()
                .map(u -> new RefOption(String.valueOf(u.id()), u.toString())).toList());
        if (productId == null) {
            // Nouveau produit : "Pièce (pcs)" par défaut, cas le plus courant.
            data.units.stream()
                    .filter(u -> "pcs".equalsIgnoreCase(u.symbole()))
                    .findFirst()
                    .ifPresent(u -> select(unite, u.id()));
        }
        current = data.product;
        if (current == null) {
            stockBadge.setVisible(false);
            stockBadge.setManaged(false);
            lifecycleBadge.setVisible(false);
            lifecycleBadge.setManaged(false);
            return;
        }
        nom.setText(nullToEmpty(current.nom()));
        sku.setText(nullToEmpty(current.sku()));
        codeBarre.setText(nullToEmpty(current.codeBarre()));
        generateBarcode.setSelected(false);
        description.setText(nullToEmpty(current.description()));
        prixAchat.setText(decimalText(current.prixAchat()));
        prixVente.setText(decimalText(current.prixVente()));
        select(marque, current.marqueId());
        select(categorie, current.categorieId());
        select(fournisseur, current.fournisseurPrincipalId());
        select(unite, current.unitId());
        selectCode(statut, current.statut());
        selectCode(cycleVie, current.cycleVie());
        stockBadge.setText("Stock : " + current.stockLabel());
        stockBadge.setVisible(true);
        stockBadge.setManaged(true);
        lifecycleBadge.setText(ProductLabels.lifecycle(current.cycleVie()));
        lifecycleBadge.getStyleClass().removeAll("badge-neutral", "badge-warning", "badge-success", "badge-danger");
        lifecycleBadge.getStyleClass().add(lifecycleBadgeClass(current.cycleVie()));
        lifecycleBadge.setVisible(true);
        lifecycleBadge.setManaged(true);
        if (current.hasVariants()) {
            codeBarre.setDisable(true);
            generateBarcode.setDisable(true);
        }
        historyTable.getItems().setAll(data.history);
        variantsTable.getItems().setAll(current.variantes() == null ? List.of() : current.variantes());
        packagingTable.getItems().setAll(data.packagings);
        auditTable.getItems().setAll(data.audit);
        renderImages();
        buildWorkflowButtons();
    }

    private void save() {
        String nameError = ProductValidator.validateName(nom.getText());
        if (nameError != null) {
            errorBanner.show(nameError);
            return;
        }
        String priceError = firstNonNull(
                ProductValidator.validateOptionalPrice(prixAchat.getText()),
                ProductValidator.validateOptionalPrice(prixVente.getText()));
        if (priceError != null) {
            errorBanner.show(priceError);
            return;
        }
        ProductDraft draft = readDraft();
        errorBanner.hide();
        setBusy(true);
        if (productId == null) {
            FxAsync.run(() -> products.create(draft), created -> {
                setBusy(false);
                if (created != null && created.id() != null && onCreated != null) {
                    onCreated.accept(created.id());
                } else {
                    onBack.run();
                }
            }, this::showError);
        } else {
            FxAsync.run(() -> products.update(productId, draft), updated -> {
                setBusy(false);
                load();
            }, this::showError);
        }
    }

    private ProductDraft readDraft() {
        ProductDraft draft = ProductDraft.from(current);
        draft.nom = ProductValidator.normalize(nom.getText());
        draft.sku = ProductValidator.normalize(sku.getText());
        draft.codeBarre = ProductValidator.normalize(codeBarre.getText());
        draft.generateBarcode = generateBarcode.isSelected();
        draft.description = ProductValidator.normalize(description.getText());
        draft.marqueId = selectedId(marque);
        draft.categorieId = selectedId(categorie);
        draft.fournisseurPrincipalId = selectedId(fournisseur);
        draft.unitId = selectedId(unite);
        draft.prixAchat = silentPrice(prixAchat.getText());
        draft.prixVente = silentPrice(prixVente.getText());
        draft.statut = selectedCode(statut, "ACTIF");
        draft.cycleVie = selectedCode(cycleVie, "ACTIF");
        return draft;
    }

    private void applyPrice() {
        String error = ProductValidator.validateRequiredPrice(newPrice.getText());
        if (error != null) {
            errorBanner.show(error);
            return;
        }
        BigDecimal value = ProductValidator.parsePrice(newPrice.getText());
        String type = priceType.getValue() == null ? "VENTE" : priceType.getValue();
        errorBanner.hide();
        setBusy(true);
        FxAsync.run(() -> products.updatePrice(productId, type, value), updated -> {
            newPrice.clear();
            load();
        }, this::showError);
    }

    private void uploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Image produit");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        java.io.File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        setBusy(true);
        boolean principale = uploadAsPrimary.isSelected()
                || current == null || current.images() == null || current.images().isEmpty();
        FxAsync.run(() -> {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return products.uploadImage(productId, file.getName(), bytes, principale);
        }, ignored -> load(), this::showError);
    }

    private void addVariant() {
        BigDecimal prix = silentPrice(variantPrix.getText());
        setBusy(true);
        FxAsync.run(() -> products.addVariant(productId,
                variantCouleur.getText(), variantTaille.getText(), variantSku.getText(),
                prix, variantGenBarcode.isSelected()), created -> {
            variantCouleur.clear();
            variantTaille.clear();
            variantSku.clear();
            variantPrix.clear();
            load();
        }, this::showError);
    }

    private void deleteVariant(ProductVariant variant) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer", "Supprimer cette variante ?")) {
            return;
        }
        setBusy(true);
        FxAsync.runVoid(() -> products.deleteVariant(productId, variant.id()), this::load, this::showError);
    }

    private void addPackaging() {
        if (pkgNom.getText() == null || pkgNom.getText().isBlank()) {
            errorBanner.show("Nom du conditionnement obligatoire.");
            return;
        }
        BigDecimal qty;
        try {
            qty = new BigDecimal(pkgQty.getText().trim());
        } catch (Exception e) {
            errorBanner.show("Quantité de base invalide.");
            return;
        }
        BigDecimal prix = silentPrice(pkgPrix.getText());
        setBusy(true);
        FxAsync.run(() -> products.addPackaging(productId, pkgNom.getText().trim(),
                blankToNull(pkgSymbole.getText()), qty, prix), created -> {
            pkgNom.clear();
            pkgSymbole.clear();
            pkgQty.clear();
            pkgPrix.clear();
            load();
        }, this::showError);
    }

    private void deletePackaging(ProductPackaging pkg) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer", "Supprimer ce conditionnement ?")) {
            return;
        }
        setBusy(true);
        FxAsync.runVoid(() -> products.deletePackaging(productId, pkg.id()), this::load, this::showError);
    }

    private void renderImages() {
        imagePane.getChildren().clear();
        if (current == null || current.images() == null || current.images().isEmpty()) {
            Label empty = new Label("Aucune image");
            empty.getStyleClass().add("empty-state");
            imagePane.getChildren().add(empty);
            return;
        }
        for (ProductImage image : current.images()) {
            imagePane.getChildren().add(imageCard(image));
        }
    }

    private VBox imageCard(ProductImage image) {
        ImageView view = new ImageView();
        view.setFitWidth(120);
        view.setFitHeight(120);
        view.setPreserveRatio(true);
        String url = resolveUrl(image.url());
        if (url != null) {
            try {
                view.setImage(new Image(url, 120, 120, true, true, true));
            } catch (Exception ignored) {
                // nom de fichier ci-dessous
            }
        }
        Label name = new Label(image.fileName() == null ? "" : image.fileName());
        name.setWrapText(true);
        name.setMaxWidth(120);
        VBox box = new VBox(6, view, name);
        if (image.principale()) {
            Label main = new Label("Principale");
            main.getStyleClass().add("badge");
            box.getChildren().add(main);
        } else if (session.hasPermission("products.create")) {
            Button setPrimary = new Button("Principale");
            setPrimary.getStyleClass().add("button-ghost");
            setPrimary.setOnAction(e -> setPrimaryFromUrl(image));
            box.getChildren().add(setPrimary);
        }
        if (session.hasPermission("products.delete")) {
            Button del = new Button("Suppr.");
            del.getStyleClass().add("button-danger");
            del.setOnAction(e -> deleteImage(image));
            box.getChildren().add(del);
        }
        box.getStyleClass().add("image-card");
        return box;
    }

    /** Ré-upload avec principale=true (seul mécanisme API pour forcer le primaire). */
    private void setPrimaryFromUrl(ProductImage image) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Recharger l'image comme principale");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        java.io.File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        setBusy(true);
        FxAsync.run(() -> {
            byte[] bytes = Files.readAllBytes(file.toPath());
            ProductImage uploaded = products.setPrimaryImage(productId, file.getName(), bytes);
            if (image.id() != null) {
                try {
                    products.deleteImage(productId, image.id());
                } catch (ApiException ignored) {
                    // ancienne image peut rester si droits insuffisants
                }
            }
            return uploaded;
        }, ignored -> load(), this::showError);
    }

    private void deleteImage(ProductImage image) {
        if (!ConfirmationDialog.confirm(getScene() == null ? null : getScene().getWindow(),
                "Supprimer l'image", "Supprimer cette image ?")) {
            return;
        }
        setBusy(true);
        FxAsync.runVoid(() -> products.deleteImage(productId, image.id()), this::load, this::showError);
    }

    private String resolveUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return session.api().getBaseUrl() + (url.startsWith("/") ? url : "/" + url);
    }

    private void showError(Throwable error) {
        setBusy(false);
        if (error instanceof ApiException api && api.isUnauthorized()) {
            return;
        }
        if (error instanceof ApiException api) {
            errorBanner.show(ApiException.userMessage(api));
        } else {
            errorBanner.show("Une erreur est survenue.");
        }
    }

    private void setBusy(boolean busy) {
        loading.setLoading(busy);
        saveButton.setDisable(busy);
    }

    private static VBox labeled(String label, javafx.scene.Node node) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        if (node instanceof javafx.scene.layout.Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        VBox box = new VBox(4, l, node);
        box.setFillWidth(true);
        return box;
    }

    private static TableColumn<PriceHistory, String> col(String title, java.util.function.Function<PriceHistory, String> fn) {
        TableColumn<PriceHistory, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue() == null ? "" : fn.apply(data.getValue())));
        return col;
    }

    private static TableColumn<ProductVariant, String> colV(String title,
                                                            java.util.function.Function<ProductVariant, String> fn) {
        TableColumn<ProductVariant, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue() == null ? "" : fn.apply(data.getValue())));
        return col;
    }

    private static TableColumn<ProductPackaging, String> colP(String title,
                                                              java.util.function.Function<ProductPackaging, String> fn) {
        TableColumn<ProductPackaging, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue() == null ? "" : fn.apply(data.getValue())));
        return col;
    }

    private static TableColumn<AuditLogEntry, String> colA(String title,
                                                           java.util.function.Function<AuditLogEntry, String> fn) {
        TableColumn<AuditLogEntry, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue() == null ? "" : fn.apply(data.getValue())));
        return col;
    }

    private static void fillRefs(ComboBox<RefOption> combo, List<RefOption> items) {
        combo.getItems().setAll(RefOption.none());
        combo.getItems().addAll(items);
        combo.getSelectionModel().selectFirst();
    }

    private static void flatten(List<Category> cats, String prefix, List<RefOption> out) {
        if (cats == null) {
            return;
        }
        for (Category cat : cats) {
            String label = prefix.isEmpty() ? cat.nom() : prefix + " > " + cat.nom();
            out.add(new RefOption(String.valueOf(cat.id()), label));
            flatten(cat.childrenOrEmpty(), label, out);
        }
    }

    private static void select(ComboBox<RefOption> combo, Long id) {
        if (id == null) {
            combo.getSelectionModel().selectFirst();
            return;
        }
        combo.getItems().stream()
                .filter(o -> o.id != null && o.id.equals(String.valueOf(id)))
                .findFirst()
                .ifPresent(combo.getSelectionModel()::select);
    }

    private static void selectCode(ComboBox<RefOption> combo, String code) {
        if (code == null) {
            return;
        }
        combo.getItems().stream()
                .filter(o -> code.equals(o.id))
                .findFirst()
                .ifPresent(combo.getSelectionModel()::select);
    }

    private static Long selectedId(ComboBox<RefOption> combo) {
        RefOption option = combo.getValue();
        if (option == null || option.id == null) {
            return null;
        }
        return Long.parseLong(option.id);
    }

    private static String selectedCode(ComboBox<RefOption> combo, String fallback) {
        RefOption option = combo.getValue();
        return option == null || option.id == null ? fallback : option.id;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private static String lifecycleBadgeClass(String cycle) {
        if (cycle == null) {
            return "badge-neutral";
        }
        return switch (cycle) {
            case "ACTIF" -> "badge-success";
            case "EN_ATTENTE_VALIDATION" -> "badge-warning";
            case "SUSPENDU", "ARRETE", "ARCHIVE" -> "badge-danger";
            default -> "badge-neutral";
        };
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String decimalText(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static BigDecimal silentPrice(String raw) {
        try {
            return ProductValidator.parsePrice(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class FormData {
        List<Category> categories = List.of();
        List<Brand> brands = List.of();
        List<Supplier> suppliers = List.of();
        List<Unit> units = List.of();
        Product product;
        List<PriceHistory> history = List.of();
        List<ProductPackaging> packagings = List.of();
        List<AuditLogEntry> audit = List.of();
    }

    static final class RefOption {
        final String id;
        final String label;

        RefOption(String id, String label) {
            this.id = id;
            this.label = label;
        }

        static RefOption none() {
            return new RefOption(null, "—");
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
