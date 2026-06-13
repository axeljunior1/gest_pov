package com.erp.products.service;

import com.erp.products.domain.entity.Sale;
import com.erp.products.domain.enums.SaleStatus;
import com.erp.products.dto.TicketResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.PosMapper;
import com.erp.products.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosTicketService {

    private final SaleRepository saleRepository;
    private final SettingsService settingsService;
    private final PosMapper mapper;

    @Transactional(readOnly = true)
    public TicketResponse buildTicket(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvee: " + saleId));
        if (sale.getStatus() != SaleStatus.VALIDATED
                && sale.getStatus() != SaleStatus.PARTIALLY_REFUNDED
                && sale.getStatus() != SaleStatus.REFUNDED) {
            throw new BusinessException("Ticket disponible uniquement pour une vente validee");
        }

        var publicSettings = settingsService.getPublicSettings();
        return TicketResponse.builder()
                .ticketNumber(sale.getSaleNumber())
                .saleDate(sale.getValidatedAt())
                .companyName(publicSettings.getCompanyName())
                .registerName(settingsService.getSetting(com.erp.products.settings.SettingKeys.POS_REGISTER_NAME))
                .cashierName(sale.getCashier().fullName())
                .lines(sale.getLignes().stream().map(l -> TicketResponse.TicketLine.builder()
                        .productNom(l.getProduct().getNom())
                        .quantity(l.getQuantityInput())
                        .unitPrice(l.getUnitPrice())
                        .discountAmount(l.getDiscountAmount())
                        .lineTotal(l.getLineTotal())
                        .build()).collect(Collectors.toList()))
                .subtotal(sale.getSubtotal())
                .discountTotal(sale.getDiscountTotal())
                .taxTotal(sale.getTaxTotal())
                .total(sale.getTotal())
                .payments(sale.getPayments().stream().map(mapper::toPaymentResponse).collect(Collectors.toList()))
                .changeAmount(sale.getChangeAmount())
                .currency(publicSettings.getCurrency())
                .build();
    }
}
