package com.erp.products.service;

import com.erp.products.domain.enums.StockMovementType;
import com.erp.products.dto.StockMovementResponse;
import com.erp.products.mapper.StockMapper;
import com.erp.products.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository movementRepository;
    private final StockMapper mapper;

    @Transactional(readOnly = true)
    public List<StockMovementResponse> findByProduct(Long productId) {
        return movementRepository.findByProductIdOrderByMovementDateDescCreatedAtDesc(productId).stream()
                .map(mapper::toMovementResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> findByWarehouse(Long warehouseId) {
        return movementRepository.findByWarehouseIdOrderByMovementDateDescCreatedAtDesc(warehouseId).stream()
                .map(mapper::toMovementResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> findByType(StockMovementType type) {
        return movementRepository.findByMovementTypeOrderByMovementDateDescCreatedAtDesc(type).stream()
                .map(mapper::toMovementResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> findRecent() {
        return movementRepository.findTop200ByOrderByMovementDateDescCreatedAtDesc().stream()
                .map(mapper::toMovementResponse)
                .collect(Collectors.toList());
    }
}
