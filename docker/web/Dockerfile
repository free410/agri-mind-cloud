FROM node:20-alpine AS build
WORKDIR /web
COPY smart-agriculture-web/package*.json ./
RUN npm ci
COPY smart-agriculture-web/ ./
ARG VITE_APP_BASE_API=/api
ENV VITE_APP_BASE_API=$VITE_APP_BASE_API
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /web/dist /usr/share/nginx/html
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
