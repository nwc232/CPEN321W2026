import fs from 'node:fs';
import http from 'node:http';
import https from 'node:https';

import { createApp } from './app';
import { env } from './config/env';

const app = createApp();

const useHttps = env.tlsKeyPath !== '' && env.tlsCertPath !== '';

const server = useHttps
  ? https.createServer(
      {
        key: fs.readFileSync(env.tlsKeyPath),
        cert: fs.readFileSync(env.tlsCertPath),
      },
      app,
    )
  : http.createServer(app);

server.listen(env.port, () => {
  console.log(`Server listening on ${useHttps ? 'https' : 'http'}://0.0.0.0:${env.port}`);
});

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    server.close(() => {
      process.exit(0);
    });
  });
}
