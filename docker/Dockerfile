# syntax=docker/dockerfile:1

FROM maven:3.8.6-openjdk-8-slim AS builder

ARG CATALOG_VERSION=dev
ARG GITHUB_ACTOR
ARG TOKEN

RUN mkdir -p /root/.m2 && \
    if [ -n "${GITHUB_ACTOR}" ] && [ -n "${TOKEN}" ]; then \
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
</settings>' "${GITHUB_ACTOR}" "${TOKEN}" > /root/.m2/settings.xml; \
    fi

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      description="Docker image for Cloudfeeds Catalog" \
      NAME="cloudfeeds-catalog" \
      version="${CATALOG_VERSION}"

WORKDIR /cf-catalog
COPY pom.xml /cf-catalog/pom.xml
RUN mvn -B dependency:go-offline

COPY . /cf-catalog
RUN mvn clean -B package && \
    schema_dir="$(find /cf-catalog/target/usage-schema -maxdepth 1 -type d -name 'usage-schema-*' | head -n 1)" && \
    test -n "${schema_dir}" && \
    mkdir -p /tmp/catalog-xslt && \
    cp -R "${schema_dir}/xslt-artifacts/." /tmp/catalog-xslt/

FROM tomcat:9-jre8 AS tomcat

ARG service_port=8080
ARG SCHEMA_VERSION
ARG CATALOG_VERSION=dev
ARG environment
ARG CATALOG_ENVIRONMENT_NAME
ARG CATALOG_VIP_URL
ARG CATALOG_EXTERNAL_VIP_URL
ARG CATALOG_PREFS_SERVICE_URL

RUN test -n "${SCHEMA_VERSION}" || (echo "SCHEMA_VERSION build arg is required" && exit 1)
RUN test -n "${environment}" || (echo "environment build arg is required" && exit 1)
RUN case "${environment}" in test|staging|prod) ;; *) echo "environment must be one of: test, staging, prod"; exit 1 ;; esac
RUN test -n "${CATALOG_ENVIRONMENT_NAME}" || (echo "CATALOG_ENVIRONMENT_NAME build arg is required" && exit 1)
RUN test -n "${CATALOG_VIP_URL}" || (echo "CATALOG_VIP_URL build arg is required" && exit 1)
RUN test -n "${CATALOG_EXTERNAL_VIP_URL}" || (echo "CATALOG_EXTERNAL_VIP_URL build arg is required" && exit 1)
RUN test -n "${CATALOG_PREFS_SERVICE_URL}" || (echo "CATALOG_PREFS_SERVICE_URL build arg is required" && exit 1)

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      description="Docker image for Cloudfeeds Catalog" \
      NAME="catalog" \
      version="${CATALOG_VERSION}" \
      environment="${environment}"

ENV SAXON_HOME=/etc/saxon \
    SERVICE_PORT=${SERVICE_PORT:-$service_port} \
    CATALOG_ENVIRONMENT_NAME="${CATALOG_ENVIRONMENT_NAME}" \
    CATALOG_VIP_URL="${CATALOG_VIP_URL}" \
    CATALOG_EXTERNAL_VIP_URL="${CATALOG_EXTERNAL_VIP_URL}" \
    CATALOG_PREFS_SERVICE_URL="${CATALOG_PREFS_SERVICE_URL}"

RUN groupadd --gid 1000 feeds && \
      useradd -u 1000 -g feeds feeds && \
      mkdir -p /etc/cloudfeeds/translation /etc/feedscatalog /var/log/catalog && \
      chown -R feeds:feeds "${CATALINA_HOME}" /etc/cloudfeeds/translation /etc/feedscatalog /var/log/catalog && \
      wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.5/dumb-init_1.2.5_x86_64 && \
      chown feeds:feeds /usr/local/bin/dumb-init && \
      chmod u+x /usr/local/bin/dumb-init

WORKDIR ${CATALINA_HOME}

COPY docker/transform.xsl /usr/local/tomcat/transform.xsl
COPY docker/config/common/feedscatalog.xml.tpl /tmp/feedscatalog.xml.tpl
COPY docker/config/env /tmp/catalog-config/env

RUN apt-get update && \
      apt-get install -y --no-install-recommends xsltproc gettext-base curl && \
      cp "/tmp/catalog-config/env/${environment}/catalog.env" /tmp/catalog.env && \
      xsltproc --output /usr/local/tomcat/conf/server.xml /usr/local/tomcat/transform.xsl /usr/local/tomcat/conf/server.xml && \
      set -a && . /tmp/catalog.env && set +a && \
      envsubst < /tmp/feedscatalog.xml.tpl > /etc/feedscatalog/feedscatalog.xml && \
      rm -rf /var/lib/apt/lists/* /tmp/feedscatalog.xml.tpl /tmp/catalog.env /tmp/catalog-config

USER feeds
VOLUME /usr/local/tomcat/logs/ /var/log/catalog/

COPY --chown=feeds:feeds --from=builder /tmp/catalog-xslt/. /etc/cloudfeeds/translation/
COPY --chown=feeds:feeds --from=builder /cf-catalog/target/feedscatalog-*.war ${CATALINA_HOME}/webapps/feedscatalog.war

EXPOSE ${SERVICE_PORT}

HEALTHCHECK --interval=20s --timeout=5s --start-period=10s --retries=3 \
      CMD curl -L --fail --silent --show-error --connect-timeout 3 --max-time 3 "http://localhost:${SERVICE_PORT}/catalog" || exit 1

ENTRYPOINT ["/usr/local/bin/dumb-init", "--"]
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
