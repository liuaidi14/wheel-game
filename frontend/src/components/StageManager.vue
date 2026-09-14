<template>
  <el-dialog v-model="dialogVisible" width="80%" center>
    <div class="manager-panel">
      <div class="panel-header">
        <h3>📝 编辑完整人生轨迹</h3>
        <div class="header-actions">
          <el-button type="info" @click="isDiagramVisible = !isDiagramVisible" round>
            {{ isDiagramVisible ? '隐藏结构图' : '查看结构图' }}
          </el-button>
          <el-button type="info" @click="isAttrManagerVisible = true" round>⚙️ 管理属性</el-button>
          <el-button type="info" @click="openGroupManager" round>⚙️ 管理剧本</el-button>
          <el-button type="danger" @click="closePanel" round>✕ 关闭</el-button>
        </div>
      </div>

      <div class="group-manager">
        <el-select v-model="currentGroupId" @change="switchGroup" placeholder="请选择剧本组" style="width: 200px;">
          <el-option value="" label="-- 请选择剧本组 --" />
          <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
        </el-select>
      </div>

      <div class="split-body">
        <div class="left-editor" ref="editorContainer" @scroll="saveScrollPosition">
          <el-tabs v-model="activeTab" class="sticky-tabs">
            <el-tab-pane label="📘 基础信息阶段" name="base">
              <StageEditor v-model:stages="baseStages" type="base" :attributes-list="attributesList"
                :all-selectable-stages="allSelectableStages" @type-change="handleTypeChange" />
            </el-tab-pane>
            <el-tab-pane label="📙 人生经历阶段" name="life">
              <StageEditor v-model:stages="lifeStages" type="life" :attributes-list="attributesList"
                :all-selectable-stages="allSelectableStages" @type-change="handleTypeChange" />
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="right-graph" v-if="isDiagramVisible">
          <StageFlowGraph :currentGroupId="currentGroupId" :userId="userId" :baseStages="baseStages"
            :lifeStages="lifeStages" :jumpToStage="handleDiagramJump" />
        </div>
      </div>

      <div class="global-actions">
        <el-button type="success" @click="saveAll" round>💾 保存所有阶段</el-button>
      </div>
    </div>

    <!-- 属性管理弹窗 -->
    <el-dialog v-model="isAttrManagerVisible" title="⚙️ 管理自定义属性" width="400px" append-to-body>
      <el-input v-model.trim="newAttrName" placeholder="输入新属性名 (如: 查克拉)" />
      <el-button type="primary" @click="handleAddAttribute" style="margin-top: 10px;">➕ 添加</el-button>
      <div class="attr-mgr-list" style="margin-top: 15px;">
        <div v-for="attr in attributesList" :key="attr.id" class="attr-mgr-item">
          <span>{{ attr.name }}</span>
          <el-button type="danger" circle size="small" @click="handleDeleteAttribute(attr.id)">✕</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 剧本管理弹窗（所有剧本列表 + 逐个删除） -->
    <el-dialog v-model="isGroupManagerVisible" title="⚙️ 剧本管理" width="480px" append-to-body>
      <!-- 创建新剧本 -->
      <div class="gm-section">
        <div class="gm-section-title">创建新剧本</div>
        <div class="gm-row">
          <el-input v-model.trim="modalNewGroupName" placeholder="输入新剧本名" style="flex: 1;" />
          <el-button type="primary" @click="createGroupInModal">➕ 创建</el-button>
        </div>
      </div>

      <div class="gm-divider"></div>

      <!-- 所有剧本列表 -->
      <div class="gm-section">
        <div class="gm-section-title">所有剧本（共 {{ groups.length }} 个）</div>
        <div class="gm-list">
          <div v-for="g in groups" :key="g.id" class="gm-group-item"
               :class="{ 'gm-group-current': g.id === currentGroupId }">
            <div class="gm-group-info">
              <span v-if="g.id === currentGroupId" class="gm-current-tag">● 当前</span>
              <span class="gm-group-name">{{ g.name }}</span>
              <el-tag v-if="g.type" size="small" class="gm-type-tag"
                      :type="g.type === 'battle' ? 'danger' : 'info'">
                {{ g.type === 'battle' ? '对战' : '人生' }}
              </el-tag>
            </div>
            <div class="gm-group-actions">
              <el-button size="small" @click="switchToGroup(g.id)" :disabled="g.id === currentGroupId">
                切换
              </el-button>
              <el-button size="small" type="danger" @click="deleteGroupById(g.id)">
                删除
              </el-button>
            </div>
          </div>
          <div v-if="groups.length === 0" class="gm-empty">
            暂无剧本组，先在上方创建一个吧
          </div>
        </div>
      </div>

      <div class="gm-divider"></div>

      <!-- 重命名当前剧本 -->
      <div class="gm-section">
        <div class="gm-section-title">重命名当前剧本</div>
        <div class="gm-row">
          <el-input v-model.trim="groupEditName" placeholder="输入新名称" style="flex: 1;" />
          <el-button type="success" @click="saveGroupName">✅ 重命名</el-button>
        </div>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import StageEditor from './StageEditor.vue'
import StageFlowGraph from './StageFlowGraph.vue'
import { wheelApi } from '../api'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  baseStages: { type: Array, default: () => [] },
  lifeStages: { type: Array, default: () => [] },
  groups: { type: Array, default: () => [] },
  currentGroupId: { type: Number, default: null },
  userId: { type: Number, default: 1 },
  attributes: { type: Array, default: () => [] }
})

const emit = defineEmits([
  'update:visible',
  'save',
  'update:currentGroupId',
  'addGroup',
  'renameGroup',
  'deleteGroup',
  'reloadAttributes'
])

// 使用计算属性代理 visible，便于在模板中使用 v-model
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const activeTab = ref('base')
const baseStages = ref([])
const lifeStages = ref([])
const groups = ref(props.groups)
const currentGroupId = ref(props.currentGroupId)
const isDiagramVisible = ref(false)
const isAttrManagerVisible = ref(false)
const isGroupManagerVisible = ref(false)
const newAttrName = ref('')
const modalNewGroupName = ref('')
const groupEditName = ref('')
const editorContainer = ref(null)
const attributesList = ref(props.attributes || [])

const allSelectableStages = computed(() => [...baseStages.value, ...lifeStages.value])

watch(() => props.baseStages, (val) => {
  baseStages.value = val.map(stage => ({
    _uid: uuidv4(),
    id: stage.id,
    name: stage.name,
    type: 'base',
    options: stage.options.map(o => ({ ...o, _uid: uuidv4() }))
  }))
}, { immediate: true })

watch(() => props.lifeStages, (val) => {
  lifeStages.value = val.map(stage => ({
    _uid: uuidv4(),
    id: stage.id,
    name: stage.name,
    type: 'life',
    options: stage.options.map(o => ({ ...o, _uid: uuidv4() }))
  }))
}, { immediate: true })

watch(() => props.groups, (val) => { groups.value = val }, { immediate: true })
watch(() => props.currentGroupId, (val) => { currentGroupId.value = val }, { immediate: true })
watch(() => props.attributes, (val) => { attributesList.value = val }, { immediate: true })

watch(activeTab, () => {
  nextTick(() => restoreScrollPosition())
})

async function switchGroup() {
  if (currentGroupId.value) emit('update:currentGroupId', currentGroupId.value)
}

function openGroupManager() {
  // 打开弹窗时，把当前组名填入重命名输入框
  if (currentGroupId.value) {
    const currentName = groups.value.find(g => g.id === currentGroupId.value)?.name || ''
    groupEditName.value = currentName
  } else {
    groupEditName.value = ''
  }
  isGroupManagerVisible.value = true
}

function createGroupInModal() {
  if (modalNewGroupName.value.trim()) {
    emit('addGroup', modalNewGroupName.value.trim())
    modalNewGroupName.value = ''
  }
}

function saveGroupName() {
  if (groupEditName.value.trim() && currentGroupId.value) {
    emit('renameGroup', { groupId: currentGroupId.value, newName: groupEditName.value.trim() })
  }
}

function deleteCurrentGroup() {
  if (!currentGroupId.value) return
  deleteGroupById(currentGroupId.value)
}

// 切换到指定剧本组
function switchToGroup(groupId) {
  currentGroupId.value = groupId
  emit('update:currentGroupId', groupId)
  // 同步更新重命名输入框
  const g = groups.value.find(item => item.id === groupId)
  groupEditName.value = g?.name || ''
}

// 删除指定剧本组（逐个删除）
function deleteGroupById(groupId) {
  const group = groups.value.find(g => g.id === groupId)
  const name = group?.name || '该剧本组'
  if (!confirm(`⚠️ 确定要删除剧本组 "${name}" 吗？此操作不可撤销！`)) return
  emit('deleteGroup', { groupId })
  // 如果删除的是当前组，清空当前选择
  if (groupId === currentGroupId.value) {
    currentGroupId.value = null
    groupEditName.value = ''
  }
}

function handleTypeChange(element) {
  const sourceArray = element.type === 'base' ? lifeStages.value : baseStages.value
  const targetArray = element.type === 'base' ? baseStages.value : lifeStages.value
  const index = sourceArray.findIndex(item => item._uid === element._uid)
  if (index !== -1) {
    const [removed] = sourceArray.splice(index, 1)
    removed.type = element.type
    targetArray.push(removed)
  }
}

function handleDiagramJump(stageName, stageType) {
  const needSwitch = (stageType === 'base' && activeTab.value !== 'base') || (stageType === 'life' && activeTab.value !== 'life')
  if (needSwitch) activeTab.value = stageType
  setTimeout(() => {
    const container = editorContainer.value
    if (!container) return
    const targetCard = container.querySelector(`[data-stage="${stageName.trim()}"]`)
    if (targetCard) {
      const cardTop = targetCard.offsetTop
      container.scrollTop = cardTop - 200
      targetCard.style.transition = 'border 0.3s'
      targetCard.style.border = '2px solid #f5d78a'
      setTimeout(() => { targetCard.style.border = '1px solid #4a3a3a' }, 2000)
    }
  }, 500)
}

function restoreScrollPosition() {
  const saved = localStorage.getItem('editorScrollTop')
  if (saved !== null && editorContainer.value) {
    editorContainer.value.scrollTop = parseInt(saved)
  }
}

function saveScrollPosition() {
  if (editorContainer.value) {
    localStorage.setItem('editorScrollTop', editorContainer.value.scrollTop)
  }
}

async function handleAddAttribute() {
  if (!newAttrName.value.trim()) return
  if (!currentGroupId.value) {
    ElMessage.error('请先在顶部下拉框中选择一个剧本组！')
    return
  }
  try {
    await wheelApi.createAttribute(props.userId, currentGroupId.value, newAttrName.value.trim())
    emit('reloadAttributes', currentGroupId.value)
    newAttrName.value = ''
    ElMessage.success('属性添加成功！')
  } catch (error) {
    ElMessage.error('添加属性失败: ' + (error.response?.data || '未知错误'))
  }
}

async function handleDeleteAttribute(attrId) {
  try {
    await wheelApi.deleteAttribute(props.userId, attrId)
    emit('reloadAttributes', currentGroupId.value)
    ElMessage.success('属性已删除！')
  } catch (error) {
    ElMessage.error('删除失败: ' + (error.response?.data || '未知错误'))
  }
}

function cleanStages(stages, type) {
  // 建立 id -> name 映射，键使用数字类型
  const idToName = new Map();
  stages.forEach(s => {
    if (s.id != null) {
      idToName.set(Number(s.id), s.name);
    }
  });

  return stages.filter(s => s.type === type).map((s, idx) => ({
    id: s.id,                    // 保留阶段ID（数字）
    name: s.name,
    type,
    listOrder: idx,
    options: s.options.map(o => {
      // 将 nextStageId 规范化为数字或 null
      let nextStageId = null;
      if (o.nextStageId !== null && o.nextStageId !== undefined && o.nextStageId !== '') {
        const parsedId = Number(o.nextStageId);
        if (!isNaN(parsedId)) {
          nextStageId = parsedId;
        }
      }

      // 根据数字 ID 查找对应名称
      const nextStageName = nextStageId != null ? (idToName.get(nextStageId) || '') : '';

      return {
        label: o.label,
        weight: o.weight,
        descText: o.descText,
        nextStageId: nextStageId,      // 数字或 null
        nextStageName: nextStageName,  // 字符串（可能为空）
        attributeGains: o.attributeGains && Object.keys(o.attributeGains).length > 0 ? o.attributeGains : {}
      };
    })
  }))
}

function saveAll() {
  emit('save', {
    baseStages: cleanStages(baseStages.value, 'base'),
    lifeStages: cleanStages(lifeStages.value, 'life'),
    groupId: currentGroupId.value
  })
  closePanel()
}

function closePanel() {
  emit('update:visible', false)
}

onMounted(() => {
  restoreScrollPosition()
})
</script>

<style scoped>
.manager-panel {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.group-manager {
  display: flex;
  gap: 10px;
}

.split-body {
  display: flex;
  gap: 15px;
  overflow: hidden;
  min-height: 0;
}

.left-editor {
  flex: 1.3;
  overflow-y: auto;
  max-height: 65vh;
  padding-right: 10px;
  border-right: 1px solid #2a1a2a;
}

.right-graph {
  flex: 1;
  background: #0a0710;
  border-radius: 12px;
  overflow: hidden;
  min-height: 300px;
  border: 1px solid #2a1a2a;
}

.sticky-tabs :deep(.el-tabs__header) {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #1a1020;
  padding: 4px 0;
  border-bottom: 1px solid #2a1a2a;
}

.global-actions {
  display: flex;
  justify-content: flex-end;
}

.attr-mgr-list {
  max-height: 200px;
  overflow-y: auto;
}

.attr-mgr-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid #2a1a2a;
}

/* ===== 剧本管理弹窗样式 ===== */
.gm-section {
  margin-bottom: 4px;
}

.gm-section-title {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
  font-weight: 500;
}

.gm-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.gm-divider {
  border-top: 1px solid #2a1a2a;
  margin: 12px 0;
}

.gm-list {
  max-height: 280px;
  overflow-y: auto;
}

.gm-group-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 6px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid transparent;
  transition: all 0.2s;
}

.gm-group-item:hover {
  background: rgba(255, 255, 255, 0.06);
}

.gm-group-current {
  background: rgba(155, 120, 200, 0.15);
  border-color: rgba(155, 120, 200, 0.4);
}

.gm-group-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.gm-current-tag {
  color: #c9a7e8;
  font-size: 11px;
  flex-shrink: 0;
}

.gm-group-name {
  color: #e0dce8;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gm-type-tag {
  margin-left: 4px;
  flex-shrink: 0;
}

.gm-group-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  margin-left: 10px;
}

.gm-empty {
  text-align: center;
  color: #666;
  padding: 24px 0;
  font-size: 13px;
}
</style>
