#!/usr/bin/env bash
# Self-check for resolve-submodule-conflicts.sh against throwaway local repos. Run: bash test-resolve-submodule-conflicts.sh
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/resolve-submodule-conflicts.sh"
export GIT_CONFIG_GLOBAL=/dev/null GIT_AUTHOR_NAME=t GIT_AUTHOR_EMAIL=t@t GIT_COMMITTER_NAME=t GIT_COMMITTER_EMAIL=t@t

commit() { # repo file content -> sha
  echo "$3" > "$1/$2" && git -C "$1" add "$2" && git -C "$1" commit -qm "$2=$3" && git -C "$1" rev-parse HEAD
}

# Builds sub_up/sub_fork submodule repos and a superproject merge stopped on the sub pointer.
# $1: fork submodule content, $2: upstream submodule content, $3: whether the fork pointer is a branch tip
setup() {
  t=$(mktemp -d) && cd "$t"
  git init -q -b master sub_up && base=$(commit sub_up a base)
  git clone -q sub_up sub_fork && git -C sub_fork checkout -q -b feat
  fork_sha=$(commit sub_fork "${1%%=*}" "${1#*=}")
  [ "$3" = tip ] || commit sub_fork extra x > /dev/null
  up_sha=$(commit sub_up "${2%%=*}" "${2#*=}")
  git clone -q --bare sub_fork sub_fork.git && git clone -q --bare sub_up sub_up.git

  git init -q -b master super && cd super
  git config user.name t && git config user.email t@t
  printf '[submodule "sub"]\n\tpath = sub\n\turl = %s\n' "$t/sub_up.git" > .gitmodules
  git update-index --add --cacheinfo "160000,$base,sub" && git add .gitmodules && git commit -qm base
  git checkout -q -b up && git update-index --cacheinfo "160000,$up_sha,sub" && git commit -qm "up bump"
  git checkout -q master
  git config -f .gitmodules submodule.sub.url "$t/sub_fork.git" && git add .gitmodules
  git update-index --cacheinfo "160000,$fork_sha,sub" && git commit -qm "fork bump"
  ! git merge -q --no-edit up > /dev/null 2>&1
}

fail() { echo "FAIL: $*"; exit 1; }

# 1. Clean submodule merge, fork pointer is the tip of feat: merge lands on feat and is staged
setup b=fork c=up tip
bash "$script" > /dev/null
merged=$(git rev-parse :0:sub)
[ -z "$(git diff --name-only --diff-filter=U)" ] || fail "1: still conflicted"
[ "$(git -C ../sub_fork.git rev-parse feat)" = "$merged" ] || fail "1: feat not advanced to merge"
git -C ../sub_fork.git merge-base --is-ancestor "$fork_sha" "$merged" && git -C ../sub_fork.git merge-base --is-ancestor "$up_sha" "$merged" || fail "1: merge lacks a side"

# 2. Fork pointer is not a branch tip: merge goes to sync/upstream-<sha>, feat untouched
setup b=fork c=up not-tip
feat_before=$(git -C ../sub_fork.git rev-parse feat)
bash "$script" > /dev/null
[ "$(git -C ../sub_fork.git rev-parse "sync/upstream-${up_sha:0:8}")" = "$(git rev-parse :0:sub)" ] || fail "2: sync branch missing"
[ "$(git -C ../sub_fork.git rev-parse feat)" = "$feat_before" ] || fail "2: feat moved"

# 3. Both sides edit the same file in the submodule: script fails, superproject stays conflicted
setup a=fork a=up tip
! bash "$script" > /dev/null 2>&1 || fail "3: should fail"
[ -n "$(git diff --name-only --diff-filter=U)" ] || fail "3: conflict was hidden"

echo "all submodule resolution checks passed"
