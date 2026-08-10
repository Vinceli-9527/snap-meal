# Snap Meal 外卖系统

Snap Meal 是一个用于 Java 软件开发实战课程的外卖业务示例项目。项目包含 Spring Boot 后端、React 管理端、React Web 用户点餐端和微信小程序用户端，并集成 H2/MySQL、Swagger/OpenAPI、ECharts、Excel 报表导出、Redis 可选 token 存储和 OSS 可选上传能力。

当前项目名称统一为 `snap-meal`。

## 功能概览

- 管理端：管理员登录、分类管理、菜品管理、订单管理、经营概览、ECharts 图表仪表盘、Excel 报表导出、经营问答（Text2SQL Agent）。
- Web 用户端：模拟手机号登录、模拟微信登录、分类浏览、菜品列表、购物车明细、数量调整、地址管理、下单和模拟支付。
- 微信小程序用户端：登录、分类菜品浏览、首页购物车弹层、购物车数量调整、地址管理、结算、模拟支付、订单列表。
- 后端能力：统一 REST API、token 鉴权、H2/MySQL 数据库、购物车数量调整、阿里云 OSS 可选上传、Redis 可选 token 存储与菜品列表缓存、Bucket4j 下单限流、Swagger/OpenAPI 文档。
- 实验材料：版本归档、实验报告、外部工具使用文档、Postman Collection 模板。

## v0.1.8 更新重点

v0.1.8 为后端补上「缓存 + 限流」两块高可用基础能力，并让可选的 Redis 从 token 存储扩展到业务数据缓存：

- **Redis 菜品列表缓存**：菜品列表查询（`/api/admin/dishes`、`/api/user/catalog/dishes`）接入 `CatalogCache`——启用 Redis 时优先读 Redis（TTL 5 分钟），未启用或 Redis 不可用时自动降级到本地内存缓存；新增/编辑/删除/上下架菜品等写操作会按缓存前缀失效（Redis SCAN + DEL，内存模式按前缀移除），保证缓存与数据库一致。
- **Bucket4j 令牌桶下单限流**：`POST /api/user/orders` 提交订单接口引入令牌桶限流，按用户隔离（未登录按 IP），每分钟允许次数可用 `ORDERS_PER_MINUTE` 配置（默认 10），超限返回 HTTP 429；闲置桶自动回收，避免内存无限增长。
- **依赖说明**：Bucket4j 使用 `com.github.vladimir-bukhtoyarov:bucket4j-core:7.0.0`（Java 8 兼容版本；`com.bucket4j` 新坐标的 7.6.1 是 Java 11 字节码，与项目 JDK 8 不符）。
- 新增 8 个测试：CatalogCache 缓存命中/前缀失效/Redis 读写单测（4）、RateLimitInterceptor 限流单测（3）、下单限流集成测试（1，同一用户第 3 次提交订单返回 429），测试总数从 57 增加到 65。

## v0.1.7 更新重点

v0.1.7 修复了 DeepSeek function-calling 协议 bug，并把模型改为可运行时选择：

- **修复 tool_calls 协议 bug**：此前对话偶发报错 `400 Bad Request: Messages with role 'tool' must be a response to a preceding message with 'tool_calls'`。原因是多轮自纠错时只回传了 tool 结果，缺少前导的 `assistant(tool_calls)` 消息。现已按 OpenAI 兼容协议补全消息顺序（assistant(tool_calls) → tool），并在 V4 思考模式下把 `reasoning_content` 原样带回，避免再次 400。
- **默认模型切换为 `deepseek-v4-flash`**：官方已停用 `deepseek-chat`（2026-07-24 下线），现默认使用 `deepseek-v4-flash`，可用 `deepseek-v4-pro`。
- **新增可选模型功能**：「经营问答」面板顶部改为「Agent 设置」卡片，内置模型下拉框，支持在 `deepseek-v4-flash` / `deepseek-v4-pro` 间切换，保存后写入 `.env`（`DEEPSEEK_MODEL=`）并即时生效、重启仍保留。
- 新增 2 个鉴权接口：`GET /api/admin/agent/model`（当前模型 + 可选列表）、`POST /api/admin/agent/model`（切换模型，非法值返回 400）。
- **回答按 Markdown 渲染**：经营问答的回答不再以纯文本展示，而是用 `marked` 渲染成真正排版（表格、加粗、列表、标题等），并过 `DOMPurify` 消毒（禁用 `img`/`iframe`/`form` 等标签与行内样式），大模型输出不会被注入脚本。
- 新增 12 个测试：AgentModelStore 运行时模型存储单测（5）、自纠错循环协议断言（assistant 消息须排在 tool 消息之前 / reasoning_content 保留，2）、模型接口离线测试（5），测试总数从 45 增加到 57。

