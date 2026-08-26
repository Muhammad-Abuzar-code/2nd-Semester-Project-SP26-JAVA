import csv
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
CSV = ROOT / "data" / "benchmark_results.csv"
OUT = ROOT / "plots"
OUT.mkdir(parents=True, exist_ok=True)

plt.rcParams.update({
    "figure.dpi": 150,
    "font.size": 10,
    "axes.spines.top": False,
    "axes.spines.right": False,
    "axes.grid": True,
    "grid.alpha": 0.3,
    "grid.linestyle": "--",
})

RAZ_COLOR = "#e4572e"
ZIP_COLOR = "#2e86ab"

rows = []
with open(CSV) as f:
    for r in csv.DictReader(f):
        rows.append({
            "name": r["dataset"],
            "orig": int(r["original_bytes"]),
            "raz": int(r["raz_bytes"]),
            "zip": int(r["zip_bytes"]),
            "winner": r["winner_algo"],
            "raz_c": float(r["raz_compress_ms"]),
            "zip_c": float(r["zip_compress_ms"]),
            "raz_d": float(r["raz_decompress_ms"]),
            "zip_d": float(r["zip_decompress_ms"]),
        })

names = [r["name"] for r in rows]
short = [n.split(".")[0] for n in names]
x = np.arange(len(rows))
w = 0.38


def fmt_kb(v):
    return f"{v/1024:,.0f}" if v >= 1024 * 10 else f"{v/1024:.1f}"


# ---- derived stats for RESULTS.md ----
print(f"{'dataset':<18}{'raz_ratio%':>11}{'zip_ratio%':>11}{'raz/zip':>9}{'speedup':>9}")
tot_o = tot_r = tot_z = 0
geo = 0.0
for r in rows:
    rr = (1 - r["raz"] / r["orig"]) * 100
    zr = (1 - r["zip"] / r["orig"]) * 100
    ratio = r["raz"] / r["zip"]
    geo += np.log(ratio)
    tot_o += r["orig"]; tot_r += r["raz"]; tot_z += r["zip"]
    print(f"{r['name']:<18}{rr:>11.2f}{zr:>11.2f}{ratio:>9.3f}{r['raz_c']/r['zip_c']:>8.1f}x")
agg_r = (1 - tot_r / tot_o) * 100
agg_z = (1 - tot_z / tot_o) * 100
print(f"\nAGGREGATE orig={tot_o:,} raz={tot_r:,} zip={tot_z:,}")
print(f"aggregate ratio raz={agg_r:.2f}% zip={agg_z:.2f}%")
print(f"geomean raz/zip size factor={np.exp(geo/len(rows)):.3f}")

# ---- Plot 1: compressed sizes (log scale) ----
fig, ax = plt.subplots(figsize=(9, 4.8))
ax.bar(x - w / 2, [r["raz"] / 1024 for r in rows], w, label="RAZ (.raz)", color=RAZ_COLOR)
ax.bar(x + w / 2, [r["zip"] / 1024 for r in rows], w, label="ZIP DEFLATE (.zip)", color=ZIP_COLOR)
ax.set_yscale("log")
ax.set_xticks(x, short)
ax.set_ylabel("Compressed size (KB, log scale)")
ax.set_title("Compressed size per dataset — RAZ vs ZIP DEFLATE")
for i, r in enumerate(rows):
    ax.text(i - w / 2, r["raz"] / 1024 * 1.06, f'{r["raz"]/1024:.1f}', ha="center", va="bottom", fontsize=7)
    ax.text(i + w / 2, r["zip"] / 1024 * 1.06, f'{r["zip"]/1024:.1f}', ha="center", va="bottom", fontsize=7)
ax.legend()
fig.tight_layout()
fig.savefig(OUT / "plot_compressed_sizes.png")

# ---- Plot 2: compression ratio ----
fig, ax = plt.subplots(figsize=(9, 4.8))
raz_ratio = [(1 - r["raz"] / r["orig"]) * 100 for r in rows]
zip_ratio = [(1 - r["zip"] / r["orig"]) * 100 for r in rows]
ax.bar(x - w / 2, raz_ratio, w, label="RAZ", color=RAZ_COLOR)
ax.bar(x + w / 2, zip_ratio, w, label="ZIP DEFLATE", color=ZIP_COLOR)
ax.axhline(0, color="black", lw=0.8)
ax.set_xticks(x, short)
ax.set_ylabel("Space saved (%)")
ax.set_ylim(-10, 108)
ax.set_title("Compression ratio — higher is better")
for i, (a, b) in enumerate(zip(raz_ratio, zip_ratio)):
    ax.text(i - w / 2, a + 1.5 if a >= 0 else a - 0.8, f"{a:.2f}", ha="center",
            va="bottom" if a >= 0 else "top", fontsize=7)
    ax.text(i + w / 2, b + 1.5 if b >= 0 else b - 0.8, f"{b:.2f}", ha="center",
            va="bottom" if b >= 0 else "top", fontsize=7)
ax.legend(loc="upper center", ncol=2)
fig.tight_layout()
fig.savefig(OUT / "plot_compression_ratio.png")

# ---- Plot 3: relative size RAZ vs ZIP ----
fig, ax = plt.subplots(figsize=(9, 4.2))
rel = [r["raz"] / r["zip"] * 100 for r in rows]
colors = [RAZ_COLOR if v > 100 else "#3aa35c" for v in rel]
bars = ax.barh(np.arange(len(rows)), rel, color=colors)
ax.axvline(100, color="black", lw=1, ls="--", label="parity with ZIP (100%)")
ax.set_yticks(np.arange(len(rows)), short)
ax.invert_yaxis()
ax.set_xlabel("RAZ archive size as % of ZIP archive size")
ax.set_title("RAZ size relative to ZIP (bars < 100% mean RAZ is smaller)")
for i, v in enumerate(rel):
    ax.text(v + 2, i, f"{v:.0f}%", va="center", fontsize=8)
ax.set_xlim(0, max(rel) * 1.25)
ax.legend(loc="lower right")
fig.tight_layout()
fig.savefig(OUT / "plot_relative_size.png")

# ---- Plot 4 & 5: timings ----
fig, axes = plt.subplots(1, 2, figsize=(11, 4.4))
for ax, key_r, key_z, title in (
    (axes[0], "raz_c", "zip_c", "Compression time (ms, median of 5)"),
    (axes[1], "raz_d", "zip_d", "Decompression time (ms, median of 5)"),
):
    vr = [r[key_r] for r in rows]
    vz = [r[key_z] for r in rows]
    ax.bar(x - w / 2, vr, w, label="RAZ", color=RAZ_COLOR)
    ax.bar(x + w / 2, vz, w, label="ZIP DEFLATE", color=ZIP_COLOR)
    ax.set_yscale("log")
    ax.set_xticks(x, short, rotation=30, ha="right")
    ax.set_ylabel("Milliseconds (log scale)")
    ax.set_title(title)
    for i, (a, b) in enumerate(zip(vr, vz)):
        ax.text(i - w / 2, a, f"{a:.1f}", ha="center", va="bottom", fontsize=6.5)
        ax.text(i + w / 2, b, f"{b:.2f}", ha="center", va="bottom", fontsize=6.5)
axes[0].legend()
fig.tight_layout()
fig.savefig(OUT / "plot_timings.png")

print("\nPlots written to", OUT)
