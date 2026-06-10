# CLAUDE.md — Wordle (self-playing)

Project context for Claude Code. Repo root: `C:\dev\wordle`.

This is a recreational, single-developer project. It is **not** part of the consulting practice — treat it as a sandbox, but follow the same engineering conventions as the rest of the suite (KOTH, GolferFest, Brier, BingmerFestFF, NinjaSensation).

---

## 1. What this app is

A self-playing Wordle engine with three modes sharing one solver core:

1. **Self-play simulator** — picks a (seedable) random answer, the engine solves it autonomously, and the UI animates the board while showing the candidate-set size collapsing and bits of information gained per turn. Autoplay with a speed control; loop.
2. **Daily NYT solver** — two sub-modes:
   - **Auto**: fetches today's solution from the NYT endpoint and self-plays it.
   - **Assist** (honest, no peeking): the engine proposes a guess; the user enters the green/yellow/gray pattern they observed on the real NYT board; the engine narrows. This is the "solver as advisor" flow.
3. **Benchmark harness** — runs the solver across the full answer list (or a seedable sample), records win-rate, average guesses, the 1–6 guess-count distribution, fails, and worst-case answers. Persists runs so different openers/strategies can be compared over time.

**The core solver requires no LLM and no external AI calls.** It is pure information theory. Do not wire in the Anthropic SDK for the solver. (See §9 for an optional, out-of-critical-path commentary stretch goal.)

---

## 2. Tech stack (matches the suite)

- Java 21
- Spring Boot 3.4.x, **WAR** packaging
- JSP + JSTL views (server-rendered; no SPA framework)
- MySQL 9.5
- Flyway for schema migrations
- Tomcat 10.1 (prod port 8080, Eclipse test server port 8081)
- Cloudflare for optional public exposure
- Eclipse + EGit + WTP. **No CLI Maven** — builds/runs happen in Eclipse.

Profiles: `prod` (set globally via `setenv.bat` CATALINA_OPTS on the 8080 service) and `dev` (set per-server on the Eclipse 8081 test server).

---

## 3. Layout & naming

- Repo root: `C:\dev\wordle`
- Package base: `com.bingmerfest.wordle`  *(one-line swap if you'd rather use `com.ninjasensation.wordle`)*
- WAR / context path: `wordle.war` → `/wordle`
- DB schemas: `wordle_test`, `wordle_prod`
- Secrets dir: `C:\WORDLE-secrets\secrets.properties`, loaded via `setenv.bat` CATALINA_OPTS (same pattern as the other apps)
- This file (`CLAUDE.md`) lives at the repo root, like the Brier and BingmerFestFF context files.

Suggested module / package structure:

```
com.bingmerfest.wordle
├── engine            // pure, no Spring — the solver lives here
│   ├── WordLists        // loads answers + allowed-guesses from classpath
│   ├── PatternService   // feedback computation, base-3 encoding
│   ├── PatternMatrix    // precomputed guess×answer pattern table
│   ├── EntropySolver    // the brain
│   └── SolverConfig     // opener, hard-mode flag, guess pool, tiebreak
├── nyt               // NytWordleClient (daily answer fetch + cache)
├── persistence       // JPA entities + repositories (see §7)
├── web               // Spring MVC controllers, one per mode
└── WordleApplication
```

Keep `engine` free of Spring annotations so it can be unit-tested and benchmarked standalone.

---

## 4. The solver (the crux — get this exactly right)

### 4.1 Feedback / pattern computation (handle duplicate letters correctly)

Wordle's duplicate-letter rules require a **two-pass** algorithm. Naive per-letter checks are wrong (e.g. guess `SPEED` vs answer `ERASE`).

1. **Pass 1 (greens):** for each position, if `guess[i] == answer[i]`, mark green and decrement that letter's count in a multiset built from the answer.
2. **Pass 2 (yellows/grays):** for each non-green position, if the guessed letter still has remaining count in the answer multiset, mark yellow and decrement; otherwise mark gray.

Encode the 5-tile result as a base-3 integer: `gray=0, yellow=1, green=2` → range `0..242` (243 possible patterns). This integer is the partition key.

**Write unit tests first** against known tricky cases (repeated letters in guess, repeated letters in answer, both) before building anything on top.

### 4.2 Pattern matrix precompute

Precompute `pattern[guessIndex][answerIndex]` as a `byte[][]` over the **full** guess list (~12,972) × the **full** answer list (~2,315). That's ~30 MB — fits comfortably in memory. Build it once at startup; optionally serialize a cache to a data dir to skip rebuild. (The Threadripper builds it in seconds; don't over-engineer the cache.)

### 4.3 Guess selection (entropy maximization)

Given the current set of remaining candidate answers `R`:

- For each candidate guess `g` (the full guess pool in easy mode; only clue-consistent words in hard mode), partition `R` by `pattern[g][a]` over all `a ∈ R`.
- Compute entropy `H(g) = -Σ p·log2(p)` where `p = bucketCount / |R|`.
- Choose `argmax H(g)`.
- **Tiebreak:** prefer a `g` that is itself in `R` (it might be the answer and end the game a turn early); then fall back to lexicographic order for determinism.
- When `|R| == 1`, guess it.

### 4.4 Opener

Precompute the best opening guess **once** and cache/hardcode it so every game skips the expensive first full scan. `SALET` is the standard strong opener (~5.8 bits on the classic list); `SOARE`, `CRANE`, `TRACE` are close alternatives. Make the opener a `SolverConfig` field so the benchmark harness can compare them.

### 4.5 Config flags (`SolverConfig`)

- `opener` (default `SALET`)
- `hardMode` (default **false** — allow any guess for maximum information; document both behaviors)
- `guessPool` (FULL allowed list vs answers-only)
- `tiebreak` policy
- max guesses = 6; record a failure if unsolved.

