package com.example.yardenbourg.mindhivepodcast;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MindHiveMainScreen extends AppCompatActivity {

    // Objects used to connect to and download from, Amazon S3.
    private static CognitoCachingCredentialsProvider credentialsProvider;
    private static AmazonSQSClient sqsClient;
    private static boolean bucketHasChanged;
    private MindHiveFragment mindhiveFragment;
    private KinwomenFragment kinwomenFragment;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mindhive_main_screen);

        setupActionBar();

        // Initialising the CognitoCachingCredentialsProvider
        initCredentialsProvider();

        // Retrieving the Google ID token from the Intent
        token = getIntent().getStringExtra("ID Token");

        // Using the token passed in the Intent to log in to the credentialsProvider above.
        if (token != "") {
            loginWithToken(token);
        }

        // Creating the ViewPager...
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);

        // Instantiating the Fragments to use down below...
        mindhiveFragment = MindHiveFragment.newInstance(getCredentialsProvider());
        kinwomenFragment = KinwomenFragment.newInstance(getCredentialsProvider());

        //... and adding them to the ViewPager which will also define the number of tabs.
        setupViewPager(viewPager);

        // Polling the SQS queue for messages...
        new PollMessageQueue().execute();

        // Creating the TabLayout, and assigning the ViewPager to it by calling
        // setupWithViewPager(), which basically connects the two.
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    /**
     * Sets up the ViewPager with its Adapter
     * @param viewPager
     */
    private void setupViewPager(ViewPager viewPager) {

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        if (mindhiveFragment != null && kinwomenFragment != null) {

            // Adding both Fragments, and the titles of each
            adapter.addFragment(mindhiveFragment, "Mindhive Podcast");
            adapter.addFragment(kinwomenFragment, "Kinwomen Podcast");
        }

        // Setting the adapter on the ViewPager
        viewPager.setAdapter(adapter);
    }

    /**
     * Sets up the Action Bar to display the logged in users name
     */
    private void setupActionBar() {

        // Handling the text in the ActionBar
        if (getSupportActionBar() != null) {

            ActionBar actionBar = getSupportActionBar();

            // Getting the users name from the Intent
            String userName = getIntent().getStringExtra("UserName");

            if (userName != null) {

                // Displaying the users name in the actionBar
                actionBar.setTitle("Welcome " + userName);

            } else {

                actionBar.setTitle("Choose a Podcast");
            }
        }
    }

    private void initCredentialsProvider() {

        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                getString(R.string.identity_pool_id),
                Regions.US_EAST_1
        );
    }

    /**
     * Returns a CognitoCachingCredentialsProvider instance ecto use in the
     * Fragments
     * @return
     */
    private CognitoCachingCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    /**
     * Initialises and returns an AmazonSQSClient instance
     * @return sqsClient
     */
    private AmazonSQSClient getSQSClient() {
        sqsClient = new AmazonSQSClient(getCredentialsProvider());
        return sqsClient;
    }

    /**
     * Polls the SQS queue attached to the bucket for messages. If there are any, it means the bucket
     * has either had an item added, or deleted. Either way, the list of podcasts needs
     * to be refreshed. If no messages exist, then the saved list of podcasts in SharedPreferences
     * is still current. It also deletes the messages once the result has been found.
     * @return
     */
    private boolean checkForBucketEvents() {

        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(getString(R.string.sqs_queue_url))
                .withWaitTimeSeconds(8);
        PurgeQueueRequest purgeQueueRequest = new PurgeQueueRequest(getString(R.string.sqs_queue_url));

        // Lists to hold the actual messages, and the message delete requests
        List<Message> messages = getSQSClient().receiveMessage(receiveMessageRequest).getMessages();

        if (!messages.isEmpty()) {

            Log.v("checkForBucketEvents", "Messages found!");

            // Purging the queue of all messages.
            getSQSClient().purgeQueue(purgeQueueRequest);

            Log.v("checkForBucketEvents", "Messages deleted.");
            return true; // A message exists
        }

        Log.v("checkForBucketEvents", "No messages exist");
        return false; // A message does not exist
    }

    /**
     * Polling the SQS queue asynchronously
     */
    private class PollMessageQueue extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {

            // This method returns a boolean representing if messages were found in the SQS queue.
            return checkForBucketEvents();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {

            // If the response was true, call the respective Fragment methods with true passed as the
            // argument.
            if (aBoolean) {
                kinwomenFragment.receiveBucketState(true);
                mindhiveFragment.receiveBucketState(true);

            } else {
                Log.v("PollMessageQueue", "Podcasts do not need updating.");
            }
        }
    }

    /**
     * Logs into Amazon AWS with the token provided from Google, retrieved from the Intent that started
     * this Activity
     */
    public void loginWithToken(String token) {
        setCredentials(token);
        new RefreshCredentials().execute();
    }

    /**
     * Sets the credentials into the logins map in the credentialsProvider
     */
    private void setCredentials(String token) {

        Map<String, String> logins = new HashMap<>();
        logins.put("accounts.google.com", token);
        credentialsProvider.withLogins(logins);
    }

    /**
     * Refreshes the credentialsProvider, which requires a network request
     */
    private class RefreshCredentials extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            credentialsProvider.refresh();

            return null;
        }
    }
}
