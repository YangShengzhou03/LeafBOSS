<template>
  <div class="author-info-page">
    <header class="page-header">
      <div class="container">
        <h1>关于作者</h1>
        <p>LEAF-BOSS · 独立开发者的技术实践与产品探索</p>
      </div>
    </header>

    <main class="page-content">
      <div class="container">
        <div class="author-sections">
          <section class="author-card">
            <div class="author-profile">
              <div class="avatar-circle">
                <span class="avatar-text">Yang</span>
              </div>
              <div class="profile-info">
                <div class="name-title">
                  <h2>Yangshengzhou</h2>
                  <span class="developer-badge">独立开发者</span>
                </div>
                <div class="expertise-tags">
                  <span class="tag">Vue.js</span>
                  <span class="tag">Spring Boot</span>
                  <span class="tag">MySQL</span>
                  <span class="tag">Docker</span>
                  <span class="tag">Microservices</span>
                </div>
                <p class="profile-bio">
                  全栈独立开发者，专注于从零到一构建实用产品。深入研究用户需求，独立完成从产品设计、前后端开发到部署上线的全流程。
                  已成功打造 LEAF-BOSS、LEAFTIDY、LEAFAUTO 等多个项目，
                  致力于将创意转化为高效、实用的工具产品。
                </p>
              </div>
            </div>
          </section>

          <section class="timeline-card">
            <h3>开发历程</h3>
            <div class="timeline">
              <div class="timeline-item">
                <div class="timeline-marker"></div>
                <div class="timeline-content">
                  <h4>项目启动 · 2025年6月</h4>
                  <p>分析业务运营痛点，确立系统架构与技术选型，采用 Vue 3 + Spring Boot 3 现代化技术栈。</p>
                </div>
              </div>
              <div class="timeline-item">
                <div class="timeline-marker"></div>
                <div class="timeline-content">
                  <h4>核心开发 · 2025年9月</h4>
                  <p>完成核心功能开发，实现授权管理、权限控制、商品规格配置等关键业务模块。</p>
                </div>
              </div>
              <div class="timeline-item">
                <div class="timeline-marker"></div>
                <div class="timeline-content">
                  <h4>正式发布 · 2026年1月</h4>
                  <p>系统正式上线并持续迭代优化，为用户提供稳定可靠的业务运营支持。</p>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { onMounted } from 'vue';

export default {
  name: 'AuthorInfoPage',
  setup() {
    onMounted(() => {
      document.title = '关于作者 - LEAF-BOSS';
    });
  }
}
</script>

<style scoped>
.author-info-page {
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

.author-sections {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.author-card {
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
  padding: 48px;
  transition: box-shadow 0.3s ease;
}

.author-card:hover {
  box-shadow: rgba(23, 23, 23, 0.08) 0px 6px 16px;
}

.author-profile {
  display: flex;
  align-items: flex-start;
  gap: 40px;
  max-width: 800px;
  margin: 0 auto;
}

.avatar-circle {
  flex-shrink: 0;
  width: 120px;
  height: 120px;
  background-color: #533afd;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
}

.avatar-text {
  font-size: 36px;
  font-weight: 300;
  color: white;
  letter-spacing: -0.72px;
}

.profile-info {
  flex: 1;
}

.name-title {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.name-title h2 {
  font-size: 32px;
  font-weight: 300;
  line-height: 1.10;
  margin: 0;
  color: #061b31;
  letter-spacing: -0.64px;
}

.developer-badge {
  background-color: #533afd;
  color: white;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 400;
}

.expertise-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.tag {
  background-color: #f8fafc;
  color: #061b31;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 400;
  border: 1px solid #e5edf5;
}

.profile-bio {
  font-size: 16px;
  font-weight: 300;
  line-height: 1.40;
  color: #64748d;
  margin: 0;
}

.timeline-card {
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
  padding: 48px;
  transition: box-shadow 0.3s ease;
}

.timeline-card:hover {
  box-shadow: rgba(23, 23, 23, 0.08) 0px 6px 16px;
}

.timeline-card h3 {
  font-size: 22px;
  font-weight: 300;
  line-height: 1.10;
  margin: 0 0 32px 0;
  color: #061b31;
  letter-spacing: -0.22px;
}

.timeline {
  position: relative;
  padding-left: 32px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 4px;
  bottom: 4px;
  width: 2px;
  background-color: #e5edf5;
}

.timeline-item {
  position: relative;
  margin-bottom: 24px;
}

.timeline-item:last-child {
  margin-bottom: 0;
}

.timeline-marker {
  position: absolute;
  left: -28px;
  top: 8px;
  width: 12px;
  height: 12px;
  background-color: #533afd;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 0 0 2px #533afd;
}

.timeline-content h4 {
  font-size: 16px;
  font-weight: 400;
  margin-bottom: 8px;
  color: #061b31;
}

.timeline-content p {
  font-size: 14px;
  font-weight: 300;
  line-height: 1.40;
  color: #64748d;
  margin: 0;
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

  .author-card {
    padding: 32px 24px;
  }

  .author-profile {
    flex-direction: column;
    text-align: center;
    gap: 24px;
  }

  .avatar-circle {
    width: 100px;
    height: 100px;
    margin: 0 auto;
  }

  .avatar-text {
    font-size: 28px;
    letter-spacing: -0.56px;
  }

  .name-title {
    flex-direction: column;
    gap: 12px;
  }

  .name-title h2 {
    font-size: 26px;
    letter-spacing: -0.52px;
  }

  .expertise-tags {
    justify-content: center;
  }

  .profile-bio {
    font-size: 14px;
  }

  .timeline-card {
    padding: 32px 24px;
  }

  .timeline-card h3 {
    font-size: 18px;
    letter-spacing: -0.18px;
    margin-bottom: 24px;
  }
}
</style>
