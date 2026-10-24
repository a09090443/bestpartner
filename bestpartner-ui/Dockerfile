# 工作流程前端 image：Node 建置 + nginx 提供靜態檔與反向代理
# 建置 context 為 bestpartner-ui/；.env 與 .env.* 已由 .dockerignore 排除，不會進入 image。

# ---- build stage ----
# Vite 8 需要 Node 20.19+ 或 22.12+
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- runtime stage ----
FROM nginx:1.27-alpine
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
