# Snap Meal 外卖系统部署与试用手册

Snap Meal 是依据“Java 软件开发实战”课程要求构建的外卖业务演示系统，包含首页、运营管理端和用户点餐端。

本手册主要面向第一次接触 Java 项目的试用者。按照“默认部署”章节逐步操作，不需要了解编程，也不需要安装 MySQL、Redis、Node.js、微信开发者工具或阿里云 OSS。

## 1. 系统包含什么

- 运营管理端：管理员登录、分类管理、菜品管理、套餐管理、订单处理、经营统计和 Excel 报表导出。
- 用户点餐端：模拟手机号码登录、模拟微信登录、浏览菜品、购物车、地址簿、下单和模拟支付。
- 本地数据存储：默认使用内置 H2 数据库，关闭程序后数据不会丢失。
- 本地文件存储：上传的图片默认保存在项目的 `uploads` 目录。

当前微信登录、微信支付和阿里云 OSS 均为教学模拟模式，不会发送短信、唤起真实微信支付或上传文件到云端。

## 2. 部署前准备

### 2.1 必须准备的环境

| 项目 | 要求 | 用途 |
| --- | --- | --- |
| 操作系统 | Windows 10 或 Windows 11，64 位 | 本手册中的命令均以 Windows 为例 |
| JDK | JDK 8，推荐 Eclipse Temurin 8 | 编译和运行 Java 程序 |
| Maven | 3.6 或更高版本 | 下载依赖、测试和启动项目 |
| 浏览器 | Edge、Chrome 或 Firefox | 访问系统页面 |
| 网络 | 第一次构建时需要联网 | 下载 Maven 依赖，后续通常可以离线运行 |
| 端口 | 默认使用 8080 | 浏览器通过该端口访问系统 |

请注意：必须安装 **JDK**，不能只有 JRE。判断标准是 `java` 和 `javac` 两条命令都必须可用。

### 2.2 默认部署不需要安装的内容

- MySQL
- Redis
- Nginx
- Node.js 或 npm
- 微信开发者工具
- 阿里云 OSS
- Cpolar

如果只是本机试用或课程验收，请直接使用默认部署方式。

## 3. 检查 Java 和 Maven

### 3.1 打开 PowerShell

按下 `Win + R`，输入：

```text
powershell
```

按回车打开蓝色或黑色命令窗口。

### 3.2 检查 JDK

依次执行：

```powershell
java -version
javac -version
```

两条命令都应该显示版本号，例如：

```text
java version "1.8.0_492"
javac 1.8.0_492
```

如果 `java -version` 正常，但 `javac -version` 提示未找到命令，说明当前只有 JRE，需要安装完整 JDK。

### 3.3 检查 Maven

执行：

```powershell
mvn -version
```

应看到 Maven 版本和 Java 信息。重点检查 `Java home`，它必须指向 JDK，例如：

```text
Java home: C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot\jre
```

某些 Maven 版本会显示 `runtime` 而不是 `Java home`。路径末尾出现 `\jre` 并不一定有问题；只要它位于 `jdk-...-hotspot` 目录内部，并且 `javac -version` 正常即可。

如果 `Java home` 指向旧 JRE，可以先在当前 PowerShell 临时修正。请将路径改成电脑上的实际 JDK 目录：

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

然后重新执行：

```powershell
javac -version
mvn -version
```

如果不知道 JDK 安装位置，可以执行：

```powershell
Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory
```

## 4. 默认部署：无需安装数据库

### 第一步：进入项目目录

本项目根目录中应该能看到 `pom.xml`、`README.md` 和 `maven-settings.xml`。

假设项目位于 `C:\Users\Vince\Desktop\java\snap-meal`，在 PowerShell 执行：

```powershell
cd "C:\Users\Vince\Desktop\java\snap-meal"
```

如果项目放在其他位置，请替换成实际路径。

执行下面的命令确认位置正确：

```powershell
Get-ChildItem pom.xml,maven-settings.xml
```

如果能看到两个文件，说明目录正确。

### 第二步：下载依赖并执行自动测试

执行：

```powershell
mvn -s maven-settings.xml clean test
```

第一次执行可能需要几分钟，因为 Maven 需要联网下载依赖。下载文件会保存在项目的 `.m2` 目录。

看到以下内容表示构建和测试成功：

```text
BUILD SUCCESS
Tests run: 6, Failures: 0, Errors: 0
```

如果这里出现错误，请先查看本文末尾的“常见问题”。

### 第三步：启动系统

