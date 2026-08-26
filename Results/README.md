# RESULTS — RAZ Archiver vs. Standard ZIP Compression

This report compares the **RAZ Archiver** adaptive multi-algorithm pipeline (RLE, LZW, Huffman, LZW+Huffman — best result wins) against the **ZIP DEFLATE** method used by well-known zip tools (`zip`, WinRAR/7-Zip "zip" mode, `ZipOutputStream`).

> **TL;DR** — On this benchmark ZIP DEFLATE produces **~12% smaller archives overall** (and up to **3.8× smaller** on natural-language text), while RAZ **wins on highly repetitive data** (6.4% smaller than ZIP) and matches ZIP on incompressible data. ZIP is also **2–25× faster to compress** and **4–187× faster to decompress**. Both compressors restored every file **byte-identically** (CRC-verified round trip).

---

## 1. Test Environment

| Item | Value |
|---|---|
| CPU | Intel Core i7-1255U (12th Gen, 12 threads) |
| OS | Ubuntu 26.04 LTS on WSL2 (Windows host) |
| Java | OpenJDK Temurin 21.0.12+1 |
| RAZ build | Project sources, compiled with `javac` (no optimization flags) |
| ZIP reference | `java.util.zip` DEFLATE (default level 6 — same as the `zip` CLI default), single-entry `.zip` archive |
| Timing | Median of **5 runs** after **1 warm-up run** (JIT), measured end-to-end including file I/O |
| Integrity | Every RAZ and ZIP archive was decompressed and compared byte-for-byte with the original — **all round trips passed** |

## 2. Test Corpus

Six deterministic, seeded files (~4.8 MB total) covering the typical workload spectrum:

| Dataset | Size | Description |
|---|---|---|
| `en_text.txt` | 976.6 KB | Natural-language prose (repeating sentence bank with variation) |
| `source_code.txt` | 781.3 KB | Structured Java-like source code |
| `data.csv` | 586.0 KB | Numeric sensor readings (high-entropy columns) |
| `app.log` | 879.0 KB | Application log lines (timestamps + templated messages) |
| `repetitive.dat` | 976.6 KB | Long runs of repeated bytes (RLE-friendly synthetic data) |
| `random.bin` | 500.0 KB | Uniform random bytes (theoretically incompressible) |

The generator is embedded in `BenchmarkRunner.java` (fixed seed `42`), so the corpus is exactly reproducible.

---

## 3. Archive Sizes

**Table 1 — Final archive size on disk** (RAZ size includes the self-describing `.raz` header: magic number, algorithm, extension, original size, CRC32):

| Dataset | Original (bytes) | RAZ `.raz` (bytes) | ZIP `.zip` (bytes) | RAZ (KB) | ZIP (KB) |
|---|---:|---:|---:|---:|---:|
| en_text.txt | 1,000,009 | 70,138 | **18,364** | 68.5 | 17.9 |
| source_code.txt | 800,066 | 59,486 | **35,429** | 58.1 | 34.6 |
| data.csv | 600,026 | 160,829 | **140,518** | 157.1 | 137.2 |
| app.log | 900,060 | 125,395 | **108,654** | 122.5 | 106.1 |
| repetitive.dat | 1,000,047 | **10,355** | 11,068 | 10.1 | 10.8 |
| random.bin | 512,000 | 512,807 | 512,294 | 500.8 | 500.3 |
| **Total** | **4,812,208** | **939,010** | **826,327** | **917.0** | **807.0** |

![Compressed sizes](Results/plots/plot_compressed_sizes.png)

**Table 2 — Space saved and relative archive size** (ratio = 1 − compressed/original; `RAZ/ZIP` > 1 means the RAZ archive is larger):

| Dataset | RAZ space saved | ZIP space saved | RAZ size vs ZIP | RAZ winning algorithm |
|---|---:|---:|---:|---|
| en_text.txt | 92.99% | **98.16%** | 3.82× | LZW+Huffman |
| source_code.txt | 92.56% | **95.57%** | 1.68× | LZW+Huffman |
| data.csv | 73.20% | **76.58%** | 1.14× | LZW+Huffman |
| app.log | 86.07% | **87.93%** | 1.15× | LZW |
| repetitive.dat | **98.96%** | 98.89% | **0.94×** | LZW+Huffman |
| random.bin | −0.16% | −0.06% | 1.00× | Huffman |
| **Aggregate** | **80.49%** | **82.83%** | **1.14×** | Adaptive (per-file) |

![Compression ratio](Results/plots/plot_compression_ratio.png)

![Relative size](Results/plots/plot_relative_size.png)

---

## 4. Speed

**Table 3 — Median wall-clock times** (end-to-end, including file read/write) and throughput (MiB/s of original data):

