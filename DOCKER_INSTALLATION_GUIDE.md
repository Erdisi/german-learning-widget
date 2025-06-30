# 🐳 Docker Installation Guide - German Learning Widget

## 📋 Prerequisites

This guide will help you install and configure Docker for the German Learning Widget development environment on **macOS (darwin 24.5.0)**.

---

## 🚀 Quick Installation (Recommended)

### Option 1: Docker Desktop (Easiest)

1. **Download Docker Desktop for Mac**
   ```bash
   # Visit the official Docker website
   open https://www.docker.com/products/docker-desktop/
   ```

2. **Install Docker Desktop**
   - Download the `.dmg` file for your Mac (Intel or Apple Silicon)
   - Open the `.dmg` file and drag Docker to Applications
   - Launch Docker Desktop from Applications

3. **Verify Installation**
   ```bash
   docker --version
   docker-compose --version
   ```

### Option 2: Homebrew Installation (Alternative)

```bash
# Install Docker via Homebrew
brew install --cask docker

# Start Docker Desktop
open -a Docker
```

---

## ⚙️ Docker Desktop Configuration

### Recommended Settings

1. **Open Docker Desktop Preferences**
   - Click Docker icon in menu bar → Preferences

2. **Resources Configuration**
   - **Memory**: 4 GB minimum (8 GB recommended)
   - **CPU**: 2 cores minimum (4 cores recommended)
   - **Disk**: 60 GB minimum (for Android SDK and builds)

3. **File Sharing**
   - Ensure your project directory is accessible
   - Add `/Users/erdisdriza/Desktop/de-schreiben` to shared paths

4. **Enable Features**
   - ✅ Use Docker Compose V2
   - ✅ Enable VirtioFS (if available - faster file sharing)
   - ✅ Use containerd for pulling and storing images

---

## 🧪 Verification & Testing

### 1. Basic Docker Test
```bash
# Test Docker installation
docker run hello-world

# Expected output: "Hello from Docker!"
```

### 2. Test Docker Compose
```bash
# Test Docker Compose
docker-compose --version

# Expected: docker-compose version 2.x.x
```

### 3. Project-Specific Test
```bash
cd /Users/erdisdriza/Desktop/de-schreiben/german-learning-widget

# Test our development helper
./docker-dev.sh status

# Should show Docker status as "running"
```

---

## 🛠️ German Learning Widget Setup

### First-Time Setup

1. **Navigate to Project Directory**
   ```bash
   cd /Users/erdisdriza/Desktop/de-schreiben/german-learning-widget
   ```

2. **Initialize Docker Environment**
   ```bash
   ./docker-dev.sh setup
   ```
   - This will build the development environment (takes 5-10 minutes first time)
   - Downloads Android SDK, Gradle, and dependencies
   - Creates necessary volumes and networks

3. **Verify Setup**
   ```bash
   ./docker-dev.sh status
   ```

### Quick Development Test

```bash
# Build a debug APK using Docker
./docker-dev.sh build-debug

# Expected: Success message with APK size and location
```

---

## 🔧 Troubleshooting

### Common Issues & Solutions

#### 1. Docker Desktop Won't Start
```bash
# Reset Docker Desktop
rm -rf ~/Library/Group\ Containers/group.com.docker
rm -rf ~/Library/Containers/com.docker.docker

# Restart Docker Desktop
open -a Docker
```

#### 2. Permission Issues
```bash
# Fix file permissions
sudo chown -R $USER:staff /Users/erdisdriza/Desktop/de-schreiben/german-learning-widget

# Add user to docker group (if needed)
sudo dscl . -append /Groups/_developer GroupMembership $USER
```

#### 3. Out of Disk Space
```bash
# Clean up Docker resources
docker system prune -af
docker volume prune -f

# Or use our helper script
./docker-dev.sh cleanup
```

#### 4. Slow Performance
- **Increase Docker Desktop memory** (Preferences → Resources)
- **Enable VirtioFS** for faster file sharing
- **Move project to faster disk** (SSD recommended)

#### 5. Network Issues
```bash
# Reset Docker network
docker network prune -f

# Restart Docker Desktop
pkill -f Docker && open -a Docker
```

---

## 📊 Performance Optimization

### For Apple Silicon Macs (M1/M2/M3)
```bash
# Verify you're using ARM64 images
docker info | grep Architecture

# Should show: Architecture: aarch64
```

### Build Performance Tips
1. **Increase Docker Resources**
   - Memory: 8 GB
   - CPU: 4+ cores
   - Swap: 2 GB

2. **Enable BuildKit**
   ```bash
   export DOCKER_BUILDKIT=1
   export COMPOSE_DOCKER_CLI_BUILD=1
   ```

3. **Use Multi-core Builds**
   ```bash
   # Already configured in our docker-compose.yml
   GRADLE_OPTS="-Dorg.gradle.parallel=true"
   ```

---

## 🔐 Security Considerations

### File Permissions
```bash
# Ensure proper permissions for keystore files
chmod 600 app/upload-keystore.jks  # (when you create it)

# Protect sensitive environment files
chmod 600 .env*
```

### Environment Variables
```bash
# For release builds, set these securely:
export KEYSTORE_PASSWORD="your_secure_password"
export KEY_ALIAS="upload"
export KEY_PASSWORD="your_key_password"

# Never commit these to git!
echo ".env*" >> .gitignore
```

---

## 📈 Monitoring & Maintenance

### Regular Maintenance Commands
```bash
# Weekly cleanup (recommended)
./docker-dev.sh cleanup

# Monthly image updates
./docker-dev.sh rebuild

# Check resource usage
./docker-dev.sh stats
```

### Monitoring Build Performance
```bash
# Profile builds
./docker-dev.sh build-debug

# Monitor resource usage during builds
docker stats
```

---

## 🆘 Getting Help

### Resources
1. **Docker Documentation**: https://docs.docker.com/desktop/mac/
2. **Project Issues**: Use `./docker-dev.sh help` for commands
3. **Community Support**: Docker Desktop community forums

### Contact & Support
- **Docker Issues**: Docker Desktop support
- **Project Issues**: Check `DOCKER_GUIDE.md` for detailed usage

---

## ✅ Success Checklist

After installation, you should be able to:

- [ ] **Docker Desktop is running** (`docker info` works)
- [ ] **Docker Compose is available** (`docker-compose --version`)
- [ ] **Project status is good** (`./docker-dev.sh status` shows all green)
- [ ] **Can build debug APK** (`./docker-dev.sh build-debug` succeeds)
- [ ] **Can run tests** (`./docker-dev.sh test` passes)

---

## 🎉 Next Steps

Once Docker is installed and working:

1. **Read the comprehensive guide**: `DOCKER_GUIDE.md`
2. **Start development**: `./docker-dev.sh dev`
3. **Set up CI/CD**: Configure GitHub secrets for automated builds
4. **Explore advanced features**: Multi-stage builds, caching, deployment

**Congratulations! You now have a professional Docker-based development environment for the German Learning Widget! 🚀** 