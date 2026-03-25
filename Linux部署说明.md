# Linux 部署说明（宝塔 + Nginx + Spring Boot + Vue）

本文档适用于当前“校园任务接单平台”项目部署到 Linux 服务器的场景。

## 一、部署结构建议

建议采用以下目录结构：

- 前端静态文件：`/www/wwwroot/dist`
- 后端 Jar：`/www/wwwroot/campus-task-backend-1.0.0.jar`
- 后端运行日志：`/www/wwwroot/nohup.out`
- 上传目录：与后端运行目录下的 `uploads/` 保持一致

项目访问路径规划：

- `/` -> 前端 Vue 页面
- `/api/` -> Spring Boot 后端接口
- `/ws/` -> WebSocket
- `/uploads/` -> 后端上传资源

---

## 二、前端生产环境配置

已新增文件：`Front/.env.production`

内容如下：

```env
VITE_API_BASE_URL=/api
VITE_WS_HOST=8.138.149.54
```

说明：

- `VITE_API_BASE_URL=/api` 表示前端通过 Nginx 转发到后端
- `VITE_WS_HOST=8.138.149.54` 用于生产环境 WebSocket 连接
- 当前 `Rally.vue` 已支持未配置时自动回退到 `location.host`

前端重新打包命令：

```bash
cd /www/wwwroot/Front
npm install
npm run build
```

打包完成后，将 `dist/` 部署到：

```bash
/www/wwwroot/dist
```

---

## 三、后端启动方式

后端配置默认端口为 `8080`，可直接使用以下命令启动：

```bash
nohup java -jar /www/wwwroot/campus-task-backend-1.0.0.jar > /www/wwwroot/nohup.out 2>&1 &
```

常用检查命令：

```bash
ps -ef | grep campus-task-backend
ss -lntp | grep 8080
tail -f /www/wwwroot/nohup.out
```

如需停止进程：

```bash
ps -ef | grep campus-task-backend
kill -9 进程ID
```

---

## 四、宝塔 / Nginx 可部署配置

你当前服务器主配置已经是宝塔标准结构，核心思路是：

- **主配置文件 `nginx.conf` 保持宝塔结构不动**
- **业务项目只新增站点配置文件**
- 通过 `include /www/server/panel/vhost/nginx/*.conf;` 自动加载

### 4.1 可部署的主配置文件参考

如果你需要把当前主配置整理成可直接部署版本，可使用下面这一版。

```nginx
user  www www;
worker_processes auto;
error_log  /www/wwwlogs/nginx_error.log crit;
pid        /www/server/nginx/logs/nginx.pid;
worker_rlimit_nofile 51200;

stream {
    log_format tcp_format '$time_local|$remote_addr|$protocol|$status|$bytes_sent|$bytes_received|$session_time|$upstream_addr|$upstream_bytes_sent|$upstream_bytes_received|$upstream_connect_time';

    access_log /www/wwwlogs/tcp-access.log tcp_format;
    error_log  /www/wwwlogs/tcp-error.log;
    include /www/server/panel/vhost/nginx/tcp/*.conf;
}

events {
    use epoll;
    worker_connections 51200;
    multi_accept on;
}

http {
    include       mime.types;
    include       proxy.conf;
    lua_package_path "/www/server/nginx/lib/lua/?.lua;;";

    default_type  application/octet-stream;

    server_names_hash_bucket_size 512;
    client_header_buffer_size 32k;
    large_client_header_buffers 4 32k;
    client_max_body_size 50m;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 60;

    fastcgi_connect_timeout 300;
    fastcgi_send_timeout 300;
    fastcgi_read_timeout 300;
    fastcgi_buffer_size 64k;
    fastcgi_buffers 4 64k;
    fastcgi_busy_buffers_size 128k;
    fastcgi_temp_file_write_size 256k;
    fastcgi_intercept_errors on;

    gzip on;
    gzip_min_length 1k;
    gzip_buffers 4 16k;
    gzip_http_version 1.1;
    gzip_comp_level 5;
    gzip_types text/plain application/javascript application/x-javascript text/javascript text/css application/xml application/json image/jpeg image/gif image/png font/ttf font/otf image/svg+xml application/xml+rss text/x-js;
    gzip_vary on;
    gzip_proxied expired no-cache no-store private auth;
    gzip_disable "MSIE [1-6]\.";

    limit_conn_zone $binary_remote_addr zone=perip:10m;
    limit_conn_zone $server_name zone=perserver:10m;

    server_tokens off;
    access_log off;

    server {
        listen 888;
        server_name phpmyadmin;
        index index.html index.htm index.php;
        root /www/server/phpmyadmin;

        include enable-php.conf;

        location ~ .*\.(gif|jpg|jpeg|png|bmp|swf)$ {
            expires 30d;
        }

        location ~ .*\.(js|css)?$ {
            expires 12h;
        }

        location ~ /\. {
            deny all;
        }

        access_log /www/wwwlogs/access.log;
    }

    include /www/server/panel/vhost/nginx/*.conf;
}
```