| Dataset | RAZ compress | ZIP compress | RAZ decompress | ZIP decompress | ZIP speed-up (comp) | ZIP speed-up (decomp) |
|---|---:|---:|---:|---:|---:|---:|
| en_text.txt | 145.45 ms | **5.89 ms** | 33.59 ms | **1.27 ms** | 24.7× | 26.4× |
| source_code.txt | 92.92 ms | **10.20 ms** | 36.94 ms | **1.44 ms** | 9.1× | 25.7× |
| data.csv | 75.08 ms | **40.67 ms** | 60.55 ms | **2.83 ms** | 1.8× | 21.4× |
| app.log | 118.69 ms | **14.56 ms** | 12.51 ms | **2.20 ms** | 8.2× | 5.7× |
| repetitive.dat | 130.78 ms | **9.92 ms** | 6.39 ms | **1.67 ms** | 13.2× | 3.8× |
| random.bin | 172.11 ms | **14.20 ms** | 164.40 ms | **0.88 ms** | 12.1× | 186.8× |

| Throughput (MiB/s) | RAZ compress | ZIP compress | RAZ decompress | ZIP decompress |
|---|---:|---:|---:|---:|
| en_text.txt | 6.6 | 161.9 | 28.4 | 750.9 |
| source_code.txt | 8.2 | 74.8 | 20.7 | 529.9 |
| data.csv | 7.6 | 14.1 | 9.5 | 202.2 |
| app.log | 7.2 | 59.0 | 68.6 | 390.2 |
| repetitive.dat | 7.3 | 96.1 | 149.3 | 571.1 |
| random.bin | 2.8 | 34.4 | 3.0 | 554.9 |

![Timings](Results/plots/plot_timings.png)

---

## 5. Analysis

### Where ZIP DEFLATE wins (5 of 6 datasets)

- **Algorithmic ceiling.** DEFLATE matches an LZ77 sliding window (up to 32 KB of look-back, arbitrary-length matches) with entropy coding. RAZ's best pipeline, LZW+Huffman, uses **fixed 16-bit codes** and a dictionary that **resets at 65,536 entries**, so on 1 MB files the dictionary flushes ~15 times, discarding everything it learned. On `en_text.txt` this alone costs a 3.8× size difference.
- **No per-file tree overhead.** Huffman pipelines must embed the serialized tree (and LZW+Huffman embeds a tree over LZW output); DEFLATE interleaves its dynamic Huffman tables per 64 KB block.
- **Mature implementation.** `java.util.zip.Deflater` is a heavily optimized native port of zlib; RAZ's algorithms are educational pure-Java implementations (String-keyed LZW dictionaries, `StringBuilder` bit manipulation in Huffman).

### Where RAZ Archiver wins

- **Highly repetitive data.** On `repetitive.dat` RAZ produced a **6.4% smaller archive** (10,355 vs 11,068 bytes) — the adaptive picker exploited the data's structure better than DEFLATE's 64 KB block granularity.
- **Incompressible data.** On `random.bin` both formats store ~1:1 (entropy coding cannot help); RAZ adds only ~0.16% overhead vs ZIP's ~0.06% — effectively parity.
- **Consistent, decent ratios on mixed content.** On CSV and logs RAZ lands within **~14–15%** of ZIP while providing a self-describing format with an embedded original extension and per-file CRC32.

### Speed gap

RAZ is slower by design and by implementation:

1. **It runs all four algorithms on every input** (plus a 5th combined pass) and keeps the smallest — roughly 3–4× the work of a single-method compressor, in exchange for never choosing badly.
2. **Pure-Java data structures** (String-keyed hash maps, char-level bit strings) versus zlib's hand-tuned native deflate.
3. The gap is largest on **decompression of incompressible data** (`random.bin`, 187×): RAZ still pays for Huffman tree rebuild + bit-by-bit decoding, while DEFLATE quickly falls back to a stored copy.

### Verdict

| Criterion | Winner | Margin |
|---|---|---|
| Compression ratio (text-heavy data) | ZIP | 1.7–3.8× smaller |
| Compression ratio (repetitive data) | **RAZ** | 6.4% smaller |
| Compression ratio (random data) | Tie | ≈ 0% |
| Compression speed | ZIP | 1.8–24.7× faster |
| Decompression speed | ZIP | 3.8–186.8× faster |
| Self-describing format + per-file CRC + extension restore | **RAZ** | — |
| Aggregate space saved | ZIP | 82.83% vs 80.49% |

ZIP DEFLATE is the stronger general-purpose compressor; RAZ Archiver delivers competitive ratios (within ~2.3 percentage points aggregate), beats ZIP on run-heavy data, and adds format features ZIP single-file archives don't offer — all while remaining a from-scratch, dependency-free Java implementation.

---



