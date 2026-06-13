package com.erp.products.dto;

import com.erp.products.domain.enums.AppSettingType;
import com.erp.products.domain.enums.ReferenceValueCategory;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AppSettingResponse {

    private Long id;
    private String key;
    private String value;
    private AppSettingType type;
    private String description;
    private Boolean isPublic;
    /** Catégorie de liste de valeurs si le paramètre doit être choisi dans un menu déroulant. */
    private ReferenceValueCategory referenceCategory;
    private String updatedBy;
    private Instant updatedAt;
}
