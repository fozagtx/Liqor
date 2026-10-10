// Port of the liquidation maths in github.com/julianfssen/jupiter-perps-anchor-idl-parsing
// (get-liquidation-price.ts, get-borrow-fee-and-funding-rate.ts), with borrow interest
// accrued to now from the collateral custody and the linear price-impact term in the close fee.
import { ed25519 } from "@noble/curves/ed25519";
import { sha256 } from "@noble/hashes/sha256";
import bs58 from "bs58";
import { JLP_POOL, PERPS_LINK, PERPS_PROGRAM, SYMBOLS } from "./config";

const BPS_POWER = 10_000n;
const DBPS_POWER = 100_000n;
const RATE_POWER = 1_000_000_000n;
const DEBT_POWER = RATE_POWER;
const HOURS_IN_A_YEAR = 8760n;
const USD_DECIMALS = 1e6;

export type Side = "long" | "short";

export interface Custody {
  address: string;
  mint: string;
  isStable: boolean;
  tradeImpactFeeScalar: bigint;
  maxLeverage: bigint;
  owned: bigint;
  locked: bigint;
  cumulativeInterestRate: bigint;
  lastUpdate: bigint;
  hourlyFundingDbps: bigint;
  decreasePositionBps: bigint;
  jumpMinRateBps: bigint;
  jumpMaxRateBps: bigint;
  jumpTargetRateBps: bigint;
  jumpTargetUtilizationRate: bigint;
  dovesAgOracle: string;
  debt: bigint;
  borrowLendInterestsAccured: bigint;
}

export interface Position {
  address: string;
  owner: string;
  custody: string;
  collateralCustody: string;
  side: Side;
  price: bigint;
  sizeUsd: bigint;
  collateralUsd: bigint;
  cumulativeInterestSnapshot: bigint;
}

export interface OraclePrice {
  price: number;
  timestamp: number;
}

class Reader {
  private view: DataView;
  constructor(private bytes: Uint8Array) {
    this.view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  }
  u8(o: number) { return this.bytes[o]; }
  i8(o: number) { return this.view.getInt8(o); }
  u32(o: number) { return this.view.getUint32(o, true); }
  u64(o: number) { return this.view.getBigUint64(o, true); }
  i64(o: number) { return this.view.getBigInt64(o, true); }
  u128(o: number) { return this.u64(o) | (this.u64(o + 8) << 64n); }
  key(o: number) { return bs58.encode(this.bytes.subarray(o, o + 32)); }
}

// Offsets from the perps program's on-chain Anchor IDL (8-byte discriminator first).
export function decodePoolCustodies(data: Uint8Array): string[] {
  const r = new Reader(data);
  const nameLen = r.u32(8);
  const vecAt = 12 + nameLen;
  const count = r.u32(vecAt);
  return Array.from({ length: count }, (_, i) => r.key(vecAt + 4 + i * 32));
}

export function decodeCustody(address: string, data: Uint8Array): Custody {
  const r = new Reader(data);
  return {
    address,
    mint: r.key(40),
    isStable: r.u8(105) === 1,
    tradeImpactFeeScalar: r.u64(151),
    maxLeverage: r.u64(175),
    owned: r.u64(222),
    locked: r.u64(230),
    cumulativeInterestRate: r.u128(262),
    lastUpdate: r.i64(278),
    hourlyFundingDbps: r.u64(286),
    decreasePositionBps: r.u64(304),
    jumpMinRateBps: r.u64(352),
    jumpMaxRateBps: r.u64(360),
    jumpTargetRateBps: r.u64(368),
    jumpTargetUtilizationRate: r.u64(376),
    dovesAgOracle: r.key(384),
    debt: r.u128(1004),
    borrowLendInterestsAccured: r.u128(1020),
  };
}

const POSITION_DISCRIMINATOR = sha256(new TextEncoder().encode("account:Position")).subarray(0, 8);
const POSITION_MIN_SIZE = 210;

