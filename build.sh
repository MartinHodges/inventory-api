#!/bin/bash

# Docker Build and Push Script for AMD64 Linux
# Usage: ./build.sh [OPTIONS]
#
# Options:
#   -i, --image IMAGE_NAME    Image name (e.g., myregistry.com/all-i-want or all-i-want)
#   -t, --tag TAG            Tag for the image (default: git commit hash or 'latest')
#   -a, --also-tag TAG       Additional tag to apply (can be used multiple times)
#   -r, --registry REGISTRY  Container registry (e.g., docker.io, ghcr.io)
#   --no-push                Build only, don't push to registry
#   --no-cache               Build without using cache
#   --no-rollout             Skip rollout after successful push
#   -h, --help               Show this help message

set -e  # Exit on error

# Default values
IMAGE_NAME="martinhodges/inventory-api"
TAG="latest"
ADDITIONAL_TAGS=()
REGISTRY=""
PUSH=true
ROLLOUT=true
NO_CACHE=""
PLATFORM="linux/amd64"
SSH_HOST="kates"
SSH_ROLLOUT_COMMAND="~/k8s/inventory/rollout.sh api"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -i|--image)
      IMAGE_NAME="$2"
      shift 2
      ;;
    -t|--tag)
      TAG="$2"
      shift 2
      ;;
    -a|--also-tag)
      ADDITIONAL_TAGS+=("$2")
      shift 2
      ;;
    -r|--registry)
      REGISTRY="$2"
      shift 2
      ;;
    --no-push)
      PUSH=false
      shift
      ;;
    --no-rollout)
      ROLLOUT=false
      shift
      ;;
    --no-cache)
      NO_CACHE="--no-cache"
      shift
      ;;
    -h|--help)
      grep "^#" "$0" | grep -E "^# (Usage|Options)" -A 100 | tail -n +1 | sed 's/^# \?//'
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Use -h or --help for usage information"
      exit 1
      ;;
  esac
done

# Validate required parameters
if [ -z "$IMAGE_NAME" ]; then
  echo "Error: Image name is required (-i or --image)"
  echo "Use -h or --help for usage information"
  exit 1
fi

# Determine tag if not specified
if [ -z "$TAG" ]; then
  if git rev-parse --git-dir > /dev/null 2>&1; then
    TAG=$(git rev-parse --short HEAD)
    echo "Using git commit hash as tag: $TAG"
  else
    TAG="latest"
    echo "Not a git repository, using tag: $TAG"
  fi
fi

# Build full image name
if [ -n "$REGISTRY" ]; then
  FULL_IMAGE="${REGISTRY}/${IMAGE_NAME}"
else
  FULL_IMAGE="${IMAGE_NAME}"
fi

MAIN_TAG="${FULL_IMAGE}:${TAG}"

echo "======================================"
echo "Docker Build Configuration"
echo "======================================"
echo "Platform: $PLATFORM"
echo "Image: $FULL_IMAGE"
echo "Main tag: $TAG"
if [ ${#ADDITIONAL_TAGS[@]} -gt 0 ]; then
  echo "Additional tags: ${ADDITIONAL_TAGS[*]}"
fi
echo "Push to registry: $PUSH"
echo "======================================"
echo ""

# Build the image
echo "Building Docker image for $PLATFORM..."
docker build \
  --platform "$PLATFORM" \
  $NO_CACHE \
  -t "$MAIN_TAG" \
  .

echo "✓ Build completed: $MAIN_TAG"

# Tag with additional tags
for ADDITIONAL_TAG in "${ADDITIONAL_TAGS[@]}"; do
  ADDITIONAL_FULL_TAG="${FULL_IMAGE}:${ADDITIONAL_TAG}"
  echo "Tagging image: $ADDITIONAL_FULL_TAG"
  docker tag "$MAIN_TAG" "$ADDITIONAL_FULL_TAG"
done

# Push to registry if requested
if [ "$PUSH" = true ]; then
  echo ""
  echo "Pushing to registry..."
  docker push "$MAIN_TAG"
  echo "✓ Pushed: $MAIN_TAG"

  for ADDITIONAL_TAG in "${ADDITIONAL_TAGS[@]}"; do
    ADDITIONAL_FULL_TAG="${FULL_IMAGE}:${ADDITIONAL_TAG}"
    docker push "$ADDITIONAL_FULL_TAG"
    echo "✓ Pushed: $ADDITIONAL_FULL_TAG"
  done
else
  echo ""
  echo "Skipping push (--no-push specified)"
fi

# Trigger rollout if requested
if [ "$ROLLOUT" = true ] && [ "$PUSH" = true ]; then
  echo ""
  echo "======================================"
  echo "Triggering rollout on kates..."
  echo "======================================"
  if ssh $SSH_HOST "$SSH_ROLLOUT_COMMAND"; then
    echo "✓ Rollout completed successfully"
  else
    echo "✗ Rollout failed"
    exit 1
  fi
elif [ "$ROLLOUT" = false ]; then
  echo ""
  echo "Skipping rollout (--no-rollout specified)"
elif [ "$PUSH" = false ]; then
  echo ""
  echo "Skipping rollout (--no-push specified)"
fi

echo ""
echo "======================================"
echo "✓ Docker build completed successfully"
echo "======================================"
echo "Image: $MAIN_TAG"
if [ ${#ADDITIONAL_TAGS[@]} -gt 0 ]; then
  for ADDITIONAL_TAG in "${ADDITIONAL_TAGS[@]}"; do
    echo "       ${FULL_IMAGE}:${ADDITIONAL_TAG}"
  done
fi
