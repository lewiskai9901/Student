package com.school.management.infrastructure.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * ContributionDispatcher 写入失败策略 (统一锚定 P3 fail-fast)。
 *
 * <p>{@code resource_relations} 是数据权限承重表 (R2.4 删注解兜底后无后备), 单条写失败 =
 * 该资源裸奔。故 dispatcher 对 {@link Contribution.ResourceRelationContribution} 写失败必须
 * fail-fast (抛出令启动失败), 不得沿用其它声明型贡献的「log ERROR 继续」韧性策略
 * (与已有的「跨包 uniqueKey 冲突 fail-fast」同策)。
 */
class ContributionDispatcherTest {

    private static ContributionDispatcher dispatcherWith(
            ResourceRelationUpserter upserter, PluginPackage pkg) {
        return new ContributionDispatcher(
                List.of(pkg),
                mock(MessagingRegistrar.class),
                mock(RelationTypeUpserter.class),
                mock(RoleScopeBindingRegistrar.class),
                mock(DataResourceUpserter.class),
                upserter,
                mock(DataScopeUpserter.class),
                mock(PluginPackageRegistrar.class));
    }

    private static PluginPackage packageContributing(Contribution... contributions) {
        PluginPackage pkg = mock(PluginPackage.class);
        PluginMetadata md = mock(PluginMetadata.class);
        when(md.industryCode()).thenReturn("EDU");
        when(pkg.metadata()).thenReturn(md);
        when(pkg.contribute()).thenReturn(Stream.of(contributions));
        return pkg;
    }

    @Test
    @DisplayName("fail-fast: resource_relations 写入失败 → run() 抛 IllegalStateException (不静默吞掉)")
    void resourceRelationWriteFailure_failsFast() {
        ResourceRelationUpserter failing = mock(ResourceRelationUpserter.class);
        doThrow(new RuntimeException("DB boom"))
                .when(failing).upsert(any(ResourceRelationDef.class), anyString());

        ContributionDispatcher dispatcher = dispatcherWith(failing, packageContributing(
                new Contribution.ResourceRelationContribution("EDU",
                        ResourceRelationDef.column(
                                "inspection_record", "creator", "创建者", "USER", "created_by"))));

        assertThrows(IllegalStateException.class,
                () -> dispatcher.run(mock(ApplicationArguments.class)),
                "resource_relations 写失败必须 fail-fast, 不得静默继续 (锚点缺失=资源裸奔)");
    }
}
