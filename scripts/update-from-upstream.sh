#!/bin/sh
# ============================================================
# OilVPN: get updates from the upstream project (mlmvpn/mlmvpn_android)
# ============================================================
# Usage:  ./scripts/update-from-upstream.sh
#
# What it does:
#   1. Fetches the latest source of the upstream project
#   2. If nothing new, exits right away
#   3. If there is, merges; auto-resolves known conflicts:
#      - Windscribe engine: the upstream copy (which needs the private
#        SDK) is dropped and the OilVPN stub is kept
#   4. Lists any remaining conflicts for manual resolution
# ============================================================
set -e

UPSTREAM="https://github.com/mlmvpn/mlmvpn_android.git"

echo "==> Fetching latest changes from the upstream project..."
git fetch upstream --tags

BASE=$(git merge-base main upstream/main)
UP=$(git rev-parse upstream/main)

if [ "$BASE" = "$UP" ]; then
    echo "OK: the upstream project has not changed - you are up to date."
    exit 0
fi

echo "==> New changes:"
echo "==> from: $(git log -1 --format='%s' $BASE)"
echo "==>   to: $(git log -1 --format='%s' $UP)"
echo "==> new commits: $(git rev-list --count main..upstream/main)"
echo ""

# Windscribe files: the upstream copy needs the private SDK (com.wsnet.lib)
# which is not in the public source - so always keep our own stub.
WS_PATHS="app/src/main/java/com/mlmvpn/scanner/engines/windscribe/ \
app/src/main/java/com/mlmvpn/scanner/ui/windscribe/ \
app/src/main/java/com/mlmvpn/scanner/engines/scout/adapters/WindscribeAdapter.kt \
app/src/debug/java/com/mlmvpn/scanner/wslab/"

if git merge upstream/main 2>/tmp/oilvpn_merge_err.txt; then
    echo "OK: merged with no conflicts."
else
    echo "==> Some files conflict; auto-resolving known ones..."

    # Auto-resolve: drop any upstream file under the Windscribe paths
    # (our stub stays) and continue the merge.
    for p in $WS_PATHS; do
        CONFLICTED=$(git diff --name-only --diff-filter=U | grep -F "$p" || true)
        for f in $CONFLICTED; do
            echo "   - $f : upstream copy dropped (OilVPN stub kept)"
            git rm -q "$f" 2>/dev/null || rm -f "$f"
        done
    done

    REMAINING=$(git diff --name-only --diff-filter=U || true)
    if [ -n "$REMAINING" ]; then
        echo ""
        echo "WARNING: these files need a manual conflict resolution:"
        echo "$REMAINING"
        echo ""
        echo "   How to resolve:"
        echo "   - Open the file, look at the <<<<<<< to >>>>>>> blocks"
        echo "   - Keep the OilVPN changes (OilVPN name, subscription gate, neon)"
        echo "   - Take new engine logic from the upstream side"
        echo "   - Then: git add <file> and finally git merge --continue"
        exit 1
    fi

    git commit --no-edit 2>/dev/null || true
    echo "OK: merge completed."
fi

echo ""
echo "Next steps:"
echo "  1. Build:  ./gradlew assembleDebug -PmlmQuick   (or with GitHub Actions)"
echo "  2. If a new official APK is out, build.yml fetches the latest native"
echo "     libs automatically - no manual edit needed."
echo "  3. Publish:  git push origin main"
