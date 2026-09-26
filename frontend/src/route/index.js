import { createRouter, createWebHistory } from 'vue-router';
import store from '@/utils/store.js';
import * as utils from '@/utils/utils.js';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/UserLoginPage.vue'),
    meta: {
      title: '用户登录 - LEAF-BOSS',
      requiresAuth: false
    }
  },
  {
    path: '/admin-login',
    name: 'AdminLogin',
    component: () => import('@/views/AdminLoginPage.vue'),
    meta: {
      title: '管理员登录 - LEAF-BOSS',
      requiresAdmin: false
    }
  },
  {
    path: '/user',
    component: () => import('@/components/UserLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'UserDashboard',
        component: () => import('@/views/user/UserHome.vue'),
        meta: {
          title: '用户中心 - LEAF-BOSS',
          requiresAuth: true
        }
      },
      {
        path: 'card-keys',
        name: 'UserCardKeys',
        component: () => import('@/views/user/card/UserCardKeyManagement.vue'),
        meta: {
          title: 'LEAF-BOSS - 卡密列表',
          requiresAuth: true
        }
      },
      {
        path: 'card-verify',
        name: 'UserCardVerify',
        component: () => import('@/views/user/card/UserCardKeyVerify.vue'),
        meta: {
          title: 'LEAF-BOSS - 卡密验证',
          requiresAuth: true
        }
      },
      {
        path: 'card-generate',
        name: 'UserCardGenerate',
        component: () => import('@/views/user/card/UserCardKeyGenerate.vue'),
        meta: {
          title: 'LEAF-BOSS - 卡密生成',
          requiresAuth: true
        }
      },
      {
        path: 'card-redeem',
        name: 'UserCardRedeem',
        component: () => import('@/views/user/card/UserCardKeyRedeem.vue'),
        meta: {
          title: 'LEAF-BOSS - 商品兑换',
          requiresAuth: true
        }
      },
      {
        path: 'agent-authorizations',
        name: 'UserAgentAuthorizations',
        component: () => import('@/views/user/agent/AgentAuthorizations.vue'),
        meta: {
          title: 'LEAF-BOSS - 授权列表',
          requiresAuth: true
        }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/UserProfile.vue'),
        meta: {
          title: 'LEAF-BOSS - 个人资料',
          requiresAuth: true
        }
      }
    ]
  },

  {
    path: '/',
    name: 'HomePage',
    component: () => import('@/components/IndexLayout.vue'),
    meta: {
      title: '个人全栈技术学习',
      requiresAuth: false
    }
  },

  {
    path: '/admin',
    component: () => import('@/components/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/DashboardPage.vue'),
        meta: {
          title: 'LEAF-BOSS - 管理员仪表盘',
          requiresAdmin: true
        }
      },
      {
        path: 'admins',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminsPage.vue'),
        meta: {
          title: 'LEAF-BOSS - 管理人员',
          requiresAdmin: true
        }
      },
      {
        path: 'users',
        name: 'CustomerUsers',
        component: () => import('@/views/admin/CustomerUsersPage.vue'),
        meta: {
          title: 'LEAF-BOSS - 用户管理',
          requiresAdmin: true
        }
      },
      {
        path: 'authorizations',
        name: 'AuthorizationManagement',
        component: () => import('@/views/admin/AuthorizationManagement.vue'),
        meta: {
          title: 'LEAF-BOSS - 授权管理',
          requiresAdmin: true
        }
      },
      {
        path: 'agent-authorizations',
        name: 'AgentAuthorizationManagement',
        component: () => import('@/views/admin/AgentAuthorizationManagement.vue'),
        meta: {
          title: 'LEAF-BOSS - 代理授权管理',
          requiresAdmin: true
        }
      },

      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/LogsPage.vue'),
        meta: {
          title: 'LEAF-BOSS - 操作日志',
          requiresAdmin: true
        }
      },
      {
        path: 'products',
        name: 'ProductManagement',
        component: () => import('@/views/admin/ProductManagement.vue'),
        meta: {
          title: 'LEAF-BOSS - 商品管理',
          requiresAdmin: true
        }
      },
      {
        path: 'card-keys',
        name: 'CardKeyManagement',
        component: () => import('@/views/admin/card/CardKeyManagement.vue'),
        meta: {
          title: 'LEAF-BOSS - 卡密管理',
          requiresAdmin: true
        }
      },
      {
        path: 'card-verify',
        name: 'CardKeyVerifyAdmin',
        component: () => import('@/views/admin/card/CardKeyVerify.vue'),
        meta: {
          title: 'LEAF-BOSS - 卡密验证',
          requiresAdmin: true
        }
      },
      {
        path: 'card-generate',
        name: 'CardKeyGenerate',
        component: () => import('@/views/admin/card/CardKeyGenerate.vue'),
        meta: {
          title: 'LEAF-BOSS - 卡密生成',
          requiresAdmin: true
        }
      },
      {
        path: 'profile',
        name: 'ProfilePage',
        component: () => import('@/views/admin/ProfilePage.vue'),
        meta: {
          title: 'LEAF-BOSS - 个人资料',
          requiresAdmin: true
        }
      },
      {
        path: 'product-specs',
        name: 'ProductSpecManagement',
        component: () => import('@/views/admin/ProductSpecManagement.vue'),
        meta: {
          title: 'LEAF-BOSS - 规格管理',
          requiresAdmin: true
        }
      },
      {
        path: 'jobs/companies',
        name: 'CompanyManagement',
        component: () => import('@/views/admin/jobs/CompanyManagement.vue'),
        meta: {
          title: 'LEAF-BOSS - 公司管理',
          requiresAdmin: true
        }
      },
      {
        path: 'jobs/boss-reviews',
        name: 'BossReviewManagement',
        component: () => import('@/views/admin/jobs/BossReviewManagement.vue'),
        meta: {
          title: 'LEAF-BOSS - 评论管理',
          requiresAdmin: true
        }
      },
      {
        path: 'notices',
        name: 'NoticeManagement',
        component: () => import('@/views/admin/NoticeManagement.vue'),
        meta: {
          title: 'LEAF-BOSS - 通知管理',
          requiresAdmin: true
        }
      },
      {
        path: 'product-feedback',
        name: 'ProductFeedback',
        component: () => import('@/views/admin/ProductFeedback.vue'),
        meta: {
          title: 'LEAF-BOSS - 商品反馈',
          requiresAdmin: true
        }
      },
    ]
  },

  {
    path: '/user-guide',
    name: 'UserGuidePage',
    component: () => import('@/views/index/UserGuidePage.vue'),
    meta: {
      title: '使用指南 - LEAF-BOSS',
      requiresAuth: false
    }
  },
  {
    path: '/author-info',
    name: 'AuthorInfoPage',
    component: () => import('@/views/index/AuthorInfoPage.vue'),
    meta: {
      title: '作者介绍 - LEAF-BOSS',
      requiresAuth: false
    }
  },
  {
    path: '/privacy-policy',
    name: 'PrivacyPolicyPage',
    component: () => import('@/views/index/PrivacyPolicyPage.vue'),
    meta: {
      title: '隐私保护 - LEAF-BOSS',
      requiresAuth: false
    }
  },

  {
    path: '/verify',
    name: 'CardKeyVerify',
    component: () => import('@/views/admin/card/CardKeyVerify.vue'),
    meta: {
      title: 'LEAF-BOSS - 卡密验证',
      requiresAuth: false
    }
  },

  { path: '/:pathMatch(.*)*', redirect: '/' }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title;
  }

  if (to.meta.requiresAuth || to.meta.requiresAdmin) {
    const role = to.meta.requiresAdmin ? 'admin' : (store.state.isAdmin ? 'admin' : 'user')
    const isAuthenticated = await store.checkAuthStatus();

    if (!isAuthenticated) {
      store.clearUser();
      next('/login');
      return;
    }

    if (!store.state.user) {
      try {
        await store.fetchCurrentUser(role);
      } catch (error) {
        store.clearUser();
        next('/login');
        return;
      }
    }

    if (to.meta.requiresAdmin && !store.state.isAdmin) {
      next('/user');
      return;
    }
  }

  if (to.path === '/login' && utils.isLoggedIn()) {
    const isAuthenticated = await store.checkAuthStatus();
    if (isAuthenticated) {
      next(store.state.isAdmin ? '/admin' : '/user');
      return;
    } else {
      store.clearUser();
    }
  }

  next();
});

export default router;
