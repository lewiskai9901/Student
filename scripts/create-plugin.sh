#!/usr/bin/env bash
# create-plugin.sh — 行业插件脚手架
#
# 用法:
#   ./scripts/create-plugin.sh HEALTH "医疗行业" "CORE"
#   参数:
#     $1 = industry code (大写,如 HEALTH)
#     $2 = 中文名
#     $3 = 依赖的行业包(逗号分隔,如 "CORE" 或 "CORE,EDU"),默认 CORE
#
# 生成:
#   backend/src/main/java/.../plugins/{lowercase}/
#     ├── {Industry}Manifest.java
#     ├── {Industry}PermissionProvider.java
#     ├── {Industry}RolePresetPlugin.java
#     ├── {Industry}RelationsPlugin.java
#     ├── {Industry}MenuPlugin.java
#     └── constants/{Industry}Permissions.java

set -euo pipefail

INDUSTRY_CODE="${1:?需要 industry code, 如 HEALTH}"
INDUSTRY_NAME="${2:?需要中文名, 如 医疗行业}"
DEPENDS_ON_RAW="${3:-CORE}"

# 大小写转换
INDUSTRY_LOWER=$(echo "$INDUSTRY_CODE" | tr '[:upper:]' '[:lower:]')
INDUSTRY_PASCAL=$(echo "$INDUSTRY_CODE" | sed 's/\(.\)\(.*\)/\1\2/' | awk '{ print toupper(substr($0,1,1)) tolower(substr($0,2)) }')

# 路径
BACKEND_BASE="backend/src/main/java/com/school/management/infrastructure/extension/plugins/$INDUSTRY_LOWER"
CONSTANTS_DIR="$BACKEND_BASE/constants"
PACKAGE="com.school.management.infrastructure.extension.plugins.$INDUSTRY_LOWER"

# 依赖列表(Java List.of() 形式)
DEPENDS_LIST=$(echo "$DEPENDS_ON_RAW" | sed 's/,/", "/g')

echo "┌─ create-plugin scaffolding ──────────────"
echo "│ Industry code:  $INDUSTRY_CODE"
echo "│ Industry name:  $INDUSTRY_NAME"
echo "│ Pascal class:   $INDUSTRY_PASCAL"
echo "│ Package:        $PACKAGE"
echo "│ Depends on:     $DEPENDS_ON_RAW"
echo "└──────────────────────────────────────────"

# 创建目录
mkdir -p "$BACKEND_BASE"
mkdir -p "$CONSTANTS_DIR"

# ═══════════ 1. Manifest ═══════════
cat > "$BACKEND_BASE/${INDUSTRY_PASCAL}Manifest.java" <<EOF
package $PACKAGE;

import com.school.management.infrastructure.extension.PluginManifest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * $INDUSTRY_NAME 插件包 Manifest.
 *
 * 生成方式: scripts/create-plugin.sh $INDUSTRY_CODE "$INDUSTRY_NAME" $DEPENDS_ON_RAW
 */
@Component
public class ${INDUSTRY_PASCAL}Manifest implements PluginManifest {

    @Override public String getIndustryCode() { return "$INDUSTRY_CODE"; }
    @Override public String getIndustryName() { return "$INDUSTRY_NAME"; }
    @Override public String getVersion() { return "1.0.0"; }

    @Override
    public List<String> getDependsOn() {
        return List.of("$DEPENDS_LIST");
    }

    @Override
    public Map<String, String> getDependsOnWithVersion() {
        return Map.of("CORE", ">=1.0.0 <2.0.0");
    }

    @Override
    public boolean owns(Class<?> pluginClass) {
        return pluginClass.getPackageName().contains(".plugins.$INDUSTRY_LOWER");
    }
}
EOF

# ═══════════ 2. PermissionProvider ═══════════
cat > "$BACKEND_BASE/${INDUSTRY_PASCAL}PermissionProvider.java" <<EOF
package $PACKAGE;

import com.school.management.infrastructure.extension.PermissionProvider;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.PermissionProvider.PermissionDef.of;

/**
 * $INDUSTRY_NAME 权限声明.
 */
@Component
public class ${INDUSTRY_PASCAL}PermissionProvider implements PermissionProvider {

