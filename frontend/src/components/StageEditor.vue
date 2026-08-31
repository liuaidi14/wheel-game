<template>
  <draggable
    :model-value="stages"
    item-key="_uid"
    handle=".drag-handle"
    animation="200"
    @update:model-value="handleDragSort"
  >
    <template #item="{ element, index }">
      <div class="item-container">
        <div class="stage-block" :data-stage="element.name">
          <div class="stage-header">
            <span class="drag-handle">⠿</span>
            <el-input v-model="element.name" placeholder="阶段名称" />
            <el-select
              :model-value="element.type"
              @change="handleTypeChange(element, $event)"
              style="width: 120px;"
            >
              <el-option label="📘 基础信息" value="base" />
              <el-option label="📙 人生经历" value="life" />
            </el-select>
            <el-button type="danger" circle size="small" @click="removeStage(index)">✕</el-button>
          </div>

          <div class="option-list">
            <div v-for="(opt, oIdx) in element.options" :key="opt._uid" class="option-item">
              <el-input v-model="opt.label" placeholder="选项" style="width: 140px;" />
              <el-input-number v-model="opt.weight" :min="1" size="small" />
              <el-input v-model="opt.descText" placeholder="描述" style="width: 140px;" />

              <!-- 下一阶段下拉框：添加 popper-class 和 visible-change -->
              <el-select
                v-model="opt.nextStageId"
                placeholder="无分支"
                filterable
                clearable
                style="width: 180px;"
                popper-class="shared-stage-select"
                @visible-change="handleDropdownVisible"
              >
                <el-option :value="null" label="无分支（默认顺序）" />
                <el-option
                  v-for="stage in selectableStages"
                  :key="stage.id"
                  :value="stage.id"
                  :label="stage.name"
                />
              </el-select>

              <div class="dynamic-attr-wrapper">
                <div class="attr-tag-list">
                  <el-tag
                    v-for="(val, key) in opt.attributeGains"
                    :key="key"
                    closable
                    @close="removeAttributeGain(opt, key)"
                  >
                    {{ key }}: +{{ val }}
                  </el-tag>
                </div>
                <div class="add-attr-row">
                  <el-select v-model="opt._tempAttrKey" placeholder="选择属性" size="small" style="width: 120px;">
                    <el-option
                      v-for="attr in attributesList"
                      :key="attr.id"
                      :label="attr.name"
                      :value="attr.name"
                    />
                  </el-select>
                  <el-input-number v-model="opt._tempAttrVal" :min="1" size="small" />
                  <el-button size="small" type="primary" @click="addAttributeGainToOpt(opt)">+ 添加</el-button>
                </div>
              </div>
              <el-button type="danger" circle size="small" @click="removeOption(index, oIdx)">✕</el-button>
            </div>
          </div>

          <div class="add-row">
            <el-input v-model="newLabel" placeholder="新选项名" style="width: 140px;" />
            <el-input-number v-model="newWeight" :min="1" size="small" />
            <el-input v-model="newDesc" placeholder="描述" style="width: 140px;" />
            <el-button type="primary" size="small" @click="addOption(index)">➕ 添加</el-button>
          </div>
        </div>
        <div class="insert-stage-wrapper">
          <el-button size="small" type="primary" plain @click="insertStage(index + 1)">在此后插入阶段</el-button>
        </div>
      </div>
    </template>
  </draggable>
</template>

<script>
// 模块级共享滚动位置（所有 StageEditor 实例、所有“下一阶段”下拉框共用）
let sharedScrollTop = 0
</script>

<script setup>
import { ref, computed } from 'vue'
import draggable from 'vuedraggable'
import { v4 as uuidv4 } from 'uuid'
import { ElMessage } from 'element-plus'

