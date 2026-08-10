# snap-meal v0.1.8

发布日期：2026-08-10

本版本为后端补上「缓存 + 限流」两块高可用基础能力：Redis 菜品列表缓存（可选启用、自动降级），以及 Bucket4j 令牌桶下单限流（按用户隔离、超限返回 429）。

## 更新重点

### Redis 菜品列表缓存

- 菜品列表查询（`/api/admin/dishes`、`/api/user/catalog/dishes`）接入 `CatalogCache`：启用 Redis 时优先读 Redis（TTL 5 分钟），未启用或 Redis 不可用时自动降级到本地内存缓存，保证功能不受影响。
- 新增/编辑/删除/上下架菜品等写操作会按缓存前缀失效（Redis 模式 SCAN + DEL，内存模式按前缀移除），保证缓存与数据库一致。
- 由 `redis-mode`（`REDIS_MODE`）开关统一控制，与既有 token 存储共享同一套 Redis 连接。

### Bucket4j 令牌桶下单限流

- `POST /api/user/orders` 提交订单接口引入令牌桶限流，按用户隔离（未登录按 IP）。
- 每分钟允许次数可用 `ORDERS_PER_MINUTE` 配置（默认 10），超限返回 HTTP 429 与中文提示。
- 闲置桶自动回收，避免内存无限增长。

### 依赖说明

- Bucket4j 使用 `com.github.vladimir-bukhtoyarov:bucket4j-core:7.0.0`（Java 8 兼容版本）；`com.bucket4j` 新坐标的 7.6.1 是 Java 11 字节码，与项目 JDK 8 不符。

### 数据与测试

- 新增 8 个测试：CatalogCache 缓存命中/前缀失效/Redis 读写单测（4）、RateLimitInterceptor 限流单测（3）、下单限流集成测试（1，同一用户第 3 次提交订单返回 429），测试总数从 57 增加到 65。

## 验证情况

- `mvn -s maven-settings.xml clean test` 通过，65 个用例全部成功（含完整 Spring 上下文的 429 集成测试）。
- 无前端改动，无需重新构建。
- 冒烟验证：Redis 缓存读写与失效通过单测覆盖；限流集成测试在内存 H2 上下文中验证同一用户第 3 次下单返回 429。
- 未配置 Redis 时项目照常启动，菜品列表缓存自动降级到本地内存。

## 文件

- `snap-meal-v0.1.8-source-20260810.zip`：源码归档
- `snap-meal-v0.1.8-source-20260810.sha256.txt`：SHA-256 校验文件

## 排除的本地或生成内容

- `.git`
- `.env`（包含本机 DeepSeek API Key，不进入版本库与归档）
- `.m2`
- `.claude`
- `.tmp`
- `.tmp-docx-check`
- `.idea`
- `frontend/node_modules`
- `frontend/dist`
- `target`
- `dist`
- `releases`
- `output`
- `uploads`
- `data`（运行期 H2 数据库文件，如 `data/*.mv.db`）
- `*.log`
- `codex-prompts.txt`
- `miniprogram/project.private.config.json`

## 注意事项

- 限流频率可用环境变量 `ORDERS_PER_MINUTE` 调整（默认 10）；演示时若连续快速下单，第 N+1 次会返回 429。
- 启用 Redis 缓存需设置 `REDIS_MODE=redis`（及 `REDIS_HOST`/`REDIS_PORT`），未启用时菜品列表缓存使用本地内存兜底。
- 本归档是源码快照，不包含本地依赖、运行期数据库、构建缓存和日志。

## 用法示例

```text
启动：mvn -s maven-settings.xml spring-boot:run
访问：http://localhost:8080/user.html  →  点餐下单
限流演示：把 ORDERS_PER_MINUTE 调低后快速连续下单，观察第 N+1 次返回 HTTP 429
```
