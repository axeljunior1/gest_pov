package com.erp.products.service;

import com.erp.products.dto.LoyaltyConfigResponse;
import com.erp.products.dto.LoyaltyTierConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoyaltySettingsService {

    private final SettingsService settingsService;
    private final ObjectMapper objectMapper;

    public LoyaltyConfigResponse getConfig() {
        return settingsService.getLoyaltyConfig();
    }

    public String snapshotRules() {
        try {
            return objectMapper.writeValueAsString(getConfig());
        } catch (Exception e) {
            return "{}";
        }
    }

    public String resolveTier(int points, List<LoyaltyTierConfig> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return "BRONZE";
        }
        return tiers.stream()
                .sorted(Comparator.comparing(LoyaltyTierConfig::getMinPoints,
                        Comparator.nullsFirst(Integer::compareTo)).reversed())
                .filter(t -> t.getMinPoints() != null && points >= t.getMinPoints())
                .map(LoyaltyTierConfig::getName)
                .findFirst()
                .orElse(tiers.get(0).getName());
    }
}
