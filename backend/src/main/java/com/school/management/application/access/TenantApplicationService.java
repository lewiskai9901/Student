package com.school.management.application.access;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.infrastructure.persistence.tenant.TenantMapper;
import com.school.management.infrastructure.persistence.tenant.TenantPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service for tenant management.
 */
@Service
@RequiredArgsConstructor
public class TenantApplicationService {

    private final TenantMapper tenantMapper;

    public List<TenantPO> list() {
        return tenantMapper.selectList(
            new LambdaQueryWrapper<TenantPO>().orderByAsc(TenantPO::getId));
    }

    public TenantPO getById(Long id) {
        return tenantMapper.selectById(id);
    }

    public TenantPO create(TenantPO tenant) {
        tenantMapper.insert(tenant);
        return tenant;
    }

    public TenantPO update(TenantPO tenant) {
        tenantMapper.updateById(tenant);
        return tenant;
    }

    public void delete(Long id) {
        tenantMapper.deleteById(id);
    }
}
