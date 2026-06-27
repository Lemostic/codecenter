-- 达梦 DM8 Schema (兼容 Oracle 语法)
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE cosid (
    name         VARCHAR(128) NOT NULL,
    namespace    VARCHAR(64)  DEFAULT ''cosid'' NOT NULL,
    max_id       BIGINT       DEFAULT 0 NOT NULL,
    step         INT          DEFAULT 1000 NOT NULL,
    description  VARCHAR(256),
    create_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (name, namespace)
);

CREATE TABLE md_theme_domain (
    id VARCHAR(32) NOT NULL,
    parent_id VARCHAR(32),
    domain_code VARCHAR(50) NOT NULL,
    domain_name VARCHAR(50) NOT NULL,
    sort_order INT DEFAULT 0 NOT NULL,
    remark VARCHAR(500),
    tenant_id VARCHAR(32),
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE md_model (
    id VARCHAR(32) NOT NULL,
    model_code VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    table_name VARCHAR(64) NOT NULL,
    model_type VARCHAR(20) DEFAULT ''NORMAL'' NOT NULL,
    theme_id VARCHAR(32),
    description VARCHAR(500),
    security_level VARCHAR(16) DEFAULT ''INTERNAL'',
    version INT DEFAULT 1 NOT NULL,
    status VARCHAR(16) DEFAULT ''EDIT'' NOT NULL,
    tenant_id VARCHAR(32),
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE md_model_attribute (
    id VARCHAR(32) NOT NULL,
    model_id VARCHAR(32) NOT NULL,
    cn_name VARCHAR(100) NOT NULL,
    en_name VARCHAR(100) NOT NULL,
    data_type VARCHAR(32) NOT NULL,
    data_length INT,
    decimal_length INT,
    is_required TINYINT DEFAULT 0 NOT NULL,
    is_unique TINYINT DEFAULT 0 NOT NULL,
    is_code_field TINYINT DEFAULT 0 NOT NULL,
    default_value VARCHAR(500),
    dict_type VARCHAR(64),
    sort_order INT DEFAULT 0 NOT NULL,
    status VARCHAR(16) DEFAULT ''EDIT'' NOT NULL,
    comment VARCHAR(500),
    tenant_id VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE md_code_rule (
    id VARCHAR(32) NOT NULL,
    model_id VARCHAR(32) NOT NULL,
    encode_field_id VARCHAR(32) NOT NULL,
    rule_name VARCHAR(64) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_desc VARCHAR(200),
    rule_mode VARCHAR(16) DEFAULT ''DSL'' NOT NULL,
    trigger_type VARCHAR(16) DEFAULT ''BUTTON'' NOT NULL,
    dsl_template VARCHAR(1000),
    groovy_script TEXT,
    version INT DEFAULT 1 NOT NULL,
    status VARCHAR(16) DEFAULT ''EDIT'' NOT NULL,
    recycle_lock_hours INT DEFAULT 24 NOT NULL,
    recycle_strategy VARCHAR(16) DEFAULT ''AUTO'',
    created_by VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(64),
    updated_at TIMESTAMP,
    published_at TIMESTAMP,
    disabled_at TIMESTAMP,
    tenant_id VARCHAR(32),
    PRIMARY KEY (id)
);

CREATE TABLE md_code_segment (
    id VARCHAR(32) NOT NULL,
    segment_code VARCHAR(64) NOT NULL,
    segment_name VARCHAR(100) NOT NULL,
    segment_type VARCHAR(32) NOT NULL,
    config_json TEXT NOT NULL,
    description VARCHAR(200),
    is_archived TINYINT DEFAULT 0 NOT NULL,
    tenant_id VARCHAR(32),
    created_by VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(64),
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE md_code_rule_segment (
    id VARCHAR(32) NOT NULL,
    rule_id VARCHAR(32) NOT NULL,
    segment_id VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL,
    reset_condition VARCHAR(64),
    tenant_id VARCHAR(32),
    PRIMARY KEY (id)
);

CREATE TABLE md_code_allocation (
    id VARCHAR(32) NOT NULL,
    rule_id VARCHAR(32) NOT NULL,
    rule_version_id VARCHAR(32),
    code VARCHAR(200) NOT NULL,
    sequence_num BIGINT,
    status VARCHAR(20) DEFAULT ''PENDING'' NOT NULL,
    is_exposed TINYINT DEFAULT 0 NOT NULL,
    is_archived TINYINT DEFAULT 0 NOT NULL,
    waste_type VARCHAR(20),
    segment_values TEXT,
    data_id VARCHAR(64),
    allocate_time TIMESTAMP NOT NULL,
    confirm_time TIMESTAMP,
    used_time TIMESTAMP,
    cancel_time TIMESTAMP,
    recycle_time TIMESTAMP,
    recycle_lock_time TIMESTAMP,
    trace_id VARCHAR(64),
    tenant_id VARCHAR(32),
    version INT DEFAULT 0 NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE md_code_water_mark (
    id VARCHAR(32) NOT NULL,
    biz_tag VARCHAR(128) NOT NULL,
    rule_id VARCHAR(32) NOT NULL,
    current_water BIGINT DEFAULT 0 NOT NULL,
    last_allocate_time TIMESTAMP,
    last_calibrate_time TIMESTAMP,
    calibrate_source VARCHAR(20),
    version INT DEFAULT 0 NOT NULL,
    tenant_id VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE md_model_audit_log (
    log_id VARCHAR(32) NOT NULL,
    operator_id VARCHAR(32) NOT NULL,
    operator_name VARCHAR(64),
    operation_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(32) NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    before_state VARCHAR(32),
    after_state VARCHAR(32),
    diff_snapshot TEXT,
    operator_ip VARCHAR(64),
    operated_at TIMESTAMP NOT NULL,
    tenant_id VARCHAR(32),
    PRIMARY KEY (log_id)
);

SET FOREIGN_KEY_CHECKS = 1;
