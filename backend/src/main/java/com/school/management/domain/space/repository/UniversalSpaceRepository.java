package com.school.management.domain.space.repository;

import com.school.management.domain.space.model.aggregate.UniversalSpace;

import java.util.List;
import java.util.Optional;

/**
 * 通用空间仓储接口
 */
public interface UniversalSpaceRepository {

    /**
     * 保存空间
     */
    UniversalSpace save(UniversalSpace space);

    /**
     * 根据ID查询
     */
    Optional<UniversalSpace> findById(Long id);

    /**
     * 根据空间编码查询
     */
    Optional<UniversalSpace> findBySpaceCode(String spaceCode);

    /**
     * 查询所有空间
     */
    List<UniversalSpace> findAll();

    /**
     * 查询所有根空间
     */
    List<UniversalSpace> findAllRoots();

    /**
     * 查询子空间
     */
    List<UniversalSpace> findChildren(Long parentId);

    /**
     * 查询所有后代空间
     */
    List<UniversalSpace> findDescendants(Long ancestorId);

    /**
     * 根据路径前缀查询所有后代
     */
    List<UniversalSpace> findByPathPrefix(String pathPrefix);

    /**
     * 根据类型查询
     */
    List<UniversalSpace> findByTypeCode(String typeCode);

    /**
     * 根据组织单元查询
     */
    List<UniversalSpace> findByOrgUnitId(Long orgUnitId);

    /**
     * 根据负责人查询
     */
    List<UniversalSpace> findByResponsibleUserId(Long userId);

    /**
     * 检查空间编码是否存在
     */
    boolean existsBySpaceCode(String spaceCode);

    /**
     * 根据ID删除
     */
    void deleteById(Long id);

    /**
     * 批量删除
     */
    void deleteByIds(List<Long> ids);

    /**
     * 统计子空间数量
     */
    int countChildren(Long parentId);

    /**
     * 统计后代空间数量
     */
    int countDescendants(Long ancestorId);

    /**
     * 分页查询
     */
    List<UniversalSpace> findPage(SpaceQueryCriteria criteria, int page, int size);

    /**
     * 统计符合条件的数量
     */
    long count(SpaceQueryCriteria criteria);

    /**
     * 查询条件
     */
    class SpaceQueryCriteria {
        private String typeCode;
        private Long parentId;
        private Long orgUnitId;
        private Integer status;
        private String keyword;

        // Getters and Setters
        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public Long getOrgUnitId() { return orgUnitId; }
        public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
    }
}
