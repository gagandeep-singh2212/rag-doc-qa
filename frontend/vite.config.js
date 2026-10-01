import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The dev server forwards /api to Spring Boot, so the browser never makes a
// cross-origin request and no CORS setup is needed on the backend.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: { "/api": "http://localhost:8080" } },
});