## v0.1.6 更新重点

v0.1.6 让「经营问答」配置 API Key 不再依赖命令行/环境变量，直接在管理端页面完成：

- 「经营问答」面板新增 **API Key 设置**卡片：粘贴 DeepSeek API Key → 保存即写入项目根目录 `.env` 并**即时生效，无需重启**；支持「测试连接」按钮实时校验 Key 是否有效（区分认证失败/余额不足/限流/网络问题）。
- 未配置 Key 时卡片自动展开并给出明确提示；配置后显示脱敏后的 Key（`sk-****后四位`）、模型名与保存位置。
- 新增 3 个鉴权接口：`GET /api/admin/agent/key`、`POST /api/admin/agent/key`、`POST /api/admin/agent/key/test`（均需登录）。
- `.env` 已加入 `.gitignore`，Key 不会进入版本库。
- 新增 20 个测试：KeyStore 持久化/脱敏单测（7）、DeepSeekClient 错误映射单测（8，Mockito 脚本化）、Key 接口离线测试（5），测试总数从 25 增加到 45。

## v0.1.5 更新重点

v0.1.5 为管理端新增「经营问答」能力：一个 Text2SQL Agent，把自然语言经营问题转成只读 SQL 查询并给出中文经营解释，运营者无需掌握 SQL 即可洞察订单数据。

- 管理端新增「经营问答」面板：输入「最近7天营收多少？」这类问题，Agent 自动生成 SQL、只读执行、展示结果表，并给出中文经营解读。
- 多轮 function-calling 自纠错循环：SQL 校验或执行失败时，错误会回传给大模型自动修正重试（最多 3 次），成功后才返回最终答案。
- 只读 SQL 安全层：仅允许单条 SELECT，拒绝多语句拼接、注释绕过、DDL/DML 关键字与危险函数；查询在只读连接上执行，带 10 秒超时与 200 行结果上限。
- 示例订单数据：内置 16 笔覆盖全部 6 种订单状态的演示订单，下单时间相对当前时刻偏移，保证任意时刻启动都能回答「近 7 天营收」「待接单数量」等问题。
- DeepSeek API Key 通过环境变量注入，不写入仓库；未配置 Key 时面板给出明确提示，不影响项目其他功能。
- 新增 16 个测试：SQL 校验器单测（10）、自纠错循环端到端单测（3，真实 H2 + 种子数据，LLM 用 Mockito 脚本化）、离线接口测试（3），测试总数从 9 增加到 25。

## v0.1.4 更新重点

v0.1.4 打通订单履约链路的"最后一公里"：后端履约能力（接单、拒单、取消、派送、完成）此前已经就绪，本版本将其完整接通到管理端和 Web 用户端界面，并补齐自动化测试。

