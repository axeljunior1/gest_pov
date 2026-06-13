--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: alert_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.alert_rules (
    id bigint NOT NULL,
    alert_type character varying(30) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    default_severity character varying(20) NOT NULL,
    description character varying(255),
    enabled boolean NOT NULL,
    CONSTRAINT alert_rules_alert_type_check CHECK (((alert_type)::text = ANY ((ARRAY['LOW_STOCK'::character varying, 'OUT_OF_STOCK'::character varying, 'OVERSTOCK'::character varying, 'EXPIRY_SOON'::character varying, 'EXPIRED'::character varying, 'DORMANT_PRODUCT'::character varying, 'SUPPLIER_DELAY'::character varying, 'INVENTORY_DISCREPANCY'::character varying])::text[]))),
    CONSTRAINT alert_rules_default_severity_check CHECK (((default_severity)::text = ANY ((ARRAY['INFO'::character varying, 'WARNING'::character varying, 'CRITICAL'::character varying])::text[])))
);


--
-- Name: alert_rules_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.alert_rules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: alert_rules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.alert_rules_id_seq OWNED BY public.alert_rules.id;


--
-- Name: alert_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.alert_settings (
    id bigint NOT NULL,
    actif boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    dormant_days integer,
    expiry_alert_days integer,
    max_stock_level numeric(19,6),
    min_stock_level numeric(19,6),
    scope character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone,
    product_id bigint,
    warehouse_id bigint,
    CONSTRAINT alert_settings_scope_check CHECK (((scope)::text = ANY ((ARRAY['GLOBAL'::character varying, 'PRODUCT'::character varying, 'WAREHOUSE'::character varying, 'PRODUCT_WAREHOUSE'::character varying])::text[])))
);


--
-- Name: alert_settings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.alert_settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: alert_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.alert_settings_id_seq OWNED BY public.alert_settings.id;


--
-- Name: alerts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.alerts (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    first_triggered_at timestamp(6) with time zone NOT NULL,
    last_triggered_at timestamp(6) with time zone NOT NULL,
    lot_key bigint NOT NULL,
    message character varying(500) NOT NULL,
    resolved_at timestamp(6) with time zone,
    resolved_by character varying(100),
    severity character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    threshold_value numeric(19,6),
    trigger_count integer NOT NULL,
    triggered_value numeric(19,6),
    type character varying(30) NOT NULL,
    location_id bigint,
    lot_id bigint,
    product_id bigint,
    warehouse_id bigint,
    CONSTRAINT alerts_severity_check CHECK (((severity)::text = ANY ((ARRAY['INFO'::character varying, 'WARNING'::character varying, 'CRITICAL'::character varying])::text[]))),
    CONSTRAINT alerts_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'ACKNOWLEDGED'::character varying, 'RESOLVED'::character varying, 'IGNORED'::character varying])::text[]))),
    CONSTRAINT alerts_type_check CHECK (((type)::text = ANY ((ARRAY['LOW_STOCK'::character varying, 'OUT_OF_STOCK'::character varying, 'OVERSTOCK'::character varying, 'EXPIRY_SOON'::character varying, 'EXPIRED'::character varying, 'DORMANT_PRODUCT'::character varying, 'SUPPLIER_DELAY'::character varying, 'INVENTORY_DISCREPANCY'::character varying])::text[])))
);


--
-- Name: alerts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.alerts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: alerts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.alerts_id_seq OWNED BY public.alerts.id;


--
-- Name: app_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_settings (
    id bigint NOT NULL,
    description character varying(500),
    is_public boolean NOT NULL,
    setting_key character varying(100) NOT NULL,
    type character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by character varying(120),
    setting_value text,
    CONSTRAINT app_settings_type_check CHECK (((type)::text = ANY ((ARRAY['STRING'::character varying, 'NUMBER'::character varying, 'BOOLEAN'::character varying, 'JSON'::character varying])::text[])))
);


--
-- Name: app_settings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.app_settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: app_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.app_settings_id_seq OWNED BY public.app_settings.id;


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id bigint NOT NULL,
    action character varying(255) NOT NULL,
    date_action timestamp(6) with time zone NOT NULL,
    details character varying(2000),
    entity_id bigint NOT NULL,
    entity_type character varying(255) NOT NULL,
    utilisateur character varying(255) NOT NULL,
    CONSTRAINT audit_logs_action_check CHECK (((action)::text = ANY ((ARRAY['CREATION'::character varying, 'MODIFICATION'::character varying, 'SUPPRESSION'::character varying, 'CHANGEMENT_PRIX'::character varying, 'CHANGEMENT_CATEGORIE'::character varying, 'CHANGEMENT_STATUT'::character varying, 'CHANGEMENT_CYCLE_VIE'::character varying, 'AJOUT_VARIANTE'::character varying, 'SUPPRESSION_VARIANTE'::character varying, 'AJOUT_FOURNISSEUR'::character varying, 'SUPPRESSION_FOURNISSEUR'::character varying, 'AJOUT_IMAGE'::character varying, 'SUPPRESSION_IMAGE'::character varying, 'AJOUT_DOCUMENT'::character varying, 'SUPPRESSION_DOCUMENT'::character varying])::text[])))
);


--
-- Name: audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.audit_logs_id_seq OWNED BY public.audit_logs.id;


--
-- Name: categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categories (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    nom character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    parent_id bigint
);


--
-- Name: categories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.categories_id_seq OWNED BY public.categories.id;


--
-- Name: custom_attribute_definitions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.custom_attribute_definitions (
    id bigint NOT NULL,
    code character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    label character varying(255) NOT NULL,
    type character varying(255) NOT NULL
);


--
-- Name: custom_attribute_definitions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.custom_attribute_definitions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: custom_attribute_definitions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.custom_attribute_definitions_id_seq OWNED BY public.custom_attribute_definitions.id;


--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    id bigint NOT NULL,
    address character varying(500),
    birth_date date,
    city character varying(100),
    company_name character varying(200),
    created_at timestamp(6) with time zone NOT NULL,
    customer_number character varying(50) NOT NULL,
    email character varying(150),
    first_name character varying(100) NOT NULL,
    is_active boolean NOT NULL,
    last_name character varying(100) NOT NULL,
    loyalty_points integer NOT NULL,
    loyalty_tier character varying(50),
    notes character varying(1000),
    phone character varying(30),
    updated_at timestamp(6) with time zone NOT NULL
);


--
-- Name: customers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customers_id_seq OWNED BY public.customers.id;


--
-- Name: import_jobs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.import_jobs (
    id bigint NOT NULL,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(120) NOT NULL,
    error_report text,
    error_rows integer NOT NULL,
    file_name character varying(255) NOT NULL,
    import_type character varying(30) NOT NULL,
    status character varying(20) NOT NULL,
    success_rows integer NOT NULL,
    total_rows integer NOT NULL,
    CONSTRAINT import_jobs_import_type_check CHECK (((import_type)::text = ANY ((ARRAY['PRODUCTS'::character varying, 'PACKAGINGS'::character varying, 'INITIAL_STOCK'::character varying])::text[]))),
    CONSTRAINT import_jobs_status_check CHECK (((status)::text = ANY ((ARRAY['COMPLETED'::character varying, 'FAILED'::character varying, 'PARTIAL'::character varying])::text[])))
);


--
-- Name: import_jobs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.import_jobs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: import_jobs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.import_jobs_id_seq OWNED BY public.import_jobs.id;


--
-- Name: inventory_count_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventory_count_lines (
    id bigint NOT NULL,
    ecart numeric(19,6) NOT NULL,
    quantity_counted numeric(19,6) NOT NULL,
    quantity_system numeric(19,6) NOT NULL,
    inventory_count_id bigint NOT NULL,
    location_id bigint NOT NULL,
    lot_id bigint,
    product_id bigint NOT NULL,
    variant_id bigint,
    notes character varying(500),
    quantity_input numeric(19,6),
    packaging_id bigint
);


--
-- Name: inventory_count_lines_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inventory_count_lines_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inventory_count_lines_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inventory_count_lines_id_seq OWNED BY public.inventory_count_lines.id;


