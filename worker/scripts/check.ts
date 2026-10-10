export {};

import { GUM_API_URL } from "../src/config";
import { gumPositions } from "../src/gum";

interface Ours {
  protocol: "jupiter-perps" | "jupiter-beta";
  address: string;
  side: string;
  asset: string;
  liquidationPrice: number;
  oraclePrice: number;
}

interface Theirs {
  positionPubkey: string;
  liquidationPrice: string;
  markPrice: string;
}

interface GumAccount {
  pubkey: string;
  isolated: boolean;
  market: string | null;
}

interface GumPosition {
  symbol: string;
  isolated: boolean;
  size: number | string;
  liquidationPrice: number | string | null;
}

async function getJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init);
  const text = await response.text();
  if (!response.ok) throw new Error(`${url} HTTP ${response.status}: ${text.slice(0, 200)}`);
  const body = JSON.parse(text) as unknown;
  if (body === null || typeof body !== "object") {
    throw new Error(`${url} returned an error body: ${text.slice(0, 200)}`);
  }
  if (!Array.isArray(body)) {
    const errorBody = body as Record<string, unknown>;
    if (
      "error" in body ||
      errorBody.code === "SERVICE_DEGRADED" ||
      errorBody.status === "error" ||
      errorBody.status === "SERVICE_DEGRADED"
    ) {
      throw new Error(`${url} returned an error body: ${text.slice(0, 200)}`);
    }
  }
  return body as T;
}

function errorPercent(ours: number, reference: number): number {
  return Math.abs(ours - reference) / Math.abs(reference) * 100;
}

const [base, ...wallets] = process.argv.slice(2);
if (!base || wallets.length === 0) throw new Error("usage: npm run check -- <worker url> <wallet> [wallet...]");

let failed = false;
const workerBase = base.replace(/\/+$/, "");

for (const wallet of wallets) {
  let workerAvailable = true;
  let ours: Ours[];
  try {
    const oursResponse = await getJson<{ positions: Ours[] }>(
      `${workerBase}/v1/positions?wallet=${encodeURIComponent(wallet)}`,
    );
    if (!Array.isArray(oursResponse.positions)) throw new Error(`${wallet}: Worker positions response is invalid`);
    ours = oursResponse.positions;
  } catch (error) {
    if (!(error instanceof Error) || !error.message.includes("HTTP 502")) throw error;
    workerAvailable = false;
    failed = true;
    console.log(`${wallet}: Worker endpoint unavailable (${error.message}); checking GUM helper directly`);
    ours = await gumPositions(wallet);
  }

  const jupiterPositions = ours.filter((position) => position.protocol === "jupiter-perps");
  const betaPositions = ours.filter((position) => position.protocol === "jupiter-beta");
  if (workerAvailable) {
    const theirs = await getJson<{ dataList: Theirs[] }>(
      `https://perps-api.jup.ag/v1/positions?walletAddress=${encodeURIComponent(wallet)}`,
    );
    if (!Array.isArray(theirs.dataList)) throw new Error(`${wallet}: Jupiter perps response is invalid`);
    const ref = new Map(theirs.dataList.map((position) => [position.positionPubkey, position]));
    const countMatches = jupiterPositions.length === theirs.dataList.length;
    console.log(
      `${wallet}: jupiter-perps count ours=${jupiterPositions.length} jupiter=${theirs.dataList.length} ${countMatches ? "OK" : "FAIL"}`,
    );
    if (!countMatches) failed = true;

    for (const position of jupiterPositions) {
      const reference = ref.get(position.address);
      if (!reference) {
        console.log(`${position.address}: missing from Jupiter FAIL`);
        failed = true;
        continue;
      }
      const liquidationPrice = Number(reference.liquidationPrice);
      const err = errorPercent(position.liquidationPrice, liquidationPrice);
      const ok = Number.isFinite(err) && err <= 0.5;
      console.log(
        `${position.side.padEnd(5)} ${position.asset.padEnd(4)} ${position.address.slice(0, 8)} ours=${position.liquidationPrice.toFixed(4)} jupiter=${liquidationPrice} err=${err.toFixed(4)}% ${ok ? "OK" : "FAIL"} price ours=${position.oraclePrice.toFixed(4)} jupiter=${reference.markPrice}`,
      );
      if (!ok) failed = true;
    }
  } else {
    console.log(`${wallet}: jupiter-perps comparison unavailable because the local Worker returned HTTP 502`);
  }

  const gumAccountResponse = await getJson<{ exists: boolean; accounts?: GumAccount[] }>(
    `${GUM_API_URL}/tradingAccount`,
    { headers: { "X-PUBKEY": wallet } },
  );
  if (typeof gumAccountResponse.exists !== "boolean") {
    throw new Error(`${wallet}: GUM account response has no exists flag`);
  }
  if (gumAccountResponse.exists && !Array.isArray(gumAccountResponse.accounts)) {
    throw new Error(`${wallet}: GUM accounts response is invalid`);
  }
  const accounts = gumAccountResponse.exists ? gumAccountResponse.accounts ?? [] : [];
  const accountPositions = await Promise.all(accounts.map(async (account) => {
    const positions = await getJson<GumPosition[]>(
      `${GUM_API_URL}/tradingAccount/${encodeURIComponent(account.pubkey)}/positions`,
    );
    if (!Array.isArray(positions)) throw new Error(`${account.pubkey}: GUM positions response is invalid`);
    return { account, positions };
  }));
  const betaRef = new Map<string, GumPosition>();
  for (const { account, positions } of accountPositions) {
    for (const position of positions) {
      const belongsToAccount = account.isolated
        ? position.isolated && position.symbol === account.market
        : !position.isolated;
      if (!belongsToAccount || Number(position.size) === 0 || position.liquidationPrice === null) continue;
      betaRef.set(`${account.pubkey}:${position.symbol}`, position);
    }
  }
  const betaCountMatches = betaPositions.length === betaRef.size;
  console.log(
    `${wallet}: jupiter-beta count ours=${betaPositions.length} GUM=${betaRef.size} ${betaCountMatches ? "OK" : "FAIL"}`,
  );
  if (!betaCountMatches) failed = true;

  for (const position of betaPositions) {
    const reference = betaRef.get(position.address);
    if (!reference) {
      console.log(`${position.address} ${position.asset}: missing from GUM FAIL`);
      failed = true;
      continue;
    }
    const liquidationPrice = Number(reference.liquidationPrice);
    const err = errorPercent(position.liquidationPrice, liquidationPrice);
    const ok = Number.isFinite(err) && err <= 0.5;
    console.log(
      `${position.side.padEnd(5)} ${position.asset.padEnd(7)} ${position.address.slice(0, 8)} ours=${position.liquidationPrice.toFixed(4)} GUM=${liquidationPrice} err=${err.toFixed(4)}% ${ok ? "OK" : "FAIL"}`,
    );
    if (!ok) failed = true;
  }
}

if (failed) process.exitCode = 1;