- 管理端订单管理新增状态筛选（全部/待付款/待接单/已接单/派送中/已完成/已取消），每个筛选项实时显示对应状态的订单数量角标。
- 管理端订单管理新增订单号模糊搜索。
- 管理端订单操作补全：待接单订单可接单或拒单，待接单和已接单订单可取消，拒单和取消需填写原因并持久化。
- 管理端订单支持行内详情展开：收货信息、备注、下单时间、预计送达时间、实际送达时间、拒单/取消原因和菜品明细。
- Web 用户端新增"我的订单"视图：页面头部切换，订单卡片展示状态、订单号、金额、预计送达时间和拒单/取消原因。
- Web 用户端支持取消待付款、待接单订单；支付成功后自动跳转到"我的订单"。
- 管理端与用户端订单状态文案统一口径。
- 新增 3 个履约链路集成测试：完整生命周期、拒单原因持久化、非法状态迁移拦截，测试用例从 6 个增加到 9 个。

## v0.1.3 更新重点

v0.1.3 主要完善微信小程序端体验，并补强管理端菜品维护能力：

- 微信小程序用户端视觉统一到 Web 点餐端风格：深绿头部、米白背景、硬边界菜品卡片、青柠色圆形加购按钮和深色底部购物车栏。
- 小程序首页新增底部购物车弹层：点击底部购物车区域在当前页面向上弹出已选菜品面板，不再跳转独立购物车页。
- 小程序购物车弹层支持完整购物车逻辑：查看菜品、单价、数量、小计、合计，支持 `+` / `-` 调整数量、减到 0 自动移除、清空购物车和直接结算。
- 小程序移除冗余独立购物车页面与底部 TabBar，订单入口保留在首页头部，点餐流程更接近 Web 用户端。
- 小程序加入可执行的轻量动态效果：菜品卡片上浮淡入、加购按钮短暂 `✓` 反馈、购物车面板向上弹出。
- 管理端菜品管理新增页面内表单：新增/编辑菜品时可调整菜品名称、分类、价格和起售状态。
- 管理端分类管理简化为商家实际需要的名称和启用状态，分类类型与排序字段改为后端兼容处理，不再暴露给商家操作。

## v0.1.2 更新重点

v0.1.2 主要强化 Web 用户点餐端的交互体验：

- 分类导航改造为 Dock 风格交互：支持横向滑动、选中态滑块、鼠标靠近放大和键盘聚焦反馈。
- 菜品列表接入 AnimatedList：滚动时菜品卡片以弹簧式滑入，带顶部/底部渐隐提示和键盘方向键滚动定位。
- 加购物车反馈增强：点击 `+` 成功后按钮短暂变为 `✓`，菜品图片轻跳，并显示飞向购物车栏的小圆点；失败时按钮变红并抖动。
- 底部购物车新增展开面板：点击购物车信息区域可展开当前已选菜品，支持查看菜名、单价、数量、小计和合计。
- 购物车面板支持真实增减逻辑：`+` 调用加购接口，`-` 调用数量更新接口；数量减到 0 时自动移除该项。
- 用户端 React 源码整理为 UTF-8，修复历史中文乱码对维护和构建的影响。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 | Spring Boot 2.7.18、Spring JDBC、H2、MySQL、Apache POI |
| Agent | DeepSeek Chat Completions（function-calling）、RestTemplate、只读 SQL 安全校验 |
| 前端 | React 19、Vite、多页面构建、motion |
| 管理端图表 | ECharts 5.5.0 |
| API 文档 | springdoc-openapi-ui 1.7.0 |
| 缓存 | Redis，可选启用（token 存储 + 菜品列表缓存） |
| 接口限流 | Bucket4j 令牌桶（下单接口） |
| 文件存储 | 本地 uploads 目录，或阿里云 OSS |
| 小程序 | 微信小程序原生 WXML/WXSS/JS |
| 构建工具 | Maven、npm |

## 目录结构

```text
sky-lab/
  src/main/java/com/snapmeal/       Spring Boot 后端源码
  src/main/resources/               后端配置、SQL、静态构建产物
  frontend/                         React + Vite 前端源码
  miniprogram/                      微信小程序用户端
  docs/                             实验和外部工具文档
  output/                           实验报告、Postman 模板、截图材料
  releases/v0.1.0/                 v0.1.0 源码归档
  releases/v0.1.2/                 v0.1.2 源码归档
  releases/v0.1.3/                 v0.1.3 源码归档
  releases/v0.1.7/                 v0.1.7 源码归档
  releases/v0.1.8/                 v0.1.8 源码归档
  package.json                      根目录前端构建代理脚本
  pom.xml                           Maven 后端配置
  maven-settings.xml                Maven 镜像配置
```

