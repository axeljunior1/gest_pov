package com.erp.products.settings;

public final class SettingKeys {

    public static final String COMPANY_NAME = "company.name";
    public static final String COMPANY_LOGO = "company.logo";
    public static final String APP_CURRENCY = "app.currency";
    public static final String APP_LANGUAGE = "app.language";
    public static final String APP_TIMEZONE = "app.timezone";
    public static final String APP_DATE_FORMAT = "app.date_format";
    public static final String STOCK_ALLOW_NEGATIVE = "stock.allow_negative";
    public static final String STOCK_LOW_THRESHOLD = "stock.low_threshold_default";
    public static final String ALERT_EXPIRY_DAYS = "alert.expiry_days_default";
    public static final String NUMBERING_ENTRY_PREFIX = "numbering.entry_prefix";
    public static final String NUMBERING_EXIT_PREFIX = "numbering.exit_prefix";
    public static final String NUMBERING_INVENTORY_PREFIX = "numbering.inventory_prefix";
    public static final String NUMBERING_MOVEMENT_PREFIX = "numbering.movement_prefix";
    public static final String NUMBERING_SALE_PREFIX = "numbering.sale_prefix";
    public static final String POS_REGISTER_NAME = "pos.register_name";
    public static final String POS_TAX_RATE_DEFAULT = "pos.tax_rate_default";
    public static final String POS_DEFAULT_WAREHOUSE_CODE = "pos.default_warehouse_code";
    public static final String POS_SALES_FLOW_MODE = "pos_sales_flow_mode";
    /** @deprecated utiliser {@link #POS_SALES_FLOW_MODE} */
    public static final String POS_CASH_HANDLING_MODE = "pos.cash_handling_mode";
    public static final String POS_ALLOW_SELLER_CASH_COLLECTION = "pos.allow_seller_cash_collection";
    public static final String POS_ALLOW_PARTIAL_PAYMENT = "pos.allow_partial_payment";
    public static final String POS_ALLOW_SPLIT_PAYMENT = "pos.allow_split_payment";
    public static final String POS_MAX_PENDING_PAYMENT_DURATION = "pos.max_pending_payment_duration";
    public static final String POS_ALERT_PENDING_PAYMENT_MINUTES = "pos.alert.pending_payment_minutes";
    public static final String POS_ALERT_CASH_DIFFERENCE_THRESHOLD = "pos.alert.cash_difference_threshold";

    public static final String LOYALTY_ENABLED = "loyalty.enabled";
    public static final String LOYALTY_POINTS_PER_CURRENCY_UNIT = "loyalty.points_per_currency_unit";
    public static final String LOYALTY_CURRENCY_UNIT_AMOUNT = "loyalty.currency_unit_amount";
    public static final String LOYALTY_POINT_VALUE = "loyalty.point_value";
    public static final String LOYALTY_MINIMUM_POINTS_TO_REDEEM = "loyalty.minimum_points_to_redeem";
    public static final String LOYALTY_MAXIMUM_DISCOUNT_PERCENT = "loyalty.maximum_discount_percent";
    public static final String LOYALTY_POINTS_EXPIRATION_ENABLED = "loyalty.points_expiration_enabled";
    public static final String LOYALTY_POINTS_EXPIRATION_DAYS = "loyalty.points_expiration_days";
    public static final String LOYALTY_EARN_ON_DISCOUNTED_SALES = "loyalty.earn_points_on_discounted_sales";
    public static final String LOYALTY_EARN_ON_TAX_INCLUDED = "loyalty.earn_points_on_tax_included_amount";
    public static final String LOYALTY_ALLOW_REDEMPTION = "loyalty.allow_points_redemption";
    public static final String LOYALTY_TIERS_CONFIG = "loyalty.tiers_config";

    private SettingKeys() {}
}
