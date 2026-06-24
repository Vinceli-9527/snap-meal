# Snap Meal 外部工具使用文档

本文档用于记录实验 7、8、9、10 中需要手动完成的外部工具操作。实际写入实验报告时，每个实验至少包含工具名称和版本、配置步骤、关键截图、遇到的问题和解决方法。

项目根目录：`C:\Users\Vince\Desktop\java\sky-lab`

后端默认地址：`http://localhost:8080`

启动命令：

```powershell
mvn -s maven-settings.xml spring-boot:run
```

## 截图命名建议

建议将截图统一保存到 `output/experiment-screenshots/`，命名如下：

- `exp7-workbench-tables.png`
- `exp8-apifox-api-tree.png`
- `exp8-apifox-shared-doc.png`
- `exp9-swagger-home.png`
- `exp9-postman-runner-pass.png`
- `exp10-cpolar-forwarding.png`
- `exp10-phone-public-page.png`

## 实验 7：WorkBench 建表

### 工具名称和版本

- 工具：MySQL Workbench
- 建议版本：8.0.x
- 数据库：MySQL 8.0.x
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_unicode_ci`

### 操作步骤

1. 访问 MySQL 官网下载并安装 MySQL Workbench。
2. 启动本地 MySQL 服务。
3. 打开 MySQL Workbench，连接本地 MySQL 实例。
4. 新建 SQL 查询窗口，执行数据库创建语句：

```sql
CREATE DATABASE sky_take_out CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sky_take_out;
```

5. 打开项目中的建表脚本：

```text
src/main/resources/schema.sql
```

6. 按实验要求执行以下 11 张表的建表语句：

```text
employee
app_user
category
dish
setmeal
setmeal_dish
address_book
shopping_cart
orders
order_detail
auth_session
```

7. 在 Workbench 左侧 `Schemas` 面板刷新 `sky_take_out`，展开 `Tables` 节点。
8. 截图左侧 Schema 面板，确保能看到上述 11 张表。

注意：项目当前 `schema.sql` 还包含 `dish_flavor` 和 `shop_state` 等辅助表。如果直接导入完整脚本，Workbench 中可能显示超过 11 张表；报告中按实验要求重点说明指定的 11 张业务表即可。

### 报告截图

- 截图 1：Workbench 连接本地 MySQL 成功。
- 截图 2：`sky_take_out` 数据库下显示 11 张表。
- 截图 3：任选一张表展示字段结构，例如 `orders` 或 `dish`。

### 常见问题和解决方法

- 问题：连接 MySQL 失败。
  解决：检查 MySQL 服务是否启动，端口是否为 `3306`，用户名和密码是否正确。

- 问题：中文乱码。
  解决：创建数据库时使用 `utf8mb4` 字符集，并确认 SQL 文件以 UTF-8 编码打开。

- 问题：外键创建失败。
  解决：按脚本顺序执行建表语句，先创建被引用的主表，再创建包含外键的子表。

## 实验 8：Apifox 接口文档管理

### 工具名称和版本

- 工具：Apifox
- 建议版本：最新版桌面版
- 项目名称：`Snap Meal API`
- 基础地址：`http://localhost:8080`

### 操作步骤

1. 下载安装 Apifox。
2. 打开 Apifox，创建新项目 `Snap Meal API`。
3. 在项目中配置环境变量：

```text
baseUrl = http://localhost:8080
adminToken = 管理端登录后返回的 token
userToken = 用户端登录后返回的 token
```

4. 按模块创建接口目录：

```text
管理端-认证
管理端-菜品
管理端-分类
管理端-订单
管理端-报表
用户端-认证
用户端-目录
用户端-购物车
用户端-地址
用户端-订单
```

5. 每个接口填写请求方式、路径、请求头、请求体示例、成功响应示例、失败响应示例。
6. 管理端接口需要请求头：

```text
token: {{adminToken}}
```

7. 用户端接口需要请求头：

```text
authentication: {{userToken}}
```

8. 使用 Apifox 的“分享文档”功能生成在线文档链接。
9. 截图接口目录结构和分享文档页面。

### 核心接口录入建议

