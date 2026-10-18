import { defineConfig } from 'vite';

// Local laboratory only. Network policy applies to workers and embedded SDK UI too.
const policy = "default-src 'self' data: blob:; script-src 'self' blob: 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; connect-src 'self' blob: ws://127.0.0.1:5173; worker-src 'self' blob:; img-src 'self' data: blob:; font-src 'self' data: blob:; frame-src 'self' blob:; object-src 'none'; base-uri 'self'; form-action 'self'";
export default defineConfig({
  server: { host: '127.0.0.1', port: 5173, strictPort: true, headers: { 'Content-Security-Policy': policy } },
  preview: { host: '127.0.0.1', port: 5173, strictPort: true, headers: { 'Content-Security-Policy': policy } },
});
