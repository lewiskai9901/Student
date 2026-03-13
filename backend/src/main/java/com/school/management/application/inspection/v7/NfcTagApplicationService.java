package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.platform.NfcTag;
import com.school.management.domain.inspection.repository.v7.NfcTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class NfcTagApplicationService {

    private final NfcTagRepository nfcTagRepository;

    // ========== CRUD ==========

    @Transactional
    public NfcTag register(String tagUid, String locationName, Long placeId, Long orgUnitId) {
        nfcTagRepository.findByTagUid(tagUid).ifPresent(existing -> {
            throw new IllegalArgumentException("NFC标签UID已存在: " + tagUid);
        });
        NfcTag nfcTag = NfcTag.create(tagUid, locationName, placeId, orgUnitId);
        NfcTag saved = nfcTagRepository.save(nfcTag);
        log.info("Registered NFC tag: uid={}, location={}", tagUid, locationName);
        return saved;
    }

    @Transactional
    public NfcTag update(Long id, String locationName, Long placeId, Long orgUnitId) {
        NfcTag nfcTag = nfcTagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("NFC标签不存在: " + id));
        nfcTag.updateLocation(locationName, placeId, orgUnitId);
        NfcTag saved = nfcTagRepository.save(nfcTag);
        log.info("Updated NFC tag: id={}, location={}", id, locationName);
        return saved;
    }

    @Transactional
    public void activate(Long id) {
        NfcTag nfcTag = nfcTagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("NFC标签不存在: " + id));
        nfcTag.activate();
        nfcTagRepository.save(nfcTag);
        log.info("Activated NFC tag: id={}", id);
    }

    @Transactional
    public void deactivate(Long id) {
        NfcTag nfcTag = nfcTagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("NFC标签不存在: " + id));
        nfcTag.deactivate();
        nfcTagRepository.save(nfcTag);
        log.info("Deactivated NFC tag: id={}", id);
    }

    @Transactional(readOnly = true)
    public List<NfcTag> getAll() {
        return nfcTagRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<NfcTag> getActive() {
        return nfcTagRepository.findActive();
    }

    @Transactional(readOnly = true)
    public NfcTag getByTagUid(String tagUid) {
        return nfcTagRepository.findByTagUid(tagUid)
                .orElseThrow(() -> new IllegalArgumentException("NFC标签不存在: uid=" + tagUid));
    }

    @Transactional(readOnly = true)
    public NfcTag getById(Long id) {
        return nfcTagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("NFC标签不存在: " + id));
    }

    @Transactional
    public void delete(Long id) {
        nfcTagRepository.deleteById(id);
        log.info("Deleted NFC tag: id={}", id);
    }
}
