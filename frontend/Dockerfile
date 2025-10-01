# Build Angular app
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Serve with nginx
FROM nginx:stable-alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80

# Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf
