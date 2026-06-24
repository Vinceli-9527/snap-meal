# snap-meal React 前端

前端源码位于 `frontend/`，使用 React + Vite。构建产物会输出到后端静态目录：

```text
src/main/resources/static
```

## 常用命令

```powershell
cd frontend
npm install
npm run dev
npm run build
```

也可以在项目根目录直接执行：

```powershell
npm run build
```

- `npm run dev` 启动 Vite 开发服务器，适合修改 React 源码时使用。
- `npm run build` 生成 Spring Boot 可直接托管的静态文件。
- 后端接口路径保持不变，仍然使用 `/api/admin/**` 和 `/api/user/**`。

## 页面入口

- `/` 对应 `frontend/src/pages/home.jsx`
- `/admin.html` 对应 `frontend/src/pages/admin.jsx`
- `/user.html` 对应 `frontend/src/pages/user.jsx`
