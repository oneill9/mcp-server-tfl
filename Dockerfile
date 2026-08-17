FROM node:22-alpine AS builder

WORKDIR /workspace/node
COPY node/package.json node/package-lock.json ./
RUN npm ci

COPY node/tsconfig.json ./
COPY node/src ./src
COPY node/scripts/copy-resources.mjs ./scripts/copy-resources.mjs
COPY shared /workspace/shared
RUN npm run build \
    && npm prune --omit=dev

FROM node:22-alpine

ENV NODE_ENV=production \
    HOST=0.0.0.0

WORKDIR /app
COPY --from=builder --chown=node:node /workspace/node/package.json ./package.json
COPY --from=builder --chown=node:node /workspace/node/node_modules ./node_modules
COPY --from=builder --chown=node:node /workspace/node/dist ./dist

LABEL io.modelcontextprotocol.server.name="io.github.oneill9/tfl-mcp-server"

USER node

EXPOSE 8080

ENTRYPOINT ["node", "dist/index.js"]
