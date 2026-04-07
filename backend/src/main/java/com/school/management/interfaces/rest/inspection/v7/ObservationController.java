package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.school.management.common.util.SnakeToCamelUtil.toCamelCaseList;

/**
 * 评分观察记录 API
 */
@RestController
@RequestMapping("/v7/insp/observations")
@Tag(name = "评分观察记录", description = "检查平台归一化评分观察数据")
@RequiredArgsConstructor
public class ObservationController {

    private final JdbcTemplate jdbc;

    @GetMapping
    @Operation(summary = "查询评分观察记录（分页）")
    @CasbinAccess(resource = "inspection_record", action = "view")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String subjectType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean isNegative,
            @RequestParam(required = false) String linkedEventTypeCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {

        StringBuilder sql = new StringBuilder("SELECT * FROM insp_submission_observations WHERE deleted = 0");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM insp_submission_observations WHERE deleted = 0");
        List<Object> params = new ArrayList<>();

        if (projectId != null) { sql.append(" AND project_id = ?"); countSql.append(" AND project_id = ?"); params.add(projectId); }
        if (subjectType != null) { sql.append(" AND subject_type = ?"); countSql.append(" AND subject_type = ?"); params.add(subjectType); }
        if (severity != null) { sql.append(" AND severity = ?"); countSql.append(" AND severity = ?"); params.add(severity); }
        if (isNegative != null) { sql.append(" AND is_negative = ?"); countSql.append(" AND is_negative = ?"); params.add(isNegative ? 1 : 0); }
        if (linkedEventTypeCode != null) { sql.append(" AND linked_event_type_code = ?"); countSql.append(" AND linked_event_type_code = ?"); params.add(linkedEventTypeCode); }

        Object[] paramArr = params.toArray();
        long total = jdbc.queryForObject(countSql.toString(), Long.class, paramArr);

        sql.append(" ORDER BY observed_at DESC LIMIT ? OFFSET ?");
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add((page - 1) * size);

        List<Map<String, Object>> records = jdbc.queryForList(sql.toString(), pageParams.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", toCamelCaseList(records));
        result.put("total", total);
        result.put("current", page);
        result.put("size", size);
        return Result.success(result);
    }

    @GetMapping("/by-submission/{submissionId}")
    @Operation(summary = "查询指定提交的观察记录")
    @CasbinAccess(resource = "inspection_record", action = "view")
    public Result<List<Map<String, Object>>> bySubmission(@PathVariable Long submissionId) {
        return Result.success(toCamelCaseList(
            jdbc.queryForList("SELECT * FROM insp_submission_observations WHERE submission_id = ? AND deleted = 0 ORDER BY id",
                submissionId)));
    }
}
