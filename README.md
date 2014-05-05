# Overview
This is a simple component that takes the WADL files for Cloud Feeds and
generates a feeds catalog in various format.

Only XML is supported at the moment but JSON support is planned.

# Developer Guide
## To build the war file:
Run ```mvn clean install```

## To build the RPM files:
This component produces 2 RPM artifacts:
* feedscatalog-app-${version}*.rpm (a web application to be deployed on Tomcat)
* feedscatalog-xslt-${version}*.rpm (an xslt to be deployed with Repose)

To build the RPM files, you can issue this command:
```mvn clean install -Pbuild-app-rpm,build-xslt-rpm```
