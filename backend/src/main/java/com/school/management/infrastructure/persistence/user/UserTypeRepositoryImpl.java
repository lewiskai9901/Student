package com.school.management.infrastructure.persistence.user;

import com.school.management.domain.user.model.entity.UserType;
import com.school.management.domain.user.repository.UserTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户类型仓储实现
 */
@Repository
public class UserTypeRepositoryImpl implements UserTypeRepository {

    private final UserTypeMapper userTypeMapper;

    public UserTypeRepositoryImpl(UserTypeMapper userTypeMapper) {
        this.userTypeMapper = userTypeMapper;
    }

    @Override
    public UserType save(UserType userType) {
        UserTypePO po = toPO(userType);
        if (po.getId() == null) {
            userTypeMapper.insert(po);
        } else {
            userTypeMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<UserType> findById(Long id) {
        UserTypePO po = userTypeMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<UserType> findByTypeCode(String typeCode) {
        UserTypePO po = userTypeMapper.findByTypeCode(typeCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UserType> findAll() {
        return userTypeMapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserType> findAllEnabled() {
        return userTypeMapper.findAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserType> findByParentTypeCode(String parentTypeCode) {
        return userTypeMapper.findByParentTypeCode(parentTypeCode).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserType> findTopLevelTypes() {
        return userTypeMapper.findTopLevelTypes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserType> findLoginableTypes() {
        return userTypeMapper.findLoginableTypes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserType> findInspectorTypes() {
        return userTypeMapper.findInspectorTypes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserType> findInspectableTypes() {
        return userTypeMapper.findInspectableTypes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserType> findClassRequiredTypes() {
        return userTypeMapper.findClassRequiredTypes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTypeCode(String typeCode) {
        return userTypeMapper.existsByTypeCode(typeCode);
    }

    @Override
    public boolean isTypeInUse(String typeCode) {
        try {
            return userTypeMapper.countUsersByTypeCode(typeCode) > 0;
        } catch (Exception e) {
            // 用户表可能字段不存在，返回false
            return false;
        }
    }

    @Override
    public void deleteById(Long id) {
        userTypeMapper.deleteById(id);
    }

    // ==================== 转换方法 ====================

    private UserTypePO toPO(UserType entity) {
        UserTypePO po = new UserTypePO();
        po.setId(entity.getId());
        po.setTypeCode(entity.getTypeCode());
        po.setTypeName(entity.getTypeName());
        po.setParentTypeCode(entity.getParentTypeCode());
        po.setLevelOrder(entity.getLevelOrder());
        po.setIcon(entity.getIcon());
        po.setColor(entity.getColor());
        po.setDescription(entity.getDescription());
        po.setCanLogin(entity.isCanLogin());
        po.setCanBeInspector(entity.isCanBeInspector());
        po.setCanBeInspected(entity.isCanBeInspected());
        po.setCanManageOrg(entity.isCanManageOrg());
        po.setCanViewReports(entity.isCanViewReports());
        po.setRequiresClass(entity.isRequiresClass());
        po.setRequiresDormitory(entity.isRequiresDormitory());
        po.setDefaultRoleCodes(entity.getDefaultRoleCodes());
        po.setIsSystem(entity.isSystem());
        po.setIsEnabled(entity.isEnabled());
        po.setSortOrder(entity.getSortOrder());
        return po;
    }

    private UserType toDomain(UserTypePO po) {
        return UserType.builder()
                .id(po.getId())
                .typeCode(po.getTypeCode())
                .typeName(po.getTypeName())
                .parentTypeCode(po.getParentTypeCode())
                .levelOrder(po.getLevelOrder())
                .icon(po.getIcon())
                .color(po.getColor())
                .description(po.getDescription())
                .canLogin(Boolean.TRUE.equals(po.getCanLogin()))
                .canBeInspector(Boolean.TRUE.equals(po.getCanBeInspector()))
                .canBeInspected(Boolean.TRUE.equals(po.getCanBeInspected()))
                .canManageOrg(Boolean.TRUE.equals(po.getCanManageOrg()))
                .canViewReports(Boolean.TRUE.equals(po.getCanViewReports()))
                .requiresClass(Boolean.TRUE.equals(po.getRequiresClass()))
                .requiresDormitory(Boolean.TRUE.equals(po.getRequiresDormitory()))
                .defaultRoleCodes(po.getDefaultRoleCodes())
                .isSystem(Boolean.TRUE.equals(po.getIsSystem()))
                .isEnabled(Boolean.TRUE.equals(po.getIsEnabled()))
                .sortOrder(po.getSortOrder())
                .build();
    }
}
