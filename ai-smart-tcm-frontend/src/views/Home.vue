<template>
  <div class="home">
    <!-- 顶部导航 -->
    <header class="home-header">
      <div class="header-inner">
        <div class="logo">
          <el-icon :size="22" color="#fff"><MagicStick /></el-icon>
          <span class="title">AI 智能云</span>
        </div>
        <span class="subtitle">Smart Cloud · 中医问诊 & 智能体</span>
      </div>
    </header>

    <!-- Banner 区域 -->
    <section class="banner">
      <div class="banner-content">
        <h1>欢迎使用 AI 智能云</h1>
        <p>请选择一个应用开始体验</p>
      </div>
    </section>

    <!-- 应用列表 -->
    <main class="app-list">
      <el-row :gutter="20" class="row">
        <el-col :xs="24" :sm="12" :md="12" :lg="12">
          <el-card
            class="app-card tcm"
            shadow="hover"
            @click="goTo('/tcm-chat')"
          >
            <div class="card-icon">
              <el-icon :size="40" color="#67c23a"><ChatDotRound /></el-icon>
            </div>
            <h2>AI 云中医问诊</h2>
            <p>融合传统中医智慧与现代 AI，模拟中医师问诊流程，提供体质辨识与养生建议。</p>
            <el-button type="success" plain class="enter-btn">
              开始问诊 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="12" :lg="12">
          <el-card
            class="app-card manus"
            shadow="hover"
            @click="goTo('/manus-chat')"
          >
            <div class="card-icon">
              <el-icon :size="40" color="#409eff"><Cpu /></el-icon>
            </div>
            <h2>AI 超级智能体</h2>
            <p>通用超级智能体，支持多工具协同调用、复杂任务规划与执行，应对各类业务问题。</p>
            <el-button type="primary" plain class="enter-btn">
              立即体验 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </main>

    <!-- 底部信息 -->
    <footer class="home-footer">
      <span>© {{ new Date().getFullYear() }} AI Smart Cloud · Powered by Vue 3 &amp; Element Plus</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

/**
 * 跳转到指定应用
 * @param path 应用路由地址
 */
const goTo = (path: string) => {
  router.push(path)
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f0f5ff 0%, #f5f7fa 60%);
}

/* ============ Header ============ */
.home-header {
  background: linear-gradient(90deg, #409eff 0%, #5470d4 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}
.header-inner {
  max-width: 1100px;
  margin: 0 auto;
  height: 56px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 18px;
}
.subtitle {
  font-size: 13px;
  opacity: 0.9;
}
@media (max-width: 480px) {
  .subtitle {
    display: none;
  }
}

/* ============ Banner ============ */
.banner {
  padding: 40px 20px 20px;
  text-align: center;
}
.banner-content h1 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #303133;
  font-weight: 600;
}
.banner-content p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}
@media (max-width: 480px) {
  .banner-content h1 {
    font-size: 22px;
  }
}

/* ============ App Cards ============ */
.app-list {
  flex: 1;
  max-width: 1100px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
}
.row {
  margin-top: 20px;
}
.app-card {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.25s ease;
  border: none;
  height: 100%;
  min-height: 240px;
}
.app-card:hover {
  transform: translateY(-4px);
}
.app-card.tcm {
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%);
}
.app-card.manus {
  background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
}
.card-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.app-card h2 {
  font-size: 20px;
  margin: 0 0 8px;
  color: #303133;
}
.app-card p {
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  margin: 0 0 16px;
  min-height: 60px;
}
.enter-btn {
  width: 100%;
}

/* ============ Footer ============ */
.home-footer {
  text-align: center;
  padding: 24px 20px;
  color: #909399;
  font-size: 12px;
}
</style>