Expected performance to validate against: average ≈ 3.4–3.6 guesses, win-rate ≈ 100% on the classic list. The benchmark harness is the regression check.

---

## 5. Word lists

Bundle as classpath resources under `src/main/resources/words/`:

- `answers.txt` — the ~2,315 curated solution words
- `allowed.txt` — the accepted non-answer guesses (combined with answers = the ~12,972 valid guesses)

Source: the `stuartpb/wordles` list (`wordles.json` solutions + `nonwordles.json` accepted guesses). NYT now curates daily answers **server-side**, so this historical list is a stable, correct basis for the *engine*. The **live daily answer** for the daily-solver auto mode comes from the NYT endpoint (§6), not from the bundled list.

Load both into memory at startup and build the index maps used by the pattern matrix.

---

## 6. NYT daily integration

Endpoint: `https://www.nytimes.com/svc/wordle/v2/{YYYY}-{MM}-{DD}.json`

Returns JSON including `solution` (the 5-letter answer), `id` / `days_since_launch`, `print_date`, and `editor`. Supported date range: launch (2021-05-19) through ~23 days in the future; out-of-range dates error.

- `NytWordleClient` fetches and **caches** the day's response (the answers don't change; don't repeatedly hammer the endpoint).
- Auto mode: fetch `solution`, run the solver against it as a self-play game.
- Assist mode: never fetch the solution — the engine proposes, the user supplies the observed pattern.

---

## 7. Persistence (Flyway + JPA)

Keep it modest. Suggested tables:

- `solver_config` — id, name, opener, hard_mode, guess_pool, tiebreak, created_at
- `game` — id, mode (SIMULATOR/DAILY/BENCHMARK), answer, solved, num_guesses, guesses (JSON or child rows), config_id, created_at
- `benchmark_run` — id, config_id, answer_count, win_rate, avg_guesses, dist_1..dist_6, fails, duration_ms, created_at
- `daily_solve` — id, puzzle_date, puzzle_id, answer, solved, num_guesses, mode (AUTO/ASSIST), created_at

**Watch for MySQL 9.5 reserved words** when naming columns/tables (we got bitten by `LEAD` on NinjaSensation — kept the Java entity as `Lead` via `@Table(name="download_request")`). `game` is fine, but check anything new.

---

## 8. Conventions (suite-wide — follow these)

- **Eclipse import:** always Import → General → *Existing Projects into Workspace*. **Never** use Maven import — it regenerates `.classpath` and can drop the WTP attribute `org.eclipse.jst.component.dependency=/WEB-INF/lib`.
- **Flyway filenames:** always double underscore — `V8__add_benchmark_run.sql`. Add a `.gitattributes` with `*.sql text eol=lf` to prevent checksum drift across line endings.
- **JSP asset paths:** use plain relative paths (no `${pageContext.request.contextPath}`); keep pages at non-nested URL paths so relative paths resolve.
- **CSS:** the global `styles.css` sets `.card { height:100%; background-color:black; margin:20px }`, which breaks layouts. Override with `#pageId .card` selectors scoped per page.
- **Theme:** dark UI — black backgrounds, `#1A43BF` blue headers. The Wordle board should feel native to that palette (keep the green/yellow/gray tile semantics, but the chrome is dark).
- **Secrets:** externalized to `C:\WORDLE-secrets\secrets.properties`, loaded via `setenv.bat` CATALINA_OPTS. Never commit secrets.
- **Deliverables:** prefer **complete files**, not diffs/snippets. Sequence deployment steps explicitly: **SQL first, then Java, then JSP.**

---

## 9. Optional stretch — commentary layer (NOT critical path)

The suite has a house style for AI commentary (GolferFest, KOTH): witty, cutting-yet-lighthearted metaphors. A purely cosmetic narration layer over the simulator ("the solver just torched 90% of the candidate field with one word") would be on-brand.

If/when added: model `claude-sonnet-4-6`, Anthropic SDK, with a hard daily cost cap (cf. KOTH's $5/day, Brier's spend gate). Keep it strictly decorative — the solver must run identically with commentary disabled. Do not build this until the three core modes work.

---

## 10. Milestones (build in this order)

- **M1 — Engine core:** word-list loading, `PatternService` with correct duplicate handling, base-3 encoding. Unit tests on known patterns. No Spring.
- **M2 — Solver:** pattern matrix precompute, `EntropySolver`, `SolverConfig`, precomputed opener. A standalone test harness that solves a given answer.
- **M3 — Benchmark (engine-level):** run over all answers, print win-rate / avg / distribution. Validate ≈3.4–3.6 avg, ~100% win.
- **M4 — Web skeleton:** Spring Boot WAR, Flyway schema, dev/prod profiles, secrets externalization, persistence layer.
- **M5 — Simulator page:** dark-themed animated board, candidate-count + bits-per-turn readout, autoplay + speed control.
- **M6 — Daily solver:** `NytWordleClient` + auto mode + assist/manual page.
- **M7 — Benchmark UI:** trigger runs, persist, compare configs, distribution histogram.
- **M8 — Deploy:** `wordle.war` on prod 8080 alongside the other WARs.

---

## 11. Cloudflare (optional — only if exposed publicly)

If you decide to expose it (e.g. `wordle.bingmerfest.com`), mirror the established pattern:

- **Origin Rule:** rewrite origin port → 8080.
- **URL Rewrite:** use **Edit-expression** mode with `concat("/wordle", http.request.uri.path)`. Do **not** use the URI-Full / wildcard form — it produces broken rules.
- **www → apex** redirect.
- **SSL:** Flexible.

Deploys as another context path on the existing prod Tomcat — no vhosts.