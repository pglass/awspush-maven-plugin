Author
    Paul Glass
    pnglass@gmail.com

Overview
    This is a Maven plugin that will deploy a local file (e.g. a WAR file)
    to an ElasticBeanstalk environment. To do this it does the following:
        1. Upload the file to S3
        2. Create a new version under your application using the uploaded file
        3. Update your environment with the new version

Usage
    Here's a sample plugin section which includes all possible tags.
    There are only snapshot builds for now.

    <plugin>
        <groupId>com.pearson.tn8</groupId>
        <artifactId>awspush-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>install</phase>
                <goals>
                    <goal>push</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <awsCredentialsFile>${basedir}/cred.properties</awsCredentialsFile>
            <awsAccessKey>12345</awsAccessKey>
            <awsSecretKey>abcde</awsSecretKey>
            <filepath>${basedir}/standalone.war</filepath>
            <s3BucketName>elasticbeanstalk-us-east-1-114320312179</s3BucketName>
            <s3FileKey>ROOT.war</s3FileKey>
            <applicationName>beanstalkApp</applicationName>
            <environmentName>beanstalkEnv</environmentName>
            <applicationVersion>appVersion1.0</applicationVersion>
            <versionSuffix>-standalone-${maven.build.timestamp}</versionSuffix>
            <fileKeySuffix>standalone-${maven.build.timestamp}</fileKeySuffix>
            <noDeploy>true</noDeploy>
            <noUpload>true</noUpload>
        </configuration>
    </plugin>

    -- There is only one goal, "push".
    -- You can specify the AWS secret key and access key in two ways:
        1. Explicitly in the pom (as above) using the <awsAccessKey> and <awsSecretKey> tags
        2. Specify a .properties file in the pom using the <awsCredentialsFile> tag
            A valid .properties file can contain just the following two lines:
                awsAccessKey = 12345
                awsSecretKey = abcde
            http://en.wikipedia.org/wiki/.properties
        (The plugin checks for credentials in this order in the event both used)

    -- You can specify suffixes which contain build ids or timestamps, as above.
        These are not required and default to empty strings.

        -- The application version suffix should always be supplied, since application versions
        must be unique within a Beanstalk application.
        -- If the file suffix is '-1.0-SNAPSHOT', then s3FileKey will be modified as follows.
            ROOT        -->     ROOT-1.0-SNAPSHOT
            ROOT.txt    -->     ROOT-1.0-SNAPSHOT.txt
            ROOT.war    -->     ROOT##-1.0-SNAPSHOT.war
        For war files, the '##' syntax is allowed by Tomcat for versioning.
        That is, Tomcat servers ROOT.war and ROOT##-1.0-SNAPSHOT.war ar the same url.

    -- It can be useful to specify the <noUpload> or <noDeploy> tags to true while your setting up the pom.
        These are not required default to false
        This will print out all of the properties without uploading or deploying anything.

    -- This plugin can be used on its own, but you may get an empty jar as output since Maven likes
        to package things. To avoid this, set the packaging type to "pom": <packaging>pom</packaging>
