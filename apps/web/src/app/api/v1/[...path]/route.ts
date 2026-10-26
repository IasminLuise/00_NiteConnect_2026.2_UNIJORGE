import { NextRequest, NextResponse } from "next/server";

const DOCUMENT_ID = "[0-9a-fA-F-]{36}";
const VERSION_ID = "[0-9a-fA-F-]{36}";
const ALLOWED_PATHS = [
  new RegExp(`^diaries/me$`),
  new RegExp(`^diaries/me/activities$`),
  new RegExp(`^documents/${DOCUMENT_ID}/editor-sessions$`),
  new RegExp(`^documents/${DOCUMENT_ID}/editor-sessions/${VERSION_ID}/close$`),
  new RegExp(`^documents/${DOCUMENT_ID}/versions/${VERSION_ID}/download$`),
  new RegExp(`^documents/${DOCUMENT_ID}/versions/${VERSION_ID}/pdf$`),
];

async function forward(request: NextRequest, path: string[]) {
  const relativePath = path.join("/");
  if (!ALLOWED_PATHS.some((pattern) => pattern.test(relativePath))) {
    return NextResponse.json({ error: "Rota não disponível pelo cliente web" }, { status: 404, headers: { "Cache-Control": "no-store" } });
  }

  const authorization = request.headers.get("authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return NextResponse.json({ error: "Sessão institucional necessária" }, { status: 401, headers: { "Cache-Control": "no-store" } });
  }

  const configuredBase = process.env.NITE_CONNECT_API_URL;
  if (!configuredBase) {
    return NextResponse.json({ error: "API não configurada" }, { status: 503, headers: { "Cache-Control": "no-store" } });
  }
  const base = new URL(configuredBase);
  const loopback = ["localhost", "127.0.0.1", "[::1]"].includes(base.hostname);
  if ((!loopback && base.protocol !== "https:") || (loopback && !new Set(["http:", "https:"]).has(base.protocol))
    || base.username || base.password || base.search || base.hash) {
    return NextResponse.json({ error: "Configuração da API inválida" }, { status: 503, headers: { "Cache-Control": "no-store" } });
  }

  const destination = new URL(`/api/v1/${relativePath}`, base);
  destination.search = request.nextUrl.search;
  const method = request.method;
  if (method !== "GET" && method !== "POST") {
    return NextResponse.json({ error: "Método não permitido" }, { status: 405, headers: { Allow: "GET, POST", "Cache-Control": "no-store" } });
  }
  const body = method === "POST" ? await request.arrayBuffer() : undefined;
  if (body && body.byteLength > 16 * 1024) {
    return NextResponse.json({ error: "Solicitação excede o limite" }, { status: 413, headers: { "Cache-Control": "no-store" } });
  }

  const contentType = request.headers.get("content-type");
  const response = await fetch(destination, {
    method,
    headers: {
      Authorization: authorization,
      ...(contentType ? { "Content-Type": contentType } : {}),
    },
    body,
    cache: "no-store",
    redirect: "manual",
  });
  const headers = new Headers({ "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" });
  for (const name of ["content-type", "content-disposition", "x-document-version"]) {
    const value = response.headers.get(name);
    if (value) headers.set(name, value);
  }
  return new NextResponse(response.body, { status: response.status, headers });
}

export async function GET(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return forward(request, (await context.params).path);
}

export async function POST(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return forward(request, (await context.params).path);
}
