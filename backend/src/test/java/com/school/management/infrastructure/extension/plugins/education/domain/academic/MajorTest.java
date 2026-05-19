package com.school.management.infrastructure.extension.plugins.education.domain.academic;

import com.school.management.infrastructure.extension.plugins.education.domain.academic.model.Major;
import com.school.management.infrastructure.extension.plugins.education.domain.academic.model.MajorDirection;
import com.school.management.infrastructure.extension.plugins.education.domain.academic.model.MajorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Major 聚合根单元测试 - 验证创建、专业方向管理、状态流转、转移组织单元等业务规则
 */
@DisplayName("Major 聚合根测试")
class MajorTest {

    private static final Long CREATOR_ID = 1L;
    private static final Long ORG_UNIT_ID = 100L;

    private Major major;

    @BeforeEach
    void setUp() {
        major = Major.create("CS001", "计算机应用", ORG_UNIT_ID, "计算机应用专业描述", CREATOR_ID);
    }

    @Nested
    @DisplayName("创建专业")
    class CreateMajorTests {

        @Test
        @DisplayName("应成功创建专业并初始化默认值")
        void shouldCreateMajorWithDefaults() {
            assertThat(major).isNotNull();
            assertThat(major.getMajorCode()).isEqualTo("CS001");
            assertThat(major.getMajorName()).isEqualTo("计算机应用");
            assertThat(major.getOrgUnitId()).isEqualTo(ORG_UNIT_ID);
            assertThat(major.getDescription()).isEqualTo("计算机应用专业描述");
            assertThat(major.getCreatedBy()).isEqualTo(CREATOR_ID);
            // 默认值
            assertThat(major.isEnabled()).isTrue();
            assertThat(major.getSortOrder()).isEqualTo(0);
            assertThat(major.getMajorStatus()).isEqualTo(MajorStatus.ENROLLING);
            assertThat(major.getDirections()).isEmpty();
            assertThat(major.getCreatedAt()).isNotNull();
            assertThat(major.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("专业编码为 null 抛 NPE")
        void shouldFailWhenMajorCodeIsNull() {
            assertThatThrownBy(() -> Major.create(null, "计算机应用", ORG_UNIT_ID, null, CREATOR_ID))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("专业名称为 null 抛 NPE")
        void shouldFailWhenMajorNameIsNull() {
            assertThatThrownBy(() -> Major.create("CS001", null, ORG_UNIT_ID, null, CREATOR_ID))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("组织单元 ID 为 null 抛 NPE")
        void shouldFailWhenOrgUnitIdIsNull() {
            assertThatThrownBy(() -> Major.create("CS001", "计算机应用", null, null, CREATOR_ID))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("专业编码为空字符串抛 IllegalArgumentException")
        void shouldFailWhenMajorCodeBlank() {
            assertThatThrownBy(() -> Major.create("   ", "计算机应用", ORG_UNIT_ID, null, CREATOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Major code");
        }

        @Test
        @DisplayName("专业名称为空字符串抛 IllegalArgumentException")
        void shouldFailWhenMajorNameBlank() {
            assertThatThrownBy(() -> Major.create("CS001", "   ", ORG_UNIT_ID, null, CREATOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Major name");
        }

        @Test
        @DisplayName("专业编码超长抛异常")
        void shouldFailWhenMajorCodeTooLong() {
            String longCode = "X".repeat(51);
            assertThatThrownBy(() -> Major.create(longCode, "计算机应用", ORG_UNIT_ID, null, CREATOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceed 50");
        }
    }

    @Nested
    @DisplayName("更新专业信息")
    class UpdateMajorTests {

        @Test
        @DisplayName("应正常更新专业基本信息")
        void shouldUpdateInfo() {
            major.updateInfo("软件工程", "新描述", 5, 2L);

            assertThat(major.getMajorName()).isEqualTo("软件工程");
            assertThat(major.getDescription()).isEqualTo("新描述");
            assertThat(major.getSortOrder()).isEqualTo(5);
            assertThat(major.getUpdatedBy()).isEqualTo(2L);
        }

        @Test
        @DisplayName("传入空白名称时保留原名称")
        void shouldKeepNameWhenBlank() {
            major.updateInfo("  ", "新描述", null, 2L);
            assertThat(major.getMajorName()).isEqualTo("计算机应用");
        }

        @Test
        @DisplayName("更新技工院校字段")
        void shouldUpdateVocationalInfo() {
            major.updateVocationalInfo("0101", "初中毕业生", "全日制",
                    99L, "李老师", 2023, MajorStatus.SUSPENDED);

            assertThat(major.getMajorCategoryCode()).isEqualTo("0101");
            assertThat(major.getEnrollmentTarget()).isEqualTo("初中毕业生");
            assertThat(major.getEducationForm()).isEqualTo("全日制");
            assertThat(major.getLeadTeacherId()).isEqualTo(99L);
            assertThat(major.getLeadTeacherName()).isEqualTo("李老师");
            assertThat(major.getApprovalYear()).isEqualTo(2023);
            assertThat(major.getMajorStatus()).isEqualTo(MajorStatus.SUSPENDED);
        }

        @Test
        @DisplayName("majorStatus 为 null 时不覆盖原状态")
        void shouldNotOverrideStatusWhenNull() {
            MajorStatus before = major.getMajorStatus();
            major.updateVocationalInfo(null, null, null, null, null, null, null);
            assertThat(major.getMajorStatus()).isEqualTo(before);
        }
    }

    @Nested
    @DisplayName("专业方向管理")
    class DirectionManagementTests {

        @Test
        @DisplayName("应能添加专业方向")
        void shouldAddDirection() {
            MajorDirection dir = major.addDirection("D1", "软件方向", "高级工", 3,
                    false, null, null, null, null);

            assertThat(dir).isNotNull();
            assertThat(dir.getDirectionCode()).isEqualTo("D1");
            assertThat(dir.getDirectionName()).isEqualTo("软件方向");
            assertThat(major.getDirections()).hasSize(1);
        }

        @Test
        @DisplayName("方向编码重复抛异常")
        void shouldFailWhenDirectionCodeDuplicate() {
            major.addDirection("D1", "软件方向", "高级工", 3,
                    false, null, null, null, null);

            assertThatThrownBy(() -> major.addDirection("D1", "另一方向", "中级工", 3,
                    false, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("通过编码查找专业方向")
        void shouldFindDirectionByCode() {
            major.addDirection("D1", "软件方向", "高级工", 3,
                    false, null, null, null, null);

            Optional<MajorDirection> found = major.findDirectionByCode("D1");
            assertThat(found).isPresent();
            assertThat(found.get().getDirectionName()).isEqualTo("软件方向");

            Optional<MajorDirection> missing = major.findDirectionByCode("D99");
            assertThat(missing).isEmpty();
        }

        @Test
        @DisplayName("移除不存在的方向抛异常")
        void shouldFailToRemoveMissingDirection() {
            assertThatThrownBy(() -> major.removeDirection(9999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("getDirections 返回不可修改列表")
        void shouldReturnUnmodifiableDirections() {
            major.addDirection("D1", "软件方向", "高级工", 3,
                    false, null, null, null, null);

            assertThatThrownBy(() -> major.getDirections().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("启用/禁用")
    class EnableDisableTests {

        @Test
        @DisplayName("disable 后 enabled 变 false")
        void shouldDisable() {
            major.disable();
            assertThat(major.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable 后 enabled 变 true")
        void shouldEnable() {
            major.disable();
            major.enable();
            assertThat(major.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("转移组织单元")
    class TransferOrgUnitTests {

        @Test
        @DisplayName("应能转移到新组织单元")
        void shouldTransfer() {
            major.transferToOrgUnit(200L, 99L);
            assertThat(major.getOrgUnitId()).isEqualTo(200L);
            assertThat(major.getUpdatedBy()).isEqualTo(99L);
        }

        @Test
        @DisplayName("转移到 null 组织单元抛 NPE")
        void shouldFailWhenTransferToNull() {
            assertThatThrownBy(() -> major.transferToOrgUnit(null, 99L))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