export function isPosition(data: Uint8Array): boolean {
  return data.length >= POSITION_MIN_SIZE && POSITION_DISCRIMINATOR.every((b, i) => data[i] === b);
}

export function decodePosition(address: string, data: Uint8Array): Position {
  const r = new Reader(data);
  const side = r.u8(152);
  if (side !== 1 && side !== 2) throw new Error("position side");
  return {
    address,
    owner: r.key(8),
    custody: r.key(72),
    collateralCustody: r.key(104),
    side: side === 1 ? "long" : "short",
    price: r.u64(153),
    sizeUsd: r.u64(161),
    collateralUsd: r.u64(169),
    cumulativeInterestSnapshot: r.u128(185),
  };
}

// Doves AgPriceFeed: mint, edge_feed, cl_feed, pyth_feed, pyth_feed_id, price u64, expo i8, timestamp i64.
export function decodeAgPrice(data: Uint8Array): OraclePrice {
  const r = new Reader(data);
  return { price: Number(r.u64(168)) * 10 ** r.i8(176), timestamp: Number(r.i64(177)) };
}

const PDA_MARKER = new TextEncoder().encode("ProgramDerivedAddress");

function isOnCurve(bytes: Uint8Array): boolean {
  try {
    ed25519.ExtendedPoint.fromHex(bytes);
    return true;
  } catch {
    return false;
  }
}

function findProgramAddress(seeds: Uint8Array[], programId: Uint8Array): string {
  for (let bump = 255; bump >= 0; bump--) {
    const hash = sha256(concat([...seeds, Uint8Array.of(bump), programId, PDA_MARKER]));
    if (!isOnCurve(hash)) return bs58.encode(hash);
  }
  throw new Error("no viable bump");
}

function concat(parts: Uint8Array[]): Uint8Array {
  const out = new Uint8Array(parts.reduce((n, p) => n + p.length, 0));
  let o = 0;
  for (const p of parts) { out.set(p, o); o += p.length; }
  return out;
}

export interface Candidate { address: string; custody: string; collateralCustody: string; side: Side }

// One long per asset (collateral = the asset) and one short per asset per stablecoin custody.
export function positionCandidates(wallet: string, custodies: Pick<Custody, "address" | "isStable">[]): Candidate[] {
  const program = bs58.decode(PERPS_PROGRAM);
  const owner = bs58.decode(wallet);
  const pool = bs58.decode(JLP_POOL);
  const seed = new TextEncoder().encode("position");
  const derive = (custody: string, collateral: string, side: Side): Candidate => ({
    address: findProgramAddress(
      [seed, owner, pool, bs58.decode(custody), bs58.decode(collateral), Uint8Array.of(side === "long" ? 1 : 2)],
      program,
    ),
    custody,
    collateralCustody: collateral,
    side,
  });
  const assets = custodies.filter((c) => !c.isStable);
  const stables = custodies.filter((c) => c.isStable);
  return assets.flatMap((a) => [
    derive(a.address, a.address, "long"),
    ...stables.map((s) => derive(a.address, s.address, "short")),
  ]);
}

const max = (a: bigint, b: bigint) => (a > b ? a : b);
const abs = (a: bigint) => (a < 0n ? -a : a);

function divCeil(a: bigint, b: bigint): bigint {
  const q = a / b;
  if (a % b === 0n) return q;
  return q < 0n ? q - 1n : q + 1n;
}

function getDebt(c: Custody) {
  return divCeil(max(c.debt - c.borrowLendInterestsAccured, 0n), DEBT_POWER);
}

