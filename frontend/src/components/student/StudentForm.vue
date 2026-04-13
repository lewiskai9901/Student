<template>
  <div class="p-6">
    <!-- Loading 状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
    </div>

    <form v-else @submit.prevent="handleSubmit" class="space-y-6">
      <!-- Tab 导航 -->
      <div class="border-b border-gray-200">
        <nav class="-mb-px flex space-x-8">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            :class="[
              'whitespace-nowrap border-b-2 py-3 px-1 text-sm font-medium',
              activeTab === tab.key
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
            ]"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </nav>
      </div>

      <!-- 基本信息 Tab -->
      <div v-show="activeTab === 'basic'" class="space-y-6">
        <div class="rounded-lg border border-gray-200 bg-white">
          <div class="border-b border-gray-200 px-4 py-3">
            <h3 class="text-sm font-medium text-gray-900">基本信息</h3>
          </div>
          <div class="p-4">
            <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  学号
                </label>
                <input
                  v-model="formData.studentNo"
                  type="text"
                  placeholder="选填，不填则自动生成"
                  :disabled="mode === 'edit'"
                  class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  姓名 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="formData.realName"
                  type="text"
                  placeholder="请输入姓名"
                  class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  :class="{ 'border-red-500': errors.realName }"
                />
                <p v-if="errors.realName" class="mt-1 text-xs text-red-500">{{ errors.realName }}</p>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  性别 <span class="text-red-500">*</span>
                </label>
                <div class="flex items-center gap-6 h-9">
                  <label class="flex items-center cursor-pointer">
                    <input type="radio" v-model="formData.gender" :value="1" class="h-4 w-4 text-blue-600 focus:ring-blue-500" />
                    <span class="ml-2 text-sm text-gray-700">男</span>
                  </label>
                  <label class="flex items-center cursor-pointer">
                    <input type="radio" v-model="formData.gender" :value="2" class="h-4 w-4 text-blue-600 focus:ring-blue-500" />
                    <span class="ml-2 text-sm text-gray-700">女</span>
                  </label>
                </div>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">证件类型</label>
                <select v-model="formData.idCardType" class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500">
                  <option value="身份证">身份证</option>
                  <option value="护照">护照</option>
                  <option value="港澳通行证">港澳通行证</option>
                  <option value="其他">其他</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">身份证号</label>
                <input
                  v-model="formData.identityCard"
                  type="text"
                  placeholder="请输入身份证号"
                  class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">出生年月</label>
                <input
                  v-model="formData.birthDate"
                  type="date"
                  class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">民族</label>
                <select v-model="formData.ethnicity" class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500">
                  <option value="">请选择</option>
                  <option v-for="item in ethnicityOptions" :key="item" :value="item">{{ item }}</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">政治面貌</label>
                <select v-model="formData.politicalStatus" class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500">
                  <option value="">请选择</option>
                  <option v-for="item in politicalOptions" :key="item" :value="item">{{ item }}</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">联系方式</label>
                <input
                  v-model="formData.phone"
                  type="text"
                  placeholder="请输入联系电话"
                  class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 学籍信息 Tab -->
      <div v-show="activeTab === 'academic'" class="space-y-6">
        <div class="rounded-lg border border-gray-200 bg-white">
          <div class="border-b border-gray-200 px-4 py-3">
            <h3 class="text-sm font-medium text-gray-900">学籍信息</h3>
          </div>
          <div class="p-4">
            <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">招生年度</label>
                <input
                  v-model="formData.admissionDate"
                  type="date"
                  class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">年级</label>
                <select
                  v-model="formData.gradeId"
                  class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  @change="handleGradeChange"
                >
                  <option :value="null">请选择年级</option>
                  <option v-for="item in gradeList" :key="item.id" :value="item.id">{{ item.gradeName }}</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">专业</label>
                <select
                  v-model="formData.majorDirectionId"
                  class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  :disabled="!formData.gradeId"
                  @change="handleMajorDirectionChange"
                >
                  <option :value="null">{{ formData.gradeId ? '请选择专业' : '请先选择年级' }}</option>
                  <option v-for="item in gradeDirections" :key="item.majorDirectionId" :value="item.majorDirectionId">
                    {{ item.majorName }} - {{ item.directionName }}
                  </option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">所属部门</label>
                <select
                  v-model="formData.orgUnitId"
                  class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  @change="handleDepartmentChange"
                >
                  <option :value="null">请选择部门</option>
                  <option v-for="item in departmentList" :key="item.id" :value="item.id">{{ item.unitName }}</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">班级</label>
                <select
                  v-model="formData.orgUnitId"
                  class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  :disabled="!formData.orgUnitId"
                >
                  <option :value="null">{{ formData.orgUnitId ? '请选择班级' : '请先选择部门' }}</option>
                  <option v-for="item in filteredClassList" :key="item.id" :value="item.id">{{ item.className }}</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  层次
                  <span class="ml-1 text-xs font-normal text-gray-400">（可修改）</span>
                </label>
                <input
                  v-model="formData.educationLevel"
                  type="text"
                  :placeholder="formData.majorDirectionId ? '可根据实际情况修改' : '选择专业后自动填充，可自定义'"
                  class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  学制
                  <span class="ml-1 text-xs font-normal text-gray-400">（可修改）</span>
                </label>
                <input
                  v-model="formData.studyLength"
                  type="text"
                  :placeholder="formData.majorDirectionId ? '可根据实际情况修改' : '选择专业后自动填充，可自定义'"
                  class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">入学前学历</label>
                <select v-model="formData.degreeType" class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500">
                  <option value="">请选择</option>
                  <option value="初中">初中</option>
                  <option value="中职">中职</option>
                  <option value="技校">技校</option>
                  <option value="高中">高中</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">学生状态</label>
                <select v-model="formData.studentStatus" class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500">
                  <option :value="1">在校</option>
                  <option :value="2">休学</option>
                  <option :value="3">毕业</option>
                  <option :value="4">退学</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">预计毕业日期</label>
                <input v-model="formData.graduationDate" type="date" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 户籍信息 Tab -->
      <div v-show="activeTab === 'hukou'" class="space-y-6">
        <div class="rounded-lg border border-gray-200 bg-white">
          <div class="border-b border-gray-200 px-4 py-3">
            <h3 class="text-sm font-medium text-gray-900">户籍信息</h3>
          </div>
          <div class="p-4">
            <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">户口所在地-省</label>
                <input v-model="formData.hukouProvince" type="text" placeholder="请输入省份" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">户口所在地-市</label>
                <input v-model="formData.hukouCity" type="text" placeholder="请输入城市" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">户口所在地-区</label>
                <input v-model="formData.hukouDistrict" type="text" placeholder="请输入区县" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div class="md:col-span-2">
                <label class="mb-1.5 block text-sm font-medium text-gray-700">户口详细地址</label>
                <input v-model="formData.hukouAddress" type="text" placeholder="请输入详细地址" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">户口性质</label>
                <select v-model="formData.hukouType" class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500">
                  <option value="">请选择</option>
                  <option value="农业">农业</option>
                  <option value="非农业">非农业</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">邮政编码</label>
                <input v-model="formData.postalCode" type="text" placeholder="请输入邮编" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div class="md:col-span-3">
                <label class="mb-1.5 block text-sm font-medium text-gray-700">家庭住址</label>
                <input v-model="formData.homeAddress" type="text" placeholder="请输入家庭住址（如与户籍地址不同）" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
            </div>
          </div>
        </div>

        <!-- 资助信息 -->
        <div class="rounded-lg border border-gray-200 bg-white">
          <div class="border-b border-gray-200 px-4 py-3">
            <h3 class="text-sm font-medium text-gray-900">资助信息</h3>
          </div>
          <div class="p-4">
            <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">是否建档立卡</label>
                <div class="flex items-center gap-6 h-9">
                  <label class="flex items-center cursor-pointer">
                    <input type="radio" v-model="formData.isPovertyRegistered" :value="1" class="h-4 w-4 text-blue-600 focus:ring-blue-500" />
                    <span class="ml-2 text-sm text-gray-700">是</span>
                  </label>
                  <label class="flex items-center cursor-pointer">
                    <input type="radio" v-model="formData.isPovertyRegistered" :value="0" class="h-4 w-4 text-blue-600 focus:ring-blue-500" />
                    <span class="ml-2 text-sm text-gray-700">否</span>
                  </label>
                </div>
              </div>
              <div class="md:col-span-2">
                <label class="mb-1.5 block text-sm font-medium text-gray-700">资助申请类型</label>
                <select v-model="formData.financialAidType" class="h-9 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500">
                  <option value="">请选择</option>
                  <option value="无">无</option>
                  <option value="国家助学金">国家助学金</option>
                  <option value="校内助学金">校内助学金</option>
                  <option value="生源地贷款">生源地贷款</option>
                  <option value="其他">其他</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 家庭信息 Tab -->
      <div v-show="activeTab === 'family'" class="space-y-6">
        <!-- 父亲信息 -->
        <div class="rounded-lg border border-gray-200 bg-white">
          <div class="border-b border-gray-200 px-4 py-3">
            <h3 class="text-sm font-medium text-gray-900">父亲信息</h3>
          </div>
          <div class="p-4">
            <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">父亲姓名</label>
                <input v-model="formData.fatherName" type="text" placeholder="请输入父亲姓名" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">父亲身份证号</label>
                <input v-model="formData.fatherIdCard" type="text" placeholder="请输入身份证号" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">父亲电话</label>
                <input v-model="formData.fatherPhone" type="text" placeholder="请输入联系电话" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
            </div>
          </div>
        </div>

        <!-- 母亲信息 -->
        <div class="rounded-lg border border-gray-200 bg-white">
          <div class="border-b border-gray-200 px-4 py-3">
            <h3 class="text-sm font-medium text-gray-900">母亲信息</h3>
          </div>
          <div class="p-4">
            <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">母亲姓名</label>
                <input v-model="formData.motherName" type="text" placeholder="请输入母亲姓名" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">母亲身份证号</label>
                <input v-model="formData.motherIdCard" type="text" placeholder="请输入身份证号" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">母亲电话</label>
                <input v-model="formData.motherPhone" type="text" placeholder="请输入联系电话" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
            </div>
          </div>
        </div>

        <!-- 其他监护人信息 -->
        <div class="rounded-lg border border-gray-200 bg-white">
          <div class="border-b border-gray-200 px-4 py-3">
            <h3 class="text-sm font-medium text-gray-900">其他监护人信息（选填）</h3>
          </div>
          <div class="p-4">
            <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">监护人姓名</label>
                <input v-model="formData.guardianName" type="text" placeholder="请输入监护人姓名" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">监护人身份证号</label>
                <input v-model="formData.guardianIdCard" type="text" placeholder="请输入身份证号" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">监护人电话</label>
                <input v-model="formData.guardianPhone" type="text" placeholder="请输入联系电话" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">与学生关系</label>
                <input v-model="formData.guardianRelation" type="text" placeholder="请输入与学生关系" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 其他信息 Tab -->
      <div v-show="activeTab === 'other'" class="space-y-6">
        <div class="rounded-lg border border-gray-200 bg-white">
          <div class="border-b border-gray-200 px-4 py-3">
            <h3 class="text-sm font-medium text-gray-900">其他信息</h3>
          </div>
          <div class="p-4">
            <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">健康状况</label>
                <input v-model="formData.healthStatus" type="text" placeholder="请输入健康状况" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">过敏史</label>
                <input v-model="formData.allergies" type="text" placeholder="请输入过敏史" class="h-9 w-full rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              </div>
              <div class="md:col-span-2">
                <label class="mb-1.5 block text-sm font-medium text-gray-700">备注</label>
                <textarea v-model="formData.specialNotes" rows="3" class="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" placeholder="请输入备注信息"></textarea>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="flex items-center justify-end gap-3 pt-2">
        <button
          type="button"
          class="h-9 rounded-md border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          @click="$emit('close')"
        >
          取消
        </button>
        <button
          type="submit"
          :disabled="submitting"
          class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          <span v-if="submitting" class="mr-1.5 inline-block h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
          {{ mode === 'add' ? '确定新增' : '确定修改' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStudent, createStudent, updateStudent } from '@/api/student'
import { getClassList, getOrgUnitTree } from '@/api/organization'
// 年级和专业方向暂保留V1
import { getAllCohorts } from '@/api/organization'
import { getDirectionsByYear } from '@/api/gradeMajorDirection'

interface Props {
  mode: 'add' | 'edit'
  studentId?: number | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  success: []
  close: []
}>()

// Tab 配置
const tabs = [
  { key: 'basic', label: '基本信息' },
  { key: 'academic', label: '学籍信息' },
  { key: 'hukou', label: '户籍与资助' },
  { key: 'family', label: '家庭信息' },
  { key: 'other', label: '其他信息' }
]
const activeTab = ref('basic')

// 民族选项
const ethnicityOptions = [
  '汉族', '蒙古族', '回族', '藏族', '维吾尔族', '苗族', '彝族', '壮族', '布依族', '朝鲜族',
  '满族', '侗族', '瑶族', '白族', '土家族', '哈尼族', '哈萨克族', '傣族', '黎族', '傈僳族',
  '佤族', '畲族', '高山族', '拉祜族', '水族', '东乡族', '纳西族', '景颇族', '柯尔克孜族',
  '土族', '达斡尔族', '仫佬族', '羌族', '布朗族', '撒拉族', '毛南族', '仡佬族', '锡伯族',
  '阿昌族', '普米族', '塔吉克族', '怒族', '乌孜别克族', '俄罗斯族', '鄂温克族', '德昂族',
  '保安族', '裕固族', '京族', '塔塔尔族', '独龙族', '鄂伦春族', '赫哲族', '门巴族', '珞巴族', '基诺族'
]

// 政治面貌选项
const politicalOptions = ['群众', '共青团员', '中共党员', '中共预备党员', '民主党派', '无党派人士']

// 加载状态
const loading = ref(false)
const submitting = ref(false)

// 数据列表
const gradeList = ref<any[]>([])
const classList = ref<any[]>([])
const gradeDirections = ref<any[]>([])
const departmentList = ref<any[]>([])

// 扁平化部门树为列表
const flattenDepartments = (nodes: any[], result: any[] = []): any[] => {
  for (const node of nodes) {
    result.push(node)
    if (node.children && node.children.length > 0) {
      flattenDepartments(node.children, result)
    }
  }
  return result
}

// 根据选中部门过滤的班级列表
const filteredClassList = computed(() => {
  // 必须先选择部门才能显示班级
  if (!formData.orgUnitId) return []

  // 根据部门（orgUnitId）过滤班级
  return classList.value.filter(c => {
    return String(c.orgUnitId) === String(formData.orgUnitId)
  })
})

// 表单数据
const formData = reactive<any>({
  studentNo: '',
  idCardType: '身份证',
  ethnicity: '',
  politicalStatus: '',
  realName: '',
  gender: 1,
  birthDate: '',
  identityCard: '',
  phone: '',
  homeAddress: '',
  hukouProvince: '',
  hukouCity: '',
  hukouDistrict: '',
  hukouAddress: '',
  hukouType: '',
  postalCode: '',
  isPovertyRegistered: 0,
  financialAidType: '',
  orgUnitId: null,
  gradeId: null,
  majorId: null,
  majorDirectionId: null,
  educationLevel: '',
  studyLength: '',
  degreeType: '',
  admissionDate: null,
  graduationDate: null,
  studentStatus: 1,
  fatherName: '',
  fatherIdCard: '',
  fatherPhone: '',
  motherName: '',
  motherIdCard: '',
  motherPhone: '',
  guardianName: '',
  guardianIdCard: '',
  guardianPhone: '',
  guardianRelation: '',
  healthStatus: '',
  allergies: '',
  specialNotes: ''
})

// 错误信息
const errors = reactive<Record<string, string>>({})

// 部门变更处理
const handleDepartmentChange = () => {
  // 清空班级选择
  formData.orgUnitId = null
}

// 年级变更处理
const handleGradeChange = async () => {
  formData.orgUnitId = null
  formData.majorDirectionId = null
  formData.educationLevel = ''
  formData.studyLength = ''
  gradeDirections.value = []

  if (formData.gradeId) {
    // 从年级列表中获取选中年级的 enrollmentYear
    const selectedGrade = gradeList.value.find(g => g.id === formData.gradeId)
    if (selectedGrade?.enrollmentYear) {
      await loadGradeDirections(selectedGrade.enrollmentYear)
    }
  }
}

// 加载年级的专业配置
const loadGradeDirections = async (academicYear: number) => {
  try {
    // 使用学年（如2025）查询专业方向配置
    const data = await getDirectionsByYear(academicYear)
    gradeDirections.value = data || []
  } catch (error) {
    console.error('加载年级专业配置失败:', error)
    gradeDirections.value = []
  }
}

// 专业方向变更处理
const handleMajorDirectionChange = () => {
  if (formData.majorDirectionId) {
    const selected = gradeDirections.value.find(d => String(d.majorDirectionId) === String(formData.majorDirectionId))
    if (selected) {
      // 处理分段培养情况
      if (selected.isSegmented === 1) {
        // 分段培养：显示两个阶段的层次和学制
        formData.educationLevel = `${selected.phase1Level || ''} → ${selected.phase2Level || ''}`
        formData.studyLength = `${selected.phase1Years || 0}+${selected.phase2Years || 0}年`
      } else {
        // 非分段培养：直接显示
        formData.educationLevel = selected.level || ''
        formData.studyLength = selected.years ? `${selected.years}年` : ''
      }
    }
  } else {
    formData.educationLevel = ''
    formData.studyLength = ''
  }
}

// 验证表单 - 只验证姓名和性别为必填项
const validateForm = () => {
  Object.keys(errors).forEach(key => { errors[key] = '' })
  let isValid = true

  // 姓名必填
  if (!formData.realName?.trim()) {
    errors.realName = '请输入姓名'
    isValid = false
  }

  // 性别必填（已有默认值，通常不会触发）
  if (formData.gender === null || formData.gender === undefined) {
    isValid = false
  }

  return isValid
}

// 加载数据
const loadDepartmentListData = async () => {
  try {
    const tree = await getOrgUnitTree()
    // 扁平化部门树，只保留教学单位
    departmentList.value = flattenDepartments(tree || [])
  } catch (error) {
    console.error('加载部门列表失败:', error)
  }
}

const loadGradeListData = async () => {
  try {
    const response = await getAllCohorts()
    gradeList.value = response || []
  } catch (error) {
    console.error('加载年级列表失败:', error)
  }
}

const loadClassListData = async () => {
  try {
    // getClassList 已经返回 records 数组，不需要再访问 .records
    const classes = await getClassList({ pageNum: 1, pageSize: 1000 })
    classList.value = classes || []
  } catch (error) {
    console.error('加载班级列表失败:', error)
  }
}

const loadStudentDetail = async () => {
  if (props.mode === 'add' || !props.studentId) return

  loading.value = true
  try {
    const student = await getStudent(props.studentId)
    Object.assign(formData, student)
    // 如果有年级，加载对应的专业配置
    if (formData.gradeId) {
      // 从年级列表中获取选中年级的 enrollmentYear
      const selectedGrade = gradeList.value.find(g => g.id === formData.gradeId)
      if (selectedGrade?.enrollmentYear) {
        await loadGradeDirections(selectedGrade.enrollmentYear)
      }
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载学生详情失败')
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!validateForm()) {
    ElMessage.warning('请完善必填信息')
    return
  }

  submitting.value = true

  try {
    const submitData = { ...formData }

    if (props.mode === 'add') {
      submitData.username = formData.studentNo
      submitData.password = '123456'
      await createStudent(submitData)
      ElMessage.success('学生创建成功')
    } else {
      submitData.id = props.studentId!
      await updateStudent(props.studentId!, submitData)
      ElMessage.success('学生信息更新成功')
    }

    emit('success')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || `学生${props.mode === 'add' ? '创建' : '更新'}失败`)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadDepartmentListData()
  loadGradeListData()
  loadClassListData()
  loadStudentDetail()
})
</script>
