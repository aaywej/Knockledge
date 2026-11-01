---
trigger: always_on
description: Always link Superpowers plan and SpecKit artifacts to Antigravity artifacts directly without copying or generating content in .artifacts/.
---

# Antigravity Artifact Linking Rules

Expose Superpowers and SpecKit plan/specification files directly as Antigravity artifacts for the `agy` CLI without creating duplicate content or using an `.artifacts/` directory.

## Core Directives

1. **No `.artifacts/` Directory or Duplicate Content**:
   - Do NOT create, duplicate, or write files into an `.artifacts/` directory.
   - Do NOT generate new or separate artifact content files.

2. **Direct Superpowers Artifact Linking**:
   - Register and reference Superpowers plan and execution files (e.g., `docs/superpowers/plans/*.md`, `.superpowers/`) directly as Antigravity artifacts in place.
   - Ensure the `agy` CLI points directly to the source Superpowers documents.

3. **Direct SpecKit Artifact Linking**:
   - Register and reference SpecKit specification, plan, task, and feature documents (e.g., `specs/*/spec.md`, `specs/*/plan.md`, `specs/*/tasks.md`, `.specify/`) directly as Antigravity artifacts in place.
   - Ensure the `agy` CLI points directly to the source SpecKit files without copying or re-generating them.
