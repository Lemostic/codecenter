-- Oracle 11g/19c Schema for Code Center
-- 适用: Oracle 11g+, VARCHAR2 替代 VARCHAR, NUMBER 替代 INT, CLOB 替代 TEXT
-- 部署: 通过 sqlplus 或 jdbc 执行

CREATE SEQUENCE COSID_SEQ START WITH 1 INCREMENT BY 1;

CREATE TABLE cosid (
    name         VARCHAR2(128) NOT NULL,
    namespace    VARCHAR2(64)  DEFAULT ''cosid'' NOT NULL,
    max_id       NUMBER(19)    DEFAULT 0 NOT NULL,
    step         NUMBER(10)    DEFAULT 1000 NOT NULL,
    description  VARCHAR2(256),
    create_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (name, namespace)
);

CREATE TABLE md_theme_domain (
    id          VARCHAR2(32)  NOT NULL,
    parent_id   VARCHAR2(32),
    domain_code VARCHAR2(50)  NOT NULL,
    domain_name VARCHAR2(50)  NOT NULL,
    sort_order  NUMBER(10)    DEFAULT 0 NOT NULL,
    remark      VARCHAR2(500),
    tenant_id   VARCHAR2(32),
    create_by   VARCHAR2(64),
    create_time TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_by   VARCHAR2(64),
    update_time TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_domain_code ON md_theme_domain(tenant_id, domain_code);

CREATE TABLE md_model (
    id             VARCHAR2(32)  NOT NULL,
    model_code     VARCHAR2(50)  NOT NULL,
    model_name     VARCHAR2(100) NOT NULL,
    table_name     VARCHAR2(64)  NOT NULL,
    model_type     VARCHAR2(20)  DEFAULT ''NORMAL'' NOT NULL,
    theme_id       VARCHAR2(32),
    description    VARCHAR2(500),
    security_level VARCHAR2(16)  DEFAULT ''INTERNAL'',
    version        NUMBER(10)    DEFAULT 1 NOT NULL,
    status         VARCHAR2(16)  DEFAULT ''EDIT'' NOT NULL,
    tenant_id      VARCHAR2(32),
    create_by      VARCHAR2(64),
    create_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_by      VARCHAR2(64),
    update_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_model_code ON md_model(tenant_id, model_code);
CREATE UNIQUE INDEX uk_table_name ON md_model(tenant_id, table_name);

CREATE TABLE md_model_attribute (
    id              VARCHAR2(32)  NOT NULL,
    model_id        VARCHAR2(32)  NOT NULL,
    cn_name         VARCHAR2(100) NOT NULL,
    en_name         VARCHAR2(100) NOT NULL,
    data_type       VARCHAR2(32)  NOT NULL,
    data_length     NUMBER(10),
    decimal_length  NUMBER(10),
    is_required     NUMBER(1)     DEFAULT 0 NOT NULL,
    is_unique       NUMBER(1)     DEFAULT 0 NOT NULL,
    is_code_field   NUMBER(1)     DEFAULT 0 NOT NULL,
    default_value   VARCHAR2(500),
    dict_type       VARCHAR2(64),
    sort_order      NUMBER(10)    DEFAULT 0 NOT NULL,
    status          VARCHAR2(16)  DEFAULT ''EDIT'' NOT NULL,
    comment         VARCHAR2(500),
    tenant_id       VARCHAR2(32),
    create_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_model_enname ON md_model_attribute(model_id, en_name);

CREATE TABLE md_code_rule (
    id                 VARCHAR2(32)  NOT NULL,
    model_id           VARCHAR2(32)  NOT NULL,
    encode_field_id    VARCHAR2(32)  NOT NULL,
    rule_name          VARCHAR2(64)  NOT NULL,
    rule_code          VARCHAR2(64)  NOT NULL,
    rule_desc          VARCHAR2(200),
    rule_mode          VARCHAR2(16)  DEFAULT ''DSL'' NOT NULL,
    trigger_type       VARCHAR2(16)  DEFAULT ''BUTTON'' NOT NULL,
    dsl_template       VARCHAR2(1000),
    groovy_script      CLOB,
    version            NUMBER(10)    DEFAULT 1 NOT NULL,
    status             VARCHAR2(16)  DEFAULT ''EDIT'' NOT NULL,
    recycle_lock_hours NUMBER(10)    DEFAULT 24 NOT NULL,
    recycle_strategy   VARCHAR2(16)  DEFAULT ''AUTO'',
    created_by         VARCHAR2(64),
    created_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by         VARCHAR2(64),
    updated_at         TIMESTAMP,
    published_at       TIMESTAMP,
    disabled_at        TIMESTAMP,
    tenant_id          VARCHAR2(32),
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_model_field_version ON md_code_rule(model_id, encode_field_id, version);

CREATE TABLE md_code_segment (
    id           VARCHAR2(32)  NOT NULL,
    segment_code VARCHAR2(64)  NOT NULL,
    segment_name VARCHAR2(100) NOT NULL,
    segment_type VARCHAR2(32)  NOT NULL,
    config_json  CLOB          NOT NULL,
    description  VARCHAR2(200),
    is_archived  NUMBER(1)     DEFAULT 0 NOT NULL,
    tenant_id    VARCHAR2(32),
    created_by   VARCHAR2(64),
    created_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by   VARCHAR2(64),
    updated_at   TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_segment_code ON md_code_segment(tenant_id, segment_code);
CREATE UNIQUE INDEX uk_segment_name ON md_code_segment(tenant_id, segment_name);

CREATE TABLE md_code_rule_segment (
    id              VARCHAR2(32) NOT NULL,
    rule_id         VARCHAR2(32) NOT NULL,
    segment_id      VARCHAR2(32) NOT NULL,
    sort_order      NUMBER(10)   NOT NULL,
    reset_condition VARCHAR2(64),
    tenant_id       VARCHAR2(32),
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_rule_order ON md_code_rule_segment(rule_id, sort_order);

CREATE TABLE md_code_allocation (
    id                VARCHAR2(32)  NOT NULL,
    rule_id           VARCHAR2(32)  NOT NULL,
    rule_version_id   VARCHAR2(32),
    code              VARCHAR2(200) NOT NULL,
    sequence_num      NUMBER(19),
    status            VARCHAR2(20)  DEFAULT ''PENDING'' NOT NULL,
    is_exposed        NUMBER(1)     DEFAULT 0 NOT NULL,
    is_archived       NUMBER(1)     DEFAULT 0 NOT NULL,
    waste_type        VARCHAR2(20),
    segment_values    CLOB,
    data_id           VARCHAR2(64),
    allocate_time     TIMESTAMP     NOT NULL,
    confirm_time      TIMESTAMP,
    used_time         TIMESTAMP,
    cancel_time       TIMESTAMP,
    recycle_time      TIMESTAMP,
    recycle_lock_time TIMESTAMP,
    trace_id          VARCHAR2(64),
    tenant_id         VARCHAR2(32),
    version           NUMBER(10)    DEFAULT 0 NOT NULL,
    create_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_code ON md_code_allocation(code);
CREATE INDEX idx_rule_status ON md_code_allocation(rule_id, status);
CREATE INDEX idx_recycle ON md_code_allocation(rule_id, status, recycle_lock_time);
CREATE INDEX idx_expire ON md_code_allocation(status, is_exposed, allocate_time);

CREATE TABLE md_code_water_mark (
    id                  VARCHAR2(32)  NOT NULL,
    biz_tag             VARCHAR2(128) NOT NULL,
    rule_id             VARCHAR2(32)  NOT NULL,
    current_water       NUMBER(19)    DEFAULT 0 NOT NULL,
    last_allocate_time  TIMESTAMP,
    last_calibrate_time TIMESTAMP,
    calibrate_source    VARCHAR2(20),
    version             NUMBER(10)    DEFAULT 0 NOT NULL,
    tenant_id           VARCHAR2(32),
    create_time         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_biz_tag ON md_code_water_mark(biz_tag);

CREATE TABLE md_model_audit_log (
    log_id         VARCHAR2(32)  NOT NULL,
    operator_id    VARCHAR2(32)  NOT NULL,
    operator_name  VARCHAR2(64),
    operation_type VARCHAR2(32)  NOT NULL,
    target_id      VARCHAR2(32)  NOT NULL,
    target_type    VARCHAR2(16)  NOT NULL,
    before_state   VARCHAR2(32),
    after_state    VARCHAR2(32),
    diff_snapshot  CLOB,
    operator_ip    VARCHAR2(64),
    operated_at    TIMESTAMP     NOT NULL,
    tenant_id      VARCHAR2(32),
    PRIMARY KEY (log_id)
);
CREATE INDEX idx_target ON md_model_audit_log(target_id, target_type);
CREATE INDEX idx_time ON md_model_audit_log(operated_at);

COMMENT ON TABLE md_code_rule IS ''encoding rule main table'';
COMMENT ON TABLE md_code_allocation IS ''code allocation combined table'';
