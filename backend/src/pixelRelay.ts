import { WebSocketServer, WebSocket as WsWebSocket } from 'ws';
import type { Server as HttpServer } from 'node:http';
import type { Server as HttpsServer } from 'node:https';

const UPSTREAM_URL = 'wss://8.229.22.124';

// Attaches websocket server to https server
export function attachPixelRelay(server: HttpServer | HttpsServer): void {
  const wss = new WebSocketServer({ server, path: '/pixels' });
  const clients = new Set<WsWebSocket>();

  wss.on('connection', (ws) => {
    clients.add(ws);
    ws.on('close', () => clients.delete(ws));
    ws.on('error', () => clients.delete(ws));
  });

  const connectUpstream = (): void => {
    const upstream = new WsWebSocket(UPSTREAM_URL, { rejectUnauthorized: false });

    upstream.on('open', () => console.log('Upstream pixel server connected'));

    upstream.on('message', (data, isBinary) => {
      for (const client of clients) {
        if (client.readyState === WsWebSocket.OPEN) {
          client.send(data, { binary: isBinary });
        }
      }
    });

    upstream.on('close', () => {
      console.log('Upstream closed; reconnecting in 2s');
      setTimeout(connectUpstream, 2000);
    });

    upstream.on('error', (err) => {
      console.log('Upstream error:', err.message);
      upstream.close();
    });
  };

  connectUpstream();
}