# LEAF-BOSS - Business Operation Support System

<div align="center">

[![GitHub stars](https://img.shields.io/github/stars/YangShengzhou03/LeafBoss?style=for-the-badge&logo=github)](https://github.com/YangShengzhou03/LeafBoss/stargazers)&nbsp;[![GitHub forks](https://img.shields.io/github/forks/YangShengzhou03/LeafBoss?style=for-the-badge&logo=github)](https://github.com/YangShengzhou03/LeafBoss/network/members)&nbsp;[![GitHub issues](https://img.shields.io/github/issues/YangShengzhou03/LeafBoss?style=for-the-badge&logo=github)](https://github.com/YangShengzhou03/LeafBoss/issues)&nbsp;[![GitHub license](https://img.shields.io/github/license/YangShengzhou03/LeafBoss?style=for-the-badge)](https://github.com/YangShengzhou03/LeafBoss/blob/main/LICENSE)&nbsp;[![Vue.js](https://img.shields.io/badge/Vue.js-3.4.0-42b883?style=for-the-badge&logo=vuedotjs)](https://vuejs.org/)&nbsp;[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.0-6DB33F?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)

**Personal full-stack technology learning project with separated frontend and backend architecture**

[Quick Start](#-quick-start) • [Features](#-features) • [Live Demo](#-live-demo) • [API Documentation](API_DOCUMENTATION.md)

</div>

## Features

LeafBoss is a full-stack management system for business operations. The card key module supports batch generation, export, verification, activation, and deactivation, covering the complete lifecycle. The product and specification module supports product information maintenance and flexible multi-specification configuration with different durations and pricing combinations.

The company and comment module provides company information maintenance, status management, and comment statistics, with filtering by company or card key. User management includes administrator accounts and customer user management, with permission control and control management.

Data features include visualized statistics for card key inventory distribution, daily sales, and monthly revenue comparison, plus operation audit logs covering login, card keys, products, specifications, and users. The system uses JWT Token authentication, responsive design for PC/tablet/mobile, built on Spring Boot 3 + Vue 3.

### System Preview

![LEAF-BOSS Business Operation Support System](https://gitee.com/Yangshengzhou/leaf-boss/raw/master/assets/LEAF-BOSS-Overview.png)

![LEAF-BOSS Login Page](https://gitee.com/Yangshengzhou/leaf-boss/raw/master/assets/LEAF-BOSS-Login.png)

## Quick Start

Default admin account: `admin@qq.com` / `123456`

![LEAF-BOSS Admin Dashboard](https://gitee.com/Yangshengzhou/leaf-boss/raw/master/assets/LEAF-BOSS-Dashboard.png)

![LEAF-BOSS Product Management](https://gitee.com/Yangshengzhou/leaf-boss/raw/master/assets/LEAF-BOSS-Product-Management.png)

![LEAF-BOSS Admin Management](https://gitee.com/Yangshengzhou/leaf-boss/raw/master/assets/LEAF-BOSS-Admin-Management.png)

## System Architecture

LeafBoss uses a classic three-tier architecture: frontend layer, backend layer, and data layer. The frontend uses Vue 3 + Element Plus with component-based development and responsive design. The backend uses Spring Boot 3 with MyBatis Plus for data access and JWT Token for authentication. The data layer uses MySQL 8.0 with MyBatis-Plus Mapper for data mapping and persistence.

Frontend stack: Vue 3.4.0, Element Plus 2.14.3, Vue Router 4.2.0, Axios 1.6.0, Sass 1.69.0. Backend stack: Spring Boot 3.1.0, MyBatis Plus 3.5.4.1, MySQL 8.0.33, Maven 3.6+, Java 17+, JWT 0.11.5.

## Deployment

### Docker

Supports containerized deployment. Frontend uses nginx:alpine for static file serving, backend uses eclipse-temurin:17-alpine for Spring Boot. Two deployment methods below:

#### Docker Compose (Recommended)

Pre-built images are published on Aliyun ACR and Docker Hub. The compose file pulls from Aliyun ACR by default (recommended in China), registry: `crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss`. These images are release builds; to deploy the latest code from this repository, build and push the images yourself.

```bash
curl -sSL https://gitee.com/Yangshengzhou/leaf-boss/raw/master/docker-compose.yml -o docker-compose.yml
curl -sSL https://gitee.com/Yangshengzhou/leaf-boss/raw/master/data.sql -o data.sql

# The first two are required (docker compose refuses to start without them); SMTP_PASSWORD is optional
# APP_JWT_SECRET must be a Base64-encoded random string (at least 32 bytes decoded); generated here
APP_JWT_SECRET=$(openssl rand -base64 32)

cat > .env <<EOF
MYSQL_ROOT_PASSWORD=change_me_to_a_strong_password
APP_JWT_SECRET=$APP_JWT_SECRET
SMTP_PASSWORD=your_qq_mail_authorization_code
EOF

docker compose up -d
docker exec -i leafboss-mysql mysql -uroot -p'YOUR_MYSQL_PASSWORD' < data.sql
```

Visit `http://SERVER_IP` after deployment. Default: `admin@qq.com` / `123456` (change it right after the first login).

Remove: `docker compose down -v`

#### Manual Docker Deployment (docker run)

Pick an image source based on your network; steps 2 onward are identical.

**1. Pull images**

China mainland (Aliyun ACR, recommended, no docker login):

```bash
docker pull crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/backend:backend-v1
docker pull crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/frontend:frontend-v1

# Retag to the short name so the commands below work unchanged
docker tag crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/backend:backend-v1 yangshengzhou/leafboss:backend-v1
docker tag crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/frontend:frontend-v1 yangshengzhou/leafboss:frontend-v1

# Drop the original tags (the image stays under the short name)
docker rmi crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/backend:backend-v1
docker rmi crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/frontend:frontend-v1
```

Overseas (Docker Hub, may timeout in China):

```bash
docker pull yangshengzhou/leafboss:backend-v1
docker pull yangshengzhou/leafboss:frontend-v1
```

```bash
curl -sSL https://gitee.com/Yangshengzhou/leaf-boss/raw/master/data.sql -o data.sql
```

**2. Create network**

```bash
docker network create leafboss-network
```

**3. Start MySQL and initialize**

```bash
docker run -d \
  --name leafboss-mysql \
  --network leafboss-network \
  --restart always \
  -e MYSQL_ROOT_PASSWORD=change_me_to_a_strong_password \
  -e MYSQL_DATABASE=leaf_boss \
  -p 127.0.0.1:3306:3306 \
  mysql:8.0

# Wait 30 seconds before initializing
docker exec -i leafboss-mysql mysql -uroot -p'YOUR_MYSQL_PASSWORD' < data.sql
```

**4. Start backend**

`APP_JWT_SECRET` is generated randomly on first creation. Reuse the same value when recreating the container, otherwise all issued sessions are invalidated.

```bash
docker run -d \
  --name leafboss-backend \
  --network leafboss-network \
  --restart always \
  -p 127.0.0.1:8081:8081 \
  -e SPRING_DATASOURCE_PASSWORD=YOUR_MYSQL_PASSWORD \
  -e APP_JWT_SECRET="$(openssl rand -base64 32)" \
  -e SMTP_PASSWORD=your_auth_code \
  yangshengzhou/leafboss:backend-v1
```

**5. Start frontend**

```bash
docker run -d \
  --name leafboss-frontend \
  --network leafboss-network \
  --restart always \
  yangshengzhou/leafboss:frontend-v1
```

**6. Deploy HTTPS proxy (optional, full-site encryption)**

> The `https-nginx` image is published on Docker Hub only (`yangshengzhou/leafboss:https-nginx-v1`); ACR users pull it from Docker Hub, or just run the command below to pull it automatically.

```bash
mkdir -p /data/nginx/certs/
# Place leafboss.top_certificate.pem and leafboss.top_private.key in /data/nginx/certs/
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

### Common Commands

#### Check network configuration

```bash
docker network inspect leafboss-network
```

#### Stop / remove all containers

```bash
docker stop leafboss-https-nginx leafboss-frontend leafboss-backend leafboss-mysql
docker rm leafboss-https-nginx leafboss-frontend leafboss-backend leafboss-mysql
```

#### Remove Docker network

```bash
docker network rm leafboss-network
```

### Deployment Notes

1. HTTPS certificate error: check certificate path matches Nginx config
2. Server must open ports 80/443 (firewall/security group)

#### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| SPRING_DATASOURCE_URL | Database URL | jdbc:mysql://localhost:3306/leaf_boss |
| SPRING_DATASOURCE_USERNAME | Database username | root |
| SPRING_DATASOURCE_PASSWORD | Database password | Required in prod, no default |
| APP_JWT_SECRET | JWT secret (Base64) | Required in compose; in-code default is for local dev only |
| APP_JWT_EXPIRATION | JWT expiration (ms) | 604800000 (7-day sliding window) |
| APP_JWT_RENEW_THRESHOLD | JWT renewal threshold (ms) | 259200000 (3 days) |
| APP_JWT_MAX_AGE | JWT max age (ms) | 2592000000 (30-day absolute limit) |
| SMTP_PASSWORD | SMTP authorization code (QQ Mail) | - |
| SERVER_PORT | Server port | 8081 |

#### Troubleshooting

**MySQL connection failed**

```bash
docker logs leafboss-mysql
```

**Backend startup failed**

```bash
docker logs leafboss-backend
```

**Frontend cannot access backend**

```bash
docker network inspect leafboss-network
```

**Database initialization failed**

Ensure data.sql is in the correct location and MySQL container has permission to access it.

### Contributing

We welcome all forms of contributions. Fork this repository, create a feature branch (`git checkout -b feature/xxx`), commit changes (`git commit -m 'Add xxx'`), push the branch (`git push origin feature/xxx`), and create a Pull Request.

## License

[GNU Affero General Public License v3.0](LICENSE)

## Contact

- GitHub: https://github.com/YangShengzhou03/LeafBoss
- Issues: GitHub Issues
- Email: yangsz03@foxmail.com

---

**Thank you for using LeafBoss!**

<div align="center">

If this project helps you, please give it a Star!

[![Star History Chart](https://api.star-history.com/svg?repos=YangShengzhou03/LeafBoss&type=Date)](https://star-history.com/#YangShengzhou03/LeafBoss&Date)

</div>
