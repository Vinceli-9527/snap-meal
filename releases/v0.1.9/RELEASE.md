# snap-meal v0.1.9

发布日期：2026-08-17

本版本为安全加固迭代：针对审查发现的 4 处「非预期实现缺陷」做定向修复，不影响任何演示设定（默认口令、无验证码登录、mock 支付/微信等保持不变）。

## 更新重点

### U1 上传校验加固

- `OssService` 在 Content-Type 白名单之外，新增扩展名白名单（`.jpg`/`.jpeg`/`.png`/`.webp`）。
- 新增图片魔数（Magic Number）嗅探：按文件头识别 JPEG（FF D8 FF）/PNG（89 50 4E 47）/WebP（RIFF....WEBP），识别失败即拒绝。
- 保存文件名不再信任原始文件名，扩展名以魔数识别结果为准。
- 效果：杜绝「声明 image/png 的 .html/.svg 脚本文件」经 `/uploads/**` 同源直出造成的存储型 XSS。

### U2 Text2SQL 校验器加固

- `SqlSafetyValidator` 新增危险函数/写操作黑名单：`CSVREAD`、`CSVWRITE`、`FILE_READ`、`FILE_WRITE`、`SLEEP`、`BENCHMARK`、`GET_LOCK`、`RELEASE_LOCK`、`PG_SLEEP`、`WAITFOR`、`SHUTDOWN`、`SELECT INTO`、`SELECT FOR UPDATE` 等。
- 新增业务表白名单：Agent 仅可查询 `orders`、`order_detail`、`dish`、`category`、`app_user`、`setmeal`、`setmeal_dish`、`address_book`、`shop_state`、`shopping_cart`；`employee`（含口令）、`auth_session`（token 哈希）等敏感表不可查询。
- 效果：即使提示注入诱导，也无法通过 Agent 读取服务器文件或敏感表。

### U3 异常信息脱敏

- `GlobalExceptionHandler` 对未处理异常仅返回通用文案「服务器内部错误，请稍后重试」。
- 异常详情（SQL 错误、内部路径、第三方服务细节）写入服务端日志，不再回显给前端。

### U4 H2 配置收敛

- 默认连接串移除 `AUTO_SERVER=TRUE`，不再在 localhost:9092 开放无鉴权 TCP 端口。
- 单进程演示（`mvn -s maven-settings.xml spring-boot:run`）不受影响；若确实需要多进程共享数据库文件，请自行加回并设置强口令。

### 其他

- `.gitignore` 增补 `/.tmp-docx-check/`。
- 保留本机已有的 CORS 全开与 OPTIONS 预检放行（演示便利）。

### 数据与测试

- 新增 10 个测试：SqlSafetyValidator 危险函数/未知表/业务表白名单（3）、OssService 上传校验（7），测试总数从 65 增加到 75。

## 验证情况

- `mvn -s maven-settings.xml clean test` 通过，75 个用例全部成功。
- 说明：本机 JDK 安装路径含空格（C:\Program Files\...），Surefire 2.22.2 fork 启动时需加 `-DforkCount=0` 在 Maven 进程内执行；该参数不影响测试结果与产物。
- 无前端改动，无需重新构建。

## 文件

- `snap-meal-v0.1.9-source-20260817.zip`：源码归档
- `snap-meal-v0.1.9-source-20260817.sha256.txt`：SHA-256 校验文件

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

- 「经营问答」Agent 现仅可查询业务表白名单内的表；后续若新增业务表，需要同步加入 `SqlSafetyValidator.ALLOWED_TABLES`。
- 上传仅接受 JPG/PNG/WebP（Content-Type + 扩展名 + 文件头三重校验），其他格式会被拒绝。
- 未处理异常不再向前端回显细节，排查问题请查看服务端日志。

## 用法示例

```text
启动：mvn -s maven-settings.xml spring-boot:run
访问：http://localhost:8080/user.html  →  点餐下单
管理端：http://localhost:8080/admin.html  →  admin / 123456（演示默认口令）
经营问答：管理端「经营问答」→ 配置 DeepSeek API Key 后提问
```
