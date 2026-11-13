import test from "node:test";
import assert from "node:assert/strict";
import worker from "../public/_worker.js";

const release = "release-" + "a".repeat(24);
const binary = `${release}/app/spirit-gate-rpg.jar`;

function fixture(initial = {}) {
  const files = new Map(Object.entries(initial));
  const calls = { get: [], head: [], match: [], put: [] };
  const entries = new Map();
  const jobs = [];
  const object = (key, body) => {
    const value = files.get(key);
    if (value === undefined) return null;
    const bytes = new TextEncoder().encode(value);
    return {
      size: bytes.byteLength,
      body: body ? bytes : undefined,
      httpEtag: '"etag-' + key.split("/").at(-1) + '"',
      writeHttpMetadata(headers) { headers.set("Cache-Control", "max-age=999"); },
    };
  };
  const env = { RELEASES: {
    async get(key) { calls.get.push(key); return object(key, true); },
    async head(key) { calls.head.push(key); return object(key, false); },
  } };
  globalThis.caches = { default: {
    async match(key) { calls.match.push(key.url); return entries.get(key.url)?.clone(); },
    async put(key, response) { calls.put.push(key.url); entries.set(key.url, response.clone()); },
  } };
  const ctx = { waitUntil(job) { jobs.push(job); } };
  return {
    env, ctx, files, calls,
    async fetch(path, options) { return worker.fetch(new Request("https://spiritgate-updates.pages.dev/" + path, options), env, ctx); },
    async flush() { await Promise.all(jobs); },
  };
}

test("immutable releases are cached under a canonical URL without query fragments", async () => {
  const f = fixture({ [binary]: "verified-game-bytes" });
  const first = await f.fetch(binary + "?noise=1");
  assert.equal(first.status, 200);
  assert.equal(await first.text(), "verified-game-bytes");
  assert.equal(first.headers.get("Content-Type"), "application/octet-stream");
  assert.equal(first.headers.get("Content-Length"), "19");
  assert.equal(first.headers.get("ETag"), '"etag-spirit-gate-rpg.jar"');
  assert.match(first.headers.get("Cache-Control"), /max-age=31536000, immutable/);
  assert.equal(first.headers.get("X-Content-Type-Options"), "nosniff");
  await f.flush();
  const second = await f.fetch(binary + "?noise=2");
  assert.equal(await second.text(), "verified-game-bytes");
  assert.deepEqual(f.calls.get, [binary]);
  assert.equal(f.calls.put.length, 1);
  assert.equal(new URL(f.calls.put[0]).search, "");
});

test("latest and every mutable installer alias always read the current origin", async () => {
  for (const path of ["latest.json", "SpiritGate-Anfitriao-Windows64.zip", "SpiritGate-Convidado-Windows64.zip",
    "SpiritGate-Instalar-Windows64.zip.sha256", "SpiritGate-Ativar-CDN.zip"]) {
    const f = fixture({ [path]: "v1" });
    assert.equal(await (await f.fetch(path)).text(), "v1");
    f.files.set(path, "v2");
    const updated = await f.fetch(path);
    assert.equal(await updated.text(), "v2");
    assert.equal(updated.headers.get("Cache-Control"), "no-store, max-age=0, must-revalidate");
    assert.equal(f.calls.get.length, 2);
    assert.equal(f.calls.match.length, 0);
    assert.equal(f.calls.put.length, 0);
  }
});

test("hashed installer downloads are immutable and return attachment metadata", async () => {
  const path = "downloads/" + "b".repeat(64) + "/SpiritGate-Atualizavel-0.201.0-Convidado-Windows64.zip";
  const f = fixture({ [path]: "zip" });
  const response = await f.fetch(path);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("Content-Type"), "application/zip");
  assert.match(response.headers.get("Content-Disposition"), /Convidado-Windows64\.zip/);
  assert.match(response.headers.get("Cache-Control"), /immutable/);
  await f.flush();
});

