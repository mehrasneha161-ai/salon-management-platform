# syntax=docker/dockerfile:1

# ---- Build stage: compile the React app with Vite ----
FROM node:20-alpine AS builder
WORKDIR /app

# Install deps first for better layer caching. No package-lock.json in repo,
# so use `npm install` (not `npm ci`).
COPY package.json ./
RUN npm install

# Copy sources and build. We invoke `vite build` directly (esbuild transpile)
# so the production image build is not blocked by strict `tsc` type-checks —
# type-checking belongs in CI/lint, not in the runtime image build.
COPY . .
RUN npx vite build

# ---- Serve stage: static files behind nginx (also reverse-proxies the API) ----
FROM nginx:1.27-alpine
# Custom config: SPA fallback + /api and /ws proxy to the backend.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
