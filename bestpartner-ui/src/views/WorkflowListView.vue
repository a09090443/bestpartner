<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useWorkflowStore } from '../stores/workflow'
import { useAuthStore } from '../stores/auth'
import type { WorkflowSummaryDTO } from '../types/workflow'

const router = useRouter()
const workflowStore = useWorkflowStore()
const authStore = useAuthStore()

onMounted(() => {
  workflowStore.fetchList()
})

function goCreate() {
  router.push('/editor')
}

function goEdit(row: WorkflowSummaryDTO) {
  router.push(`/editor/${row.id}`)
}

async function handleDelete(row: WorkflowSummaryDTO) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`確定刪除 workflow「${row.name}」？`, '刪除確認', {
      type: 'warning',
      confirmButtonText: '刪除',
      cancelButtonText: '取消',
    })
  } catch {
    // 使用者取消
    return
  }
  await workflowStore.remove(row.id)
  ElMessage.success('已刪除')
}

async function handleToggle(row: WorkflowSummaryDTO) {
  if (!row.id) return
  const activate = row.status !== 'ACTIVE'
  try {
    await workflowStore.switchStatus(row.id, activate)
    ElMessage.success(activate ? '已啟用' : '已停用')
  } catch (err) {
    const message = err instanceof Error ? err.message : '操作失敗'
    ElMessage.error(message)
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

const statusTagType: Record<string, 'success' | 'info' | 'warning'> = {
  ACTIVE: 'success',
  INACTIVE: 'info',
  DRAFT: 'warning',
}
</script>

<template>
  <div class="workflow-list">
    <div class="toolbar">
      <h2 class="title">我的 Workflow</h2>
      <div class="actions">
        <el-button type="primary" data-test="create-button" @click="goCreate">
          新建 workflow
        </el-button>
        <el-button text data-test="logout-button" @click="handleLogout">登出</el-button>
      </div>
    </div>

    <el-table :data="workflowStore.summaries" border style="width: 100%">
      <el-table-column prop="name" label="名稱" />
      <el-table-column label="狀態" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType[row.status] ?? 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="updatedAt" label="更新時間" width="200" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" :data-test="`edit-${row.id}`" @click="goEdit(row)">
            編輯
          </el-button>
          <el-button
            size="small"
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            :data-test="`toggle-${row.id}`"
            @click="handleToggle(row)"
          >
            {{ row.status === 'ACTIVE' ? '停用' : '啟用' }}
          </el-button>
          <el-button
            size="small"
            type="danger"
            :data-test="`delete-${row.id}`"
            @click="handleDelete(row)"
          >
            刪除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.workflow-list {
  padding: 24px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.title {
  margin: 0;
  font-size: 20px;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
