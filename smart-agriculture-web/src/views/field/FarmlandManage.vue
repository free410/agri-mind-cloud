<template>
  <div class="page-shell farmland-page">
    <section class="page-banner">
      <div class="page-banner__main">
        <span class="page-banner__badge">农田空间管理</span>
        <h1 class="page-banner__title">农田管理</h1>
        <p class="page-banner__desc">统一查看地块面积、位置、土壤类型与种植状态。</p>
      </div>

      <div class="page-banner__actions">
        <div class="page-banner__meta">
          <span class="meta-label">平均面积</span>
          <strong>{{ averageAreaDisplay }}</strong>
        </div>
        <el-button type="primary" @click="openCreateDialog">新增农田</el-button>
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
      <el-card class="page-grid-card soil-card" shadow="never">
        <template #header>
          <div class="page-panel-title">
            <strong>土壤类型分布</strong>
            <span class="page-panel-tip">按当前筛选结果统计地块土壤结构。</span>
          </div>
        </template>

        <div class="page-summary-list compact-soil-list">
          <div v-for="item in soilTypeStats" :key="item.name" class="page-summary-item">
            <div>
              <span class="page-summary-item__title">{{ item.name }}</span>
              <span class="page-summary-item__desc">{{ item.count }} 块地块</span>
            </div>
            <span class="page-summary-item__value">{{ item.percent }}%</span>
          </div>
        </div>
      </el-card>

      <el-card class="page-grid-card hint-card" shadow="never">
        <template #header>
          <div class="page-panel-title">
            <strong>管理提示</strong>
            <span class="page-panel-tip">将当前农田情况压缩成简短摘要。</span>
          </div>
        </template>

        <div class="hint-list" :style="hintListStyle">
          <div v-for="item in filledManagementHints" :key="item.label" class="hint-item" :class="item.tone">
            <span class="hint-item__label">{{ item.label }}</span>
            <strong class="hint-item__value">{{ item.value }}</strong>
            <el-tag v-if="item.tag" :type="item.tagType" effect="light" size="small">{{ item.tag }}</el-tag>
          </div>
        </div>
      </el-card>
    </section>

    <el-card class="page-filter-card" shadow="never">
      <template #header>
        <div class="page-panel-title">
          <strong>筛选条件</strong>
          <span class="page-panel-tip">支持按农田名称、土壤类型和状态快速定位。</span>
        </div>
      </template>

      <el-form :model="queryForm" inline class="page-filter-form">
        <el-form-item label="农田名称">
          <el-input
            v-model="queryForm.fieldName"
            placeholder="请输入农田名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="土壤类型">
          <el-input
            v-model="queryForm.soilType"
            placeholder="请输入土壤类型"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="种植状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable style="width: 180px">
            <el-option
              v-for="item in fieldStatusOptions"
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
        <div class="page-panel-head">
          <div class="page-panel-title">
            <strong>农田列表</strong>
            <span class="page-panel-tip">共 {{ pagination.total }} 条农田数据。</span>
          </div>

          <div class="page-view-switch">
            <button
              type="button"
              :class="['page-view-switch__btn', { active: viewMode === 'table' }]"
              @click="viewMode = 'table'"
            >
              表格视图
            </button>
            <button
              type="button"
              :class="['page-view-switch__btn', { active: viewMode === 'card' }]"
              @click="viewMode = 'card'"
            >
              卡片视图
            </button>
          </div>
        </div>
      </template>

      <template v-if="viewMode === 'table'">
        <div class="page-table-shell">
          <el-table v-loading="loading" :data="tableData" class="field-table">
            <el-table-column prop="id" label="编号" width="86" />
            <el-table-column prop="fieldName" label="农田名称" min-width="180" show-overflow-tooltip />
            <el-table-column label="面积" width="120" align="center">
              <template #default="{ row }">
                {{ formatNumber(row.area, ' 亩', 2) }}
              </template>
            </el-table-column>
            <el-table-column prop="location" label="位置" min-width="220" show-overflow-tooltip />
            <el-table-column prop="soilType" label="土壤类型" width="130" />
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="getOptionTagType(fieldStatusOptions, row.status)" effect="plain">
                  {{ row.statusName || getOptionLabel(fieldStatusOptions, row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="风险提示" min-width="132">
              <template #default="{ row }">
                <span class="risk-chip" :class="getRiskLevel(row)">
                  <span class="risk-chip__dot"></span>
                  {{ getRiskText(row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="176" />
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
      </template>

      <template v-else>
        <div v-loading="loading" class="card-grid">
          <FarmlandCard
            v-for="row in tableData"
            :key="row.id"
            :field="row"
            @view="handleView"
            @edit="handleEdit"
            @delete="handleDelete"
          />
        </div>
      </template>

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
      :title="dialogMode === 'create' ? '新增农田' : '编辑农田'"
      width="680px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="农田名称" prop="fieldName">
              <el-input v-model="formData.fieldName" placeholder="请输入农田名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="面积" prop="area">
              <el-input-number v-model="formData.area" :min="0.1" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="土壤类型" prop="soilType">
              <el-input v-model="formData.soilType" placeholder="请输入土壤类型" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="种植状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择状态" style="width: 100%">
                <el-option
                  v-for="item in fieldStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="位置" prop="location">
          <el-input v-model="formData.location" placeholder="请输入农田位置" />
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

    <el-drawer v-model="detailVisible" title="农田详情" size="500px">
      <div v-loading="detailLoading" class="detail-panel">
        <section class="drawer-banner">
          <div>
            <h3>{{ detailData.fieldName || '未命名农田' }}</h3>
            <p>{{ detailData.location || '暂无位置说明' }}</p>
          </div>
          <el-tag :type="getOptionTagType(fieldStatusOptions, detailData.status)" effect="plain">
            {{ detailData.statusName || getOptionLabel(fieldStatusOptions, detailData.status) }}
          </el-tag>
        </section>

        <div class="detail-grid">
          <div class="detail-item">
            <span>农田编号</span>
            <strong>{{ detailData.id || '--' }}</strong>
          </div>
          <div class="detail-item">
            <span>面积</span>
            <strong>{{ formatNumber(detailData.area, ' 亩', 2) }}</strong>
          </div>
          <div class="detail-item">
            <span>土壤类型</span>
            <strong>{{ detailData.soilType || '--' }}</strong>
          </div>
          <div class="detail-item">
            <span>风险提示</span>
            <strong>{{ getRiskText(detailData) }}</strong>
          </div>
          <div class="detail-item wide">
            <span>备注信息</span>
            <strong>{{ detailData.remark || '--' }}</strong>
          </div>
          <div class="detail-item">
            <span>创建时间</span>
            <strong>{{ detailData.createTime || '--' }}</strong>
          </div>
          <div class="detail-item">
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
import {
  createFieldApi,
  deleteFieldApi,
  getFieldDetailApi,
  getFieldPageApi,
  updateFieldApi
} from '@/api/field'
import { fieldStatusOptions, formatNumber, getOptionLabel, getOptionTagType } from '@/utils/agriDisplay'
import FarmlandCard from '@/components/common/FarmlandCard.vue'

const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const submitLoading = ref(false)
const detailLoading = ref(false)
const dialogMode = ref('create')
const formRef = ref()
const viewMode = ref('table')

const queryForm = reactive({
  fieldName: '',
  soilType: '',
  status: undefined
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const tableData = ref([])
const overviewData = ref([])

const formData = reactive({
  id: undefined,
  fieldName: '',
  area: 1,
  location: '',
  soilType: '',
  status: 1,
  remark: ''
})

const detailData = reactive({
  id: undefined,
  fieldName: '',
  area: '',
  location: '',
  soilType: '',
  status: undefined,
  statusName: '',
  remark: '',
  createTime: '',
  updateTime: ''
})

const formRules = {
  fieldName: [
    { required: true, message: '请输入农田名称', trigger: 'blur' },
    { min: 2, max: 30, message: '农田名称长度应在 2 到 30 个字符之间', trigger: 'blur' }
  ],
  area: [{ required: true, message: '请输入农田面积', trigger: 'change' }],
  location: [{ required: true, message: '请输入农田位置', trigger: 'blur' }],
  soilType: [{ required: true, message: '请输入土壤类型', trigger: 'blur' }],
  status: [{ required: true, message: '请选择农田状态', trigger: 'change' }]
}

const totalArea = computed(() => overviewData.value.reduce((sum, item) => sum + Number(item.area || 0), 0))
const averageArea = computed(() => (overviewData.value.length ? totalArea.value / overviewData.value.length : 0))
const plantingCount = computed(() => overviewData.value.filter((item) => item.status === 1).length)
const plantingRatio = computed(() =>
  overviewData.value.length ? Math.round((plantingCount.value / overviewData.value.length) * 100) : 0
)
const riskFieldCount = computed(() => overviewData.value.filter((item) => getRiskLevel(item) !== 'low').length)
const averageAreaDisplay = computed(() => `${averageArea.value.toFixed(2)} 亩`)

const soilTypeStats = computed(() => {
  const total = overviewData.value.length || 1
  const counter = overviewData.value.reduce((map, item) => {
    const key = item.soilType || '未标注'
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
    .slice(0, 3)
})

const primarySoilType = computed(() => soilTypeStats.value[0]?.name || '未统计')

const overviewCards = computed(() => [
  {
    label: '农田总数',
    value: `${pagination.total} 块`,
    desc: '当前筛选结果下的地块数量。'
  },
  {
    label: '平均面积',
    value: averageAreaDisplay.value,
    desc: '按当前列表数据计算。'
  },
  {
    label: '种植中农田',
    value: `${plantingCount.value} 块`,
    desc: `当前占比 ${plantingRatio.value}%。`
  },
  {
    label: '风险农田数',
    value: `${riskFieldCount.value} 块`,
    desc: '依据状态与备注识别。'
  }
])

const managementHints = computed(() => [
  {
    label: '种植中占比',
    value: `${plantingRatio.value}%`,
    tag: plantingRatio.value >= 50 ? '主力区域' : '需关注',
    tagType: plantingRatio.value >= 50 ? 'success' : 'warning',
    tone: 'success'
  },
  {
    label: '重点土壤类型',
    value: primarySoilType.value,
    tag: soilTypeStats.value.length ? `${soilTypeStats.value[0]?.count || 0} 块` : '',
    tagType: 'info',
    tone: 'info'
  },
  {
    label: '风险地块',
    value: `${riskFieldCount.value} 块`,
    tag: riskFieldCount.value > 0 ? '优先查看' : '整体平稳',
    tagType: riskFieldCount.value > 0 ? 'danger' : 'success',
    tone: 'warning'
  },
  {
    label: '管理重点',
    value: '位置与备注',
    tag: '适合答辩讲解',
    tagType: 'warning',
    tone: 'default'
  }
])

const filledManagementHints = computed(() => {
  const next = [...managementHints.value].slice(0, 3)
  if (next.length < 3) {
    next.push({
      label: '巡检建议',
      value: riskFieldCount.value > 0 ? '每日一次' : '两日一次',
      tag: `${pagination.total} 块覆盖`,
      tagType: 'info',
      tone: 'info'
    })
  }
  return next.slice(0, 3)
})

const hintListStyle = computed(() => ({
  gridTemplateRows: `repeat(${filledManagementHints.value.length}, minmax(0, 1fr))`
}))

function getRiskLevel(row) {
  if (!row) return 'low'
  const remark = row.remark || ''
  if (remark.includes('风险') || remark.includes('预警')) return 'high'
  if (row.status === 2) return 'medium'
  return 'low'
}

function getRiskText(row) {
  const level = getRiskLevel(row)
  if (level === 'high') return '重点关注'
  if (level === 'medium') return '维护关注'
  return '整体稳定'
}

function resetFormData() {
  formData.id = undefined
  formData.fieldName = ''
  formData.area = 1
  formData.location = ''
  formData.soilType = ''
  formData.status = 1
  formData.remark = ''
}

async function fetchTableData() {
  loading.value = true
  try {
    const params = {
      fieldName: queryForm.fieldName || undefined,
      soilType: queryForm.soilType || undefined,
      status: queryForm.status
    }

    const [pageRes, overviewRes] = await Promise.all([
      getFieldPageApi({
        current: pagination.current,
        size: pagination.size,
        ...params
      }),
      getFieldPageApi({
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
  queryForm.fieldName = ''
  queryForm.soilType = ''
  queryForm.status = undefined
  pagination.current = 1
  fetchTableData()
}

function openCreateDialog() {
  dialogMode.value = 'create'
  dialogVisible.value = true
  resetFormData()
}

function handleEdit(row) {
  dialogMode.value = 'edit'
  dialogVisible.value = true
  formData.id = row.id
  formData.fieldName = row.fieldName
  formData.area = Number(row.area || 1)
  formData.location = row.location
  formData.soilType = row.soilType
  formData.status = row.status
  formData.remark = row.remark || ''
}

async function handleView(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getFieldDetailApi(row.id)
    Object.assign(detailData, res)
  } finally {
    detailLoading.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除农田“${row.fieldName}”吗？删除后不可恢复。`, '删除确认', {
    type: 'warning'
  })
  await deleteFieldApi(row.id)
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
      fieldName: formData.fieldName,
      area: formData.area,
      location: formData.location,
      soilType: formData.soilType,
      status: formData.status,
      remark: formData.remark
    }

    if (dialogMode.value === 'create') {
      await createFieldApi(payload)
      ElMessage.success('新增农田成功')
    } else {
      await updateFieldApi(formData.id, payload)
      ElMessage.success('更新农田成功')
    }

    dialogVisible.value = false
    fetchTableData()
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
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

.soil-card :deep(.el-card__body),
.hint-card :deep(.el-card__body) {
  min-height: 0;
}

.compact-soil-list {
  flex: 1;
  min-height: 0;
  grid-template-rows: repeat(3, minmax(0, 1fr));
}

.compact-soil-list .page-summary-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  min-height: 0;
  height: 100%;
  padding: 18px;
  border-radius: 16px;
}

.compact-soil-list .page-summary-item__title {
  font-size: 15px;
}

.compact-soil-list .page-summary-item__desc {
  margin-top: 6px;
  font-size: 13px;
}

.compact-soil-list .page-summary-item__value {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  justify-self: center;
  min-width: 96px;
  font-size: 30px;
  line-height: 1;
}

.hint-list {
  display: grid;
  flex: 1;
  min-height: 0;
  gap: 12px;
}

.hint-card .hint-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  min-height: 0;
  height: 100%;
  padding: 18px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 16px;
  background: #fff;
}

.hint-item__label {
  justify-self: start;
  font-size: 14px;
  color: var(--app-text-secondary);
}

.hint-item__value {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  justify-self: center;
  min-width: 120px;
  font-size: 26px;
  line-height: 1;
  color: var(--app-text);
}

.hint-card :deep(.el-tag) {
  justify-self: end;
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.field-table :deep(.el-table__row:hover td) {
  background: rgba(23, 138, 98, 0.04) !important;
}

.risk-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.risk-chip__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.risk-chip.low {
  color: var(--app-primary-dark);
  background: var(--app-primary-soft);
}

.risk-chip.low .risk-chip__dot {
  background: var(--app-primary);
}

.risk-chip.medium {
  color: #9b6a08;
  background: var(--app-accent-soft);
}

.risk-chip.medium .risk-chip__dot {
  background: var(--app-accent);
}

.risk-chip.high {
  color: var(--app-danger);
  background: var(--app-danger-soft);
}

.risk-chip.high .risk-chip__dot {
  background: var(--app-danger);
}

.action-group {
  display: flex;
  gap: 12px;
  align-items: center;
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
  margin: 0 0 6px;
  font-size: 20px;
  color: #17352e;
}

.drawer-banner p {
  margin: 0;
  color: #5c7269;
  line-height: 1.55;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-item {
  padding: 14px 16px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 16px;
  background: #fff;
}

.detail-item.wide {
  grid-column: span 2;
}

.detail-item span {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.detail-item strong {
  font-size: 14px;
  line-height: 1.6;
  color: var(--app-text);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .card-grid,
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
