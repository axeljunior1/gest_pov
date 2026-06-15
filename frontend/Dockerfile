# =============================================================================
# Frontend Gest_POV — image monapp-frontend:1.0.0
# Build Vite + serveur nginx (SPA + proxy /api)
# =============================================================================

# --- Étape 1 : build React/Vite ---
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

COPY . .
# VITE_API_BASE non défini → le client axios utilise /api (proxifié par nginx)
RUN npm run build

# --- Étape 2 : nginx ---
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:80/ || exit 1
