# docker build image and run the conatiner
Your current direcotry should be pointing to ***cloudfeeds-catalog/docker*** 

Passing value of the **version** argument is mandatory for successful build. This defines version for both *feedscatalog* and *usage-schema*
Run the following command to build an image by providing the *feedscatalog_version* value. 
```
$docker build --build-arg version=feedscatalog_version -t feedscatalog:feedscatalog_version-alpine . 
```
Use the following command to run a container on port 8080
```
$docker run -itd --name [Conatiner_Name] -p 8080:8080 feedscatalog:feedscatalog_version-alpine
```

Test the feedscatalog at http://localhost:8080/feedscatalog/catalog


Following environment variables are set by default 
```
JAVA_HOME=/opt/java/openjdk
CATALINA_HOME=/opt/tomcat
APP_HOME=/opt/feedscatalog
APP_VERSION=${version} 
```

Example of building an image with version 1.137.0  and running a container.
```
$docker build --build-arg version=1.137.0 -t feedscatalog:1.137.0-alpine .
$docker run -itd --name feedscatalog -p 8080:8080 feedscatalog:1.137.0-alpine
```



