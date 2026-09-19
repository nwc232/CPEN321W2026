import express, { type Express } from 'express';
import { env } from './config/env';

export function createApp(): Express {
  const app = express();

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  //Button 1 endpoints
  app.get('/api/server-ip', (_req, res) => {
    res.json({ ip: env.serverPublicIp });
  });

  app.get('/api/server-time', (_req, res) => {
    res.json({ time: formatTimeGmt(new Date()) });
  });

  app.get('/api/name', (_req, res) => {
    res.json({ first: env.nameFirst, last: env.nameLast });
  });
  //

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}

function formatTimeGmt(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');

  const hh = pad(date.getHours());
  const mm = pad(date.getMinutes());
  const ss = pad(date.getSeconds());

  const offsetMin = -date.getTimezoneOffset();
  const sign = offsetMin >= 0 ? '+' : '-';
  const abs = Math.abs(offsetMin);

  return `${hh}:${mm}:${ss} GMT${sign}${pad(Math.floor(abs / 60))}:${pad(abs % 60)}`;
}