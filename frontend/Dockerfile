# frontend/Dockerfile
# Dockerfile personalizado para Railway - Sin cache

FROM node:22-alpine AS builder

# Set working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies (sin cache)
RUN npm ci --legacy-peer-deps

# Copy all files
COPY . .

# Build the application (sin cache)
RUN npm run build -- --configuration=production

# Production stage - Serve static files
FROM node:22-alpine AS production

WORKDIR /app

# Install serve globally
RUN npm install -g serve

# Copy built application from builder stage
COPY --from=builder /app/dist/gamelyx ./dist/gamelyx

# Expose port (Railway sets PORT env variable)
EXPOSE 3000

# Start the server
CMD ["serve", "dist/gamelyx", "-s", "-l", "3000"]