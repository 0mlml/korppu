# AGENTS.md

Project-wide instructions for AI coding agents.

## Core Rule: Do Not Assume Behavior

- Do not invent mechanics, shortcuts, or "likely" behavior.
- If implementation intent is unclear and materially affects behavior, stop and ask one focused question.
- Prefer a short clarification over a wrong implementation.

## Implementation Workflow

1. Read the existing module/file first.
2. If there is a source-of-truth reference (upstream mod, spec, linked file), compare against it before coding.
3. Implement the real behavior, not a visual/client-only approximation, unless explicitly requested.
4. Keep changes minimal and aligned with existing project conventions.
5. Do not add unnecessary comments

## When You Must Ask Before Coding

Ask before proceeding when any of the following is true:

- Multiple plausible implementations exist.
- The change can alter gameplay semantics, exploit behavior, networking, persistence, or security.
- The request could mean either a cosmetic fix or a functional mechanic change.
- Required source-of-truth is missing or conflicting.

When asking, provide:

- Recommended default.
- 1-2 alternatives.
- Exact impact of each option.

## Validation Requirements

- Compile after code changes (`./gradlew compileClientJava`) when feasible.
- If behavior-sensitive, include a quick note on what was verified and what still needs in-game validation.

## Communication Style

- Be concise and explicit.
- State assumptions only when unavoidable, and label them as assumptions.
- Never present guessed behavior as confirmed behavior.

## Change Safety

- Do not remove or replace existing mechanics without confirming intent.
- Do not silently downgrade functional behavior to a simpler approximation.
- Preserve unrelated user changes in a dirty worktree.
