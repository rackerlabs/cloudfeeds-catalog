#base tomcat 9 with openjdk 8
FROM tomcat:9.0.41-jdk8 as tomcat

FROM adoptopenjdk/openjdk8:alpine-slim

LABEL maintainer="Cloudfeeds-core@rackspace.com" \
      description="Docker image for FeedsCatalog"

#FeedsCatalog version
ARG version
ARG schema_version

ENV CATALINA_HOME=/opt/tomcat \
    APP_HOME=/opt/feedscatalog \
    APP_VERSION=${version} \
    PATH=${PATH}:${CATALINA_HOME}/bin:${APP_HOME}

RUN mkdir -p ${CATALINA_HOME} ${APP_HOME} /etc/cloudfeeds/translation/

WORKDIR ${APP_HOME}

COPY --from=tomcat /usr/local/tomcat ${CATALINA_HOME}

RUN apk --no-cache add curl tar bash \
    && curl https://artifacts.rackspace.net/artifactory/cloudfeeds-maven-local/com/rackspace/usage/usage-schema/${schema_version}/usage-schema-${schema_version}-schema.tar.gz | tar xz \
    && mv usage-schema-${schema_version}/xslt-artifacts/* /etc/cloudfeeds/translation/. \
    && curl -o ${CATALINA_HOME}/webapps/feedscatalog.war https://artifacts.rackspace.net/artifactory/cloudfeeds-maven-local/com/rackspace/feeds/feedscatalog/${APP_VERSION}/feedscatalog-${APP_VERSION}.war \
    && rm -rf ${APP_HOME}

EXPOSE 8080

CMD ["/opt/tomcat/bin/catalina.sh", "run"]
