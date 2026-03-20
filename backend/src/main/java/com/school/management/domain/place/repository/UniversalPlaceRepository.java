package com.school.management.domain.place.repository;

import com.school.management.domain.place.model.aggregate.UniversalPlace;

import java.util.List;
import java.util.Optional;

/**
 * 通用空间仓储接口
 */
public interface UniversalPlaceRepository {

    /**
     * 保存空间
     */
    UniversalPlace save(UniversalPlace place);

    /**
     * 根据ID查询
     */
    Optional<UniversalPlace> findById(Long id);

    /**
     * 根据空间编码查询
     */
    Optional<UniversalPlace> findByPlaceCode(String placeCode);

    /**
     * 查询所有空间
     */
    List<UniversalPlace> findAll();

    /**
     * 查询所有根空间
     */
    List<UniversalPlace> findAllRoots();

    /**
     * 查询子空间
     */
    List<UniversalPlace> findChildren(Long parentId);

    /**
     * 根据父ID查询（别名，与findChildren相同）
     */
    default List<UniversalPlace> findByParentId(Long parentId) {
        return findChildren(parentId);
    }

    /**
     * 查询所有后代空间
     */
    List<UniversalPlace> findDescendants(Long ancestorId);

    /**
     * 根据路径前缀查询所有后代
     */
    List<UniversalPlace> findByPathPrefix(String pathPrefix);

    /**
     * 根据类型查询
     */
    List<UniversalPlace> findByTypeCode(String typeCode);

    /**
     * 根据组织单元查询
     */
    List<UniversalPlace> findByOrgUnitId(Long orgUnitId);

    /**
     * 根据负责人查询
     */
    List<UniversalPlace> findByResponsibleUserId(Long userId);

    /**
     * 检查空间编码是否存在
     */
    boolean existsByPlaceCode(String placeCode);

    /**
     * 统计同父节点下相同编号的数量
     */
    int countByParentIdAndPlaceCode(Long parentId, String placeCode);

    /**
     * 统计根节点中相同编号的数量
     */
    int countRootByPlaceCode(String placeCode);

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
    List<UniversalPlace> findPage(PlaceQueryCriteria criteria, int page, int size);

    /**
     * 统计符合条件的数量
     */
    long count(PlaceQueryCriteria criteria);

    /**
     * 查询条件
     */
    class PlaceQueryCriteria {
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
