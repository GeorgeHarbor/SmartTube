#!/usr/bin/env bash
# Run inside a stopped merge. For each conflicted submodule pointer, merges upstream's submodule
# commit into the fork's one, pushes the result to the fork's submodule repo and stages it.
# Pushes to the branch whose tip is the fork's pointer, else to sync/upstream-<sha> to keep it reachable.
# Exits non-zero if a submodule merge itself conflicts. PUSH_TOKEN (optional) authenticates to github.com.
set -euo pipefail

if [ -n "${PUSH_TOKEN:-}" ]; then
  export GIT_CONFIG_COUNT=1
  export GIT_CONFIG_KEY_0="http.https://github.com/.extraheader"
  export GIT_CONFIG_VALUE_0="AUTHORIZATION: basic $(printf 'x-access-token:%s' "$PUSH_TOKEN" | base64 | tr -d '\n')"
fi

name=$(git config user.name)
email=$(git config user.email)
work=$(mktemp -d)

for path in $(git ls-files -u | awk '$1 == "160000" && $3 == "2" {print $4}'); do
  ours=$(git rev-parse ":2:$path")
  theirs=$(git rev-parse ":3:$path")
  repo="$work/$path"
  git clone -q "$(git config -f .gitmodules "submodule.$path.url")" "$repo"
  git -C "$repo" fetch -q "$(git config --blob MERGE_HEAD:.gitmodules "submodule.$path.url")" '+refs/heads/*:refs/remotes/upstream/*'
  git -C "$repo" checkout -q --detach "$ours"

  if ! git -C "$repo" -c user.name="$name" -c user.email="$email" merge -q --no-edit -m "Merge upstream $path@${theirs:0:8}" "$theirs"; then
    echo "::error::$path: merging upstream ${theirs:0:8} into fork ${ours:0:8} conflicts, resolve it by hand"
    exit 1
  fi

  merged=$(git -C "$repo" rev-parse HEAD)
  branch=""
  if [ "$merged" != "$ours" ]; then
    branch=$(git -C "$repo" for-each-ref --points-at "$ours" --format='%(refname:lstrip=3)' refs/remotes/origin | grep -vx HEAD | head -1 || true)
    # The sync/ branch is ours alone, so a retry after a failed run may overwrite it
    refspec="HEAD:refs/heads/$branch"
    if [ -z "$branch" ]; then
      branch="sync/upstream-${theirs:0:8}"
      refspec="+HEAD:refs/heads/$branch"
    fi
    git -C "$repo" push -q origin "$refspec"
  fi
  git update-index --cacheinfo "160000,$merged,$path"
  echo "$path: fork ${ours:0:8} + upstream ${theirs:0:8} -> ${merged:0:8} ${branch:+pushed to $branch}"
done
