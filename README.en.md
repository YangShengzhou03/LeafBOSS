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

LeafBoss uses a classic three-tier architecture: frontend layer, backend layer, and data layer. The frontend uses Vue 3 + Element Plus with component-based development and responsive design. The backend uses Spring Boot 3 with MyBatis Plus for data access and JWT Token for authentication. The data layer uses MySQL 8.0 with Repository pattern for data mapping and persistence.

Frontend stack: Vue 3.4.0, Element Plus 2.14.3, Vue Router 4.2.0, Axios 1.6.0, Sass 1.69.0. Backend stack: Spring Boot 3.1.0, MyBatis Plus 3.5.4.1, MySQL 8.0.33, Maven 3.6+, Java 17+, JWT 0.11.5.

## Deployment

### Docker

Supports containerized deployment. Frontend uses nginx:alpine for static file serving, backend uses openjdk:17-alpine for Spring Boot. Two deployment methods below:

#### Docker Compose (Recommended)

Pre-built images published on Docker Hub: `yangshengzhou/leafboss`.

```bash
curl -sSL https://gitee.com/Yangshengzhou/leaf-boss/raw/master/docker-compose.yml -o docker-compose.yml && docker-compose up -d && sleep 20 && curl -sSL https://gitee.com/Yangshengzhou/leaf-boss/raw/master/data.sql -o data.sql && docker exec -i leafboss-mysql mysql -uroot -p123456 < data.sql && docker ps
```

Visit `http://SERVER_IP` after deployment. Default: `admin@qq.com` / `123456`.

Remove: `docker-compose down -v`

#### Manual Docker Deployment (Docker Hub, for overseas users)

> Image source: Docker Hub (`yangshengzhou/leafboss`), may timeout in China.

**1. Pull images**

```bash
docker pull yangshengzhou/leafboss:frontend-v1
docker pull yangshengzhou/leafboss:backend-v1
docker pull yangshengzhou/leafboss:https-nginx-v1
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
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=leaf_boss \
  -p 3306:3306 \
  mysql:8.0

# Wait 30 seconds before initializing
docker exec -i leafboss-mysql mysql -uroot -p123456 < data.sql
```

**4. Start backend**

```bash
docker run -d \
  --name leafboss-backend \
  --network leafboss-network \
  --restart always \
  -p 8081:8080 \
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

#### Manual Docker Deployment (Aliyun ACR, recommended for China users)

> Image source: Aliyun Container Registry (ACR), no timeout in China. Public repo, **no docker login required**.

**1. Pull images**

```bash
docker pull crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/backend:backend-v1
docker pull crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/frontend:frontend-v1

# Optional: tag with shorter names
docker tag crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/backend:backend-v1 leafboss:backend-v1
docker tag crpi-kczczesrmgxok6ke.cn-hangzhou.personal.cr.aliyuncs.com/leafboss/frontend:frontend-v1 leafboss:frontend-v1

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
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=leaf_boss \
  -p 3306:3306 \
  mysql:8.0

# Wait 30 seconds before initializing
docker exec -i leafboss-mysql mysql -uroot -p123456 < data.sql
```

**4. Start backend**

```bash
docker run -d \
  --name leafboss-backend \
  --network leafboss-network \
  --restart always \
  -p 8081:8080 \
  -e SMTP_PASSWORD=your_auth_code \
  leafboss:backend-v1
```

**5. Start frontend**

```bash
docker run -d \
  --name leafboss-frontend \
  --network leafboss-network \
  --restart always \
  leafboss:frontend-v1
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

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| SPRING_DATASOURCE_URL | Database URL | jdbc:mysql://localhost:3306/leaf_boss |
| SPRING_DATASOURCE_USERNAME | Database username | root |
| SPRING_DATASOURCE_PASSWORD | Database password | 123456 |
| APP_JWT_SECRET | JWT secret (Base64) | bGVhZi1ib3NzLXNlY3JldC1rZXktZm9yLWp3dC10b2tlbi1nZW5lcmF0aW9uLXBsZWFzZS1jaGFuZ2U= |
| APP_JWT_EXPIRATION | JWT expiration (ms) | 7200000 |
| APP_JWT_RENEW_THRESHOLD | JWT renewal threshold (ms) | 1800000 |
| APP_JWT_MAX_AGE | JWT max age (ms) | 604800000 |
| SMTP_PASSWORD | SMTP authorization code (QQ Mail) | - |
| SERVER_PORT | Server port | 8081 |

### Troubleshooting

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
