package com.school.management.infrastructure.persistence.place;

import com.school.management.infrastructure.access.DataPermission;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守护: {@link UniversalPlaceMapper} 全部 {@code @DataPermission.orgUnitField}
 * 必须是 {@code effective_org_unit_id} (投影列), 不得回退旧列名 {@code org_unit_id}。
 *
 * <p>背景 (V20260612_2 场所归属关系化): 归属真相在 access_relations 的
 * {@code belongs_to|place|org_unit} 覆盖点关系, {@code places.effective_org_unit_id}
 * 是解析后含继承的投影列。数据权限过滤必须走投影列 —— 这同时修复了旧模型下
 * "NULL 继承场所匹配不到 scope 过滤" 的缺陷 (旧 org_unit_id 列 NULL=继承,
 * {@code org_unit_id IN (子树)} 永远排除继承态场所)。
 *
 * <p>另: {@code data_resources} 的 place 行 {@code org_unit_field} 必须保持 NULL
 * (拦截器优先 moduleConfig 列配置, 旧列名残留会压住注解 → "Unknown column" 500,
 * 同 V20260612_1 清洗的事故类别) — 该约束在迁移验证清单中以 SQL 断言, 此处守护注解侧。
 */
class PlaceDataPermissionOrgFieldTest {

    @Test
    void allDataPermissionMethods_useEffectiveProjectionColumn() {
        List<Method> annotated = Arrays.stream(UniversalPlaceMapper.class.getMethods())
            .filter(m -> m.isAnnotationPresent(DataPermission.class))
            .toList();

        assertThat(annotated)
            .as("UniversalPlaceMapper 应有带 @DataPermission 的查询方法")
            .isNotEmpty();

        for (Method m : annotated) {
            DataPermission ann = m.getAnnotation(DataPermission.class);
            assertThat(ann.orgUnitField())
                .as("方法 %s 的 orgUnitField 必须是投影列 effective_org_unit_id (归属真相在 belongs_to 关系)",
                    m.getName())
                .isEqualTo("effective_org_unit_id");
        }
    }
}
