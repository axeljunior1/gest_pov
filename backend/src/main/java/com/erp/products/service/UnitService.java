package com.erp.products.service;

import com.erp.products.domain.entity.UnitConversion;
import com.erp.products.domain.entity.UnitOfMeasure;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.ProductMapper;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.UnitConversionRepository;
import com.erp.products.repository.UnitOfMeasureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitOfMeasureRepository unitRepository;
    private final UnitConversionRepository conversionRepository;
    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    private static final int REFERENCE_UNIT_COUNT = 20;
    private static final int REFERENCE_CONVERSION_COUNT = 15;

    @Transactional(readOnly = true)
    public List<UnitOfMeasureResponse> findAll() {
        return unitRepository.findAll().stream()
                .map(mapper::toUnitResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UnitOfMeasureResponse create(UnitOfMeasureRequest request) {
        if (unitRepository.count() >= REFERENCE_UNIT_COUNT) {
            throw new BusinessException("Le catalogue d'unités de référence est pré-configuré et ne peut pas être modifié");
        }
        if (unitRepository.findBySymbole(request.getSymbole()).isPresent()) {
            throw new BusinessException("Symbole déjà utilisé: " + request.getSymbole());
        }
        UnitOfMeasure unit = UnitOfMeasure.builder()
                .nom(request.getNom())
                .symbole(request.getSymbole())
                .build();
        return mapper.toUnitResponse(unitRepository.save(unit));
    }

    @Transactional
    public UnitConversionResponse createConversion(UnitConversionRequest request) {
        if (conversionRepository.count() >= REFERENCE_CONVERSION_COUNT) {
            throw new BusinessException("Les conversions globales sont pré-configurées et ne peuvent pas être modifiées");
        }
        if (request.getFromUnitId().equals(request.getToUnitId())) {
            throw new BusinessException("Les unités source et cible doivent être différentes");
        }
        if (conversionRepository.findByFromUnitIdAndToUnitId(request.getFromUnitId(), request.getToUnitId()).isPresent()) {
            throw new BusinessException("Cette conversion globale existe déjà");
        }

        UnitOfMeasure from = unitRepository.findById(request.getFromUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unité source non trouvée"));
        UnitOfMeasure to = unitRepository.findById(request.getToUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unité cible non trouvée"));

        UnitConversion conversion = UnitConversion.builder()
                .fromUnit(from)
                .toUnit(to)
                .factor(request.getFactor())
                .build();

        return mapper.toConversionResponse(conversionRepository.save(conversion));
    }

    @Transactional(readOnly = true)
    public List<UnitConversionResponse> getGlobalConversions() {
        return conversionRepository.findAll().stream()
                .map(mapper::toConversionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal convertGlobal(Long fromUnitId, Long toUnitId, BigDecimal quantity) {
        return conversionRepository.findByFromUnitIdAndToUnitId(fromUnitId, toUnitId)
                .map(c -> quantity.multiply(c.getFactor()).setScale(6, RoundingMode.HALF_UP))
                .orElseGet(() -> conversionRepository.findByFromUnitIdAndToUnitId(toUnitId, fromUnitId)
                        .map(c -> quantity.divide(c.getFactor(), 6, RoundingMode.HALF_UP))
                        .orElseThrow(() -> new BusinessException(
                                "Conversion globale non définie entre ces unités (ex: kg → g)")));
    }

    @Transactional
    public void delete(Long id) {
        UnitOfMeasure unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unité non trouvée: " + id));
        if (productRepository.countByUnitId(id) > 0) {
            throw new BusinessException("Impossible : des produits utilisent cette unité");
        }
        conversionRepository.findByFromUnitIdOrToUnitId(id, id).forEach(conversionRepository::delete);
        unitRepository.delete(unit);
    }

    @Transactional
    public void deleteConversion(Long id) {
        UnitConversion conversion = conversionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversion non trouvée: " + id));
        conversionRepository.delete(conversion);
    }
}
