#!/bin/bash

# 🚀 German Learning Widget - Git Aliases Setup
# This script sets up convenient git aliases for version management

echo "🔧 Setting up Git aliases for version management..."

# Create version branch alias
git config alias.create-version '!f() { 
    if [ -z "$1" ]; then 
        echo "Usage: git create-version <version>"; 
        echo "Example: git create-version 1.02"; 
        return 1; 
    fi; 
    ./scripts/create-version-branch.sh "$1"; 
}; f'

# Quick version commit alias
git config alias.version-commit '!f() { 
    if [ -z "$1" ]; then 
        echo "Usage: git version-commit <version> [message]"; 
        echo "Example: git version-commit 1.02 \"Add new features\""; 
        return 1; 
    fi; 
    MESSAGE="${2:-Release version $1}"; 
    git add -A && git commit -m "🎉 $MESSAGE [version:$1]"; 
}; f'

# Quick tag and push alias
git config alias.release '!f() { 
    if [ -z "$1" ]; then 
        echo "Usage: git release <version>"; 
        echo "Example: git release 1.02"; 
        return 1; 
    fi; 
    git tag -a "v$1" -m "Release v$1" && git push origin "v$1"; 
}; f'

# List version branches
git config alias.version-branches '!git branch -a | grep -E "(release/|v[0-9])"'

# Switch to version branch
git config alias.switch-version '!f() { 
    if [ -z "$1" ]; then 
        echo "Usage: git switch-version <version>"; 
        echo "Example: git switch-version 1.02"; 
        return 1; 
    fi; 
    git checkout "release/v$1" 2>/dev/null || git checkout -b "release/v$1"; 
}; f'

echo "✅ Git aliases configured successfully!"
echo ""
echo "📋 Available aliases:"
echo "  • git create-version <version>     - Create new version branch with auto-versioning"
echo "  • git version-commit <version>     - Commit with version tag for auto-branching"
echo "  • git release <version>            - Create and push version tag"
echo "  • git version-branches             - List all version branches"
echo "  • git switch-version <version>     - Switch to or create version branch"
echo ""
echo "🚀 Example usage:"
echo "  git create-version 1.02            # Creates release/v1.02 branch"
echo "  git version-commit 1.03 \"New UI\"   # Commits with version trigger"
echo "  git release 1.02                   # Tags and pushes v1.02"
echo ""
echo "🎯 Ready to use! Try: git create-version 1.02" 