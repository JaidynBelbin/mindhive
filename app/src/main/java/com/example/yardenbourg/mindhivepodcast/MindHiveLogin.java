package com.example.yardenbourg.mindhivepodcast;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

public class MindHiveLogin extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
                                                                View.OnClickListener {
    // Tag to identify the log message
    private static final String TAG = "ServerAuthCodeActivity";

    // Success code to be returned from our sign in/authentication activity
    private static final int RC_GET_ID_TOKEN = 9002;

    // String holding the Client ID received from Google
    private String clientID;

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.mindhive_login);

        findViewById(R.id.sign_in_button).setOnClickListener(this);

        // Initialising the Google Client to sign in with our app
        buildGoogleClient();
    }

    /**
     * Creates and initialises the Google Client we will use to connect to Google and sign in.
     */
    private void buildGoogleClient() {

        clientID = getString(R.string.server_client_id);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .requestIdToken(clientID)
                .requestEmail()
                .build();

        // Build GoogleAPIClient with the Google Sign-In API and the above options.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

    }

    /**
     * Starting the retrieval process for an ID Token. If the user has already signed in before, user consent
     * is not required.
     */
    private void getToken() {

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_GET_ID_TOKEN);
    }

    /**
     * Handles the result from the activity started by the above Intent. If the result of logging in is
     * a success, an ID Token is retrieved from Google and passed to the credentialsProvider.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GET_ID_TOKEN) {

            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Log.d(TAG, "onActivityResult:GET_ID_TOKEN:success:" + result.getStatus().isSuccess());

            if (result.isSuccess()) {

                GoogleSignInAccount acct = result.getSignInAccount();

                // Getting a token from Google and starting a new Activity with the Token
                // attached as an Extra.
                new GetGoogleToken().execute();

                // Show signed-in UI.
                //updateUI(true);

            } else {

                // Show signed-out UI.
                //updateUI(false);
            }
        }
    }

    /**
     * Gets a Google Token to pass to the credentialsProvider, and starts a new Activity with the
     * token passed as an Extra to the Intent
     */
    private class GetGoogleToken extends AsyncTask<Void, Void, String> {

        String token;

        @Override
        protected String doInBackground(Void... params) {

            try {

                GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

                AccountManager am = AccountManager.get(getApplicationContext());
                Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

                token = GoogleAuthUtil.getToken(getApplicationContext(), accounts[0].name,
                        "audience:server:client_id:" + clientID);

            } catch(GoogleAuthException ex) {
                Log.d(TAG, "GoogleAuthException has been thrown by GetAndSetGoogleToken!");

            } catch(IOException ex2) {

                Log.d(TAG, "IOException has been thrown by GetAndSetGoogleToken!");
            }

            return token;
        }

        @Override
        protected void onPostExecute(String token) {

            // Passing the ID Token as an Extra to the Intent and starting a new Activity.
            goToNextActivity(token);

            super.onPostExecute(token);
        }
    }

    /**
     * Sends the user to the next screen. Called when they have successfully logged in with Google
     */
    private void goToNextActivity(String token) {

        Intent intent = new Intent(getApplicationContext(), MindHiveMainScreen.class);
        intent.putExtra("ID Token", token);
        startActivity(intent);
    }

    /**
     * Sign out method that signs the user out and updates the UI.
     */
    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback (
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Log.d(TAG, "signOut:onResult:" + status);
                        updateUI(false);
                    }
                });
    }

    /**
     * Method to revoke access, i.e. erase the saved user credentials so they do not get
     * automatically signed in.
     */
    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Log.d(TAG, "revokeAccess:onResult:" + status);
                        updateUI(false);
                    }
                });
    }

    /**
     * Method to handle failed connections
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    /**
     * Method to update the UI to show sign-in or sign-out options.
     */
    private void updateUI(boolean signedIn) {

        if (signedIn) {

            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        } else {

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                getToken();
                break;
            /**
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break; */
        }
    }
}