--
-- Name: inventory_counts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inventory_counts (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    reference character varying(50) NOT NULL,
    status character varying(20) NOT NULL,
    utilisateur character varying(255) NOT NULL,
    validated_at timestamp(6) with time zone,
    warehouse_id bigint NOT NULL,
    cancelled_at timestamp(6) with time zone,
    cancelled_by character varying(100),
    completed_at timestamp(6) with time zone,
    created_by character varying(100) NOT NULL,
    inventory_number character varying(50) NOT NULL,
    notes character varying(1000),
    started_at timestamp(6) with time zone,
    validated_by character varying(100),
    version bigint,
    location_id bigint,
    CONSTRAINT inventory_counts_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'IN_PROGRESS'::character varying, 'VALIDATED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: inventory_counts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.inventory_counts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: inventory_counts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.inventory_counts_id_seq OWNED BY public.inventory_counts.id;


--
-- Name: locations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.locations (
    id bigint NOT NULL,
    actif boolean NOT NULL,
    code character varying(50) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    nom character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    warehouse_id bigint NOT NULL
);


--
-- Name: locations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.locations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: locations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.locations_id_seq OWNED BY public.locations.id;


--
-- Name: lots; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lots (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    date_fabrication date,
    date_peremption date,
    numero_lot character varying(100) NOT NULL,
    product_id bigint NOT NULL,
    variant_id bigint
);


--
-- Name: lots_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.lots_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lots_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.lots_id_seq OWNED BY public.lots.id;


--
-- Name: loyalty_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loyalty_transactions (
    id bigint NOT NULL,
    amount_value numeric(19,4),
    balance_after integer NOT NULL,
    balance_before integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(150),
    points integer NOT NULL,
    rule_snapshot text,
    type character varying(30) NOT NULL,
    customer_id bigint NOT NULL,
    sale_id bigint,
    CONSTRAINT loyalty_transactions_type_check CHECK (((type)::text = ANY ((ARRAY['EARN'::character varying, 'REDEEM'::character varying, 'MANUAL_ADD'::character varying, 'MANUAL_REMOVE'::character varying, 'EXPIRED'::character varying, 'REFUND_REVERSAL'::character varying])::text[])))
);


--
-- Name: loyalty_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.loyalty_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: loyalty_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.loyalty_transactions_id_seq OWNED BY public.loyalty_transactions.id;


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id bigint NOT NULL,
    channel character varying(20) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    error_message character varying(500),
    read_at timestamp(6) with time zone,
    sent_at timestamp(6) with time zone,
    status character varying(20) NOT NULL,
    user_id character varying(100) NOT NULL,
    alert_id bigint NOT NULL,
    CONSTRAINT notifications_channel_check CHECK (((channel)::text = ANY ((ARRAY['IN_APP'::character varying, 'EMAIL'::character varying, 'SMS'::character varying, 'PUSH'::character varying, 'SLACK'::character varying, 'WHATSAPP'::character varying])::text[]))),
    CONSTRAINT notifications_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying, 'READ'::character varying])::text[])))
);


--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id bigint NOT NULL,
    amount numeric(19,4) NOT NULL,
    method character varying(30) NOT NULL,
    paid_at timestamp(6) with time zone NOT NULL,
    status character varying(20) NOT NULL,
    sale_id bigint NOT NULL,
    cashier_id bigint,
    pos_session_id bigint,
    CONSTRAINT payments_method_check CHECK (((method)::text = ANY ((ARRAY['CASH'::character varying, 'CARD'::character varying, 'MOBILE_MONEY'::character varying, 'BANK_TRANSFER'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT payments_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PAID'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying])::text[])))
);


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payments_id_seq OWNED BY public.payments.id;


--
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
    id bigint NOT NULL,
    code character varying(80) NOT NULL,
    description character varying(255),
    module character varying(50) NOT NULL,
    name character varying(150) NOT NULL
);


--
-- Name: permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.permissions_id_seq OWNED BY public.permissions.id;


--
-- Name: pos_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pos_sessions (
    id bigint NOT NULL,
    closed_at timestamp(6) with time zone,
    closing_cash_amount numeric(19,4),
    difference_amount numeric(19,4),
    expected_cash_amount numeric(19,4),
    opened_at timestamp(6) with time zone NOT NULL,
    opening_cash_amount numeric(19,4),
    session_number character varying(50) NOT NULL,
    status character varying(20) NOT NULL,
    version bigint,
    cashier_id bigint NOT NULL,
    warehouse_id bigint NOT NULL,
    session_type character varying(20) DEFAULT 'CASHIER'::character varying NOT NULL,
    CONSTRAINT pos_sessions_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'CLOSED'::character varying])::text[])))
);


--
-- Name: pos_sessions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pos_sessions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pos_sessions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pos_sessions_id_seq OWNED BY public.pos_sessions.id;


--
-- Name: price_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.price_history (
    id bigint NOT NULL,
    ancien_prix numeric(19,4),
    date_modification timestamp(6) with time zone NOT NULL,
    nouveau_prix numeric(19,4) NOT NULL,
    type character varying(255) NOT NULL,
    utilisateur character varying(255) NOT NULL,
    product_id bigint,
    variant_id bigint,
    CONSTRAINT price_history_type_check CHECK (((type)::text = ANY ((ARRAY['ACHAT'::character varying, 'VENTE'::character varying, 'PROMOTIONNEL'::character varying])::text[])))
);


--
-- Name: price_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.price_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: price_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.price_history_id_seq OWNED BY public.price_history.id;


--
-- Name: product_custom_attribute_values; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_custom_attribute_values (
    id bigint NOT NULL,
    valeur character varying(2000),
    attribute_id bigint NOT NULL,
    product_id bigint NOT NULL
);


--
-- Name: product_custom_attribute_values_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_custom_attribute_values_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_custom_attribute_values_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_custom_attribute_values_id_seq OWNED BY public.product_custom_attribute_values.id;


--
-- Name: product_documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_documents (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    file_name character varying(255) NOT NULL,
    file_path character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    product_id bigint NOT NULL,
    CONSTRAINT product_documents_type_check CHECK (((type)::text = ANY ((ARRAY['FICHE_TECHNIQUE'::character varying, 'CERTIFICAT_QUALITE'::character varying, 'NOTICE_UTILISATEUR'::character varying, 'GARANTIE'::character varying, 'AUTRE'::character varying])::text[])))
);


--
-- Name: product_documents_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_documents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_documents_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_documents_id_seq OWNED BY public.product_documents.id;


--
-- Name: product_images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_images (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    file_name character varying(255) NOT NULL,
    file_path character varying(255) NOT NULL,
    ordre integer,
    principale boolean NOT NULL,
    product_id bigint NOT NULL
);


--
-- Name: product_images_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_images_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_images_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_images_id_seq OWNED BY public.product_images.id;


--
-- Name: product_packagings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_packagings (
    id bigint NOT NULL,
    code_barre character varying(255),
    created_at timestamp(6) with time zone NOT NULL,
    nom character varying(255) NOT NULL,
    principal boolean NOT NULL,
    quantite_base numeric(19,6) NOT NULL,
    symbole character varying(255),
    updated_at timestamp(6) with time zone NOT NULL,
    product_id bigint NOT NULL,
    prix_vente numeric(19,4) NOT NULL,
    prix_achat numeric(19,4),
    default_vente boolean DEFAULT false NOT NULL,
    default_achat boolean DEFAULT false NOT NULL,
    actif boolean DEFAULT true NOT NULL
);


--
-- Name: product_packagings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_packagings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_packagings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_packagings_id_seq OWNED BY public.product_packagings.id;


--
-- Name: product_suppliers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_suppliers (
    id bigint NOT NULL,
    delai_livraison_jours integer,
    principal boolean NOT NULL,
    prix_negocie numeric(19,4),
    reference_fournisseur character varying(255),
    product_id bigint NOT NULL,
    supplier_id bigint NOT NULL
);


--
-- Name: product_suppliers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_suppliers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_suppliers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_suppliers_id_seq OWNED BY public.product_suppliers.id;


--
-- Name: product_variants; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_variants (
    id bigint NOT NULL,
    barcode_type character varying(255),
    code_barre character varying(255),
    couleur character varying(255),
    created_at timestamp(6) with time zone NOT NULL,
    prix numeric(19,4),
    sku character varying(255) NOT NULL,
    stock integer NOT NULL,
    taille character varying(255),
    updated_at timestamp(6) with time zone NOT NULL,
    product_id bigint NOT NULL,
    CONSTRAINT product_variants_barcode_type_check CHECK (((barcode_type)::text = ANY ((ARRAY['EAN13'::character varying, 'UPC'::character varying, 'CODE128'::character varying, 'QR_CODE'::character varying])::text[])))
);


--
-- Name: product_variants_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_variants_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_variants_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_variants_id_seq OWNED BY public.product_variants.id;


--
-- Name: products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.products (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    cycle_vie character varying(255) NOT NULL,
    description character varying(2000),
    marque character varying(255),
    nom character varying(255) NOT NULL,
    prix_achat numeric(19,4),
    prix_promotionnel numeric(19,4),
    prix_promotionnel_debut timestamp(6) with time zone,
    prix_promotionnel_fin timestamp(6) with time zone,
    prix_vente numeric(19,4),
    sku character varying(255) NOT NULL,
    statut character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    categorie_id bigint,
    fournisseur_principal_id bigint,
    unit_id bigint,
    CONSTRAINT products_cycle_vie_check CHECK (((cycle_vie)::text = ANY ((ARRAY['BROUILLON'::character varying, 'EN_ATTENTE_VALIDATION'::character varying, 'ACTIF'::character varying, 'SUSPENDU'::character varying, 'ARRETE'::character varying, 'ARCHIVE'::character varying])::text[]))),
    CONSTRAINT products_statut_check CHECK (((statut)::text = ANY ((ARRAY['ACTIF'::character varying, 'INACTIF'::character varying, 'ARCHIVE'::character varying])::text[])))
);


--
-- Name: products_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.products_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: products_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.products_id_seq OWNED BY public.products.id;


--
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permissions (
    role_id bigint NOT NULL,
    permission_id bigint NOT NULL
);


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    description character varying(255),
    is_system boolean NOT NULL,
    name character varying(100) NOT NULL
);