如果时间有限，至少完成以下 4 条核心链路：

1. 管理员登录：`POST /api/admin/auth/login`
2. 用户登录：`POST /api/user/auth/login`
3. 菜品浏览：`GET /api/user/catalog/categories` 和 `GET /api/user/catalog/dishes?categoryId=1`
4. 下单支付：`POST /api/user/orders` 和 `POST /api/user/orders/{id}/pay`

### 请求示例

管理员登录：

```json
{
  "username": "admin",
  "password": "123456"
}
```

用户登录：

```json
{
  "loginMethod": "WECHAT"
}
```

添加购物车：

```json
{
  "dishId": 1
}
```

新增地址：

```json
{
  "consignee": "张三",
  "sex": "",
  "phone": "13800000000",
  "provinceName": "上海市",
  "cityName": "上海",
  "districtName": "杨浦",
  "detail": "大学路100号",
  "label": "学校",
  "longitude": 121.506377,
  "latitude": 31.302272
}
```

提交订单：

```json
{
  "addressBookId": 1,
  "payMethod": 1,
  "remark": ""
}
```

### 报告截图

- 截图 1：Apifox 项目 `Snap Meal API` 的接口目录。
- 截图 2：某个接口的请求参数和响应示例。
- 截图 3：在线分享文档链接页面。

### 常见问题和解决方法

- 问题：接口返回 401。
  解决：检查请求头名称。管理端为 `token`，用户端为 `authentication`。

- 问题：下单接口报“购物车为空”。
  解决：先调用添加购物车接口，再调用提交订单接口。

- 问题：接口路径不一致。
  解决：以当前后端真实接口为准。菜品列表接口为 `GET /api/user/catalog/dishes?categoryId=1`。

## 实验 9：Swagger + Postman 接口测试

### Swagger 工具名称和版本

- 工具：springdoc-openapi
- 依赖版本：`springdoc-openapi-ui 1.7.0`
- 文档地址：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/api-docs`

### Swagger 操作步骤

1. 启动后端项目。
2. 浏览器访问：

```text
http://localhost:8080/swagger-ui.html
```

3. 查看自动扫描生成的 Controller 接口。
4. 点击某个接口，查看请求路径、参数、响应结构。
5. 对需要认证的接口，点击页面中的 Authorize，填写对应请求头 token。
6. 截图 Swagger 页面。

### Postman 工具名称和版本

- 工具：Postman
- 建议版本：最新版桌面版
- Collection 名称：`Snap Meal`
- 环境变量：

```text
baseUrl = http://localhost:8080
adminToken = 空
userToken = 空
addressBookId = 空
orderId = 空
```

### Postman 操作步骤

1. 下载安装 Postman。
2. 创建 Collection `Snap Meal`。
3. 创建 Environment `Snap Meal Local`。
4. 添加 `baseUrl`、`adminToken`、`userToken`、`addressBookId`、`orderId` 等变量。
5. 添加并保存以下请求：

```text
管理员登录
获取分类列表
获取菜品列表
用户登录
添加购物车
查看购物车
新增地址
提交订单
模拟支付
```

6. 在登录接口的 Tests 中提取 token。
7. 在新增地址或提交订单后提取 `addressBookId`、`orderId`。
8. 使用 Runner 按顺序执行完整用户下单流程：

```text
用户登录 -> 获取菜品列表 -> 添加购物车 -> 新增地址 -> 提交订单 -> 模拟支付 -> 查看订单
```

9. 截图 Runner 执行结果，要求全部绿色 PASS。
10. 导出 Collection 为 JSON 文件，保存到：

```text
output/SnapMeal.postman_collection.json
```

本项目已提供一个基础 Postman Collection 模板，可直接导入后根据实际运行结果调整。

### Postman Tests 脚本示例

用户登录后提取 token：

```javascript
const body = pm.response.json();
pm.test("login success", function () {
  pm.expect(body.success).to.eql(true);
});
pm.environment.set("userToken", body.data.token);
```

提交订单后提取订单 ID：

```javascript
const body = pm.response.json();
pm.test("submit order success", function () {
  pm.expect(body.success).to.eql(true);
});
pm.environment.set("orderId", body.data.id);
```

### 报告截图

- 截图 1：Swagger 首页。
- 截图 2：Postman Collection 请求列表。
- 截图 3：Runner 执行结果全部 PASS。
- 截图 4：导出的 Collection JSON 文件位于 `output/`。

### 常见问题和解决方法

- 问题：Postman 请求失败，提示连接不上。
  解决：确认后端已启动，`baseUrl` 为 `http://localhost:8080`。

