package com.school.management.domain.relation.repository;

import com.school.management.domain.relation.model.entity.UserOrgRelation;

import java.util.List;
import java.util.Optional;

/**
 * 用户-组织关系仓储接口
 */
public interface UserOrgRelationRepository {

    /**
     * 保存关系
     */
    UserOrgRelation save(UserOrgRelation relation);

    /**
     * 根据ID查询
     */
    Optional<UserOrgRelation> findById(Long id);

    /**
     * 查询用户的所有关系
     */
    List<UserOrgRelation> findByUserId(Long userId);

    /**
     * 查询用户的有效关系
     */
    List<UserOrgRelation> findActiveByUserId(Long userId);

    /**
     * 查询用户的主归属
     */
    Optional<UserOrgRelation> findPrimaryByUserId(Long userId);

    /**
     * 查询组织的所有成员关系
     */
    List<UserOrgRelation> findByOrgUnitId(Long orgUnitId);

    /**
     * 查询组织的领导
     */
    List<UserOrgRelation> findLeadersByOrgUnitId(Long orgUnitId);

    /**
     * 查询用户在指定组织的关系
     */
    List<UserOrgRelation> findByUserAndOrg(Long userId, Long orgUnitId);

    /**
     * 清除用户的主归属标记
     */
    void clearPrimaryByUserId(Long userId);

    /**
     * 检查用户是否已有主归属
     */
    boolean existsPrimaryByUserId(Long userId);

    /**
     * 检查关系是否存在
     */
    boolean existsRelation(Long userId, Long orgUnitId, UserOrgRelation.RelationType relationType);

    /**
     * 统计组织成员数
     */
    int countByOrgUnitId(Long orgUnitId);

    /**
     * 统计用户归属数
     */
    int countByUserId(Long userId);

    /**
     * 删除关系
     */
    void deleteById(Long id);

    /**
     * 删除用户的所有关系
     */
    void deleteByUserId(Long userId);
}
