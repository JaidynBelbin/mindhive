package com.example.yardenbourg.mindhivepodcast;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.ArrayList;

/**
 * Created by Yardenbourg on 29/08/16.
 */
public final class FragmentUtilities {

    private FragmentUtilities() {}

    /**
     * Generic method to download all records from S3 from a specified bucket name with a specfied prefix.
     * The titles of each record are placed in an ArrayList, which is then returned.
     *
     * Must be called from an AsyncTask.
     *
     * @param s3Client
     * @param bucketName
     * @param prefix
     * @return podcastArray
     */
    public static ArrayList<String> downloadPodcasts(AmazonS3Client s3Client, String bucketName, String prefix) {

        ArrayList<String> podcastArray = new ArrayList<>();
        final String LOG_TAG = FragmentUtilities.class.getSimpleName();

        try {
            // Makes a new request to the bucket to List the objects from the specified
            // bucket and prefix, allowing us to access the Objects in each folder separately.
            ListObjectsRequest podcastRequest = new ListObjectsRequest()
                    .withBucketName(bucketName)
                    .withPrefix(prefix);

            ObjectListing objectListing;

            do {
                objectListing = s3Client.listObjects(podcastRequest);

                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {

                    podcastArray.add(objectSummary.getKey());
                }

                podcastRequest.setMarker(objectListing.getNextMarker());

            } while (objectListing.isTruncated());

            // Logging any problems we may encounter, such as invalid credentials
            // or not enough privileges on the bucket itself.
        } catch(AmazonServiceException ex) {

            Log.d(LOG_TAG, "Caught an AmazonServiceException, " +
                    "which means your request made it " +
                    "to Amazon S3, but was rejected with an error response " +
                    "for some reason.");
            Log.d(LOG_TAG, "Error Message:    " + ex.getMessage());
            Log.d(LOG_TAG, "HTTP Status Code: " + ex.getStatusCode());
            Log.d(LOG_TAG, "AWS Error Code:   " + ex.getErrorCode());
            Log.d(LOG_TAG, "Error Type:       " + ex.getErrorType());
            Log.d(LOG_TAG, "Request ID:       " + ex.getRequestId());

        } catch(AmazonClientException ex2) {

            Log.d(LOG_TAG, "Caught an AmazonClientException, " +
                    "which means the client encountered " +
                    "an internal error while trying to communicate" +
                    " with S3, " +
                    "such as not being able to access the network.");
            Log.d(LOG_TAG, "Error Message: " + ex2.getMessage());
        }

        // Returning that ArrayList
        return podcastArray;
    }
}
