# 1. 构建阶段
FROM node:22-alpine AS build-stage
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 2. 运行阶段（用 Nginx 托管）
FROM nginx:alpine AS production-stage
# 将构建好的静态文件复制到 Nginx 目录
COPY --from=build-stage /app/dist /usr/share/nginx/html
# 将自定义的 nginx.conf 复制到容器内，替换默认配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]