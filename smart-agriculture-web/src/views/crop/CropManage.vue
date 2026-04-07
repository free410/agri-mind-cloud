<template>
  <div class="page-shell crop-page">
    <section class="page-banner">
      <div class="page-banner__main">
        <span class="page-banner__badge">作物生命周期管理</span>
        <h1 class="page-banner__title">作物管理</h1>
        <p class="page-banner__desc">聚焦作物阶段与健康状态，支撑种植过程精细化管理。</p>
      </div>

      <div class="page-banner__actions">
        <el-button type="primary" @click="openCreateDialog">新增作物</el-button>
      </div>
    </section>

    <section class="page-overview-grid">
      <article v-for="item in overviewCards" :key="item.label" class="page-overview-card">
        <span class="page-overview-card__label">{{ item.label }}</span>
        <strong class="page-overview-card__value">{{ item.value }}</strong>
        <p class="page-overview-card__desc">{{ item.desc }}</p>
      </article>
    </section>

    <el-card class="page-filter-card" shadow="never">
      <template #header>
        <div class="page-panel-title">
          <strong>筛选条件</strong>
          <span class="page-panel-tip">按作物名称、类型、所属农田快速定位。</span>
        </div>
      </template>

      <el-form :model="queryForm" inline class="page-filter-form">
        <el-form-item label="作物名称">
          <el-input
            v-model="queryForm.cropName"
            placeholder="请输入作物名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="作物类型">
          <el-input
            v-model="queryForm.cropType"
            placeholder="例如：蔬菜、水果、粮食作物"
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
          <strong>作物列表</strong>
          <span class="page-panel-tip">共 {{ pagination.total }} 条作物数据。</span>
        </div>
      </template>

      <div class="page-table-shell">
        <el-table v-loading="loading" :data="tableData" class="crop-table">
          <el-table-column prop="id" label="编号" width="88" align="center" />
          <el-table-column prop="cropName" label="作物名称" min-width="150" align="center" />
          <el-table-column prop="cropType" label="作物类型" width="140" align="center" />
          <el-table-column prop="farmlandName" label="所属农田" min-width="170" align="center" />
          <el-table-column label="播种日期" width="128" align="center">
            <template #default="{ row }">{{ row.sowingDate || '--' }}</template>
          </el-table-column>
          <el-table-column label="成熟日期" width="128" align="center">
            <template #default="{ row }">{{ row.maturityDate || '--' }}</template>
          </el-table-column>
          <el-table-column label="生长阶段" width="126" align="center">
            <template #default="{ row }">
              <el-tag class="stage-tag" :type="getOptionTagType(cropGrowthStageOptions, row.growthStage)" effect="light">
                {{ row.growthStageName || getOptionLabel(cropGrowthStageOptions, row.growthStage) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="健康状态" width="126" align="center">
            <template #default="{ row }">
              <el-tag class="health-tag" :type="getHealthTagType(row.healthStatus)" effect="light">
                {{ row.healthStatusName || getOptionLabel(cropHealthStatusOptions, row.healthStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right" align="center">
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
      :title="dialogMode === 'create' ? '新增作物' : '编辑作物'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="作物名称" prop="cropName">
              <el-input v-model="formData.cropName" placeholder="请输入作物名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作物类型" prop="cropType">
              <el-input v-model="formData.cropType" placeholder="请输入作物类型" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
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
          <el-col :span="12">
            <el-form-item label="生长阶段" prop="growthStage">
              <el-select v-model="formData.growthStage" placeholder="请选择生长阶段" style="width: 100%">
                <el-option
                  v-for="item in cropGrowthStageOptions"
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
            <el-form-item label="健康状态" prop="healthStatus">
              <el-select v-model="formData.healthStatus" placeholder="请选择健康状态" style="width: 100%">
                <el-option
                  v-for="item in cropHealthStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="播种日期" prop="sowingDate">
              <el-date-picker
                v-model="formData.sowingDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择播种日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="成熟日期" prop="maturityDate">
          <el-date-picker
            v-model="formData.maturityDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择成熟日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="4" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="作物详情" size="560px">
      <div v-loading="detailLoading" class="detail-panel">
        <section class="drawer-banner">
          <div>
            <h3>{{ detailData.cropName || '未命名作物' }}</h3>
            <p>{{ detailData.farmlandName || '暂无所属农田信息' }}</p>
          </div>
          <div class="drawer-tags">
            <el-tag :type="getOptionTagType(cropGrowthStageOptions, detailData.growthStage)" effect="light">
              {{ detailData.growthStageName || getOptionLabel(cropGrowthStageOptions, detailData.growthStage) }}
            </el-tag>
            <el-tag :type="getHealthTagType(detailData.healthStatus)" effect="light">
              {{ detailData.healthStatusName || getOptionLabel(cropHealthStatusOptions, detailData.healthStatus) }}
            </el-tag>
          </div>
        </section>

        <div class="detail-grid">
          <div class="detail-card">
            <span class="detail-card__label">基础信息</span>
            <div class="detail-card__list">
              <div class="detail-card__item">
                <span>作物编号</span>
                <strong>{{ detailData.id || '--' }}</strong>
              </div>
              <div class="detail-card__item">
                <span>作物类型</span>
                <strong>{{ detailData.cropType || '--' }}</strong>
              </div>
              <div class="detail-card__item">
                <span>所属农田</span>
                <strong>{{ detailData.farmlandName || '--' }}</strong>
              </div>
              <div class="detail-card__item">
                <span>健康状态</span>
                <strong>{{ detailData.healthStatusName || getOptionLabel(cropHealthStatusOptions, detailData.healthStatus) }}</strong>
              </div>
            </div>
          </div>

          <div class="detail-card">
            <span class="detail-card__label">生长时间轴</span>
            <div class="timeline-list">
              <div class="timeline-item">
                <span class="timeline-dot active"></span>
                <div>
                  <strong>播种日期</strong>
                  <p>{{ detailData.sowingDate || '--' }}</p>
                </div>
              </div>
              <div class="timeline-item">
                <span class="timeline-dot"></span>
                <div>
                  <strong>当前阶段</strong>
                  <p>{{ detailData.growthStageName || getOptionLabel(cropGrowthStageOptions, detailData.growthStage) }}</p>
                </div>
              </div>
              <div class="timeline-item">
                <span class="timeline-dot warning"></span>
                <div>
                  <strong>成熟日期</strong>
                  <p>{{ detailData.maturityDate || '--' }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-meta">
          <div class="detail-meta__item">
            <span>备注</span>
            <strong>{{ detailData.remark || '--' }}</strong>
          </div>
          <div class="detail-meta__item">
            <span>创建时间</span>
            <strong>{{ detailData.createTime || '--' }}</strong>
          </div>
          <div class="detail-meta__item">
            <span>更新时间</span>
            <strong>{{ detailData.updateTime || '--' }}</strong>
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
import { createCropApi, deleteCropApi, getCropDetailApi, getCropPageApi, updateCropApi } from '@/api/crop'
import {
  cropGrowthStageOptions,
  cropHealthStatusOptions,
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
  cropName: '',
  cropType: '',
  farmlandId: undefined
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const formData = reactive({
  id: undefined,
  cropName: '',
  cropType: '',
  farmlandId: undefined,
  sowingDate: '',
  maturityDate: '',
  growthStage: 'GROWING',
  healthStatus: 'HEALTHY',
  remark: ''
})

const detailData = reactive({
  id: undefined,
  cropName: '',
  cropType: '',
  farmlandName: '',
  sowingDate: '',
  maturityDate: '',
  growthStage: '',
  growthStageName: '',
  healthStatus: '',
  healthStatusName: '',
  remark: '',
  createTime: '',
  updateTime: ''
})

const formRules = {
  cropName: [
    { required: true, message: '请输入作物名称', trigger: 'blur' },
    { min: 2, max: 30, message: '作物名称长度应在 2 到 30 个字符之间', trigger: 'blur' }
  ],
  cropType: [{ required: true, message: '请输入作物类型', trigger: 'blur' }],
  farmlandId: [{ required: true, message: '请选择所属农田', trigger: 'change' }],
  sowingDate: [{ required: true, message: '请选择播种日期', trigger: 'change' }],
  maturityDate: [{ required: true, message: '请选择成熟日期', trigger: 'change' }],
  growthStage: [{ required: true, message: '请选择生长阶段', trigger: 'change' }],
  healthStatus: [{ required: true, message: '请选择健康状态', trigger: 'change' }]
}

const totalCount = computed(() => overviewData.value.length)
const healthyCount = computed(() => overviewData.value.filter((item) => item.healthStatus === 'HEALTHY').length)
const attentionCount = computed(() =>
  overviewData.value.filter((item) => ['ATTENTION', 'DISEASE_RISK', 'ABNORMAL'].includes(item.healthStatus)).length
)
const fruitingCount = computed(() => overviewData.value.filter((item) => item.growthStage === 'FRUITING').length)

const overviewCards = computed(() => [
  {
    label: '作物总数',
    value: `${totalCount.value} 株`,
    desc: '当前筛选条件下的作物数量。'
  },
  {
    label: '健康作物数',
    value: `${healthyCount.value} 株`,
    desc: '健康状态为“健康”的作物。'
  },
  {
    label: '需关注作物数',
    value: `${attentionCount.value} 株`,
    desc: '关注、病害风险或异常状态。'
  },
  {
    label: '结果期作物数',
    value: `${fruitingCount.value} 株`,
    desc: '当前处于结果期的作物。'
  }
])

function getHealthTagType(status) {
  if (status === 'HEALTHY') return 'success'
  if (status === 'ATTENTION') return 'warning'
  return 'danger'
}

function resetFormData() {
  Object.assign(formData, {
    id: undefined,
    cropName: '',
    cropType: '',
    farmlandId: undefined,
    sowingDate: '',
    maturityDate: '',
    growthStage: 'GROWING',
    healthStatus: 'HEALTHY',
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
      cropName: queryForm.cropName || undefined,
      cropType: queryForm.cropType || undefined,
      farmlandId: queryForm.farmlandId
    }

    const [pageRes, overviewRes] = await Promise.all([
      getCropPageApi({
        current: pagination.current,
        size: pagination.size,
        ...params
      }),
      getCropPageApi({
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
  queryForm.cropName = ''
  queryForm.cropType = ''
  queryForm.farmlandId = undefined
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
    cropName: row.cropName,
    cropType: row.cropType,
    farmlandId: row.farmlandId,
    sowingDate: row.sowingDate,
    maturityDate: row.maturityDate,
    growthStage: row.growthStage,
    healthStatus: row.healthStatus,
    remark: row.remark || ''
  })
}

async function handleView(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    Object.assign(detailData, await getCropDetailApi(row.id))
  } finally {
    detailLoading.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除作物“${row.cropName}”吗？删除后不可恢复。`, '删除确认', {
    type: 'warning'
  })
  await deleteCropApi(row.id)
  ElMessage.success('删除成功')
  if (tableData.value.length === 1 && pagination.current > 1) {
    pagination.current -= 1
  }
  fetchTableData()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()

  if (new Date(formData.maturityDate).getTime() < new Date(formData.sowingDate).getTime()) {
    ElMessage.warning('成熟日期不能早于播种日期')
    return
  }

  submitLoading.value = true
  try {
    const payload = {
      cropName: formData.cropName,
      cropType: formData.cropType,
      farmlandId: formData.farmlandId,
      sowingDate: formData.sowingDate,
      maturityDate: formData.maturityDate,
      growthStage: formData.growthStage,
      healthStatus: formData.healthStatus,
      remark: formData.remark
    }

    if (dialogMode.value === 'create') {
      await createCropApi(payload)
      ElMessage.success('新增作物成功')
    } else {
      await updateCropApi(formData.id, payload)
      ElMessage.success('更新作物成功')
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
.crop-table :deep(.el-table__row:hover td) {
  background: rgba(23, 138, 98, 0.04) !important;
}

.stage-tag,
.health-tag {
  min-width: 72px;
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
  gap: 16px;
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
  background: linear-gradient(135deg, rgba(22, 163, 74, 0.1), rgba(29, 78, 216, 0.06));
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
  gap: 14px;
}

.detail-card {
  padding: 16px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 16px;
  background: #fff;
}

.detail-card__label {
  display: block;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 700;
  color: var(--app-text-secondary);
}

.detail-card__list {
  display: grid;
  gap: 10px;
}

.detail-card__item span {
  display: block;
  margin-bottom: 5px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.detail-card__item strong {
  font-size: 14px;
  color: var(--app-text);
}

.timeline-list {
  display: grid;
  gap: 12px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 18px 1fr;
  gap: 10px;
  align-items: flex-start;
}

.timeline-item strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
  color: var(--app-text);
}

.timeline-item p {
  margin: 0;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.timeline-dot {
  width: 10px;
  height: 10px;
  margin-top: 4px;
  border-radius: 50%;
  background: #d4dfdb;
  box-shadow: 0 0 0 4px rgba(17, 82, 62, 0.06);
}

.timeline-dot.active {
  background: var(--app-primary);
}

.timeline-dot.warning {
  background: var(--app-accent);
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-meta__item {
  padding: 14px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 14px;
  background: #fff;
}

.detail-meta__item span {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.detail-meta__item strong {
  font-size: 13px;
  color: var(--app-text);
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 980px) {
  .detail-grid,
  .detail-meta {
    grid-template-columns: 1fr;
  }

  .action-group {
    gap: 10px;
    flex-wrap: wrap;
  }
}
</style>
