package com.example.yardenbourg.mindhivepodcast;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MindHiveMainScreen extends AppCompatActivity {

    public static final String LOG_TAG = MindHiveMainScreen.class.getSimpleName();

    private static CognitoCachingCredentialsProvider credentialsProvider;
    public static AmazonS3Client s3Client;
    private static TransferUtility transferUtility;

    private TextView infoView;
    private TextView keysView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mindhive_main_screen);

        // Initialising and returning the credentialsProvider
        credentialsProvider = getCredProvider();

        infoView = (TextView)findViewById(R.id.infoView);
        keysView = (TextView)findViewById(R.id.keysView);

        // Using the token received from Google to login to Amazon Cognito
        loginWithToken();

        new ListKeys().execute();
    }

    private class ListKeys extends AsyncTask<Void, Void, ArrayList<S3ObjectSummary>> {

        ArrayList<S3ObjectSummary> objectKeys = new ArrayList<>();

        @Override
        protected void onPreExecute() {

            infoView.setText("Bucket Contents: ");
            super.onPreExecute();
        }

        @Override
        protected ArrayList<S3ObjectSummary> doInBackground(Void... params) {

            if (s3Client == null) {

                s3Client = new AmazonS3Client(credentialsProvider);
            }

            try {

                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName("themindhive")
                        .withPrefix("mindhive podcast/");

                ObjectListing objectListing;

                do {
                    objectListing = s3Client.listObjects(listObjectsRequest);

                    for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {

                        // Adding each objectSummary to the list
                        objectKeys.add(objectSummary);
                    }

                    listObjectsRequest.setMarker(objectListing.getNextMarker());

                } while (objectListing.isTruncated());

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

            return objectKeys;
        }

        @Override
        protected void onPostExecute(ArrayList<S3ObjectSummary> objectKeys) {

            for (S3ObjectSummary summary : objectKeys) {

                keysView.append(summary.getKey() + "\n");
            }

            super.onPostExecute(objectKeys);
        }
    }

    /**
     * Logs into Amazon AWS with the token provided from Google, retrieved from the Intent that started
     * this Activity
     */
    private void loginWithToken() {

        String token = getIntent().getStringExtra("ID Token");

        if (token == null || token == "") {

            // If there is no token, send the user back to the main screen.
            finish();

        } else {

            setCredentials(token);
            new RefreshCredentials().execute();
        }
    }

    /**
     * Initialises and returns the credentialsProvider
     */
    public CognitoCachingCredentialsProvider getCredProvider() {

        // Initializing the Amazon Cognito Credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                getString(R.string.identity_pool_id), // Identity Pool ID
                Regions.US_EAST_1 // Example Region
        );

        return credentialsProvider;
    }

    /**
     * Sets the credentials
     */
    private void setCredentials(String token) {

        Map<String, String> logins = new HashMap<>();
        logins.put("accounts.google.com", token);
        credentialsProvider.withLogins(logins);
    }

    /**
     * Refreshes the credentialsProvider, which requires a network request
     */
    private class RefreshCredentials extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            credentialsProvider.refresh();

            // Getting the identityID of the user
            String identityID = credentialsProvider.getIdentityId();

            return identityID;
        }
    }
}