--
-- Name: roles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.roles_id_seq OWNED BY public.roles.id;


--
-- Name: sale_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sale_lines (
    id bigint NOT NULL,
    discount_amount numeric(19,4),
    line_total numeric(19,4) NOT NULL,
    quantity_in_base_unit numeric(19,6) NOT NULL,
    quantity_input numeric(19,6) NOT NULL,
    tax_rate numeric(8,4),
    unit_price numeric(19,4) NOT NULL,
    packaging_id bigint,
    product_id bigint NOT NULL,
    sale_id bigint NOT NULL,
    variant_id bigint,
    packaging_name_snapshot character varying(255),
    packaging_quantity_snapshot numeric(19,6),
    unit_price_snapshot numeric(19,4)
);


--
-- Name: sale_lines_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sale_lines_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sale_lines_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sale_lines_id_seq OWNED BY public.sale_lines.id;


--
-- Name: sale_refund_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sale_refund_lines (
    id bigint NOT NULL,
    quantity numeric(19,6) NOT NULL,
    refund_amount numeric(19,4) NOT NULL,
    refund_id bigint NOT NULL,
    sale_line_id bigint NOT NULL
);


--
-- Name: sale_refund_lines_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sale_refund_lines_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sale_refund_lines_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sale_refund_lines_id_seq OWNED BY public.sale_refund_lines.id;


--
-- Name: sale_refunds; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sale_refunds (
    id bigint NOT NULL,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(120),
    reason character varying(500),
    refund_number character varying(50) NOT NULL,
    return_to_stock boolean NOT NULL,
    status character varying(20) NOT NULL,
    total_amount numeric(19,4),
    sale_id bigint NOT NULL,
    CONSTRAINT sale_refunds_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'COMPLETED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: sale_refunds_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sale_refunds_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sale_refunds_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sale_refunds_id_seq OWNED BY public.sale_refunds.id;


--
-- Name: sales; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sales (
    id bigint NOT NULL,
    cancelled_at timestamp(6) with time zone,
    change_amount numeric(19,4),
    created_at timestamp(6) with time zone NOT NULL,
    discount_total numeric(19,4),
    hold_label character varying(120),
    paid_amount numeric(19,4),
    sale_number character varying(50) NOT NULL,
    status character varying(30) NOT NULL,
    subtotal numeric(19,4),
    tax_total numeric(19,4),
    total numeric(19,4),
    validated_at timestamp(6) with time zone,
    version bigint,
    cashier_id bigint NOT NULL,
    location_id bigint NOT NULL,
    pos_session_id bigint NOT NULL,
    warehouse_id bigint NOT NULL,
    loyalty_discount_amount numeric(19,4),
    loyalty_points_earned integer,
    loyalty_points_redeemed integer,
    customer_id bigint,
    submitted_at timestamp(6) with time zone,
    payment_session_id bigint,
    seller_id bigint NOT NULL,
    paid_at timestamp with time zone,
    CONSTRAINT sales_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'HOLD'::character varying, 'PENDING_PAYMENT'::character varying, 'PAID'::character varying, 'VALIDATED'::character varying, 'CANCELLED'::character varying, 'REFUNDED'::character varying, 'PARTIALLY_REFUNDED'::character varying])::text[])))
);


--
-- Name: sales_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sales_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sales_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sales_id_seq OWNED BY public.sales.id;


