import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock the http client before importing API modules
vi.mock('@/utils/request', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn()
  }
}))

// Import after mocking
import { http } from '@/utils/request'
import {
  getStudents,
  getStudent,
  getStudentByNo,
  createStudent,
  updateStudent,
  deleteStudent,
  deleteStudents,
  searchStudents,
  existsStudentNo,
  updateStudentStatus,
  transferClass,
  studentApi
} from '@/api/student'

import {
  getOrgUnits,
  getOrgUnitTree,
  getOrgUnit,
  createOrgUnit,
  updateOrgUnit,
  deleteOrgUnit,
  enableOrgUnit,
  disableOrgUnit,
  getOrgUnitsByType,
  getClasses,
  getClass,
  createClass,
  updateClass,
  deleteClass,
  activateClass,
  graduateClass,
  dissolveClass,
  assignHeadTeacher,
  orgUnitApi,
  schoolClassApi
} from '@/api/organization'

const mockedHttp = vi.mocked(http)

/**
 * V2 API 测试
 * 验证 DDD 架构适配的 API 模块正确调用后端接口
 */
describe('V2 API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Student API', () => {
    describe('getStudents', () => {
      it('should call GET /students with params', async () => {
        const mockResponse = {
          records: [{ id: 1, name: '张三', studentNo: '2024001' }],
          total: 1,
          size: 10,
          current: 1,
          pages: 1
        }
        mockedHttp.get.mockResolvedValue(mockResponse)

        const params = { pageNum: 1, pageSize: 10, status: 0 }
        const result = await getStudents(params)

        expect(mockedHttp.get).toHaveBeenCalledWith('/students', { params })
        expect(result).toEqual(mockResponse)
      })

      it('should work without params', async () => {
        const mockResponse = { records: [], total: 0, size: 10, current: 1, pages: 0 }
        mockedHttp.get.mockResolvedValue(mockResponse)

        await getStudents()

        expect(mockedHttp.get).toHaveBeenCalledWith('/students', { params: undefined })
      })
    })

    describe('getStudent', () => {
      it('should call GET /students/:id', async () => {
        const mockStudent = { id: 1, name: '张三', studentNo: '2024001', status: 0 }
        mockedHttp.get.mockResolvedValue(mockStudent)

        const result = await getStudent(1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/students/1')
        expect(result).toEqual(mockStudent)
      })
    })

    describe('getStudentByNo', () => {
      it('should call GET /students/by-no/:studentNo', async () => {
        const mockStudent = { id: 1, name: '张三', studentNo: '2024001' }
        mockedHttp.get.mockResolvedValue(mockStudent)

        const result = await getStudentByNo('2024001')

        expect(mockedHttp.get).toHaveBeenCalledWith('/students/by-no/2024001')
        expect(result).toEqual(mockStudent)
      })
    })

    describe('createStudent', () => {
      it('should call POST /students with data', async () => {
        mockedHttp.post.mockResolvedValue(1)

        const data = {
          studentNo: '2024001',
          name: '张三',
          gender: 1,
          classId: 1
        }
        const result = await createStudent(data)

        expect(mockedHttp.post).toHaveBeenCalledWith('/students', data)
        expect(result).toBe(1)
      })
    })

    describe('updateStudent', () => {
      it('should call PUT /students/:id with data', async () => {
        mockedHttp.put.mockResolvedValue(undefined)

        const data = { name: '李四', phone: '13800138000' }
        await updateStudent(1, data)

        expect(mockedHttp.put).toHaveBeenCalledWith('/students/1', data)
      })
    })

    describe('deleteStudent', () => {
      it('should call DELETE /students/:id', async () => {
        mockedHttp.delete.mockResolvedValue(undefined)

        await deleteStudent(1)

        expect(mockedHttp.delete).toHaveBeenCalledWith('/students/1')
      })
    })

    describe('deleteStudents', () => {
      it('should call DELETE /students/batch with ids', async () => {
        mockedHttp.delete.mockResolvedValue(undefined)

        await deleteStudents([1, 2, 3])

        expect(mockedHttp.delete).toHaveBeenCalledWith('/students/batch', { data: [1, 2, 3] })
      })
    })

    describe('searchStudents', () => {
      it('should call GET /students/search with params', async () => {
        const mockStudents = [{ id: 1, name: '张三' }]
        mockedHttp.get.mockResolvedValue(mockStudents)

        const params = { keyword: '张', limit: 10 }
        const result = await searchStudents(params)

        expect(mockedHttp.get).toHaveBeenCalledWith('/students/search', { params })
        expect(result).toEqual(mockStudents)
      })
    })

    describe('existsStudentNo', () => {
      it('should call GET /students/exists with studentNo', async () => {
        mockedHttp.get.mockResolvedValue(false)

        const result = await existsStudentNo('2024001')

        expect(mockedHttp.get).toHaveBeenCalledWith('/students/exists', {
          params: { studentNo: '2024001', excludeId: undefined }
        })
        expect(result).toBe(false)
      })

      it('should include excludeId when provided', async () => {
        mockedHttp.get.mockResolvedValue(true)

        await existsStudentNo('2024001', 1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/students/exists', {
          params: { studentNo: '2024001', excludeId: 1 }
        })
      })
    })

    describe('updateStudentStatus', () => {
      it('should call PATCH /students/:id/status with status', async () => {
        mockedHttp.patch.mockResolvedValue(undefined)

        await updateStudentStatus(1, 1)

        expect(mockedHttp.patch).toHaveBeenCalledWith('/students/1/status', null, {
          params: { status: 1 }
        })
      })
    })

    describe('transferClass', () => {
      it('should call PATCH /students/:id/transfer with newClassId', async () => {
        mockedHttp.patch.mockResolvedValue(undefined)

        await transferClass(1, 2)

        expect(mockedHttp.patch).toHaveBeenCalledWith('/students/1/transfer', null, {
          params: { newClassId: 2 }
        })
      })
    })

    describe('studentApi object', () => {
      it('should have all API methods', () => {
        expect(studentApi.getList).toBe(getStudents)
        expect(studentApi.getById).toBe(getStudent)
        expect(studentApi.getByNo).toBe(getStudentByNo)
        expect(studentApi.create).toBe(createStudent)
        expect(studentApi.update).toBe(updateStudent)
        expect(studentApi.delete).toBe(deleteStudent)
        expect(studentApi.deleteBatch).toBe(deleteStudents)
        expect(studentApi.search).toBe(searchStudents)
        expect(studentApi.exists).toBe(existsStudentNo)
        expect(studentApi.updateStatus).toBe(updateStudentStatus)
        expect(studentApi.transferClass).toBe(transferClass)
      })
    })
  })

  describe('Organization API', () => {
    describe('OrgUnit API', () => {
      describe('getOrgUnits', () => {
        it('should call GET /org-units', async () => {
          const mockUnits = [{ id: 1, unitCode: 'CS', unitName: '计算机学院' }]
          mockedHttp.get.mockResolvedValue(mockUnits)

          const result = await getOrgUnits()

          expect(mockedHttp.get).toHaveBeenCalledWith('/org-units')
          expect(result).toEqual(mockUnits)
        })
      })

      describe('getOrgUnitTree', () => {
        it('should call GET /org-units/tree', async () => {
          const mockTree = [{ id: 1, unitName: '学校', children: [] }]
          mockedHttp.get.mockResolvedValue(mockTree)

          const result = await getOrgUnitTree()

          expect(mockedHttp.get).toHaveBeenCalledWith('/org-units/tree')
          expect(result).toEqual(mockTree)
        })
      })

      describe('getOrgUnit', () => {
        it('should call GET /org-units/:id', async () => {
          const mockUnit = { id: 1, unitCode: 'CS', unitName: '计算机学院' }
          mockedHttp.get.mockResolvedValue(mockUnit)

          const result = await getOrgUnit(1)

          expect(mockedHttp.get).toHaveBeenCalledWith('/org-units/1')
          expect(result).toEqual(mockUnit)
        })
      })

      describe('createOrgUnit', () => {
        it('should call POST /org-units with data', async () => {
          const mockUnit = { id: 1, unitCode: 'CS', unitName: '计算机学院' }
          mockedHttp.post.mockResolvedValue(mockUnit)

          const data = { unitCode: 'CS', unitName: '计算机学院', unitType: 'COLLEGE' as const }
          const result = await createOrgUnit(data)

          expect(mockedHttp.post).toHaveBeenCalledWith('/org-units', data)
          expect(result).toEqual(mockUnit)
        })
      })

      describe('updateOrgUnit', () => {
        it('should call PUT /org-units/:id with data', async () => {
          const mockUnit = { id: 1, unitCode: 'CS', unitName: '计算机科学学院' }
          mockedHttp.put.mockResolvedValue(mockUnit)

          const data = { unitName: '计算机科学学院' }
          const result = await updateOrgUnit(1, data)

          expect(mockedHttp.put).toHaveBeenCalledWith('/org-units/1', data)
          expect(result).toEqual(mockUnit)
        })
      })

      describe('deleteOrgUnit', () => {
        it('should call DELETE /org-units/:id', async () => {
          mockedHttp.delete.mockResolvedValue(undefined)

          await deleteOrgUnit(1)

          expect(mockedHttp.delete).toHaveBeenCalledWith('/org-units/1')
        })
      })

      describe('enableOrgUnit', () => {
        it('should call PUT /org-units/:id/enable', async () => {
          mockedHttp.put.mockResolvedValue(undefined)

          await enableOrgUnit(1)

          expect(mockedHttp.put).toHaveBeenCalledWith('/org-units/1/enable')
        })
      })

      describe('disableOrgUnit', () => {
        it('should call PUT /org-units/:id/disable', async () => {
          mockedHttp.put.mockResolvedValue(undefined)

          await disableOrgUnit(1)

          expect(mockedHttp.put).toHaveBeenCalledWith('/org-units/1/disable')
        })
      })

      describe('getOrgUnitsByType', () => {
        it('should call GET /org-units/by-type/:type', async () => {
          const mockUnits = [{ id: 1, unitType: 'DEPARTMENT' }]
          mockedHttp.get.mockResolvedValue(mockUnits)

          const result = await getOrgUnitsByType('DEPARTMENT')

          expect(mockedHttp.get).toHaveBeenCalledWith('/org-units/by-type/DEPARTMENT')
          expect(result).toEqual(mockUnits)
        })
      })

      describe('orgUnitApi object', () => {
        it('should have all API methods', () => {
          expect(orgUnitApi.getList).toBe(getOrgUnits)
          expect(orgUnitApi.getTree).toBe(getOrgUnitTree)
          expect(orgUnitApi.getById).toBe(getOrgUnit)
          expect(orgUnitApi.getByType).toBe(getOrgUnitsByType)
          expect(orgUnitApi.create).toBe(createOrgUnit)
          expect(orgUnitApi.update).toBe(updateOrgUnit)
          expect(orgUnitApi.delete).toBe(deleteOrgUnit)
          expect(orgUnitApi.enable).toBe(enableOrgUnit)
          expect(orgUnitApi.disable).toBe(disableOrgUnit)
        })
      })
    })

    describe('Class API', () => {
      describe('getClasses', () => {
        it('should call GET /organization/classes with params', async () => {
          const mockResponse = {
            records: [{ id: 1, className: '计算机1班' }],
            total: 1,
            size: 10,
            current: 1,
            pages: 1
          }
          mockedHttp.get.mockResolvedValue(mockResponse)

          const params = { pageNum: 1, pageSize: 10, status: 'ACTIVE' as const }
          const result = await getClasses(params)

          expect(mockedHttp.get).toHaveBeenCalledWith('/organization/classes', { params })
          expect(result).toEqual(mockResponse)
        })
      })

      describe('getClass', () => {
        it('should call GET /organization/classes/:id', async () => {
          const mockClass = { id: 1, className: '计算机1班', status: 'ACTIVE' }
          mockedHttp.get.mockResolvedValue(mockClass)

          const result = await getClass(1)

          expect(mockedHttp.get).toHaveBeenCalledWith('/organization/classes/1')
          expect(result).toEqual(mockClass)
        })
      })

      describe('createClass', () => {
        it('should call POST /organization/classes with data', async () => {
          const mockClass = { id: 1, className: '计算机2班' }
          mockedHttp.post.mockResolvedValue(mockClass)

          const data = {
            classCode: 'CS2024-02',
            className: '计算机2班',
            orgUnitId: 1,
            enrollmentYear: 2024,
            gradeLevel: 1
          }
          const result = await createClass(data)

          expect(mockedHttp.post).toHaveBeenCalledWith('/organization/classes', data)
          expect(result).toEqual(mockClass)
        })
      })

      describe('updateClass', () => {
        it('should call PUT /organization/classes/:id with data', async () => {
          const mockClass = { id: 1, className: '计算机2班(更新)' }
          mockedHttp.put.mockResolvedValue(mockClass)

          const data = { className: '计算机2班(更新)' }
          const result = await updateClass(1, data)

          expect(mockedHttp.put).toHaveBeenCalledWith('/organization/classes/1', data)
          expect(result).toEqual(mockClass)
        })
      })

      describe('deleteClass', () => {
        it('should call DELETE /organization/classes/:id', async () => {
          mockedHttp.delete.mockResolvedValue(undefined)

          await deleteClass(1)

          expect(mockedHttp.delete).toHaveBeenCalledWith('/organization/classes/1')
        })
      })

      describe('activateClass', () => {
        it('should call POST /organization/classes/:id/activate', async () => {
          mockedHttp.post.mockResolvedValue(undefined)

          await activateClass(1)

          expect(mockedHttp.post).toHaveBeenCalledWith('/organization/classes/1/activate')
        })
      })

      describe('graduateClass', () => {
        it('should call POST /organization/classes/:id/graduate', async () => {
          mockedHttp.post.mockResolvedValue(undefined)

          await graduateClass(1)

          expect(mockedHttp.post).toHaveBeenCalledWith('/organization/classes/1/graduate')
        })
      })

      describe('dissolveClass', () => {
        it('should call POST /organization/classes/:id/dissolve', async () => {
          mockedHttp.post.mockResolvedValue(undefined)

          await dissolveClass(1)

          expect(mockedHttp.post).toHaveBeenCalledWith('/organization/classes/1/dissolve')
        })
      })

      describe('assignHeadTeacher', () => {
        it('should call POST /organization/classes/:id/head-teacher with data', async () => {
          mockedHttp.post.mockResolvedValue(undefined)

          const data = { teacherId: 100 }
          await assignHeadTeacher(1, data)

          expect(mockedHttp.post).toHaveBeenCalledWith('/organization/classes/1/head-teacher', data)
        })
      })

      describe('schoolClassApi object', () => {
        it('should have all API methods', () => {
          expect(schoolClassApi.getList).toBe(getClasses)
          expect(schoolClassApi.getById).toBe(getClass)
          expect(schoolClassApi.create).toBe(createClass)
          expect(schoolClassApi.update).toBe(updateClass)
          expect(schoolClassApi.delete).toBe(deleteClass)
          expect(schoolClassApi.activate).toBe(activateClass)
          expect(schoolClassApi.graduate).toBe(graduateClass)
          expect(schoolClassApi.dissolve).toBe(dissolveClass)
          expect(schoolClassApi.assignHeadTeacher).toBe(assignHeadTeacher)
        })
      })
    })
  })

  describe('API Error Handling', () => {
    it('should propagate errors from http client', async () => {
      const mockError = new Error('Network error')
      mockedHttp.get.mockRejectedValue(mockError)

      await expect(getStudents()).rejects.toThrow('Network error')
    })

    it('should propagate errors for POST requests', async () => {
      const mockError = new Error('Validation failed')
      mockedHttp.post.mockRejectedValue(mockError)

      await expect(createStudent({ studentNo: '', name: '', gender: 1, classId: 1 })).rejects.toThrow('Validation failed')
    })
  })
})
