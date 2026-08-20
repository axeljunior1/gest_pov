package com.gestpov.desktop.ui.pos;

import com.gestpov.desktop.ui.Reloadable;

import com.gestpov.desktop.model.Sale;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.PosClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.ui.products.ProductLabels;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public final class PosHistoryView extends StackPane implements Reloadable {

    private final SessionContext session;
    private final PosClient pos;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<Sale> table = new TableView<>();
    private final TextField search = new TextField();
    private final CheckBox sessionOnly = new CheckBox("Session courante uniquement");
    private final Label count = new Label();
    private List<Sale> all = List.of();

    public PosHistoryView(SessionContext session) {
        this.session = session;
        this.pos = new PosClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Historique caisse");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Ventes validées — double-clic ou Ticket pour réimprimer");
        sub.getStyleClass().add("page-sub");
        count.getStyleClass().add("page-sub");

        search.setPromptText("Filtrer n°, client…");
        search.textProperty().addListener((o, a, b) -> filterTable());
        sessionOnly.setOnAction(e -> reload());

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());
        Button ticket = new Button("Ticket");
        ticket.getStyleClass().add("button-primary");
        ticket.setOnAction(e -> showTicket());
        ticket.setDisable(!(session.hasPermission("pos.ticket.print")
                || session.hasPermission("pos.ticket.reprint")));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("N°", Sale::saleNumber),
                col("Statut", Sale::status),
                col("Client", s -> s.customerName() == null ? "—" : s.customerName()),
                col("Vendeur", s -> s.sellerName() == null ? "—" : s.sellerName()),
                col("Total", s -> ProductLabels.price(s.total() == null ? BigDecimal.ZERO : s.total()))
        );
        table.setPlaceholder(new EmptyState("Aucune vente"));
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                showTicket();
            }
        });

        HBox bar = new HBox(10, search, sessionOnly, refresh, ticket);
        HBox.setHgrow(search, Priority.ALWAYS);
        VBox page = new VBox(12, title, sub, error, bar, count, table);
        page.getStyleClass().add("content");
        page.setPadding(new Insets(0));
        VBox.setVgrow(table, Priority.ALWAYS);
        return page;
    }

    @Override
    public void reload() {
        loading.setLoading(true);
        boolean only = sessionOnly.isSelected();
        FxAsync.run(() -> pos.listCompletedSales(only, 100), list -> {
            loading.setLoading(false);
            all = list;
            filterTable();
        }, this::fail);
    }

    private void filterTable() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        var filtered = all.stream().filter(s ->
                q.isEmpty()
                        || (s.saleNumber() != null && s.saleNumber().toLowerCase(Locale.ROOT).contains(q))
                        || (s.customerName() != null && s.customerName().toLowerCase(Locale.ROOT).contains(q))
                        || (s.sellerName() != null && s.sellerName().toLowerCase(Locale.ROOT).contains(q))
        ).toList();
        table.getItems().setAll(filtered);
        count.setText(filtered.size() + " vente(s)");
    }

    private void showTicket() {
        Sale s = table.getSelectionModel().getSelectedItem();
        if (s == null || s.id() == null) {
            error.show("Sélectionnez une vente.");
            return;
        }
        loading.setLoading(true);
        FxAsync.run(() -> pos.ticket(s.id()), ticketNode -> {
            loading.setLoading(false);
            PosTicketHelper.showAndOfferPrint(getScene() == null ? null : getScene().getWindow(), ticketNode);
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

    private static TableColumn<Sale, String> col(String title, java.util.function.Function<Sale, String> fn) {
        TableColumn<Sale, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return c;
    }
}