## 环境要求

| 环境 | 要求 |
| --- | --- |
| JDK | JDK 8 |
| Maven | 3.6 或更高 |
| Node.js/npm | 修改 React 前端时需要 |
| 浏览器 | Edge、Chrome 或 Firefox |
| 微信开发者工具 | 运行 `miniprogram/` 时需要 |
| MySQL | 可选，默认使用 H2 |
| Redis | 可选，默认使用内存 token |

检查 Java 和 Maven：

```powershell
java -version
javac -version
mvn -version
```

## 快速启动

进入项目根目录：

```powershell
cd "C:\Users\Vince\Desktop\java\sky-lab"
```

运行后端测试：

```powershell
mvn -s maven-settings.xml clean test
```

启动项目：

```powershell
mvn -s maven-settings.xml spring-boot:run
```

启动成功后访问：

| 页面 | 地址 |
| --- | --- |
| 首页 | http://localhost:8080/ |
| 管理端 | http://localhost:8080/admin.html |
| Web 用户端 | http://localhost:8080/user.html |
| Swagger | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api-docs |

默认管理端账号：

```text
username: admin
password: 123456
```

## 前端开发

安装依赖：

```powershell
npm install --prefix frontend
```

启动 Vite 开发服务器：

```powershell
npm run dev --prefix frontend
```

构建 React 前端并输出到 Spring Boot 静态目录：

```powershell
npm run build --prefix frontend
```

也可以使用根目录代理脚本：

```powershell
npm run build
```

构建输出位置：

```text
src/main/resources/static/
```

说明：Spring Boot 运行时直接读取 `src/main/resources/static` 下的构建产物，因此修改 React 源码后需要重新执行 `npm run build`。

## Web 用户端交互说明

用户端地址：

```text
http://localhost:8080/user.html
```

主要流程：

```text
登录 -> 浏览分类 -> 浏览菜品 -> 加入购物车 -> 调整购物车 -> 添加地址 -> 结算 -> 模拟支付
```

交互细节：

- 分类导航支持横向滚动和 Dock 式放大。
- 菜品列表滚动时有弹簧式滑入效果。
- 点击菜品 `+` 后会播放成功反馈并刷新购物车。
- 点击底部购物车栏左侧信息区域会展开购物车面板。
- 购物车面板中可通过 `+` / `-` 调整数量，减到 0 时自动移除。
- `去结算` 按钮保留直接结算行为。

## 微信小程序

小程序项目目录：

```text
miniprogram/
```

使用方式：

1. 启动后端，确保 `http://localhost:8080` 可访问。
2. 打开微信开发者工具。
3. 选择导入项目，目录选择 `sky-lab/miniprogram`。
4. 如果本地调试 HTTP 接口，需要在微信开发者工具中关闭合法域名校验。

小程序全局后端地址位于：

```text
miniprogram/app.js
```

默认值：

```javascript
baseUrl: 'http://localhost:8080'
```

### 小程序真机调试后端地址

如果使用微信开发者工具模拟器，默认的 `localhost` 通常可以访问电脑本机后端：

```javascript
baseUrl: 'http://localhost:8080'
```

如果使用真机调试，不能继续使用 `localhost`。手机上的 `localhost` 指向手机本机，不是电脑。需要按下面步骤临时改为电脑局域网 IP：

1. 确认手机和电脑连接到同一个 Wi-Fi。
2. 在电脑 PowerShell 执行：

```powershell
ipconfig
```

3. 找到当前网络适配器的 IPv4 地址，例如：

```text
IPv4 地址 . . . . . . . . . . . . : 192.168.1.23
```

4. 打开 `miniprogram/app.js`，将：

