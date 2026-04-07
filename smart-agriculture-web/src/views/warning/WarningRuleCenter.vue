<template>
  <div class="page-shell warning-rule-page">
    <section class="page-banner warning-rule-banner">
      <div class="page-banner__main">
        <span class="page-banner__badge">预警规则中心</span>
        <h1 class="page-banner__title">自动预警规则配置台</h1>
        <p class="page-banner__desc">按农田、作物和监测指标配置触发条件，驱动智能预警自动生成。</p>
      </div>
      <div class="page-banner__actions">
        <el-button type="primary" @click="handleOpenCreate">新增规则</el-button>
      </div>
    </section>

    <section class="page-overview-grid warning-rule-overview">
      <article class="page-overview-card">
        <span class="page-overview-card__label">规则总数</span>
        <strong class="page-overview-card__value">{{ summary.totalCount }}</strong>
        <p class="page-overview-card__desc">当前已配置的自动预警规则</p>
      </article>
      <article class="page-overview-card">
        <span class="page-overview-card__label">启用规则</span>
        <strong class="page-overview-card__value">{{ summary.enabledCount }}</strong>
        <p class="page-overview-card__desc">参与实时预警判断的规则</p>
      </article>
      <article class="page-overview-card">
        <span class="page-overview-card__label">全局规则</span>
        <strong class="page-overview-card__value">{{ summary.globalCount }}</strong>
        <p class="page-overview-card__desc">作用于全农田范围</p>
      </article>
      <article class="page-overview-card">
        <span class="page-overview-card__label">定向规则</span>
        <strong class="page-overview-card__value">{{ summary.scopedCount }}</strong>
        <p class="page-overview-card__desc">按农田/作物定向生效</p>
      </article>
    </section>

    <el-card class="page-filter-card" shadow="never">
      <template #header>
        <div class="page-panel-title">
          <strong>规则筛选</strong>
          <span class="page-panel-tip">按规则名称、预警类型、状态筛选。</span>
        </div>
      </template>
      <el-form :model="queryForm" inline class="page-filter-form">
        <el-form-item label="规则名称">
          <el-input v-model="queryForm.ruleName" placeholder="请输入规则名称" clearable />
        </el-form-item>
        <el-form-item label="预警类型">
          <el-select v-model="queryForm.warningType" placeholder="全部类型" clearable>
            <el-option v-for="item in warningTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.enabled" placeholder="全部状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
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
            <strong>规则列表</strong>
            <span class="page-panel-tip">连续触发次数和去重抑制时间会直接影响预警触发频率。</span>
          </div>
          <el-button type="primary" plain @click="handleOpenCreate">新增规则</el-button>
        </div>
      </template>

      <div class="page-table-shell">
        <el-table v-loading="loading" :data="tableData">
          <el-table-column prop="ruleName" label="规则名称" min-width="160" />
          <el-table-column prop="ruleCode" label="规则编码" min-width="190" />
          <el-table-column label="预警类型" min-width="130">
            <template #default="{ row }">
              {{ getLabel(warningTypeOptions, row.warningType) }}
            </template>
          </el-table-column>
          <el-table-column label="等级" width="86" align="center">
            <template #default="{ row }">
              <el-tag :type="getLabelTagType(row.warningLevel)" effect="plain">
                {{ getLabel(levelOptions, row.warningLevel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="作用域" width="110" align="center">
            <template #default="{ row }">
              {{ getLabel(scopeTypeOptions, row.targetScopeType) }}
            </template>
          </el-table-column>
          <el-table-column label="范围对象" min-width="140">
            <template #default="{ row }">
              {{ renderScopeTarget(row) }}
            </template>
          </el-table-column>
          <el-table-column label="触发条件" min-width="200">
            <template #default="{ row }">
              {{ getLabel(metricOptions, row.metricKey) }} {{ row.compareOperator }} {{ row.thresholdValue }}
            </template>
          </el-table-column>
          <el-table-column label="连续触发" width="100" align="center">
            <template #default="{ row }">{{ row.continuousCount }} 次</template>
          </el-table-column>
          <el-table-column label="去重抑制" width="110" align="center">
            <template #default="{ row }">{{ row.deduplicateMinutes }} 分钟</template>
          </el-table-column>
          <el-table-column label="状态" width="96" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled === 1"
                inline-prompt
                active-text="启用"
                inactive-text="停用"
                @change="(value) => handleToggleEnabled(row, value)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleOpenEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
          layout="total, sizes, prev, pager, next"
          @size-change="fetchTableData"
          @current-change="fetchTableData"
        />
      </div>
    </el-card>

    <el-card class="page-content-card inspection-card" shadow="never">
      <template #header>
        <div class="page-panel-head">
          <div class="page-panel-title">
            <strong>定时巡检任务</strong>
            <span class="page-panel-tip">自动扫描环境数据与设备状态，触发预警规则并记录巡检结果。</span>
          </div>
          <div class="inspection-actions">
            <el-tag :type="inspectionSummary.taskStatus === 'RUNNING' ? 'success' : 'info'" effect="plain">
              {{ inspectionSummary.taskStatus === 'RUNNING' ? '运行中' : '已停止' }}
            </el-tag>
            <el-button :loading="inspectionLoading" @click="fetchInspectionSummary">刷新</el-button>
            <el-button type="primary" :loading="runOnceLoading" @click="runInspectionOnce">手动巡检</el-button>
          </div>
        </div>
      </template>

      <section class="inspection-overview-grid">
        <article class="inspection-mini-card">
          <span>最近巡检时间</span>
          <strong>{{ inspectionSummary.latestInspectionTime || '--' }}</strong>
        </article>
        <article class="inspection-mini-card">
          <span>今日巡检次数</span>
          <strong>{{ inspectionSummary.todayInspectionCount }}</strong>
        </article>
        <article class="inspection-mini-card">
          <span>今日新增预警数</span>
          <strong>{{ inspectionSummary.todayTriggeredWarningCount }}</strong>
        </article>
        <article class="inspection-mini-card">
          <span>巡检间隔</span>
          <strong>{{ formatDelayText(inspectionSummary.fixedDelayMs) }}</strong>
        </article>
      </section>

      <div class="page-table-shell">
        <el-table v-loading="inspectionLoading" :data="inspectionSummary.recentLogs" size="small">
          <el-table-column prop="inspectionTime" label="巡检时间" min-width="170" />
          <el-table-column prop="detectDataCount" label="检测数据条数" width="120" align="center" />
          <el-table-column prop="triggeredWarningCount" label="触发预警条数" width="120" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : 'info'" effect="plain">
                {{ row.statusName || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="triggerMode" label="触发方式" width="100" align="center" />
          <el-table-column prop="summary" label="巡检摘要" min-width="280" show-overflow-tooltip />
        </el-table>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑规则' : '新增规则'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="ruleFormRef" :model="formModel" :rules="formRules" label-width="120px">
        <div class="form-grid">
          <el-form-item label="规则名称" prop="ruleName">
            <el-input v-model="formModel.ruleName" maxlength="100" />
          </el-form-item>
          <el-form-item label="规则编码" prop="ruleCode">
            <el-input v-model="formModel.ruleCode" :disabled="isEdit" maxlength="64" placeholder="如 RULE_TEMP_HIGH_GLOBAL" />
          </el-form-item>
          <el-form-item label="预警类型" prop="warningType">
            <el-select v-model="formModel.warningType">
              <el-option v-for="item in warningTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="预警等级" prop="warningLevel">
            <el-select v-model="formModel.warningLevel">
              <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="作用域类型" prop="targetScopeType">
            <el-select v-model="formModel.targetScopeType" @change="handleScopeTypeChange">
              <el-option v-for="item in scopeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="作用范围" prop="targetScopeId">
            <el-select
              v-model="formModel.targetScopeId"
              :disabled="formModel.targetScopeType === 'GLOBAL'"
              :placeholder="formModel.targetScopeType === 'GLOBAL' ? '全局规则无需选择' : '请选择'"
            >
              <el-option
                v-for="item in getScopeOptionsByType(formModel.targetScopeType)"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="监测指标" prop="metricKey">
            <el-select v-model="formModel.metricKey">
              <el-option v-for="item in metricOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="比较符" prop="compareOperator">
            <el-select v-model="formModel.compareOperator">
              <el-option v-for="item in operatorOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="阈值" prop="thresholdValue">
            <el-input-number v-model="formModel.thresholdValue" :precision="2" :step="0.5" :min="-99999" :max="99999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="连续触发次数" prop="continuousCount">
            <el-input-number v-model="formModel.continuousCount" :min="1" :max="20" style="width: 100%" />
          </el-form-item>
          <el-form-item label="去重抑制分钟" prop="deduplicateMinutes">
            <el-input-number v-model="formModel.deduplicateMinutes" :min="0" :max="1440" style="width: 100%" />
          </el-form-item>
          <el-form-item label="是否启用" prop="enabled">
            <el-radio-group v-model="formModel.enabled">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <el-form-item label="规则说明" prop="description">
          <el-input
            v-model="formModel.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="可填写规则用途、适用场景说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFieldPageApi } from '@/api/field'
import { getCropPageApi } from '@/api/crop'
import { getInspectionSummaryApi, runInspectionOnceApi } from '@/api/inspection'
import {
  createWarningRuleApi,
  deleteWarningRuleApi,
  getWarningRulePageApi,
  getWarningRuleSummaryApi,
  updateWarningRuleApi,
  updateWarningRuleEnabledApi
} from '@/api/warningRule'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const ruleFormRef = ref()

const inspectionLoading = ref(false)
const runOnceLoading = ref(false)

const tableData = ref([])
const farmlandOptions = ref([])
const cropOptions = ref([])

const summary = reactive({
  totalCount: 0,
  enabledCount: 0,
  globalCount: 0,
  scopedCount: 0
})

const inspectionSummary = reactive({
  latestInspectionTime: '',
  todayInspectionCount: 0,
  todayTriggeredWarningCount: 0,
  taskStatus: 'STOPPED',
  fixedDelayMs: 0,
  recentLogs: []
})

const queryForm = reactive({
  ruleName: '',
  warningType: '',
  enabled: undefined
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const warningTypeOptions = [
  { label: '温度过高', value: 'TEMPERATURE_HIGH' },
  { label: '温度过低', value: 'TEMPERATURE_LOW' },
  { label: '空气湿度过高', value: 'HUMIDITY_HIGH' },
  { label: '土壤湿度过低', value: 'SOIL_MOISTURE_LOW' },
  { label: '光照异常', value: 'LIGHT_ABNORMAL' },
  { label: '光照强度过高', value: 'LIGHT_INTENSITY_HIGH' },
  { label: '设备离线', value: 'DEVICE_OFFLINE' },
  { label: '设备维护', value: 'DEVICE_MAINTENANCE' }
]

const levelOptions = [
  { label: '低', value: 1 },
  { label: '中', value: 2 },
  { label: '高', value: 3 }
]

const scopeTypeOptions = [
  { label: '全局', value: 'GLOBAL' },
  { label: '农田定向', value: 'FARMLAND' },
  { label: '作物定向', value: 'CROP' }
]

const metricOptions = [
  { label: '温度', value: 'temperature' },
  { label: '湿度', value: 'humidity' },
  { label: '土壤湿度', value: 'soil_moisture' },
  { label: '光照', value: 'light' },
  { label: '设备离线', value: 'device_offline' }
]

const operatorOptions = [
  { label: '>', value: '>' },
  { label: '<', value: '<' },
  { label: '>=', value: '>=' },
  { label: '<=', value: '<=' },
  { label: '==', value: '==' }
]

const formModel = reactive({
  ruleName: '',
  ruleCode: '',
  warningType: '',
  warningLevel: 2,
  targetScopeType: 'GLOBAL',
  targetScopeId: undefined,
  metricKey: '',
  compareOperator: '<',
  thresholdValue: undefined,
  continuousCount: 1,
  deduplicateMinutes: 30,
  enabled: 1,
  description: ''
})

const formRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
  warningType: [{ required: true, message: '请选择预警类型', trigger: 'change' }],
  warningLevel: [{ required: true, message: '请选择预警等级', trigger: 'change' }],
  targetScopeType: [{ required: true, message: '请选择作用域类型', trigger: 'change' }],
  targetScopeId: [
    {
      validator: (_rule, value, callback) => {
        if (formModel.targetScopeType !== 'GLOBAL' && !value) {
          callback(new Error('请指定作用范围对象'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  metricKey: [{ required: true, message: '请选择监测指标', trigger: 'change' }],
  compareOperator: [{ required: true, message: '请选择比较符', trigger: 'change' }],
  thresholdValue: [{ required: true, message: '请输入阈值', trigger: 'blur' }],
  continuousCount: [{ required: true, message: '请输入连续触发次数', trigger: 'blur' }],
  deduplicateMinutes: [{ required: true, message: '请输入去重抑制分钟', trigger: 'blur' }]
}

onMounted(async () => {
  await Promise.all([fetchScopeOptions(), fetchSummary(), fetchTableData(), fetchInspectionSummary()])
})

async function fetchScopeOptions() {
  try {
    const [fieldRes, cropRes] = await Promise.all([
      getFieldPageApi({ current: 1, size: 100 }),
      getCropPageApi({ current: 1, size: 100 })
    ])
    farmlandOptions.value = (fieldRes.records || []).map((item) => ({ id: item.id, name: item.fieldName }))
    cropOptions.value = (cropRes.records || []).map((item) => ({ id: item.id, name: item.cropName }))
  } catch (error) {
    farmlandOptions.value = []
    cropOptions.value = []
  }
}

async function fetchSummary() {
  try {
    const res = await getWarningRuleSummaryApi()
    Object.assign(summary, res || {})
  } catch (error) {
    Object.assign(summary, { totalCount: 0, enabledCount: 0, globalCount: 0, scopedCount: 0 })
  }
}

async function fetchInspectionSummary() {
  inspectionLoading.value = true
  try {
    const res = await getInspectionSummaryApi({ recentLimit: 5 })
    Object.assign(inspectionSummary, {
      latestInspectionTime: res.latestInspectionTime || '',
      todayInspectionCount: res.todayInspectionCount || 0,
      todayTriggeredWarningCount: res.todayTriggeredWarningCount || 0,
      taskStatus: res.taskStatus || 'STOPPED',
      fixedDelayMs: res.fixedDelayMs || 0,
      recentLogs: res.recentLogs || []
    })
  } catch (error) {
    Object.assign(inspectionSummary, {
      latestInspectionTime: '',
      todayInspectionCount: 0,
      todayTriggeredWarningCount: 0,
      taskStatus: 'STOPPED',
      fixedDelayMs: 0,
      recentLogs: []
    })
  } finally {
    inspectionLoading.value = false
  }
}

async function runInspectionOnce() {
  runOnceLoading.value = true
  try {
    await runInspectionOnceApi()
    ElMessage.success('已触发一次巡检任务')
    await fetchInspectionSummary()
  } finally {
    runOnceLoading.value = false
  }
}

async function fetchTableData() {
  loading.value = true
  try {
    const res = await getWarningRulePageApi({
      current: pagination.current,
      size: pagination.size,
      ruleName: queryForm.ruleName || undefined,
      warningType: queryForm.warningType || undefined,
      enabled: queryForm.enabled
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchTableData()
}

function handleReset() {
  queryForm.ruleName = ''
  queryForm.warningType = ''
  queryForm.enabled = undefined
  pagination.current = 1
  fetchTableData()
}

function handleOpenCreate() {
  isEdit.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleOpenEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  formModel.ruleName = row.ruleName
  formModel.ruleCode = row.ruleCode
  formModel.warningType = row.warningType
  formModel.warningLevel = row.warningLevel
  formModel.targetScopeType = row.targetScopeType
  formModel.targetScopeId = row.targetScopeId || undefined
  formModel.metricKey = row.metricKey
  formModel.compareOperator = row.compareOperator
  formModel.thresholdValue = Number(row.thresholdValue)
  formModel.continuousCount = row.continuousCount
  formModel.deduplicateMinutes = row.deduplicateMinutes
  formModel.enabled = row.enabled
  formModel.description = row.description || ''
  dialogVisible.value = true
}

async function handleSubmitForm() {
  if (!ruleFormRef.value) return
  try {
    await ruleFormRef.value.validate()
  } catch (_error) {
    return
  }

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateWarningRuleApi(editingId.value, buildUpdatePayload())
      ElMessage.success('规则更新成功')
    } else {
      await createWarningRuleApi(buildCreatePayload())
      ElMessage.success('规则新增成功')
    }
    dialogVisible.value = false
    await Promise.all([fetchSummary(), fetchTableData()])
  } finally {
    submitLoading.value = false
  }
}

async function handleToggleEnabled(row, enabledValue) {
  try {
    await updateWarningRuleEnabledApi(row.id, { enabled: enabledValue ? 1 : 0 })
    row.enabled = enabledValue ? 1 : 0
    ElMessage.success(enabledValue ? '规则已启用' : '规则已停用')
    fetchSummary()
  } catch (_error) {
    fetchTableData()
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除规则「${row.ruleName}」吗？`, '删除确认', {
      type: 'warning'
    })
    await deleteWarningRuleApi(row.id)
    ElMessage.success('规则删除成功')
    if (tableData.value.length === 1 && pagination.current > 1) {
      pagination.current -= 1
    }
    await Promise.all([fetchSummary(), fetchTableData()])
  } catch (_error) {
    // 取消删除不提示
  }
}

function handleScopeTypeChange(scopeType) {
  if (scopeType === 'GLOBAL') {
    formModel.targetScopeId = undefined
  }
}

function getScopeOptionsByType(scopeType) {
  if (scopeType === 'FARMLAND') return farmlandOptions.value
  if (scopeType === 'CROP') return cropOptions.value
  return []
}

function renderScopeTarget(row) {
  if (row.targetScopeType === 'GLOBAL') return '全局'
  if (!row.targetScopeId) return '--'
  const options = row.targetScopeType === 'FARMLAND' ? farmlandOptions.value : cropOptions.value
  const target = options.find((item) => item.id === row.targetScopeId)
  return target?.name || `ID:${row.targetScopeId}`
}

function getLabel(options, value) {
  return options.find((item) => item.value === value)?.label || '--'
}

function getLabelTagType(level) {
  if (level === 3) return 'danger'
  if (level === 2) return 'warning'
  return 'success'
}

function formatDelayText(delayMs) {
  if (!delayMs) return '--'
  const seconds = Math.floor(delayMs / 1000)
  if (seconds < 60) return `${seconds} 秒`
  const minutes = Math.floor(seconds / 60)
  return `${minutes} 分钟`
}

function buildCreatePayload() {
  return {
    ruleName: formModel.ruleName.trim(),
    ruleCode: formModel.ruleCode.trim().toUpperCase(),
    warningType: formModel.warningType,
    warningLevel: formModel.warningLevel,
    targetScopeType: formModel.targetScopeType,
    targetScopeId: formModel.targetScopeType === 'GLOBAL' ? null : formModel.targetScopeId,
    metricKey: formModel.metricKey,
    compareOperator: formModel.compareOperator,
    thresholdValue: Number(formModel.thresholdValue),
    continuousCount: Number(formModel.continuousCount),
    deduplicateMinutes: Number(formModel.deduplicateMinutes),
    enabled: formModel.enabled,
    description: formModel.description?.trim() || ''
  }
}

function buildUpdatePayload() {
  return {
    ruleName: formModel.ruleName.trim(),
    warningType: formModel.warningType,
    warningLevel: formModel.warningLevel,
    targetScopeType: formModel.targetScopeType,
    targetScopeId: formModel.targetScopeType === 'GLOBAL' ? null : formModel.targetScopeId,
    metricKey: formModel.metricKey,
    compareOperator: formModel.compareOperator,
    thresholdValue: Number(formModel.thresholdValue),
    continuousCount: Number(formModel.continuousCount),
    deduplicateMinutes: Number(formModel.deduplicateMinutes),
    enabled: formModel.enabled,
    description: formModel.description?.trim() || ''
  }
}

function resetForm() {
  formModel.ruleName = ''
  formModel.ruleCode = ''
  formModel.warningType = ''
  formModel.warningLevel = 2
  formModel.targetScopeType = 'GLOBAL'
  formModel.targetScopeId = undefined
  formModel.metricKey = ''
  formModel.compareOperator = '<'
  formModel.thresholdValue = undefined
  formModel.continuousCount = 1
  formModel.deduplicateMinutes = 30
  formModel.enabled = 1
  formModel.description = ''
}
</script>

<style scoped lang="scss">
.warning-rule-banner {
  background:
    radial-gradient(circle at right top, rgba(59, 130, 246, 0.14), transparent 26%),
    radial-gradient(circle at left center, rgba(34, 197, 94, 0.14), transparent 28%),
    var(--app-hero-gradient);
}

.warning-rule-overview {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.inspection-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inspection-overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.inspection-mini-card {
  border: 1px solid rgba(15, 73, 54, 0.1);
  border-radius: 12px;
  padding: 12px 14px;
  background: #fff;

  span {
    display: block;
    font-size: 12px;
    color: var(--app-text-secondary);
  }

  strong {
    display: block;
    margin-top: 6px;
    font-size: 18px;
    color: var(--app-text);
  }
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 12px;
}

@media (max-width: 1280px) {
  .inspection-overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1024px) {
  .warning-rule-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>

