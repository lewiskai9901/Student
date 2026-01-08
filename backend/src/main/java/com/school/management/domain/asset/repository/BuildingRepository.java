package com.school.management.domain.asset.repository;

import com.school.management.domain.asset.model.aggregate.Building;
import com.school.management.domain.asset.model.valueobject.BuildingType;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 楼宇仓储接口
 */
public interface BuildingRepository extends Repository<Building, Long> {

    /**
     * 根据楼号查询
     */
    Optional<Building> findByBuildingNo(String buildingNo);

    /**
     * 根据类型查询
     */
    List<Building> findByType(BuildingType type);

    /**
     * 查询所有启用的楼宇
     */
    List<Building> findAllActive();

    /**
     * 查询所有宿舍楼
     */
    List<Building> findDormitoryBuildings();

    /**
     * 检查楼号是否存在
     */
    boolean existsByBuildingNo(String buildingNo);

    /**
     * 分页查询
     */
    List<Building> findByPage(BuildingQueryCriteria criteria, int pageNum, int pageSize);

    /**
     * 统计符合条件的数量
     */
    long countByCriteria(BuildingQueryCriteria criteria);

    /**
     * 查询条件
     */
    class BuildingQueryCriteria {
        private String keyword;
        private BuildingType buildingType;
        private Integer status;

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public BuildingType getBuildingType() { return buildingType; }
        public void setBuildingType(BuildingType buildingType) { this.buildingType = buildingType; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}
