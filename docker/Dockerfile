# ======================== STAGE 1: Download Catalog WAR from GitHub Packages ========================
FROM maven:3.8.6-openjdk-8-slim AS artifact-fetcher

ARG CATALOG_VERSION
ARG GITHUB_ACTOR
ARG TOKEN

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
</settings>' "${GITHUB_ACTOR}" "${TOKEN}" > /root/.m2/settings.xml

# Download released Catalog WAR from GitHub Packages
RUN mvn dependency:copy \
    -Dartifact=com.rackspace.feeds:feedscatalog:${CATALOG_VERSION}:war \
    -DoutputDirectory=/tmp/artifact && \
    ls -lh /tmp/artifact/

# ======================== STAGE 2: Download Schema from GitHub Packages ========================
FROM maven:3.8.6-openjdk-8-slim AS schema-fetcher

ARG SCHEMA_VERSION
ARG GITHUB_ACTOR
ARG TOKEN

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
</settings>' "${GITHUB_ACTOR}" "${TOKEN}" > /root/.m2/settings.xml

# Download schema tar.gz from GitHub Packages
RUN mvn dependency:copy \
    -Dartifact=com.rackspace.usage:usage-schema:${SCHEMA_VERSION}:tar.gz:schema \
    -DoutputDirectory=/tmp/schema-download && \
    cd /tmp/schema-download && \
    tar -xzf usage-schema-${SCHEMA_VERSION}-schema.tar.gz && \
    echo "Schema extracted successfully"

# ======================== STAGE 3: Runtime Image ========================
FROM tomcat:9-jre8

ARG service_port=8080
ARG SCHEMA_VERSION
ARG CATALOG_VERSION

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      description="Docker image for Cloudfeeds Catalog" \
      NAME="catalog" \
      CatalogVersion="${CATALOG_VERSION}" \
      SchemaVersion="${SCHEMA_VERSION}"

ENV SERVICE_PORT=${service_port} \
    SCHEMA_VERSION=${SCHEMA_VERSION} \
    CATALOG_VERSION=${CATALOG_VERSION}

RUN groupadd -o --gid 1000 feeds && \
    useradd -o -u 1000 -g feeds feeds && \
    mkdir -p /etc/cloudfeeds/translation/ /var/log/catalog && \
    chown -R feeds:feeds "${CATALINA_HOME}" /etc/cloudfeeds/translation/ /var/log/catalog && \
    wget -O /usr/local/bin/dumb-init \
        https://github.com/Yelp/dumb-init/releases/download/v1.2.5/dumb-init_1.2.5_x86_64 && \
    chown feeds:feeds /usr/local/bin/dumb-init && \
    chmod u+x /usr/local/bin/dumb-init

WORKDIR ${CATALINA_HOME}

# Config files from build context (service repo checked out by workflow)
COPY --chown=feeds:feeds docker/feedscatalog.xml /etc/feedscatalog/
COPY --chown=feeds:feeds docker/transform.xsl    /usr/local/tomcat/

RUN apt-get update && \
    yes | apt-get install xsltproc -y && \
    xsltproc --output /usr/local/tomcat/conf/server.xml \
             /usr/local/tomcat/transform.xsl \
             /usr/local/tomcat/conf/server.xml && \
    rm -rf /var/lib/apt/lists/*

USER feeds

# Schema xslt artifacts from GitHub Packages
COPY --chown=feeds:feeds \
    --from=schema-fetcher /tmp/schema-download/usage-schema-${SCHEMA_VERSION}/xslt-artifacts/* \
    /etc/cloudfeeds/translation/

# Catalog WAR downloaded from GitHub Packages — no build from source
COPY --chown=feeds:feeds \
    --from=artifact-fetcher /tmp/artifact/feedscatalog-${CATALOG_VERSION}.war \
    ${CATALINA_HOME}/webapps/feedscatalog.war

VOLUME ["/usr/local/tomcat/logs/", "/var/log/catalog/"]
EXPOSE ${service_port}

HEALTHCHECK --interval=20s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -L --fail --silent --show-error --connect-timeout 3 --max-time 3 \
        http://localhost:${SERVICE_PORT}/catalog || exit 1

ENTRYPOINT ["/usr/local/bin/dumb-init", "--"]
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