执行：

```powershell
mvn -s maven-settings.xml spring-boot:run
```

启动过程中不要关闭 PowerShell。看到类似以下内容表示启动成功：

```text
Started SnapMealApplication
```

### 第四步：打开系统

在浏览器地址栏分别访问：

| 页面 | 地址 | 说明 |
| --- | --- | --- |
| 系统首页 | <http://localhost:8080/> | 对应项目中的 `index.html` |
| 运营管理端 | <http://localhost:8080/admin.html> | 首次进入需要管理员登录 |
| 用户点餐端 | <http://localhost:8080/user.html> | 首次进入需要选择模拟登录方式 |

管理端默认账号：

```text
用户名：admin
密码：123456
```

用户端登录方式：

- 手机号码登录：输入任意非空手机号码，登录后显示“手机体验用户”。
- 微信登录：不调用真实微信，登录后显示“微信体验用户”。

### 第五步：停止系统

回到正在运行程序的 PowerShell 窗口，按下：

```text
Ctrl + C
```

出现是否终止的询问时，输入 `Y` 并回车。停止后浏览器将无法继续访问系统。

## 5. 推荐试用流程

### 5.1 用户下单

1. 打开系统首页。
2. 点击“打开用户点餐端”。
3. 选择“手机号码登录”或“微信登录”。
4. 选择分类和菜品，点击 `+` 加入购物车。
5. 点击“去结算”。
6. 第一次结算时填写收货人、手机号和地址。
7. 提交订单，系统会自动完成模拟支付。

### 5.2 管理端处理订单

1. 回到系统首页。
2. 点击“进入运营管理端”。
3. 使用 `admin / 123456` 登录。
4. 打开“订单管理”。
5. 按订单状态依次执行“接单”“派送”“完成”。
6. 在“经营概览”查看统计数据。
7. 点击“导出运营报表”下载 Excel 文件。
8. 点击“退出登录”后，后端会立即撤销当前登录状态；原令牌无法再次使用。

## 6. 数据保存、备份和重置

### 6.1 数据保存位置

默认数据库文件位于：

```text
data\snap-meal.mv.db
```

上传图片位于：

```text
uploads\
```

登录会话、用户、购物车、订单和管理数据都保存在数据库中。正常停止或重启程序不会清空数据。

### 6.2 备份数据

1. 先按 `Ctrl + C` 停止系统。
2. 复制整个 `data` 目录到安全位置。
3. 如果需要保留上传图片，同时复制 `uploads` 目录。

不要在程序运行时复制或移动数据库文件，否则可能得到不完整的备份。

### 6.3 恢复为全新数据

该操作会清除用户、购物车、订单和登录记录，请先备份。

1. 停止系统。
2. 将项目中的 `data` 目录改名为 `data-backup`。
3. 重新启动系统。

程序会自动创建新的数据库和演示数据。确认新数据库正常后，再决定是否删除 `data-backup`。

## 7. 构建独立运行包

如果不想每次都通过 Maven 启动，可以先生成一个可运行的 JAR 文件。

### 7.1 生成 JAR

在项目根目录执行：

```powershell
mvn -s maven-settings.xml clean package
```

看到 `BUILD SUCCESS` 后，会生成：

```text
target\snap-meal-1.0.0.jar
```

### 7.2 运行 JAR

执行：

```powershell
java -jar target\snap-meal-1.0.0.jar
```

看到 `Started SnapMealApplication` 后，仍然通过 <http://localhost:8080/> 访问。

停止方式仍然是 `Ctrl + C`。

## 8. 修改端口

如果启动时提示 `Port 8080 was already in use`，说明8080端口已被其他程序占用。

可以临时改用8090端口：

```powershell
$env:SERVER_PORT="8090"
mvn -s maven-settings.xml spring-boot:run
```

然后访问：

```text
http://localhost:8090/
```

关闭当前 PowerShell 后，该临时端口设置会自动失效。

## 9. 可选：切换到 MySQL

本节不是首次试用的必需步骤。只有明确要求使用 MySQL 时才需要操作。

### 9.1 准备 MySQL

- 安装并启动 MySQL 8。
- 记住 `root` 用户的密码。
- 使用 MySQL Workbench 或其他数据库工具连接 MySQL。

执行以下 SQL 创建数据库：

```sql
CREATE DATABASE sky_take_out
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 9.2 设置 MySQL 参数

在启动项目的同一个 PowerShell 窗口执行：

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="sky_take_out"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="你的MySQL密码"
```

