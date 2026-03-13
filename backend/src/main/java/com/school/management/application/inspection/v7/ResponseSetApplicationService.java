package com.school.management.application.inspection.v7;

import com.school.management.common.PageResult;
import com.school.management.domain.inspection.model.v7.template.ResponseSet;
import com.school.management.domain.inspection.model.v7.template.ResponseSetOption;
import com.school.management.domain.inspection.repository.v7.ResponseSetOptionRepository;
import com.school.management.domain.inspection.repository.v7.ResponseSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResponseSetApplicationService {

    private final ResponseSetRepository responseSetRepository;
    private final ResponseSetOptionRepository optionRepository;

    @Transactional
    public ResponseSet createResponseSet(String setCode, String setName,
                                          Boolean isGlobal, Long createdBy) {
        ResponseSet rs = ResponseSet.create(setCode, setName, createdBy);
        if (isGlobal != null) {
            rs.update(setName, isGlobal, true, createdBy);
        }
        return responseSetRepository.save(rs);
    }

    @Transactional(readOnly = true)
    public Optional<ResponseSet> getResponseSet(Long id) {
        return responseSetRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public PageResult<ResponseSet> listResponseSets(int page, int size, String keyword) {
        int offset = (page - 1) * size;
        List<ResponseSet> list = responseSetRepository.findPagedWithConditions(offset, size, keyword);
        long total = responseSetRepository.countWithConditions(keyword);
        return PageResult.of(list, total, page, size);
    }

    @Transactional
    public ResponseSet updateResponseSet(Long id, String setName, Boolean isGlobal,
                                          Boolean isEnabled, Long updatedBy) {
        ResponseSet rs = responseSetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("选项集不存在: " + id));
        rs.update(setName, isGlobal, isEnabled, updatedBy);
        return responseSetRepository.save(rs);
    }

    @Transactional
    public void deleteResponseSet(Long id) {
        optionRepository.deleteByResponseSetId(id);
        responseSetRepository.deleteById(id);
    }

    // --- Options ---

    @Transactional
    public ResponseSetOption addOption(Long responseSetId, String optionValue, String optionLabel,
                                        String optionColor, BigDecimal score,
                                        Boolean isFlagged, Integer sortOrder) {
        ResponseSetOption option = ResponseSetOption.create(responseSetId, optionValue, optionLabel, score);
        option.update(optionLabel, optionColor, score, isFlagged, sortOrder);
        return optionRepository.save(option);
    }

    @Transactional(readOnly = true)
    public List<ResponseSetOption> listOptions(Long responseSetId) {
        return optionRepository.findByResponseSetId(responseSetId);
    }

    @Transactional
    public ResponseSetOption updateOption(Long optionId, String optionLabel, String optionColor,
                                           BigDecimal score, Boolean isFlagged, Integer sortOrder) {
        ResponseSetOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("选项不存在: " + optionId));
        option.update(optionLabel, optionColor, score, isFlagged, sortOrder);
        return optionRepository.save(option);
    }

    @Transactional
    public void deleteOption(Long optionId) {
        optionRepository.deleteById(optionId);
    }
}
