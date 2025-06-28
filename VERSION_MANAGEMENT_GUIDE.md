# 🚀 Version Management & Automated Branching Guide

This guide explains how to use the automated version branching system for the German Learning Widget project.

## 🎯 **OVERVIEW**

The project now supports **automatic version branch creation** through multiple methods:

1. **GitHub Actions** - Automatic branching on tag push or manual trigger
2. **Local Scripts** - Command-line tools for version management
3. **Git Aliases** - Quick shortcuts for common version operations
4. **Commit Triggers** - Auto-branching based on commit messages

## 🔧 **SETUP**

### Initial Setup (One-time)
```bash
# 1. Set up git aliases for convenience
./scripts/setup-git-aliases.sh

# 2. Ensure scripts are executable
chmod +x scripts/*.sh

# 3. Push the workflow to enable GitHub Actions
git add .github/workflows/
git commit -m "🔧 Add automated version branching system"
git push origin main
```

## 🚀 **USAGE METHODS**

### **Method 1: Local Script (Recommended for Development)**
```bash
# Create version branch with automatic versioning
./scripts/create-version-branch.sh 1.02

# This will:
# - Update app/build.gradle.kts with new version
# - Create release/v1.02 branch
# - Create v1.02 tag
# - Push everything to GitHub
# - Switch back to main branch
```

### **Method 2: Git Aliases (Quick & Easy)**
```bash
# After running setup-git-aliases.sh, you can use:

git create-version 1.02          # Create version branch
git version-commit 1.03 "New UI" # Commit with version trigger
git release 1.02                 # Create and push tag
git version-branches             # List all version branches
git switch-version 1.02          # Switch to version branch
```

### **Method 3: GitHub Actions (Automatic)**

#### Option A: Manual Trigger
1. Go to GitHub → Actions → "Automatic Version Branch Creator"
2. Click "Run workflow"
3. Enter version number (e.g., `1.02`)
4. Choose whether to create a release
5. Click "Run workflow"

#### Option B: Tag Push Trigger
```bash
# Create and push a tag - GitHub Actions will auto-create branch
git tag v1.02
git push origin v1.02
```

#### Option C: Commit Message Trigger
```bash
# Include [version:X.XX] in commit message
git commit -m "🎉 Add new features [version:1.02]"
git push origin main
```

### **Method 4: Manual Process**
```bash
# Traditional manual approach
git checkout -b release/v1.02
# Update version in app/build.gradle.kts manually
git add app/build.gradle.kts
git commit -m "🔖 Bump version to 1.02"
git push -u origin release/v1.02
git tag v1.02
git push origin v1.02
git checkout main
```

## 📋 **BRANCH NAMING CONVENTION**

- **Release Branches**: `release/v1.02`, `release/v2.0`, etc.
- **Tags**: `v1.02`, `v2.0`, etc.
- **Version Format**: `MAJOR.MINOR` (e.g., 1.02, 2.0, 10.5)

## 🔄 **WORKFLOW EXAMPLES**

### **Scenario 1: Regular Feature Release**
```bash
# 1. Develop features on main branch
git checkout main
git pull origin main

# 2. When ready for release, create version branch
git create-version 1.02

# 3. Branch is automatically created and ready for deployment
# 4. Continue development on main for next version
```

### **Scenario 2: Hotfix Release**
```bash
# 1. Create hotfix branch from existing release
git checkout release/v1.01
git checkout -b hotfix/v1.01.1

# 2. Fix the issue and commit
git add .
git commit -m "🐛 Fix critical bug"

# 3. Create new version
git create-version 1.01.1

# 4. Merge back to main if needed
git checkout main
git merge hotfix/v1.01.1
```

### **Scenario 3: Quick Version Bump**
```bash
# One-liner to commit and trigger auto-branching
git version-commit 1.03 "Add widget customization features"
git push origin main
# GitHub Actions will automatically create release/v1.03
```

## 🎯 **AUTOMATIC FEATURES**

### **Version Code Calculation**
The system automatically calculates Android version codes:
- Version 1.02 → Version Code 12 (1*10 + 2)
- Version 2.5 → Version Code 25 (2*10 + 5)
- Version 10.3 → Version Code 103 (10*10 + 3)

### **Automatic Updates**
When creating a version branch, the system automatically:
- ✅ Updates `versionName` in `app/build.gradle.kts`
- ✅ Updates `versionCode` in `app/build.gradle.kts`
- ✅ Creates release branch (`release/vX.XX`)
- ✅ Creates version tag (`vX.XX`)
- ✅ Pushes branch and tag to GitHub
- ✅ Triggers GitHub Actions for release creation
- ✅ Returns to main branch for continued development

### **GitHub Release Creation**
GitHub Actions automatically creates releases with:
- 📦 Release notes
- 🏷️ Version tags
- 📋 Build artifacts
- 🔗 Branch links
- ✅ Production-ready status

## 🛠️ **TROUBLESHOOTING**

### **Common Issues**

#### Branch Already Exists
```bash
# Error: Branch release/v1.02 already exists
# Solution: Use a different version number or delete existing branch
git branch -D release/v1.02  # Delete local branch
git push origin --delete release/v1.02  # Delete remote branch
```

#### Working Directory Not Clean
```bash
# Error: Working directory is not clean
# Solution: Commit or stash changes first
git add .
git commit -m "Save work in progress"
# Then try version creation again
```

#### Permission Issues
```bash
# Error: Permission denied
# Solution: Make scripts executable
chmod +x scripts/*.sh
```

## 📊 **MONITORING & VERIFICATION**

### **Check Version Branches**
```bash
git version-branches  # List all version branches
git branch -r | grep release  # List remote release branches
```

### **Verify GitHub Actions**
1. Go to GitHub → Actions tab
2. Check "Automatic Version Branch Creator" workflow
3. Verify successful runs and any errors

### **Check Tags**
```bash
git tag -l  # List all tags
git show v1.02  # Show tag details
```

## 🎉 **BEST PRACTICES**

1. **Always work on main** for new features
2. **Use semantic versioning** (1.02, 1.03, 2.0)
3. **Test before version creation** - ensure builds pass
4. **Create version branches** only when ready for release
5. **Use descriptive commit messages** with version triggers
6. **Monitor GitHub Actions** for any failures
7. **Keep version branches** for deployment and hotfixes

## 🚀 **QUICK REFERENCE**

| Command | Description |
|---------|-------------|
| `git create-version 1.02` | Create version branch locally |
| `git version-commit 1.02 "message"` | Commit with version trigger |
| `git release 1.02` | Create and push tag |
| `git version-branches` | List version branches |
| `git switch-version 1.02` | Switch to version branch |
| `./scripts/create-version-branch.sh 1.02` | Full version creation script |

---

**🎯 Ready to create your next version? Try: `git create-version 1.02`** 