```javascript
baseUrl: 'http://localhost:8080'
```

临时改为：

```javascript
baseUrl: 'http://192.168.1.23:8080'
```

5. 确保后端已启动：

```powershell
mvn -s maven-settings.xml spring-boot:run
```

6. 可先在手机浏览器访问下面地址验证网络是否通：

```text
http://192.168.1.23:8080/api/user/catalog/categories?type=1
```

7. 真机调试结束后，发布或提交代码前建议把 `baseUrl` 改回：

```javascript
baseUrl: 'http://localhost:8080'
```

同时还需要确保：

- 手机和电脑在同一局域网。
- 后端服务已启动并监听对应端口。
- Windows 防火墙允许手机访问该端口。
- 微信开发者工具已开启“不校验合法域名、web-view、TLS 版本以及 HTTPS 证书”。

小程序端主要流程：

```text
登录 -> 浏览分类 -> 浏览菜品 -> 加入购物车 -> 展开购物车弹层 -> 调整数量 -> 结算 -> 选择或新增地址 -> 模拟支付 -> 查看订单
```

小程序交互说明：

- 首页样式与 Web 用户点餐端保持一致，使用同一套深绿、米白和青柠色视觉语言。
- 点击菜品 `+` 后会短暂显示 `✓` 反馈，并刷新底部购物车汇总。
- 点击底部购物车栏左侧信息区域，会在当前页面向上弹出购物车明细面板。
- 购物车弹层中可通过 `+` / `-` 调整数量，数量减到 0 时自动移除。
- 购物车弹层支持清空购物车，点击遮罩或 `×` 可收起。
- `去结算` 按钮进入结算页，支付成功后进入订单页。

说明：小程序端已移除独立购物车页面，购物车功能集中在首页弹层中。

## 后端配置

主配置文件：

```text
src/main/resources/application.yml
```

默认模式可零配置启动：

- 数据库：H2 文件数据库
- 文件上传：本地 `uploads/`
- Redis：内存模式
- 微信登录：模拟模式
- 支付：模拟模式

### 常用环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 服务端口 |
| `TOKEN_SECRET` | 教学默认值 | token 签名密钥 |
| `REDIS_MODE` | `memory` | `memory` 或 `redis` |
| `REDIS_HOST` | `localhost` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `ORDERS_PER_MINUTE` | `10` | 下单接口每分钟允许的请求数（Bucket4j 令牌桶容量） |
| `OSS_MODE` | `local` | `local` 或 `oss` |
| `OSS_ENDPOINT` | 空 | 阿里云 OSS endpoint |
| `OSS_ACCESS_KEY_ID` | 空 | 阿里云 AccessKey ID |
| `OSS_ACCESS_KEY_SECRET` | 空 | 阿里云 AccessKey Secret |
| `OSS_BUCKET_NAME` | 空 | OSS bucket |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek API Key（经营问答必填，也可在管理端「经营问答 → Agent 设置」直接填写） |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | DeepSeek 兼容接口地址 |
| `DEEPSEEK_MODEL` | `deepseek-v4-flash` | 经营问答使用的模型（可选 `deepseek-v4-flash` / `deepseek-v4-pro`，也可在管理端下拉框切换） |
| `AGENT_KEY_FILE` | `.env` | API Key 与模型持久化文件路径（一般无需修改） |

### 切换 Redis

默认不依赖 Redis。需要启用 Redis 时：

```powershell
$env:REDIS_MODE="redis"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
mvn -s maven-settings.xml spring-boot:run
```

Redis 不可用时，token 会自动降级到内存和数据库兼容逻辑，不阻止项目启动；菜品列表缓存由同一 `redis-mode` 开关控制，Redis 不可用时同样自动降级到本地内存缓存（5 分钟 TTL）兜底。

### 切换阿里云 OSS

默认上传到本地 `uploads/`。需要启用 OSS 时：

