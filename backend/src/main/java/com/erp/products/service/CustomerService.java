package com.erp.products.service;

import com.erp.products.domain.entity.Customer;
import com.erp.products.domain.enums.SaleStatus;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.CustomerRepository;
import com.erp.products.repository.LoyaltyTransactionRepository;
import com.erp.products.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final LoyaltyService loyaltyService;
    private final LoyaltySettingsService loyaltySettingsService;

    @Transactional(readOnly = true)
    public List<CustomerResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return customerRepository.searchActive(query.trim()).stream()
                .limit(20)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> listAll() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return toResponse(findCustomer(id));
    }

    @Transactional(readOnly = true)
    public CustomerHistoryResponse getHistory(Long id) {
        Customer customer = findCustomer(id);
        Object[] agg = saleRepository.aggregateCustomerPurchases(id);
        long count = agg[0] != null ? ((Number) agg[0]).longValue() : 0;
        BigDecimal totalSpent = agg[1] != null ? new BigDecimal(agg[1].toString()) : BigDecimal.ZERO;
        Instant lastPurchase = agg[2] != null ? (Instant) agg[2] : null;

        BigDecimal avgBasket = count > 0
                ? totalSpent.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Object[] pointsAgg = loyaltyTransactionRepository.sumEarnedAndRedeemed(id);
        int earned = pointsAgg[0] != null ? ((Number) pointsAgg[0]).intValue() : 0;
        int redeemed = pointsAgg[1] != null ? ((Number) pointsAgg[1]).intValue() : 0;

        List<CustomerHistoryResponse.TopProduct> topProducts = saleRepository
                .findTopProductsByCustomer(id, PageRequest.of(0, 5))
                .stream()
                .map(row -> CustomerHistoryResponse.TopProduct.builder()
                        .productId((Long) row[0])
                        .productNom((String) row[1])
                        .totalQuantity(new BigDecimal(row[2].toString()))
                        .totalAmount(new BigDecimal(row[3].toString()))
                        .build())
                .toList();

        return CustomerHistoryResponse.builder()
                .customerId(customer.getId())
                .customerNumber(customer.getCustomerNumber())
                .fullName(customer.fullName())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .loyaltyTier(customer.getLoyaltyTier())
                .purchaseCount(count)
                .totalSpent(totalSpent)
                .averageBasket(avgBasket)
                .lastPurchaseAt(lastPurchase)
                .totalPointsEarned(earned)
                .totalPointsRedeemed(redeemed)
                .topProducts(topProducts)
                .recentTransactions(loyaltyService.listTransactions(id).stream().limit(10).toList())
                .build();
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        validateNames(request.getFirstName(), request.getLastName());
        Customer customer = Customer.builder()
                .customerNumber(generateCustomerNumber())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .companyName(request.getCompanyName())
                .phone(normalizePhone(request.getPhone()))
                .email(normalizeEmail(request.getEmail()))
                .birthDate(request.getBirthDate())
                .address(request.getAddress())
                .city(request.getCity())
                .notes(request.getNotes())
                .loyaltyPoints(0)
                .loyaltyTier(loyaltySettingsService.resolveTier(0,
                        loyaltySettingsService.getConfig().getLoyaltyTiersConfig()))
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse quickCreate(CustomerQuickCreateRequest request) {
        if (request.getLastName() == null || request.getLastName().isBlank()) {
            throw new BusinessException("Nom obligatoire");
        }
        String firstName = request.getFirstName() != null && !request.getFirstName().isBlank()
                ? request.getFirstName().trim() : "Client";
        CustomerRequest full = new CustomerRequest();
        full.setFirstName(firstName);
        full.setLastName(request.getLastName().trim());
        full.setPhone(request.getPhone());
        full.setIsActive(true);
        return create(full);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findCustomer(id);
        validateNames(request.getFirstName(), request.getLastName());
        customer.setFirstName(request.getFirstName().trim());
        customer.setLastName(request.getLastName().trim());
        customer.setCompanyName(request.getCompanyName());
        customer.setPhone(normalizePhone(request.getPhone()));
        customer.setEmail(normalizeEmail(request.getEmail()));
        customer.setBirthDate(request.getBirthDate());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setNotes(request.getNotes());
        if (request.getIsActive() != null) {
            customer.setIsActive(request.getIsActive());
        }
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findCustomer(id);
        customer.setIsActive(false);
        customerRepository.save(customer);
    }

    Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouve: " + id));
    }

    CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .customerNumber(customer.getCustomerNumber())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .fullName(customer.fullName())
                .companyName(customer.getCompanyName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .birthDate(customer.getBirthDate())
                .address(customer.getAddress())
                .city(customer.getCity())
                .notes(customer.getNotes())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .loyaltyTier(customer.getLoyaltyTier())
                .isActive(customer.getIsActive())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    private void validateNames(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            throw new BusinessException("Nom obligatoire");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new BusinessException("Prenom obligatoire");
        }
    }

    private String generateCustomerNumber() {
        String prefix = "CLI-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long seq = customerRepository.countByCustomerNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private String normalizePhone(String phone) {
        return phone != null && !phone.isBlank() ? phone.trim() : null;
    }

    private String normalizeEmail(String email) {
        return email != null && !email.isBlank() ? email.trim().toLowerCase() : null;
    }
}
