import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  base: "/forespoersler/",
  plugins: [react()],
  server: {
    proxy: {
      "/hentEksponerteForespoersler": "http://localhost:8080",
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test-setup.ts",
    restoreMocks: true,
  },
});
