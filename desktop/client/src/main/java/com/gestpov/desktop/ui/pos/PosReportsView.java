package com.gestpov.desktop.ui.pos;

import com.gestpov.desktop.ui.Reloadable;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.PosClient;
import com.gestpov.desktop.session.SessionContext;
import com.gestpov.desktop.ui.component.EmptyState;
import com.gestpov.desktop.ui.component.ErrorBanner;
import com.gestpov.desktop.ui.component.ListPager;
import com.gestpov.desktop.ui.component.LoadingOverlay;
import com.gestpov.desktop.ui.products.ProductLabels;
import com.gestpov.desktop.util.FxAsync;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PosReportsView extends StackPane implements Reloadable {

    private final PosClient pos;
    private final ErrorBanner error = new ErrorBanner();
    private final LoadingOverlay loading = new LoadingOverlay();
    private final TableView<JsonNode> table = new TableView<>();
    private final ListPager<JsonNode> pager = new ListPager<>(table);
    private final VBox detail = new VBox(4);
    private final Label count = new Label();

    private final TextField search = new TextField();
    private final CheckBox differenceOnly = new CheckBox("Écart uniquement");
    private final DatePicker dateFrom = new DatePicker();
    private final DatePicker dateTo = new DatePicker();

    private List<JsonNode> all = List.of();

    public PosReportsView(SessionContext session) {
        this.pos = new PosClient(session.api());
        getChildren().addAll(build(), loading);
        reload();
    }

    private VBox build() {
        Label title = new Label("Rapports de caisse");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Sessions fermées — sélectionnez pour le détail");
        sub.getStyleClass().add("page-sub");
        count.getStyleClass().add("page-sub");

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("button-secondary");
        refresh.setOnAction(e -> reload());

        search.setPromptText("N° session ou caissier…");
        search.textProperty().addListener((o, a, b) -> applyFilter());
        differenceOnly.setOnAction(e -> applyFilter());
        dateFrom.setPromptText("Du");
        dateFrom.setOnAction(e -> applyFilter());
        dateTo.setPromptText("Au");
        dateTo.setOnAction(e -> applyFilter());
        Button clearFilters = new Button("×");
        clearFilters.getStyleClass().add("button-ghost");
        clearFilters.setTooltip(new javafx.scene.control.Tooltip("Effacer les filtres"));
        clearFilters.setOnAction(e -> {
            search.clear();
            differenceOnly.setSelected(false);
            dateFrom.setValue(null);
            dateTo.setValue(null);
            applyFilter();
        });

        HBox bar = new HBox(8, search, differenceOnly, dateFrom, dateTo, clearFilters, refresh);
        bar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                col("Session", n -> text(n, "sessionNumber")),
                col("Caissier", n -> text(n, "cashierName")),
                col("Ouverture", n -> text(n, "openedAt")),
                col("Clôture", n -> text(n, "closedAt")),
                col("Ventes", n -> String.valueOf(n == null ? 0 : n.path("saleCount").asInt(0))),
                col("Total", n -> money(n, "totalRevenue")),
                differenceCol()
        );
        table.setPlaceholder(new EmptyState("Aucune session fermée"));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> loadReport(b));

        detail.getStyleClass().add("card");
        detail.setPadding(new Insets(12));
        detail.getChildren().add(new Label("Sélectionnez une session"));

        VBox page = new VBox(12, title, sub, error, bar, count, table, pager.bar(), detail);
        page.getStyleClass().add("content");
        VBox.setVgrow(table, Priority.ALWAYS);
        return page;
    }

    @Override
    public void reload() {
        loading.setLoading(true);
        FxAsync.run(() -> pos.listClosedSessions(200), list -> {
            loading.setLoading(false);
            all = list;
            applyFilter();
        }, this::fail);
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        boolean onlyDiff = differenceOnly.isSelected();
        LocalDate from = dateFrom.getValue();
        LocalDate to = dateTo.getValue();

        List<JsonNode> filtered = new ArrayList<>();
        for (JsonNode n : all) {
            if (!q.isEmpty()
                    && !text(n, "sessionNumber").toLowerCase(Locale.ROOT).contains(q)
                    && !text(n, "cashierName").toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            if (onlyDiff && decimal(n, "differenceAmount").compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if ((from != null || to != null) && !matchesDate(n, "openedAt", from, to)) {
                continue;
            }
            filtered.add(n);
        }
        pager.setItems(filtered);
        count.setText(filtered.size() + " session(s)");
    }

    private static boolean matchesDate(JsonNode n, String field, LocalDate from, LocalDate to) {
        if (n == null || !n.hasNonNull(field)) {
            return false;
        }
        LocalDate date;
        try {
            date = java.time.Instant.parse(n.get(field).asText()).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            return false;
        }
        if (from != null && date.isBefore(from)) {
            return false;
        }
        return to == null || !date.isAfter(to);
    }

    private void loadReport(JsonNode sessionNode) {
        if (sessionNode == null || !sessionNode.hasNonNull("id")) {
            return;
        }
        long id = sessionNode.get("id").asLong();
        loading.setLoading(true);
        FxAsync.run(() -> pos.sessionReport(id), report -> {
            loading.setLoading(false);
            List<Label> rows = new ArrayList<>();
            rows.add(row("Session", text(report, "sessionNumber")));
            rows.add(row("Caissier", text(report, "cashierName")));
            if (report.hasNonNull("closedBy") && !text(report, "closedBy").equals(text(report, "cashierName"))) {
                rows.add(row("Clôturé par", text(report, "closedBy")));
            }
            rows.add(row("Ventes", String.valueOf(report.path("saleCount").asInt(0))));
            rows.add(row("Total", money(report, "totalRevenue")));
            rows.add(row("Espèces", money(report, "cashRevenue")));
            rows.add(row("Carte", money(report, "cardRevenue")));
            if (nonZero(report, "mobileMoneyRevenue")) {
                rows.add(row("Mobile Money", money(report, "mobileMoneyRevenue")));
            }
            if (nonZero(report, "bankTransferRevenue")) {
                rows.add(row("Virement", money(report, "bankTransferRevenue")));
            }
            if (nonZero(report, "refundsTotal")) {
                rows.add(row("Remboursé (total)", money(report, "refundsTotal")));
                rows.add(row("Remboursé (espèces)", money(report, "cashRefundTotal")));
            }
            rows.add(row("Fond", money(report, "openingCashAmount")));
            rows.add(row("Attendu", money(report, "expectedCashAmount")));
            rows.add(row("Déclaré", money(report, "declaredCashAmount")));
            Label diffRow = row("Écart", money(report, "cashDifference"));
            colorBySeverity(diffRow, text(report, "differenceSeverity"));
            rows.add(diffRow);
            if (report.hasNonNull("differenceReasonLabel")) {
                rows.add(row("Motif", report.get("differenceReasonLabel").asText()));
            }
            if (report.hasNonNull("differenceComment") && !report.get("differenceComment").asText("").isBlank()) {
                rows.add(row("Commentaire", report.get("differenceComment").asText()));
            }
            if (report.hasNonNull("managerValidatedBy") && !report.get("managerValidatedBy").asText("").isBlank()) {
                rows.add(row("Validé par", text(report, "managerValidatedBy")
                        + (report.hasNonNull("managerValidatedAt") ? " · " + text(report, "managerValidatedAt") : "")));
            }
            detail.getChildren().setAll(rows);
        }, this::fail);
    }

    private static void colorBySeverity(Label label, String severity) {
        label.setStyle(switch (severity == null ? "" : severity) {
            case "MAJOR" -> "-fx-text-fill: #b91c1c; -fx-font-weight: 800;";
            case "MINOR" -> "-fx-text-fill: #b45309; -fx-font-weight: 700;";
            default -> "-fx-text-fill: #166534; -fx-font-weight: 700;";
        });
    }

    private static boolean nonZero(JsonNode n, String field) {
        return decimal(n, field).compareTo(BigDecimal.ZERO) != 0;
    }

    private void fail(Throwable t) {
        loading.setLoading(false);
        if (t instanceof ApiException api && !api.isUnauthorized()) {
            error.show(ApiException.userMessage(api));
        } else if (!(t instanceof ApiException a && a.isUnauthorized())) {
            error.show("Une erreur est survenue.");
        }
    }

    private static Label row(String k, String v) {
        Label l = new Label(k + " : " + v);
        l.getStyleClass().add("page-sub");
        return l;
    }

    private static String text(JsonNode n, String f) {
        return n != null && n.hasNonNull(f) ? n.get(f).asText("") : "—";
    }

    private static BigDecimal decimal(JsonNode n, String f) {
        if (n == null || !n.hasNonNull(f)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(n.get(f).asText());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static String money(JsonNode n, String f) {
        return ProductLabels.price(decimal(n, f));
    }

    private static TableColumn<JsonNode, String> col(String title, java.util.function.Function<JsonNode, String> fn) {
        TableColumn<JsonNode, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue() == null ? "" : fn.apply(d.getValue())));
        return c;
    }

    private static TableColumn<JsonNode, String> differenceCol() {
        TableColumn<JsonNode, String> c = col("Écart", n -> money(n, "differenceAmount"));
        c.setCellFactory(cc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                BigDecimal diff = decimal(getTableRow().getItem(), "differenceAmount");
                setStyle(diff.compareTo(BigDecimal.ZERO) == 0
                        ? "-fx-text-fill: #166534;"
                        : "-fx-text-fill: #b45309; -fx-font-weight: 700;");
            }
        });
        return c;
    }
}
