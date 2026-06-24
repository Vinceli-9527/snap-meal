# Snap Meal 外卖系统

Snap Meal 是一个用于 Java 软件开发实战课程的外卖业务示例项目。项目包含 Spring Boot 后端、React 管理端、React Web 用户点餐端和微信小程序用户端，并集成 H2/MySQL、Swagger/OpenAPI、ECharts、Excel 报表导出、Redis 可选 token 存储和 OSS 可选上传能力。

当前项目名称统一为 `snap-meal`。

## 功能概览

- 管理端：管理员登录、分类管理、菜品管理、订单管理、经营概览、ECharts 图表仪表盘、Excel 报表导出。
- Web 用户端：模拟手机号登录、模拟微信登录、分类浏览、菜品列表、购物车明细、数量调整、地址管理、下单和模拟支付。
- 微信小程序用户端：登录、分类菜品浏览、加购物车、地址管理、结算、模拟支付、订单列表。
- 后端能力：统一 REST API、token 鉴权、H2/MySQL 数据库、购物车数量调整、阿里云 OSS 可选上传、Redis 可选 token 存储、Swagger/OpenAPI 文档。
- 实验材料：版本归档、实验报告、外部工具使用文档、Postman Collection 模板。

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
| 前端 | React 19、Vite、多页面构建、motion |
| 管理端图表 | ECharts 5.5.0 |
| API 文档 | springdoc-openapi-ui 1.7.0 |
| 缓存 | Redis，可选启用 |
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
| `OSS_MODE` | `local` | `local` 或 `oss` |
| `OSS_ENDPOINT` | 空 | 阿里云 OSS endpoint |
| `OSS_ACCESS_KEY_ID` | 空 | 阿里云 AccessKey ID |
| `OSS_ACCESS_KEY_SECRET` | 空 | 阿里云 AccessKey Secret |
| `OSS_BUCKET_NAME` | 空 | OSS bucket |

### 切换 Redis

默认不依赖 Redis。需要启用 Redis 时：

```powershell
$env:REDIS_MODE="redis"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
mvn -s maven-settings.xml spring-boot:run
```

Redis 不可用时，token 会自动降级到内存和数据库兼容逻辑，不阻止项目启动。

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
CREATE DATABASE sky_take_out CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 设置环境变量：

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="sky_take_out"
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

1. 后端是否运行在 `localhost:8080`。
2. 微信开发者工具是否关闭合法域名校验。
3. `miniprogram/app.js` 中的 `baseUrl` 是否正确。
4. 需要登录的接口是否已经获取并保存 token。

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
- [ ] Web 用户端可完成下单和模拟支付。
- [ ] 微信小程序可完成登录、加购物车、添加地址、结算、支付、查看订单。
- [ ] `http://localhost:8080/swagger-ui.html` 可查看 API 文档。
- [ ] Postman Collection 可导入并运行核心下单流程。
