# 🐳 Docker Quick Reference - German Learning Widget

## 🚦 Platform Compatibility Status

| Platform | Status | Recommendation | Build Time |
|----------|--------|---------------|------------|
| **Apple Silicon Mac (M1/M2/M3)** | ❌ AAPT2 Issue | Use host system | ~19 seconds |
| **Intel Mac** | ✅ Works | Docker or host | ~5-7 minutes |
| **Linux (x86_64)** | ✅ Works | Docker preferred | ~5-7 minutes |
| **Windows WSL2** | ✅ Works | Docker preferred | ~5-7 minutes |
| **CI/CD (GitHub Actions)** | ✅ Works | Always Docker | ~6-8 minutes |

## ⚡ Quick Commands

### Development Commands
```bash
# Check status and get help
./docker-dev.sh status
./docker-dev.sh help

# Setup and build
./docker-dev.sh setup        # First time setup
./docker-dev.sh build-debug  # Build debug APK
./docker-dev.sh test         # Run tests

# Development environment
./docker-dev.sh dev          # Interactive container
```

### Build Commands
```bash
# Debug builds
docker-compose run --rm ci-build ./gradlew assembleDebug

# Release builds (requires signing)
docker-compose run --rm production-build

# Testing
docker-compose run --rm test ./gradlew test lint
```

### Maintenance Commands
```bash
# Clean up
./docker-dev.sh cleanup      # Remove unused resources
./docker-dev.sh rebuild      # Rebuild from scratch

# Monitoring
./docker-dev.sh stats        # Performance monitoring
docker system df             # Check disk usage
```

## 🔧 Troubleshooting

### Apple Silicon Mac Users
```bash
# Don't use Docker - use host builds instead
./gradlew assembleDebug      # Works perfectly
./gradlew test              # Fast and reliable
```

### Docker Issues
```bash
# Out of disk space
docker system prune -af && docker volume prune -f

# Permission issues (Linux/macOS)
sudo chown -R $USER:$USER .

# Rebuild everything
docker-compose build --no-cache
```

### Performance Issues
- **Memory**: Increase Docker Desktop to 8GB
- **Disk**: Ensure 10GB+ free space
- **CPU**: Allocate 4+ cores to Docker

## 📁 Key Files

| File | Purpose |
|------|---------|
| `Dockerfile` | Multi-stage build configuration |
| `docker-compose.yml` | Service orchestration |
| `docker-dev.sh` | Interactive development helper |
| `.dockerignore` | Build context optimization |
| `docker-gradle.properties` | Container-specific Gradle config |

## 🚀 CI/CD Pipeline

The GitHub Actions workflow automatically:
- ✅ Builds debug and release APKs
- ✅ Runs tests and linting
- ✅ Performs security scanning
- ✅ Can deploy to Play Store
- ✅ Generates build artifacts

Trigger: Push to `main` or `version-*` branches

## 🎯 When to Use Docker

### ✅ Always Use Docker
- CI/CD pipelines
- Team collaboration (non-Apple Silicon)
- Release builds (automated)
- Testing automation

### ⚠️ Conditional Use
- Local development (platform dependent)
- Debugging complex build issues
- Environment isolation needs

### ❌ Don't Use Docker
- Apple Silicon Mac local development
- Quick iterative development (on compatible platforms, host is faster)

## 📚 Documentation Links

- **[Complete Docker Guide](DOCKER_GUIDE.md)** - Comprehensive usage instructions
- **[Installation Guide](DOCKER_INSTALLATION_GUIDE.md)** - Platform-specific setup
- **[Implementation Summary](DOCKER_IMPLEMENTATION_SUMMARY.md)** - Technical overview
- **[Project Requirements](PRD.md)** - Full technical specification

## 🔍 Quick Diagnostics

### Check Docker Status
```bash
./docker-dev.sh status      # Project-specific status
docker --version            # Docker version
docker-compose --version    # Compose version
docker system df            # Disk usage
```

### Verify Setup
```bash
docker run hello-world      # Basic Docker test
./docker-dev.sh setup       # Project setup
./docker-dev.sh build-debug # Test build
```

### Get Help
```bash
./docker-dev.sh help        # All available commands
docker-compose --help       # Compose help
docker --help               # Docker help
```

---

**💡 Tip**: For Apple Silicon users, this Docker infrastructure is still valuable for CI/CD automation even if you can't use it locally. Your commits will trigger automated builds that work perfectly on x86_64 servers! 