### 4.2 项目站点配置文件

当前主配置文件 `nginx.conf` 已包含：

```nginx
include /www/server/panel/vhost/nginx/*.conf;
```

因此**无需把项目配置直接塞进主配置文件**，只需要在宝塔站点配置目录中新增一个站点配置文件即可，例如：

```nginx
/www/server/panel/vhost/nginx/8.138.149.54.conf
```

推荐项目站点配置如下：

```nginx
server {
    listen 80;
    server_name 8.138.149.54;

    root /www/wwwroot/dist;
    index index.html index.htm;

    # 前端单页应用
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 300s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }

    # WebSocket
    location /ws/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 600s;
        proxy_send_timeout 600s;
    }

    # 上传文件访问
    location /uploads/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 接口文档（可选）
    location /doc.html {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }

    location /v3/api-docs {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }

    location /swagger-ui/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }

    # 常见静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf)$ {
        expires 7d;
        access_log off;
    }

    access_log /www/wwwlogs/campus-task-access.log;
    error_log  /www/wwwlogs/campus-task-error.log;
}
```

### 4.3 为什么 `proxy_pass` 不再写 `/api/`

这里采用：

```nginx
proxy_pass http://127.0.0.1:8080;
```

而不是：

```nginx
proxy_pass http://127.0.0.1:8080/api/;
```

原因是当前 `location /api/` 已经带上了完整前缀，请求会原样转发为：

- 浏览器请求：`/api/auth/login`
- Nginx 转发到：`http://127.0.0.1:8080/api/auth/login`

这样更稳，不容易因为尾部斜杠导致路径拼接错误。

---

## 五、Nginx 校验与重载命令

保存配置后执行：

```bash
nginx -t
nginx -s reload
```

如果你是宝塔环境，也可以直接在面板中“重载配置”。

推荐同时检查：

```bash
ss -lntp | grep 80
ss -lntp | grep 8080
```

---

## 六、HTTPS 部署（可选）

如果后续绑定域名并启用 SSL，可改为如下结构：

```nginx
server {
    listen 443 ssl http2;
    server_name 你的域名;

    ssl_certificate     /www/server/panel/vhost/cert/你的域名/fullchain.pem;
    ssl_certificate_key /www/server/panel/vhost/cert/你的域名/privkey.pem;

    root /www/wwwroot/dist;
    index index.html index.htm;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 600s;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8080;
    }
}

server {
    listen 80;
    server_name 你的域名;
    return 301 https://$host$request_uri;
}
```

---

## 七、部署顺序建议

推荐按以下顺序执行：

1. 修改前端生产环境变量
2. 执行前端打包：`npm run build`
3. 上传前端 `dist/` 到服务器
4. 上传后端 Jar 到服务器
5. 启动 Spring Boot
6. 新增 Nginx 站点配置
7. 执行 `nginx -t && nginx -s reload`
8. 浏览器访问首页，检查：
   - 页面是否正常打开
   - 登录是否正常
   - `/api` 接口是否正常
   - WebSocket 是否正常连接
   - 上传图片是否能访问

---

## 八、补充说明

### 1. 关于前端 WebSocket
当前前端已经调整为：

- 优先读取 `VITE_WS_HOST`
- 未配置时自动回退到 `location.host`

因此后续如果更换域名，只需调整 `.env.production` 或直接依赖当前域名访问即可。

### 2. 关于 OSS 密钥
当前后端 `application.yml` 中直接写有 OSS 密钥，生产环境不建议继续明文保存在配置文件中。
建议：

- 尽快更换已暴露的 AccessKey
- 改为环境变量或服务器私有配置注入

### 3. 关于数据库和 Redis
当前默认连接配置为：

- MySQL：`localhost:3306/campus_task`
- Redis：`localhost:6379`

部署前请确认数据库、Redis 服务均已启动，并已导入初始化 SQL。
