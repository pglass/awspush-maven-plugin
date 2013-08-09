/**
 * @author Paul Glass
 * @email paul.glass@pearson.com
 * 8.7.2013
 */

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;

import java.io.*;

/**
 * To use this, just instantiate the class:
 *      new AWSPush(mavenArgs);
 * This will:
 *      1. Upload a file to S3, and
 *      2. Deploy a file on S3 to an ElasticBeanstalk environment
 * Depending on the arguments, one or both of these steps can be skipped.
 */
public class AWSPush {

    private MavenArgs mvnArgs;

    /**
     * @throws java.io.FileNotFoundException thrown if the file to be uploaded does not exist
     */
    public AWSPush(MavenArgs args) throws FileNotFoundException {
        this.mvnArgs = args;
        this.applyVersionSuffix();
        this.applyFileKeySuffix();
        this.mvnArgs.dump();
        this.uploadAndDeploy();
    }

    private void error(Throwable t) {
        this.mvnArgs.getLog().error(t);
    }

    private void info(Object o) {
        this.mvnArgs.getLog().info(o.toString());
    }

    private void uploadAndDeploy() throws FileNotFoundException {
        if (this.mvnArgs.noUpload)
            this.info("SKIPPING the upload step.");
        else
            this.uploadToS3Multipart();

        if (this.mvnArgs.noDeploy)
            this.info("SKIPPING the deployment step");
        else
            this.deployToEnvironment();
    }

    private void applyVersionSuffix() {
        if (Util.nullOrEmpty(this.mvnArgs.versionSuffix)) {
            return;
        }
        this.info("Found version suffix: " + this.mvnArgs.versionSuffix);
        this.mvnArgs.applicationVersion += this.mvnArgs.versionSuffix;
    }

    private void applyFileKeySuffix() {
        if (Util.nullOrEmpty(this.mvnArgs.fileKeySuffix)) {
            return;
        }
        this.info("Found file key suffix: " + this.mvnArgs.fileKeySuffix);
        String filekey = this.mvnArgs.s3FileKey;
        if (!filekey.contains(".")) {
            filekey += this.mvnArgs.fileKeySuffix;
        } else {
            String[] parts = filekey.split("[.]", 2);   // split only at the first dot
            if (parts[1].equals("war"))
                filekey = parts[0] + "##" + this.mvnArgs.fileKeySuffix + "." + parts[1];
            else
                filekey = parts[0] + this.mvnArgs.fileKeySuffix + "." + parts[1];
        }
        this.mvnArgs.s3FileKey = filekey;
    }

    /* A single part upload. This has slow upload speeds compared to a multi-part upload */
//    public void uploadToS3Singlepart() {
//        AmazonS3Client s3Client = new AmazonS3Client(
//                new BasicAWSCredentials(this.mvnArgs.awsAccessKey, this.mvnArgs.awsSecretKey)
//        );
//
//        File fileToUpload = new File(this.mvnArgs.filepath);
//        PutObjectRequest request = new PutObjectRequest(
//                this.mvnArgs.s3BucketName,
//                this.mvnArgs.s3FileKey,
//                fileToUpload
//        );
//        request.setProgressListener(new Util.SimpleProgressListener(fileToUpload.length()));
//        PutObjectResult result = s3Client.putObject(request);
//        Util.dump(result, this.mvnArgs.getLog());
//    }

    /**
     * Upload a file stored locally to S3. This is the preferred method for uploading since
     * there seems to be a limit on single part upload speeds.
     *
     * @throws java.io.FileNotFoundException if the file to be uploaded is not found
     */
    public void uploadToS3Multipart() throws FileNotFoundException {
        AmazonS3Client s3Client = new AmazonS3Client(this.mvnArgs.getCredentials());

        // Ensure file exists
        File fileToUpload = new File(this.mvnArgs.filepath);
        if (!fileToUpload.exists()) {
            throw new FileNotFoundException("File '" + this.mvnArgs.filepath + "' was not found");
        }

        // This file upload happens in some other thread(s)
        TransferManager transferManager = new TransferManager(s3Client);
        Upload upload = transferManager.upload(this.mvnArgs.s3BucketName, this.mvnArgs.s3FileKey, fileToUpload);

        // Print info to monitor the upload
        String totalBytesString = Util.prettyByteString(upload.getProgress().getTotalBytesToTransfer());
        this.info("Transfer: " + upload.getDescription() + " (" + totalBytesString + ")");
        while (!upload.isDone()) {
            this.info("  " + upload.getState() + ": "
                    + Util.prettyByteString(upload.getProgress().getBytesTransferred())
                    + String.format(" \t(%2.2f%%)", upload.getProgress().getPercentTransferred()));
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Cleanup
        try {
            UploadResult result = upload.waitForUploadResult();
            this.info("Finished:");
            Util.dump(result, this.mvnArgs.getLog());
        } catch (InterruptedException e) {
            error(e);
        } finally {
            transferManager.shutdownNow();
        }
    }

    /**
     * Create a new Beanstalk application version using the file on S3 at s3BucketName/s3FileKey.
     * Deploy this application version to the Beanstalk environment.
     */
    public void deployToEnvironment() {
        this.info("Connecting to ElasticBeanstalk");
        AWSElasticBeanstalkClient beanstalkClient = new AWSElasticBeanstalkClient(this.mvnArgs.getCredentials());

        // create new application version
        this.info("Creating new version " + this.mvnArgs.applicationName + "/" + this.mvnArgs.applicationVersion);
        S3Location s3Location = new S3Location()
                .withS3Bucket(this.mvnArgs.s3BucketName)
                .withS3Key(this.mvnArgs.s3FileKey);
        CreateApplicationVersionRequest versionRequest = new CreateApplicationVersionRequest()
                .withApplicationName(this.mvnArgs.applicationName)
                .withVersionLabel(this.mvnArgs.applicationVersion)
                .withSourceBundle(s3Location);
        CreateApplicationVersionResult versionResult = beanstalkClient.createApplicationVersion(versionRequest);
        Util.dump(versionResult, this.mvnArgs.getLog());

        // tell the environment to use the new application version
        this.info("Updating environment " + this.mvnArgs.environmentName + " with version " + this.mvnArgs.applicationVersion);
        UpdateEnvironmentRequest environmentRequest = new UpdateEnvironmentRequest()
                .withEnvironmentName(this.mvnArgs.environmentName)
                .withVersionLabel(this.mvnArgs.applicationVersion);
        UpdateEnvironmentResult envResult = beanstalkClient.updateEnvironment(environmentRequest);
        Util.dump(envResult, this.mvnArgs.getLog());
    }
}