package com.gestpov.desktop.ui.pos;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.net.ApiException;
import com.gestpov.desktop.net.PosClient;
import com.gestpov.desktop.ui.products.ProductLabels;
import com.gestpov.desktop.util.FxAsync;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Clôture session caissier : récap, cash déclaré, motif d'écart, rapport final visible.
 */
final class PosCloseSessionDialog {

    private PosCloseSessionDialog() {
    }

    static void show(Window owner, PosClient pos, Consumer<JsonNode> onClosed, Consumer<Throwable> onError) {
        Stage stage = new Stage();
        stage.setTitle("Clôture de caisse");
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }

        Label status = new Label("Chargement du récapitulatif…");
        status.setWrapText(true);
        status.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");

        VBox summary = new VBox(4);
        summary.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12;"
                + " -fx-border-color: #bfdbfe; -fx-border-radius: 12;");
        summary.setPadding(new Insets(12));

        TextField declaredCash = new TextField();
        declaredCash.setPromptText("Cash réellement présent");
        Label diffLabel = new Label();
        diffLabel.setWrapText(true);
        diffLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");

        ComboBox<ReasonOpt> reasonCombo = new ComboBox<>();
        reasonCombo.setPromptText("Motif de l'écart");
        reasonCombo.setMaxWidth(Double.MAX_VALUE);
        TextArea comment = new TextArea();
        comment.setPromptText("Commentaire (optionnel)");
        comment.setPrefRowCount(3);
        comment.setWrapText(true);

        TextField managerEmail = new TextField();
        managerEmail.setPromptText("Email manager");
        PasswordField managerPassword = new PasswordField();
        managerPassword.setPromptText("Mot de passe manager");
        TextField managerBadge = new TextField();
        managerBadge.setPromptText("Badge manager");
        PasswordField managerPin = new PasswordField();
        managerPin.setPromptText("Code PIN manager");

        VBox reasonBox = new VBox(8,
                section("Écart détecté — justification obligatoire"),
                labeled("Motif *", reasonCombo),
                labeled("Commentaire", comment));
        reasonBox.setVisible(false);
        reasonBox.setManaged(false);

        VBox managerEmailBox = new VBox(8, labeled("Email", managerEmail), labeled("Mot de passe", managerPassword));
        VBox managerBadgeBox = new VBox(8, labeled("Badge", managerBadge), labeled("Code PIN", managerPin));
        managerBadgeBox.setVisible(false);
        managerBadgeBox.setManaged(false);
        Button toggleManagerMode = new Button("Utiliser un badge");
        toggleManagerMode.setStyle("-fx-background-color: transparent; -fx-text-fill: #1d4ed8;"
                + " -fx-font-weight: 700; -fx-padding: 4 0 4 0;");
        toggleManagerMode.setOnAction(e -> {
            boolean toBadge = !managerBadgeBox.isVisible();
            managerEmailBox.setVisible(!toBadge);
            managerEmailBox.setManaged(!toBadge);
            managerBadgeBox.setVisible(toBadge);
            managerBadgeBox.setManaged(toBadge);
            toggleManagerMode.setText(toBadge ? "Utiliser email + mot de passe" : "Utiliser un badge");
        });

        VBox managerBox = new VBox(8,
                section("Validation manager obligatoire"),
                managerEmailBox, managerBadgeBox, toggleManagerMode);
        managerBox.setVisible(false);
        managerBox.setManaged(false);

        VBox form = new VBox(14,
                status,
                section("Résumé d'activité"),
                summary,
                labeled("Cash réellement présent en caisse *", declaredCash),
                diffLabel,
                reasonBox,
                managerBox);
        form.setPadding(new Insets(16));
        form.setPrefWidth(520);
        form.setStyle("-fx-background-color: #eef3ff;");

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: #eef3ff; -fx-background-color: #eef3ff;");

        Button cancel = new Button("Annuler");
        cancel.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #0f172a; -fx-font-weight: 700;"
                + " -fx-background-radius: 10; -fx-padding: 9 16 9 16;");
        cancel.setOnAction(e -> stage.close());

        Button validate = new Button("Valider la clôture");
        validate.setStyle("-fx-background-color: #dc2626; -fx-text-fill: #ffffff; -fx-font-weight: 700;"
                + " -fx-background-radius: 10; -fx-padding: 9 16 9 16;");
        validate.setDisable(true);
        validate.setDefaultButton(true);

        HBox footer = new HBox(12, cancel, validate);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 16, 16, 16));
        footer.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-width: 1 0 0 0;");

        BorderPane root = new BorderPane();
        root.setCenter(scroll);
        root.setBottom(footer);
        root.setStyle("-fx-background-color: #eef3ff;");

        Scene scene = new Scene(root, 560, 640);
        stage.setScene(scene);
        stage.setMinWidth(480);
        stage.setMinHeight(420);

        final JsonNode[] previewHolder = new JsonNode[1];

        Runnable refreshDiff = () -> {
            JsonNode preview = previewHolder[0];
            if (preview == null) {
                return;
            }
            BigDecimal expected = decimal(preview, "expectedCashAmount");
            BigDecimal declared;
            try {
                declared = parse(declaredCash.getText());
            } catch (Exception e) {
                diffLabel.setText("Montant invalide");
                reasonBox.setVisible(false);
                reasonBox.setManaged(false);
                managerBox.setVisible(false);
                managerBox.setManaged(false);
                return;
            }
            BigDecimal diff = declared.subtract(expected == null ? BigDecimal.ZERO : expected);
            boolean needsReason = diff.compareTo(BigDecimal.ZERO) != 0;
            diffLabel.setText("Écart calculé : " + ProductLabels.price(diff)
                    + (needsReason ? "" : " — caisse équilibrée"));
            reasonBox.setVisible(needsReason);
            reasonBox.setManaged(needsReason);
            boolean needMgr = needsReason && preview.path("requireManagerValidationForDifference").asBoolean(false);
            managerBox.setVisible(needMgr);
            managerBox.setManaged(needMgr);
        };

        declaredCash.textProperty().addListener((o, a, b) -> refreshDiff.run());

        validate.setOnAction(e -> {
            JsonNode preview = previewHolder[0];
            if (preview == null) {
                status.setText("Aperçu non chargé.");
                return;
            }
            BigDecimal declared;
            try {
                declared = parse(declaredCash.getText());
            } catch (Exception ex) {
                status.setText("Saisissez le cash réellement présent.");
                return;
            }
            BigDecimal expected = decimal(preview, "expectedCashAmount");
            BigDecimal diff = declared.subtract(expected == null ? BigDecimal.ZERO : expected);
            boolean needsReason = diff.compareTo(BigDecimal.ZERO) != 0;
            String reason = reasonCombo.getValue() == null ? null : reasonCombo.getValue().code();
            if (needsReason && (reason == null || reason.isBlank())) {
                status.setText("Motif de l'écart obligatoire.");
                return;
            }
            boolean needMgr = needsReason && preview.path("requireManagerValidationForDifference").asBoolean(false);
            boolean hasEmailPwd = !blank(managerEmail.getText()) && !blank(managerPassword.getText());
            boolean hasBadge = !blank(managerBadge.getText()) && !blank(managerPin.getText());
            if (needMgr && !hasEmailPwd && !hasBadge) {
                status.setText("Validation manager obligatoire (email+mot de passe ou badge+PIN).");
                return;
            }
            validate.setDisable(true);
            cancel.setDisable(true);
            status.setText("Clôture en cours…");
            FxAsync.run(() -> pos.closeSession(
                    declared,
                    true,
                    needsReason ? reason : null,
                    blankToNull(comment.getText()),
                    needMgr && hasEmailPwd ? managerEmail.getText().trim() : null,
                    needMgr && hasEmailPwd ? managerPassword.getText() : null,
                    needMgr && hasBadge ? managerBadge.getText().trim() : null,
                    needMgr && hasBadge ? managerPin.getText() : null
            ), report -> {
                // Afficher le rapport DANS cette fenêtre (évite un 2e Stage blanc)
                showReportInStage(stage, root, report, () -> {
                    stage.close();
                    onClosed.accept(report);
                });
            }, t -> {
                validate.setDisable(false);
                cancel.setDisable(false);
                status.setText(t instanceof ApiException api ? ApiException.userMessage(api) : "Échec de clôture.");
                onError.accept(t);
            });
        });

        FxAsync.run(pos::closePreview, preview -> {
            previewHolder[0] = preview;
            status.setText(preview.hasNonNull("sessionNumber")
                    ? "Session " + preview.get("sessionNumber").asText()
                    : "Récapitulatif");
            summary.getChildren().setAll(buildSummaryRows(preview));
            BigDecimal expected = decimal(preview, "expectedCashAmount");
            declaredCash.setText(expected == null ? "0" : expected.stripTrailingZeros().toPlainString());
            reasonCombo.getItems().setAll(parseReasons(preview));
            validate.setDisable(false);
            refreshDiff.run();
            declaredCash.requestFocus();
        }, err -> {
            status.setText("Impossible de charger l'aperçu.");
            onError.accept(err);
        });

        stage.showAndWait();
    }

    private static void showReportInStage(Stage stage, BorderPane root, JsonNode report, Runnable onDone) {
        stage.setTitle("Rapport de clôture");
        boolean balanced = report != null && report.path("balanced").asBoolean(false);
        String header = balanced ? "Caisse équilibrée — session fermée" : "Session fermée avec écart";

        Label headline = new Label(header);
        headline.setWrapText(true);
        headline.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        TextArea area = new TextArea(formatReportText(report));
        area.setEditable(false);
        area.setWrapText(true);
        area.setStyle("-fx-font-family: Consolas, 'Courier New', monospace; -fx-font-size: 13px;"
                + " -fx-control-inner-background: #ffffff; -fx-text-fill: #0f172a;");
        VBox.setVgrow(area, Priority.ALWAYS);

        Button close = new Button("Fermer");
        close.setDefaultButton(true);
        close.setStyle("-fx-background-color: #2563eb; -fx-text-fill: #ffffff; -fx-font-weight: 700;"
                + " -fx-background-radius: 10; -fx-padding: 10 20 10 20;");
        close.setOnAction(e -> onDone.run());

        HBox footer = new HBox(close);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 16, 16, 16));
        footer.setStyle("-fx-background-color: #ffffff;");

        VBox body = new VBox(12, headline, area);
        body.setPadding(new Insets(16));
        body.setStyle("-fx-background-color: #eef3ff;");
        VBox.setVgrow(area, Priority.ALWAYS);

        root.setCenter(body);
        root.setBottom(footer);
        Platform.runLater(close::requestFocus);
    }

    /** Fallback Alert si besoin (tests / appels hors Stage). */
    static void showReportAlert(Window owner, JsonNode report) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rapport de clôture");
        boolean balanced = report != null && report.path("balanced").asBoolean(false);
        alert.setHeaderText(balanced ? "Caisse équilibrée — session fermée" : "Session fermée avec écart");
        TextArea area = new TextArea(formatReportText(report));
        area.setEditable(false);
        area.setPrefSize(420, 360);
        area.setStyle("-fx-font-family: Consolas, monospace; -fx-font-size: 13px;");
        alert.getDialogPane().setContent(area);
        alert.getButtonTypes().setAll(ButtonType.CLOSE);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private static String formatReportText(JsonNode n) {
        if (n == null || n.isNull()) {
            return "(rapport vide)";
        }
        StringBuilder sb = new StringBuilder();
        append(sb, "Session", text(n, "sessionNumber"));
        append(sb, "Caissier", text(n, "cashierName"));
        append(sb, "Nombre de ventes", String.valueOf(n.path("saleCount").asInt(0)));
        append(sb, "Total ventes", money(n, "totalRevenue"));
        append(sb, "Espèces", money(n, "cashRevenue"));
        append(sb, "Carte", money(n, "cardRevenue"));
        append(sb, "Mobile money", money(n, "mobileMoneyRevenue"));
        append(sb, "Virement", money(n, "bankTransferRevenue"));
        append(sb, "Remboursements cash", money(n, "cashRefundTotal"));
        append(sb, "Fond initial", money(n, "openingCashAmount"));
        append(sb, "Cash attendu", money(n, "expectedCashAmount"));
        append(sb, "Cash déclaré", money(n, "declaredCashAmount"));
        append(sb, "Écart", money(n, "cashDifference"));
        if (n.hasNonNull("differenceReasonLabel")) {
            append(sb, "Motif écart", n.get("differenceReasonLabel").asText());
        } else if (n.hasNonNull("differenceReason")) {
            append(sb, "Motif écart", n.get("differenceReason").asText());
        }
        if (n.hasNonNull("differenceComment")) {
            append(sb, "Commentaire", n.get("differenceComment").asText());
        }
        if (n.hasNonNull("closedBy")) {
            append(sb, "Clôturé par", n.get("closedBy").asText());
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String label, String value) {
        sb.append(label).append(" : ").append(value == null || value.isBlank() ? "—" : value).append('\n');
    }

    private static List<Label> buildSummaryRows(JsonNode n) {
        List<Label> rows = new ArrayList<>();
        rows.add(row("Nombre de ventes", String.valueOf(n.path("saleCount").asInt(0))));
        rows.add(row("Total ventes", money(n, "totalRevenue")));
        rows.add(row("Espèces", money(n, "cashRevenue")));
        rows.add(row("Carte", money(n, "cardRevenue")));
        rows.add(row("Mobile money", money(n, "mobileMoneyRevenue")));
        rows.add(row("Virement", money(n, "bankTransferRevenue")));
        rows.add(row("Remboursements cash", money(n, "cashRefundTotal")));
        rows.add(row("Fond initial", money(n, "openingCashAmount")));
        rows.add(row("Cash attendu", money(n, "expectedCashAmount")));
        if (n.hasNonNull("declaredCashAmount")) {
            rows.add(row("Cash déclaré", money(n, "declaredCashAmount")));
        }
        if (n.hasNonNull("cashDifference")) {
            rows.add(row("Écart", money(n, "cashDifference")));
        }
        return rows;
    }

    private static List<ReasonOpt> parseReasons(JsonNode preview) {
        List<ReasonOpt> list = new ArrayList<>();
        JsonNode opts = preview.get("differenceReasonOptions");
        if (opts != null && opts.isArray()) {
            for (JsonNode o : opts) {
                String code = o.path("code").asText(null);
                String label = o.path("label").asText(code);
                if (code != null && !code.isBlank()) {
                    list.add(new ReasonOpt(code, label));
                }
            }
        }
        if (list.isEmpty()) {
            list.add(new ReasonOpt("CHANGE_ERROR", "Erreur de rendu monnaie"));
            list.add(new ReasonOpt("INPUT_ERROR", "Erreur de saisie"));
            list.add(new ReasonOpt("COUNT_ERROR", "Erreur de comptage"));
            list.add(new ReasonOpt("OTHER", "Autre"));
        }
        return list;
    }

    private static Label row(String label, String value) {
        Label l = new Label(label + " : " + value);
        l.setWrapText(true);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: #0f172a;");
        return l;
    }

    private static Label section(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        return l;
    }

    private static VBox labeled(String title, javafx.scene.Node node) {
        Label l = new Label(title);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        return new VBox(4, l, node);
    }

    private static String text(JsonNode n, String field) {
        return n != null && n.hasNonNull(field) ? n.get(field).asText("") : "";
    }

    private static String money(JsonNode n, String field) {
        BigDecimal v = decimal(n, field);
        return ProductLabels.price(v == null ? BigDecimal.ZERO : v);
    }

    private static BigDecimal decimal(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) {
            return null;
        }
        JsonNode v = n.get(field);
        try {
            if (v.isNumber()) {
                return v.decimalValue();
            }
            String raw = v.asText(null);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return new BigDecimal(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal parse(String text) {
        if (text == null || text.isBlank()) {
            throw new NumberFormatException("empty");
        }
        return new BigDecimal(text.trim().replace(',', '.'));
    }

    private static String blankToNull(String s) {
        return blank(s) ? null : s.trim();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private record ReasonOpt(String code, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
