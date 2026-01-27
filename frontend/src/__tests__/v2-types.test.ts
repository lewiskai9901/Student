import { describe, it, expect } from 'vitest'
import type {
  // Organization types
  OrgUnit,
  OrgUnitType,
  SchoolClass,
  ClassStatus,
  ClassQueryParams,
  // Student types
  Student,
  StudentStatus,
  StudentQueryParams,
  // Dormitory types
  Dormitory,
  Building,
  DormitoryQueryParams,
  // Access types
  Role,
  Permission,
  // Common types
  PageResponse,
  PageParams
} from '@/types'
import {
  StudentStatusMap,
  GenderMap,
  ClassStatusConfig,
  DormitoryStatusMap,
  GenderTypeMap,
  RoomUsageTypeMap
} from '@/types'

/**
 * V2 类型定义测试
 * 验证 DDD 架构适配的类型定义正确导出
 */
describe('V2 Types', () => {
  describe('Organization Types', () => {
    it('should have correct OrgUnitType values', () => {
      const validTypes: OrgUnitType[] = ['SCHOOL', 'COLLEGE', 'DEPARTMENT', 'TEACHING_GROUP']
      expect(validTypes).toHaveLength(4)
    })

    it('should have correct ClassStatus values', () => {
      const validStatuses: ClassStatus[] = ['PREPARING', 'ACTIVE', 'GRADUATED', 'DISSOLVED']
      expect(validStatuses).toHaveLength(4)
    })

    it('should have ClassStatusConfig for all statuses', () => {
      expect(ClassStatusConfig.PREPARING).toBeDefined()
      expect(ClassStatusConfig.ACTIVE).toBeDefined()
      expect(ClassStatusConfig.GRADUATED).toBeDefined()
      expect(ClassStatusConfig.DISSOLVED).toBeDefined()
    })

    it('ClassStatusConfig should have required properties', () => {
      expect(ClassStatusConfig.ACTIVE).toHaveProperty('label')
      expect(ClassStatusConfig.ACTIVE).toHaveProperty('type')
      expect(ClassStatusConfig.ACTIVE).toHaveProperty('color')
    })
  })

  describe('Student Types', () => {
    it('should have correct StudentStatus values', () => {
      const validStatuses: StudentStatus[] = [0, 1, 2, 3, 4]
      expect(validStatuses).toHaveLength(5)
    })

    it('should have StudentStatusMap for all statuses', () => {
      expect(StudentStatusMap[0]).toBe('在读')
      expect(StudentStatusMap[1]).toBe('休学')
      expect(StudentStatusMap[2]).toBe('退学')
      expect(StudentStatusMap[3]).toBe('毕业')
      expect(StudentStatusMap[4]).toBe('转学')
    })

    it('should have GenderMap', () => {
      expect(GenderMap[1]).toBe('男')
      expect(GenderMap[2]).toBe('女')
    })
  })

  describe('Dormitory Types', () => {
    it('should have DormitoryStatusMap', () => {
      expect(DormitoryStatusMap[0]).toBe('停用')
      expect(DormitoryStatusMap[1]).toBe('正常')
    })

    it('should have GenderTypeMap', () => {
      expect(GenderTypeMap[1]).toBe('男')
      expect(GenderTypeMap[2]).toBe('女')
      expect(GenderTypeMap[3]).toBe('混合')
    })

    it('should have RoomUsageTypeMap', () => {
      expect(RoomUsageTypeMap[1]).toBe('学生宿舍')
      expect(RoomUsageTypeMap[2]).toBe('教职工宿舍')
      expect(RoomUsageTypeMap[3]).toBe('配电室')
      expect(RoomUsageTypeMap[4]).toBe('卫生间')
      expect(RoomUsageTypeMap[5]).toBe('杂物间')
      expect(RoomUsageTypeMap[6]).toBe('其他')
    })
  })

  describe('Common Types', () => {
    it('PageParams should be usable', () => {
      const params: PageParams = {
        pageNum: 1,
        pageSize: 10
      }
      expect(params.pageNum).toBe(1)
      expect(params.pageSize).toBe(10)
    })

    it('PageResponse should be usable with generics', () => {
      const response: PageResponse<{ id: number }> = {
        records: [{ id: 1 }, { id: 2 }],
        total: 100,
        size: 10,
        current: 1,
        pages: 10
      }
      expect(response.records).toHaveLength(2)
      expect(response.total).toBe(100)
    })
  })

  describe('Type Compatibility', () => {
    it('should create valid Student object', () => {
      const student: Partial<Student> = {
        id: 1,
        studentNo: '2024001',
        name: '张三',
        gender: 1,
        status: 0,
        classId: 1
      }
      expect(student.id).toBe(1)
      expect(student.name).toBe('张三')
    })

    it('should create valid SchoolClass object', () => {
      const schoolClass: Partial<SchoolClass> = {
        id: 1,
        classCode: 'CS2024-01',
        className: '计算机科学2024级1班',
        orgUnitId: 1,
        enrollmentYear: 2024,
        gradeLevel: 1,
        status: 'ACTIVE',
        teacherAssignments: []
      }
      expect(schoolClass.status).toBe('ACTIVE')
    })

    it('should create valid Dormitory object', () => {
      const dormitory: Partial<Dormitory> = {
        id: 1,
        buildingId: 1,
        dormitoryNo: '101',
        floorNumber: 1,
        roomUsageType: 1,
        bedCapacity: 6,
        bedCount: 6,
        occupiedBeds: 4,
        genderType: 1,
        status: 1
      }
      expect(dormitory.bedCapacity).toBe(6)
    })

    it('should create valid OrgUnit object', () => {
      const orgUnit: Partial<OrgUnit> = {
        id: 1,
        unitCode: 'CS',
        unitName: '计算机学院',
        unitType: 'COLLEGE',
        parentId: null,
        enabled: true
      }
      expect(orgUnit.unitType).toBe('COLLEGE')
    })
  })
})
