package com.meritdata.mdm.codecenter.domain.repository;

import com.meritdata.mdm.codecenter.domain.entity.Model;
import com.meritdata.mdm.codecenter.domain.enums.ModelStatus;
import com.meritdata.mdm.codecenter.domain.enums.ModelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<Model, String> {
    Optional<Model> findByTenantIdAndModelCode(String tenantId, String modelCode);
    Optional<Model> findByTenantIdAndTableName(String tenantId, String tableName);
    List<Model> findByThemeId(String themeId);
    Page<Model> findByTenantIdAndStatus(String tenantId, ModelStatus status, Pageable pageable);
    List<Model> findByTenantIdOrderByCreateTimeDesc(String tenantId);
    Page<Model> findByTenantId(String tenantId, Pageable pageable);

    /**
     * 多条件分页查询（model-index 页面用）
     * keyword: 模糊匹配名称/编码/表名
     * themeIds: 主题域过滤（含子域）
     * status/modelType: 状态/类型过滤
     */
    @Query("SELECT m FROM Model m WHERE " +
            "(:tenantId IS NULL OR m.tenantId = :tenantId) AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "   LOWER(m.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(m.modelCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(m.tableName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:status IS NULL OR m.status = :status) AND " +
            "(:modelType IS NULL OR m.modelType = :modelType) AND " +
            "(:themeIdScope = false OR m.themeId IN :themeIds)")
    Page<Model> search(@Param("tenantId") String tenantId,
                       @Param("keyword") String keyword,
                       @Param("status") ModelStatus status,
                       @Param("modelType") ModelType modelType,
                       @Param("themeIdScope") boolean themeIdScope,
                       @Param("themeIds") List<String> themeIds,
                       Pageable pageable);
}
