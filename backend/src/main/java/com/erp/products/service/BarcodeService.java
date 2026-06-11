package com.erp.products.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.oned.UPCAWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.erp.products.domain.enums.BarcodeType;
import com.erp.products.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

@Service
public class BarcodeService {

    public String generateBase64(String content, BarcodeType type) {
        try {
            BitMatrix matrix = switch (type) {
                case EAN13 -> generateEan13(content);
                case UPC -> generateUpc(content);
                case CODE128 -> generateCode128(content);
                case QR_CODE -> generateQrCode(content);
            };
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new BusinessException("Impossible de générer le code-barres: " + e.getMessage());
        }
    }

    private BitMatrix generateEan13(String content) throws Exception {
        String normalized = normalizeNumeric(content, 12);
        return new EAN13Writer().encode(normalized, BarcodeFormat.EAN_13, 300, 100);
    }

    private BitMatrix generateUpc(String content) throws Exception {
        String normalized = normalizeNumeric(content, 11);
        return new UPCAWriter().encode(normalized, BarcodeFormat.UPC_A, 300, 100);
    }

    private BitMatrix generateCode128(String content) throws Exception {
        return new Code128Writer().encode(content, BarcodeFormat.CODE_128, 300, 100);
    }

    private BitMatrix generateQrCode(String content) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 250, 250, hints);
    }

    private String normalizeNumeric(String content, int length) {
        String digits = content.replaceAll("\\D", "");
        if (digits.length() > length) {
            digits = digits.substring(0, length);
        }
        while (digits.length() < length) {
            digits = "0" + digits;
        }
        return digits;
    }
}
