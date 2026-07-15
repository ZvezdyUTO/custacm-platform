FROM node:20.19-alpine AS training-build

RUN corepack enable
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY frontend/ ./
RUN pnpm build

FROM node:20.19-alpine AS blog-build

WORKDIR /workspace/blog-view
COPY platform-blog/upstream/nblog/blog-view/package.json ./
RUN npm install
COPY platform-blog/upstream/nblog/blog-view/ ./
RUN npm run build

FROM nginx:1.27-alpine

COPY frontend/nginx.conf /etc/nginx/custacm/nginx-http.conf
COPY frontend/nginx-https.conf /etc/nginx/custacm/nginx-https.conf
COPY frontend/docker-entrypoint.d/10-select-nginx-config.sh /docker-entrypoint.d/10-select-nginx-config.sh
COPY frontend/test-nginx-config.sh /tmp/test-nginx-config.sh
RUN sed -i 's/\r$//' /docker-entrypoint.d/10-select-nginx-config.sh /tmp/test-nginx-config.sh \
    && chmod +x /docker-entrypoint.d/10-select-nginx-config.sh /tmp/test-nginx-config.sh \
    && /tmp/test-nginx-config.sh \
    && rm /tmp/test-nginx-config.sh
COPY --from=blog-build /workspace/blog-view/dist /usr/share/nginx/html/blog
COPY --from=training-build /workspace/frontend/dist /usr/share/nginx/html/training-app
