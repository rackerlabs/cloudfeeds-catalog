# syntax=docker/dockerfile:1

# Build command:
# docker build -t catalog:v1 -f Dockerfile --build-arg SCHEMA_VERSION=1.139.3 --build-arg GITHUB_ACTOR=username --build-arg GITHUB_TOKEN=token .

# =============== Clone Cloudfeeds-catalog repo ===============
FROM bitnami/git:latest AS fetcher

ARG GIT_URL=https://github.com/rackerlabs/cloudfeeds-catalog.git
ARG GIT_BRANCH="develop"

RUN mkdir -p /home/cl && \
    cd /home && \
    git clone -b ${GIT_BRANCH} ${GIT_URL} cl

# ======================== Build cloudfeeds catalog using maven =======================
FROM maven:3.8.6-openjdk-8-slim AS builder

ARG SCHEMA_VERSION="1.139.3"
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      description="Docker image for Cloudfeeds Catalog" \
      NAME="cloudfeeds-catalog"

# Create Maven settings for GitHub Packages with direct substitution
RUN mkdir -p /root/.m2 && \
    printf '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">\n\
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

WORKDIR /cf-catalog
COPY --from=fetcher /home/cl/pom.xml /cf-catalog/pom.xml

# UPDATE SCHEMA VERSION IN POM.XML BEFORE DOWNLOADING DEPENDENCIES
RUN echo "🔧 Updating schema version to ${SCHEMA_VERSION} in pom.xml..." && \
    mvn versions:use-dep-version \
    -Dincludes=com.rackspace.usage:usage-schema \
    -DdepVersion=${SCHEMA_VERSION} \
    -DforceVersion=true \
    -DgenerateBackupPoms=false && \
    echo "✅ Schema version updated"

# Verify the change
RUN echo "📦 Current schema version in pom.xml:" && \
    grep -A 2 "usage-schema" /cf-catalog/pom.xml || echo "Schema dependency not found in pom.xml"

# Download dependencies with the correct schema version
RUN echo "⬇️ Downloading dependencies..." && \
    mvn dependency:go-offline && \
    echo "✅ Dependencies downloaded"

COPY --from=fetcher /home/cl/ /cf-catalog
RUN mvn -B verify

# ======================== Download schema from GitHub Packages =======================
FROM maven:3.8.6-openjdk-8-slim as package

ARG SCHEMA_VERSION="1.139.3"
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

# Create Maven settings for GitHub Packages with direct substitution
RUN mkdir -p /root/.m2 && \
    printf '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">\n\
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

# Download schema artifact from GitHub Packages using Maven
RUN echo "📦 Downloading schema ${SCHEMA_VERSION} from GitHub Packages..." && \
    mvn dependency:copy \
    -Dartifact=com.rackspace.usage:usage-schema:${SCHEMA_VERSION}:tar.gz:schema \
    -DoutputDirectory=/tmp \
    -s /root/.m2/settings.xml && \
    echo "✅ Schema downloaded"

# Extract schema
RUN cd /tmp && \
    tar -xzf usage-schema-${SCHEMA_VERSION}-schema.tar.gz && \
    ls -la usage-schema-${SCHEMA_VERSION}/ && \
    echo "✅ Schema extracted"

# ======================== Create deploy Image =======================
FROM tomcat:9-jre8 as tomcat

ARG service_port=8080
ARG saxon_lic
ARG SCHEMA_VERSION="1.139.3"

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
