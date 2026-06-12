package com.erp.products.service;

import com.erp.products.domain.entity.Location;
import com.erp.products.domain.entity.Lot;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.domain.entity.Warehouse;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.StockMapper;
import com.erp.products.repository.LocationRepository;
import com.erp.products.repository.LotRepository;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.ProductVariantRepository;
import com.erp.products.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final LotRepository lotRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final StockMapper mapper;

    @Transactional(readOnly = true)
    public List<WarehouseResponse> findAll() {
        return warehouseRepository.findByActifTrueOrderByNomAsc().stream()
                .map(mapper::toWarehouseResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        if (warehouseRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("Code entrepot deja utilise: " + request.getCode());
        }
        Warehouse w = Warehouse.builder()
                .code(request.getCode().trim().toUpperCase())
                .nom(request.getNom())
                .adresse(request.getAdresse())
                .actif(request.getActif() != null ? request.getActif() : true)
                .build();
        return mapper.toWarehouseResponse(warehouseRepository.save(w));
    }

    @Transactional
    public LocationResponse addLocation(Long warehouseId, LocationRequest request) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrepot non trouve"));
        if (locationRepository.findByWarehouseIdAndCode(warehouseId, request.getCode()).isPresent()) {
            throw new BusinessException("Code emplacement deja utilise");
        }
        Location location = Location.builder()
                .warehouse(warehouse)
                .code(request.getCode().trim().toUpperCase())
                .nom(request.getNom())
                .actif(request.getActif() != null ? request.getActif() : true)
                .build();
        return mapper.toLocationResponse(locationRepository.save(location));
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> listLocations(Long warehouseId) {
        return locationRepository.findByWarehouseIdAndActifTrueOrderByCodeAsc(warehouseId).stream()
                .map(mapper::toLocationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LotResponse createLot(LotRequest request) {
        var product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouve"));
        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvee"));
        }
        Lot lot = Lot.builder()
                .product(product)
                .variant(variant)
                .numeroLot(request.getNumeroLot().trim())
                .datePeremption(request.getDatePeremption())
                .dateFabrication(request.getDateFabrication())
                .build();
        return mapper.toLotResponse(lotRepository.save(lot));
    }
}
