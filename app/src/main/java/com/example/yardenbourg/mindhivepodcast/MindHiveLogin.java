package com.example.yardenbourg.mindhivepodcast;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import com.facebook.FacebookSdk;

import java.util.HashMap;
import java.util.Map;

public class MindHiveLogin extends AppCompatActivity {

    static final String TAG = "MindHiveLogin";

    private TextView statusTextView;
    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private AccessTokenTracker accessTokenTracker;

    static AccessToken accessToken;

    static CognitoCachingCredentialsProvider credentialsProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /**
         * Initialising the Facebook SDK
         */
        FacebookSdk.sdkInitialize(getApplicationContext());

        // Initialising the callback manager
        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.mindhive_login);

        // Initialising the Credentials Provider
        initCognitoCachingCredentialsProvider();

        // If no change to the AccessToken has been made, start the login flow with the current AccessToken
        startLoginFlow();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
    }

    /**
     * This method checks to see if an AccessToken has been given already and if there is
     * a valid Cognito Identity, if so, the user is sent to the next Activity; if not,
     * the usual login flow is started and an AccessToken is given.
     */
    private void startLoginFlow() {

        accessToken = AccessToken.getCurrentAccessToken();

        // If there is no AccessToken...
        if (accessToken == null) {

            // Show the login screen so that one can be provided
            initLoginUI();

        } else {

            // Setting the Credentials, and refreshing the credentialsProvider
            setCognitoLogins(accessToken);
            new RefreshCredentials().execute();

            // Sending the user to the next Activity
            Intent mainActivityIntent = new Intent(getApplicationContext(), MindHiveMain.class);

            startActivity(mainActivityIntent);
        }
    }

    /**
     * Initialising the login UI
     */
    private void initLoginUI() {

        // Instantiating the Views
        statusTextView = (TextView) findViewById(R.id.status);
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("email");

        // Setting up the Facebook Login Button to handle
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {

                accessToken = loginResult.getAccessToken();

                if (accessToken != null) {

                    Log.i("onSuccess", "Access Token is valid!");

                    // Setting the login token in the credentialsProvider
                    setCognitoLogins(accessToken);

                    //... and refreshing it in an Async task
                    new RefreshCredentials().execute();

                    //TODO: Send the Credentials and AccessToken to next Activity as well.
                    // Sending the user to the next Activity
                    Intent mainActivityIntent = new Intent(getApplicationContext(), MindHiveMain.class);

                    startActivity(mainActivityIntent);
                }
            }

            @Override
            public void onCancel() {

                statusTextView.setText(R.string.login_cancel_message);
                updateUI(false);
            }

            @Override
            public void onError(FacebookException error) {

                statusTextView.setText(R.string.login_failed_message);
                updateUI(false);
            }
        });
    }

    /**
     * Method that initialises the CognitoCachingCredentialsProvider so our app can communicate
     * with AWS
     */
    private void initCognitoCachingCredentialsProvider() {

        // Initializing the Amazon Credentials Provider
        credentialsProvider = new CognitoCachingCredentialsProvider (
                getApplicationContext(),
                "611688356871", // AWS Account ID
                "us-east-1:b97a7eee-bfca-4c77-bccb-e422475f8613", // Identity Pool ID
                "arn:aws:iam::611688356871:role/Cognito_mind_hive_identity_poolUnauth_Role", // Unauthenticated Role
                "arn:aws:iam::611688356871:role/Cognito_mind_hive_identity_poolAuth_Role", // Authenticated Role
                Regions.US_EAST_1 // Region
        );
    }

    /**
     * Puts the given AccessToken into the logins Map, and sets it in the credentialsProvider
     * @param accessToken
     */
    private void setCognitoLogins(AccessToken accessToken) {

        Map<String, String> logins = new HashMap<>();
        logins.put("graph.facebook.com", accessToken.getToken());
        credentialsProvider.setLogins(logins);
    }

    /**
     * Updates the UI to show sign-in or sign-out options.
     */
    private void updateUI(boolean signedIn) {

        if (signedIn) {

            statusTextView.setText(R.string.logged_in);
        } else {

            statusTextView.setText(R.string.logged_out);
        }
    }

    /**
     * Refreshes the credentialsProvider in a background thread, as it requires a network request
     */
    private class RefreshCredentials extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            credentialsProvider.refresh();
            return null;
        }
    }
}