```powershell
$env:OSS_MODE="oss"
$env:OSS_ENDPOINT="https://oss-cn-shanghai.aliyuncs.com"
$env:OSS_ACCESS_KEY_ID="你的AccessKeyId"
$env:OSS_ACCESS_KEY_SECRET="你的AccessKeySecret"
$env:OSS_BUCKET_NAME="你的BucketName"
mvn -s maven-settings.xml spring-boot:run
```

不要把真实密钥写入仓库文件。

## MySQL 可选配置

默认使用 H2，不需要安装 MySQL。需要切换到 MySQL 时：

1. 创建数据库：

```sql
CREATE DATABASE snap_meal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 设置环境变量：

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="snap_meal"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="你的MySQL密码"
mvn -s maven-settings.xml spring-boot:run
```

MySQL 配置文件：

```text
src/main/resources/application-mysql.yml
```

## API 认证规则

统一返回格式：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

请求头：

| 端 | Header |
| --- | --- |
| 管理端 | `token` |
| 用户端 | `authentication` |

常用接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/admin/auth/login` | 管理员登录 |
| `GET` | `/api/admin/reports/overview` | 经营概览 |
| `GET` | `/api/admin/reports/turnover` | 营业额统计 |
| `GET` | `/api/admin/reports/sales-top10` | 销量排行 |
| `GET` | `/api/admin/reports/export` | 导出 Excel |
| `POST` | `/api/admin/agent/chat` | 经营问答：自然语言 → SQL → 只读执行 → 中文解释 |
| `GET` | `/api/admin/agent/status` | 经营问答配置状态（是否配置 Key、模型名、脱敏 Key、保存位置） |
| `GET` | `/api/admin/agent/key` | 获取当前 API Key 配置（脱敏） |
| `POST` | `/api/admin/agent/key` | 保存 API Key（写入 `.env` 并即时生效） |
| `POST` | `/api/admin/agent/key/test` | 测试 API Key 连通性（不保存） |
| `GET` | `/api/admin/agent/model` | 获取当前模型与可选模型列表 |
| `POST` | `/api/admin/agent/model` | 切换模型（写入 `.env` 并即时生效） |
| `POST` | `/api/user/auth/login` | 用户登录 |
| `GET` | `/api/user/catalog/categories` | 分类列表 |
| `GET` | `/api/user/catalog/dishes?categoryId=1` | 菜品列表 |
| `POST` | `/api/user/cart/items` | 添加购物车 |
| `GET` | `/api/user/cart` | 查看购物车 |
| `PATCH` | `/api/user/cart/items/{id}?number=2` | 设置购物车项数量，数量小于等于 0 时删除 |
| `DELETE` | `/api/user/cart` | 清空购物车 |
| `POST` | `/api/user/addresses` | 新增地址 |
| `POST` | `/api/user/orders` | 提交订单 |
| `POST` | `/api/user/orders/{id}/pay` | 模拟支付 |

## 经营概览图表

管理端“经营概览”页面使用 ECharts 展示：

- 4 个数字卡片：营业额、有效订单、注册用户、待接单。
- 近 7 日营业额折线图。
- 菜品销量 Top10 横向柱状图。

相关文件：

```text
frontend/public/echarts-dashboard.js
src/main/resources/static/echarts-dashboard.js
frontend/src/pages/admin.jsx
```

图表数据来自真实后端 API：

```text
/api/admin/reports/overview
/api/admin/reports/turnover
/api/admin/reports/sales-top10
```

## 经营问答（Text2SQL Agent）

管理端「经营问答」面板把自然语言经营问题转成可执行 SQL：

```text
自然语言问题 -> DeepSeek 生成 SQL -> 只读安全校验 -> 只读连接执行 -> 结果回传 -> 中文经营解释
```

### 使用方式

1. 启动项目，登录管理端，点击左侧「经营问答」。
2. 首次使用时，若未配置 Key，面板顶部「Agent 设置」卡片会自动展开：粘贴 DeepSeek API Key → 点击「测试连接」确认有效 → 点击「保存」。保存后立即生效，无需重启；Key 写入项目根目录 `.env`（已加入 `.gitignore`）。
   - 也可以在启动前设置环境变量 `DEEPSEEK_API_KEY`（见「常用环境变量」表），两种方式等效。
   - 同一张卡片内置**模型下拉框**，可在 `deepseek-v4-flash` / `deepseek-v4-pro` 间切换，保存后即时生效，重启仍保留。
3. 输入问题并回车，面板会依次展示：中文解释、执行 SQL（含纠错次数）、查询结果表。

可尝试的问题：

```text
- 最近7天营收多少？
- 哪个菜卖得最好？
- 有多少待接单订单？
- 近30天已完成订单数是多少？
```

### 多轮自纠错循环

- Agent 采用 function-calling，大模型通过 `run_query` 工具执行查询。
- SQL 校验失败或执行失败时，错误信息会回传大模型自动修正，最多重试 3 次。
- 连续失败会终止并提示换个问法，不会无限循环。

### 只读 SQL 安全层

`SqlSafetyValidator` 是查询前的唯一入口：

- 仅允许单条 `SELECT`，拒绝多语句（分号拼接）。
- 拒绝注释（`--`、`/* */`、`#`）绕过。
- 拒绝 DDL/DML 关键字（INSERT/UPDATE/DELETE/DROP/ALTER/CREATE 等，按单词边界匹配，不影响 `updated_at` 这类列名）。
- 拒绝 `into outfile`、`load_file`、`information_schema` 等危险内容。
- 查询在只读连接上执行，带 10 秒超时与 200 行结果上限，返回给大模型的结果同样截断。

