package com.school.management.infrastructure.extension.plugins.education.application.myclass;

import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.model.entity.UniversalPlaceOccupant;
import com.school.management.domain.place.repository.UniversalPlaceOccupantRepository;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.exception.BusinessException;
import com.school.management.infrastructure.extension.plugins.education.application.myclass.query.DormitoryDistributionDTO;
import com.school.management.infrastructure.extension.plugins.education.application.myclass.query.MyClassDTO;
import com.school.management.infrastructure.extension.plugins.education.application.myclass.query.MyClassOverviewDTO;
import com.school.management.infrastructure.extension.plugins.education.application.myclass.query.MyClassStudentDTO;
import com.school.management.infrastructure.extension.plugins.education.domain.student.model.ClassStatus;
import com.school.management.infrastructure.extension.plugins.education.domain.student.model.SchoolClass;
import com.school.management.infrastructure.extension.plugins.education.domain.student.model.aggregate.Student;
import com.school.management.infrastructure.extension.plugins.education.domain.student.model.valueobject.Gender;
import com.school.management.infrastructure.extension.plugins.education.domain.student.model.valueobject.StudentStatus;
import com.school.management.infrastructure.extension.plugins.education.domain.student.repository.SchoolClassRepository;
import com.school.management.infrastructure.extension.plugins.education.domain.student.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MyClassApplicationService 单测 — 验证"我的班级"查询、权限校验与宿舍分布组装
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MyClassApplicationService 测试")
class MyClassApplicationServiceTest {

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UniversalPlaceRepository universalPlaceRepository;

    @Mock
    private UniversalPlaceOccupantRepository placeOccupantRepository;

    @Mock
    private ClassInspectionStatsQueryService inspectionStatsQuery;

    @InjectMocks
    private MyClassApplicationService service;

    // ---- helpers ----

    private SchoolClass buildClass(Long id) {
        return SchoolClass.builder()
            .id(id)
            .classCode("CLS-" + id)
            .className("软件" + id + "班")
            .shortName("软" + id)
            .orgUnitId(100L)
            .enrollmentYear(2024)
            .standardSize(50)
            .currentSize(30)
            .status(ClassStatus.ACTIVE)
            .createdBy(1L)
            .build();
    }

    private Student buildStudent(Long id, String studentNo, String name,
                                 Gender gender, StudentStatus status) {
        return Student.reconstruct(
            id, 100L, studentNo, name, gender, "110101200001011234",
            "13800000000", "s@test.com", LocalDate.of(2006, 1, 1),
            LocalDate.of(2024, 9, 1), LocalDate.of(2028, 6, 30),
            10L, status, null, "家庭住址", "联系人", "13900000000",
            "备注", null, null);
    }

    private UniversalPlace buildPlace(Long id, String code, String name, Long parentId) {
        return UniversalPlace.builder()
            .id(id)
            .placeCode(code)
            .placeName(name)
            .typeCode("ROOM")
            .parentId(parentId)
            .build();
    }

    private UniversalPlaceOccupant buildOccupant(Long placeId, Long occupantId,
                                                 String occupantName, String positionNo) {
        return UniversalPlaceOccupant.builder()
            .id(occupantId)
            .placeId(placeId)
            .occupantType("STUDENT")
            .occupantId(occupantId)
            .occupantName(occupantName)
            .positionNo(positionNo)
            .status(1)
            .build();
    }

    @Nested
    @DisplayName("getMyClasses 我的班级列表")
    class GetMyClassesTests {

        @Test
        @DisplayName("应将教师管理的班级映射为 MyClassDTO 列表")
        void shouldReturnMyClasses() {
            when(schoolClassRepository.findByTeacherId(99L))
                .thenReturn(List.of(buildClass(1L), buildClass(2L)));

            List<MyClassDTO> result = service.getMyClasses(99L);

            assertThat(result).hasSize(2);
            MyClassDTO dto = result.get(0);
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getClassCode()).isEqualTo("CLS-1");
            assertThat(dto.getClassName()).isEqualTo("软件1班");
            assertThat(dto.getMyRole()).isEqualTo("HEAD_TEACHER");
            assertThat(dto.getStatus()).isEqualTo("ACTIVE");
            assertThat(dto.getStandardSize()).isEqualTo(50);
        }

        @Test
        @DisplayName("教师无班级时返回空列表")
        void shouldReturnEmptyWhenNoClasses() {
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of());

