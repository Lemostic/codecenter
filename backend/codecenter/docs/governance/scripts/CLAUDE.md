# Governance Scripts

Bash + Python tooling for the Enterprise Coding Standards governance system: registry maintenance, proposal validation, and AI spec-engine injection.

## Stack
- Bash 4+ (strict mode: `set -euo pipefail`)
- Python 3.8+ with PyYAML (auto-installed; called inline via `python3 -c`)
- GitHub Actions for CI

## Scripts
- `init.sh` — fresh-checkout initializer: structure check, registry validation, pre-commit hook install
- `bootstrap.sh` — inject spec engine into a project: creates `.spec/spec-manifest.yaml`, project-inventory, glossary, and patches target `CLAUDE.md` with an idempotent marker block
- `validate-spec.sh` — runs 14 rule families (V-001 ... V-303): YAML syntax, registry integrity, profile completeness, routing/cross-reference, SemVer, dependencies, protected files, role permissions, CHANGELOG
- `.github-workflow.yml` — wires `validate-spec.sh` into PR (incremental), push to main, and weekly full scan

## Conventions
- Exit codes: 0 = pass, 1 = error, 2 = warn-only
- Idempotent file writes via `safe_write` / `safe_append` with a unique marker (e.g. `<!-- ENTERPRISE-CODING-STANDARDS -->`)
- New validation rules: append to `validate-spec.sh` following the V-NNN numbering
- Bilingual zh/en comments — preserve the style when editing
- Scripts assume they live at `docs/governance/scripts/` within the spec tree; resolve sibling paths (`../registry.yaml`, `../../skill/`, `../../../`) via `SCRIPT_DIR` / `GOVERNANCE_DIR` / `DOCS_DIR`
- Pass `DOCS_DIR` and `GOVERNANCE_DIR` as env vars to inline Python to avoid CJK-path encoding loss across the shell-to-Python boundary
