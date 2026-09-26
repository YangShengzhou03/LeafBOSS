<template>
  <div class="user-guide-page">
    <header class="page-header">
      <div class="container">
        <h1>使用指南</h1>
        <p>快速了解 LEAF-BOSS 的核心功能与使用方法</p>
      </div>
    </header>

    <main class="page-content">
      <div class="container">
        <section class="guide-card">
          <h3>快速入门</h3>
          <p class="guide-subtitle">快速上手 LEAF-BOSS</p>
          <el-collapse v-model="activeNames" accordion>
            <el-collapse-item name="register" title="注册账号">
              <div class="collapse-content">
                <p>注册 LEAF-BOSS 账号只需以下信息：</p>
                <ul>
                  <li>用户名：3-20个字符，支持中文、英文、数字和下划线</li>
                  <li>密码：6-100个字符，建议包含大小写字母和数字</li>
                  <li>邮箱：用于登录和接收系统通知</li>
                </ul>
              </div>
            </el-collapse-item>

            <el-collapse-item name="login" title="登录系统">
              <div class="collapse-content">
                <p>输入邮箱和密码即可登录。支持"记住密码"功能，连续登录失败5次将临时锁定账户30分钟。</p>
              </div>
            </el-collapse-item>

            <el-collapse-item name="card" title="授权管理">
              <div class="collapse-content">
                <p>LEAF-BOSS 支持完整的授权生命周期管理：</p>
                <ul>
                  <li>生成卡密：根据商品规格批量生成授权卡密</li>
                  <li>验证激活：用户通过验证页面激活并绑定邮箱</li>
                  <li>状态监控：实时查看卡密状态、激活时间及有效期</li>
                </ul>
                <p>支持批量导入导出、状态切换、过期自动清理等功能。</p>
              </div>
            </el-collapse-item>

            <el-collapse-item name="product" title="商品规格配置">
              <div class="collapse-content">
                <p>灵活的商品与规格定义，助力业务快速上线：</p>
                <ul>
                  <li>商品管理：创建不同商品线，设置状态与描述</li>
                  <li>规格定义：为商品配置规格（如月卡、年卡、永久版）</li>
                  <li>价格库存：精细化控制规格价格、有效期及库存</li>
                </ul>
              </div>
            </el-collapse-item>

            <el-collapse-item name="review" title="评论管理">
              <div class="collapse-content">
                <p>集成 Boss 评论管理模块：</p>
                <ul>
                  <li>评论提交：持有有效授权的用户可对特定公司发表评论</li>
                  <li>评论审核：管理员在后台审核与管理用户评论</li>
                  <li>公司关联：评论与公司主体关联，自动统计评论数</li>
                </ul>
              </div>
            </el-collapse-item>
          </el-collapse>
        </section>
      </div>
    </main>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';

export default {
  name: 'UserGuidePage',
  setup() {
    const activeNames = ref(['register']);

    onMounted(() => {
      document.title = '使用指南 - LEAF-BOSS';
    });

    return {
      activeNames
    };
  }
}
</script>

<style scoped>
.user-guide-page {
  min-height: 100vh;
  background-color: #ffffff;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.page-header {
  background-color: #1c1e54;
  color: white;
  padding: 80px 0;
  text-align: center;
}

.page-header h1 {
  font-size: 48px;
  font-weight: 300;
  line-height: 1.15;
  margin-bottom: 16px;
  letter-spacing: -0.96px;
}

.page-header p {
  font-size: 18px;
  font-weight: 300;
  line-height: 1.40;
  color: rgba(255, 255, 255, 0.7);
  max-width: 600px;
  margin: 0 auto;
}

.page-content {
  padding: 80px 0;
  background-color: #f8fafc;
}

.guide-card {
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
  padding: 48px;
  transition: box-shadow 0.3s ease;
}

.guide-card:hover {
  box-shadow: rgba(23, 23, 23, 0.08) 0px 6px 16px;
}

.guide-card h3 {
  font-size: 22px;
  font-weight: 300;
  line-height: 1.10;
  margin: 0 0 8px 0;
  color: #061b31;
  letter-spacing: -0.22px;
}

.guide-subtitle {
  font-size: 14px;
  font-weight: 300;
  line-height: 1.40;
  color: #64748d;
  margin: 0 0 32px 0;
}

.collapse-content {
  padding: 16px 0;
}

.collapse-content p {
  font-size: 14px;
  font-weight: 300;
  line-height: 1.40;
  color: #64748d;
  margin: 0 0 12px 0;
}

.collapse-content ul {
  margin: 12px 0;
  padding-left: 20px;
}

.collapse-content li {
  font-size: 14px;
  font-weight: 300;
  line-height: 1.60;
  color: #64748d;
  margin-bottom: 8px;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

@media (max-width: 768px) {
  .page-header {
    padding: 60px 0;
  }

  .page-header h1 {
    font-size: 32px;
    letter-spacing: -0.64px;
  }

  .page-header p {
    font-size: 16px;
  }

  .page-content {
    padding: 40px 0;
  }

  .guide-card {
    padding: 32px 24px;
  }

  .guide-card h3 {
    font-size: 18px;
    letter-spacing: -0.18px;
  }

  .collapse-content p,
  .collapse-content li {
    font-size: 13px;
  }
}
</style>
