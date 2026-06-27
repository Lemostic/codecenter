-- H2 schema for tests (MySQL compatibility mode)
SET MODE MySQL;

DROP TABLE IF EXISTS cosid;
CREATE TABLE cosid (
    name         VARCHAR(128) NOT NULL,
    namespace    VARCHAR(64)  NOT NULL DEFAULT ''cosid'',
    max_id       BIGINT       NOT NULL DEFAULT 0,
    step         INT          NOT NULL DEFAULT 1000,
    description  VARCHAR(256) DEFAULT NULL,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (name, namespace)
);

DROP TABLE IF EXISTS md_theme_domain;
CREATE TABLE md_theme_domain (
    id            VARCHAR(32)  NOT NULL,
    parent_id     VARCHAR(32)  DEFAULT NULL,
    domain_code   VARCHAR(50)  NOT NULL,
    domain_name   VARCHAR(50)  NOT NULL,
    sort_order    INT          NOT NULL DEFAULT 0,
    remark        VARCHAR(500) DEFAULT NULL,
    tenant_id     VARCHAR(32)  DEFAULT NULL,
    create_by     VARCHAR(64)  DEFAULT NULL,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     VARCHAR(64)  DEFAULT NULL,
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_parent ON md_theme_domain(parent_id);
CREATE INDEX idx_tenant ON md_theme_domain(tenant_id);

DROP TABLE IF EXISTS md_model;
CREATE TABLE md_model (
    id              VARCHAR(32)  NOT NULL,
    model_code      VARCHAR(50)  NOT NULL,
    model_name      VARCHAR(100) NOT NULL,
    table_name      VARCHAR(64)  NOT NULL,
    model_type      VARCHAR(20)  NOT NULL DEFAULT ''NORMAL'',
    theme_id        VARCHAR(32)  DEFAULT NULL,
    description     VARCHAR(500) DEFAULT NULL,
    security_level  VARCHAR(16)  DEFAULT ''INTERNAL'',
    version         INT          NOT NULL DEFAULT 1,
    status          VARCHAR(16)  NOT NULL DEFAULT ''EDIT'',
    tenant_id       VARCHAR(32)  DEFAULT NULL,
    create_by       VARCHAR(64)  DEFAULT NULL,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)  DEFAULT NULL,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_model_code ON md_model(tenant_id, model_code);
CREATE UNIQUE INDEX uk_table_name ON md_model(tenant_id, table_name);

DROP TABLE IF EXISTS md_model_attribute;
CREATE TABLE md_model_attribute (
    id                VARCHAR(32)  NOT NULL,
    model_id          VARCHAR(32)  NOT NULL,
    cn_name           VARCHAR(100) NOT NULL,
    en_name           VARCHAR(100) NOT NULL,
    data_type         VARCHAR(32)  NOT NULL,
    data_length       INT          DEFAULT NULL,
    decimal_length    INT          DEFAULT NULL,
    is_required       TINYINT      NOT NULL DEFAULT 0,
    is_unique         TINYINT      NOT NULL DEFAULT 0,
    is_code_field     TINYINT      NOT NULL DEFAULT 0,
    default_value     VARCHAR(500) DEFAULT NULL,
    dict_type         VARCHAR(64)  DEFAULT NULL,
    sort_order        INT          NOT NULL DEFAULT 0,
    status            VARCHAR(16)  NOT NULL DEFAULT ''EDIT'',
    comment           VARCHAR(500) DEFAULT NULL,
    tenant_id         VARCHAR(32)  DEFAULT NULL,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_model_enname ON md_model_attribute(model_id, en_name);
CREATE INDEX idx_code_field ON md_model_attribute(model_id, is_code_field);

DROP TABLE IF EXISTS md_code_rule;
CREATE TABLE md_code_rule (
    id                  VARCHAR(32)   NOT NULL,
    model_id            VARCHAR(32)   NOT NULL,
    encode_field_id     VARCHAR(32)   NOT NULL,
    rule_name           VARCHAR(64)   NOT NULL,
    rule_code           VARCHAR(64)   NOT NULL,
    rule_desc           VARCHAR(200)  DEFAULT NULL,
    rule_mode           VARCHAR(16)   NOT NULL DEFAULT ''DSL'',
    trigger_type        VARCHAR(16)   NOT NULL DEFAULT ''BUTTON'',
    dsl_template        VARCHAR(1000) DEFAULT NULL,
    groovy_script       CLOB          DEFAULT NULL,
    version             INT           NOT NULL DEFAULT 1,
    status              VARCHAR(16)   NOT NULL DEFAULT ''EDIT'',
    recycle_lock_hours  INT           NOT NULL DEFAULT 24,
    recycle_strategy    VARCHAR(16)   DEFAULT ''AUTO'',
    created_by          VARCHAR(64)   DEFAULT NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64)   DEFAULT NULL,
    updated_at          TIMESTAMP     DEFAULT NULL,
    published_at        TIMESTAMP     DEFAULT NULL,
    disabled_at         TIMESTAMP     DEFAULT NULL,
    tenant_id           VARCHAR(32)   DEFAULT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_model_field_version ON md_code_rule(model_id, encode_field_id, version);

DROP TABLE IF EXISTS md_code_segment;
CREATE TABLE md_code_segment (
    id              VARCHAR(32)  NOT NULL,
    segment_code    VARCHAR(64)  NOT NULL,
    segment_name    VARCHAR(100) NOT NULL,
    segment_type    VARCHAR(32)  NOT NULL,
    config_json     CLOB         NOT NULL,
    description     VARCHAR(200) DEFAULT NULL,
    is_archived     TINYINT      NOT NULL DEFAULT 0,
    tenant_id       VARCHAR(32)  DEFAULT NULL,
    created_by      VARCHAR(64)  DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64)  DEFAULT NULL,
    updated_at      TIMESTAMP    DEFAULT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_segment_code ON md_code_segment(tenant_id, segment_code);
CREATE UNIQUE INDEX uk_segment_name ON md_code_segment(tenant_id, segment_name);

DROP TABLE IF EXISTS md_code_rule_segment;
CREATE TABLE md_code_rule_segment (
    id              VARCHAR(32)  NOT NULL,
    rule_id         VARCHAR(32)  NOT NULL,
    segment_id      VARCHAR(32)  NOT NULL,
    sort_order      INT          NOT NULL,
    reset_condition VARCHAR(64)  DEFAULT NULL,
    tenant_id       VARCHAR(32)  DEFAULT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_rule_order ON md_code_rule_segment(rule_id, sort_order);

DROP TABLE IF EXISTS md_code_allocation;
CREATE TABLE md_code_allocation (
    id                  VARCHAR(32)   NOT NULL,
    rule_id             VARCHAR(32)   NOT NULL,
    rule_version_id     VARCHAR(32)   DEFAULT NULL,
    code                VARCHAR(200)  NOT NULL,
    sequence_num        BIGINT        DEFAULT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT ''PENDING'',
    is_exposed          TINYINT       NOT NULL DEFAULT 0,
    is_archived         TINYINT       NOT NULL DEFAULT 0,
    waste_type          VARCHAR(20)   DEFAULT NULL,
    segment_values      CLOB          DEFAULT NULL,
    data_id             VARCHAR(64)   DEFAULT NULL,
    allocate_time       TIMESTAMP     NOT NULL,
    confirm_time        TIMESTAMP     DEFAULT NULL,
    used_time           TIMESTAMP     DEFAULT NULL,
    cancel_time         TIMESTAMP     DEFAULT NULL,
    recycle_time        TIMESTAMP     DEFAULT NULL,
    recycle_lock_time   TIMESTAMP     DEFAULT NULL,
    trace_id            VARCHAR(64)   DEFAULT NULL,
    tenant_id           VARCHAR(32)   DEFAULT NULL,
    version             INT           NOT NULL DEFAULT 0,
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_code ON md_code_allocation(code);
CREATE INDEX idx_rule_status ON md_code_allocation(rule_id, status);
CREATE INDEX idx_recycle ON md_code_allocation(rule_id, status, recycle_lock_time);
CREATE INDEX idx_expire ON md_code_allocation(status, is_exposed, allocate_time);
CREATE INDEX idx_data ON md_code_allocation(data_id);

DROP TABLE IF EXISTS md_code_water_mark;
CREATE TABLE md_code_water_mark (
    id                  VARCHAR(32)   NOT NULL,
    biz_tag             VARCHAR(128)  NOT NULL,
    rule_id             VARCHAR(32)   NOT NULL,
    current_water       BIGINT        NOT NULL DEFAULT 0,
    last_allocate_time  TIMESTAMP     DEFAULT NULL,
    last_calibrate_time TIMESTAMP     DEFAULT NULL,
    calibrate_source    VARCHAR(20)   DEFAULT NULL,
    version             INT           NOT NULL DEFAULT 0,
    tenant_id           VARCHAR(32)   DEFAULT NULL,
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_tag ON md_code_water_mark(biz_tag);

DROP TABLE IF EXISTS md_model_audit_log;
CREATE TABLE md_model_audit_log (
    log_id         VARCHAR(32)  NOT NULL,
    operator_id    VARCHAR(32)  NOT NULL,
    operator_name  VARCHAR(64)  DEFAULT NULL,
    operation_type VARCHAR(32)  NOT NULL,
    target_id      VARCHAR(32)  NOT NULL,
    target_type    VARCHAR(16)  NOT NULL,
    before_state   VARCHAR(32)  DEFAULT NULL,
    after_state    VARCHAR(32)  DEFAULT NULL,
    diff_snapshot  CLOB         DEFAULT NULL,
    operator_ip    VARCHAR(64)  DEFAULT NULL,
    operated_at    TIMESTAMP    NOT NULL,
    tenant_id      VARCHAR(32)  DEFAULT NULL,
    PRIMARY KEY (log_id)
);
CREATE INDEX idx_target ON md_model_audit_log(target_id, target_type);
CREATE INDEX idx_time ON md_model_audit_log(operated_at);
