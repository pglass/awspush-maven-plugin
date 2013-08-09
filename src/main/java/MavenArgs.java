import com.amazonaws.auth.*;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This provides an interface for accessing properties from a PushMojo.
 * A MavenArgs object is passed to an AWSPush constructor.
 */
public class MavenArgs implements AWSCredentialsProvider {

    public static class MavenArgException extends Exception {
        public MavenArgException(String msg) {
            super(msg);
        }
    }

    public String awsAccessKey;
    public String awsSecretKey;
    public String awsCredentialsFile;
    public String filepath;
    public String s3BucketName;
    public String s3FileKey;
    public String applicationName;
    public String environmentName;
    public String applicationVersion;
    public boolean noDeploy;
    public boolean noUpload;
    public String versionSuffix;
    public String fileKeySuffix;

    private Log _log;
    private AWSCredentials _credentials;

    public MavenArgs(PushMojo pushMojo) throws MavenArgException, FileNotFoundException {
        this.awsAccessKey       = pushMojo.awsAccessKey;
        this.awsSecretKey       = pushMojo.awsSecretKey;
        this.awsCredentialsFile = pushMojo.awsCredentialsFile;
        this.filepath           = pushMojo.filepath;
        this.s3BucketName       = pushMojo.s3BucketName;
        this.s3FileKey          = pushMojo.s3FileKey;
        this.applicationName    = pushMojo.applicationName;
        this.environmentName    = pushMojo.environmentName;
        this.applicationVersion = pushMojo.applicationVersion;
        this.noDeploy           = pushMojo.noDeploy;
        this.noUpload           = pushMojo.noUpload;
        this.versionSuffix      = pushMojo.versionSuffix;
        this.fileKeySuffix      = pushMojo.fileKeySuffix;

        this._log = pushMojo.getLog();

        this.refresh(); // init AWS credentials
        if (this._credentials == null) {
            throw new MavenArgException("Unable to load credentials");
        }
    }

    /**
     * @return The AWS credentials specified by the POM
     * @see {@link MavenArgs#refresh()}
     */
    public AWSCredentials getCredentials() {
        return this._credentials;
    }

    /**
     * Refresh the AWS credentials
     * This will look for credentials in the following order, using the first which appears valid:
     *      1. Check the access key and secret specified explicitly in the POM
     *      2. Check the credentials file specified in the POM
     */
    /*
     * This method overrides and is not allowed to throw.
     * An error is thrown afterwards if the credentials are null.
     */
    public void refresh() {
        if (this._credentials != null) {
            return;
        }
        if (!Util.nullOrEmpty(this.awsAccessKey) && !Util.nullOrEmpty(this.awsSecretKey)) {
            this._credentials = new BasicAWSCredentials(this.awsAccessKey, this.awsSecretKey);
            this.getLog().info("Credentials loaded from POM");
        } else if (!Util.nullOrEmpty(this.awsCredentialsFile)) {
            try {
                this._credentials = new PropertiesCredentials(new File(this.awsCredentialsFile));
                this.getLog().info("Credentials loaded from properties file");
            } catch (IOException e) {
                // this method overrides and is not allowed to throw.
                // an error should be thrown afterwards if this._credentials is null
            }
        }
    }

    public Log getLog() {
        return this._log;
    }

    public void dump() {
        if (this.getLog() == null)
            return;
        this.getLog().info("File to upload: " + this.filepath);
        this.getLog().info("S3 Bucket:      " + this.s3BucketName);
        this.getLog().info("S3 File key:    " + this.s3FileKey);
        this.getLog().info("Application:    " + this.applicationName);
        this.getLog().info("Environment:    " + this.environmentName);
        this.getLog().info("App Version:    " + this.applicationVersion);
    }
}
