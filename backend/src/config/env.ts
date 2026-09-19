import 'dotenv/config';

const rawPort = process.env.PORT;
const port =
  rawPort === undefined || rawPort === ''
    ? 3000
    : Number.parseInt(rawPort, 10);

if (Number.isNaN(port) || port < 1 || port > 65535) {
  throw new Error(`Invalid PORT: ${rawPort}`);
}

const serverPublicIp = process.env.SERVER_PUBLIC_IP ?? '';
const nameFirst = process.env.SERVER_NAME_FIRST ?? '';
const nameLast = process.env.SERVER_NAME_LAST ?? '';
const tlsKeyPath = process.env.TLS_KEY_PATH ?? '';
const tlsCertPath = process.env.TLS_CERT_PATH ?? '';

export const env = {
  port,
  serverPublicIp,
  nameFirst,
  nameLast,
  tlsKeyPath,
  tlsCertPath,
} as const;
