import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({
    plugins: [react()],
    server: {
        host: true, // 局域网可访问（手机/其他电脑经 http://本机IP:5173 打开），生产用后端托管 dist
        port: 5173,
        proxy: {
            '/api': 'http://localhost:3000',
            '/uploads': 'http://localhost:3000',
            '/ws': { target: 'ws://localhost:3000', ws: true },
        },
    },
});
