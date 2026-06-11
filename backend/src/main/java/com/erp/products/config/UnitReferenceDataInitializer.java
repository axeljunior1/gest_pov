package com.erp.products.config;

import com.erp.products.domain.entity.UnitConversion;
import com.erp.products.domain.entity.UnitOfMeasure;
import com.erp.products.repository.UnitConversionRepository;
import com.erp.products.repository.UnitOfMeasureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalogue d'unités SI / usage courant en France, avec conversions globales pré-définies.
 * Non modifiable par l'utilisateur via l'UI standard.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class UnitReferenceDataInitializer implements ApplicationRunner {

    private final UnitOfMeasureRepository unitRepository;
    private final UnitConversionRepository conversionRepository;

    private record UnitDef(String nom, String symbole) {}

    private record ConversionDef(String fromSymbole, String toSymbole, String factor) {}

    private static final UnitDef[] UNITS = {
            new UnitDef("Pièce", "pcs"),
            new UnitDef("Douzaine", "dz"),
            new UnitDef("Kilogramme", "kg"),
            new UnitDef("Gramme", "g"),
            new UnitDef("Milligramme", "mg"),
            new UnitDef("Tonne", "t"),
            new UnitDef("Quintal", "q"),
            new UnitDef("Litre", "L"),
            new UnitDef("Millilitre", "mL"),
            new UnitDef("Centilitre", "cL"),
            new UnitDef("Décilitre", "dL"),
            new UnitDef("Hectolitre", "hL"),
            new UnitDef("Mètre cube", "m³"),
            new UnitDef("Mètre", "m"),
            new UnitDef("Centimètre", "cm"),
            new UnitDef("Millimètre", "mm"),
            new UnitDef("Kilomètre", "km"),
            new UnitDef("Mètre carré", "m²"),
            new UnitDef("Centimètre carré", "cm²"),
            new UnitDef("Millimètre carré", "mm²"),
    };

    /** 1 unité source = factor × unité cible */
    private static final ConversionDef[] CONVERSIONS = {
            // Masse
            new ConversionDef("kg", "g", "1000"),
            new ConversionDef("g", "mg", "1000"),
            new ConversionDef("t", "kg", "1000"),
            new ConversionDef("q", "kg", "100"),
            // Volume
            new ConversionDef("L", "mL", "1000"),
            new ConversionDef("cL", "mL", "10"),
            new ConversionDef("dL", "mL", "100"),
            new ConversionDef("hL", "L", "100"),
            new ConversionDef("m³", "L", "1000"),
            // Longueur
            new ConversionDef("km", "m", "1000"),
            new ConversionDef("m", "cm", "100"),
            new ConversionDef("cm", "mm", "10"),
            // Surface
            new ConversionDef("m²", "cm²", "10000"),
            new ConversionDef("cm²", "mm²", "100"),
            // Comptage
            new ConversionDef("dz", "pcs", "12"),
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (unitRepository.count() > 0) {
            return;
        }

        log.info("Initialisation du catalogue d'unités de référence (SI / France)...");

        Map<String, UnitOfMeasure> bySymbole = new LinkedHashMap<>();
        for (UnitDef def : UNITS) {
            UnitOfMeasure unit = UnitOfMeasure.builder()
                    .nom(def.nom())
                    .symbole(def.symbole())
                    .build();
            bySymbole.put(def.symbole(), unitRepository.save(unit));
        }

        for (ConversionDef def : CONVERSIONS) {
            UnitOfMeasure from = bySymbole.get(def.fromSymbole());
            UnitOfMeasure to = bySymbole.get(def.toSymbole());
            if (from == null || to == null) {
                continue;
            }
            if (conversionRepository.findByFromUnitIdAndToUnitId(from.getId(), to.getId()).isPresent()) {
                continue;
            }
            conversionRepository.save(UnitConversion.builder()
                    .fromUnit(from)
                    .toUnit(to)
                    .factor(new BigDecimal(def.factor()))
                    .build());
        }

        log.info("Catalogue initialisé : {} unités, {} conversions globales", UNITS.length, CONVERSIONS.length);
    }
}
