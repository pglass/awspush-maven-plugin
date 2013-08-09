import com.amazonaws.AmazonClientException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.FileNotFoundException;

/**
 * @goal push
 */
public class PushMojo extends AbstractMojo {

    public void execute() throws MojoExecutionException {
        try {
            MavenArgs args = new MavenArgs(this);
            new AWSPush(args);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("", e);
        } catch (AmazonClientException e) {
            throw new MojoExecutionException("", e);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("", e);
        } catch (MavenArgs.MavenArgException e) {
            throw new MojoExecutionException("", e);
        }
    }

    /**
     * A .properies file containing the AWS access key and secret key to use.
     * The entire file should like:
     *      accessKey = 12345
     *      secretKey = abcde
     * http://en.wikipedia.org/wiki/.properties
     *
     * @parameter
     */
    public String awsCredentialsFile;

    /**
     * An AWS access key
     *
     * @parameter
     */
    public String awsAccessKey;

    /**
     * An AWS secret key
     *
     * @parameter
     */
    public String awsSecretKey;

    /**
     * The path to the file to upload
     *
     * @parameter
     * @required
     */
    public String filepath;

    /**
     * The name of the S3 bucket to store the file.
     * Every Beanstalk application has its own bucket.
     *
     * @parameter
     * @required
     */
    public String s3BucketName;

    /**
     * The name under which to store the file on S3.
     *
     * @parameter
     * @required
     */
    public String s3FileKey;

    /**
     * The Beanstalk application name
     *
     * @parameter
     * @required
     */
    public String applicationName;

    /**
     * The name of the environment to deploy to
     *
     * @parameter
     * @required
     */
    public String environmentName;

    /**
     * The name of the Beanstalk application version to create and deploy
     *
     * @parameter
     * @required
     */
    public String applicationVersion;

    /**
     * If true, do not do the deployment step.
     * No Beanstalk environments will be changed, but a file may still be uploaded.
     *
     * @parameter
     */
    public boolean noDeploy = false;

    /**
     * If true, do not upload a file to S3.
     * Deployment may still occur using a file already on S3.
     *
     * @parameter
     */
    public boolean noUpload = false;

    /**
     * A suffix to apply to applicationVersion
     *
     * @parameter
     */
    public String versionSuffix = "";

    /**
     * A suffix to apply to the s3FileKey.
     * If the version suffix is '-1.0-SNAPSHOT', then s3FileKey will be modified as follows.
     *      ROOT        -->     ROOT-1.0-SNAPSHOT
     *      ROOT.txt    -->     ROOT-1.0-SNAPSHOT.txt
     *      ROOT.war    -->     ROOT##-1.0-SNAPSHOT.war
     * For war files, the '##' syntax is allowed by Tomcat for versioning.
     * That is, Tomcat deploys ROOT.war and ROOT##-1.0-SNAPSHOT.war at the same url.
     *
     * @parameter
     */
    public String fileKeySuffix = "";
}
