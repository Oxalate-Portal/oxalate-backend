# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Read AGENTS.md

All agent guidance for this repository lives in [`AGENTS.md`](AGENTS.md). Read it before starting work —
it is the authoritative source, not a supplement to this file.

It covers, in order: the product domain and role model, architecture essentials and the REST surface, the
audit/exception and data-access patterns, portal configuration, file uploads, scheduled jobs, API and DTO
conventions, database migrations, build/run/debug commands and the build gates that fail a change, the test
structure, the non-negotiable OWASP security rules, step-by-step recipes for common tasks with a definition
of done, the commit message format, and the key files to learn first.

## Keeping these two files apart

`AGENTS.md` is tool-agnostic and is where new guidance belongs. Do not restate its content here, and do not
add repository knowledge to this file — an instruction duplicated in both places will drift and the two will
disagree. `CLAUDE.md` should stay an entrypoint.