    @Override public String getModuleCode() { return "$INDUSTRY_LOWER"; }
    @Override public String getModuleName() { return "$INDUSTRY_NAME"; }

    @Override
    public List<PermissionDef> getPermissions() {
        return List.of(
            // TODO: 在此声明本行业权限, 如:
            // of("${INDUSTRY_LOWER}:patient:view", "查看患者", "")
        );
    }
}
EOF

# ═══════════ 3. RolePresetPlugin ═══════════
cat > "$BACKEND_BASE/${INDUSTRY_PASCAL}RolePresetPlugin.java" <<EOF
package $PACKAGE;

import com.school.management.infrastructure.extension.RolePresetPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * $INDUSTRY_NAME 预置角色.
 */
@Component
public class ${INDUSTRY_PASCAL}RolePresetPlugin implements RolePresetPlugin {

    @Override
    public List<RolePresetDef> getPresets() {
        return List.of(
            // TODO: 声明本行业预置角色, 如:
            // RolePresetDef.of("DOCTOR", "医生", "临床医生角色", 20)
        );
    }
}
EOF

# ═══════════ 4. RelationsPlugin ═══════════
cat > "$BACKEND_BASE/${INDUSTRY_PASCAL}RelationsPlugin.java" <<EOF
package $PACKAGE;

import com.school.management.infrastructure.extension.RelationTypePlugin;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * $INDUSTRY_NAME 业务关系类型.
 */
@Component
public class ${INDUSTRY_PASCAL}RelationsPlugin implements RelationTypePlugin {

    @Override public String getSourceName() { return "${INDUSTRY_PASCAL}Plugin"; }
    @Override public String getTier() { return "DOMAIN"; }

    @Override
    public List<RelationTypeDef> getRelationTypes() {
        return List.of(
            // TODO: 声明本行业关系, 如:
            // RelationTypeDef.of("treats", "user", "user", "诊治",
            //     "ASSOCIATION", "医生诊治患者")
        );
    }
}
EOF

# ═══════════ 5. MenuPlugin ═══════════
cat > "$BACKEND_BASE/${INDUSTRY_PASCAL}MenuPlugin.java" <<EOF
package $PACKAGE;

import com.school.management.infrastructure.extension.MenuContributionPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.MenuContributionPlugin.MenuItemDef.of;

/**
 * $INDUSTRY_NAME 菜单贡献.
 */
@Component
public class ${INDUSTRY_PASCAL}MenuPlugin implements MenuContributionPlugin {

    @Override public String getDomainCode() { return "$INDUSTRY_LOWER"; }

    @Override
    public List<MenuItemDef> getMenus() {
        return List.of(
            // TODO: 声明本行业菜单, 如:
            // of("/patient", "患者管理", "heart-pulse", 5).children(List.of(
            //     of("/patient/list", "患者列表", "users", 1)
            // ))
        );
    }
}
EOF

# ═══════════ 6. Constants/Permissions ═══════════
cat > "$CONSTANTS_DIR/${INDUSTRY_PASCAL}Permissions.java" <<EOF
package $PACKAGE.constants;

/**
 * $INDUSTRY_NAME 权限码常量.
 * 业务代码通过这些常量引用权限, 禁用字符串字面量.
 */
public final class ${INDUSTRY_PASCAL}Permissions {
    private ${INDUSTRY_PASCAL}Permissions() {}

    // TODO: 添加权限码常量, 如:
    // public static final String PATIENT_VIEW = "${INDUSTRY_LOWER}:patient:view";
}
EOF

echo ""
echo "✓ 已生成 5+1 个骨架文件到 $BACKEND_BASE/"
ls -1 "$BACKEND_BASE/"
echo ""
echo "下一步:"
echo "  1. 在 getPermissions() / getPresets() / getRelationTypes() 里填声明"
echo "  2. 可选: 添加 EntityTypePlugin (用户/组织/场所类型) 单独建文件"
echo "  3. 可选: 添加 MessagingDomainPlugin (触发点+事件类型)"
echo "  4. mvn test -Dtest=PluginDeclarationCoverageTest 验证声明覆盖"
echo "  5. mvn spring-boot:run 启动 — 观察日志中本行业包加载"
echo ""