- 问题：提交订单失败。
  解决：必须先添加购物车，并确保已有收货地址。

- 问题：支付失败。
  解决：支付接口只能支付状态为待付款的订单，重复支付会失败。

## 实验 10：Cpolar 内网穿透

### 工具名称和版本

- 工具：Cpolar
- 建议版本：最新版
- 本地服务端口：`8080`
- 后端首页：`http://localhost:8080`

### 操作步骤

1. 访问 Cpolar 官网注册账号：

```text
https://www.cpolar.com/
```

2. 下载并安装 Cpolar。
3. 登录 Cpolar。
4. 启动 Snap Meal 后端，确认本机可访问：

```text
http://localhost:8080
```

5. 打开终端执行：

```powershell
cpolar http 8080
```

6. 复制终端输出的公网 Forwarding 地址，例如：

```text
https://xxxx.cpolar.io
```

7. 使用手机浏览器或另一台电脑访问该公网地址。
8. 确认能打开 Snap Meal 首页。
9. 截图 Cpolar 终端输出和手机浏览器访问结果。

### 报告截图

- 截图 1：Cpolar 终端显示 `Forwarding` 公网地址。
- 截图 2：手机浏览器打开公网地址后的 Snap Meal 首页。

### 常见问题和解决方法

- 问题：公网地址打不开。
  解决：确认后端服务正在 `8080` 端口运行，且 Cpolar 命令未关闭。

- 问题：页面能打开但接口请求失败。
  解决：检查前端请求是否使用相对路径。Snap Meal Web 端默认使用相对路径，适合通过 Cpolar 访问。

- 问题：Cpolar 地址变化。
  解决：免费临时域名可能每次启动变化，报告中记录本次实验实际生成的地址即可。

## 实验报告可粘贴内容

### 实验 7 简述

本实验使用 MySQL Workbench 完成数据库建表。首先启动本地 MySQL 服务并连接到本地实例，然后创建 `sky_take_out` 数据库，字符集设置为 `utf8mb4`。随后根据项目 `src/main/resources/schema.sql` 中的建表语句创建员工、用户、分类、菜品、购物车、订单、订单明细、认证会话等业务表。建表完成后在 Workbench 的 Schema 面板中刷新数据库并查看表结构，验证数据库表创建成功。

### 实验 8 简述

本实验使用 Apifox 管理 Snap Meal 项目的接口文档。创建 `Snap Meal API` 项目后，按照管理端和用户端业务模块划分接口目录，录入认证、菜品、分类、订单、报表、购物车、地址等接口。每个接口记录请求方式、路径、请求头、请求体示例和响应示例，并通过 Apifox 的分享文档功能生成在线接口文档链接，便于接口调试和团队查看。

### 实验 9 简述

本实验使用 Swagger 和 Postman 完成接口文档查看与接口流程测试。Swagger 由项目集成的 springdoc-openapi 自动生成，启动后端后访问 `http://localhost:8080/swagger-ui.html` 即可查看全部 REST API。Postman 中创建 `Snap Meal` Collection，并配置环境变量保存登录 token、地址 ID、订单 ID。通过 Runner 顺序执行用户登录、添加购物车、新增地址、提交订单、模拟支付等请求，验证用户下单流程可正常完成。

### 实验 10 简述

本实验使用 Cpolar 将本地 `8080` 端口映射为公网地址。启动 Snap Meal 后端后执行 `cpolar http 8080`，获取 Cpolar 分配的临时公网域名。随后使用手机浏览器访问该公网地址，验证外部网络可以打开 Snap Meal 系统首页，说明本地服务已成功完成内网穿透。
