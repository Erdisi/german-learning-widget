#!/bin/bash
# German Learning Widget - Docker Development Helper Script
# Provides easy commands for Docker-based development workflow

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Project configuration
PROJECT_NAME="German Learning Widget"
COMPOSE_PROJECT_NAME="german-learning-widget"

# Helper functions
print_header() {
    echo -e "${BLUE}🐳 $PROJECT_NAME - Docker Development Helper${NC}"
    echo -e "${BLUE}================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${PURPLE}ℹ️  $1${NC}"
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker Desktop."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker Desktop."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed."
        exit 1
    fi
}

show_usage() {
    print_header
    echo ""
    echo -e "${YELLOW}Usage: $0 [command]${NC}"
    echo ""
    echo -e "${BLUE}Development Commands:${NC}"
    echo -e "  ${GREEN}setup${NC}           - Initial setup and build development environment"
    echo -e "  ${GREEN}dev${NC}             - Start interactive development container"
    echo -e "  ${GREEN}build-debug${NC}     - Build debug APK"
    echo -e "  ${GREEN}build-release${NC}   - Build release APK (requires signing setup)"
    echo -e "  ${GREEN}test${NC}            - Run unit tests and lint analysis"
    echo -e "  ${GREEN}clean${NC}           - Clean build outputs"
    echo ""
    echo -e "${BLUE}Advanced Commands:${NC}"
    echo -e "  ${GREEN}rebuild${NC}         - Rebuild all Docker images from scratch"
    echo -e "  ${GREEN}shell${NC}           - Start bash shell in development container"
    echo -e "  ${GREEN}logs${NC}            - Show logs from last build"
    echo -e "  ${GREEN}stats${NC}           - Show Docker resource usage"
    echo -e "  ${GREEN}cleanup${NC}         - Clean up Docker resources"
    echo ""
    echo -e "${BLUE}Utility Commands:${NC}"
    echo -e "  ${GREEN}status${NC}          - Show Docker and project status"
    echo -e "  ${GREEN}help${NC}            - Show this help message"
    echo ""
    echo -e "${PURPLE}Examples:${NC}"
    echo -e "  $0 setup           # First-time setup"
    echo -e "  $0 build-debug     # Quick debug build"
    echo -e "  $0 test            # Run all tests"
    echo -e "  $0 dev             # Start development session"
    echo ""
}

setup_environment() {
    print_info "Setting up Docker development environment..."
    
    # Check if we need to build
    if ! docker-compose images -q dev &> /dev/null; then
        print_info "Building development environment (this may take a few minutes)..."
        docker-compose build dev
        print_success "Development environment built successfully!"
    else
        print_info "Development environment already exists."
    fi
    
    # Create necessary directories
    mkdir -p app/build/outputs
    mkdir -p artifacts
    
    print_success "Setup completed! You can now use: $0 dev"
}

start_development() {
    print_info "Starting interactive development container..."
    print_info "Use './gradlew assembleDebug' inside container to build"
    print_info "Press Ctrl+C to exit"
    
    docker-compose run --rm dev bash
}

build_debug() {
    print_info "Building debug APK using Docker..."
    
    docker-compose run --rm ci-build ./gradlew assembleDebug --no-daemon --stacktrace
    
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        APK_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
        print_success "Debug APK built successfully! Size: $APK_SIZE"
        print_info "Location: app/build/outputs/apk/debug/app-debug.apk"
    else
        print_error "Debug APK build failed!"
        exit 1
    fi
}

