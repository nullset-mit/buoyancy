# Buoyancy — Claude Code Guidelines

## Project

See `ai/overview.md` for the project brief and guiding principles.

## AI Interaction Pattern

### Session Logs

Use plan mode before any implementation. The approved plan file serves as the session log — it records design decisions, rationale, and implementation outcomes (class-to-package mappings, etc.) without duplicating the code itself.

Plan files are stored at: `ai/plans/feature-N-phase-M.md` and serve as the
authoritative record for implementing phase M of feature N. The user edits
this file directly to refine or update the plan.

If implementation deviates from the plan, annotate the plan file with a brief note explaining the change.
Likewise, the user may annotate the plan file with corrections or updates. The planning cycle consists of a back-and-forth between the user and serve as shared state.

### What to capture in a plan

- Design decisions and their rationale
- Implementation outcomes: each class created, its package, and its role
- Any deviations from the feature spec, with justification

Raw code should not appear in plan files — it can be read from the source on demand.
