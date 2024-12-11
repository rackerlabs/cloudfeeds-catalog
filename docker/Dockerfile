# syntax=docker/dockerfile:1

# use below command to build image
# docker build -t khannakavish/cloudfeeds-catalog:v4 -f Dockerfile --build-arg SCHEMA_VERSION=1.138.3-SNAPSHOT  --build-arg saxon_lic=saxon-license.lic  ../

# =============== Clone Cloudfeeds-atomhopper repo to build container image in AWS EC2 Image Builder ===============
FROM bitnami/git:latest AS fetcher

ARG GIT_URL=https://github.com/rackerlabs/cloudfeeds-catalog.git

# ARG GIT_BRANCH="feature_containerization"
#ARG GIT_BRANCH="CF-3592-EC2-Image-Builder"
ARG GIT_BRANCH="CF-test-ci/cd"
ARG TOKEN  

RUN mkdir -p /home/cl && \
    cd /home && \
    git clone -b ${GIT_BRANCH} https://${TOKEN}@${GIT_URL#https://} cl

# RUN mkdir -p /home/cl && \
#     cd /home && \
#     git clone -b ${GIT_BRANCH} ${GIT_URL} cl


# ======================== Build cloudfeeds catalog using maven container image =======================

FROM maven:3.8.6-openjdk-8-slim AS builder

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      description="Docker image for Cloudfeeds Catalog" \
      NAME="cloudfeeds-catalog"

WORKDIR /cf-catalog
COPY --from=fetcher /home/cl/pom.xml                              /cf-catalog/pom.xml

# RUN mvn -e -B dependency:resolve
RUN mvn dependency:go-offline

COPY --from=fetcher /home/cl/ /cf-catalog
#RUN mvn clean -e -B package
RUN mvn -B verify


# ======================== Download artifacts =======================
# Multi stage build
# Download artifacts

FROM alpine:3.18.3 as package

ARG SCHEMA_VERSION="1.138.3-SNAPSHOT"

#RUN apk add --update --no-cache curl tar ca-certificates && \
RUN apk add --update --no-cache curl tar && \
    curl https://artifacts.rackspace.net/artifactory/cloudfeeds-maven-local/com/rackspace/usage/usage-schema/${SCHEMA_VERSION}/usage-schema-${SCHEMA_VERSION}-schema.tar.gz | tar xz
   # TODO: verify the authenticity and integrity of above packages


# ======================== Create deploy Image =======================
FROM tomcat:9-jre8 as tomcat

ARG service_port=8080
ARG saxon_lic
ARG SCHEMA_VERSION="1.138.3-SNAPSHOT"

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      description="Docker image for Cloudfeeds Catalog" \
      NAME="catalog"

ENV SAXON_HOME=/etc/saxon \
    SERVICE_PORT=${SERVICE_PORT:-$service_port}

# add non privileged user
# RUN   GID=1000; \
#       while getent group $GID > /dev/null; do GID=$((GID+1)); done && \
#       groupadd --gid $GID feeds && \
#       UID=1000; \
#       while getent passwd $UID > /dev/null; do UID=$((UID+1)); done && \
#       useradd -u $UID -g feeds feeds && \
#       mkdir -p /etc/cloudfeeds/translation/ && \
#       chown -R feeds:feeds "${CATALINA_HOME}" /etc/cloudfeeds/translation/  && \
#       wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.5/dumb-init_1.2.5_x86_64  && \
#       chown feeds:feeds /usr/local/bin/dumb-init  && \
#       chmod u+x /usr/local/bin/dumb-init

RUN   groupadd --gid 1000 feeds && \
      useradd -u 1000 -g feeds feeds  && \
      mkdir -p /etc/cloudfeeds/translation/ && \
      chown -R feeds:feeds "${CATALINA_HOME}" /etc/cloudfeeds/translation/  && \
      wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.5/dumb-init_1.2.5_x86_64  && \
      chown feeds:feeds /usr/local/bin/dumb-init  && \
      chmod u+x /usr/local/bin/dumb-init

WORKDIR ${CATALINA_HOME}

#COPY --chown=feeds:docker ./docker/entrypoint_dev.sh .
COPY --chown=feeds:feeds --from=fetcher /home/cl/docker/feedscatalog.xml                        /etc/feedscatalog/
COPY --chown=feeds:feeds --from=fetcher /home/cl/docker/transform.xsl                           /usr/local/tomcat

RUN   apt-get update  && \
      yes | apt-get install xsltproc  && \
      xsltproc --output /usr/local/tomcat/conf/server.xml /usr/local/tomcat/transform.xsl /usr/local/tomcat/conf/server.xml && \
      rm -rf /var/lib/apt/lists/*

USER feeds:feeds
VOLUME /usr/local/tomcat/logs/


#  ======================= get ca cert bundle from curl.haxx.se  =======================
#RUN curl https://curl.haxx.se/ca/cacert.pem -L -o /cacert.pem

USER feeds
VOLUME /usr/local/tomcat/logs/ /var/log/catalog/

COPY --chown=feeds:feeds --from=package /usage-schema-${SCHEMA_VERSION}/xslt-artifacts/* /etc/cloudfeeds/translation/
COPY --chown=feeds:feeds --from=builder /cf-catalog/target/feedscatalog-*.war ${CATALINA_HOME}/webapps/feedscatalog.war

EXPOSE ${SERVICE_PORT}

# Perform health check
HEALTHCHECK --interval=20s --timeout=5s --start-period=10s --retries=3 \
      CMD curl -L --fail --silent --show-error --connect-timeout 3 --max-time 3 http://localhost:${SERVICE_PORT}/catalog || exit 1


#Start tomcat server
ENTRYPOINT ["/usr/local/bin/dumb-init", "--"]
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
