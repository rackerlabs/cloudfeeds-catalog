# syntax=docker/dockerfile:1

# Build command:
# docker build -t catalog:v1 -f Dockerfile --build-arg SCHEMA_VERSION=1.138.6 .

# =============== Clone Cloudfeeds-catalog repo ===============
FROM bitnami/git:latest AS fetcher

ARG GIT_URL=https://github.com/rackerlabs/cloudfeeds-catalog.git
ARG GIT_BRANCH="develop"

RUN mkdir -p /home/cl && \
    cd /home && \
    git clone -b ${GIT_BRANCH} ${GIT_URL} cl

# ======================== Build cloudfeeds catalog using maven =======================
FROM maven:3.8.6-openjdk-8-slim AS builder

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      description="Docker image for Cloudfeeds Catalog" \
      NAME="cloudfeeds-catalog"

# Create complete Maven settings for GitHub Packages
RUN mkdir -p /root/.m2 && \
    cat > /root/.m2/settings.xml << 'SETTINGS_EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <servers>
    <server>
      <id>github</id>
      <username>GITHUB_ACTOR_PLACEHOLDER</username>
      <password>GITHUB_TOKEN_PLACEHOLDER</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>github</id>
          <url>https://maven.pkg.github.com/rackerlabs/*</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>
</settings>
SETTINGS_EOF

# Replace placeholders with actual values
RUN sed -i "s/GITHUB_ACTOR_PLACEHOLDER/${GITHUB_ACTOR}/g" /root/.m2/settings.xml && \
    sed -i "s/GITHUB_TOKEN_PLACEHOLDER/${GITHUB_TOKEN}/g" /root/.m2/settings.xml

WORKDIR /cf-catalog
COPY --from=fetcher /home/cl/pom.xml /cf-catalog/pom.xml

RUN mvn dependency:go-offline

COPY --from=fetcher /home/cl/ /cf-catalog
RUN mvn -B verify

# ======================== Download schema from GitHub Packages =======================
FROM maven:3.8.6-openjdk-8-slim as package

ARG SCHEMA_VERSION="1.139.3"
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

# Create complete Maven settings for GitHub Packages
RUN mkdir -p /root/.m2 && \
    cat > /root/.m2/settings.xml << 'SETTINGS_EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <servers>
    <server>
      <id>github</id>
      <username>GITHUB_ACTOR_PLACEHOLDER</username>
      <password>GITHUB_TOKEN_PLACEHOLDER</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>github</id>
          <url>https://maven.pkg.github.com/rackerlabs/*</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>
</settings>
SETTINGS_EOF

# Replace placeholders with actual values
RUN sed -i "s/GITHUB_ACTOR_PLACEHOLDER/${GITHUB_ACTOR}/g" /root/.m2/settings.xml && \
    sed -i "s/GITHUB_TOKEN_PLACEHOLDER/${GITHUB_TOKEN}/g" /root/.m2/settings.xml

# Download schema artifact from GitHub Packages using Maven
RUN mvn dependency:copy \
    -Dartifact=com.rackspace.usage:usage-schema:${SCHEMA_VERSION}:tar.gz:schema \
    -DoutputDirectory=/tmp \
    -s /root/.m2/settings.xml

# Extract schema
RUN cd /tmp && \
    tar -xzf usage-schema-${SCHEMA_VERSION}-schema.tar.gz && \
    ls -la usage-schema-${SCHEMA_VERSION}/

# ======================== Create deploy Image =======================
FROM tomcat:9-jre8 as tomcat

ARG service_port=8080
ARG saxon_lic
ARG SCHEMA_VERSION="1.138.6"

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      description="Docker image for Cloudfeeds Catalog" \
      NAME="catalog"

ENV SAXON_HOME=/etc/saxon \
    SERVICE_PORT=${SERVICE_PORT:-$service_port}

RUN   groupadd -o --gid 1000 feeds && \
      useradd -o -u 1000 -g feeds feeds  && \
      mkdir -p /etc/cloudfeeds/translation/ && \
      chown -R feeds:feeds "${CATALINA_HOME}" /etc/cloudfeeds/translation/  && \
      wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.5/dumb-init_1.2.5_x86_64  && \
      chown feeds:feeds /usr/local/bin/dumb-init  && \
      chmod u+x /usr/local/bin/dumb-init

WORKDIR ${CATALINA_HOME}

COPY --chown=feeds:feeds --from=fetcher /home/cl/docker/feedscatalog.xml /etc/feedscatalog/
COPY --chown=feeds:feeds --from=fetcher /home/cl/docker/transform.xsl /usr/local/tomcat

RUN   apt-get update  && \
      yes | apt-get install xsltproc  && \
      xsltproc --output /usr/local/tomcat/conf/server.xml /usr/local/tomcat/transform.xsl /usr/local/tomcat/conf/server.xml && \
      rm -rf /var/lib/apt/lists/*

USER feeds:feeds
VOLUME /usr/local/tomcat/logs/

USER feeds
VOLUME /usr/local/tomcat/logs/ /var/log/catalog/

# Copy schema from package stage
COPY --chown=feeds:feeds --from=package /tmp/usage-schema-${SCHEMA_VERSION}/xslt-artifacts/* /etc/cloudfeeds/translation/

# Copy built WAR file
COPY --chown=feeds:feeds --from=builder /cf-catalog/target/feedscatalog-*.war ${CATALINA_HOME}/webapps/feedscatalog.war

EXPOSE ${SERVICE_PORT}

# Perform health check
HEALTHCHECK --interval=20s --timeout=5s --start-period=10s --retries=3 \
      CMD curl -L --fail --silent --show-error --connect-timeout 3 --max-time 3 http://localhost:${SERVICE_PORT}/catalog || exit 1

#Start tomcat server
ENTRYPOINT ["/usr/local/bin/dumb-init", "--"]
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
