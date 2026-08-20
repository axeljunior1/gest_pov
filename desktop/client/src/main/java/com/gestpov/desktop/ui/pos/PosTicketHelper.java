package com.gestpov.desktop.ui.pos;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/** Formatage et impression / export ticket POS. */
public final class PosTicketHelper {

    private PosTicketHelper() {
    }

    public static String format(JsonNode node) {
        if (node == null || node.isNull()) {
            return "(ticket vide)";
        }
        StringBuilder sb = new StringBuilder();
        line(sb, center(text(node, "companyName"), 42));
        if (node.hasNonNull("companyAddress")) {
            line(sb, center(node.get("companyAddress").asText(), 42));
        }
        if (node.hasNonNull("registerName")) {
            line(sb, center(node.get("registerName").asText(), 42));
        }
        line(sb, "------------------------------------------");
        if (node.hasNonNull("ticketNumber") || node.hasNonNull("saleNumber")) {
            String num = text(node, "ticketNumber");
            if (num.isBlank()) {
                num = text(node, "saleNumber");
            }
            line(sb, "Ticket : " + num);
        }
        if (node.hasNonNull("saleDate")) {
            line(sb, "Date   : " + node.get("saleDate").asText());
        }
        if (node.hasNonNull("cashierName")) {
            line(sb, "Caissier: " + node.get("cashierName").asText());
        }
        line(sb, "------------------------------------------");
        JsonNode lines = node.get("lines");
        if (lines != null && lines.isArray()) {
            for (JsonNode l : lines) {
                String name = text(l, "productNom");
                if (l.hasNonNull("variantNameSnapshot")) {
                    name += " / " + l.get("variantNameSnapshot").asText();
                }
                if (l.hasNonNull("packagingNameSnapshot")) {
                    name += " [" + l.get("packagingNameSnapshot").asText() + "]";
                }
                line(sb, name);
                String qty = l.hasNonNull("quantity") ? l.get("quantity").asText() : "1";
                String tot = l.hasNonNull("lineTotal") ? l.get("lineTotal").asText() : "";
                line(sb, String.format(Locale.ROOT, "  %s x %s", qty, money(l, "unitPrice"))
                        + padLeft(tot, 20));
            }
        }
        line(sb, "------------------------------------------");
        line(sb, padRow("Sous-total", money(node, "subtotal")));
        if (node.hasNonNull("discountTotal") && !"0".equals(node.get("discountTotal").asText())
                && !"0.00".equals(node.get("discountTotal").asText())) {
            line(sb, padRow("Remise", money(node, "discountTotal")));
        }
        if (node.hasNonNull("taxTotal")) {
            line(sb, padRow("Taxe", money(node, "taxTotal")));
        }
        line(sb, padRow("TOTAL", money(node, "total")));
        JsonNode payments = node.get("payments");
        if (payments != null && payments.isArray()) {
            for (JsonNode p : payments) {
                line(sb, padRow(text(p, "method"), money(p, "amount")));
            }
        }
        if (node.hasNonNull("changeAmount")) {
            line(sb, padRow("Monnaie", money(node, "changeAmount")));
        }
        if (node.hasNonNull("ticketFooter")) {
            line(sb, "------------------------------------------");
            line(sb, center(node.get("ticketFooter").asText(), 42));
        }
        return sb.toString();
    }

    public static void showAndOfferPrint(Window owner, JsonNode ticket) {
        String text = format(ticket);
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setPrefSize(440, 480);
        area.setStyle("-fx-font-family: Consolas, 'Courier New', monospace; -fx-font-size: 13px;");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle("Ticket de caisse");
        alert.setHeaderText("Ticket");
        alert.getDialogPane().setContent(area);
        ButtonType print = new ButtonType("Imprimer");
        ButtonType save = new ButtonType("Enregistrer…");
        alert.getButtonTypes().setAll(print, save, ButtonType.CLOSE);
        var result = alert.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        if (result.get() == print) {
            printText(owner, text);
        } else if (result.get() == save) {
            saveText(owner, text);
        }
    }

    private static void printText(Window owner, String content) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            new Alert(Alert.AlertType.WARNING, "Aucune imprimante disponible.").showAndWait();
            return;
        }
        if (!job.showPrintDialog(owner)) {
            return;
        }
        Text text = new Text(content);
        text.setFont(Font.font("Consolas", 10));
        if (job.printPage(text)) {
            job.endJob();
        }
    }

    private static void saveText(Window owner, String content) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le ticket");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texte", "*.txt"));
        chooser.setInitialFileName("ticket.txt");
        var file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Écriture impossible : " + e.getMessage()).showAndWait();
        }
    }

    private static void line(StringBuilder sb, String s) {
        sb.append(s).append('\n');
    }

    private static String text(JsonNode n, String field) {
        return n != null && n.hasNonNull(field) ? n.get(field).asText("") : "";
    }

    private static String money(JsonNode n, String field) {
        if (n == null || !n.hasNonNull(field)) {
            return "0.00";
        }
        return n.get(field).asText();
    }

    private static String center(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        int pad = (width - s.length()) / 2;
        return " ".repeat(pad) + s;
    }

    private static String padRow(String left, String right) {
        String l = left == null ? "" : left;
        String r = right == null ? "" : right;
        int spaces = Math.max(1, 42 - l.length() - r.length());
        return l + " ".repeat(spaces) + r;
    }

    private static String padLeft(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s;
        }
        return " ".repeat(width - s.length()) + s;
    }
}
