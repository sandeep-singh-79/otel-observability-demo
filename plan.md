# Plan: Automated Dependabot PR Lifecycle
> Last updated: 2026-03-06
> Track all phases here. Update status and notes as work progresses.

---

## Objective

Fully automate the lifecycle of Dependabot PRs in `sandeep-singh-79/otel-observability-demo`:
- Auto-merge passing patch/minor dependency bumps into `main`
- Delete the Dependabot branch on merge
- On CI failure: automatically analyse logs using GitHub Copilot, post a diagnosis comment, and where possible push a fix commit
- Major version bumps always require manual human review

---

## Status Legend

| Symbol | Meaning |
|---|---|
| ⬜ | Not started |
| 🔧 | In progress |
| ✅ | Complete |
| 🚫 | Blocked |
| ⏭️ | Skipped / deferred |

---

## Phase 1 — Repository Settings (GitHub UI)

**Type:** Manual — GitHub Settings UI (no code)
**Status:** ⬜ Not started

### Steps
- [ ] Settings → General → **Allow auto-merge** → Enable
- [ ] Settings → General → **Automatically delete head branches** → Enable
- [ ] Settings → General → Confirm merge strategy (recommend: **Squash and merge** for clean main history)

### Notes
- These are prerequisites for Phases 3 and 4 — must be done first.
- "Allow auto-merge" is a repo-level toggle; without it, `gh pr merge --auto` silently fails.
- "Automatically delete head branches" handles branch cleanup on *every* merge (not just Dependabot), so it's globally beneficial.

---

## Phase 2 — Branch Protection Rule on `main`

**Type:** Manual — GitHub Settings UI (no code)
**Status:** ⬜ Not started

### Steps
- [ ] Settings → Branches → Add rule → Branch name pattern: `main`
- [ ] Enable: **Require status checks to pass before merging**
  - [ ] Add required check: `Build Project` (from `API Test CI` workflow)
  - [ ] Add required check: `Analyze (java)` (from `CodeQL` workflow)
- [ ] Enable: **Require branches to be up to date before merging**
- [ ] Enable: **Do not allow bypassing the above settings**

### Notes
- This is the non-bypassable gate — auto-merge only fires when all checks are green.
- Required check names must match exactly the `name:` field inside the workflow job, not the workflow file name.
  - `testCI.yml` → job name: `Build Project`
  - `codeQualityChecks.yml` → job name: `Analyze`

---

## Phase 3 — Update `dependabot.yml`

**Type:** Code change — `.github/dependabot.yml`
**Status:** ✅ Complete — `feature/dependabot-automation`

### Change Summary
Add `auto-merge: true` to both `maven` and `github-actions` ecosystems.
Scope `groups` to batch minor/patch updates to reduce PR noise.

### Target State
```yaml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "monthly"
    open-pull-requests-limit: 10
    groups:
      maven-patch-minor:
        update-types:
          - "minor"
          - "patch"
    auto-merge: true

  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "monthly"
    auto-merge: true
```

### Notes
- `auto-merge: true` is a Dependabot-native feature — only applies to Dependabot-opened PRs.
- Requires Phase 1 (Allow auto-merge in repo settings) to be done first.
- `groups` reduces the number of individual PRs — all patch/minor maven deps get batched into one PR.

---

## Phase 4 — New Workflow: `dependabot-auto-merge.yml`

**Type:** Code change — `.github/workflows/dependabot-auto-merge.yml`
**Status:** ✅ Complete — `feature/dependabot-automation`

### Purpose
Safety-net workflow — arms auto-merge on Dependabot PRs after the CI checks pass. Handles cases where native Dependabot auto-merge doesn't fire (e.g. after a manual push to the branch).

### Trigger
```yaml
on:
  pull_request_target:
    types: [opened, synchronize, reopened]
    branches: [main]
```

### Guards (all must pass before merge is armed)
1. `github.actor == 'dependabot[bot]'`
2. `startsWith(github.head_ref, 'dependabot/')`
3. `update-type` is `semver-patch` or `semver-minor` (via `dependabot/fetch-metadata`)

### Behaviour on major version
Post a PR comment: "⚠️ Major version bump — requires manual review before merging."

### Permissions needed
```yaml
permissions:
  contents: write
  pull-requests: write
```

