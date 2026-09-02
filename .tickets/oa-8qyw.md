---
id: oa-8qyw
status: closed
deps: [oa-v3js]
links: []
created: 2026-05-05T18:19:53Z
type: chore
priority: 2
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, sora, cleanup]
---
# Delete SyntaxHighlighter.kt; replace with Sora TextMate tokenizer wrapper

Once SoraEditor lands (depends on the editor ticket), delete the 546-line custom regex syntax highlighter at ui/components/code/SyntaxHighlighter.kt. Replace its read-only AnnotatedString output (used for chat code fences via mikepenz markdown renderer's code-block slot, and for FileViewerScreen view mode) with a thin wrapper around Sora's TextMate tokenizer — TextMateLanguage.create(...) with analyzeManager headless mode, converting tokens to AnnotatedString.

Net change: -546 LOC + ~120 LOC = -426 LOC. Plus correctness gains for languages the regex highlighter never properly handled (TypeScript, Rust, Go, Ruby, PHP, SQL — all listed at SyntaxHighlighter.kt:31-55). Delete , ,  from that file; the new wrapper lives at ui/components/code/TextMateAnnotatedString.kt.

## Acceptance Criteria

SyntaxHighlighter.kt deleted. New TextMateAnnotatedString.kt < 130 LOC. Chat code fences still render with highlighting. FileViewerScreen view mode still highlights. Tests on at least Kotlin, Python, TypeScript, Rust verify token correctness.


## Notes

**2026-05-05T18:20:03Z**

Correction: the three eaten identifiers in the description are 'SyntaxColors', 'Language', and 'OpenCodeSyntaxHighlighter' (the public symbols in SyntaxHighlighter.kt that need deleting along with the file). zsh ate them due to backtick interpretation in the original create command.

**2026-05-07T14:48:05Z**

Pausing while corrective batch lands: WorkspaceFileRepository drops FileContentDto.hash, OfishBaselineHasher hashes in-memory string instead of on-disk bytes, GPL Termux relinking notice missing, MIT grammar entries marked version=null. Council audit confirmed reward hacks. Resuming after fix batch with view/edit highlighter unification.
