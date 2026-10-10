import bs58 from "bs58";
import {
  CODE_VERSION, HELIUS_RPC_URL, JLP_POOL, PERPS_PROGRAM, POOL_META_CACHE_MS, PUBLIC_RPC_URL, WALLET_CACHE_MS,
} from "./config";
import {
  type Custody, decodeAgPrice, decodeCustody, decodePoolCustodies, decodePosition, isPosition, liquidation,
  positionCandidates, toOutput,
} from "./jupiter";

export interface Env { HELIUS_API_KEY?: string }

interface Account { owner: string; data: Uint8Array }

async function getMultipleAccounts(rpc: string, keys: string[]) {
  const res = await fetch(rpc, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      jsonrpc: "2.0", id: 1, method: "getMultipleAccounts",
      params: [keys, { encoding: "base64", commitment: "confirmed" }],
    }),
  });
  if (!res.ok) throw new Error(`rpc ${res.status} ${(await res.text()).slice(0, 200)}`);
  const body = (await res.json()) as {
    error?: { message: string };
    result?: { context: { slot: number }; value: ({ owner: string; data: [string, string] } | null)[] };
  };
  if (body.error || !body.result) throw new Error(`rpc ${body.error?.message ?? "no result"}`);
  const accounts = body.result.value.map((v): Account | null =>
    v ? { owner: v.owner, data: Uint8Array.from(atob(v.data[0]), (c) => c.charCodeAt(0)) } : null,
  );
  return { slot: body.result.context.slot, accounts };
}

let poolMeta: { at: number; custodies: Custody[] } | null = null;
const walletCache = new Map<string, { at: number; body: string }>();

async function loadPoolMeta(rpc: string) {
  if (poolMeta && Date.now() - poolMeta.at < POOL_META_CACHE_MS) return poolMeta;
  const { accounts: [pool] } = await getMultipleAccounts(rpc, [JLP_POOL]);
  if (!pool || pool.owner !== PERPS_PROGRAM) throw new Error("pool account");
  const keys = decodePoolCustodies(pool.data);
  const { accounts } = await getMultipleAccounts(rpc, keys);
  const custodies = accounts.map((a, i) => {
    if (!a || a.owner !== PERPS_PROGRAM) throw new Error("custody account");
    return decodeCustody(keys[i], a.data);
  });
  poolMeta = { at: Date.now(), custodies };
  return poolMeta;
}

async function positionsFor(rpc: string, wallet: string) {
  const meta = await loadPoolMeta(rpc);
  const keys = meta.custodies.map((c) => c.address);
  const n = keys.length;
  const candidates = positionCandidates(wallet, meta.custodies);
  const { slot, accounts } = await getMultipleAccounts(rpc, [
    ...keys, ...meta.custodies.map((c) => c.dovesAgOracle), ...candidates.map((c) => c.address),
  ]);
  const now = BigInt(Math.floor(Date.now() / 1000));
  const byAddress = new Map<string, Custody>();
  accounts.slice(0, n).forEach((a, i) => {
    if (!a || a.owner !== PERPS_PROGRAM) throw new Error("custody account");
    byAddress.set(keys[i], decodeCustody(keys[i], a.data));
  });
  const prices = new Map(
    accounts.slice(n, 2 * n).map((a, i) => {
      if (!a) throw new Error("price account");
      return [keys[i], decodeAgPrice(a.data)] as const;
    }),
  );
  const positions = accounts.slice(2 * n).flatMap((a, i) => {
    if (!a || a.owner !== PERPS_PROGRAM || !isPosition(a.data)) return [];
    const p = decodePosition(candidates[i].address, a.data);
    if (p.sizeUsd === 0n || p.owner !== wallet) return [];
    const custody = byAddress.get(p.custody);
    const collateral = byAddress.get(p.collateralCustody);
    const price = prices.get(p.custody);
    if (!custody || !collateral || !price) throw new Error("unknown custody");
    return [toOutput(p, custody, liquidation(p, custody, collateral, now), price)];
  });
  return { slot, serverTime: new Date().toISOString(), codeVersion: CODE_VERSION, positions };
}

const json = (status: number, body: unknown) =>
  new Response(typeof body === "string" ? body : JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json", "cache-control": "no-store" },
  });

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (url.pathname !== "/v1/positions") return json(404, { error: "not found" });
    const wallet = url.searchParams.get("wallet") ?? "";
    let valid = false;
    try { valid = bs58.decode(wallet).length === 32; } catch { valid = false; }
    if (!valid) return json(400, { error: "invalid wallet" });

    const cached = walletCache.get(wallet);
    if (cached && Date.now() - cached.at < WALLET_CACHE_MS) return json(200, cached.body);

    const rpc = env.HELIUS_API_KEY ? HELIUS_RPC_URL + env.HELIUS_API_KEY : PUBLIC_RPC_URL;
    try {
      const body = JSON.stringify(await positionsFor(rpc, wallet));
      for (const [k, v] of walletCache) if (Date.now() - v.at >= WALLET_CACHE_MS) walletCache.delete(k);
      walletCache.set(wallet, { at: Date.now(), body });
      return json(200, body);
    } catch (e) {
      return json(502, { error: "could not check", detail: (e as Error).message });
    }
  },
};
