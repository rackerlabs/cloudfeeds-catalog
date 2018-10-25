node('java') {
    def mvnHome
    stage('Preparation') { // for display purposes
       // Get some code from a GitHub repository
       git 'https://github.com/rackerlabs/cloudfeeds-catalog.git'
       // Get the Maven tool.
       // ** NOTE: This 'M3' Maven tool must be configured
       // **       in the global configuration.           
       mvnHome = tool 'M3'
    }
    stage('Build') {
       // Run the maven build
       sh "'${mvnHome}/bin/mvn' clean install" 
    }
    stage('Results') {
       archiveArtifacts artifacts: "target/*.war"
       slackSend channel: "@teja.cheruku", color: "#FC05DE", message: "Deployed branch" 
    }
 }
