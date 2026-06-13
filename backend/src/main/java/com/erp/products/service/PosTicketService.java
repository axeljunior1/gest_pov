package com.erp.products.service;

import com.erp.products.domain.entity.Customer;
import com.erp.products.domain.entity.Sale;
import com.erp.products.domain.enums.SaleStatus;
import com.erp.products.domain.enums.SaleStatuses;
import com.erp.products.dto.InvoiceResponse;
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
        if (!SaleStatuses.isPaid(sale.getStatus())
                && sale.getStatus() != SaleStatus.PARTIALLY_REFUNDED
                && sale.getStatus() != SaleStatus.REFUNDED) {
            throw new BusinessException("Ticket disponible uniquement pour une vente payee");
        }

        var publicSettings = settingsService.getPublicSettings();
        return TicketResponse.builder()
                .ticketNumber(sale.getSaleNumber())
                .saleDate(sale.getPaidAt() != null ? sale.getPaidAt() : sale.getValidatedAt())
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

    @Transactional(readOnly = true)
    public InvoiceResponse buildInvoice(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvee: " + saleId));
        if (!SaleStatuses.countsForRevenue(sale.getStatus())) {
            throw new BusinessException("Facture disponible uniquement pour une vente finalisee");
        }

        var publicSettings = settingsService.getPublicSettings();
        Customer customer = sale.getCustomer();
        String customerName = null;
        if (customer != null) {
            customerName = customer.getCompanyName() != null && !customer.getCompanyName().isBlank()
                    ? customer.getCompanyName()
                    : customer.getFirstName() + " " + customer.getLastName();
        }

        return InvoiceResponse.builder()
                .invoiceNumber(sale.getSaleNumber())
                .saleDate(sale.getPaidAt() != null ? sale.getPaidAt() : sale.getValidatedAt())
                .companyName(publicSettings.getCompanyName())
                .registerName(settingsService.getSetting(com.erp.products.settings.SettingKeys.POS_REGISTER_NAME))
                .sellerName(sale.getSeller().fullName())
                .cashierName(sale.getCashier().fullName())
                .customerId(customer != null ? customer.getId() : null)
                .customerNumber(customer != null ? customer.getCustomerNumber() : null)
                .customerName(customerName)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerAddress(customer != null ? customer.getAddress() : null)
                .customerCity(customer != null ? customer.getCity() : null)
                .lines(sale.getLignes().stream().map(l -> TicketResponse.TicketLine.builder()
                        .productNom(l.getProduct().getNom())
                        .quantity(l.getQuantityInput())
                        .unitPrice(l.getUnitPrice())
                        .discountAmount(l.getDiscountAmount())
                        .lineTotal(l.getLineTotal())
                        .build()).collect(Collectors.toList()))
                .subtotal(sale.getSubtotal())
                .discountTotal(sale.getDiscountTotal())
                .loyaltyDiscountAmount(sale.getLoyaltyDiscountAmount())
                .taxTotal(sale.getTaxTotal())
                .total(sale.getTotal())
                .payments(sale.getPayments().stream().map(mapper::toPaymentResponse).collect(Collectors.toList()))
                .changeAmount(sale.getChangeAmount())
                .currency(publicSettings.getCurrency())
                .build();
    }
}
