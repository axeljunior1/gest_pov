package com.gestpov.desktop.ui.pos;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;

import javax.print.PrintService;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Impression ticket en ESC/POS brut — pour les imprimantes thermiques sans pilote Windows
 * classique (passthrough via la file d'impression Windows, cf. PrinterOutputStream).
 *
 * Les caracteres accentues sont translitteres (e -> e, etc.) plutot que de parier sur la table
 * de caracteres du modele exact de l'imprimante (inconnu ici) — un ticket lisible sans accent
 * vaut mieux qu'un ticket illisible avec la mauvaise table de code.
 */
public final class EscPosPrinter {

    private EscPosPrinter() {
    }

    public static List<String> listPrinters() {
        return List.of(PrinterOutputStream.getListPrintServicesNames());
    }

    public static void printTicket(String printerName, JsonNode ticket, int widthChars) throws IOException {
        PrintService service = PrinterOutputStream.getPrintServiceByName(printerName);
        try (PrinterOutputStream out = new PrinterOutputStream(service)) {
            EscPos escpos = new EscPos(out);
            writeTicket(escpos, ticket, widthChars);
        }
    }

    public static void printTestPage(String printerName, int widthChars) throws IOException {
        PrintService service = PrinterOutputStream.getPrintServiceByName(printerName);
        try (PrinterOutputStream out = new PrinterOutputStream(service)) {
            EscPos escpos = new EscPos(out);
            Style title = new Style().setBold(true).setJustification(EscPosConst.Justification.Center)
                    .setFontSize(Style.FontSize._2, Style.FontSize._2);
            escpos.write(title, "GEST POV");
            escpos.feed(1);
            escpos.writeLF(center("Ticket de test", widthChars));
            escpos.writeLF(divider(widthChars));
            escpos.writeLF(center("Imprimante OK", widthChars));
            escpos.writeLF(divider(widthChars));
            escpos.feed(4);
            escpos.cut(EscPos.CutMode.FULL);
        }
    }

    private static void writeTicket(EscPos escpos, JsonNode node, int width) throws IOException {
        if (node == null || node.isNull()) {
            escpos.writeLF("(ticket vide)");
            escpos.feed(3).cut(EscPos.CutMode.FULL);
            return;
        }
        Style header = new Style().setBold(true).setJustification(EscPosConst.Justification.Center)
                .setFontSize(Style.FontSize._2, Style.FontSize._2);
        Style centerNormal = new Style().setJustification(EscPosConst.Justification.Center);
        Style bold = new Style().setBold(true);

        escpos.write(header, ascii(text(node, "companyName")));
        escpos.feed(1);
        if (node.hasNonNull("companyAddress")) {
            escpos.write(centerNormal, ascii(node.get("companyAddress").asText()));
        }
        if (node.hasNonNull("registerName")) {
            escpos.write(centerNormal, ascii(node.get("registerName").asText()));
        }
        escpos.writeLF(divider(width));

        if (node.hasNonNull("ticketNumber") || node.hasNonNull("saleNumber")) {
            String num = text(node, "ticketNumber");
            if (num.isBlank()) {
                num = text(node, "saleNumber");
            }
            escpos.writeLF(ascii("Ticket : " + num));
        }
        if (node.hasNonNull("saleDate")) {
            escpos.writeLF(ascii("Date   : " + node.get("saleDate").asText()));
        }
        if (node.hasNonNull("cashierName")) {
            escpos.writeLF(ascii("Caissier: " + node.get("cashierName").asText()));
        }
        escpos.writeLF(divider(width));

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
                escpos.writeLF(ascii(name));
                String qty = l.hasNonNull("quantity") ? l.get("quantity").asText() : "1";
                String tot = l.hasNonNull("lineTotal") ? l.get("lineTotal").asText() : "";
                String left = String.format(Locale.ROOT, "  %s x %s", qty, money(l, "unitPrice"));
                escpos.writeLF(ascii(padRow(left, tot, width)));
            }
        }
        escpos.writeLF(divider(width));
        escpos.writeLF(ascii(padRow("Sous-total", money(node, "subtotal"), width)));
        if (node.hasNonNull("discountTotal") && !"0".equals(node.get("discountTotal").asText())
                && !"0.00".equals(node.get("discountTotal").asText())) {
            escpos.writeLF(ascii(padRow("Remise", money(node, "discountTotal"), width)));
        }
        if (node.hasNonNull("taxTotal")) {
            escpos.writeLF(ascii(padRow("Taxe", money(node, "taxTotal"), width)));
        }
        escpos.write(bold, ascii(padRow("TOTAL", money(node, "total"), width)));

        JsonNode payments = node.get("payments");
        if (payments != null && payments.isArray()) {
            for (JsonNode p : payments) {
                escpos.writeLF(ascii(padRow(text(p, "method"), money(p, "amount"), width)));
            }
        }
        if (node.hasNonNull("changeAmount")) {
            escpos.writeLF(ascii(padRow("Monnaie", money(node, "changeAmount"), width)));
        }
        if (node.hasNonNull("ticketFooter")) {
            escpos.writeLF(divider(width));
            escpos.write(centerNormal, ascii(node.get("ticketFooter").asText()));
        }
        escpos.feed(4);
        escpos.cut(EscPos.CutMode.FULL);
    }

    private static String divider(int width) {
        return "-".repeat(Math.max(1, width));
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

    private static String padRow(String left, String right, int width) {
        String l = left == null ? "" : left;
        String r = right == null ? "" : right;
        int spaces = Math.max(1, width - l.length() - r.length());
        return l + " ".repeat(spaces) + r;
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

    /** Translitteration accents -> ASCII (table de code de l'imprimante inconnue et non fiable). */
    private static String ascii(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").replaceAll("[^\\x00-\\x7F]", "?");
    }
}
