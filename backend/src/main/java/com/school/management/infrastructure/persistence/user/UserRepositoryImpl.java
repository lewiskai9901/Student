package com.school.management.infrastructure.persistence.user;

import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.model.valueobject.UserStatus;
import com.school.management.domain.user.model.valueobject.UserType;
import com.school.management.domain.user.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of UserRepository
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserDomainMapper userMapper;

    public UserRepositoryImpl(UserDomainMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        UserPO po = toPO(user);

        if (user.getId() == null) {
            userMapper.insert(po);
            user.setId(po.getId());
        } else {
            userMapper.updateById(po);
        }

        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        UserPO po = userMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        UserPO po = userMapper.findByUsername(username);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        UserPO po = userMapper.findByPhone(phone);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        UserPO po = userMapper.findByEmail(email);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public Optional<User> findByEmployeeNo(String employeeNo) {
        UserPO po = userMapper.findByEmployeeNo(employeeNo);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public Optional<User> findByWechatOpenid(String openid) {
        UserPO po = userMapper.findByWechatOpenid(openid);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }

    @Override
    public boolean existsByUsernameAndIdNot(String username, Long excludeId) {
        return userMapper.existsByUsernameAndIdNot(username, excludeId);
    }

    @Override
    public List<User> findByDepartmentId(Long departmentId) {
        return userMapper.findByDepartmentId(departmentId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByDepartmentIdIn(List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userMapper.findByDepartmentIdIn(departmentIds).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            userMapper.deleteByIds(ids);
        }
    }

    @Override
    public List<User> findAll(int page, int size) {
        int offset = (page - 1) * size;
        return userMapper.findAllPaged(offset, size).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return userMapper.countAll();
    }

    @Override
    public List<User> findPagedWithConditions(int page, int size, String username, String realName,
                                              String phone, Long departmentId, Integer status) {
        int offset = (page - 1) * size;
        return userMapper.findPagedWithConditions(offset, size, username, realName, phone, departmentId, status)
                .stream()
                .map(this::toDomainWithDepartment)
                .collect(Collectors.toList());
    }

    @Override
    public long countWithConditions(String username, String realName, String phone,
                                    Long departmentId, Integer status) {
        return userMapper.countWithConditions(username, realName, phone, departmentId, status);
    }

    @Override
    public List<User> findSimpleList(String keyword) {
        return userMapper.findSimpleList(keyword).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findAllUsers() {
        return userMapper.findAllPaged(0, 10000).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ==================== Mapping Methods ====================

    private UserPO toPO(User domain) {
        UserPO po = new UserPO();
        po.setId(domain.getId());
        po.setUsername(domain.getUsername());
        po.setPassword(domain.getPassword());
        po.setRealName(domain.getRealName());
        po.setPhone(domain.getPhone());
        po.setEmail(domain.getEmail());
        po.setAvatar(domain.getAvatar());
        po.setEmployeeNo(domain.getEmployeeNo());
        po.setGender(domain.getGender());
        po.setBirthDate(domain.getBirthDate());
        po.setIdCard(domain.getIdCard());
        po.setDepartmentId(domain.getDepartmentId());
        po.setClassId(domain.getClassId());
        po.setManagedClassId(domain.getManagedClassId());
        po.setUserType(domain.getUserType() != null ? domain.getUserType().getCode() : null);
        po.setStatus(domain.getStatus() != null ? domain.getStatus().getCode() : null);
        po.setLastLoginTime(domain.getLastLoginTime());
        po.setLastLoginIp(domain.getLastLoginIp());
        po.setPasswordChangedAt(domain.getPasswordChangedAt());
        po.setWechatOpenid(domain.getWechatOpenid());
        po.setAllowMultipleDevices(domain.isAllowMultipleDevices() ? 1 : 0);
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private User toDomain(UserPO po) {
        return User.reconstruct(
                po.getId(),
                po.getUsername(),
                po.getPassword(),
                po.getRealName(),
                po.getPhone(),
                po.getEmail(),
                po.getAvatar(),
                po.getEmployeeNo(),
                po.getGender(),
                po.getBirthDate(),
                po.getIdCard(),
                po.getDepartmentId(),
                po.getClassId(),
                po.getManagedClassId(),
                po.getUserType() != null ? UserType.fromCode(po.getUserType()) : null,
                po.getStatus() != null ? UserStatus.fromCode(po.getStatus()) : null,
                po.getLastLoginTime(),
                po.getLastLoginIp(),
                po.getPasswordChangedAt(),
                po.getWechatOpenid(),
                po.getAllowMultipleDevices() != null && po.getAllowMultipleDevices() == 1,
                new ArrayList<>(), // Role IDs loaded separately
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    private User toDomainWithDepartment(UserPO po) {
        User user = toDomain(po);
        // 设置部门名称（从JOIN查询获取）
        if (po.getDepartmentName() != null) {
            user.setDepartmentName(po.getDepartmentName());
        }
        return user;
    }
}
