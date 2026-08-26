# Per-version changelogs

Each release version lands here as `<mod_version>.md`. The build wires
`publishMods` to read this file as the changelog text Modrinth and GitHub
Releases display for that version.

```
.modrinth/changelogs/
  1.0.0.md     ← changelog for v1.0.0
  1.1.0.md     ← changelog for v1.1.0
  ...
```

## Format

Markdown. Modrinth renders it on the version's row in the Changelog tab;
GitHub renders it as the Release body. Keep it focused on what changed for
end users — short paragraph or bullet list, no version-bump
boilerplate. Example:

```markdown
- New: the Amethyst Crown, one per world, found rather than crafted.
- Fix: the Netherite Spear can no longer be stashed in a powered shelf.
- Internal: ported to Minecraft 26.3 (no behavior change).
```

## Discipline

Bumping `mod_version` in `gradle.properties` without creating the matching
file under here will fail the publish step at build time with a clear
error pointing at the missing path. The intent: every release has written
notes, by construction.
