package com.school.management.infrastructure.access;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RecordRelationRepositoryImpl SQL 单测 (统一锚定 R4 地基)。
 *
 * <p>mock JdbcTemplate, 捕获 SQL 断言 WHERE 子句与参数 —— 守 record_relations 读/删 SQL 不漂移。
 * 真库行为 (save upsert / KeyHolder / mapRow) 由真启动 + 真库 SQL round-trip 验证 (该 test 模块
 * 无 Spring 集成 harness, 同 AccessRelationRepositoryImplIT 的 @Disabled 现状)。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecordRelationRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("findRecordIdsBySubject: 按 resource+relation+subject+tenant 过滤, 限 deleted=0 + 有效期")
    void findRecordIdsBySubject_filtersCorrectly() {
        RecordRelationRepositoryImpl repo = new RecordRelationRepositoryImpl(jdbc);
        when(jdbc.queryForList(anyString(), eq(Long.class), any(), any(), any(), any(), any()))
                .thenReturn(List.of(7L, 8L));

        List<Long> ids = repo.findRecordIdsBySubject("inspection_record", "reviewer", "USER", 9L, 1L);

        assertThat(ids).containsExactly(7L, 8L);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), eq(Long.class), any(), any(), any(), any(), any());
        assertThat(sql.getValue())
                .contains("SELECT record_id FROM record_relations")
                .contains("resource_code=?").contains("relation_code=?")
                .contains("subject_type=?").contains("subject_id=?").contains("tenant_id=?")
                .contains("deleted=0")
                .contains("valid_to IS NULL OR valid_to > NOW()");
    }

    @Test
    @DisplayName("softDelete: UPDATE deleted=1, 按全键定位, 限 deleted=0")
    void softDelete_narrowsToTuple() {
        RecordRelationRepositoryImpl repo = new RecordRelationRepositoryImpl(jdbc);
        when(jdbc.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        int affected = repo.softDelete("inspection_record", 100L, "reviewer", "USER", 9L, 1L);

        assertThat(affected).isEqualTo(1);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), any(), any(), any(), any(), any(), any());
        assertThat(sql.getValue())
                .contains("UPDATE record_relations SET deleted=1")
                .contains("resource_code=?").contains("record_id=?").contains("relation_code=?")
                .contains("subject_type=?").contains("subject_id=?").contains("tenant_id=?")
                .contains("deleted=0");
    }
}
