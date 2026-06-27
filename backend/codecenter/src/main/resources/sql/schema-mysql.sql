-- ====================================================================
--  编码中心 (Code Center) - MySQL 8.0+ Schema
--  Database: mdm_code / Charset: utf8mb4 / Engine: InnoDB
-- ====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `cosid`;
CREATE TABLE `cosid` (
    `name`         VARCHAR(128) NOT NULL,
    `namespace`    VARCHAR(64)  NOT NULL DEFAULT ''cosid'',
    `max_id`       BIGINT       NOT NULL DEFAULT 0,
    `step`         INT          NOT NULL DEFAULT 1000,
    `description`  VARCHAR(256) DEFAULT NULL,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`name`, `namespace`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''CosId number segment table'';

DROP TABLE IF EXISTS `md_theme_domain`;
CREATE TABLE `md_theme_domain` (
    `id`            VARCHAR(32)  NOT NULL,
    `parent_id`     VARCHAR(32)  DEFAULT NULL,
    `domain_code`   VARCHAR(50)  NOT NULL,
    `domain_name`   VARCHAR(50)  NOT NULL,
    `sort_order`    INT          NOT NULL DEFAULT 0,
    `remark`        VARCHAR(500) DEFAULT NULL,
    `tenant_id`     VARCHAR(32)  DEFAULT NULL,
    `create_by`     VARCHAR(64)  DEFAULT NULL,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`     VARCHAR(64)  DEFAULT NULL,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`),
    KEY `idx_tenant` (`tenant_id`),
    UNIQUE KEY `uk_domain_code` (`tenant_id`, `domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''theme domain tree'';

DROP TABLE IF EXISTS `md_model`;
CREATE TABLE `md_model` (
    `id`              VARCHAR(32)  NOT NULL,
    `model_code`      VARCHAR(50)  NOT NULL,
    `model_name`      VARCHAR(100) NOT NULL,
    `table_name`      VARCHAR(64)  NOT NULL,
    `model_type`      VARCHAR(20)  NOT NULL DEFAULT ''NORMAL'',
    `theme_id`        VARCHAR(32)  DEFAULT NULL,
    `description`     VARCHAR(500) DEFAULT NULL,
    `security_level`  VARCHAR(16)  DEFAULT ''INTERNAL'',
    `version`         INT          NOT NULL DEFAULT 1,
    `status`          VARCHAR(16)  NOT NULL DEFAULT ''EDIT'',
    `tenant_id`       VARCHAR(32)  DEFAULT NULL,
    `create_by`       VARCHAR(64)  DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(64)  DEFAULT NULL,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_code` (`tenant_id`, `model_code`),
    UNIQUE KEY `uk_table_name` (`tenant_id`, `table_name`),
    KEY `idx_theme` (`theme_id`),
    KEY `idx_status` (`status`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''master data model main table'';

DROP TABLE IF EXISTS `md_model_attribute`;
CREATE TABLE `md_model_attribute` (
    `id`                VARCHAR(32)  NOT NULL,
    `model_id`          VARCHAR(32)  NOT NULL,
    `cn_name`           VARCHAR(100) NOT NULL,
    `en_name`           VARCHAR(100) NOT NULL,
    `data_type`         VARCHAR(32)  NOT NULL,
    `data_length`       INT          DEFAULT NULL,
    `decimal_length`    INT          DEFAULT NULL,
    `is_required`       TINYINT(1)   NOT NULL DEFAULT 0,
    `is_unique`         TINYINT(1)   NOT NULL DEFAULT 0,
    `is_code_field`     TINYINT(1)   NOT NULL DEFAULT 0,
    `default_value`     VARCHAR(500) DEFAULT NULL,
    `dict_type`         VARCHAR(64)  DEFAULT NULL,
    `sort_order`        INT          NOT NULL DEFAULT 0,
    `status`            VARCHAR(16)  NOT NULL DEFAULT ''EDIT'',
    `comment`           VARCHAR(500) DEFAULT NULL,
    `tenant_id`         VARCHAR(32)  DEFAULT NULL,
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_enname` (`model_id`, `en_name`),
    KEY `idx_model_sort` (`model_id`, `sort_order`),
    KEY `idx_code_field` (`model_id`, `is_code_field`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''model attribute metadata'';

DROP TABLE IF EXISTS `md_code_rule`;
CREATE TABLE `md_code_rule` (
    `id`                  VARCHAR(32)   NOT NULL,
    `model_id`            VARCHAR(32)   NOT NULL,
    `encode_field_id`     VARCHAR(32)   NOT NULL,
    `rule_name`           VARCHAR(64)   NOT NULL,
    `rule_code`           VARCHAR(64)   NOT NULL,
    `rule_desc`           VARCHAR(200)  DEFAULT NULL,
    `rule_mode`           VARCHAR(16)   NOT NULL DEFAULT ''DSL'',
    `trigger_type`        VARCHAR(16)   NOT NULL DEFAULT ''BUTTON'',
    `dsl_template`        VARCHAR(1000) DEFAULT NULL,
    `groovy_script`       MEDIUMTEXT    DEFAULT NULL,
    `version`             INT           NOT NULL DEFAULT 1,
    `status`              VARCHAR(16)   NOT NULL DEFAULT ''EDIT'',
    `recycle_lock_hours`  INT           NOT NULL DEFAULT 24,
    `recycle_strategy`    VARCHAR(16)   DEFAULT ''AUTO'',
    `created_by`          VARCHAR(64)   DEFAULT NULL,
    `created_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`          VARCHAR(64)   DEFAULT NULL,
    `updated_at`          DATETIME      DEFAULT NULL,
    `published_at`        DATETIME      DEFAULT NULL,
    `disabled_at`         DATETIME      DEFAULT NULL,
    `tenant_id`           VARCHAR(32)   DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_field_version` (`model_id`, `encode_field_id`, `version`),
    KEY `idx_model` (`model_id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''encoding rule main table'';

DROP TABLE IF EXISTS `md_code_segment`;
CREATE TABLE `md_code_segment` (
    `id`              VARCHAR(32)  NOT NULL,
    `segment_code`    VARCHAR(64)  NOT NULL,
    `segment_name`    VARCHAR(100) NOT NULL,
    `segment_type`    VARCHAR(32)  NOT NULL,
    `config_json`     JSON         NOT NULL,
    `description`     VARCHAR(200) DEFAULT NULL,
    `is_archived`     TINYINT(1)   NOT NULL DEFAULT 0,
    `tenant_id`       VARCHAR(32)  DEFAULT NULL,
    `created_by`      VARCHAR(64)  DEFAULT NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by`      VARCHAR(64)  DEFAULT NULL,
    `updated_at`      DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_segment_code` (`tenant_id`, `segment_code`),
    UNIQUE KEY `uk_segment_name` (`tenant_id`, `segment_name`),
    KEY `idx_type` (`segment_type`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''code segment master data'';

DROP TABLE IF EXISTS `md_code_rule_segment`;
CREATE TABLE `md_code_rule_segment` (
    `id`              VARCHAR(32)  NOT NULL,
    `rule_id`         VARCHAR(32)  NOT NULL,
    `segment_id`      VARCHAR(32)  NOT NULL,
    `sort_order`      INT          NOT NULL,
    `reset_condition` VARCHAR(64)  DEFAULT NULL,
    `tenant_id`       VARCHAR(32)  DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_order` (`rule_id`, `sort_order`),
    KEY `idx_segment` (`segment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''rule-segment mapping'';

DROP TABLE IF EXISTS `md_code_allocation`;
CREATE TABLE `md_code_allocation` (
    `id`                  VARCHAR(32)   NOT NULL,
    `rule_id`             VARCHAR(32)   NOT NULL,
    `rule_version_id`     VARCHAR(32)   DEFAULT NULL,
    `code`                VARCHAR(200)  NOT NULL,
    `sequence_num`        BIGINT        DEFAULT NULL,
    `status`              VARCHAR(20)   NOT NULL DEFAULT ''PENDING'',
    `is_exposed`          TINYINT(1)    NOT NULL DEFAULT 0,
    `is_archived`         TINYINT(1)    NOT NULL DEFAULT 0,
    `waste_type`          VARCHAR(20)   DEFAULT NULL,
    `segment_values`      JSON          DEFAULT NULL,
    `data_id`             VARCHAR(64)   DEFAULT NULL,
    `allocate_time`       DATETIME      NOT NULL,
    `confirm_time`        DATETIME      DEFAULT NULL,
    `used_time`           DATETIME      DEFAULT NULL,
    `cancel_time`         DATETIME      DEFAULT NULL,
    `recycle_time`        DATETIME      DEFAULT NULL,
    `recycle_lock_time`   DATETIME      DEFAULT NULL,
    `trace_id`            VARCHAR(64)   DEFAULT NULL,
    `tenant_id`           VARCHAR(32)   DEFAULT NULL,
    `version`             INT           NOT NULL DEFAULT 0,
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_code` (`code`),
    KEY `idx_rule_status` (`rule_id`, `status`),
    KEY `idx_recycle` (`rule_id`, `status`, `recycle_lock_time`),
    KEY `idx_expire` (`status`, `is_exposed`, `allocate_time`),
    KEY `idx_data` (`data_id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''code allocation combined table'';

DROP TABLE IF EXISTS `md_code_water_mark`;
CREATE TABLE `md_code_water_mark` (
    `id`                  VARCHAR(32)   NOT NULL,
    `biz_tag`             VARCHAR(128)  NOT NULL,
    `rule_id`             VARCHAR(32)   NOT NULL,
    `current_water`       BIGINT        NOT NULL DEFAULT 0,
    `last_allocate_time`  DATETIME      DEFAULT NULL,
    `last_calibrate_time` DATETIME      DEFAULT NULL,
    `calibrate_source`    VARCHAR(20)   DEFAULT NULL,
    `version`             INT           NOT NULL DEFAULT 0,
    `tenant_id`           VARCHAR(32)   DEFAULT NULL,
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_tag` (`biz_tag`),
    KEY `idx_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''number segment water mark'';

DROP TABLE IF EXISTS `md_model_audit_log`;
CREATE TABLE `md_model_audit_log` (
    `log_id`         VARCHAR(32)  NOT NULL,
    `operator_id`    VARCHAR(32)  NOT NULL,
    `operator_name`  VARCHAR(64)  DEFAULT NULL,
    `operation_type` VARCHAR(32)  NOT NULL,
    `target_id`      VARCHAR(32)  NOT NULL,
    `target_type`    VARCHAR(16)  NOT NULL,
    `before_state`   VARCHAR(32)  DEFAULT NULL,
    `after_state`    VARCHAR(32)  DEFAULT NULL,
    `diff_snapshot`  JSON         DEFAULT NULL,
    `operator_ip`    VARCHAR(64)  DEFAULT NULL,
    `operated_at`    DATETIME     NOT NULL,
    `tenant_id`      VARCHAR(32)  DEFAULT NULL,
    PRIMARY KEY (`log_id`),
    KEY `idx_target` (`target_id`, `target_type`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_time` (`operated_at`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''operation audit log'';

SET FOREIGN_KEY_CHECKS = 1;
