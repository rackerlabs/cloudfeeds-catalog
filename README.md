# Overview
This is a simple component that takes the WADL files for Cloud Feeds and
generates a feeds catalog in various format.

Only XML is supported at the moment but JSON support is planned.

# Developer Guide
## To build the war file:
Run ```mvn clean install```

The above command will download a version of usage-schema-_version_-schema.tar.gz, unpack the file locally, then run transformation against the *.wadl files and generate corresponding feeds XML catalog files.

The version of usage-schema to be downloaded is defined as a property inside the pom.xml file. As with any property, the value of this is somewhat static inside the pom.xml file. Thankfully, someone wrote a maven plugin that automatically updates the version of any dependencies. Therefore, it is usually a good idea to run this before you do a build:
```mvn versions:update-properties -DincludeProperties=usage-schema.version```

The above command essentially grabs the latest release of usage-schema and updates the pom.xml file. Isn't that awesome!? You can then check in the new pom.xml file.

## To build the RPM files:
This component produces 2 RPM artifacts:
* feedscatalog-app-${version}*.rpm (a web application to be deployed on Tomcat)
* feedscatalog-xslt-${version}*.rpm (an xslt to be deployed with Repose)

To build the RPM files, you can issue this command:
```mvn clean install -Pbuild-app-rpm,build-xslt-rpm```

## To turn on debug for Spring:
Change the file src/main/resources/log4j.properties to have DEBUG level, rebuild the war and redeploy.

## To access the Feeds Catalog
Deploy the feedscatalog-app-_version_.war into a host with Tomcat. Then issue HTTP GET using this URL format:
* http://_hostname_/feedscatalog/catalog, for admin catalog
* http://_hostname_/feedscatalog/catalog/_tenantId_, for tenanted catalog

# Links
* XML Schema to Feeds Catalog XML: src/main/resources/feedscatalog.xsd
* 