### Key Actions Used
- `dependabot/fetch-metadata@v2` — reads bump type from PR
- `gh pr merge --auto --squash "$PR_URL"` — arms auto-merge (fires once all required checks pass)

### Notes
- Uses `pull_request_target` (not `pull_request`) so the workflow has write permissions even from a fork-like Dependabot context.
- `--auto` means it doesn't merge immediately — it arms the merge to fire only when branch protection checks pass.

---

## Phase 5 — New Workflow: `dependabot-failure-analysis.yml`

**Type:** Code change — `.github/workflows/dependabot-failure-analysis.yml`
**Status:** ✅ Complete — `feature/dependabot-automation`
**Prerequisite:** GitHub Copilot Business/Enterprise OR GitHub Models access (public repo = free via `models: read`)

### Purpose
When CI fails on a Dependabot PR, automatically analyse the failure and post a structured diagnosis comment. Optionally push a fix commit.

### Trigger
```yaml
on:
  workflow_run:
    workflows: ["API Test CI"]
    types: [completed]
```

### Steps
1. **Gate:** Only proceed if `github.event.workflow_run.conclusion == 'failure'`
2. **Gate:** Only proceed if head branch starts with `dependabot/`
3. **Find PR:** Query GitHub API for open PR matching the `head_sha`
4. **Fetch logs:** Download failed job logs via GitHub Actions API; extract surefire XML or last 200 lines
5. **AI analysis:** Call `actions/ai-inference` (GitHub Models / Copilot) with:
   - The error text
   - `pom.xml` diff (from PR)
   - Diagnosis prompt asking for: root cause, category (version conflict / compile / test / infra), fix recommendation
6. **Post comment:** Structured PR comment with:
   - Failure category
   - Root cause explanation
   - Suggested fix
   - `@github-copilot try to fix` mention for interactive Copilot assist
7. **(Optional)** If fix is a simple single-line `pom.xml` change and confidence is high: apply and commit directly to the branch

### Permissions needed
```yaml
permissions:
  contents: write
  pull-requests: write
  actions: read
  models: read
```

### Notes
- `models: read` permission enables GitHub Models API access within Actions (no extra billing on public repos as of early 2026).
- Step 7 (auto-commit fix) should be conservative — only for patterns we've seen before (e.g. OTel version skew → BOM fix). Avoid auto-committing speculative fixes.

---

## Phase 6 — Copilot for PRs (GitHub Native, Zero Config)

**Type:** Settings — GitHub org/repo settings
**Status:** ⬜ Not started (verify if already enabled)

### Steps
- [ ] Settings → Copilot → **Copilot for Pull Requests** → Enable
- [ ] Verify `@github-copilot try to fix` trigger works on a test PR comment

### Notes
- Complements Phase 5 — handles cases where the automated analysis can't produce a committed fix.
- Copilot reads the PR diff and CI logs natively; no workflow code needed.

---

## Completed Work (Pre-Plan)

| Date | Item | Outcome |
|---|---|---|
| 2026-03-06 | Diagnosed GrpcSenderConfig NoClassDefFoundError on PR #27 | Root cause: OTel version skew (sdk 1.50.0 vs exporter 1.59.0) |
| 2026-03-06 | Fixed pom.xml — unified `opentelemetry.version=1.59.0` + added BOM | 5/5 local tests passed |
| 2026-03-06 | Committed fix to `dependabot/maven/io.opentelemetry-opentelemetry-exporter-otlp-1.59.0` | Pushed to origin |
| 2026-03-06 | Monitored PR #27 CI | All checks passed ✅ |

---

## Open Questions

- [ ] Does the repo have a GitHub Copilot Business/Enterprise subscription? (Affects Phase 5 AI actions choice)
- [ ] What merge strategy is preferred — squash, merge commit, or rebase? (Affects Phase 1 config and `--squash` flag in Phase 4)
- [ ] Should `groups` in dependabot.yml batch *all* maven deps or keep them separate by ecosystem subset?
- [ ] Phase 5 Step 7 (auto-commit fix): opt-in or opt-out by default?

---

## Next Action

**→ Phases 1 and 2** are manual GitHub UI steps — must be done in GitHub UI before merging `feature/dependabot-automation` to `main`.
**→ Phases 3, 4, 5** are implemented on `feature/dependabot-automation` — open a PR to `main` and review.
**→ Phase 6** — verify Copilot for PRs is enabled in repo Settings → Copilot.