const props = defineProps({
  stages: { type: Array, required: true },
  type: { type: String, required: true },
  attributesList: { type: Array, default: () => [] },
  allSelectableStages: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:stages', 'type-change'])

const newLabel = ref('')
const newWeight = ref(1)
const newDesc = ref('')

// 过滤掉没有 id 的临时阶段
const selectableStages = computed(() => props.allSelectableStages.filter(s => s.id != null))

// 查找当前可见的下拉滚动容器
function findVisibleScrollWrap() {
  const dropdowns = document.querySelectorAll('.el-select-dropdown')
  for (const dropdown of dropdowns) {
    if (dropdown.style.display !== 'none' && dropdown.offsetParent !== null) {
      const scrollWrap = dropdown.querySelector('.el-scrollbar__wrap')
      if (scrollWrap) return scrollWrap
    }
  }
  return null
}

// 打开/关闭下拉框时处理滚动记忆
function handleDropdownVisible(visible) {
  if (visible) {
    // 打开：延迟等待下拉面板渲染后恢复位置
    setTimeout(() => {
      const scrollWrap = findVisibleScrollWrap()
      if (scrollWrap) {
        scrollWrap.scrollTop = sharedScrollTop
        // 移除旧监听，避免重复绑定
        if (scrollWrap._sharedScrollHandler) {
          scrollWrap.removeEventListener('scroll', scrollWrap._sharedScrollHandler)
        }
        const handler = () => {
          sharedScrollTop = scrollWrap.scrollTop
        }
        scrollWrap.addEventListener('scroll', handler)
        scrollWrap._sharedScrollHandler = handler
      }
    }, 80)
  } else {
    // 关闭时移除滚动监听
    const scrollWrap = findVisibleScrollWrap()
    if (scrollWrap && scrollWrap._sharedScrollHandler) {
      scrollWrap.removeEventListener('scroll', scrollWrap._sharedScrollHandler)
      scrollWrap._sharedScrollHandler = null
    }
  }
}

function handleDragSort(newOrder) {
  emit('update:stages', newOrder)
}

function insertStage(index) {
  const newStage = { _uid: uuidv4(), name: '新阶段', type: props.type, options: [] }
  const updated = [...props.stages]
  updated.splice(index, 0, newStage)
  emit('update:stages', updated)
}

function removeStage(index) {
  const updated = [...props.stages]
  updated.splice(index, 1)
  emit('update:stages', updated)
}

function addOption(stageIdx) {
  if (!newLabel.value.trim()) {
    ElMessage.warning('请先输入【新选项名】再点击添加！')
    return
  }
  const stage = props.stages[stageIdx]
  stage.options.push({
    _uid: uuidv4(),
    label: newLabel.value.trim(),
    weight: newWeight.value || 1,
    descText: newDesc.value.trim(),
    nextStageId: null,
    attributeGains: {}
  })
  newLabel.value = ''
  newWeight.value = 1
  newDesc.value = ''
}

function removeOption(stageIdx, optionIdx) {
  props.stages[stageIdx].options.splice(optionIdx, 1)
}

function addAttributeGainToOpt(opt) {
  if (opt._tempAttrKey && opt._tempAttrVal && opt._tempAttrVal > 0) {
    if (!opt.attributeGains) opt.attributeGains = {}
    opt.attributeGains[opt._tempAttrKey] = opt._tempAttrVal
    opt._tempAttrKey = ''
    opt._tempAttrVal = 0
  } else {
    ElMessage.warning('请先选择一个属性并输入有效的加成数值！')
  }
}

function removeAttributeGain(opt, key) {
  if (opt.attributeGains) delete opt.attributeGains[key]
}

function handleTypeChange(element, newType) {
  if (newType !== element.type) {
    const oldType = element.type
    element.type = newType
    emit('type-change', element, oldType, newType)
  }
}
</script>

<style scoped>
.item-container { margin-bottom: 15px; }
.stage-block { background: #20152a; border: 1px solid #4a3a3a; border-radius: 12px; padding: 15px; }
.stage-header { display: flex; gap: 8px; align-items: center; margin-bottom: 10px; }
.drag-handle { cursor: grab; font-size: 1.2rem; color: #8a7a8a; }
.option-item { display: flex; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; align-items: center; }
.add-row { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; align-items: center; }
.dynamic-attr-wrapper { display: flex; flex-direction: column; gap: 6px; min-width: 180px; }
.attr-tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
.add-attr-row { display: flex; gap: 8px; align-items: center; }
.insert-stage-wrapper { display: flex; justify-content: center; margin-top: 5px; }
</style>