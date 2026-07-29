#!/usr/bin/env bash
set -euo pipefail

bump="${1:-patch}"
case "$bump" in
	major | minor | patch) ;;
	*)
		echo "Usage: $0 [major|minor|patch]" >&2
		exit 2
		;;
esac

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

branch="$(git symbolic-ref --quiet --short HEAD)" || {
	echo "Release must run from a branch." >&2
	exit 1
}

if ! git diff --quiet || ! git diff --cached --quiet; then
	echo "Commit or stash tracked changes before releasing." >&2
	exit 1
fi

current_version="$(sed -n 's/^mod_version=//p' gradle.properties)"
if [[ ! "$current_version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
	echo "Invalid mod_version in gradle.properties: $current_version" >&2
	exit 1
fi

major="${BASH_REMATCH[1]}"
minor="${BASH_REMATCH[2]}"
patch="${BASH_REMATCH[3]}"

case "$bump" in
	major) next_version="$((major + 1)).0.0" ;;
	minor) next_version="$major.$((minor + 1)).0" ;;
	patch) next_version="$major.$minor.$((patch + 1))" ;;
esac

tag="v$next_version"
if git rev-parse --quiet --verify "refs/tags/$tag" >/dev/null; then
	echo "Tag already exists: $tag" >&2
	exit 1
fi

sed -i "s/^mod_version=.*/mod_version=$next_version/" gradle.properties
git add gradle.properties
git commit -m "chore: release $tag"
git tag -a "$tag" -m "Release $tag"
git push --atomic origin "HEAD:refs/heads/$branch" "refs/tags/$tag:refs/tags/$tag"

echo "Released $tag. GitHub Actions will publish it after CI passes."
