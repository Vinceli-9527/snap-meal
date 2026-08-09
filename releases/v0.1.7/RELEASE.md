# snap-meal v0.1.7

发布日期：2026-08-10

本版本为管理端加入完整的「经营问答」能力：一个 Text2SQL Agent，把自然语言经营问题转成只读 SQL 查询并给出中文经营解释；同时把 DeepSeek 模型的配置入口搬到页面内，支持在多个模型间切换，并修复了 function-calling 多轮对话的协议问题。

## 更新重点

### 经营问答 Text2SQL Agent

- 管理端新增「经营问答」面板：输入「最近7天营收多少？」这类问题，Agent 自动生成 SQL、只读执行、展示结果表，并给出中文经营解读。
- 多轮 function-calling 自纠错循环：SQL 校验或执行失败时，错误会回传大模型自动修正重试（最多 3 次），成功后才返回最终答案。
- 只读 SQL 安全层：仅允许单条 SELECT，拒绝多语句拼接、注释绕过、DDL/DML 关键字与危险函数；查询在只读连接上执行，带 10 秒超时与 200 行结果上限。
- 回答按 Markdown 渲染：表格、加粗、列表、标题等都能正常排版显示，不再出现原始 `|` 管道符。

### Agent 设置卡片（页面内配置）

- 「经营问答」面板顶部新增「Agent 设置」卡片：粘贴 DeepSeek API Key → 保存即写入项目根目录 `.env` 并**即时生效，无需重启**；支持「测试连接」按钮实时校验 Key（区分认证失败/余额不足/限流/网络问题）。
- 模型下拉框：可在 `deepseek-v4-flash` / `deepseek-v4-pro` 间切换，保存后即时生效、重启仍保留。
- 配置信息脱敏展示：Key 显示为 `sk-****后四位`，并显示保存位置。

### 协议与模型修复

- 修复 `400 Messages with role 'tool' must be a response to a preceding message with 'tool_calls'`：多轮自纠错时按 OpenAI 兼容协议回传带 `tool_calls` 的 assistant 消息，V4 思考模式下同时回传 `reasoning_content`。
- 默认模型切换为 `deepseek-v4-flash`（官方已停用 `deepseek-chat`，2026-07-24 下线）。

### 数据与测试

- `data.sql` 内置 16 笔演示订单（订单号 `SM...`），覆盖全部 6 种订单状态，下单时间相对当前时刻偏移，保证任意时刻启动都能回答「近 7 天营收」「待接单数量」等问题。
- 测试从 9 个增加到 57 个：SQL 校验器单测、自纠错循环端到端（真实 H2 + 种子数据，LLM 用 Mockito 脚本化）、Key/模型存储单测、接口离线测试。

## 验证情况

- `mvn -s maven-settings.xml clean test` 通过，57 个用例全部成功。
- `npm run build` 通过，构建产物输出到 `src/main/resources/static/`。
- 真实服务冒烟验证：
  - 管理端登录、经营问答提问、Key 保存/测试连接、模型切换接口正常
  - `admin.html`、`user.html` 页面正常访问
  - headless 浏览器验证 markdown 表格渲染为真正的 HTML 表格，XSS 载荷被消毒移除

## 文件

- `snap-meal-v0.1.7-source-20260810.zip`：源码归档
- `snap-meal-v0.1.7-source-20260810.sha256.txt`：SHA-256 校验文件

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

- 演示/使用前需在管理端「经营问答 → Agent 设置」卡片粘贴真实 DeepSeek API Key 并保存（或设置环境变量 `DEEPSEEK_API_KEY`）；未配置时经营问答给出明确提示，不影响项目其他功能。
- 本归档是源码快照，不包含本地依赖、运行期数据库、构建缓存和日志。

## 用法示例

```text
启动：mvn -s maven-settings.xml spring-boot:run
访问：http://localhost:8080/admin.html  →  admin / 123456  →  经营问答
提问：最近7天营收多少？ / 哪个菜卖得最好？ / 有多少待接单订单？
```
