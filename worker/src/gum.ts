import { GUM_API_URL, PERPS_LINK } from "./config";
import type { PositionOut, Side } from "./jupiter";

interface TradingAccount {
  pubkey: string;
  isolated: boolean;
  market: string | null;
  data?: {
    accountValue?: number | string | null;
    totalLiquidationRequirement?: number | string | null;
  } | null;
}

interface TradingAccountResponse {
  exists: boolean;
  accounts?: TradingAccount[];
}

interface GumPosition {
  symbol: string;
  side: "Bid" | "Ask";
  size: number | string;
  avgEntryPrice: number | string;
  markPrice: number | string;
  liquidationPrice: number | string | null;
  maintenanceMarginRatioBps: number | string;
  isolated: boolean;
  collateral?: number | string | null;
}

interface GumMarket {
  symbol: string;
  takerFeeBps: number | string;
  liquidationFeeBps: number | string;
  timestamp: number | string;
}

function errorMessage(value: unknown): string {
  if (typeof value === "string") return value;
  try { return JSON.stringify(value); } catch { return "unknown error"; }
}

async function getJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${GUM_API_URL}${path}`, init);
  const text = await response.text();
  if (!response.ok) throw new Error(`GUM ${path} HTTP ${response.status}: ${text.slice(0, 200)}`);

  let value: unknown;
  try {
    value = JSON.parse(text);
  } catch {
    throw new Error(`GUM ${path} returned invalid JSON`);
  }
  if (typeof value === "string") throw new Error(`GUM ${path}: ${value}`);
  if (value === null || typeof value !== "object") throw new Error(`GUM ${path} returned an invalid body`);
  if (!Array.isArray(value)) {
    const body = value as Record<string, unknown>;
    if (
      "error" in body ||
      body.code === "SERVICE_DEGRADED" ||
      body.status === "error" ||
      body.status === "SERVICE_DEGRADED"
    ) {
      throw new Error(`GUM ${path}: ${errorMessage(body.error ?? body.message ?? body.code ?? body.status)}`);
    }
  }
  return value as T;
}

function number(value: unknown, label: string): number {
  if (typeof value !== "number" && typeof value !== "string") throw new Error(`GUM ${label} is missing`);
  if (typeof value === "string" && value.trim() === "") throw new Error(`GUM ${label} is missing`);
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) throw new Error(`GUM ${label} is not finite`);
  return parsed;
}

function marketFor(position: GumPosition, markets: Map<string, GumMarket>): GumMarket {
  const market = markets.get(position.symbol);
  if (!market) throw new Error(`GUM unknown market ${position.symbol}`);
  const timestamp = number(market.timestamp, `${position.symbol} market timestamp`);
  if (Date.now() / 1000 - timestamp > 60) throw new Error(`GUM ${position.symbol} market is stale`);
  return market;
}

function betaPosition(
  account: TradingAccount,
  position: GumPosition,
  market: GumMarket,
): PositionOut | null {
  const size = number(position.size, `${position.symbol} size`);
  if (size < 0) throw new Error(`GUM ${position.symbol} size is negative`);
  if (size === 0 || position.liquidationPrice === null) return null;
  if (position.side !== "Bid" && position.side !== "Ask") {
    throw new Error(`GUM ${position.symbol} side is invalid`);
  }

  const entryPrice = number(position.avgEntryPrice, `${position.symbol} entry price`);
  const markPrice = number(position.markPrice, `${position.symbol} mark price`);
  const liquidationPrice = number(position.liquidationPrice, `${position.symbol} liquidation price`);
  const maintenanceMarginRatioBps = number(
    position.maintenanceMarginRatioBps,
    `${position.symbol} maintenance margin ratio`,
  );
  const takerFeeBps = number(market.takerFeeBps, `${position.symbol} taker fee`);
  const liquidationFeeBps = number(market.liquidationFeeBps, `${position.symbol} liquidation fee`);
  if (entryPrice <= 0 || markPrice <= 0 || liquidationPrice <= 0) {
    throw new Error(`GUM ${position.symbol} prices must be positive`);
  }

  if (!account.data) throw new Error(`GUM ${account.pubkey} account data is missing`);
  const accountValue = number(account.data.accountValue, `${account.pubkey} account value`);
  const totalLiquidationRequirement = number(
    account.data.totalLiquidationRequirement,
    `${account.pubkey} liquidation requirement`,
  );
  if (accountValue <= 0) throw new Error(`GUM ${account.pubkey} account value is not positive`);

  const sizeUsd = size * markPrice;
  const ratio =
    maintenanceMarginRatioBps / 10_000 +
    (takerFeeBps + liquidationFeeBps) / 10_000;
  if (ratio < 0 || ratio >= 1) throw new Error(`GUM ${position.symbol} margin ratio is invalid`);
  const other = totalLiquidationRequirement - sizeUsd * ratio;
  const side: Side = position.side === "Bid" ? "long" : "short";
  const calculatedLiquidationPrice = side === "long"
    ? (other - accountValue + markPrice * size) / (size * (1 - ratio))
    : (accountValue - other + markPrice * size) / (size * (1 + ratio));
  const relativeError = Math.abs(calculatedLiquidationPrice - liquidationPrice) / liquidationPrice;
  if (!Number.isFinite(sizeUsd) || !Number.isFinite(ratio) || !Number.isFinite(other)) {
    throw new Error(`GUM ${position.symbol} calculation is not finite`);
  }
  if (!Number.isFinite(calculatedLiquidationPrice) || relativeError > 0.005) {
    throw new Error(
      `GUM ${position.symbol} liquidation mismatch ${calculatedLiquidationPrice} vs ${liquidationPrice}`,
    );
  }

  const closeFeeUsd = sizeUsd * takerFeeBps / 10_000;
  const priceTime = number(market.timestamp, `${position.symbol} market timestamp`);
  if (!Number.isFinite(closeFeeUsd)) throw new Error(`GUM ${position.symbol} close fee is not finite`);

  let collateralUsd: number;
  if (account.isolated) {
    collateralUsd = number(position.collateral, `${position.symbol} collateral`);
    if (collateralUsd <= 0) throw new Error(`GUM ${position.symbol} collateral is not positive`);
  } else {
    collateralUsd = accountValue;
  }

  return {
    protocol: "jupiter-beta",
    address: `${account.pubkey}:${position.symbol}`,
    asset: position.symbol,
    side,
    entryPrice,
    sizeUsd,
    collateralUsd,
    closeFeeUsd,
    borrowFeeUsd: 0,
    oraclePrice: markPrice,
    priceTime,
    priceSource: "protocol",
    liquidationPrice,
    link: PERPS_LINK,
  };
}

export async function gumPositions(wallet: string): Promise<PositionOut[]> {
  const [markets, accountResponse] = await Promise.all([
    getJson<GumMarket[]>("/markets"),
    getJson<TradingAccountResponse>("/tradingAccount", {
      headers: { "X-PUBKEY": wallet },
    }),
  ]);
  if (!Array.isArray(markets)) throw new Error("GUM markets response is not an array");
  if (typeof accountResponse.exists !== "boolean") throw new Error("GUM exists flag is missing");
  if (!accountResponse.exists) return [];
  if (!Array.isArray(accountResponse.accounts)) throw new Error("GUM accounts response is not an array");

  const marketsBySymbol = new Map(markets.map((market) => [market.symbol, market]));
  const accounts = accountResponse.accounts;
  const positions = await Promise.all(accounts.map(async (account) => {
    if (!account.pubkey || typeof account.pubkey !== "string" || typeof account.isolated !== "boolean") {
      throw new Error("GUM trading account is invalid");
    }
    if (account.isolated && typeof account.market !== "string") {
      throw new Error(`GUM ${account.pubkey} market is missing`);
    }
    const path = `/tradingAccount/${encodeURIComponent(account.pubkey)}/positions`;
    const accountPositions = await getJson<GumPosition[]>(path);
    if (!Array.isArray(accountPositions)) throw new Error(`GUM ${path} response is not an array`);

    const relevant = accountPositions.filter((position) => {
      if (typeof position.isolated !== "boolean") throw new Error(`GUM ${account.pubkey} position isolation is missing`);
      return account.isolated
        ? position.isolated && position.symbol === account.market
        : !position.isolated;
    });
    return relevant.flatMap((position) => {
      const market = marketFor(position, marketsBySymbol);
      const output = betaPosition(account, position, market);
      return output ? [output] : [];
    });
  }));
  return positions.flat();
}