function hourlyBorrowRate(c: Custody): bigint {
  const owned = c.owned + getDebt(c);
  const locked = c.locked + getDebt(c);
  if (c.hourlyFundingDbps !== 0n) {
    const hourly = (c.hourlyFundingDbps * RATE_POWER) / DBPS_POWER;
    return owned > 0n && locked > 0n ? divCeil(locked * hourly, owned) : 0n;
  }
  const util = owned > 0n && locked > 0n ? (locked * RATE_POWER) / owned : 0n;
  let yearly: bigint;
  if (util <= c.jumpTargetUtilizationRate) {
    yearly =
      ((divCeil((c.jumpTargetRateBps - c.jumpMinRateBps) * util, c.jumpTargetUtilizationRate) + c.jumpMinRateBps) *
        RATE_POWER) /
      BPS_POWER;
  } else {
    const rateDiff = max(0n, c.jumpMaxRateBps - c.jumpTargetRateBps);
    const utilDiff = max(0n, util - c.jumpTargetUtilizationRate);
    const denom = max(0n, RATE_POWER - c.jumpTargetUtilizationRate);
    if (denom === 0n) throw new Error("borrow rate denominator is 0");
    yearly = ((divCeil(rateDiff * utilDiff, denom) + c.jumpTargetRateBps) * RATE_POWER) / BPS_POWER;
  }
  return yearly / HOURS_IN_A_YEAR;
}

function cumulativeInterestNow(c: Custody, now: bigint): bigint {
  if (now <= c.lastUpdate || c.owned === 0n) return c.cumulativeInterestRate;
  return c.cumulativeInterestRate + divCeil(hourlyBorrowRate(c) * (now - c.lastUpdate), 3600n);
}

export interface Liquidation { liquidationPrice: bigint; closeFeeUsd: bigint; borrowFeeUsd: bigint }

export function liquidation(p: Position, custody: Custody, collateral: Custody, now: bigint): Liquidation {
  const impactBps =
    custody.tradeImpactFeeScalar === 0n ? 0n : divCeil(p.sizeUsd * BPS_POWER, custody.tradeImpactFeeScalar);
  const closeFeeUsd = (p.sizeUsd * (custody.decreasePositionBps + impactBps)) / BPS_POWER;
  const borrowFeeUsd =
    ((cumulativeInterestNow(collateral, now) - p.cumulativeInterestSnapshot) * p.sizeUsd) / RATE_POWER;
  const maxLossUsd = (p.sizeUsd * BPS_POWER) / custody.maxLeverage + closeFeeUsd + borrowFeeUsd;
  const margin = p.collateralUsd;
  const diff = (abs(maxLossUsd - margin) * p.price) / p.sizeUsd;
  const lossExceedsMargin = maxLossUsd > margin;
  const liquidationPrice =
    p.side === "long"
      ? lossExceedsMargin ? p.price + diff : p.price - diff
      : lossExceedsMargin ? p.price - diff : p.price + diff;
  return { liquidationPrice, closeFeeUsd, borrowFeeUsd };
}

export interface PositionOut {
  protocol: "jupiter-perps";
  address: string;
  asset: string;
  side: Side;
  entryPrice: number;
  sizeUsd: number;
  collateralUsd: number;
  closeFeeUsd: number;
  borrowFeeUsd: number;
  oraclePrice: number;
  priceTime: number;
  priceSource: "protocol";
  liquidationPrice: number;
  link: string;
}

const usd = (v: bigint) => Number(v) / USD_DECIMALS;

export function toOutput(p: Position, custody: Custody, liq: Liquidation, price: OraclePrice): PositionOut {
  return {
    protocol: "jupiter-perps",
    address: p.address,
    asset: SYMBOLS[custody.mint] ?? custody.mint.slice(0, 4),
    side: p.side,
    entryPrice: usd(p.price),
    sizeUsd: usd(p.sizeUsd),
    collateralUsd: usd(p.collateralUsd),
    closeFeeUsd: usd(liq.closeFeeUsd),
    borrowFeeUsd: usd(liq.borrowFeeUsd),
    oraclePrice: price.price,
    priceTime: price.timestamp,
    priceSource: "protocol",
    liquidationPrice: usd(liq.liquidationPrice),
    link: PERPS_LINK,
  };
}
