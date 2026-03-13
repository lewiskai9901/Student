package com.school.management.domain.relation.repository;

import com.school.management.domain.relation.model.entity.UserSpaceRelation;

import java.util.List;
import java.util.Optional;

/**
 * 用户-场所关系仓储接口
 */
public interface UserSpaceRelationRepository {

    /**
     * 保存关系
     */
    UserSpaceRelation save(UserSpaceRelation relation);

    /**
     * 根据ID查询
     */
    Optional<UserSpaceRelation> findById(Long id);

    /**
     * 查询用户的所有场所关系
     */
    List<UserSpaceRelation> findByUserId(Long userId);

    /**
     * 查询用户的有效场所关系
     */
    List<UserSpaceRelation> findActiveByUserId(Long userId);

    /**
     * 查询用户的主要场所
     */
    Optional<UserSpaceRelation> findPrimaryByUserId(Long userId);

    /**
     * 查询场所的所有用户关系
     */
    List<UserSpaceRelation> findBySpaceId(Long spaceId);

    /**
     * 查询场所的分配用户
     */
    List<UserSpaceRelation> findAssignedBySpaceId(Long spaceId);

    /**
     * 查询用户在指定场所的关系
     */
    List<UserSpaceRelation> findByUserAndSpace(Long userId, Long spaceId);

    /**
     * 根据位置编码查询
     */
    Optional<UserSpaceRelation> findBySpaceAndPosition(Long spaceId, String positionCode);

    /**
     * 查询用户未缴费的关系
     */
    List<UserSpaceRelation> findUnpaidByUserId(Long userId);

    /**
     * 清除用户的主要场所标记
     */
    void clearPrimaryByUserId(Long userId);

    /**
     * 检查用户是否已有主要场所
     */
    boolean existsPrimaryByUserId(Long userId);

    /**
     * 检查关系是否存在
     */
    boolean existsRelation(Long userId, Long spaceId, UserSpaceRelation.RelationType relationType);

    /**
     * 检查位置是否已被占用
     */
    boolean existsPosition(Long spaceId, String positionCode);

    /**
     * 统计场所的用户数
     */
    int countBySpaceId(Long spaceId);

    /**
     * 统计用户的场所数
     */
    int countByUserId(Long userId);

    /**
     * 统计场所已分配的位置数
     */
    int countAssignedPositions(Long spaceId);

    /**
     * 删除关系
     */
    void deleteById(Long id);

    /**
     * 删除用户的所有场所关系
     */
    void deleteByUserId(Long userId);
}
