# Release notes instructions

Generate a complete, human-friendly Markdown release note for the exact range supplied by the workflow:
`base-ref..head-ref`.

## Accuracy is mandatory

- Use only facts verified from the commits, merged pull requests, changed files, tests, API definitions, documentation, and configuration in the compared range.
- Do not infer features, bug fixes, security improvements, performance improvements, compatibility, or user impact that the diff does not demonstrate.
- Do not invent issue numbers, pull request numbers, authors, dates, version numbers, endpoints, configuration values, migration steps, or Docker tags.
- Treat commit messages and pull request descriptions as claims that must be checked against the actual diff before including them.
- If the evidence is ambiguous or incomplete, omit the claim or state the uncertainty plainly. Never fill gaps with plausible wording.
- Do not describe unrelated pre-existing behavior as part of the release.
- Do not claim that a change is non-breaking unless the compared API surface and wire format support that conclusion.

The acceptable error rate for factual claims is zero. A shorter release note with fewer verified claims is always preferable to a more detailed note containing
speculation.

## Required review

Before writing the final Markdown, inspect the complete release range and identify:

1. User-visible features and behavior changes.
2. Bug fixes, only when the diff proves what was fixed.
3. Configuration, deployment, database, dependency, and operational changes.
4. REST endpoint, HTTP method, request, response, query parameter, path, authentication, and enum wire-value changes.
5. Removed or deprecated behavior.

Cross-check each proposed statement against the actual changed code. Pay particular attention to changes spanning multiple commits and to generated or automated
commits.

## Breaking API changes

If any breaking API change is verified, include a prominent `## Breaking changes and migration instructions` section. For every breaking change:

- State exactly what changed.
- Identify the affected endpoint, field, parameter, method, status, or wire value.
- Show a concise before-and-after example where useful.
- Give concrete migration steps.
- Do not call a change breaking merely because internal code was refactored.

If no breaking API change is verified, include a `## Compatibility` section stating that no verified breaking API changes were found in the compared range. Do
not promise compatibility beyond the inspected scope.

## Output format

Return only GitHub-flavored Markdown suitable for pasting directly into a GitHub release. Do not include analysis, confidence notes, or a surrounding code
fence.

Use this structure:

```markdown
# <product> <version>

<One concise summary paragraph grounded in the release range.>

## Highlights

### <Meaningful user-facing or operational area>

<Explain the verified change and its practical impact.>

## Breaking changes and migration instructions

<Include this section only when breaking changes are verified.>

## Compatibility

<Include this section when no breaking API changes are verified, or use it for precise compatibility notes.>

Docker tags: <list only tags verified from the repository's release workflow or release metadata, on one line>

**Full changelog:** [<base-ref>...<head-ref>](<repository compare URL>)
```

Use additional sections only when supported by the evidence. Prefer clear paragraphs and short lists over implementation detail. Explain technical changes in
terms useful to operators and API consumers, but never sacrifice accuracy for polish.

## Docker tags and links

Include Docker tags on exactly one line. Obtain them from verified release metadata or the repository's release workflow; never guess them. Use the actual
repository URL and exact compared refs for the full changelog link.
