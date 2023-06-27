# syntax=docker/dockerfile:1

# use below command to build image
# docker build -t khannakavish/cloudfeeds-catalog:v4 -f Dockerfile --build-arg SCHEMA_VERSION=1.138.3-SNAPSHOT  --build-arg saxon_lic=saxon-license.lic  ../

# ======================== Build cloudfeeds catalog using maven container image =======================

FROM maven:3.8.6-openjdk-8-slim AS builder

ARG username
ARG password

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      description="Docker image for Cloudfeeds Catalog" \
      NAME="cloudfeeds-catalog"

WORKDIR /cf-catalog
COPY ./../pom.xml                              /cf-catalog/pom.xml
COPY ./../settings.xml                        /root/.m2/settings-security.xml


RUN <<EOF
      apt-get update
      yes | apt-get install git
      git config --global user.name "kavishkhanna"
      git config --global user.email "kavish0910@gmail.com"
EOF

# RUN mvn -e -B dependency:resolve
RUN mvn dependency:go-offline

COPY . .
#RUN mvn clean -e -B package
RUN mvn -B verify

RUN git config --global user.name "kavishkhanna"
RUN mvn -up -Dresume=false release:prepare release:perform -Dusername=${username} -Dpassword=${password} -X


# ======================== Download artifacts =======================
# Multi stage build
# Download artifacts

FROM alpine:3.17.0 as package

ARG SCHEMA_VERSION

#RUN apk add --update --no-cache curl tar ca-certificates && \
RUN apk add --update --no-cache curl tar && \
    curl https://artifacts.rackspace.net/artifactory/cloudfeeds-maven-local/com/rackspace/usage/usage-schema/${SCHEMA_VERSION}/usage-schema-${SCHEMA_VERSION}-schema.tar.gz | tar xz
   # TODO: verify the authenticity and integrity of above packages


# ======================== Create deploy Image =======================
FROM tomcat:9.0.73-jre8-temurin-jammy as tomcat

ARG service_port=8080
ARG saxon_lic
ARG SCHEMA_VERSION

LABEL Maintainer="cloudfeeds-core@rackspace.com" \
      description="Docker image for Cloudfeeds Catalog" \
      NAME="cloudfeeds-catalog"

ENV SAXON_HOME=/etc/saxon \
    SERVICE_PORT=${SERVICE_PORT:-$service_port}

# add non privileged user
RUN <<EOF
      groupadd -r feeds && useradd -r -g feeds feeds

      mkdir -p /etc/cloudfeeds/translation/
      chown -R feeds:feeds "${CATALINA_HOME}" /etc/cloudfeeds/translation/

      wget -O /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.5/dumb-init_1.2.5_x86_64
      chown feeds:feeds /usr/local/bin/dumb-init
      chmod u+x /usr/local/bin/dumb-init
EOF

WORKDIR ${CATALINA_HOME}

#COPY --chown=feeds:docker ./docker/entrypoint_dev.sh .
COPY --chown=feeds:feeds ./docker/transform.xsl                           /usr/local/tomcat

RUN <<EOF
      apt-get update
      yes | apt-get install xsltproc
      xsltproc --output /usr/local/tomcat/conf/server.xml /usr/local/tomcat/transform.xsl /usr/local/tomcat/conf/server.xml
      rm -rf /var/lib/apt/lists/*
EOF


USER feeds:feeds
VOLUME /usr/local/tomcat/logs/


#  ======================= get ca cert bundle from curl.haxx.se  =======================
#RUN curl https://curl.haxx.se/ca/cacert.pem -L -o /cacert.pem



COPY --chown=feeds:feeds --from=package /usage-schema-${SCHEMA_VERSION}/xslt-artifacts/* /etc/cloudfeeds/translation/
COPY --chown=feeds:feeds --from=builder /cf-catalog/target/feedscatalog-*.war ${CATALINA_HOME}/webapps/ROOT.war

EXPOSE ${SERVICE_PORT}

# Perform health check
HEALTHCHECK --interval=20s --timeout=5s --start-period=10s --retries=3 \
      CMD curl -L --fail --silent --show-error --connect-timeout 3 --max-time 3 http://localhost:${SERVICE_PORT}/catalog || exit 1


#Start tomcat server
ENTRYPOINT ["/usr/local/bin/dumb-init", "--"]
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
