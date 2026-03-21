package com.school.management.domain.message.repository;

import com.school.management.domain.message.model.MsgTemplate;

import java.util.List;
import java.util.Optional;

/**
 * 消息模板仓储接口
 */
public interface MsgTemplateRepository {

    MsgTemplate save(MsgTemplate template);

    Optional<MsgTemplate> findById(Long id);

    List<MsgTemplate> findAll();

    Optional<MsgTemplate> findByCode(String templateCode);

    void deleteById(Long id);
}
