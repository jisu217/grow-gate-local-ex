FROM node:20-alpine AS build
WORKDIR /workspace

COPY frontend/FE-02/package*.json ./
RUN npm ci

COPY frontend/FE-02 ./
RUN npm run build

FROM nginx:1.27-alpine
COPY frontend/FE-02/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
