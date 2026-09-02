#!/usr/bin/env bash
set -Eeuo pipefail

case "${DEMO_PROFILE:-}" in
    ada)
        expected_uid=10001
        ;;
    linus)
        expected_uid=10002
        ;;
    grace)
        expected_uid=10003
        ;;
    *)
        printf 'DEMO_PROFILE must be one of: ada, linus, grace\n' >&2
        exit 64
        ;;
esac

if [[ "$(id -u)" != "$expected_uid" ]]; then
    printf 'DEMO_PROFILE=%s must run as UID %s (running as UID %s)\n' \
        "$DEMO_PROFILE" "$expected_uid" "$(id -u)" >&2
    exit 64
fi

if [[ -z "${OPENCODE_SERVER_PASSWORD:-}" ]]; then
    printf 'OPENCODE_SERVER_PASSWORD must be set to a non-empty demo password\n' >&2
    exit 64
fi

export HOME="/home/${DEMO_PROFILE}"
workspace_root="${HOME}/workspaces"
task_marker='- [ ] Demo change: describe and implement one small improvement in this repository.'
repo_count=0

while IFS= read -r -d '' repo; do
    ((repo_count += 1))

    if [[ ! -d "${repo}/.git" ]]; then
        if [[ ! -f "${repo}/DEMO_TASKS.md" ]]; then
            printf '# Demo tasks\n\nUse this file to practice inspecting and changing the repository.\n' \
                >"${repo}/DEMO_TASKS.md"
        fi

        git -C "$repo" init --quiet --initial-branch=main
        git -C "$repo" config user.name "OpenCode Demo Server (${DEMO_PROFILE})"
        git -C "$repo" config user.email "${DEMO_PROFILE}@opencode-demo.invalid"
        git -C "$repo" add --all
        git -C "$repo" add --force DEMO_TASKS.md
        GIT_AUTHOR_DATE='2025-01-01T00:00:00Z' \
            GIT_COMMITTER_DATE='2025-01-01T00:00:00Z' \
            git -C "$repo" commit --quiet --message 'Demo baseline'
    fi

    if git -C "$repo" diff --quiet -- DEMO_TASKS.md; then
        printf '\n%s\n' "$task_marker" >>"${repo}/DEMO_TASKS.md"
    fi
done < <(find "$workspace_root" -mindepth 1 -maxdepth 1 -type d -print0 | sort --zero-terminated)

if ((repo_count == 0)); then
    printf 'No demo repositories found under %s\n' "$workspace_root" >&2
    exit 66
fi

cd "$workspace_root"
exec opencode serve --hostname 0.0.0.0 --port 4096