test("HEAD uses metadata or a cached response and returns no body", async () => {
  const f = fixture({ [binary]: "game" });
  const uncached = await f.fetch(binary, { method: "HEAD" });
  assert.equal(await uncached.text(), "");
  assert.equal(uncached.headers.get("Content-Length"), "4");
  assert.deepEqual(f.calls.head, [binary]);
  assert.equal(f.calls.get.length, 0);
  assert.equal(f.calls.put.length, 0);
  await f.fetch(binary);
  await f.flush();
  const cached = await f.fetch(binary, { method: "HEAD" });
  assert.equal(await cached.text(), "");
  assert.deepEqual(f.calls.head, [binary]);
  const missing = await f.fetch("latest.json", { method: "HEAD" });
  assert.equal(missing.status, 404);
  assert.equal(await missing.text(), "");
});

test("explicit freshness bypasses immutable cache without warming it", async () => {
  const f = fixture({ [binary]: "game" });
  for (const headers of [{ "Cache-Control": "no-cache" }, { "Cache-Control": "max-age=0" }, { Pragma: "no-cache" }]) {
    assert.equal(await (await f.fetch(binary, { headers })).text(), "game");
  }
  assert.equal(f.calls.get.length, 3);
  assert.equal(f.calls.match.length, 0);
  assert.equal(f.calls.put.length, 0);
});

test("private, malformed and unknown paths never reach R2 or cache", async () => {
  const f = fixture();
  for (const path of ["", ".env", "wrangler.toml", "deployment-plan.json", "arbitrary.zip", `${release}/data/save`,
    `${release}/.aws/config`, `${release}/src/Main.java`, `${release}/file.`, `${release}/CON.txt`,
    `${release}/app%2fsecret`, `${release}/app%5csecret`, `${release}/%252e%252e/private`, `${release}/bad%escape`]) {
    assert.equal((await f.fetch(path)).status, 404, path);
  }
  assert.equal(f.calls.get.length, 0);
  assert.equal(f.calls.match.length, 0);
  const encoded = { method: "GET", headers: new Headers(), url: `https://spiritgate-updates.pages.dev/${release}/%2e%2e/private` };
  assert.equal((await worker.fetch(encoded, f.env, f.ctx)).status, 404);
});

test("ASCII spaces in payload filenames are decoded canonically", async () => {
  const path = `${release}/docs/guia do jogo.txt`;
  const f = fixture({ [path]: "guide" });
  assert.equal(await (await f.fetch(path.replaceAll(" ", "%20"))).text(), "guide");
  assert.deepEqual(f.calls.get, [path]);
  await f.flush();
});

test("uploads and Range requests are refused without origin access", async () => {
  const f = fixture({ [binary]: "game" });
  const upload = await f.fetch(binary, { method: "PUT", body: "malicious" });
  assert.equal(upload.status, 405);
  assert.equal(upload.headers.get("Allow"), "GET, HEAD");
  assert.equal((await f.fetch(binary, { headers: { Range: "bytes=0-2" } })).status, 416);
  assert.equal(f.calls.get.length, 0);
  assert.equal(f.calls.put.length, 0);
});

test("origin faults and invalid object sizes produce generic non-cacheable errors", async () => {
  const f = fixture();
  f.env.RELEASES.get = async () => { throw new Error("PRIVATE-ACCOUNT-SECRET"); };
  const failure = await f.fetch(binary);
  assert.equal(failure.status, 502);
  assert.doesNotMatch(await failure.text(), /PRIVATE|SECRET|ACCOUNT/);
  for (const size of [NaN, -1, 551 * 1024 * 1024]) {
    f.env.RELEASES.get = async () => ({ size, body: "secret" });
    const invalid = await f.fetch(binary);
    assert.equal(invalid.status, 502);
    assert.doesNotMatch(await invalid.text(), /secret/);
  }
  assert.equal(f.calls.put.length, 0);
  assert.equal((await worker.fetch(new Request("https://test.invalid/latest.json"), {}, f.ctx)).status, 503);
});

test("cache errors do not prevent a valid origin download", async () => {
  const f = fixture({ [binary]: "game" });
  globalThis.caches.default.match = async () => { throw new Error("cache unavailable"); };
  globalThis.caches.default.put = async () => { throw new Error("cache unavailable"); };
  const response = await f.fetch(binary);
  assert.equal(response.status, 200);
  assert.equal(await response.text(), "game");
  await f.flush();
});
