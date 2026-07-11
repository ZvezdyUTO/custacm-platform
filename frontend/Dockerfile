FROM node:20.19-alpine AS training-build

RUN corepack enable
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY frontend/ ./
RUN pnpm build

FROM node:20.19-alpine AS blog-build

WORKDIR /workspace/blog-view
COPY platform-blog/upstream/nblog/blog-view/package.json \
     platform-blog/upstream/nblog/blog-view/package-lock.json ./
RUN npm ci
COPY platform-blog/upstream/nblog/blog-view/ ./
RUN npm run build

FROM nginx:1.27-alpine

COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=blog-build /workspace/blog-view/dist /usr/share/nginx/html/blog
COPY --from=training-build /workspace/frontend/dist /usr/share/nginx/html/training-app