### 示例订单数据

`data.sql` 内置 16 笔演示订单（订单号 `SM...`），覆盖全部 6 种订单状态，下单时间相对当前时刻偏移，因此任何时刻启动都能回答「近 7 天营收」「待接单数量」等问题。

## 实验材料

| 内容 | 位置 |
| --- | --- |
| 新版实验报告 | `output/snap-meal-report-v2.docx` |
| 外部工具实验文档 | `docs/external-tools-experiments.md` |
| Postman Collection | `output/SnapMeal.postman_collection.json` |
| 截图说明 | `output/experiment-screenshots/README.md` |
| 版本归档 | `releases/` |

外部工具实验文档覆盖：

- 实验 7：MySQL Workbench 建表
- 实验 8：Apifox 接口文档管理
- 实验 9：Swagger + Postman 接口测试
- 实验 10：Cpolar 内网穿透

## 版本归档

当前已保存版本：

```text
releases/v0.1.0/snap-meal-v0.1.0-source-20260623.zip
releases/v0.1.0/snap-meal-v0.1.0-source-20260623.sha256.txt
releases/v0.1.0/RELEASE.md

releases/v0.1.2/snap-meal-v0.1.2-source-20260625.zip
releases/v0.1.2/snap-meal-v0.1.2-source-20260625.sha256.txt
releases/v0.1.2/RELEASE.md

releases/v0.1.3/snap-meal-v0.1.3-source-20260625.zip
releases/v0.1.3/snap-meal-v0.1.3-source-20260625.sha256.txt
releases/v0.1.3/RELEASE.md

releases/v0.1.4/snap-meal-v0.1.4-source-20260802.zip
releases/v0.1.4/snap-meal-v0.1.4-source-20260802.sha256.txt
releases/v0.1.4/RELEASE.md

releases/v0.1.7/snap-meal-v0.1.7-source-20260810.zip
releases/v0.1.7/snap-meal-v0.1.7-source-20260810.sha256.txt
releases/v0.1.7/RELEASE.md

releases/v0.1.8/snap-meal-v0.1.8-source-20260810.zip
releases/v0.1.8/snap-meal-v0.1.8-source-20260810.sha256.txt
releases/v0.1.8/RELEASE.md
```

归档为源码快照，不包含本地依赖、运行期数据库、构建缓存和日志。

## 常见问题

### npm run build 提示 Missing script: build

