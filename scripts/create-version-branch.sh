#!/bin/bash

# 🚀 German Learning Widget - Version Branch Creator
# Usage: ./scripts/create-version-branch.sh <version>
# Example: ./scripts/create-version-branch.sh 1.02

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if version is provided
if [ -z "$1" ]; then
    print_error "Version number required!"
    echo "Usage: $0 <version>"
    echo "Example: $0 1.02"
    exit 1
fi

VERSION="$1"
BRANCH_NAME="release/v$VERSION"
TAG_NAME="v$VERSION"

print_status "🚀 Creating version branch for v$VERSION"

# Validate version format
if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+$ ]]; then
    print_error "Invalid version format. Use format like 1.02, 2.0, etc."
    exit 1
fi

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    print_error "Not in a git repository!"
    exit 1
fi

# Check if we're on main branch
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    print_warning "Not on main branch (currently on: $CURRENT_BRANCH)"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Aborted."
        exit 0
    fi
fi

# Check if branch already exists locally
if git show-ref --verify --quiet refs/heads/$BRANCH_NAME; then
    print_error "Branch $BRANCH_NAME already exists locally!"
    exit 1
fi

# Check if branch exists on remote
if git ls-remote --heads origin $BRANCH_NAME | grep -q $BRANCH_NAME; then
    print_error "Branch $BRANCH_NAME already exists on remote!"
    exit 1
fi

# Ensure we have the latest changes
print_status "📥 Fetching latest changes..."
git fetch origin

# Ensure working directory is clean
if ! git diff-index --quiet HEAD --; then
    print_error "Working directory is not clean. Please commit or stash changes."
    exit 1
fi

# Update app version in build.gradle.kts
print_status "📝 Updating version in build.gradle.kts..."
if [ -f "app/build.gradle.kts" ]; then
    # Calculate version code (multiply by 10 and add minor version)
    MAJOR=$(echo $VERSION | cut -d. -f1)
    MINOR=$(echo $VERSION | cut -d. -f2)
    VERSION_CODE=$((MAJOR * 10 + MINOR))
    
    # Update version name and code
    sed -i.bak "s/versionCode = [0-9]*/versionCode = $VERSION_CODE/" app/build.gradle.kts
    sed -i.bak "s/versionName = \"[^\"]*\"/versionName = \"$VERSION\"/" app/build.gradle.kts
    rm app/build.gradle.kts.bak
    
    print_success "Updated version to $VERSION (code: $VERSION_CODE)"
else
    print_warning "app/build.gradle.kts not found, skipping version update"
fi

# Create and switch to new branch
print_status "🌿 Creating branch: $BRANCH_NAME"
git checkout -b $BRANCH_NAME

# Commit version changes if any
if ! git diff-index --quiet HEAD --; then
    print_status "📦 Committing version update..."
    git add app/build.gradle.kts
    git commit -m "🔖 Bump version to $VERSION

- Update version name to $VERSION
- Update version code to $VERSION_CODE
- Prepare for release branch: $BRANCH_NAME"
fi

# Push branch to remote
print_status "🚀 Pushing branch to remote..."
git push -u origin $BRANCH_NAME

# Create tag
print_status "🏷️ Creating tag: $TAG_NAME"
git tag -a $TAG_NAME -m "🎉 Release $TAG_NAME

Version: $VERSION
Branch: $BRANCH_NAME
Ready for production deployment"

# Push tag
print_status "📤 Pushing tag to remote..."
git push origin $TAG_NAME

# Switch back to main
print_status "🔄 Switching back to main branch..."
git checkout main

print_success "🎉 Version branch created successfully!"
echo
echo "📋 Summary:"
echo "  • Branch: $BRANCH_NAME"
echo "  • Tag: $TAG_NAME"
echo "  • Version: $VERSION"
echo "  • Remote: https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[:/]\([^.]*\).*/\1/')/tree/$BRANCH_NAME"
echo
echo "🚀 Next steps:"
echo "  1. The branch is ready for deployment"
echo "  2. GitHub Actions will automatically create a release"
echo "  3. You can continue development on main branch"
echo
print_success "All done! 🎯" 