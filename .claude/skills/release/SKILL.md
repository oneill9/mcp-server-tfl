---
name: release
description: Prepare a release by generating release notes from git history, committing them, tagging, and pushing.
argument-hint: <version-tag e.g. v1.0.0>
---

Prepare a release for version $ARGUMENTS.

## Steps

1. **Validate the version tag** — ensure it starts with `v` and follows semver (e.g. `v1.0.0`). If no argument is provided, ask the user for the version.

2. **Find the previous tag** — run `git tag --sort=-v:refname` to find the most recent existing tag. If there is no previous tag, the changelog covers all commits.

3. **Generate the changelog** — run `git log --oneline <previous-tag>..HEAD` to get the list of commits since the last release. Group them into a readable changelog with sections like "What's Changed". Include commit messages but exclude merge commits.

4. **Write RELEASE_NOTES.md** — write the changelog to `RELEASE_NOTES.md` in the project root. Format it as:
   ```markdown
   ## What's Changed in $ARGUMENTS

   - Commit message here (short hash)
   - Another commit message (short hash)
   ...
   ```

5. **Show the release notes to the user** — display the contents of `RELEASE_NOTES.md` and ask the user to confirm or request edits before proceeding.

6. **Commit the release notes** — stage and commit `RELEASE_NOTES.md` with message: `docs: add release notes for $ARGUMENTS`

7. **Create the tag** — run `git tag $ARGUMENTS`

8. **Push** — run `git push origin main --tags` to push both the commit and the tag. This will trigger the release workflow.

9. **Confirm** — tell the user the tag has been pushed and the GitHub Actions release workflow will create the GitHub release with the committed notes and build artifacts.