build_release() {
    print_info "Building release APK using Docker..."
    
    # Check if signing is configured
    if [[ -z "$KEYSTORE_PASSWORD" || -z "$KEY_ALIAS" || -z "$KEY_PASSWORD" ]]; then
        print_warning "Release signing not configured!"
        print_info "Set these environment variables:"
        print_info "  export KEYSTORE_PASSWORD='your_password'"
        print_info "  export KEY_ALIAS='upload'"
        print_info "  export KEY_PASSWORD='your_key_password'"
        print_info ""
        print_info "Building unsigned release..."
    fi
    
    docker-compose run --rm \
        -e KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-}" \
        -e KEY_ALIAS="${KEY_ALIAS:-}" \
        -e KEY_PASSWORD="${KEY_PASSWORD:-}" \
        production-build
    
    if [ -f "app/build/outputs/apk/release"/*.apk ]; then
        APK_SIZE=$(du -h app/build/outputs/apk/release/*.apk | cut -f1)
        print_success "Release APK built successfully! Size: $APK_SIZE"
        print_info "Location: app/build/outputs/apk/release/"
    else
        print_error "Release APK build failed!"
        exit 1
    fi
}

run_tests() {
    print_info "Running tests and lint analysis..."
    
    docker-compose run --rm test ./gradlew test lint --no-daemon --stacktrace
    
    print_success "Tests completed!"
    print_info "Test reports: app/build/reports/tests/"
    print_info "Lint reports: app/build/reports/lint-results.html"
}

clean_build() {
    print_info "Cleaning build outputs..."
    
    docker-compose run --rm ci-build ./gradlew clean --no-daemon
    
    # Also clean local build directories
    rm -rf app/build
    rm -rf build
    
    print_success "Build outputs cleaned!"
}

rebuild_images() {
    print_info "Rebuilding all Docker images from scratch..."
    print_warning "This will take several minutes..."
    
    docker-compose build --no-cache --pull
    
    print_success "All Docker images rebuilt!"
}

start_shell() {
    print_info "Starting bash shell in development container..."
    
    docker-compose run --rm dev bash
}

show_logs() {
    print_info "Showing recent Docker logs..."
    
    docker-compose logs --tail=50
}

show_stats() {
    print_info "Docker resource usage:"
    
    echo ""
    echo -e "${BLUE}Container Stats:${NC}"
    docker stats --no-stream 2>/dev/null || print_warning "No running containers"
    
    echo ""
    echo -e "${BLUE}Images:${NC}"
    docker images | grep -E "(german-learning-widget|openjdk|gradle)" || true
    
    echo ""
    echo -e "${BLUE}Volumes:${NC}"
    docker volume ls | grep german-learning-widget || true
    
    echo ""
    echo -e "${BLUE}Disk Usage:${NC}"
    docker system df
}

cleanup_docker() {
    print_warning "This will remove unused Docker resources..."
    read -p "Continue? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "Cleaning up Docker resources..."
        
        # Stop any running containers
        docker-compose down 2>/dev/null || true
        
        # Remove unused containers, networks, images
        docker system prune -f
        
        # Remove unused volumes (be careful with this)
        docker volume prune -f
        
        print_success "Docker cleanup completed!"
    else
        print_info "Cleanup cancelled."
    fi
}

show_status() {
    print_header
    echo ""
    
    # Docker status
    echo -e "${BLUE}Docker Status:${NC}"
    if docker info &> /dev/null; then
        print_success "Docker is running"
        echo -e "  Version: $(docker --version | cut -d' ' -f3 | tr -d ',')"
        echo -e "  Compose: $(docker-compose --version | cut -d' ' -f4 | tr -d ',')"
    else
        print_error "Docker is not running"
    fi
    
    echo ""
    
    # Project status
    echo -e "${BLUE}Project Status:${NC}"
    if [ -f "Dockerfile" ]; then
        print_success "Dockerfile exists"
    else
        print_error "Dockerfile missing"
    fi
    
    if [ -f "docker-compose.yml" ]; then
        print_success "Docker Compose configuration exists"
    else
        print_error "docker-compose.yml missing"
    fi
    
    # Check for built images
    if docker-compose images -q dev &> /dev/null; then
        print_success "Development environment is ready"
    else
        print_warning "Development environment not built yet"
        print_info "Run: $0 setup"
    fi
    
    echo ""
    
    # Recent builds
    echo -e "${BLUE}Recent Builds:${NC}"
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        DEBUG_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
        DEBUG_DATE=$(stat -c %y app/build/outputs/apk/debug/app-debug.apk 2>/dev/null | cut -d' ' -f1)
        print_success "Debug APK: $DEBUG_SIZE ($DEBUG_DATE)"
    else
        print_info "No debug APK found"
    fi
    
    if [ -f app/build/outputs/apk/release/*.apk ]; then
        RELEASE_SIZE=$(du -h app/build/outputs/apk/release/*.apk | cut -f1)
        RELEASE_DATE=$(stat -c %y app/build/outputs/apk/release/*.apk 2>/dev/null | cut -d' ' -f1)
        print_success "Release APK: $RELEASE_SIZE ($RELEASE_DATE)"
    else
        print_info "No release APK found"
    fi
}

# Main script logic
main() {
    # Check Docker availability
    check_docker
    
    # Handle commands
    case "${1:-help}" in
        "setup")
            setup_environment
            ;;
        "dev")
            start_development
            ;;
        "build-debug")
            build_debug
            ;;
        "build-release")
            build_release
            ;;
        "test")
            run_tests
            ;;
        "clean")
            clean_build
            ;;
        "rebuild")
            rebuild_images
            ;;
        "shell")
            start_shell
            ;;
        "logs")
            show_logs
            ;;
        "stats")
            show_stats
            ;;
        "cleanup")
            cleanup_docker
            ;;
        "status")
            show_status
            ;;
        "help"|"--help"|"-h")
            show_usage
            ;;
        *)
            print_error "Unknown command: $1"
            echo ""
            show_usage
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@" 