            assertThat(service.getMyClasses(99L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getClassOverview 班级概览")
    class GetClassOverviewTests {

        @Test
        @DisplayName("有权限时应聚合学生总数与性别统计")
        void shouldReturnOverview() {
            SchoolClass clazz = buildClass(1L);
            when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(clazz));
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(clazz));
            when(studentRepository.countByClassId(1L)).thenReturn(40L);
            when(studentRepository.countByClassIdAndGender(1L, Gender.MALE)).thenReturn(25L);
            when(studentRepository.countByClassIdAndGender(1L, Gender.FEMALE)).thenReturn(15L);
            when(inspectionStatsQuery.query(1L)).thenReturn(
                ClassInspectionStatsQueryService.ClassInspectionStats.empty());

            MyClassOverviewDTO dto = service.getClassOverview(1L, 99L);

            assertThat(dto.getOrgUnitId()).isEqualTo(1L);
            assertThat(dto.getClassName()).isEqualTo("软件1班");
            assertThat(dto.getStudentCount()).isEqualTo(40);
            assertThat(dto.getMaleCount()).isEqualTo(25);
            assertThat(dto.getFemaleCount()).isEqualTo(15);
            assertThat(dto.getScoreTrendList()).isEmpty();
            assertThat(dto.getRecentRecords()).isEmpty();
        }

        @Test
        @DisplayName("班级不存在时抛 BusinessException")
        void shouldThrowWhenClassNotFound() {
            when(schoolClassRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getClassOverview(404L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("班级不存在");
        }

        @Test
        @DisplayName("教师无权访问该班级时抛 BusinessException")
        void shouldThrowWhenNoAccess() {
            SchoolClass clazz = buildClass(1L);
            when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(clazz));
            // 教师管理的是别的班级
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(buildClass(2L)));

            assertThatThrownBy(() -> service.getClassOverview(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问该班级");
            verify(studentRepository, never()).countByClassId(anyLong());
        }
    }

    @Nested
    @DisplayName("getClassStudents 班级学生列表")
    class GetClassStudentsTests {

        @Test
        @DisplayName("无关键字与状态过滤时返回全部学生并完成 DTO 映射")
        void shouldReturnAllStudents() {
            SchoolClass clazz = buildClass(1L);
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(clazz));
            Student s1 = buildStudent(11L, "2024001", "张三", Gender.MALE, StudentStatus.STUDYING);
            Student s2 = buildStudent(12L, "2024002", "李四", Gender.FEMALE, StudentStatus.SUSPENDED);
            when(studentRepository.findByClassId(1L)).thenReturn(List.of(s1, s2));
            when(placeOccupantRepository.findActiveByOccupant(eq("STUDENT"), anyLong()))
                .thenReturn(Optional.empty());

            List<MyClassStudentDTO> result = service.getClassStudents(1L, 99L, null, null);

            assertThat(result).hasSize(2);
            MyClassStudentDTO d1 = result.get(0);
            assertThat(d1.getId()).isEqualTo(11L);
            assertThat(d1.getStudentNo()).isEqualTo("2024001");
            assertThat(d1.getName()).isEqualTo("张三");
            assertThat(d1.getGender()).isEqualTo("男");
            assertThat(d1.getStatus()).isEqualTo("ENROLLED");
            assertThat(d1.getDormitoryName()).isNull();
            assertThat(result.get(1).getStatus()).isEqualTo("SUSPENDED");
        }

        @Test
        @DisplayName("关键字应按姓名或学号过滤")
        void shouldFilterByKeyword() {
            SchoolClass clazz = buildClass(1L);
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(clazz));
            Student s1 = buildStudent(11L, "2024001", "张三", Gender.MALE, StudentStatus.STUDYING);
            Student s2 = buildStudent(12L, "2024002", "李四", Gender.FEMALE, StudentStatus.STUDYING);
            when(studentRepository.findByClassId(1L)).thenReturn(List.of(s1, s2));
            lenient().when(placeOccupantRepository.findActiveByOccupant(eq("STUDENT"), anyLong()))
                .thenReturn(Optional.empty());

            List<MyClassStudentDTO> byName = service.getClassStudents(1L, 99L, "张", null);
            assertThat(byName).hasSize(1);
            assertThat(byName.get(0).getName()).isEqualTo("张三");

            List<MyClassStudentDTO> byNo = service.getClassStudents(1L, 99L, "2024002", null);
            assertThat(byNo).hasSize(1);
            assertThat(byNo.get(0).getStudentNo()).isEqualTo("2024002");
        }

        @Test
        @DisplayName("状态码应按学籍状态过滤")
        void shouldFilterByStatus() {
            SchoolClass clazz = buildClass(1L);
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(clazz));
            Student studying = buildStudent(11L, "2024001", "张三", Gender.MALE, StudentStatus.STUDYING);
            Student suspended = buildStudent(12L, "2024002", "李四", Gender.FEMALE, StudentStatus.SUSPENDED);
            when(studentRepository.findByClassId(1L)).thenReturn(List.of(studying, suspended));
            lenient().when(placeOccupantRepository.findActiveByOccupant(eq("STUDENT"), anyLong()))
                .thenReturn(Optional.empty());

            // status "1" = STUDYING
            List<MyClassStudentDTO> result = service.getClassStudents(1L, 99L, null, "1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("学生有在住宿舍时 DTO 应填充宿舍名与床位号")
        void shouldPopulateDormitory() {
            SchoolClass clazz = buildClass(1L);
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(clazz));
            Student s1 = buildStudent(11L, "2024001", "张三", Gender.MALE, StudentStatus.STUDYING);
            when(studentRepository.findByClassId(1L)).thenReturn(List.of(s1));

            UniversalPlaceOccupant occ = buildOccupant(700L, 11L, "张三", "A-1");
            when(placeOccupantRepository.findActiveByOccupant("STUDENT", 11L))
                .thenReturn(Optional.of(occ));
            // room 700 -> floor 600 -> building 500
            UniversalPlace room = buildPlace(700L, "R-101", "101室", 600L);
            UniversalPlace floor = buildPlace(600L, "F-1", "1层", 500L);
            UniversalPlace building = buildPlace(500L, "B-A", "A栋", null);
            when(universalPlaceRepository.findById(700L)).thenReturn(Optional.of(room));
            when(universalPlaceRepository.findById(600L)).thenReturn(Optional.of(floor));
            when(universalPlaceRepository.findById(500L)).thenReturn(Optional.of(building));

            List<MyClassStudentDTO> result = service.getClassStudents(1L, 99L, null, null);

            assertThat(result).hasSize(1);
            MyClassStudentDTO d = result.get(0);
            assertThat(d.getBedNo()).isEqualTo("A-1");
            assertThat(d.getDormitoryName()).isEqualTo("A栋 101室");
        }

        @Test
        @DisplayName("无权访问该班级时抛 BusinessException")
        void shouldThrowWhenNoAccess() {
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(buildClass(2L)));

            assertThatThrownBy(() -> service.getClassStudents(1L, 99L, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问该班级");
        }
    }

    @Nested
    @DisplayName("getDormitoryDistribution 宿舍分布")
    class GetDormitoryDistributionTests {

        @Test
        @DisplayName("班级无分配场所时返回空列表")
        void shouldReturnEmptyWhenNoPlaces() {
            SchoolClass clazz = buildClass(1L);
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(clazz));
            when(universalPlaceRepository.findByOrgUnitId(1L)).thenReturn(List.of());

            assertThat(service.getDormitoryDistribution(1L, 99L)).isEmpty();
        }

        @Test
        @DisplayName("应按楼栋分组并组装房间、床位与人数")
        void shouldAssembleDistributionByBuilding() {
            SchoolClass clazz = buildClass(1L);
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(clazz));

            // 两个房间, 共同父级楼层 600, 楼栋 500
            UniversalPlace room1 = buildPlace(700L, "R-101", "101室", 600L);
            UniversalPlace room2 = buildPlace(701L, "R-102", "102室", 600L);
            when(universalPlaceRepository.findByOrgUnitId(1L)).thenReturn(List.of(room1, room2));

            UniversalPlace floor = buildPlace(600L, "F-1", "1层", 500L);
            UniversalPlace building = buildPlace(500L, "B-A", "A栋", null);
            when(universalPlaceRepository.findById(600L)).thenReturn(Optional.of(floor));
            when(universalPlaceRepository.findById(500L)).thenReturn(Optional.of(building));

            when(placeOccupantRepository.findActiveByPlaceId(700L))
                .thenReturn(List.of(buildOccupant(700L, 11L, "张三", "1"),
                                    buildOccupant(700L, 12L, "李四", "2")));
            when(placeOccupantRepository.findActiveByPlaceId(701L))
                .thenReturn(List.of(buildOccupant(701L, 13L, "王五", "1")));

            List<DormitoryDistributionDTO> result = service.getDormitoryDistribution(1L, 99L);

            assertThat(result).hasSize(1);
            DormitoryDistributionDTO b = result.get(0);
            assertThat(b.getBuildingId()).isEqualTo(500L);
            assertThat(b.getBuildingName()).isEqualTo("A栋");
            assertThat(b.getStudentCount()).isEqualTo(3);
            assertThat(b.getRooms()).hasSize(2);
            DormitoryDistributionDTO.DormitoryRoomDTO firstRoom = b.getRooms().get(0);
            assertThat(firstRoom.getStudentCount()).isEqualTo(2);
            assertThat(firstRoom.getUser_student()).hasSize(2);
            assertThat(firstRoom.getUser_student().get(0).getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("房间无父节点时归入未知楼栋")
        void shouldGroupOrphanRoomsUnderUnknownBuilding() {
            SchoolClass clazz = buildClass(1L);
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(clazz));

            UniversalPlace orphanRoom = buildPlace(700L, "R-101", "101室", null);
            when(universalPlaceRepository.findByOrgUnitId(1L)).thenReturn(List.of(orphanRoom));
            when(placeOccupantRepository.findActiveByPlaceId(700L)).thenReturn(List.of());

            List<DormitoryDistributionDTO> result = service.getDormitoryDistribution(1L, 99L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBuildingId()).isEqualTo(0L);
            assertThat(result.get(0).getBuildingName()).isEqualTo("未知楼栋");
        }

        @Test
        @DisplayName("入住学生查询异常时该房间按 0 人处理且不影响整体")
        void shouldTolerateOccupantQueryFailure() {
            SchoolClass clazz = buildClass(1L);
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(clazz));

            UniversalPlace room = buildPlace(700L, "R-101", "101室", 500L);
            when(universalPlaceRepository.findByOrgUnitId(1L)).thenReturn(List.of(room));
            UniversalPlace building = buildPlace(500L, "B-A", "A栋", null);
            when(universalPlaceRepository.findById(500L)).thenReturn(Optional.of(building));
            when(placeOccupantRepository.findActiveByPlaceId(700L))
                .thenThrow(new RuntimeException("DB down"));

            List<DormitoryDistributionDTO> result = service.getDormitoryDistribution(1L, 99L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStudentCount()).isEqualTo(0);
            assertThat(result.get(0).getRooms().get(0).getStudentCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("房间含 attributes 时应解析 roomNo 与 floorNumber")
        void shouldResolveRoomAttributes() {
            SchoolClass clazz = buildClass(1L);
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(clazz));

            Map<String, Object> attrs = new HashMap<>();
            attrs.put("roomNo", "888");
            attrs.put("floorNumber", 8);
            UniversalPlace room = UniversalPlace.builder()
                .id(700L).placeCode("R-700").placeName("700室")
                .typeCode("ROOM").parentId(500L).attributes(attrs).build();
            when(universalPlaceRepository.findByOrgUnitId(1L)).thenReturn(List.of(room));
            UniversalPlace building = buildPlace(500L, "B-A", "A栋", null);
            when(universalPlaceRepository.findById(500L)).thenReturn(Optional.of(building));
            when(placeOccupantRepository.findActiveByPlaceId(700L)).thenReturn(List.of());

            List<DormitoryDistributionDTO> result = service.getDormitoryDistribution(1L, 99L);

            DormitoryDistributionDTO.DormitoryRoomDTO r = result.get(0).getRooms().get(0);
            assertThat(r.getRoomNo()).isEqualTo("888");
            assertThat(r.getFloor()).isEqualTo(8);
        }

        @Test
        @DisplayName("无权访问该班级时抛 BusinessException")
        void shouldThrowWhenNoAccess() {
            when(schoolClassRepository.findByTeacherId(99L)).thenReturn(List.of(buildClass(2L)));

            assertThatThrownBy(() -> service.getDormitoryDistribution(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问该班级");
            verify(universalPlaceRepository, never()).findByOrgUnitId(anyLong());
        }
    }
}
