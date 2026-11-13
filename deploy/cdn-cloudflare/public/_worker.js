// Origem R2 privada; este endpoint publica somente os objetos de distribuicao.
const IMMUTABLE_CACHE = "public, max-age=31536000, immutable";
const MUTABLE_CACHE = "no-store, max-age=0, must-revalidate";
const MAX_OBJECT_BYTES = 550 * 1024 * 1024;
const ALIASES = new Set([
  "SpiritGate-Anfitriao-Windows64.zip",
  "SpiritGate-Convidado-Windows64.zip",
  "SpiritGate-Instalar-Windows64.zip",
  "SpiritGate-Ativar-CDN.zip",
]);
const RESERVED = new Set([
  ".git", ".aws", "data", "saves", "studio-projects", "src", "credentials",
  "credentials.json", "credentials.ini", "id_rsa", "id_ed25519",
]);

function objectRoute(url) {
  // Os nomes da distribuicao sao ASCII; somente espacos precisam de escape.
  if (/%(?!20)/i.test(url.pathname)) return null;
  let key;
  try { key = decodeURIComponent(url.pathname.slice(1)); }
  catch { return null; }
  if (!key || !/^[A-Za-z0-9_. /-]+$/.test(key)) return null;
  for (const part of key.split("/")) {
    if (!part || part === "." || part === ".." || part !== part.replace(/[ .]+$/, "")
        || RESERVED.has(part.toLowerCase()) || part.toLowerCase().startsWith(".env")
        || /^(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])([.]|$)/i.test(part)) return null;
  }
  if (key === "latest.json") return { key, immutable: false };
  const alias = key.endsWith(".sha256") ? key.slice(0, -7) : key;
  if (ALIASES.has(alias)) return { key, immutable: false };
  if (/^release-[a-f0-9]{24}\/.+/.test(key)) return { key, immutable: true };
  if (/^downloads\/[a-f0-9]{64}\/SpiritGate-Atualizavel-[A-Za-z0-9._-]{1,80}(?:-Convidado)?-Windows64\.zip(?:\.sha256)?$/.test(key)) {
    return { key, immutable: true };
  }
  return null;
}

function errorResponse(status, message, extra = {}) {
  return new Response(message, {
    status,
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": MUTABLE_CACHE,
      "X-Content-Type-Options": "nosniff",
      ...extra,
    },
  });
}

function contentType(key) {
  if (key.endsWith(".json")) return "application/json; charset=utf-8";
  if (key.endsWith(".sha256")) return "text/plain; charset=utf-8";
  if (key.endsWith(".zip")) return "application/zip";
  if (key.endsWith(".png")) return "image/png";
  return "application/octet-stream";
}

function headersFor(object, route) {
  if (!Number.isSafeInteger(object.size) || object.size < 0 || object.size > MAX_OBJECT_BYTES) {
    throw new Error("Object size outside distribution limits");
  }
  const headers = new Headers();
  object.writeHttpMetadata?.(headers);
  headers.set("Content-Type", contentType(route.key));
  headers.set("Content-Length", String(object.size));
  headers.set("Cache-Control", route.immutable ? IMMUTABLE_CACHE : MUTABLE_CACHE);
  headers.set("X-Content-Type-Options", "nosniff");
  headers.set("Accept-Ranges", "none");
  if (object.httpEtag) headers.set("ETag", object.httpEtag);
  if (route.key.endsWith(".zip")) {
    headers.set("Content-Disposition", `attachment; filename="${route.key.split("/").at(-1)}"`);
  }
  return headers;
}

function headResponse(response) {
  return new Response(null, { status: response.status, headers: response.headers });
}

export default {
  async fetch(request, env, ctx) {
    const reply = (...args) => request.method === "HEAD" ? headResponse(errorResponse(...args)) : errorResponse(...args);
    if (request.method !== "GET" && request.method !== "HEAD") {
      return reply(405, "Metodo nao permitido.", { Allow: "GET, HEAD" });
    }
    const url = new URL(request.url);
    const route = objectRoute(url);
    if (!route) return reply(404, "Arquivo nao encontrado.");
    // O launcher transfere arquivos inteiros e valida SHA-256. Nao servir bytes
    // parciais com status 200, pois isso poderia envenenar o cache do objeto.
    if (request.headers.has("Range")) return reply(416, "Transferencia parcial indisponivel.");
    const bypass = /(?:no-cache|no-store|max-age\s*=\s*0)/i.test(request.headers.get("Cache-Control") || "")
      || /no-cache/i.test(request.headers.get("Pragma") || "");
    const cache = globalThis.caches?.default;
    const cacheable = route.immutable && !bypass && cache;
    const canonical = new URL(url.origin);
    canonical.pathname = "/" + route.key;
    const cacheKey = new Request(canonical.toString(), { method: "GET" });
    try {
      if (cacheable) {
        let cached;
        try { cached = await cache.match(cacheKey); } catch { /* A origem continua disponivel. */ }
        if (cached) return request.method === "HEAD" ? headResponse(cached) : cached;
      }
      if (!env.RELEASES) return reply(503, "Canal temporariamente indisponivel.");
      const object = request.method === "HEAD"
        ? await env.RELEASES.head(route.key)
        : await env.RELEASES.get(route.key);
      if (object === null) return reply(404, "Arquivo nao encontrado.");
      const response = new Response(request.method === "HEAD" ? null : object.body, {
        headers: headersFor(object, route),
      });
      if (cacheable && request.method === "GET") {
        // Cache API e um acelerador; falhar o cache nunca deve impedir o download.
        const stored = cache.put(cacheKey, response.clone()).catch(() => {});
        ctx?.waitUntil?.(stored);
      }
      return response;
    } catch {
      // Nao devolver paths internos, identificadores de conta ou erros do provedor.
      return reply(502, "Canal temporariamente indisponivel.");
    }
  },
};
