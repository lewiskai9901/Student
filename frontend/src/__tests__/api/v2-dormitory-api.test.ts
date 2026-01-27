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

import { http } from '@/utils/request'
import {
  getDormitories,
  getDormitory,
  createDormitory,
  updateDormitory,
  deleteDormitory,
  deleteDormitories,
  batchCreateDormitories,
  getDormitoriesByBuilding,
  getDormitoriesByGender,
  getDormitoriesByDepartment,
  getAvailableDormitories,
  existsDormitoryNo,
  updateDormitoryStatus,
  assignBed,
  releaseBed,
  getBedAllocations,
  assignStudentToDormitory,
  removeStudentFromDormitory,
  swapStudentDormitory,
  batchUpdateDepartment,
  getBuildings,
  getBuilding,
  createBuilding,
  updateBuilding,
  deleteBuilding,
  getAllEnabledBuildings,
  dormitoryApi,
  buildingApi
} from '@/api/dormitory'

const mockedHttp = vi.mocked(http)

/**
 * V2 宿舍管理 API 测试
 */
describe('V2 Dormitory API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Dormitory CRUD', () => {
    describe('getDormitories', () => {
      it('should call GET /dormitory/rooms with params', async () => {
        const mockResponse = {
          records: [{ id: 1, dormitoryNo: '101', buildingId: 1 }],
          total: 1,
          size: 10,
          current: 1,
          pages: 1
        }
        mockedHttp.get.mockResolvedValue(mockResponse)

        const params = { pageNum: 1, pageSize: 10, buildingId: 1 }
        const result = await getDormitories(params)

        expect(mockedHttp.get).toHaveBeenCalledWith('/dormitory/rooms', { params })
        expect(result).toEqual(mockResponse)
      })
    })

    describe('getDormitory', () => {
      it('should call GET /dormitory/rooms/:id', async () => {
        const mockDormitory = { id: 1, dormitoryNo: '101', bedCapacity: 6 }
        mockedHttp.get.mockResolvedValue(mockDormitory)

        const result = await getDormitory(1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/dormitory/rooms/1')
        expect(result).toEqual(mockDormitory)
      })
    })

    describe('createDormitory', () => {
      it('should call POST /dormitory/rooms with data', async () => {
        mockedHttp.post.mockResolvedValue(1)

        const data = {
          buildingId: 1,
          dormitoryNo: '101',
          floorNumber: 1,
          roomUsageType: 1,
          bedCapacity: 6
        }
        const result = await createDormitory(data)

        expect(mockedHttp.post).toHaveBeenCalledWith('/dormitory/rooms', data)
        expect(result).toBe(1)
      })
    })

    describe('updateDormitory', () => {
      it('should call PUT /dormitory/rooms/:id with data', async () => {
        mockedHttp.put.mockResolvedValue(undefined)

        const data = { bedCapacity: 8 }
        await updateDormitory(1, data)

        expect(mockedHttp.put).toHaveBeenCalledWith('/dormitory/rooms/1', data)
      })
    })

    describe('deleteDormitory', () => {
      it('should call DELETE /dormitory/rooms/:id with force=false', async () => {
        mockedHttp.delete.mockResolvedValue(undefined)

        await deleteDormitory(1)

        expect(mockedHttp.delete).toHaveBeenCalledWith('/dormitory/rooms/1', { params: { force: false } })
      })

      it('should call DELETE with force=true when specified', async () => {
        mockedHttp.delete.mockResolvedValue(undefined)

        await deleteDormitory(1, true)

        expect(mockedHttp.delete).toHaveBeenCalledWith('/dormitory/rooms/1', { params: { force: true } })
      })
    })

    describe('deleteDormitories', () => {
      it('should call DELETE /dormitory/rooms/batch with ids', async () => {
        mockedHttp.delete.mockResolvedValue(undefined)

        await deleteDormitories([1, 2, 3])

        expect(mockedHttp.delete).toHaveBeenCalledWith('/dormitory/rooms/batch', { data: [1, 2, 3] })
      })
    })

    describe('batchCreateDormitories', () => {
      it('should call POST /dormitory/rooms/batch with data', async () => {
        mockedHttp.post.mockResolvedValue(10)

        const data = {
          buildingId: 1,
          floorStart: 1,
          floorEnd: 5,
          roomsPerFloor: 10,
          bedCapacity: 6
        }
        const result = await batchCreateDormitories(data)

        expect(mockedHttp.post).toHaveBeenCalledWith('/dormitory/rooms/batch', data)
        expect(result).toBe(10)
      })
    })
  })

  describe('Dormitory Query', () => {
    describe('getDormitoriesByBuilding', () => {
      it('should call GET /dormitory/rooms/by-building/:buildingId', async () => {
        const mockDormitories = [{ id: 1, dormitoryNo: '101' }]
        mockedHttp.get.mockResolvedValue(mockDormitories)

        const result = await getDormitoriesByBuilding(1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/dormitory/rooms/by-building/1')
        expect(result).toEqual(mockDormitories)
      })
    })

    describe('getDormitoriesByGender', () => {
      it('should call GET /dormitory/rooms/by-gender/:genderType', async () => {
        const mockDormitories = [{ id: 1, genderType: 1 }]
        mockedHttp.get.mockResolvedValue(mockDormitories)

        const result = await getDormitoriesByGender(1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/dormitory/rooms/by-gender/1')
        expect(result).toEqual(mockDormitories)
      })
    })

    describe('getDormitoriesByDepartment', () => {
      it('should call GET /dormitory/rooms/by-department with params', async () => {
        const mockDormitories = [{ id: 1 }]
        mockedHttp.get.mockResolvedValue(mockDormitories)

        const result = await getDormitoriesByDepartment(100)

        expect(mockedHttp.get).toHaveBeenCalledWith('/dormitory/rooms/by-department', { params: { departmentId: 100 } })
        expect(result).toEqual(mockDormitories)
      })
    })

    describe('getAvailableDormitories', () => {
      it('should call GET /dormitory/rooms/available', async () => {
        const mockDormitories = [{ id: 1, occupiedBeds: 2, bedCapacity: 6 }]
        mockedHttp.get.mockResolvedValue(mockDormitories)

        const result = await getAvailableDormitories(1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/dormitory/rooms/available', { params: { genderType: 1 } })
        expect(result).toEqual(mockDormitories)
      })
    })

    describe('existsDormitoryNo', () => {
      it('should call GET /dormitory/rooms/exists with params', async () => {
        mockedHttp.get.mockResolvedValue(false)

        const result = await existsDormitoryNo(1, '101')

        expect(mockedHttp.get).toHaveBeenCalledWith('/dormitory/rooms/exists', {
          params: { buildingId: 1, dormitoryNo: '101', excludeId: undefined }
        })
        expect(result).toBe(false)
      })
    })
  })

  describe('Dormitory Status', () => {
    describe('updateDormitoryStatus', () => {
      it('should call PATCH /dormitory/rooms/:id/status', async () => {
        mockedHttp.patch.mockResolvedValue(undefined)

        await updateDormitoryStatus(1, 2)

        expect(mockedHttp.patch).toHaveBeenCalledWith('/dormitory/rooms/1/status', null, { params: { status: 2 } })
      })
    })
  })

  describe('Bed Management', () => {
    describe('assignBed', () => {
      it('should call POST /dormitory/rooms/:id/assign-bed', async () => {
        mockedHttp.post.mockResolvedValue(undefined)

        await assignBed(1, 100)

        expect(mockedHttp.post).toHaveBeenCalledWith('/dormitory/rooms/1/assign-bed', null, { params: { studentId: 100 } })
      })
    })

    describe('releaseBed', () => {
      it('should call POST /dormitory/rooms/:id/release-bed', async () => {
        mockedHttp.post.mockResolvedValue(undefined)

        await releaseBed(1, 100)

        expect(mockedHttp.post).toHaveBeenCalledWith('/dormitory/rooms/1/release-bed', null, { params: { studentId: 100 } })
      })
    })

    describe('getBedAllocations', () => {
      it('should call GET /dormitory/rooms/:id/bed-allocations', async () => {
        const mockAllocations = [
          { bedNumber: '1', isAssigned: true, studentId: 100, studentName: '张三' }
        ]
        mockedHttp.get.mockResolvedValue(mockAllocations)

        const result = await getBedAllocations(1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/dormitory/rooms/1/bed-allocations')
        expect(result).toEqual(mockAllocations)
      })
    })

    describe('assignStudentToDormitory', () => {
      it('should call POST /dormitory/rooms/assign-student', async () => {
        mockedHttp.post.mockResolvedValue(undefined)

        const data = { studentId: 100, dormitoryId: 1, bedNo: '1' }
        await assignStudentToDormitory(data)

        expect(mockedHttp.post).toHaveBeenCalledWith('/dormitory/rooms/assign-student', data)
      })
    })

    describe('removeStudentFromDormitory', () => {
      it('should call DELETE /dormitory/rooms/remove-student/:studentId', async () => {
        mockedHttp.delete.mockResolvedValue(undefined)

        await removeStudentFromDormitory(100)

        expect(mockedHttp.delete).toHaveBeenCalledWith('/dormitory/rooms/remove-student/100')
      })
    })

    describe('swapStudentDormitory', () => {
      it('should call POST /dormitory/rooms/swap-students', async () => {
        mockedHttp.post.mockResolvedValue(undefined)

        const data = { studentId1: 100, studentId2: 101 }
        await swapStudentDormitory(data)

        expect(mockedHttp.post).toHaveBeenCalledWith('/dormitory/rooms/swap-students', data)
      })
    })
  })

  describe('Batch Operations', () => {
    describe('batchUpdateDepartment', () => {
      it('should call PUT /dormitory/rooms/batch-department', async () => {
        mockedHttp.put.mockResolvedValue(5)

        const data = { dormitoryIds: [1, 2, 3], departmentId: 100 }
        const result = await batchUpdateDepartment(data)

        expect(mockedHttp.put).toHaveBeenCalledWith('/dormitory/rooms/batch-department', data)
        expect(result).toBe(5)
      })
    })
  })

  describe('Building API', () => {
    describe('getBuildings', () => {
      it('should call GET /teaching/buildings with params', async () => {
        const mockResponse = {
          records: [{ id: 1, buildingNo: 'A', buildingName: 'A栋' }],
          total: 1
        }
        mockedHttp.get.mockResolvedValue(mockResponse)

        const params = { pageNum: 1, pageSize: 10 }
        const result = await getBuildings(params)

        expect(mockedHttp.get).toHaveBeenCalledWith('/teaching/buildings', { params })
        expect(result).toEqual(mockResponse)
      })
    })

    describe('getBuilding', () => {
      it('should call GET /teaching/buildings/:id', async () => {
        const mockBuilding = { id: 1, buildingNo: 'A', buildingName: 'A栋' }
        mockedHttp.get.mockResolvedValue(mockBuilding)

        const result = await getBuilding(1)

        expect(mockedHttp.get).toHaveBeenCalledWith('/teaching/buildings/1')
        expect(result).toEqual(mockBuilding)
      })
    })

    describe('createBuilding', () => {
      it('should call POST /teaching/buildings', async () => {
        const mockBuilding = { id: 1, buildingNo: 'B', buildingName: 'B栋' }
        mockedHttp.post.mockResolvedValue(mockBuilding)

        const data = { buildingNo: 'B', buildingName: 'B栋', buildingType: 2, floorCount: 6 }
        const result = await createBuilding(data)

        expect(mockedHttp.post).toHaveBeenCalledWith('/teaching/buildings', data)
        expect(result).toEqual(mockBuilding)
      })
    })

    describe('updateBuilding', () => {
      it('should call PUT /teaching/buildings/:id', async () => {
        const mockBuilding = { id: 1, buildingName: 'B栋(更新)' }
        mockedHttp.put.mockResolvedValue(mockBuilding)

        const data = { buildingName: 'B栋(更新)' }
        const result = await updateBuilding(1, data)

        expect(mockedHttp.put).toHaveBeenCalledWith('/teaching/buildings/1', data)
        expect(result).toEqual(mockBuilding)
      })
    })

    describe('deleteBuilding', () => {
      it('should call DELETE /teaching/buildings/:id', async () => {
        mockedHttp.delete.mockResolvedValue(undefined)

        await deleteBuilding(1)

        expect(mockedHttp.delete).toHaveBeenCalledWith('/teaching/buildings/1')
      })
    })

    describe('getAllEnabledBuildings', () => {
      it('should call GET /teaching/buildings/enabled', async () => {
        const mockBuildings = [{ id: 1, status: 1 }]
        mockedHttp.get.mockResolvedValue(mockBuildings)

        const result = await getAllEnabledBuildings(2)

        expect(mockedHttp.get).toHaveBeenCalledWith('/teaching/buildings/enabled', { params: { buildingType: 2 } })
        expect(result).toEqual(mockBuildings)
      })
    })
  })

  describe('API Objects', () => {
    it('dormitoryApi should have all methods', () => {
      expect(dormitoryApi.getList).toBe(getDormitories)
      expect(dormitoryApi.getById).toBe(getDormitory)
      expect(dormitoryApi.create).toBe(createDormitory)
      expect(dormitoryApi.update).toBe(updateDormitory)
      expect(dormitoryApi.delete).toBe(deleteDormitory)
      expect(dormitoryApi.getByBuilding).toBe(getDormitoriesByBuilding)
      expect(dormitoryApi.updateStatus).toBe(updateDormitoryStatus)
      expect(dormitoryApi.assignBed).toBe(assignBed)
      expect(dormitoryApi.releaseBed).toBe(releaseBed)
      expect(dormitoryApi.getBedAllocations).toBe(getBedAllocations)
    })

    it('buildingApi should have all methods', () => {
      expect(buildingApi.getList).toBe(getBuildings)
      expect(buildingApi.getById).toBe(getBuilding)
      expect(buildingApi.create).toBe(createBuilding)
      expect(buildingApi.update).toBe(updateBuilding)
      expect(buildingApi.delete).toBe(deleteBuilding)
      expect(buildingApi.getAllEnabled).toBe(getAllEnabledBuildings)
    })
  })
})
