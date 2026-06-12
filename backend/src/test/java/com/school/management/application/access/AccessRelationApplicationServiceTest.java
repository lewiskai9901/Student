package com.school.management.application.access;

import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.model.valueobject.AccessLevel;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.repository.UserRepository;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.PolicyRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessRelationApplicationService 单元测试")
class AccessRelationApplicationServiceTest {

    @Mock
    private AccessRelationRepository accessRelationRepository;
    @Mock
    private UniversalPlaceRepository placeRepository;
    @Mock
    private OrgUnitRepository orgUnitRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PolicyRegistry policyRegistry;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AccessRelationApplicationService service;

    @Captor
    private ArgumentCaptor<AccessRelation> relationCaptor;

    private AccessRelation rel(Long id, String resourceType, Long resourceId,
                               String subjectType, Long subjectId, String relation) {
        return AccessRelation.builder()
                .id(id)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .subjectType(subjectType)
                .subjectId(subjectId)
                .relation(relation)
                .build();
    }

    @Nested
    @DisplayName("findByResource")
    class FindByResource {

        @Test
        @DisplayName("返回仓储结果并填充 place 名称到 metadata")
        void enrichesPlaceMetadata() {
            AccessRelation r = rel(1L, "place", 100L, "user", 9L, "user");
            when(accessRelationRepository.findByResource("place", 100L)).thenReturn(List.of(r));
            UniversalPlace place = org.mockito.Mockito.mock(UniversalPlace.class);
            when(place.getPlaceName()).thenReturn("教室A");
            when(place.getPlaceCode()).thenReturn("R-A");
            when(placeRepository.findById(100L)).thenReturn(Optional.of(place));
            User user = org.mockito.Mockito.mock(User.class);
            when(user.getRealName()).thenReturn("张三");
            when(user.getUsername()).thenReturn("zhangsan");
            when(userRepository.findById(9L)).thenReturn(Optional.of(user));

            List<AccessRelation> result = service.findByResource("place", 100L);

            assertThat(result).hasSize(1);
            Map<String, Object> meta = result.get(0).getMetadata();
            assertThat(meta).containsEntry("placeName", "教室A")
                    .containsEntry("placeCode", "R-A")
                    .containsEntry("subjectName", "张三");
        }

        @Test
        @DisplayName("空列表直接返回, 不触发仓储查询")
        void emptyListShortCircuits() {
            when(accessRelationRepository.findByResource("place", 1L)).thenReturn(List.of());

            List<AccessRelation> result = service.findByResource("place", 1L);

            assertThat(result).isEmpty();
            verify(placeRepository, never()).findById(anyLong());
        }
    }

    @Nested
    @DisplayName("findBySubject")
    class FindBySubject {

        @Test
        @DisplayName("填充 org_unit 资源名称")
        void enrichesOrgUnitResource() {
            AccessRelation r = rel(2L, "org_unit", 200L, "user", 7L, "manages");
            when(accessRelationRepository.findBySubject("user", 7L)).thenReturn(List.of(r));
            OrgUnit orgUnit = org.mockito.Mockito.mock(OrgUnit.class);
            when(orgUnit.getUnitName()).thenReturn("信息学院");
            when(orgUnitRepository.findById(200L)).thenReturn(Optional.of(orgUnit));
            User user = org.mockito.Mockito.mock(User.class);
            when(user.getRealName()).thenReturn("李四");
            when(userRepository.findById(7L)).thenReturn(Optional.of(user));

            List<AccessRelation> result = service.findBySubject("user", 7L);

            assertThat(result.get(0).getMetadata()).containsEntry("orgUnitName", "信息学院");
        }

