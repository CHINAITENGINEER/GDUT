# 阿里云 OSS 配置说明

## 配置信息

- **Endpoint**: oss-cn-beijing.aliyuncs.com（华北2北京）
- **Bucket**: java-gdut
- **访问域名**: https://java-gdut.oss-cn-beijing.aliyuncs.com/
- **AccessKeyId**: 请通过环境变量 `ALIYUN_OSS_ACCESS_KEY_ID` 配置
- **AccessKeySecret**: 请通过环境变量 `ALIYUN_OSS_ACCESS_KEY_SECRET` 配置

## 使用方式

### 切换存储方式

在 `application.yml` 中修改 `file.storage-type`：

```yaml
file:
  storage-type: oss    # 使用阿里云OSS
  # storage-type: local  # 使用本地存储
```

### 文件上传流程

1. **本地存储模式** (`storage-type: local`)
   - 文件保存到 `./uploads/yyyy/MM/` 目录
   - 返回相对路径：`/uploads/2026/03/abc123.jpg`
   - 通过 Spring Boot 静态资源映射访问

2. **OSS存储模式** (`storage-type: oss`)
   - 文件上传到阿里云OSS
   - 按日期分目录：`2026/03/14/abc123.jpg`
   - 返回完整URL：`https://java-gdut.oss-cn-beijing.aliyuncs.com/2026/03/14/abc123.jpg`

### 自动切换机制

- 使用 `@ConditionalOnProperty` 注解实现自动切换
- 修改配置后重启服务即可生效
- 无需修改业务代码，Controller 自动注入对应实现

## 文件限制

- 单文件大小：≤ 5MB
- 支持格式：jpg、png、gif、webp
- 批量上传：最多9张

## 目录结构

```
uploads/                    # 本地存储根目录
└── 2026/
    └── 03/
        ├── abc123.jpg
        └── def456.png

OSS Bucket 结构：
java-gdut/
└── 2026/
    └── 03/
        └── 14/
            ├── abc123.jpg
            └── def456.png
```

## 注意事项

1. **Bucket权限**：确保Bucket设置为公共读（public-read），否则前端无法访问图片
2. **跨域配置**：在OSS控制台配置CORS规则，允许前端域名访问
3. **费用控制**：OSS按存储量和流量计费，建议设置生命周期规则自动清理过期文件
4. **安全建议**：生产环境建议使用STS临时凭证，不要直接暴露AccessKey

## 测试接口

```bash
# 单张上传
curl -X POST http://localhost:8080/api/file/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@test.jpg"

# 批量上传
curl -X POST http://localhost:8080/api/file/upload/batch \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "files=@test1.jpg" \
  -F "files=@test2.jpg"
```
