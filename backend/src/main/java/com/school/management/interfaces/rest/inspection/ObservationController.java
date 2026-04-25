package com.school.management.interfaces.rest.inspection;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.persistence.inspection.execution.SubmissionObservationMapper;
import com.school.management.infrastructure.persistence.inspection.execution.SubmissionObservationPO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评分观察记录 API
 *
 * 走 SubmissionObservationMapper, 由 @DataPermission(module="inspection_observation")
 * 拦截器自动按角色 DataScope 过滤. 之前直接 JdbcTemplate 拼 SQL 会绕过权限.
 */
@RestController
@RequestMapping("/inspection/observations")
@Tag(name = "评分观察记录", description = "检查平台归一化评分观察数据")
@RequiredArgsConstructor
public class ObservationController {

    private final SubmissionObservationMapper observationMapper;

    @GetMapping
    @Operation(summary = "查询评分观察记录(分页)")
    @CasbinAccess(resource = "inspection_record", action = "view")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String subjectType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean isNegative,
            @RequestParam(required = false) String linkedEventTypeCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {

        Integer isNegativeInt = isNegative == null ? null : (isNegative ? 1 : 0);
        long total = observationMapper.countFiltered(projectId, subjectType, severity, isNegativeInt, linkedEventTypeCode);
        List<SubmissionObservationPO> records = observationMapper.findFiltered(
                projectId, subjectType, severity, isNegativeInt, linkedEventTypeCode,
                size, (page - 1) * size);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", page);
        result.put("size", size);
        return Result.success(result);
    }

    @GetMapping("/by-submission/{submissionId}")
    @Operation(summary = "查询指定提交的观察记录")
    @CasbinAccess(resource = "inspection_record", action = "view")
    public Result<List<SubmissionObservationPO>> bySubmission(@PathVariable Long submissionId) {
        return Result.success(observationMapper.findBySubmissionId(submissionId));
    }
}