然后启动：

```powershell
mvn -s maven-settings.xml spring-boot:run
```

程序会自动创建数据表和基础演示数据。不要把真实数据库密码写入 `application-mysql.yml` 或提交到版本管理系统。

如果需要恢复默认 H2 模式，请关闭当前 PowerShell，重新打开后直接运行默认启动命令。

## 10. 环境变量说明

| 环境变量 | 默认值 | 作用 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Web 服务端口 |
| `TOKEN_SECRET` | 内置教学密钥 | 登录令牌签名密钥，公开部署时必须修改 |
| `SPRING_PROFILES_ACTIVE` | 默认模式 | 设置为 `mysql` 时启用 MySQL |
| `MYSQL_HOST` | `localhost` | MySQL 地址 |
| `MYSQL_PORT` | `3306` | MySQL 端口 |
| `MYSQL_DATABASE` | `sky_take_out` | MySQL 数据库名 |
| `MYSQL_USERNAME` | `root` | MySQL 用户名 |
| `MYSQL_PASSWORD` | `root` | MySQL 密码 |

## 11. 常见问题

### 11.1 提示“没有编译器”或 `No compiler is provided`

原因：Maven 使用了 JRE，而不是 JDK。

处理步骤：

1. 执行 `javac -version`，确认命令存在。
2. 执行 `mvn -version`，检查 `Java home`。
3. 按照本文第3节重新设置 `JAVA_HOME`。
4. 关闭并重新打开 PowerShell 后再次启动。

### 11.2 提示无法找到 `mvn`

原因：Maven 未安装，或者 Maven 的 `bin` 目录没有加入系统 `Path`。

处理方式：安装 Maven 3.6或更高版本，将其 `bin` 目录加入 `Path`，重新打开 PowerShell，再执行 `mvn -version`。

### 11.3 Maven 下载依赖失败

确认电脑可以访问网络，然后重新执行：

```powershell
mvn -s maven-settings.xml clean test
```

项目自带的 `maven-settings.xml` 已配置公共 Maven 镜像，因此不要省略 `-s maven-settings.xml`。

### 11.4 登录状态无效或已过期

管理端可直接打开：

```text
http://localhost:8080/admin.html?logout=1
```

该地址会清除浏览器中的旧管理端令牌，并显示登录页。

用户端出现相同问题时，刷新 `user.html`，页面会自动清除失效令牌并重新显示登录方式选择。

### 11.5 页面仍显示旧内容

1. 确认已经停止并重新启动后端。
2. 在浏览器按 `Ctrl + F5` 强制刷新。
3. 如果仍未更新，关闭当前页面后重新打开。

### 11.6 中文显示乱码

项目已经统一使用 UTF-8。出现乱码时先重启系统，然后按 `Ctrl + F5` 刷新浏览器。不要使用记事本以 ANSI 或 GBK 编码保存项目文件。

### 11.7 数据库被占用或无法打开

通常是同时启动了多个 Snap Meal 实例。关闭所有正在运行项目的 PowerShell 窗口，只保留一个实例，再重新启动。

### 11.8 8080端口被占用

按照本文第8节将端口临时修改为8090，或者关闭占用8080端口的程序。

## 12. 对外部署前的安全提醒

当前系统用于本地实验和教学演示，不应直接暴露到互联网。对外部署前至少需要完成：

- 修改默认管理员密码，并改用安全的密码散列存储。
- 设置足够长且随机的 `TOKEN_SECRET`。
- 使用 HTTPS。
- 限制文件上传类型和访问权限。
- 配置正式 MySQL 账号，不使用 `root`。
- 接入真实微信、支付或 OSS 时，通过环境变量或密钥管理服务保存凭证。
- 增加日志、备份、监控和访问频率限制。

## 13. 部署完成检查表

完成部署后逐项确认：

- [ ] `java -version` 有版本输出。
- [ ] `javac -version` 有版本输出。
- [ ] `mvn -version` 中的 Java home 指向 JDK。
- [ ] `mvn -s maven-settings.xml clean test` 显示 `BUILD SUCCESS`。
- [ ] 启动日志出现 `Started SnapMealApplication`。
- [ ] 首页可以打开。
- [ ] 管理端可以使用 `admin / 123456` 登录。
- [ ] 用户端可以选择手机号码或微信模拟登录。
- [ ] 用户可以完成加入购物车和模拟支付。
- [ ] 管理端可以处理订单并导出 Excel。
- [ ] 退出管理端后，原登录状态立即失效。