--
-- Name: stock_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_entries (
    id bigint NOT NULL,
    cancelled_at timestamp(6) with time zone,
    cancelled_by character varying(100),
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(100) NOT NULL,
    entry_date date NOT NULL,
    entry_number character varying(50) NOT NULL,
    notes character varying(500),
    reference_document character varying(100),
    status character varying(20) NOT NULL,
    validated_at timestamp(6) with time zone,
    validated_by character varying(100),
    version bigint,
    location_id bigint NOT NULL,
    supplier_id bigint,
    warehouse_id bigint NOT NULL,
    CONSTRAINT stock_entries_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'VALIDATED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: stock_entries_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stock_entries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stock_entries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stock_entries_id_seq OWNED BY public.stock_entries.id;


--
-- Name: stock_entry_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_entry_lines (
    id bigint NOT NULL,
    expiry_date date,
    lot_number character varying(100),
    notes character varying(500),
    quantity_in_base_unit numeric(19,6) NOT NULL,
    quantity_input numeric(19,6) NOT NULL,
    unit_cost numeric(19,6),
    packaging_id bigint,
    product_id bigint NOT NULL,
    stock_entry_id bigint NOT NULL,
    variant_id bigint
);


--
-- Name: stock_entry_lines_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stock_entry_lines_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stock_entry_lines_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stock_entry_lines_id_seq OWNED BY public.stock_entry_lines.id;


--
-- Name: stock_exit_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_exit_lines (
    id bigint NOT NULL,
    notes character varying(500),
    quantity_in_base_unit numeric(19,6) NOT NULL,
    quantity_input numeric(19,6) NOT NULL,
    packaging_id bigint,
    product_id bigint NOT NULL,
    stock_exit_id bigint NOT NULL,
    variant_id bigint
);


--
-- Name: stock_exit_lines_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stock_exit_lines_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stock_exit_lines_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stock_exit_lines_id_seq OWNED BY public.stock_exit_lines.id;


--
-- Name: stock_exits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_exits (
    id bigint NOT NULL,
    cancelled_at timestamp(6) with time zone,
    cancelled_by character varying(100),
    created_at timestamp(6) with time zone NOT NULL,
    created_by character varying(100) NOT NULL,
    exit_date date NOT NULL,
    exit_number character varying(50) NOT NULL,
    notes character varying(500),
    reason character varying(30) NOT NULL,
    status character varying(20) NOT NULL,
    validated_at timestamp(6) with time zone,
    validated_by character varying(100),
    version bigint,
    location_id bigint NOT NULL,
    warehouse_id bigint NOT NULL,
    CONSTRAINT stock_exits_reason_check CHECK (((reason)::text = ANY ((ARRAY['SALE'::character varying, 'INTERNAL_USE'::character varying, 'DAMAGED'::character varying, 'LOST'::character varying, 'DONATION'::character varying, 'RETURN_SUPPLIER'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT stock_exits_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'VALIDATED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: stock_exits_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stock_exits_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stock_exits_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stock_exits_id_seq OWNED BY public.stock_exits.id;


--
-- Name: stock_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_items (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    lot_key bigint NOT NULL,
    quantity_on_hand numeric(19,6) NOT NULL,
    quantity_reserved numeric(19,6) NOT NULL,
    updated_at timestamp(6) with time zone,
    version bigint,
    location_id bigint NOT NULL,
    lot_id bigint,
    product_id bigint NOT NULL,
    unit_id bigint NOT NULL,
    variant_id bigint,
    warehouse_id bigint NOT NULL
);


--
-- Name: stock_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stock_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stock_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stock_items_id_seq OWNED BY public.stock_items.id;


--
-- Name: stock_movements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_movements (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    inventory_count_id bigint,
    movement_date timestamp(6) with time zone NOT NULL,
    movement_type character varying(30) NOT NULL,
    quantity numeric(19,6) NOT NULL,
    quantity_on_hand_after numeric(19,6),
    quantity_on_hand_before numeric(19,6),
    quantity_reserved_after numeric(19,6),
    quantity_reserved_before numeric(19,6),
    reason character varying(500),
    reference character varying(100),
    reference_type character varying(50),
    stock_reservation_id bigint,
    stock_transfer_id bigint,
    utilisateur character varying(255) NOT NULL,
    location_id bigint NOT NULL,
    lot_id bigint,
    product_id bigint NOT NULL,
    unit_id bigint NOT NULL,
    variant_id bigint,
    warehouse_id bigint NOT NULL,
    stock_entry_id bigint,
    stock_exit_id bigint,
    notes character varying(1000),
    reference_id bigint,
    packaging_id bigint,
    CONSTRAINT stock_movements_movement_type_check CHECK (((movement_type)::text = ANY ((ARRAY['IN'::character varying, 'OUT'::character varying, 'ADJUSTMENT'::character varying, 'TRANSFER_OUT'::character varying, 'TRANSFER_IN'::character varying, 'RESERVATION'::character varying, 'RELEASE'::character varying, 'RETURN_IN'::character varying, 'RETURN_OUT'::character varying, 'INVENTORY'::character varying])::text[])))
);


--
-- Name: stock_movements_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stock_movements_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stock_movements_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stock_movements_id_seq OWNED BY public.stock_movements.id;


--
-- Name: stock_reservations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_reservations (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone,
    quantity numeric(19,6) NOT NULL,
    reference character varying(100),
    status character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone,
    utilisateur character varying(255) NOT NULL,
    location_id bigint NOT NULL,
    lot_id bigint,
    product_id bigint NOT NULL,
    variant_id bigint,
    warehouse_id bigint NOT NULL,
    CONSTRAINT stock_reservations_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RELEASED'::character varying, 'CONSUMED'::character varying])::text[])))
);


--
-- Name: stock_reservations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stock_reservations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stock_reservations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stock_reservations_id_seq OWNED BY public.stock_reservations.id;


--
-- Name: stock_transfer_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_transfer_lines (
    id bigint NOT NULL,
    quantity numeric(19,6) NOT NULL,
    dest_location_id bigint NOT NULL,
    lot_id bigint,
    product_id bigint NOT NULL,
    source_location_id bigint NOT NULL,
    stock_transfer_id bigint NOT NULL,
    variant_id bigint
);


--
-- Name: stock_transfer_lines_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stock_transfer_lines_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stock_transfer_lines_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stock_transfer_lines_id_seq OWNED BY public.stock_transfer_lines.id;


--
-- Name: stock_transfers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_transfers (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    notes character varying(500),
    received_at timestamp(6) with time zone,
    reference character varying(50) NOT NULL,
    shipped_at timestamp(6) with time zone,
    status character varying(20) NOT NULL,
    utilisateur character varying(255) NOT NULL,
    dest_warehouse_id bigint NOT NULL,
    source_warehouse_id bigint NOT NULL,
    CONSTRAINT stock_transfers_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'IN_TRANSIT'::character varying, 'RECEIVED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: stock_transfers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stock_transfers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stock_transfers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stock_transfers_id_seq OWNED BY public.stock_transfers.id;


--
-- Name: supplier_purchase_orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.supplier_purchase_orders (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expected_delivery_date date NOT NULL,
    reference character varying(50) NOT NULL,
    status character varying(20) NOT NULL,
    product_id bigint,
    supplier_id bigint NOT NULL,
    CONSTRAINT supplier_purchase_orders_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: supplier_purchase_orders_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.supplier_purchase_orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: supplier_purchase_orders_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.supplier_purchase_orders_id_seq OWNED BY public.supplier_purchase_orders.id;


--
-- Name: suppliers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.suppliers (
    id bigint NOT NULL,
    adresse character varying(500),
    created_at timestamp(6) with time zone NOT NULL,
    email character varying(255),
    nom character varying(255) NOT NULL,
    telephone character varying(255),
    updated_at timestamp(6) with time zone NOT NULL
);


--
-- Name: suppliers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.suppliers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: suppliers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.suppliers_id_seq OWNED BY public.suppliers.id;


--
-- Name: unit_conversions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.unit_conversions (
    id bigint NOT NULL,
    factor numeric(19,6) NOT NULL,
    from_unit_id bigint NOT NULL,
    to_unit_id bigint NOT NULL
);


--
-- Name: unit_conversions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.unit_conversions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: unit_conversions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.unit_conversions_id_seq OWNED BY public.unit_conversions.id;


--
-- Name: units_of_measure; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.units_of_measure (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    nom character varying(255) NOT NULL,
    symbole character varying(255) NOT NULL
);


--
-- Name: units_of_measure_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.units_of_measure_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: units_of_measure_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.units_of_measure_id_seq OWNED BY public.units_of_measure.id;


--
-- Name: user_notification_preferences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_notification_preferences (
    id bigint NOT NULL,
    alert_type character varying(30) NOT NULL,
    channel character varying(20) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    enabled boolean NOT NULL,
    user_id character varying(100) NOT NULL,
    CONSTRAINT user_notification_preferences_alert_type_check CHECK (((alert_type)::text = ANY ((ARRAY['LOW_STOCK'::character varying, 'OUT_OF_STOCK'::character varying, 'OVERSTOCK'::character varying, 'EXPIRY_SOON'::character varying, 'EXPIRED'::character varying, 'DORMANT_PRODUCT'::character varying, 'SUPPLIER_DELAY'::character varying, 'INVENTORY_DISCREPANCY'::character varying])::text[]))),
    CONSTRAINT user_notification_preferences_channel_check CHECK (((channel)::text = ANY ((ARRAY['IN_APP'::character varying, 'EMAIL'::character varying, 'SMS'::character varying, 'PUSH'::character varying, 'SLACK'::character varying, 'WHATSAPP'::character varying])::text[])))
);


--
-- Name: user_notification_preferences_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_notification_preferences_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_notification_preferences_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_notification_preferences_id_seq OWNED BY public.user_notification_preferences.id;


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    user_id bigint NOT NULL,
    role_id bigint NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    email character varying(150) NOT NULL,
    first_name character varying(100) NOT NULL,
    is_active boolean NOT NULL,
    last_login_at timestamp(6) with time zone,
    last_name character varying(100) NOT NULL,
    password_hash character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: warehouses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.warehouses (
    id bigint NOT NULL,
    actif boolean NOT NULL,
    adresse character varying(255),
    code character varying(50) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    nom character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: warehouses_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.warehouses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: warehouses_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.warehouses_id_seq OWNED BY public.warehouses.id;


--
-- Name: alert_rules id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alert_rules ALTER COLUMN id SET DEFAULT nextval('public.alert_rules_id_seq'::regclass);


--
-- Name: alert_settings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alert_settings ALTER COLUMN id SET DEFAULT nextval('public.alert_settings_id_seq'::regclass);


--
-- Name: alerts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alerts ALTER COLUMN id SET DEFAULT nextval('public.alerts_id_seq'::regclass);


--
-- Name: app_settings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_settings ALTER COLUMN id SET DEFAULT nextval('public.app_settings_id_seq'::regclass);


--
-- Name: audit_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs ALTER COLUMN id SET DEFAULT nextval('public.audit_logs_id_seq'::regclass);


--
-- Name: categories id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories ALTER COLUMN id SET DEFAULT nextval('public.categories_id_seq'::regclass);


--
-- Name: custom_attribute_definitions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_attribute_definitions ALTER COLUMN id SET DEFAULT nextval('public.custom_attribute_definitions_id_seq'::regclass);


--
-- Name: customers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers ALTER COLUMN id SET DEFAULT nextval('public.customers_id_seq'::regclass);


--
-- Name: import_jobs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_jobs ALTER COLUMN id SET DEFAULT nextval('public.import_jobs_id_seq'::regclass);


--
-- Name: inventory_count_lines id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_count_lines ALTER COLUMN id SET DEFAULT nextval('public.inventory_count_lines_id_seq'::regclass);


--
-- Name: inventory_counts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_counts ALTER COLUMN id SET DEFAULT nextval('public.inventory_counts_id_seq'::regclass);


--
-- Name: locations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locations ALTER COLUMN id SET DEFAULT nextval('public.locations_id_seq'::regclass);


--
-- Name: lots id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lots ALTER COLUMN id SET DEFAULT nextval('public.lots_id_seq'::regclass);


--
-- Name: loyalty_transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_transactions ALTER COLUMN id SET DEFAULT nextval('public.loyalty_transactions_id_seq'::regclass);


--
-- Name: notifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications ALTER COLUMN id SET DEFAULT nextval('public.notifications_id_seq'::regclass);


--
-- Name: payments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments ALTER COLUMN id SET DEFAULT nextval('public.payments_id_seq'::regclass);


--
-- Name: permissions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions ALTER COLUMN id SET DEFAULT nextval('public.permissions_id_seq'::regclass);


--
-- Name: pos_sessions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pos_sessions ALTER COLUMN id SET DEFAULT nextval('public.pos_sessions_id_seq'::regclass);


--
-- Name: price_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_history ALTER COLUMN id SET DEFAULT nextval('public.price_history_id_seq'::regclass);


--
-- Name: product_custom_attribute_values id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_custom_attribute_values ALTER COLUMN id SET DEFAULT nextval('public.product_custom_attribute_values_id_seq'::regclass);


--
-- Name: product_documents id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_documents ALTER COLUMN id SET DEFAULT nextval('public.product_documents_id_seq'::regclass);


--
-- Name: product_images id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_images ALTER COLUMN id SET DEFAULT nextval('public.product_images_id_seq'::regclass);


--
-- Name: product_packagings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_packagings ALTER COLUMN id SET DEFAULT nextval('public.product_packagings_id_seq'::regclass);


--
-- Name: product_suppliers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_suppliers ALTER COLUMN id SET DEFAULT nextval('public.product_suppliers_id_seq'::regclass);


--
-- Name: product_variants id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_variants ALTER COLUMN id SET DEFAULT nextval('public.product_variants_id_seq'::regclass);


--
-- Name: products id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products ALTER COLUMN id SET DEFAULT nextval('public.products_id_seq'::regclass);


--
-- Name: roles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles ALTER COLUMN id SET DEFAULT nextval('public.roles_id_seq'::regclass);


--
-- Name: sale_lines id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_lines ALTER COLUMN id SET DEFAULT nextval('public.sale_lines_id_seq'::regclass);


--
-- Name: sale_refund_lines id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_refund_lines ALTER COLUMN id SET DEFAULT nextval('public.sale_refund_lines_id_seq'::regclass);


--
-- Name: sale_refunds id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_refunds ALTER COLUMN id SET DEFAULT nextval('public.sale_refunds_id_seq'::regclass);


--
-- Name: sales id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales ALTER COLUMN id SET DEFAULT nextval('public.sales_id_seq'::regclass);


--
-- Name: stock_entries id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entries ALTER COLUMN id SET DEFAULT nextval('public.stock_entries_id_seq'::regclass);


--
-- Name: stock_entry_lines id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entry_lines ALTER COLUMN id SET DEFAULT nextval('public.stock_entry_lines_id_seq'::regclass);


--
-- Name: stock_exit_lines id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_exit_lines ALTER COLUMN id SET DEFAULT nextval('public.stock_exit_lines_id_seq'::regclass);


--
-- Name: stock_exits id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_exits ALTER COLUMN id SET DEFAULT nextval('public.stock_exits_id_seq'::regclass);


--
-- Name: stock_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_items ALTER COLUMN id SET DEFAULT nextval('public.stock_items_id_seq'::regclass);


--
-- Name: stock_movements id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements ALTER COLUMN id SET DEFAULT nextval('public.stock_movements_id_seq'::regclass);


--
-- Name: stock_reservations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_reservations ALTER COLUMN id SET DEFAULT nextval('public.stock_reservations_id_seq'::regclass);


--
-- Name: stock_transfer_lines id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfer_lines ALTER COLUMN id SET DEFAULT nextval('public.stock_transfer_lines_id_seq'::regclass);


--
-- Name: stock_transfers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfers ALTER COLUMN id SET DEFAULT nextval('public.stock_transfers_id_seq'::regclass);


--
-- Name: supplier_purchase_orders id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_purchase_orders ALTER COLUMN id SET DEFAULT nextval('public.supplier_purchase_orders_id_seq'::regclass);


--
-- Name: suppliers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suppliers ALTER COLUMN id SET DEFAULT nextval('public.suppliers_id_seq'::regclass);


--
-- Name: unit_conversions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.unit_conversions ALTER COLUMN id SET DEFAULT nextval('public.unit_conversions_id_seq'::regclass);


--
-- Name: units_of_measure id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.units_of_measure ALTER COLUMN id SET DEFAULT nextval('public.units_of_measure_id_seq'::regclass);


--
-- Name: user_notification_preferences id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notification_preferences ALTER COLUMN id SET DEFAULT nextval('public.user_notification_preferences_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: warehouses id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouses ALTER COLUMN id SET DEFAULT nextval('public.warehouses_id_seq'::regclass);


--
-- Name: alert_rules alert_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alert_rules
    ADD CONSTRAINT alert_rules_pkey PRIMARY KEY (id);


--
-- Name: alert_settings alert_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alert_settings
    ADD CONSTRAINT alert_settings_pkey PRIMARY KEY (id);


--
-- Name: alerts alerts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alerts
    ADD CONSTRAINT alerts_pkey PRIMARY KEY (id);


--
-- Name: app_settings app_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_settings
    ADD CONSTRAINT app_settings_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- Name: custom_attribute_definitions custom_attribute_definitions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_attribute_definitions
    ADD CONSTRAINT custom_attribute_definitions_pkey PRIMARY KEY (id);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: customers idx_customers_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT idx_customers_number UNIQUE (customer_number);


--
-- Name: permissions idx_permissions_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT idx_permissions_code UNIQUE (code);


--
-- Name: roles idx_roles_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT idx_roles_code UNIQUE (code);


--
-- Name: sales idx_sales_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT idx_sales_number UNIQUE (sale_number);


--
-- Name: users idx_users_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT idx_users_email UNIQUE (email);


--
-- Name: import_jobs import_jobs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_jobs
    ADD CONSTRAINT import_jobs_pkey PRIMARY KEY (id);


--
-- Name: inventory_count_lines inventory_count_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_count_lines
    ADD CONSTRAINT inventory_count_lines_pkey PRIMARY KEY (id);


--
-- Name: inventory_counts inventory_counts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_counts
    ADD CONSTRAINT inventory_counts_pkey PRIMARY KEY (id);


--
-- Name: locations locations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT locations_pkey PRIMARY KEY (id);


--
-- Name: lots lots_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lots
    ADD CONSTRAINT lots_pkey PRIMARY KEY (id);


--
-- Name: loyalty_transactions loyalty_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_transactions
    ADD CONSTRAINT loyalty_transactions_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- Name: pos_sessions pos_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pos_sessions
    ADD CONSTRAINT pos_sessions_pkey PRIMARY KEY (id);


--
-- Name: price_history price_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_history
    ADD CONSTRAINT price_history_pkey PRIMARY KEY (id);


--
-- Name: product_custom_attribute_values product_custom_attribute_values_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_custom_attribute_values
    ADD CONSTRAINT product_custom_attribute_values_pkey PRIMARY KEY (id);


--
-- Name: product_documents product_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_documents
    ADD CONSTRAINT product_documents_pkey PRIMARY KEY (id);


--
-- Name: product_images product_images_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_images
    ADD CONSTRAINT product_images_pkey PRIMARY KEY (id);


--
-- Name: product_packagings product_packagings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_packagings
    ADD CONSTRAINT product_packagings_pkey PRIMARY KEY (id);


--
-- Name: product_suppliers product_suppliers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_suppliers
    ADD CONSTRAINT product_suppliers_pkey PRIMARY KEY (id);


--
-- Name: product_variants product_variants_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_variants
    ADD CONSTRAINT product_variants_pkey PRIMARY KEY (id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: sale_lines sale_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_lines
    ADD CONSTRAINT sale_lines_pkey PRIMARY KEY (id);


--
-- Name: sale_refund_lines sale_refund_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_refund_lines
    ADD CONSTRAINT sale_refund_lines_pkey PRIMARY KEY (id);


--
-- Name: sale_refunds sale_refunds_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_refunds
    ADD CONSTRAINT sale_refunds_pkey PRIMARY KEY (id);


--
-- Name: sales sales_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT sales_pkey PRIMARY KEY (id);


--
-- Name: stock_entries stock_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entries
    ADD CONSTRAINT stock_entries_pkey PRIMARY KEY (id);


--
-- Name: stock_entry_lines stock_entry_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entry_lines
    ADD CONSTRAINT stock_entry_lines_pkey PRIMARY KEY (id);


--
-- Name: stock_exit_lines stock_exit_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_exit_lines
    ADD CONSTRAINT stock_exit_lines_pkey PRIMARY KEY (id);


--
-- Name: stock_exits stock_exits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_exits
    ADD CONSTRAINT stock_exits_pkey PRIMARY KEY (id);


--
-- Name: stock_items stock_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_items
    ADD CONSTRAINT stock_items_pkey PRIMARY KEY (id);


--
-- Name: stock_movements stock_movements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT stock_movements_pkey PRIMARY KEY (id);


--
-- Name: stock_reservations stock_reservations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_reservations
    ADD CONSTRAINT stock_reservations_pkey PRIMARY KEY (id);


--
-- Name: stock_transfer_lines stock_transfer_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfer_lines
    ADD CONSTRAINT stock_transfer_lines_pkey PRIMARY KEY (id);


--
-- Name: stock_transfers stock_transfers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfers
    ADD CONSTRAINT stock_transfers_pkey PRIMARY KEY (id);


--
-- Name: supplier_purchase_orders supplier_purchase_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_purchase_orders
    ADD CONSTRAINT supplier_purchase_orders_pkey PRIMARY KEY (id);


--
-- Name: suppliers suppliers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suppliers
    ADD CONSTRAINT suppliers_pkey PRIMARY KEY (id);


--
-- Name: lots uk2vcwgjie5eq7ucwubkfvntbv1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lots
    ADD CONSTRAINT uk2vcwgjie5eq7ucwubkfvntbv1 UNIQUE (product_id, variant_id, numero_lot);


--
-- Name: app_settings uk7p82g7l6uve2vd8l30djhxpel; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_settings
    ADD CONSTRAINT uk7p82g7l6uve2vd8l30djhxpel UNIQUE (setting_key);


--
-- Name: sale_refunds uk_3qvk3ugo3mu8c7i8q6wmd86ai; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_refunds
    ADD CONSTRAINT uk_3qvk3ugo3mu8c7i8q6wmd86ai UNIQUE (refund_number);


--
-- Name: inventory_counts uk_4kxtu30m25vbxsa14w291fm72; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_counts
    ADD CONSTRAINT uk_4kxtu30m25vbxsa14w291fm72 UNIQUE (inventory_number);


--
-- Name: stock_exits uk_5jto827wavaopybx6ksigsrnh; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_exits
    ADD CONSTRAINT uk_5jto827wavaopybx6ksigsrnh UNIQUE (exit_number);


--
-- Name: warehouses uk_6herdbg4x5wp6gkor8epv73oc; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouses
    ADD CONSTRAINT uk_6herdbg4x5wp6gkor8epv73oc UNIQUE (code);


--
-- Name: inventory_counts uk_8it9e5rwrmt9sl8hokiwnrha1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_counts
    ADD CONSTRAINT uk_8it9e5rwrmt9sl8hokiwnrha1 UNIQUE (reference);


--
-- Name: units_of_measure uk_eumk25x1y82vn6anoka4hapxv; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.units_of_measure
    ADD CONSTRAINT uk_eumk25x1y82vn6anoka4hapxv UNIQUE (nom);


--
-- Name: products uk_fhmd06dsmj6k0n90swsh8ie9g; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT uk_fhmd06dsmj6k0n90swsh8ie9g UNIQUE (sku);


--
-- Name: pos_sessions uk_hkpc8rnt5dwctxsotcf25krax; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pos_sessions
    ADD CONSTRAINT uk_hkpc8rnt5dwctxsotcf25krax UNIQUE (session_number);


--
-- Name: stock_entries uk_krw0tkfyxrsosms1sqonkhner; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entries
    ADD CONSTRAINT uk_krw0tkfyxrsosms1sqonkhner UNIQUE (entry_number);


--
-- Name: custom_attribute_definitions uk_meipwsictm7fnkjw60jq23noy; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_attribute_definitions
    ADD CONSTRAINT uk_meipwsictm7fnkjw60jq23noy UNIQUE (code);


--
-- Name: supplier_purchase_orders uk_pqklkumhrtatk12aikf36auqk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_purchase_orders
    ADD CONSTRAINT uk_pqklkumhrtatk12aikf36auqk UNIQUE (reference);


--
-- Name: units_of_measure uk_q8tjevn7ymlm3kxbdv3aj2wr7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.units_of_measure
    ADD CONSTRAINT uk_q8tjevn7ymlm3kxbdv3aj2wr7 UNIQUE (symbole);


--
-- Name: product_variants uk_q935p2d1pbjm39n0063ghnfgn; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_variants
    ADD CONSTRAINT uk_q935p2d1pbjm39n0063ghnfgn UNIQUE (sku);


--
-- Name: stock_items uk_stock_item_position; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_items
    ADD CONSTRAINT uk_stock_item_position UNIQUE (product_id, variant_id, warehouse_id, location_id, lot_key);


--
-- Name: stock_transfers uk_tcv43my1dggx5g21pa56gakan; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfers
    ADD CONSTRAINT uk_tcv43my1dggx5g21pa56gakan UNIQUE (reference);


--
-- Name: alert_rules ukb8pp0qnobd5tm5oj0i6cq3lw4; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alert_rules
    ADD CONSTRAINT ukb8pp0qnobd5tm5oj0i6cq3lw4 UNIQUE (alert_type);


--
-- Name: unit_conversions ukdydnm4qghslovbttotlqbqg87; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.unit_conversions
    ADD CONSTRAINT ukdydnm4qghslovbttotlqbqg87 UNIQUE (from_unit_id, to_unit_id);


--
-- Name: product_custom_attribute_values ukkyig7pq5y5879sxove33xsgo7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_custom_attribute_values
    ADD CONSTRAINT ukkyig7pq5y5879sxove33xsgo7 UNIQUE (product_id, attribute_id);


--
-- Name: user_notification_preferences ukpdpjbvekm58r4ipsmgcxisw7r; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notification_preferences
    ADD CONSTRAINT ukpdpjbvekm58r4ipsmgcxisw7r UNIQUE (user_id, alert_type, channel);


--
-- Name: locations ukrvrg3med6hu2td5xyb6wduenx; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT ukrvrg3med6hu2td5xyb6wduenx UNIQUE (warehouse_id, code);


--
-- Name: unit_conversions unit_conversions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.unit_conversions
    ADD CONSTRAINT unit_conversions_pkey PRIMARY KEY (id);


--
-- Name: units_of_measure units_of_measure_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.units_of_measure
    ADD CONSTRAINT units_of_measure_pkey PRIMARY KEY (id);


--
-- Name: user_notification_preferences user_notification_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notification_preferences
    ADD CONSTRAINT user_notification_preferences_pkey PRIMARY KEY (id);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: warehouses warehouses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouses
    ADD CONSTRAINT warehouses_pkey PRIMARY KEY (id);


--
-- Name: idx_alerts_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_alerts_status ON public.alerts USING btree (status);


--
-- Name: idx_alerts_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_alerts_type ON public.alerts USING btree (type);


--
-- Name: idx_customers_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_email ON public.customers USING btree (email);


--
-- Name: idx_customers_phone; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_phone ON public.customers USING btree (phone);


--
-- Name: idx_import_jobs_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_jobs_created_at ON public.import_jobs USING btree (created_at);


--
-- Name: idx_inventory_counts_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_counts_status ON public.inventory_counts USING btree (status);


--
-- Name: idx_inventory_counts_warehouse; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inventory_counts_warehouse ON public.inventory_counts USING btree (warehouse_id);


--
-- Name: idx_loyalty_tx_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loyalty_tx_customer ON public.loyalty_transactions USING btree (customer_id);


--
-- Name: idx_loyalty_tx_sale; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loyalty_tx_sale ON public.loyalty_transactions USING btree (sale_id);


--
-- Name: idx_pos_sessions_cashier_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pos_sessions_cashier_status ON public.pos_sessions USING btree (cashier_id, status);


--
-- Name: idx_pos_sessions_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pos_sessions_status ON public.pos_sessions USING btree (status);


--
-- Name: idx_sales_session; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sales_session ON public.sales USING btree (pos_session_id);


--
-- Name: idx_sales_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sales_status ON public.sales USING btree (status);


--
-- Name: idx_stock_entries_entry_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_stock_entries_entry_date ON public.stock_entries USING btree (entry_date);


--
-- Name: idx_stock_entries_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_stock_entries_status ON public.stock_entries USING btree (status);


--
-- Name: idx_stock_exits_exit_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_stock_exits_exit_date ON public.stock_exits USING btree (exit_date);


--
-- Name: idx_stock_exits_reason; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_stock_exits_reason ON public.stock_exits USING btree (reason);


--
-- Name: idx_stock_exits_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_stock_exits_status ON public.stock_exits USING btree (status);


--
-- Name: idx_stock_movements_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_stock_movements_date ON public.stock_movements USING btree (movement_date);


--
-- Name: idx_stock_movements_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_stock_movements_product ON public.stock_movements USING btree (product_id);


--
-- Name: sale_refunds fk1e4hrsm5axvbajnce6ecvrumb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_refunds
    ADD CONSTRAINT fk1e4hrsm5axvbajnce6ecvrumb FOREIGN KEY (sale_id) REFERENCES public.sales(id);


--
-- Name: payments fk1iqngmfnvlamnd6hlxehpw48m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk1iqngmfnvlamnd6hlxehpw48m FOREIGN KEY (cashier_id) REFERENCES public.users(id);


--
-- Name: price_history fk1qwxogo4nas36rqukuaw0q8u6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_history
    ADD CONSTRAINT fk1qwxogo4nas36rqukuaw0q8u6 FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: stock_items fk2ak4gvkbwq1m0w2ys4xodtb2p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_items
    ADD CONSTRAINT fk2ak4gvkbwq1m0w2ys4xodtb2p FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: stock_exit_lines fk2uu7bicw4ma24ty69daa7xid5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_exit_lines
    ADD CONSTRAINT fk2uu7bicw4ma24ty69daa7xid5 FOREIGN KEY (variant_id) REFERENCES public.product_variants(id);


--
-- Name: sales fk32lqgcgbvt64uaaxuetfv2kss; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT fk32lqgcgbvt64uaaxuetfv2kss FOREIGN KEY (pos_session_id) REFERENCES public.pos_sessions(id);


--
-- Name: product_custom_attribute_values fk362h6cbibocy3y8qts75updgv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_custom_attribute_values
    ADD CONSTRAINT fk362h6cbibocy3y8qts75updgv FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: price_history fk4ev7jv8a9lg71gesy6hwtv2k1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.price_history
    ADD CONSTRAINT fk4ev7jv8a9lg71gesy6hwtv2k1 FOREIGN KEY (variant_id) REFERENCES public.product_variants(id);


--
-- Name: product_packagings fk4g7qg98oyqh3a5tna21q34o0s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_packagings
    ADD CONSTRAINT fk4g7qg98oyqh3a5tna21q34o0s FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: stock_transfer_lines fk4hs4g6emmrdjjhevm1a6lfqjx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfer_lines
    ADD CONSTRAINT fk4hs4g6emmrdjjhevm1a6lfqjx FOREIGN KEY (source_location_id) REFERENCES public.locations(id);


--
-- Name: loyalty_transactions fk4uoamo3dwy8xdystav2fs4x4c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_transactions
    ADD CONSTRAINT fk4uoamo3dwy8xdystav2fs4x4c FOREIGN KEY (sale_id) REFERENCES public.sales(id);


--
-- Name: sales fk51eqltjywqvwqy3mjiobehi7s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT fk51eqltjywqvwqy3mjiobehi7s FOREIGN KEY (seller_id) REFERENCES public.users(id);


--
-- Name: stock_movements fk68httiq2b07d6wh1p3hbkg6vx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fk68httiq2b07d6wh1p3hbkg6vx FOREIGN KEY (location_id) REFERENCES public.locations(id);


--
-- Name: sale_lines fk6htoapa13pby4h8ri0ncaqt9e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_lines
    ADD CONSTRAINT fk6htoapa13pby4h8ri0ncaqt9e FOREIGN KEY (variant_id) REFERENCES public.product_variants(id);


--
-- Name: inventory_counts fk70km01tdhwal706tmn6y6x94e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_counts
    ADD CONSTRAINT fk70km01tdhwal706tmn6y6x94e FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: inventory_count_lines fk72pj2p81o7ffb0kwwf22vr5d7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_count_lines
    ADD CONSTRAINT fk72pj2p81o7ffb0kwwf22vr5d7 FOREIGN KEY (lot_id) REFERENCES public.lots(id);


--
-- Name: stock_entry_lines fk7n12c34vkgjmkskj5spwynhpr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entry_lines
    ADD CONSTRAINT fk7n12c34vkgjmkskj5spwynhpr FOREIGN KEY (packaging_id) REFERENCES public.product_packagings(id);


--
-- Name: stock_transfer_lines fk7qao889s73qpnkqnu28bh2job; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfer_lines
    ADD CONSTRAINT fk7qao889s73qpnkqnu28bh2job FOREIGN KEY (variant_id) REFERENCES public.product_variants(id);


--
-- Name: payments fk8ip8q8rt0vahiay660ag6p7gm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk8ip8q8rt0vahiay660ag6p7gm FOREIGN KEY (pos_session_id) REFERENCES public.pos_sessions(id);


--
-- Name: stock_reservations fk8m0ux9naj6bfv94jrx2ft7s5l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_reservations
    ADD CONSTRAINT fk8m0ux9naj6bfv94jrx2ft7s5l FOREIGN KEY (variant_id) REFERENCES public.product_variants(id);


--
-- Name: stock_movements fk95qroinc6iu1e8ssep66uksrw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fk95qroinc6iu1e8ssep66uksrw FOREIGN KEY (lot_id) REFERENCES public.lots(id);


--
-- Name: inventory_count_lines fk9hy4sdtpj4j1gfhugxm1kh8ge; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_count_lines
    ADD CONSTRAINT fk9hy4sdtpj4j1gfhugxm1kh8ge FOREIGN KEY (variant_id) REFERENCES public.product_variants(id);


--
-- Name: stock_entry_lines fk9vdt7tqo0itl6tx31tw07gcih; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entry_lines
    ADD CONSTRAINT fk9vdt7tqo0itl6tx31tw07gcih FOREIGN KEY (variant_id) REFERENCES public.product_variants(id);


--
-- Name: stock_entry_lines fkas87vvseha0xdoqhb6i3sx9q2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entry_lines
    ADD CONSTRAINT fkas87vvseha0xdoqhb6i3sx9q2 FOREIGN KEY (stock_entry_id) REFERENCES public.stock_entries(id);


--
-- Name: product_documents fkasgot6r2ubpk889aisj7lgqcp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_documents
    ADD CONSTRAINT fkasgot6r2ubpk889aisj7lgqcp FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: sales fkb0oq4x86jf5yy6xjdskn3fjhr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT fkb0oq4x86jf5yy6xjdskn3fjhr FOREIGN KEY (location_id) REFERENCES public.locations(id);


--
-- Name: stock_reservations fkbvnv40iyf8tf31cs26ejqk5vl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_reservations
    ADD CONSTRAINT fkbvnv40iyf8tf31cs26ejqk5vl FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: locations fkbwtc4fdgfheldomtha9dudxij; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT fkbwtc4fdgfheldomtha9dudxij FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: stock_items fkc1g2titf47vuiv9sjsi1vcn1n; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_items
    ADD CONSTRAINT fkc1g2titf47vuiv9sjsi1vcn1n FOREIGN KEY (unit_id) REFERENCES public.units_of_measure(id);


--
-- Name: notifications fkcfowwrdbktg5p38rjdul6p0r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fkcfowwrdbktg5p38rjdul6p0r FOREIGN KEY (alert_id) REFERENCES public.alerts(id);


--
-- Name: lots fkci0gaxmj5owrs2ckmt54es6mq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lots
    ADD CONSTRAINT fkci0gaxmj5owrs2ckmt54es6mq FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: alerts fkcrnh5tx6oc6pfqvuhxo48fsan; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alerts
    ADD CONSTRAINT fkcrnh5tx6oc6pfqvuhxo48fsan FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: products fkcs0eljfnitnrdkas40xhyra9a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fkcs0eljfnitnrdkas40xhyra9a FOREIGN KEY (fournisseur_principal_id) REFERENCES public.suppliers(id);


--
-- Name: stock_items fkd27qtvqm0n1g5uuh8sfwqh1lk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_items
    ADD CONSTRAINT fkd27qtvqm0n1g5uuh8sfwqh1lk FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: sale_refund_lines fkd38959wfh8qr4de3d1iqgdpdl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_refund_lines
    ADD CONSTRAINT fkd38959wfh8qr4de3d1iqgdpdl FOREIGN KEY (refund_id) REFERENCES public.sale_refunds(id);


--
-- Name: stock_reservations fkd6ig3sfkpfuads8xkns43g513; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_reservations
    ADD CONSTRAINT fkd6ig3sfkpfuads8xkns43g513 FOREIGN KEY (location_id) REFERENCES public.locations(id);


--
-- Name: sales fkd94vrikapjd2ews1k4lb71sfg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT fkd94vrikapjd2ews1k4lb71sfg FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: product_custom_attribute_values fkdm668cqn6gsh5uuysajkxywda; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_custom_attribute_values
    ADD CONSTRAINT fkdm668cqn6gsh5uuysajkxywda FOREIGN KEY (attribute_id) REFERENCES public.custom_attribute_definitions(id);


--
-- Name: alerts fke8m8ikwlf4v27le6l2gbas23t; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alerts
    ADD CONSTRAINT fke8m8ikwlf4v27le6l2gbas23t FOREIGN KEY (lot_id) REFERENCES public.lots(id);


--
-- Name: role_permissions fkegdk29eiy7mdtefy5c7eirr6e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkegdk29eiy7mdtefy5c7eirr6e FOREIGN KEY (permission_id) REFERENCES public.permissions(id);


--
-- Name: stock_exit_lines fkegl4xmefeps776tbyvaydfx6r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_exit_lines
    ADD CONSTRAINT fkegl4xmefeps776tbyvaydfx6r FOREIGN KEY (packaging_id) REFERENCES public.product_packagings(id);


--
-- Name: alert_settings fketglcd1j5btycdijhg0tvjy0t; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alert_settings
    ADD CONSTRAINT fketglcd1j5btycdijhg0tvjy0t FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: stock_entries fkf65p1gva2groind16qbb2spyc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entries
    ADD CONSTRAINT fkf65p1gva2groind16qbb2spyc FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: inventory_count_lines fkf891jxbkhva9avg4q5hfjdy3m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_count_lines
    ADD CONSTRAINT fkf891jxbkhva9avg4q5hfjdy3m FOREIGN KEY (packaging_id) REFERENCES public.product_packagings(id);


--
-- Name: stock_items fkfhc6vs7189mf9wcgnp7jggo5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_items
    ADD CONSTRAINT fkfhc6vs7189mf9wcgnp7jggo5 FOREIGN KEY (location_id) REFERENCES public.locations(id);


--
-- Name: stock_items fkfwgumn9bglc655bbb8ylhp3rt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_items
    ADD CONSTRAINT fkfwgumn9bglc655bbb8ylhp3rt FOREIGN KEY (lot_id) REFERENCES public.lots(id);


--
-- Name: loyalty_transactions fkgjaecj4l1n9mkh4k0r2q15v0k; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_transactions
    ADD CONSTRAINT fkgjaecj4l1n9mkh4k0r2q15v0k FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: stock_entry_lines fkgrfhf2q8nr16i5tafh9x7ajh3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entry_lines
    ADD CONSTRAINT fkgrfhf2q8nr16i5tafh9x7ajh3 FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: user_roles fkh8ciramu9cc9q3qcqiv4ue8a6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkh8ciramu9cc9q3qcqiv4ue8a6 FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: stock_transfers fkhenhdgs1hqlktjhe9df0g13yq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfers
    ADD CONSTRAINT fkhenhdgs1hqlktjhe9df0g13yq FOREIGN KEY (source_warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: sales fkhf9hp5u4um5na1qrld83f70l2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT fkhf9hp5u4um5na1qrld83f70l2 FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: user_roles fkhfh9dx7w3ubf1co1vdev94g3f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: products fkhvrk5r5k4v28ha09d8ycvbdsb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fkhvrk5r5k4v28ha09d8ycvbdsb FOREIGN KEY (categorie_id) REFERENCES public.categories(id);


--
-- Name: stock_items fkhx8upf7uqcl65klkgbymqgm3s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_items
    ADD CONSTRAINT fkhx8upf7uqcl65klkgbymqgm3s FOREIGN KEY (variant_id) REFERENCES public.product_variants(id);


--
-- Name: stock_movements fki5lx44e8h4vw7varck1wpwyln; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fki5lx44e8h4vw7varck1wpwyln FOREIGN KEY (unit_id) REFERENCES public.units_of_measure(id);


--
-- Name: stock_movements fkiparp4rp4rsfsxb9y02oyxauh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fkiparp4rp4rsfsxb9y02oyxauh FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: supplier_purchase_orders fkiwapkmyg511t2int518o5rvrq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_purchase_orders
    ADD CONSTRAINT fkiwapkmyg511t2int518o5rvrq FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);


--
-- Name: stock_transfer_lines fkj3n70im5hk00uvcfkoludbh9h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfer_lines
    ADD CONSTRAINT fkj3n70im5hk00uvcfkoludbh9h FOREIGN KEY (lot_id) REFERENCES public.lots(id);


--
-- Name: product_suppliers fkj4tjiwcxs97lybw5vac1sjlbp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_suppliers
    ADD CONSTRAINT fkj4tjiwcxs97lybw5vac1sjlbp FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: stock_movements fkjcaag8ogfjxpwmqypi1wfdaog; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fkjcaag8ogfjxpwmqypi1wfdaog FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: stock_exit_lines fkjhmfp9fs0hlwx92tx36876c8r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_exit_lines
    ADD CONSTRAINT fkjhmfp9fs0hlwx92tx36876c8r FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: alerts fkk038isv4ipw82kirb02r6oivf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alerts
    ADD CONSTRAINT fkk038isv4ipw82kirb02r6oivf FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: inventory_count_lines fkkn5shhnbymyq3xiigaxmmj6ps; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_count_lines
    ADD CONSTRAINT fkkn5shhnbymyq3xiigaxmmj6ps FOREIGN KEY (location_id) REFERENCES public.locations(id);


--
-- Name: pos_sessions fkkpfjapecajra9fnfr8lrgnpud; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pos_sessions
    ADD CONSTRAINT fkkpfjapecajra9fnfr8lrgnpud FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: sale_lines fkkpwprnqql44yb12xdig9909u5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_lines
    ADD CONSTRAINT fkkpwprnqql44yb12xdig9909u5 FOREIGN KEY (sale_id) REFERENCES public.sales(id);


--
-- Name: payments fkl5ax948m03dbdcy3i0aql4jil; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fkl5ax948m03dbdcy3i0aql4jil FOREIGN KEY (sale_id) REFERENCES public.sales(id);


--
-- Name: alert_settings fklbg619xy5q1qbph97gxy0xmh2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alert_settings
    ADD CONSTRAINT fklbg619xy5q1qbph97gxy0xmh2 FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: stock_movements fklt1sie23b6mdeoko27nfop2gj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fklt1sie23b6mdeoko27nfop2gj FOREIGN KEY (packaging_id) REFERENCES public.product_packagings(id);


--
-- Name: alerts fklv2829loxkewusggr85jbu5fs; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alerts
    ADD CONSTRAINT fklv2829loxkewusggr85jbu5fs FOREIGN KEY (location_id) REFERENCES public.locations(id);


--
-- Name: unit_conversions fkm63h2x7klhu1pefo73tl91orf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.unit_conversions
    ADD CONSTRAINT fkm63h2x7klhu1pefo73tl91orf FOREIGN KEY (to_unit_id) REFERENCES public.units_of_measure(id);


--
-- Name: inventory_count_lines fkmeh9t6q4oqokce8p650ai0vcj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_count_lines
    ADD CONSTRAINT fkmeh9t6q4oqokce8p650ai0vcj FOREIGN KEY (inventory_count_id) REFERENCES public.inventory_counts(id);


--
-- Name: pos_sessions fkmpr6plls6b1sprl7ku30nb7tt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pos_sessions
    ADD CONSTRAINT fkmpr6plls6b1sprl7ku30nb7tt FOREIGN KEY (cashier_id) REFERENCES public.users(id);


--
-- Name: product_suppliers fkmymnd7phgm081dt3iv276yl8a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_suppliers
    ADD CONSTRAINT fkmymnd7phgm081dt3iv276yl8a FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);


--
-- Name: supplier_purchase_orders fkn44vwnctlmx8evfkyp2o2wwny; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplier_purchase_orders
    ADD CONSTRAINT fkn44vwnctlmx8evfkyp2o2wwny FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: role_permissions fkn5fotdgk8d1xvo8nav9uv3muc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkn5fotdgk8d1xvo8nav9uv3muc FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: stock_reservations fknojhmaduwmnxqk9fr736vs6do; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_reservations
    ADD CONSTRAINT fknojhmaduwmnxqk9fr736vs6do FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: stock_movements fko3rdw1xgft64g9fr8d3rnbt1q; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fko3rdw1xgft64g9fr8d3rnbt1q FOREIGN KEY (variant_id) REFERENCES public.product_variants(id);


--
-- Name: stock_entries fko4lbu5frifjnscud16oop18tp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entries
    ADD CONSTRAINT fko4lbu5frifjnscud16oop18tp FOREIGN KEY (location_id) REFERENCES public.locations(id);


--
-- Name: sale_refund_lines fko9dgqk3rt0ru2i16vjenlgaki; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_refund_lines
    ADD CONSTRAINT fko9dgqk3rt0ru2i16vjenlgaki FOREIGN KEY (sale_line_id) REFERENCES public.sale_lines(id);


--
-- Name: stock_transfer_lines fkomr5f22f88s8bdu4a3hfm4lxf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfer_lines
    ADD CONSTRAINT fkomr5f22f88s8bdu4a3hfm4lxf FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: stock_exits fkoovxw1184op24bq5i8a1o7l47; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_exits
    ADD CONSTRAINT fkoovxw1184op24bq5i8a1o7l47 FOREIGN KEY (location_id) REFERENCES public.locations(id);


--
-- Name: product_variants fkosqitn4s405cynmhb87lkvuau; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_variants
    ADD CONSTRAINT fkosqitn4s405cynmhb87lkvuau FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: products fkp7y7ip6v92uilvo03fidv9c0b; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fkp7y7ip6v92uilvo03fidv9c0b FOREIGN KEY (unit_id) REFERENCES public.units_of_measure(id);


--
-- Name: sale_lines fkpens42sda4j3do3tyu4u8u9el; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_lines
    ADD CONSTRAINT fkpens42sda4j3do3tyu4u8u9el FOREIGN KEY (packaging_id) REFERENCES public.product_packagings(id);


--
-- Name: stock_entries fkpx7ff456pe1y321dgv8w4cefn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_entries
    ADD CONSTRAINT fkpx7ff456pe1y321dgv8w4cefn FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);


--
-- Name: lots fkq9j88xwqnpcboa7k4mipm5y5t; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lots
    ADD CONSTRAINT fkq9j88xwqnpcboa7k4mipm5y5t FOREIGN KEY (variant_id) REFERENCES public.product_variants(id);


--
-- Name: stock_exits fkqhpkc3tyffbloberd6rcm676h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_exits
    ADD CONSTRAINT fkqhpkc3tyffbloberd6rcm676h FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: unit_conversions fkqj04ve51ya521hilcorhcdvn8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.unit_conversions
    ADD CONSTRAINT fkqj04ve51ya521hilcorhcdvn8 FOREIGN KEY (from_unit_id) REFERENCES public.units_of_measure(id);


--
-- Name: stock_transfer_lines fkqj7ihemc7rqm9fulth6ecuj5o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfer_lines
    ADD CONSTRAINT fkqj7ihemc7rqm9fulth6ecuj5o FOREIGN KEY (stock_transfer_id) REFERENCES public.stock_transfers(id);


--
-- Name: sales fkql733e5mdt3ytyey6u8egcv1c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT fkql733e5mdt3ytyey6u8egcv1c FOREIGN KEY (payment_session_id) REFERENCES public.pos_sessions(id);


--
-- Name: product_images fkqnq71xsohugpqwf3c9gxmsuy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_images
    ADD CONSTRAINT fkqnq71xsohugpqwf3c9gxmsuy FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: stock_reservations fkqwrnhld7f8puxb8f562k5wke3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_reservations
    ADD CONSTRAINT fkqwrnhld7f8puxb8f562k5wke3 FOREIGN KEY (lot_id) REFERENCES public.lots(id);


--
-- Name: stock_transfers fkr05rci2ugx45huugtjnjo05bc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfers
    ADD CONSTRAINT fkr05rci2ugx45huugtjnjo05bc FOREIGN KEY (dest_warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: stock_transfer_lines fkrrp3nr0l1thxxu1bvqc0xnq8c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfer_lines
    ADD CONSTRAINT fkrrp3nr0l1thxxu1bvqc0xnq8c FOREIGN KEY (dest_location_id) REFERENCES public.locations(id);


--
-- Name: sale_lines fkrv7cdt02ykk0w12tw35fr3w71; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sale_lines
    ADD CONSTRAINT fkrv7cdt02ykk0w12tw35fr3w71 FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: categories fksaok720gsu4u2wrgbk10b5n8d; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT fksaok720gsu4u2wrgbk10b5n8d FOREIGN KEY (parent_id) REFERENCES public.categories(id);


--
-- Name: stock_exit_lines fksbavug03a64xffpa8gq7thktm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_exit_lines
    ADD CONSTRAINT fksbavug03a64xffpa8gq7thktm FOREIGN KEY (stock_exit_id) REFERENCES public.stock_exits(id);


--
-- Name: inventory_counts fksxj6ctlb0t55kfrf02dlanja6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_counts
    ADD CONSTRAINT fksxj6ctlb0t55kfrf02dlanja6 FOREIGN KEY (location_id) REFERENCES public.locations(id);


--
-- Name: inventory_count_lines fksxxqrspfglpb9dwh0nl0bfmd8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inventory_count_lines
    ADD CONSTRAINT fksxxqrspfglpb9dwh0nl0bfmd8 FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: sales fktbqsuaq9mtds03lxuiub7m4b1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sales
    ADD CONSTRAINT fktbqsuaq9mtds03lxuiub7m4b1 FOREIGN KEY (cashier_id) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--


