# German Learning Widget - Docker Build Environment
# Multi-stage Dockerfile for Android CI/CD Pipeline

# =============================================================================
# Stage 1: Base Android Environment
# =============================================================================
FROM openjdk:17-jdk-slim as android-base

# Set environment variables
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin
ENV PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools
ENV PATH=$PATH:$ANDROID_SDK_ROOT/emulator

# Install system dependencies including libraries needed for AAPT2
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    curl \
    build-essential \
    libc6-dev \
    libbz2-1.0 \
    libncurses5 \
    libstdc++6 \
    zlib1g \
    file \
    && rm -rf /var/lib/apt/lists/*

# Create android-sdk directory
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools

# Download and install Android SDK Command Line Tools
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp \
    && mv /tmp/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

# Accept licenses and install required SDK components
RUN yes | sdkmanager --licenses
RUN sdkmanager \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;35.0.0" \
    "build-tools;34.0.0" \
    "cmake;3.22.1" \
    "ndk;26.1.10909125"

# Set up Android build environment for containerized builds
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Dorg.gradle.workers.max=2 -Dorg.gradle.jvmargs=-Xmx2g"
ENV ANDROID_BUILD_TOOLS_VERSION=35.0.0
ENV ANDROID_COMPILE_SDK=36
ENV ANDROID_TARGET_SDK=36
ENV ANDROID_MIN_SDK=21

# Configure AAPT2 for containerized builds (fix daemon issues)
ENV ANDROID_AAPT2_FROM_MAVEN=false
ENV GRADLE_OPTS="$GRADLE_OPTS -Dandroid.aapt2FromMavenOverride=/opt/android-sdk/build-tools/35.0.0/aapt2"

# =============================================================================
# Stage 2: Development Environment
# =============================================================================
FROM android-base as development

# Install additional development tools
RUN apt-get update && apt-get install -y \
    nano \
    vim \
    htop \
    && rm -rf /var/lib/apt/lists/*

# Create workspace
WORKDIR /workspace

# Copy Gradle wrapper and dependencies for caching
COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (for layer caching)
RUN ./gradlew --version

# =============================================================================
# Stage 3: CI/CD Build Environment
# =============================================================================
FROM android-base as ci-build

# Set working directory
WORKDIR /app

# Copy Gradle configuration first (for better caching)
COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY docker-gradle.properties gradle.properties
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY gradle/libs.versions.toml gradle/

# Make gradlew executable
RUN chmod +x gradlew

    # Download Gradle and dependencies
    RUN ./gradlew --version
    RUN ./gradlew dependencies

# Copy app configuration
COPY app/build.gradle.kts app/
COPY app/proguard-rules.pro app/

    # Download app dependencies
    RUN ./gradlew app:dependencies

# Copy source code
COPY app/src/ app/src/
COPY app/proguard-rules.pro app/

# =============================================================================
# Stage 4: Production Build
# =============================================================================
FROM ci-build as production

# Build arguments
ARG BUILD_TYPE=release
ARG KEYSTORE_PASSWORD=""
ARG KEY_ALIAS=""
ARG KEY_PASSWORD=""

# Copy any additional configuration files
COPY . .

# Clean and build
RUN ./gradlew clean

# Build debug APK by default
RUN if [ "$BUILD_TYPE" = "debug" ]; then \
        ./gradlew assembleDebug; \
    else \
        ./gradlew assembleRelease; \
    fi

# Create output directory
RUN mkdir -p /output

# Copy build artifacts
RUN if [ "$BUILD_TYPE" = "debug" ]; then \
        cp app/build/outputs/apk/debug/*.apk /output/ 2>/dev/null || true; \
    else \
        cp app/build/outputs/apk/release/*.apk /output/ 2>/dev/null || true; \
    fi

# Copy AAB files if they exist
RUN cp app/build/outputs/bundle/*/*.aab /output/ 2>/dev/null || true

# =============================================================================
# Stage 5: Final Runtime Image
# =============================================================================
FROM openjdk:17-jre-slim as runtime

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy built artifacts
COPY --from=production /output /artifacts

# Set working directory
WORKDIR /app

# Default command
CMD ["echo", "German Learning Widget Docker build completed successfully!"]

# =============================================================================
# Labels and Metadata
# =============================================================================
LABEL maintainer="German Learning Widget Team"
LABEL description="Docker environment for German Learning Widget Android app"
LABEL version="1.04"
LABEL project="german-learning-widget"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD echo "Docker container is healthy"

# Expose ports for development server (if needed)
EXPOSE 8080 8443 