        @Test
        @DisplayName("填充元数据抛异常被吞掉, 不影响返回")
        void swallowsEnrichException() {
            AccessRelation r = rel(3L, "place", 300L, "user", 5L, "user");
            when(accessRelationRepository.findBySubject("user", 5L)).thenReturn(List.of(r));
            when(placeRepository.findById(300L)).thenThrow(new RuntimeException("db down"));
            lenient().when(userRepository.findById(5L)).thenReturn(Optional.empty());

            List<AccessRelation> result = service.findBySubject("user", 5L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findBySubjectAndResourceType")
    class FindBySubjectAndResourceType {

        @Test
        @DisplayName("委托仓储并填充 user subject 名称")
        void enrichesUserSubject() {
            AccessRelation r = rel(4L, "org_unit", 400L, "place", 800L, "responsible");
            when(accessRelationRepository.findBySubjectAndResourceType("place", 800L, "org_unit"))
                    .thenReturn(List.of(r));
            OrgUnit orgUnit = org.mockito.Mockito.mock(OrgUnit.class);
            when(orgUnit.getUnitName()).thenReturn("后勤处");
            when(orgUnitRepository.findById(400L)).thenReturn(Optional.of(orgUnit));
            UniversalPlace place = org.mockito.Mockito.mock(UniversalPlace.class);
            when(place.getPlaceName()).thenReturn("仓库");
            when(placeRepository.findById(800L)).thenReturn(Optional.of(place));

            List<AccessRelation> result =
                    service.findBySubjectAndResourceType("place", 800L, "org_unit");

            assertThat(result.get(0).getMetadata()).containsEntry("subjectName", "仓库");
        }
    }

    @Nested
    @DisplayName("resolveOrgUnitIds")
    class ResolveOrgUnitIds {

        @Test
        @DisplayName("userId 为 null 返回空列表")
        void nullUserId() {
            assertThat(service.resolveOrgUnitIds(null)).isEmpty();
            verify(accessRelationRepository, never())
                    .findBySubjectAndResourceType(anyString(), anyLong(), anyString());
        }

        @Test
        @DisplayName("仅取 MEMBER_OF/OWNER_OF/MANAGES 关系的 resourceId")
        void filtersRelevantRelations() {
            AccessRelation member = rel(1L, "ORG_UNIT", 10L, "USER", 1L, "member_of");
            AccessRelation owner = rel(2L, "ORG_UNIT", 20L, "USER", 1L, "OWNER_OF");
            AccessRelation manages = rel(3L, "ORG_UNIT", 30L, "USER", 1L, "Manages");
            AccessRelation viewer = rel(4L, "ORG_UNIT", 40L, "USER", 1L, "viewer");
            when(accessRelationRepository.findBySubjectAndResourceType("USER", 1L, "ORG_UNIT"))
                    .thenReturn(List.of(member, owner, manages, viewer));

            List<Long> ids = service.resolveOrgUnitIds(1L);

            assertThat(ids).containsExactly(10L, 20L, 30L);
        }

        @Test
        @DisplayName("无匹配关系返回空列表")
        void noMatches() {
            when(accessRelationRepository.findBySubjectAndResourceType("USER", 2L, "ORG_UNIT"))
                    .thenReturn(List.of(rel(1L, "ORG_UNIT", 1L, "USER", 2L, "viewer")));

            assertThat(service.resolveOrgUnitIds(2L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("checkAccess")
    class CheckAccess {

        @Test
        @DisplayName("委托仓储 exists 返回 true")
        void delegatesTrue() {
            when(accessRelationRepository.exists("place", 1L, "user", "user", 2L)).thenReturn(true);

            assertThat(service.checkAccess("place", 1L, "user", "user", 2L)).isTrue();
        }

        @Test
        @DisplayName("委托仓储 exists 返回 false")
        void delegatesFalse() {
            when(accessRelationRepository.exists("place", 1L, "user", "user", 2L)).thenReturn(false);

            assertThat(service.checkAccess("place", 1L, "user", "user", 2L)).isFalse();
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        private AccessRelationApplicationService.CreateCommand cmd() {
            AccessRelationApplicationService.CreateCommand c =
                    new AccessRelationApplicationService.CreateCommand();
            c.setResourceType("place");
            c.setResourceId(100L);
            c.setRelation("user");
            c.setSubjectType("user");
            c.setSubjectId(9L);
            c.setIncludeChildren(true);
            c.setAccessLevel(AccessLevel.READ_ONLY);
            c.setRemark("备注");
            return c;
        }

        @Test
        @DisplayName("先 enforce BEFORE_GRANT, save 后 check AFTER_GRANT")
        void happyPath() {
            AccessRelationApplicationService.CreateCommand c = cmd();
            AccessRelation saved = rel(55L, "place", 100L, "user", 9L, "user");
            when(accessRelationRepository.save(any(AccessRelation.class))).thenReturn(saved);
            when(policyRegistry.check(any(PolicyContext.class))).thenReturn(List.of());

            AccessRelation result = service.create(c);

            assertThat(result).isSameAs(saved);
            verify(policyRegistry).enforce(any(PolicyContext.class));
            verify(policyRegistry).check(any(PolicyContext.class));
            verify(accessRelationRepository).save(relationCaptor.capture());
            AccessRelation built = relationCaptor.getValue();
            assertThat(built.getResourceType()).isEqualTo("place");
            assertThat(built.getResourceId()).isEqualTo(100L);
            assertThat(built.getRelation()).isEqualTo("user");
            assertThat(built.getAccessLevel()).isEqualTo(AccessLevel.READ_ONLY);
            assertThat(built.isIncludeChildren()).isTrue();
        }

        @Test
        @DisplayName("BEFORE_GRANT 策略阻断时不执行 save")
        void blockedByPolicy() {
            org.mockito.Mockito.doThrow(new RuntimeException("blocked"))
                    .when(policyRegistry).enforce(any(PolicyContext.class));

            assertThatThrownBy(() -> service.create(cmd()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("blocked");
            verify(accessRelationRepository, never()).save(any(AccessRelation.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("找不到关系时抛 IllegalArgumentException")
        void notFound() {
            when(accessRelationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L,
                    new AccessRelationApplicationService.UpdateCommand()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("关系不存在");
        }

        @Test
        @DisplayName("仅更新非 null 字段并合并 metadata")
        void updatesNonNullFields() {
            AccessRelation existing = rel(5L, "place", 1L, "user", 2L, "user");
            existing.setMetadata(new HashMap<>(Map.of("keep", "v1")));
            when(accessRelationRepository.findById(5L)).thenReturn(Optional.of(existing));

            AccessRelationApplicationService.UpdateCommand c =
                    new AccessRelationApplicationService.UpdateCommand();
            c.setRelation("manager");
            c.setAccessLevel(AccessLevel.OWNER);
            c.setIncludeChildren(true);
            c.setMetadata(Map.of("added", "v2"));
            c.setRemark("新备注");

            service.update(5L, c);

            verify(accessRelationRepository).update(existing);
            assertThat(existing.getRelation()).isEqualTo("manager");
            assertThat(existing.getAccessLevel()).isEqualTo(AccessLevel.OWNER);
            assertThat(existing.isIncludeChildren()).isTrue();
            assertThat(existing.getMetadata()).containsEntry("keep", "v1")
                    .containsEntry("added", "v2");
            assertThat(existing.getRemark()).isEqualTo("新备注");
        }

        @Test
        @DisplayName("空命令保持原值不变")
        void emptyCommandKeepsValues() {
            AccessRelation existing = rel(6L, "place", 1L, "user", 2L, "user");
            when(accessRelationRepository.findById(6L)).thenReturn(Optional.of(existing));

            service.update(6L, new AccessRelationApplicationService.UpdateCommand());

            assertThat(existing.getRelation()).isEqualTo("user");
            verify(accessRelationRepository).update(existing);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("先 enforce BEFORE_REVOKE, 读出 relation, deleteById, check AFTER_REVOKE")
        void happyPath() {
            AccessRelation existing = rel(7L, "place", 1L, "user", 2L, "user");
            when(accessRelationRepository.findById(7L)).thenReturn(Optional.of(existing));
            when(policyRegistry.check(any(PolicyContext.class))).thenReturn(List.of());

            service.delete(7L);

            verify(policyRegistry).enforce(any(PolicyContext.class));
            verify(accessRelationRepository).deleteById(7L);
            verify(policyRegistry).check(any(PolicyContext.class));
        }

        @Test
        @DisplayName("关系不存在时仍执行 deleteById, AFTER payload 为 null")
        void notFoundStillDeletes() {
            when(accessRelationRepository.findById(8L)).thenReturn(Optional.empty());
            when(policyRegistry.check(any(PolicyContext.class))).thenReturn(List.of());

            service.delete(8L);

            verify(accessRelationRepository).deleteById(8L);
        }
    }

    @Nested
    @DisplayName("batchCreate / batchDelete")
    class Batch {

        @Test
        @DisplayName("batchCreate 映射全部命令并委托 batchSave")
        void batchCreate() {
            AccessRelationApplicationService.CreateCommand c1 =
                    new AccessRelationApplicationService.CreateCommand();
            c1.setResourceType("place");
            c1.setResourceId(1L);
            AccessRelationApplicationService.CreateCommand c2 =
                    new AccessRelationApplicationService.CreateCommand();
            c2.setResourceType("org_unit");
            c2.setResourceId(2L);
            when(accessRelationRepository.batchSave(any())).thenReturn(2);

            int n = service.batchCreate(List.of(c1, c2));

            assertThat(n).isEqualTo(2);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<AccessRelation>> cap = ArgumentCaptor.forClass(List.class);
            verify(accessRelationRepository).batchSave(cap.capture());
            assertThat(cap.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("batchDelete 委托 batchDeleteByIds")
        void batchDelete() {
            when(accessRelationRepository.batchDeleteByIds(List.of(1L, 2L))).thenReturn(2);

            assertThat(service.batchDelete(List.of(1L, 2L))).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("listPaged")
    class ListPaged {

        @Test
        @DisplayName("第一页正确切片并返回总数")
        void firstPage() {
            List<AccessRelation> all = List.of(
                    rel(1L, "place", 1L, "user", 1L, "user"),
                    rel(2L, "place", 2L, "user", 1L, "user"),
                    rel(3L, "place", 3L, "user", 1L, "user"));
            when(accessRelationRepository.listFiltered("place", "user", null)).thenReturn(all);

            AccessRelationApplicationService.PagedResult result =
                    service.listPaged("place", "user", null, 1, 2);

            assertThat(result.total()).isEqualTo(3L);
            assertThat(result.records()).hasSize(2);
        }

        @Test
        @DisplayName("超出范围的页码返回空切片")
        void pageOutOfRange() {
            when(accessRelationRepository.listFiltered(null, null, null))
                    .thenReturn(List.of(rel(1L, "place", 1L, "user", 1L, "user")));

            AccessRelationApplicationService.PagedResult result =
                    service.listPaged(null, null, null, 5, 10);

            assertThat(result.records()).isEmpty();
            assertThat(result.total()).isEqualTo(1L);
        }

        @Test
        @DisplayName("最后一页部分填充")
        void lastPartialPage() {
            List<AccessRelation> all = List.of(
                    rel(1L, "place", 1L, "user", 1L, "user"),
                    rel(2L, "place", 2L, "user", 1L, "user"),
                    rel(3L, "place", 3L, "user", 1L, "user"));
            when(accessRelationRepository.listFiltered(null, null, null)).thenReturn(all);

            AccessRelationApplicationService.PagedResult result =
                    service.listPaged(null, null, null, 2, 2);

            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).getId()).isEqualTo(3L);
        }
    }
}
