export {};

// Compares the Worker's liquidation prices with Jupiter's perps-api for the given wallets.
// Usage: npm run check -- <worker base url> <wallet> [wallet...]
const [base, ...wallets] = process.argv.slice(2);
if (!base || wallets.length === 0) throw new Error("usage: npm run check -- <worker url> <wallet> [wallet...]");

interface Ours { address: string; side: string; asset: string; liquidationPrice: number; oraclePrice: number }
interface Theirs { positionPubkey: string; liquidationPrice: string; markPrice: string }

for (const wallet of wallets) {
  const ours = (await (await fetch(`${base}/v1/positions?wallet=${wallet}`)).json()) as { positions: Ours[] };
  const theirs = (await (await fetch(`https://perps-api.jup.ag/v1/positions?walletAddress=${wallet}`)).json()) as { dataList: Theirs[] };
  const ref = new Map(theirs.dataList.map((t) => [t.positionPubkey, t]));
  if (ours.positions.length !== theirs.dataList.length)
    console.log(`${wallet}: MISMATCH count ours=${ours.positions.length} jupiter=${theirs.dataList.length}`);
  for (const p of ours.positions) {
    const t = ref.get(p.address);
    if (!t) { console.log(`${p.address}: missing from Jupiter`); continue; }
    const j = Number(t.liquidationPrice);
    const err = ((p.liquidationPrice - j) / j) * 100;
    console.log(
      `${p.side.padEnd(5)} ${p.asset.padEnd(4)} ${p.address.slice(0, 8)} ours=${p.liquidationPrice.toFixed(4)} jupiter=${j} err=${err.toFixed(4)}% ${Math.abs(err) <= 0.5 ? "OK" : "FAIL"} price ours=${p.oraclePrice.toFixed(4)} jupiter=${t.markPrice}`,
    );
  }
}
