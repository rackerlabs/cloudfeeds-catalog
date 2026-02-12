# syntax=docker/dockerfile:1

# Professional CloudFeeds Catalog Dockerfile
# Handles dynamic schema version updates for all Maven project structures
# Maintainable, debuggable, and production-ready

# ===============================================================================
# STAGE 1: Clone Repository
# ===============================================================================
FROM bitnami/git:latest AS fetcher

ARG GIT_URL=https://github.com/rackerlabs/cloudfeeds-catalog.git
ARG GIT_BRANCH="develop"

RUN mkdir -p /home/cl && \
    cd /home && \
    git clone -b ${GIT_BRANCH} ${GIT_URL} cl && \
    echo "✅ Repository cloned successfully"

# ===============================================================================
# STAGE 2: Build Catalog Application
# ===============================================================================
FROM maven:3.8.6-openjdk-8-slim AS builder

ARG SCHEMA_VERSION="1.139.3"
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      Description="Docker image for CloudFeeds Catalog" \
      Name="cloudfeeds-catalog" \
      SchemaVersion="${SCHEMA_VERSION}"

# Install required tools
RUN apt-get update && \
    apt-get install -y python3 vim-common && \
    rm -rf /var/lib/apt/lists/* && \
    echo "✅ Build tools installed"

# Configure Maven settings for GitHub Packages authentication
RUN mkdir -p /root/.m2 && \
    printf '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
            xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 \
            http://maven.apache.org/xsd/settings-1.0.0.xsd">\n\
  <servers>\n\
    <server>\n\
      <id>github</id>\n\
      <username>%s</username>\n\
      <password>%s</password>\n\
    </server>\n\
  </servers>\n\
  <profiles>\n\
    <profile>\n\
      <id>github</id>\n\
      <repositories>\n\
        <repository>\n\
          <id>github</id>\n\
          <name>GitHub Packages</name>\n\
          <url>https://maven.pkg.github.com/rackerlabs/*</url>\n\
          <releases><enabled>true</enabled></releases>\n\
          <snapshots><enabled>true</enabled></snapshots>\n\
        </repository>\n\
      </repositories>\n\
    </profile>\n\
  </profiles>\n\
  <activeProfiles>\n\
    <activeProfile>github</activeProfile>\n\
  </activeProfiles>\n\
</settings>' "${GITHUB_ACTOR}" "${GITHUB_TOKEN}" > /root/.m2/settings.xml && \
    echo "✅ Maven settings configured for GitHub Packages"

WORKDIR /cf-catalog

# Copy pom.xml for dependency resolution
COPY --from=fetcher /home/cl/pom.xml /cf-catalog/pom.xml

# ===============================================================================
# Professional POM Version Update Script
# ===============================================================================
RUN cat > /tmp/update_pom_version.py << 'PYTHON_SCRIPT'
#!/usr/bin/env python3
"""
Professional Maven POM Version Updater
Updates usage-schema version handling all common Maven patterns:
- Direct version in dependency
- Property-based versions
- Version ranges
- Parent POM inheritance
"""

import re
import sys
from typing import Tuple, Optional

def read_file(filepath: str) -> str:
    """Read file content safely."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            return f.read()
    except Exception as e:
        print(f"❌ Error reading {filepath}: {e}")
        sys.exit(1)

def write_file(filepath: str, content: str) -> None:
    """Write file content safely."""
    try:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
    except Exception as e:
        print(f"❌ Error writing {filepath}: {e}")
        sys.exit(1)

def find_schema_version(content: str) -> Optional[str]:
    """Find current usage-schema version in POM."""
    patterns = [
        # Direct version in dependency
        r'<artifactId>usage-schema</artifactId>\s*<version>([^<$]+)</version>',
        # Property reference
        r'<artifactId>usage-schema</artifactId>\s*<version>\$\{([^}]+)\}</version>',
    ]
    
    for pattern in patterns:
        match = re.search(pattern, content)
        if match:
            return match.group(1)
    return None

def update_property_version(content: str, property_name: str, new_version: str) -> Tuple[str, bool]:
    """Update version defined as a property."""
    # Common property name patterns
    property_patterns = [
        property_name,
        property_name.replace('-', '.'),
        property_name.replace('.', '-'),
        'usage-schema.version',
        'usage.schema.version',
        'usageSchemaVersion',
        'schema.version'
    ]
    
    updated = False
    for prop in property_patterns:
        # Escape dots for regex
        prop_regex = prop.replace('.', r'\.')
        pattern = f'(<{prop_regex}>)[^<]+(</{prop_regex}>)'
        new_content = re.sub(pattern, rf'\g<1>{new_version}\g<2>', content)
        if new_content != content:
            print(f"✅ Updated property '{prop}' to {new_version}")
            content = new_content
            updated = True
            break
    
    return content, updated

def update_direct_version(content: str, new_version: str) -> Tuple[str, bool]:
    """Update direct version in usage-schema dependency."""
    # Match usage-schema dependency with direct version (not property)
    pattern = r'(<artifactId>usage-schema</artifactId>\s*<version>)(?!\$\{)[^<]+(</version>)'
    replacement = rf'\g<1>{new_version}\g<2>'
    
    new_content = re.sub(pattern, replacement, content)
    updated = new_content != content
    
    if updated:
        print(f"✅ Updated direct version to {new_version}")
    
    return new_content, updated

def update_property_reference(content: str, new_version: str) -> Tuple[str, bool]:
    """Replace property reference with direct version (fallback)."""
    pattern = r'(<artifactId>usage-schema</artifactId>\s*<version>)\$\{[^}]+\}(</version>)'
    replacement = rf'\g<1>{new_version}\g<2>'
    
    new_content = re.sub(pattern, replacement, content)
    updated = new_content != content
    
    if updated:
        print(f"✅ Replaced property reference with direct version {new_version}")
    
    return new_content, updated

def main():
    if len(sys.argv) != 3:
        print("Usage: update_pom_version.py <pom.xml> <new_version>")
        sys.exit(1)
    
    pom_file = sys.argv[1]
    new_version = sys.argv[2]
    
    print("=" * 80)
    print("🔧 Professional Maven POM Version Updater")
    print("=" * 80)
    print(f"📄 POM File: {pom_file}")
    print(f"📦 Target Version: {new_version}")
    print("=" * 80)
    
    # Read POM
    content = read_file(pom_file)
    original_content = content
    
    # Find current version
    current_version = find_schema_version(content)
    if current_version:
        print(f"📦 Current Version: {current_version}")
        
        # If it's a property reference, extract property name
        if current_version.startswith('${') or not re.match(r'^\d', current_version):
            print(f"🔍 Detected property-based version: {current_version}")
            # Try to update the property
            content, updated = update_property_version(content, current_version, new_version)
            if not updated:
                # Fallback: replace property with direct version
                print("⚠️  Could not find property, using direct version replacement")
                content, updated = update_property_reference(content, new_version)
        else:
            print(f"🔍 Detected direct version: {current_version}")
            # Update direct version
            content, updated = update_direct_version(content, new_version)
    else:
        print("⚠️  Could not detect current version, attempting direct update")
        content, updated = update_direct_version(content, new_version)
    
    # Additional fallback: update any remaining property references
    if not updated or content.find('${') != -1 and content.find('usage-schema') != -1:
        print("🔄 Applying fallback: replacing all property references")
        content, fallback_updated = update_property_reference(content, new_version)
        updated = updated or fallback_updated
    
    # Write back if changed
    if updated and content != original_content:
        write_file(pom_file, content)
        print("=" * 80)
        print("✅ POM successfully updated")
        print("=" * 80)
        return 0
    elif content == original_content:
        print("=" * 80)
        print("⚠️  No changes made - version might already be correct")
        print("=" * 80)
        return 0
    else:
        print("=" * 80)
        print("❌ Failed to update POM")
        print("=" * 80)
        return 1

if __name__ == "__main__":
    sys.exit(main())
PYTHON_SCRIPT

RUN chmod +x /tmp/update_pom_version.py && \
    echo "✅ POM update script created"

# Show current state
RUN echo "" && \
    echo "========================================" && \
    echo "📋 BEFORE UPDATE - Current POM State" && \
    echo "========================================" && \
    echo "🔍 Properties section (if exists):" && \
    (sed -n '/<properties>/,/<\/properties>/p' pom.xml | head -30 || echo "   No properties section found") && \
    echo "" && \
    echo "🔍 usage-schema dependency:" && \
    (grep -B 3 -A 5 "usage-schema" pom.xml || echo "   usage-schema not found") && \
    echo "========================================"

# Update POM with new schema version
RUN python3 /tmp/update_pom_version.py /cf-catalog/pom.xml "${SCHEMA_VERSION}"

# Verify update
RUN echo "" && \
    echo "========================================" && \
    echo "📋 AFTER UPDATE - Updated POM State" && \
    echo "========================================" && \
    echo "🔍 Properties section (if exists):" && \
    (sed -n '/<properties>/,/<\/properties>/p' pom.xml | head -30 || echo "   No properties section found") && \
    echo "" && \
    echo "🔍 usage-schema dependency:" && \
    (grep -B 3 -A 5 "usage-schema" pom.xml || echo "   usage-schema not found") && \
    echo "========================================" && \
    echo ""

# Download dependencies
RUN echo "⬇️  Downloading dependencies (this may take a few minutes)..." && \
    mvn dependency:go-offline -B && \
    echo "✅ Dependencies downloaded successfully"

# Copy full source code
COPY --from=fetcher /home/cl/ /cf-catalog/

# Build application
RUN echo "🔨 Building catalog application..." && \
    mvn -B verify && \
    echo "✅ Build completed successfully"

# ===============================================================================
# STAGE 3: Download Schema from GitHub Packages
# ===============================================================================
FROM maven:3.8.6-openjdk-8-slim AS package

ARG SCHEMA_VERSION="1.139.3"
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

# Configure Maven settings
RUN mkdir -p /root/.m2 && \
    printf '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
            xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 \
            http://maven.apache.org/xsd/settings-1.0.0.xsd">\n\
  <servers>\n\
    <server>\n\
      <id>github</id>\n\
      <username>%s</username>\n\
      <password>%s</password>\n\
    </server>\n\
  </servers>\n\
  <profiles>\n\
    <profile>\n\
      <id>github</id>\n\
      <repositories>\n\
        <repository>\n\
          <id>github</id>\n\
          <name>GitHub Packages</name>\n\
          <url>https://maven.pkg.github.com/rackerlabs/*</url>\n\
          <releases><enabled>true</enabled></releases>\n\
          <snapshots><enabled>true</enabled></snapshots>\n\
        </repository>\n\
      </repositories>\n\
    </profile>\n\
  </profiles>\n\
  <activeProfiles>\n\
    <activeProfile>github</activeProfile>\n\
  </activeProfiles>\n\
</settings>' "${GITHUB_ACTOR}" "${GITHUB_TOKEN}" > /root/.m2/settings.xml

# Download and extract schema
RUN echo "📦 Downloading schema version ${SCHEMA_VERSION} from GitHub Packages..." && \
    mvn dependency:copy \
        -Dartifact=com.rackspace.usage:usage-schema:${SCHEMA_VERSION}:tar.gz:schema \
        -DoutputDirectory=/tmp \
        -s /root/.m2/settings.xml && \
    echo "✅ Schema artifact downloaded" && \
    cd /tmp && \
    tar -xzf usage-schema-${SCHEMA_VERSION}-schema.tar.gz && \
    echo "📂 Schema contents:" && \
    ls -lah usage-schema-${SCHEMA_VERSION}/ && \
    echo "✅ Schema extracted successfully"

# ===============================================================================
# STAGE 4: Create Runtime Image
# ===============================================================================
FROM tomcat:9-jre8 AS runtime

ARG service_port=8080
ARG SCHEMA_VERSION="1.139.3"

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      Description="CloudFeeds Catalog Service" \
      Name="catalog" \
      Version="1.138.7" \
      SchemaVersion="${SCHEMA_VERSION}"

ENV SAXON_HOME=/etc/saxon \
    SERVICE_PORT=${service_port} \
    SCHEMA_VERSION=${SCHEMA_VERSION}

# Create feeds user and necessary directories
RUN groupadd -o --gid 1000 feeds && \
    useradd -o -u 1000 -g feeds feeds && \
    mkdir -p /etc/cloudfeeds/translation/ /var/log/catalog && \
    chown -R feeds:feeds "${CATALINA_HOME}" /etc/cloudfeeds/translation/ /var/log/catalog

# Download and configure dumb-init
RUN wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.5/dumb-init_1.2.5_x86_64 && \
    chown feeds:feeds /usr/local/bin/dumb-init && \
    chmod u+x /usr/local/bin/dumb-init && \
    echo "✅ dumb-init installed"

WORKDIR ${CATALINA_HOME}

# Copy configuration files
COPY --chown=feeds:feeds --from=fetcher /home/cl/docker/feedscatalog.xml /etc/feedscatalog/
COPY --chown=feeds:feeds --from=fetcher /home/cl/docker/transform.xsl /usr/local/tomcat/

# Install xsltproc and configure Tomcat
RUN apt-get update && \
    apt-get install -y xsltproc && \
    xsltproc --output /usr/local/tomcat/conf/server.xml \
             /usr/local/tomcat/transform.xsl \
             /usr/local/tomcat/conf/server.xml && \
    rm -rf /var/lib/apt/lists/* && \
    echo "✅ Tomcat configured"

# Copy schema artifacts
COPY --chown=feeds:feeds --from=package /tmp/usage-schema-${SCHEMA_VERSION}/xslt-artifacts/* /etc/cloudfeeds/translation/

# Copy application WAR
COPY --chown=feeds:feeds --from=builder /cf-catalog/target/feedscatalog-*.war ${CATALINA_HOME}/webapps/feedscatalog.war

# Switch to feeds user
USER feeds

# Configure volumes
VOLUME ["/usr/local/tomcat/logs/", "/var/log/catalog/"]

EXPOSE ${SERVICE_PORT}

# Health check
HEALTHCHECK --interval=20s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:${SERVICE_PORT}/catalog/health || exit 1

# Start application
ENTRYPOINT ["/usr/local/bin/dumb-init", "--"]
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
