/**
 * @author Paul Glass
 * @email paul.glass@pearson.com
 * 8.7.2013
 */
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentResult;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import org.apache.maven.plugin.logging.Log;

/**
 * This prints out progress info as the upload happens
 */
public class Util {

    public static boolean nullOrEmpty(String arg) {
        return arg == null || arg.isEmpty();
    }

    /**
     * Pad the string on the right with spaces until the result is size characters.
     * The returned string will be max(size, s.length()) characters long.
     */
    public static String padRight(String s, int size) {
        String result = s;
        while (result.length() < size) {
            result += " ";
        }
        return result;
    }

    /* Modified from: http://stackoverflow.com/a/3758880 */
    public static String prettyByteString(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static void dump(UploadResult ur, Log log) {
        int width = "VersionId".length();
        log.info("  Bucket    : " + ur.getBucketName());
        log.info("  Key       : " + ur.getKey());
        log.info("  ETag      : " + ur.getETag());
        log.info("  VersionId : " + ur.getVersionId());
    }

    public static void dump(PutObjectResult por, Log log) {
        log.info("  Bucket     : " + por.getContentMd5());
        log.info("  Expiration : " + por.getExpirationTime());
        log.info("  Encryption : " + por.getServerSideEncryption());
        log.info("  ETag       : " + por.getETag());
        log.info("  VersionId  : " + por.getVersionId());
    }

    public static void dump(UpdateEnvironmentResult uer, Log log) {
        log.info("  ApplicationName : " + uer.getApplicationName());
        log.info("  EnvironmentId   : " + uer.getEnvironmentId());
        log.info("  EnvironmentName : " + uer.getEnvironmentName());
        log.info("  VersionLabel    : " + uer.getVersionLabel());
        log.info("  Status          : " + uer.getStatus());
        log.info("  Health          : " + uer.getHealth());
    }

    public static void dump(CreateApplicationVersionResult cavr, Log log) {
        ApplicationVersionDescription avd = cavr.getApplicationVersion();
        log.info("  ApplicationName : " + avd.getApplicationName());
        log.info("  VersionLabel    : " + avd.getVersionLabel());
        log.info("  SourceBundle    : " + avd.getSourceBundle().getS3Bucket() + "/" + avd.getSourceBundle().getS3Key());
    }

    /** A listener to display progress during a single part upload
     * (multipart uploads have some built in functionality for this)
     */
//    public static class SimpleProgressListener implements ProgressListener {
//        long totalBytes, bytesTransferred, start, end, lastLog;
//
//        public SimpleProgressListener(long totalBytes) {
//            this.totalBytes = totalBytes;
//            this.bytesTransferred = 0;
//            this.start = 0;
//            this.end = 0;
//            this.lastLog = start;
//        }
//
//        public void progressChanged(ProgressEvent progressEvent) {
//            if (progressEvent.getEventCode() == ProgressEvent.STARTED_EVENT_CODE) {
//                this.start = System.currentTimeMillis();
//                this.lastLog = this.start;
//            }
//            this.bytesTransferred += progressEvent.getBytesTransfered();
//            end = System.currentTimeMillis();
//            this.logProgress();
//        }
//
//        public void logProgress() {
//            // Print out progress every five seconds
//            if (this.end - this.lastLog > 5000) {
//                System.out.print("  " + (end - start) / 1000 + "s  ");
//                System.out.print(prettyByteString(bytesTransferred) + " ");
//                System.out.println(String.format(" \t(%2.2f%%)", bytesTransferred / (float) totalBytes));
//                this.lastLog = this.end;
//            }
//        }
//    }
}