确认命令在项目根目录执行，并且根目录存在 `package.json`。当前根目录脚本会代理到 `frontend`：

```powershell
npm run build
```

### 8080 端口被占用

临时改用 8090：

```powershell
$env:SERVER_PORT="8090"
mvn -s maven-settings.xml spring-boot:run
```

访问：

```text
http://localhost:8090/
```

### Swagger 打不开

先确认后端启动成功，再访问：

```text
http://localhost:8080/swagger-ui.html
```

如果端口改为 8090，则对应访问：

```text
http://localhost:8090/swagger-ui.html
```

### 小程序请求失败

检查：

1. 后端是否运行在 `localhost:8080` 或你配置的局域网 IP 地址。
2. 微信开发者工具是否关闭合法域名校验。
3. `miniprogram/app.js` 中的 `baseUrl` 是否正确。
4. 真机调试时，`baseUrl` 是否已从 `localhost` 改为电脑局域网 IP。
5. 手机和电脑是否在同一局域网，Windows 防火墙是否允许访问后端端口。
6. 需要登录的接口是否已经获取并保存 token。

### 中文显示乱码

项目文件应使用 UTF-8。不要使用 ANSI 或 GBK 编码保存源文件。PowerShell 输出乱码通常不代表文件本身损坏，可用 VS Code 或 IntelliJ IDEA 以 UTF-8 打开确认。

## 验收检查

- [ ] `mvn -s maven-settings.xml clean test` 通过。
- [ ] `npm run build` 成功。
- [ ] `http://localhost:8080/` 能打开首页。
- [ ] 管理端可使用 `admin / 123456` 登录。
- [ ] 管理端“经营概览”显示 ECharts 图表。
- [ ] 管理端可导出运营报表。
- [ ] Web 用户端可登录、浏览分类和菜品。
- [ ] Web 用户端可加入购物车、展开购物车面板并调整数量。
- [ ] Web 用户端可完成下单和模拟支付；连续下单第 3 次被限流返回 429（把 `ORDERS_PER_MINUTE` 调低后验证）。
- [ ] 菜品列表首次访问走数据库，再次访问命中缓存（Redis 模式可 `redis-cli keys 'dish:*'` 查看缓存键；改/删菜品后缓存失效）。
- [ ] Web 用户端"我的订单"可查看订单状态、取消待付款/待接单订单。
- [ ] 管理端订单管理可按状态筛选、按订单号搜索、查看订单详情。
- [ ] 管理端可完成接单、拒单（填原因）、取消（填原因）、派送、完成操作。
- [ ] 设置 `DEEPSEEK_API_KEY` 后，管理端「经营问答」可用自然语言提问并返回中文解释与结果表。
- [ ] 未设置 `DEEPSEEK_API_KEY` 时，经营问答面板显示明确提示，其他功能不受影响。
- [ ] 在「经营问答 → Agent 设置」卡片填入 Key →「测试连接」校验 →「保存」后即时生效，重启服务后 Key 仍然有效（已持久化到 `.env`）。
- [ ] 用占位符或空值保存 API Key 会被拒绝；未登录时访问 Key 接口返回 401。
- [ ] 「Agent 设置」卡片模型下拉框可在 `deepseek-v4-flash` / `deepseek-v4-pro` 间切换，切换后即时生效，重启服务后模型仍然保留。
- [ ] 提交不支持的模型（如已停用的 `deepseek-chat`）会被拒绝（400）。
- [ ] 经营问答的回答按 Markdown 渲染：包含 `| 列 |` 的表格显示为真正的表格，加粗/列表/标题排版正常，不再出现原始 `|` 管道符。
- [ ] 微信小程序可完成登录、浏览分类和菜品。
- [ ] 微信小程序首页购物车弹层可展开、调整数量、清空购物车并进入结算。
- [ ] 微信小程序可完成添加地址、结算、支付、查看订单。
- [ ] `http://localhost:8080/swagger-ui.html` 可查看 API 文档。
- [ ] Postman Collection 可导入并运行核心下单流程。
