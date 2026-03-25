# Nginx 部署配置说明（校园任务接单平台）

> 按你的要求：这里只提供文档与配置示例，不直接改线上 Nginx 文件。

---

## 1. 适用场景

- 前端打包目录：`/www/wwwroot/dist`
- 后端服务：`127.0.0.1:8080`（Spring Boot）
- 需要反向代理：
  - `/api/`（接口）
  - `/ws/`（WebSocket：聊天 + 组队）
  - `/uploads/`（上传文件访问）

---

## 2. 推荐 Nginx 配置（可直接复制）

```nginx
worker_processes  auto;

events {
    worker_connections  4096;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    sendfile        on;
    tcp_nopush      on;
    tcp_nodelay     on;
    keepalive_timeout 65;

    server_tokens off;

    upstream campus_backend {
        server 127.0.0.1:8080;
        keepalive 64;
    }

    server {
        listen       80;
        server_name  _;

        root   /www/wwwroot/dist;
        index  index.html;

        # 静态资源缓存
        location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff|woff2)$ {
            expires 7d;
            access_log off;
        }

        # Vue history 路由
        location / {
            try_files $uri $uri/ /index.html;
        }

        # API 代理
        location /api/ {
            proxy_pass http://campus_backend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            client_max_body_size 50m;
            proxy_connect_timeout 300s;
            proxy_send_timeout    300s;
            proxy_read_timeout    300s;
        }

        # WebSocket 代理
        location /ws/ {
            proxy_pass http://campus_backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_read_timeout 3600s;
        }

        # 上传文件透传
        location /uploads/ {
            proxy_pass http://campus_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        access_log  /www/wwwlogs/campus_access.log;
        error_log   /www/wwwlogs/campus_error.log;
    }
}
```

---

## 3. `resty.core` 报错处理（你当前报错）

报错示例：

- `failed to load the 'resty.core' module ... module 'resty.core' not found`

原因：

- 你当前是普通 Nginx 环境，不是 OpenResty；
- 但配置文件里启用了 Lua/OpenResty 指令（如 `lua_package_path` / `init_by_lua*` / `luawaf.conf`）。

处理方式：

1. 在 `nginx.conf` 及 include 的配置里，移除或注释所有 Lua/OpenResty 相关指令：
   - `lua_package_path ...;`
   - `lua_package_cpath ...;`
   - `include luawaf.conf;`
   - `init_by_lua*` / `*_by_lua*`
2. 保留标准 Nginx 反向代理配置即可。

排查命令：

```bash
grep -nE "resty.core|lua_|_by_lua|init_by_lua|luawaf" /www/server/nginx/conf/nginx.conf
grep -nE "resty.core|lua_|_by_lua|init_by_lua|luawaf" /www/server/nginx/conf/proxy.conf
grep -nE "resty.core|lua_|_by_lua|init_by_lua|luawaf" /www/server/panel/vhost/nginx/*.conf
```

---

## 4. 生效步骤

```bash
nginx -t
nginx -s reload
```

如果 `nginx -t` 失败，优先修复报错行再 reload。

---

## 5. 前端 WebSocket 地址注意事项

你前端当前有 `localhost:8080` fallback。上线环境建议：

- 配置环境变量：`VITE_WS_HOST=你的域名`
- 或改成同源拼接：`ws(s)://${location.host}/ws/...`

否则可能出现“前端能访问，WebSocket 连接失败”的问题。
