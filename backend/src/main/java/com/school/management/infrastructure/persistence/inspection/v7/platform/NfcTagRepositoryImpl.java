package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.school.management.domain.inspection.model.v7.platform.NfcTag;
import com.school.management.domain.inspection.repository.v7.NfcTagRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class NfcTagRepositoryImpl implements NfcTagRepository {

    private final NfcTagMapper mapper;

    public NfcTagRepositoryImpl(NfcTagMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public NfcTag save(NfcTag nfcTag) {
        NfcTagPO po = toPO(nfcTag);
        if (nfcTag.getId() == null) {
            mapper.insert(po);
            nfcTag.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return nfcTag;
    }

    @Override
    public Optional<NfcTag> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<NfcTag> findByTagUid(String tagUid) {
        return Optional.ofNullable(mapper.findByTagUid(tagUid)).map(this::toDomain);
    }

    @Override
    public List<NfcTag> findAll() {
        return mapper.selectList(null).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<NfcTag> findActive() {
        return mapper.findActive().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private NfcTagPO toPO(NfcTag d) {
        NfcTagPO po = new NfcTagPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setTagUid(d.getTagUid());
        po.setLocationName(d.getLocationName());
        po.setPlaceId(d.getPlaceId());
        po.setOrgUnitId(d.getOrgUnitId());
        po.setIsActive(d.getIsActive());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private NfcTag toDomain(NfcTagPO po) {
        return NfcTag.reconstruct(NfcTag.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .tagUid(po.getTagUid())
                .locationName(po.getLocationName())
                .placeId(po.getPlaceId())
                .orgUnitId(po.getOrgUnitId())
                .isActive(po.getIsActive())
                .createdAt(po.getCreatedAt()));
    }
}
