package com.example.yardenbourg.mindhivepodcast;

import android.accounts.AccountManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

import java.util.HashMap;
import java.util.Map;


public class MindHiveLogin extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
                                                                View.OnClickListener {

    // Tag to identify the log message
    public static final String TAG = "ServerAuthCodeActivity";
    // Success code to be returned from our sign in/authentication activity
    private static final int RC_GET_AUTH_CODE = 9003;


    private GoogleApiClient mGoogleApiClient;
    private TextView mAuthCodeTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.mindhive_login);

        // Instantiating the View
        mAuthCodeTextView = (TextView) findViewById(R.id.detail);

        // Button click listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);


        /**
         * Configure sign-in to request offline access to the user's ID, basic
         * profile, and Google Drive. The first time you request a code you will
         * be able to exchange it for an access token and refresh token, which
         * you should store. In subsequent calls, the code will only result in
         * an access token. By asking for profile access (through
         * DEFAULT_SIGN_IN) you will also get an ID Token as a result of th
         * code exchange.
         */
        String serverClientId = getString(R.string.server_client_id);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .requestServerAuthCode(serverClientId)
                .requestEmail()
                .build();

        // Build GoogleAPIClient with the Google Sign-In API and the above options.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    /**
     * Start the retrieval process for a server auth code.  If requested, ask for a refresh
     * token.  Otherwise, only get an access token if a refresh token has been previously
     * retrieved.  Getting a new access token for an existing grant does not require
     * user consent.
     */
    private void getAuthCode() {

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_GET_AUTH_CODE);
    }

    /**
     * Method that handles the result from the activity started by the above Intent.
     * It gets the auth code if successful and displays it in the TextView. Here the authentication
     * code is also sent to the Amazon server to be validated.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GET_AUTH_CODE) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Log.d(TAG, "onActivityResult:GET_AUTH_CODE:success:" + result.getStatus().isSuccess());

            if (result.isSuccess()) {

                GoogleSignInAccount acct = result.getSignInAccount();

                String authCode = acct.getServerAuthCode();

                mAuthCodeTextView.setText(getString(R.string.auth_code_fmt, authCode));
                updateUI(true);

                // TODO(user): send code to server and exchange for access/refresh/ID tokens.
            } else {

                // Show signed-out UI.
                updateUI(false);
            }
        }
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

            ((TextView) findViewById(R.id.status)).setText(R.string.signed_in);
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);

        } else {

            ((TextView) findViewById(R.id.status)).setText(R.string.signed_out);
            mAuthCodeTextView.setText(getString(R.string.auth_code_fmt, "null"));
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                getAuthCode();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;
        }
    }
}
