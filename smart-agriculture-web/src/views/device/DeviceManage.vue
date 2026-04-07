<template>
  <div class="page-shell device-page">
    <section class="page-banner">
      <div class="page-banner__main">
        <span class="page-banner__badge">设备资产与状态管理</span>
        <h1 class="page-banner__title">设备管理</h1>
        <p class="page-banner__desc">统一管理物联网设备接入、在线状态与运行状态。</p>
      </div>

      <div class="page-banner__actions">
        <div class="page-banner__meta">
          <span class="meta-label">在线率</span>
          <strong>{{ onlineRate }}%</strong>
        </div>
        <el-button type="primary" @click="openCreateDialog">新增设备</el-button>
      </div>
    </section>

    <section class="page-overview-grid">
      <article v-for="item in overviewCards" :key="item.label" class="page-overview-card">
        <span class="page-overview-card__label">{{ item.label }}</span>
        <strong class="page-overview-card__value">{{ item.value }}</strong>
        <p class="page-overview-card__desc">{{ item.desc }}</p>
      </article>
    </section>

    <section class="page-grid-two">
      <el-card class="page-grid-card" shadow="never">
        <template #header>
          <div class="page-panel-title">
            <strong>设备类型分布</strong>
            <span class="page-panel-tip">按当前筛选条件统计设备类型结构。</span>
          </div>
        </template>

        <div class="page-summary-list">
          <div v-for="item in deviceTypeStats" :key="item.name" class="page-summary-item">
            <div>
              <span class="page-summary-item__title">{{ item.name }}</span>
              <span class="page-summary-item__desc">{{ item.count }} 台设备</span>
            </div>
            <span class="page-summary-item__value">{{ item.percent }}%</span>
          </div>
        </div>
      </el-card>

      <el-card class="page-grid-card" shadow="never">
        <template #header>
          <div class="page-panel-title">
            <strong>运行状态摘要</strong>
            <span class="page-panel-tip">聚合在线、故障、运行中的设备状态。</span>
          </div>
        </template>

        <div class="status-summary-grid">
          <article class="status-box">
            <span class="status-box__label">在线设备</span>
            <strong class="status-box__value">{{ onlineCount }} 台</strong>
            <p class="status-box__desc">采集链路正常</p>
          </article>
          <article class="status-box warning">
            <span class="status-box__label">异常设备</span>
            <strong class="status-box__value">{{ abnormalCount }} 台</strong>
            <p class="status-box__desc">优先排查</p>
          </article>
          <article class="status-box info">
            <span class="status-box__label">运行中设备</span>
            <strong class="status-box__value">{{ runningCount }} 台</strong>
            <p class="status-box__desc">当前可联动</p>
          </article>
        </div>
      </el-card>
    </section>

    <el-card class="page-filter-card" shadow="never">
      <template #header>
        <div class="page-panel-title">
          <strong>筛选条件</strong>
          <span class="page-panel-tip">按设备类型、所属农田、在线状态筛选设备。</span>
        </div>
      </template>

      <el-form :model="queryForm" inline class="page-filter-form">
        <el-form-item label="设备类型">
          <el-input
            v-model="queryForm.deviceType"
            placeholder="例如：温湿度传感器、灌溉控制器"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="所属农田">
          <el-select v-model="queryForm.farmlandId" placeholder="请选择农田" clearable style="width: 220px">
            <el-option
              v-for="item in farmlandOptions"
              :key="item.id"
              :label="item.fieldName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="在线状态">
          <el-select v-model="queryForm.onlineStatus" placeholder="请选择状态" clearable style="width: 180px">
            <el-option
              v-for="item in deviceOnlineStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="page-toolbar">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="page-content-card" shadow="never">
      <template #header>
        <div class="page-panel-title">
          <strong>设备列表</strong>
          <span class="page-panel-tip">共 {{ pagination.total }} 条设备数据。</span>
        </div>
      </template>

      <div class="page-table-shell">
        <el-table v-loading="loading" :data="tableData" class="device-table">
          <el-table-column prop="id" label="编号" width="80" />
          <el-table-column prop="deviceCode" label="设备编码" width="148" />
          <el-table-column prop="deviceName" label="设备名称" min-width="168" />
          <el-table-column prop="deviceType" label="设备类型" width="148" />
          <el-table-column prop="farmlandName" label="所属农田" min-width="150" />
          <el-table-column label="安装时间" width="168" align="center">
            <template #default="{ row }">{{ row.installTime || '--' }}</template>
          </el-table-column>
          <el-table-column label="在线状态" width="118" align="center">
            <template #default="{ row }">
              <el-tag class="status-tag online" :type="getOptionTagType(deviceOnlineStatusOptions, row.onlineStatus)" effect="light">
                {{ row.onlineStatusName || getOptionLabel(deviceOnlineStatusOptions, row.onlineStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="运行状态" width="118" align="center">
            <template #default="{ row }">
              <el-tag class="status-tag run" :type="getOptionTagType(deviceRunStatusOptions, row.runStatus)" effect="light">
                {{ row.runStatusName || getOptionLabel(deviceRunStatusOptions, row.runStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <div class="action-group">
                <el-button link type="primary" @click="handleView(row)">查看</el-button>
                <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchTableData"
          @current-change="fetchTableData"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增设备' : '编辑设备'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设备编码" prop="deviceCode">
              <el-input v-model="formData.deviceCode" placeholder="请输入设备编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="deviceName">
              <el-input v-model="formData.deviceName" placeholder="请输入设备名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设备类型" prop="deviceType">
              <el-input v-model="formData.deviceType" placeholder="请输入设备类型" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属农田" prop="farmlandId">
              <el-select v-model="formData.farmlandId" placeholder="请选择农田" style="width: 100%">
                <el-option
                  v-for="item in farmlandOptions"
                  :key="item.id"
                  :label="item.fieldName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="安装时间" prop="installTime">
              <el-date-picker
                v-model="formData.installTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择安装时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="在线状态" prop="onlineStatus">
              <el-select v-model="formData.onlineStatus" placeholder="请选择在线状态" style="width: 100%">
                <el-option
                  v-for="item in deviceOnlineStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="运行状态" prop="runStatus">
              <el-select v-model="formData.runStatus" placeholder="请选择运行状态" style="width: 100%">
                <el-option
                  v-for="item in deviceRunStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="formData.remark" placeholder="请输入设备备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="设备详情" size="520px">
      <div v-loading="detailLoading" class="detail-panel">
        <section class="drawer-banner">
          <div>
            <h3>{{ detailData.deviceName || '未命名设备' }}</h3>
            <p>{{ detailData.farmlandName || '暂无所属农田信息' }}</p>
          </div>
          <div class="drawer-tags">
            <el-tag :type="getOptionTagType(deviceOnlineStatusOptions, detailData.onlineStatus)" effect="light">
              {{ detailData.onlineStatusName || getOptionLabel(deviceOnlineStatusOptions, detailData.onlineStatus) }}
            </el-tag>
            <el-tag :type="getOptionTagType(deviceRunStatusOptions, detailData.runStatus)" effect="light">
              {{ detailData.runStatusName || getOptionLabel(deviceRunStatusOptions, detailData.runStatus) }}
            </el-tag>
          </div>
        </section>

        <div class="detail-grid">
          <div class="detail-item">
            <span>设备编号</span>
            <strong>{{ detailData.id || '--' }}</strong>
          </div>
          <div class="detail-item">
            <span>设备编码</span>
            <strong>{{ detailData.deviceCode || '--' }}</strong>
          </div>
          <div class="detail-item">
            <span>设备类型</span>
            <strong>{{ detailData.deviceType || '--' }}</strong>
          </div>
          <div class="detail-item">
            <span>安装时间</span>
            <strong>{{ detailData.installTime || '--' }}</strong>
          </div>
          <div class="detail-item">
            <span>创建时间</span>
            <strong>{{ detailData.createTime || '--' }}</strong>
          </div>
          <div class="detail-item">
            <span>更新时间</span>
            <strong>{{ detailData.updateTime || '--' }}</strong>
          </div>
          <div class="detail-item wide">
            <span>备注</span>
            <strong>{{ detailData.remark || '--' }}</strong>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFieldPageApi } from '@/api/field'
import { createDeviceApi, deleteDeviceApi, getDeviceDetailApi, getDevicePageApi, updateDeviceApi } from '@/api/device'
import {
  deviceOnlineStatusOptions,
  deviceRunStatusOptions,
  getOptionLabel,
  getOptionTagType
} from '@/utils/agriDisplay'

const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const submitLoading = ref(false)
const detailLoading = ref(false)
const dialogMode = ref('create')
const formRef = ref()

const farmlandOptions = ref([])
const tableData = ref([])
const overviewData = ref([])

const queryForm = reactive({
  deviceType: '',
  farmlandId: undefined,
  onlineStatus: undefined
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const formData = reactive({
  id: undefined,
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  farmlandId: undefined,
  installTime: '',
  onlineStatus: 1,
  runStatus: 1,
  remark: ''
})

const detailData = reactive({
  id: undefined,
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  farmlandName: '',
  installTime: '',
  onlineStatus: undefined,
  onlineStatusName: '',
  runStatus: undefined,
  runStatusName: '',
  remark: '',
  createTime: '',
  updateTime: ''
})

const formRules = {
  deviceCode: [
    { required: true, message: '请输入设备编码', trigger: 'blur' },
    { min: 4, max: 30, message: '设备编码长度应在 4 到 30 个字符之间', trigger: 'blur' }
  ],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请输入设备类型', trigger: 'blur' }],
  farmlandId: [{ required: true, message: '请选择所属农田', trigger: 'change' }],
  installTime: [{ required: true, message: '请选择安装时间', trigger: 'change' }],
  onlineStatus: [{ required: true, message: '请选择在线状态', trigger: 'change' }],
  runStatus: [{ required: true, message: '请选择运行状态', trigger: 'change' }]
}

const totalCount = computed(() => overviewData.value.length)
const onlineCount = computed(() => overviewData.value.filter((item) => item.onlineStatus === 1).length)
const abnormalCount = computed(() =>
  overviewData.value.filter((item) => item.onlineStatus === 2 || item.runStatus === 3).length
)
const controlCount = computed(() =>
  overviewData.value.filter((item) =>
    ['控制', '灌溉', '喷灌', '控制器'].some((keyword) => String(item.deviceType || '').includes(keyword))
  ).length
)
const runningCount = computed(() => overviewData.value.filter((item) => item.runStatus === 1).length)
const onlineRate = computed(() =>
  totalCount.value ? Math.round((onlineCount.value / totalCount.value) * 100) : 0
)

const overviewCards = computed(() => [
  {
    label: '设备总数',
    value: `${totalCount.value} 台`,
    desc: '当前筛选条件下的设备数量。'
  },
  {
    label: '在线设备',
    value: `${onlineCount.value} 台`,
    desc: `在线率 ${onlineRate.value}%。`
  },
  {
    label: '异常设备',
    value: `${abnormalCount.value} 台`,
    desc: '故障或运行异常需优先排查。'
  },
  {
    label: '控制类设备',
    value: `${controlCount.value} 台`,
    desc: '用于灌溉与联动控制。'
  }
])

const deviceTypeStats = computed(() => {
  const total = totalCount.value || 1
  const counter = overviewData.value.reduce((map, item) => {
    const key = item.deviceType || '未标注'
    map[key] = (map[key] || 0) + 1
    return map
  }, {})

  return Object.entries(counter)
    .map(([name, count]) => ({
      name,
      count,
      percent: Math.round((count / total) * 100)
    }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 4)
})

function resetFormData() {
  Object.assign(formData, {
    id: undefined,
    deviceCode: '',
    deviceName: '',
    deviceType: '',
    farmlandId: undefined,
    installTime: '',
    onlineStatus: 1,
    runStatus: 1,
    remark: ''
  })
}

async function fetchFarmlandOptions() {
  const res = await getFieldPageApi({ current: 1, size: 100 })
  farmlandOptions.value = res.records || []
}

async function fetchTableData() {
  loading.value = true
  try {
    const params = {
      deviceType: queryForm.deviceType || undefined,
      farmlandId: queryForm.farmlandId,
      onlineStatus: queryForm.onlineStatus
    }

    const [pageRes, overviewRes] = await Promise.all([
      getDevicePageApi({
        current: pagination.current,
        size: pagination.size,
        ...params
      }),
      getDevicePageApi({
        current: 1,
        size: 100,
        ...params
      })
    ])

    tableData.value = pageRes.records || []
    pagination.total = pageRes.total || 0
    overviewData.value = overviewRes.records || []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchTableData()
}

function handleReset() {
  queryForm.deviceType = ''
  queryForm.farmlandId = undefined
  queryForm.onlineStatus = undefined
  pagination.current = 1
  fetchTableData()
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetFormData()
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogMode.value = 'edit'
  dialogVisible.value = true
  Object.assign(formData, {
    id: row.id,
    deviceCode: row.deviceCode,
    deviceName: row.deviceName,
    deviceType: row.deviceType,
    farmlandId: row.farmlandId,
    installTime: row.installTime,
    onlineStatus: row.onlineStatus,
    runStatus: row.runStatus,
    remark: row.remark || ''
  })
}

async function handleView(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    Object.assign(detailData, await getDeviceDetailApi(row.id))
  } finally {
    detailLoading.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除设备“${row.deviceName}”吗？删除后不可恢复。`, '删除确认', {
    type: 'warning'
  })
  await deleteDeviceApi(row.id)
  ElMessage.success('删除成功')
  if (tableData.value.length === 1 && pagination.current > 1) {
    pagination.current -= 1
  }
  fetchTableData()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    const payload = {
      deviceCode: formData.deviceCode,
      deviceName: formData.deviceName,
      deviceType: formData.deviceType,
      farmlandId: formData.farmlandId,
      installTime: formData.installTime,
      onlineStatus: formData.onlineStatus,
      runStatus: formData.runStatus,
      remark: formData.remark
    }

    if (dialogMode.value === 'create') {
      await createDeviceApi(payload)
      ElMessage.success('新增设备成功')
    } else {
      await updateDeviceApi(formData.id, payload)
      ElMessage.success('更新设备成功')
    }

    dialogVisible.value = false
    fetchTableData()
  } finally {
    submitLoading.value = false
  }
}

onMounted(async () => {
  await fetchFarmlandOptions()
  fetchTableData()
})
</script>

<style scoped lang="scss">
.meta-label {
  font-size: 12px;
  color: rgba(238, 251, 245, 0.78);
}

.page-banner__meta strong {
  font-size: 14px;
  color: #fff;
}

.status-summary-grid {
  display: grid;
  gap: 10px;
}

.status-box {
  padding: 14px 16px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 16px;
  background: #fff;
}

.status-box__label {
  display: block;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.status-box__value {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  color: var(--app-text);
}

.status-box__desc {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--app-text-tertiary);
}

.status-box.warning .status-box__value {
  color: #9b6a08;
}

.status-box.info .status-box__value {
  color: var(--app-tech);
}

.device-table :deep(.el-table__row:hover td) {
  background: rgba(23, 138, 98, 0.04) !important;
}

.device-table :deep(.el-table__header-wrapper th),
.device-table :deep(.el-table__body-wrapper td) {
  text-align: center;
}

.device-table :deep(.el-table__cell .cell) {
  text-align: center;
}

.status-tag {
  min-width: 66px;
  justify-content: center;
  border-radius: 999px;
  font-weight: 600;
}

.action-group {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  gap: 12px;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-right: 8px;
}

.drawer-banner {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(15, 118, 110, 0.06));
}

.drawer-banner h3 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #17352e;
}

.drawer-banner p {
  margin: 0;
  color: #5c7269;
  line-height: 1.6;
}

.drawer-tags {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-end;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-item {
  padding: 14px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 14px;
  background: #fff;
}

.detail-item.wide {
  grid-column: span 2;
}

.detail-item span {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.detail-item strong {
  font-size: 13px;
  color: var(--app-text);
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-item.wide {
    grid-column: span 1;
  }

  .action-group {
    gap: 10px;
    flex-wrap: wrap;
  }
}
</style>
