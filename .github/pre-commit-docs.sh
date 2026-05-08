#!/bin/bash

# Pre-commit validation script for documentation version synchronization
# Validates that app/build.gradle.kts, CHANGELOG.md, and README.md all reference the same version

set -e  # Exit immediately if any command fails

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Validating documentation version synchronization...${NC}"

# Extract version from app/build.gradle.kts (format: versionName = "2.7.0")
if [ -f "app/build.gradle.kts" ]; then
    BUILD_VERSION=$(grep "versionName" app/build.gradle.kts | head -1 | sed 's/.*"\([^"]*\)".*/\1/')
    echo -e "Build version: ${GREEN}$BUILD_VERSION${NC}"
else
    echo -e "${RED}ERROR: app/build.gradle.kts not found${NC}"
    exit 1
fi

# Extract version from CHANGELOG.md (format: ## [2.7.0] or ## [X.Y.Z])
if [ -f "CHANGELOG.md" ]; then
    CHANGELOG_VERSION=$(grep "^## \[" CHANGELOG.md | head -1 | sed 's/^## \[\([^]]*\)].*/\1/')
    echo -e "CHANGELOG version: ${GREEN}$CHANGELOG_VERSION${NC}"
else
    echo -e "${RED}ERROR: CHANGELOG.md not found${NC}"
    exit 1
fi

# Extract version from README.md (format: vX.Y.Z)
if [ -f "README.md" ]; then
    README_VERSION=$(grep -oP "v\K[0-9]+\.[0-9]+\.[0-9]+" README.md | head -1)
    echo -e "README version: ${GREEN}$README_VERSION${NC}"
else
    echo -e "${RED}ERROR: README.md not found${NC}"
    exit 1
fi

# Validate versions match
ERRORS=0

if [ "$BUILD_VERSION" != "$CHANGELOG_VERSION" ]; then
    echo -e "${RED}ERROR: Version mismatch between app/build.gradle.kts and CHANGELOG.md${NC}"
    echo -e "  app/build.gradle.kts: $BUILD_VERSION"
    echo -e "  CHANGELOG.md: $CHANGELOG_VERSION"
    ERRORS=$((ERRORS + 1))
fi

if [ "$BUILD_VERSION" != "$README_VERSION" ]; then
    echo -e "${RED}ERROR: Version mismatch between app/build.gradle.kts and README.md${NC}"
    echo -e "  app/build.gradle.kts: $BUILD_VERSION"
    echo -e "  README.md: $README_VERSION"
    ERRORS=$((ERRORS + 1))
fi

if [ "$CHANGELOG_VERSION" != "$README_VERSION" ]; then
    echo -e "${RED}ERROR: Version mismatch between CHANGELOG.md and README.md${NC}"
    echo -e "  CHANGELOG.md: $CHANGELOG_VERSION"
    echo -e "  README.md: $README_VERSION"
    ERRORS=$((ERRORS + 1))
fi

# Exit with appropriate status
if [ $ERRORS -gt 0 ]; then
    echo ""
    echo -e "${RED}❌ Version synchronization check failed with $ERRORS error(s)${NC}"
    echo ""
    echo -e "${YELLOW}To fix version inconsistencies, update all files to match the correct version:${NC}"
    echo -e "  ${YELLOW}1. Bump versionName in app/build.gradle.kts (line 35)${NC}"
    echo -e "  ${YELLOW}2. Add CHANGELOG entry with same version${NC}"
    echo -e "  ${YELLOW}3. Update README badges and feature sections${NC}"
    exit 1
else
    echo ""
    echo -e "${GREEN}✅ All documentation files are synchronized to version $BUILD_VERSION${NC}"
    exit 0
fi
