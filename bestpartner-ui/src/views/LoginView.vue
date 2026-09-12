<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  email: '',
  password: '',
})
const loading = ref(false)

async function handleSubmit() {
  loading.value = true
  try {
    await authStore.login(form.email, form.password)
    router.push('/')
  } catch (err) {
    const message = err instanceof Error ? err.message : '登入失敗'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2 class="login-title">BestPartner 登入</h2>
      </template>
      <el-form label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="Email">
          <el-input
            v-model="form.email"
            type="email"
            placeholder="請輸入 Email"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item label="密碼">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="請輸入密碼"
            autocomplete="current-password"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            class="login-button"
          >
            登入
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.login-card {
  width: 360px;
}

.login-title {
  margin: 0;
  text-align: center;
  font-size: 20px;
}

.login-button {
  width: 100%;
}
</style>
