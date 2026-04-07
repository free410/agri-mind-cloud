export const fieldStatusOptions = [
  { label: '闲置', value: 0, tagType: 'info' },
  { label: '种植中', value: 1, tagType: 'success' },
  { label: '维护中', value: 2, tagType: 'warning' }
]

export const cropGrowthStageOptions = [
  { label: '播种期', value: 'SEEDING', tagType: 'info' },
  { label: '苗期', value: 'SEEDLING', tagType: 'success' },
  { label: '生长期', value: 'GROWING', tagType: 'success' },
  { label: '开花期', value: 'FLOWERING', tagType: 'warning' },
  { label: '结果期', value: 'FRUITING', tagType: 'warning' },
  { label: '成熟期', value: 'MATURING', tagType: 'danger' },
  { label: '已采收', value: 'HARVESTED', tagType: 'info' }
]

export const cropHealthStatusOptions = [
  { label: '健康', value: 'HEALTHY', tagType: 'success' },
  { label: '需关注', value: 'ATTENTION', tagType: 'warning' },
  { label: '病害风险', value: 'DISEASE_RISK', tagType: 'danger' },
  { label: '异常', value: 'ABNORMAL', tagType: 'danger' }
]

export const deviceOnlineStatusOptions = [
  { label: '离线', value: 0, tagType: 'info' },
  { label: '在线', value: 1, tagType: 'success' },
  { label: '故障', value: 2, tagType: 'danger' }
]

export const deviceRunStatusOptions = [
  { label: '停用', value: 0, tagType: 'info' },
  { label: '运行中', value: 1, tagType: 'success' },
  { label: '待机中', value: 2, tagType: 'warning' },
  { label: '异常', value: 3, tagType: 'danger' }
]

export const warningLevelOptions = [
  { label: '低', value: 1, tagType: 'success' },
  { label: '中', value: 2, tagType: 'warning' },
  { label: '高', value: 3, tagType: 'danger' }
]

export const warningStatusOptions = [
  { label: '待确认', value: 0, tagType: 'warning' },
  { label: '处理中', value: 1, tagType: '' },
  { label: '已处理', value: 2, tagType: 'success' },
  { label: '已闭环', value: 3, tagType: 'info' }
]

export function getOptionLabel(options, value, fallback = '未知') {
  return options.find((item) => item.value === value)?.label || fallback
}

export function getOptionTagType(options, value, fallback = 'info') {
  return options.find((item) => item.value === value)?.tagType || fallback
}

export function formatNumber(value, unit = '', precision = 0) {
  if (value === null || value === undefined || value === '') {
    return '--'
  }

  const numericValue = Number(value)
  if (Number.isNaN(numericValue)) {
    return String(value)
  }

  return `${numericValue.toFixed(precision)}${unit}`
}

export function buildSummaryItem(label, value, unit = '', tone = 'normal') {
  return {
    label,
    value: value === null || value === undefined ? '--' : `${value}${unit}`,
    tone
  }
}
