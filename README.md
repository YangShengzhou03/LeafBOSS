# LEAF-BOSS - 业务运营支撑系统

<div align="center">

[![GitHub stars](https://img.shields.io/github/stars/YangShengzhou03/LeafBoss?style=for-the-badge&logo=github)](https://github.com/YangShengzhou03/LeafBoss/stargazers)&nbsp;[![GitHub forks](https://img.shields.io/github/forks/YangShengzhou03/LeafBoss?style=for-the-badge&logo=github)](https://github.com/YangShengzhou03/LeafBoss/network/members)&nbsp;[![GitHub issues](https://img.shields.io/github/issues/YangShengzhou03/LeafBoss?style=for-the-badge&logo=github)](https://github.com/YangShengzhou03/LeafBoss/issues)&nbsp;[![GitHub license](https://img.shields.io/github/license/YangShengzhou03/LeafBoss?style=for-the-badge)](https://github.com/YangShengzhou03/LeafBoss/blob/main/LICENSE)&nbsp;[![Vue.js](https://img.shields.io/badge/Vue.js-3.4.0-42b883?style=for-the-badge&logo=vuedotjs)](https://vuejs.org/)&nbsp;[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.0-6DB33F?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)

**个人全栈技术学习项目，采用前后端分离架构**

[快速开始](#-快速开始) • [功能特性](#-功能特性) • [在线演示](#-在线演示) • [API文档](API_DOCUMENTATION.md)

</div>

## 功能特性

LeafBoss 是面向业务运营场景的全栈管理系统。卡密模块支持批量生成、导出、验证、激活和禁用，覆盖完整生命周期。商品与规格模块支持商品信息维护和多规格灵活配置，可定义不同时长和价格组合。

公司与评论模块提供公司信息维护、状态管理、评论统计，支持按公司或卡密筛选评论。人员管理包含管理员账户与客户用户管理，支持权限控制与状态管理。

数据方面提供卡密库存分布、当日销量、月度营收对比等可视化统计，以及覆盖登录、卡密、商品、规格、用户等关键操作的操作日志审计。系统采用 JWT Token 认证，响应式设计适配 PC、平板、手机，基于 Spring Boot 3 + Vue 3 构建。

### 系统界面预览

![LEAF-BOSS 业务运营支撑系统](https://gitee.com/Yangshengzhou/leaf-boss/raw/master/assets/LEAF-BOSS-Overview.png)

![LEAF-BOSS 登录页面](https://gitee.com/Yangshengzhou/leaf-boss/raw/master/assets/LEAF-BOSS-Login.png)

## 快速开始

系统默认提供了一个管理员账户，邮箱为 admin@qq.com，密码为 123456，使用这个账户可以登录系统并访问所有管理功能。

![LEAF-BOSS 管理员仪表盘](https://gitee.com/Yangshengzhou/leaf-boss/raw/master/assets/LEAF-BOSS-Dashboard.png)

![LEAF-BOSS 商品管理](https://gitee.com/Yangshengzhou/leaf-boss/raw/master/assets/LEAF-BOSS-Product-Management.png)

![LEAF-BOSS 管理人员](https://gitee.com/Yangshengzhou/leaf-boss/raw/master/assets/LEAF-BOSS-Admin-Management.png)

## 系统架构

LeafBoss 采用经典的三层架构设计，包括前端界面层、后端服务层和数据存储层。前端界面层使用 Vue 3 和 Element Plus 构建，提供友好的用户交互界面，支持组件化开发和响应式设计，确保在不同设备上都有良好的用户体验。后端服务层基于 Spring Boot 3 框架，使用 MyBatis Plus 进行数据访问，采用 JWT Token 进行用户认证，提供完整的业务逻辑处理和事务管理功能。数据存储层使用 MySQL 8.0 数据库，通过 Repository 模式进行数据映射和持久化操作，确保数据的安全性和一致性。

前端技术栈包括 Vue 3.4.0 作为渐进式 JavaScript 框架，Element Plus 2.14.3 作为基于 Vue 3 的 UI 组件库，Vue Router 4.2.0 作为官方路由管理器，Axios 1.6.0 作为 HTTP 客户端库，Sass 1.69.0 作为 CSS 预处理器。后端技术栈包括 Spring Boot 3.1.0 作为 Java 企业级开发框架，MyBatis Plus 3.5.4.1 作为数据持久层框架，MySQL 8.0.33 作为关系型数据库，Maven 3.6+ 作为项目构建工具，Java 17.0+ 作为开发语言，JWT 0.11.5 作为 JSON Web Token 认证工具。

项目结构分为前端和后端两个主要部分。前端项目包含 public 静态资源目录和 src 源代码目录，src 目录下包含 components 公共组件、views 页面组件、route 路由配置、services API 服务和 utils 工具函数等模块。后端项目包含 src/main/java Java 源代码目录和 src/main/resources 资源文件目录，Java 源代码目录下包含 controller 控制器层、service 服务层、mapper 数据访问层、entity 实体类、dto 数据传输对象、config 配置类、common 公共类和 utils 工具类等模块。这种清晰的分层架构使得系统易于维护和扩展。

## 部署说明

### Docker 部署

系统支持使用 Docker 进行容器化部署，前端使用 nginx:alpine 作为基础镜像提供静态文件服务，后端使用 openjdk:17-alpine 运行 Spring Boot 应用。以下提供两种部署方式：

#### 使用 Docker Compose 一键部署（推荐）

LeafBoss 已在 Docker Hub 发布预构建镜像，可直接使用 docker-compose 一键部署。镜像仓库地址：`yangshengzhou/leafboss`。

```bash
curl -sSL https://gitee.com/Yangshengzhou/leaf-boss/raw/master/docker-compose.yml -o docker-compose.yml && docker-compose up -d && sleep 20 && curl -sSL https://gitee.com/Yangshengzhou/leaf-boss/raw/master/data.sql -o data.sql && docker exec -i leafboss-mysql mysql -uroot -p123456 < data.sql && docker ps
```

部署完成后访问 `http://服务器IP`，默认账号：`admin@qq.com`，密码：`123456`。

如需要删除容器，执行以下命令：

```bash
docker-compose down -v
```

#### 使用 Docker Hub 镜像部署（海外用户）

> 镜像源：Docker Hub（`yangshengzhou/leafboss`），国内访问可能超时。

**1. 拉取镜像**

```bash
docker pull yangshengzhou/leafboss:frontend-v1
docker pull yangshengzhou/leafboss:backend-v1
docker pull yangshengzhou/leafboss:https-nginx-v1
curl -sSL https://gitee.com/Yangshengzhou/leaf-boss/raw/master/data.sql -o data.sql
```

**2. 创建容器网络**

```bash
docker network create leafboss-network
```

**3. 启动 MySQL 并初始化**

```bash
docker run -d \
  --name leafboss-mysql \
  --network leafboss-network \
  --restart always \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=leaf_boss \
  -p 3306:3306 \
  mysql:8.0

# 等待 30 秒后执行初始化
docker exec -i leafboss-mysql mysql -uroot -p123456 < data.sql
```

**4. 启动后端**

```bash
docker run -d \
  --name leafboss-backend \
  --network leafboss-network \
  --restart always \
  -p 8081:8080 \
  -e SMTP_PASSWORD=你的授权码 \
  yangshengzhou/leafboss:backend-v1
```

**5. 启动前端**

```bash
docker run -d \
  --name leafboss-frontend \
  --network leafboss-network \
  --restart always \
  yangshengzhou/leafboss:frontend-v1
```

**6. 部署 HTTPS 代理（可选，全站加密）**

```bash
mkdir -p /data/nginx/certs/
# 将证书 leafboss.top_certificate.pem 和私钥 leafboss.top_private.key 放入 /data/nginx/certs/
cd /data/nginx/certs/
docker run -d \
  --name leafboss-https-nginx \
  --network leafboss-network \
  --restart always \
  -p 80:80 \
  -p 443:443 \
  -v $(pwd)/leafboss.top_certificate.pem:/data/nginx/certs/leafboss.top_certificate.pem \
  -v $(pwd)/leafboss.top_private.key:/data/nginx/certs/leafboss.top_private.key \
  yangshengzhou/leafboss:https-nginx-v1
```

#### 使用阿里云 ACR 镜像部署（国内用户，推荐）

> 镜像源：阿里云个人容器镜像服务（ACR），国内网络不会超时。仓库公开，**无需 docker login**。

**1. 拉取镜像**

```bash
# 拉取 backend 和 frontend
docker pull crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/backend:backend-v1
docker pull crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/frontend:frontend-v1

# 可选：重命名为短名称方便使用
docker tag crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/backend:backend-v1 leafboss:backend-v1
docker tag crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/frontend:frontend-v1 leafboss:frontend-v1

curl -sSL https://gitee.com/Yangshengzhou/leaf-boss/raw/master/data.sql -o data.sql
```

**2. 创建容器网络**

```bash
docker network create leafboss-network
```

**3. 启动 MySQL 并初始化**

```bash
docker run -d \
  --name leafboss-mysql \
  --network leafboss-network \
  --restart always \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=leaf_boss \
  -p 3306:3306 \
  mysql:8.0

# 等待 30 秒后执行初始化
docker exec -i leafboss-mysql mysql -uroot -p123456 < data.sql
```

**4. 启动后端**

```bash
docker run -d \
  --name leafboss-backend \
  --network leafboss-network \
  --restart always \
  -p 8081:8080 \
  -e SMTP_PASSWORD=你的授权码 \
  leafboss:backend-v1
```

**5. 启动前端**

```bash
docker run -d \
  --name leafboss-frontend \
  --network leafboss-network \
  --restart always \
  leafboss:frontend-v1
```

### 4. 常用运维命令

#### 4.1 查看容器网络配置

```bash
docker network inspect leafboss-network
```

#### 4.2 停止/删除所有容器

```bash
# 停止容器（包含 HTTPS 代理容器）
docker stop leafboss-https-nginx leafboss-frontend leafboss-backend leafboss-mysql
# 删除容器
docker rm leafboss-https-nginx leafboss-frontend leafboss-backend leafboss-mysql
```

#### 4.3 删除 Docker 网络

```bash
docker network rm leafboss-network
```

### 5. 部署注意事项

1. 若访问 HTTPS 提示证书错误，需检查 SSL 证书文件路径与 Nginx 配置是否匹配；
2. 服务器需开放 80/443 端口（防火墙/安全组），否则无法访问 HTTPS 服务。

#### 环境变量配置

后端服务支持以下环境变量配置：

| 环境变量                   | 说明                             | 默认值                                                              |
| -------------------------- | -------------------------------- | ------------------------------------------------------------------- |
| SPRING_DATASOURCE_URL      | 数据库连接地址                   | jdbc:mysql://localhost:3306/leaf_boss                               |
| SPRING_DATASOURCE_USERNAME | 数据库用户名                     | root                                                                |
| SPRING_DATASOURCE_PASSWORD | 数据库密码                       | 123456                                                              |
| APP_JWT_SECRET             | JWT 密钥（Base64）               | bGVhZi1ib3NzLXNlY3JldC1rZXktZm9yLWp3dC10b2tlbi1nZW5lcmF0aW9uLXBsZWFzZS1jaGFuZ2U= |
| APP_JWT_EXPIRATION         | JWT 过期时间（毫秒）             | 7200000                                                             |
| APP_JWT_RENEW_THRESHOLD    | JWT 续期阈值（毫秒）             | 1800000                                                             |
| APP_JWT_MAX_AGE            | JWT 最大有效期（毫秒）           | 604800000                                                           |
| SMTP_PASSWORD              | SMTP 邮箱授权码（QQ 邮箱）       | -                                                                   |
| SERVER_PORT                | 服务器端口                       | 8081                                                                |

#### 故障排查

**MySQL 连接失败**

检查 MySQL 容器是否正常运行：

```bash
docker logs leafboss-mysql
```

**后端启动失败**

检查后端容器日志：

```bash
docker logs leafboss-backend
```

**前端无法访问后端**

确认所有容器都在同一个网络中：

```bash
docker network inspect leafboss-network
```

**数据库初始化失败**

确保 data.sql 文件在正确的位置，并且 MySQL 容器有权限访问该文件。

### 参与贡献

我们欢迎任何形式的贡献，包括代码贡献、文档改进、问题反馈等。参与贡献的流程包括：首先 Fork 本仓库到自己的 GitHub 账户，然后新建功能分支，使用 git checkout -b feature/AmazingFeature 命令创建分支。在分支上进行开发，完成后使用 git commit -m 'Add some AmazingFeature' 提交代码，提交信息应该清晰描述所做的修改。然后使用 git push origin feature/AmazingFeature 将分支推送到自己的仓库，最后在 GitHub 上新建 Pull Request，等待项目维护者审核和合并代码。贡献代码时应该遵循项目的代码规范，编写清晰的提交信息，添加适当的测试用例，并更新相关文档。

## 许可证

本项目采用 GNU Affero General Public License v3.0 许可证，这是一种开源许可证，确保用户可以自由使用、修改和分发代码。查看 LICENSE 文件可以了解许可证的详细条款和条件。

## 联系方式

如果您在使用 LeafBoss 过程中遇到任何问题，或者有任何建议和意见，欢迎通过以下方式联系我们。GitHub 仓库地址是 https://github.com/YangShengzhou03/LeafBoss，您可以在仓库中查看源代码、提交问题或参与贡献。问题反馈可以通过 GitHub Issues 进行，我们会及时回复和处理您的问题。邮箱地址是 yangsz03@foxmail.com，您可以通过邮件与我们联系。

## 项目统计

![GitHub Last Commit](https://img.shields.io/github/last-commit/YangShengzhou03/LeafBoss?style=flat-square)
![GitHub Contributors](https://img.shields.io/github/contributors/YangShengzhou03/LeafBoss?style=flat-square)
![GitHub Repo Size](https://img.shields.io/github/repo-size/YangShengzhou03/LeafBoss?style=flat-square)

---

**感谢使用 LeafBoss！**

<div align="center">

如果这个项目对您有帮助，请给个 Star 支持一下！

[![Star History Chart](https://api.star-history.com/svg?repos=YangShengzhou03/LeafBoss&type=Date)](https://star-history.com/#YangShengzhou03/LeafBoss&Date)

</div>
