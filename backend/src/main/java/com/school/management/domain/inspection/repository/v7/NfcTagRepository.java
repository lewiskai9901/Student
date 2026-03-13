package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.platform.NfcTag;

import java.util.List;
import java.util.Optional;

public interface NfcTagRepository {

    NfcTag save(NfcTag nfcTag);

    Optional<NfcTag> findById(Long id);

    Optional<NfcTag> findByTagUid(String tagUid);

    List<NfcTag> findAll();

    List<NfcTag> findActive();

    void deleteById(Long id);
}
