package com.school.management.architecture;

import com.school.management.application.inspection.InspProjectAuthorizationGuard;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Phase G 守护 - 2026-05-23 LEAD 语义化重构:
 *
 * <p>禁止 {@code interfaces/rest/inspection/InspProjectController} 在修改项目设置类
 * 端点 (PUT/PATCH/DELETE) 不经过 {@link InspProjectAuthorizationGuard#assertCanEditSettings}
 * 直接落到 service. 防止以后有人新增 PUT/PATCH 接口忘了加守护从而绕过 LEAD 授权.
 *
 * <p>这个 ratchet 不"扫所有类", 而是只盯 InspProjectController 的关键写方法:
 * 对每个 PUT/PATCH/DELETE 标注的方法, 检查方法内是否引用了 InspProjectAuthorizationGuard
 * (通过 method call 或 field access). 不引用 = fail.
 *
 * <p>豁免列表 {@link #ALLOWED_PUBLIC_WRITE_METHODS}: 不需要 guard 的写方法 (例如
 * createProject 本身不需要守护因为创建本来就要求有创建权限 + 创建后才有 LEAD).
 */
class ArchUnitInspectionRoleAuthorizationGuardTest {

    /** 不需要 LEAD 守护的写方法 — 创建/克隆 (创建动作不依赖既有 LEAD), 生命周期已由 @CasbinAccess + status guard 保护. */
    private static final Set<String> ALLOWED_PUBLIC_WRITE_METHODS = Set.of(
            "createProject",
            "cloneProject",
            "publishProject",
            "upgradeTemplateVersion",
            "pauseProject",
            "resumeProject",
            "completeProject",
            "archiveProject",
            "gradeScore",
            "addInspectorRole",        // Phase B 内部已校验 authGuard
            "removeInspectorRole",     // Phase B 内部已校验 authGuard
            "batchAssignTasks",        // Phase B 内部已校验 authGuard
            "previewTargetCount"       // 只读预览, 不修改任何状态
    );

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.school.management");
    }

    @Test
    void inspProjectControllerWriteMethods_mustCallAuthGuard() {
        String controllerName = "com.school.management.interfaces.rest.inspection.InspProjectController";
        String guardName = InspProjectAuthorizationGuard.class.getName();

        JavaClass controller = classes.get(controllerName);
        Set<String> violations = new TreeSet<>();
        for (JavaMethod method : controller.getMethods()) {
            if (!isWriteEndpoint(method)) continue;
            if (ALLOWED_PUBLIC_WRITE_METHODS.contains(method.getName())) continue;
            boolean callsGuard = method.getMethodCallsFromSelf().stream()
                    .map(JavaMethodCall::getTargetOwner)
                    .anyMatch(t -> t.getName().equals(guardName));
            if (!callsGuard) {
                violations.add(method.getName());
            }
        }

        if (!violations.isEmpty()) {
            fail(String.format(
                "%n以下 InspProjectController 写方法未调用 InspProjectAuthorizationGuard:%n  %s%n"
              + "修复: 在方法体顶部调用 authGuard.assertCanEditSettings(projectId, currentUserId);%n"
              + "如确实不需要守护 (例如内部已有等价校验), 把方法名加到 ALLOWED_PUBLIC_WRITE_METHODS.",
                String.join("\n  ", violations)));
        }
    }

    private boolean isWriteEndpoint(JavaMethod m) {
        // 不能直接判断 HTTP method (@PutMapping 等), ArchUnit 见不到注解层级.
        // 通过方法名启发 + 注解检查: 任何被 @PutMapping/@PatchMapping/@DeleteMapping/@PostMapping 标注的方法都算写入.
        return m.isAnnotatedWith("org.springframework.web.bind.annotation.PutMapping")
            || m.isAnnotatedWith("org.springframework.web.bind.annotation.PatchMapping")
            || m.isAnnotatedWith("org.springframework.web.bind.annotation.DeleteMapping")
            || m.isAnnotatedWith("org.springframework.web.bind.annotation.PostMapping");
    }
}
