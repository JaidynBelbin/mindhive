package com.example.yardenbourg.mindhivepodcast;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

import java.io.IOException;
import java.io.Serializable;

public class MindHiveLogin extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
                                                                View.OnClickListener, Serializable {
    // Tag to identify the log message
    private static final String LOG_TAG = MindHiveLogin.class.getSimpleName();

    // Success code to be returned from our sign in/authentication activity
    private static final int RC_SIGN_IN = 9001;

    public static GoogleApiClient googleApiClient;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private ProgressDialog mProgressDialog;
    // String holding the Client ID received from Google
    private String clientID;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.mindhive_login);

        // Initialise our Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        clientID = getString(R.string.server_client_id);

        // Initialising the authentication state listener
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();

                // Checking if the user has already signed in
                if (user != null) {

                    Log.d(LOG_TAG, "Signed in");

                } else {

                    Log.d(LOG_TAG, "Signed out");
                }
            }
        };

        // Initialising the Google Client to sign in with our app
        buildGoogleClient();

        findViewById(R.id.sign_in_button).setOnClickListener(this);

        // Getting the username saved in SharedPreferences, and the currently logged in users' name
        // to verify they match.
        // String sharedPrefsName = getPreferences(MODE_PRIVATE).getString("UserName", null);
        // String loggedInUserName = getLoggedInUserName();

        if (getLoggedInUserName() != null) {

            Log.d(LOG_TAG, getLoggedInUserName() + " signed in!");

            // Automatically getting a token for them and sending them to the next Activity, meaning
            // they do not have to click "Sign In" every time.
            new AuthenticateUser().execute();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }

    /**
     * Creates and initialises the Google Client we will use to connect to Google and sign in.
     */
    private void buildGoogleClient() {

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .requestIdToken(clientID)
                .requestEmail()
                .build();

        // Build GoogleAPIClient with the Google Sign-In API and the above options.
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

    }

    /**
     * Starting the retrieval process for an ID Token. If the user has already signed in before, user consent
     * is not required.
     */
    private void signIn() {

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Handles the result from the activity started by the above Intent. If the result of logging in is
     * a success, the user is signed in with Firebase with their account, and a Google token is retrieved and
     * passed to the next Activity
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if (result.isSuccess()) {

                // Getting the user and authenticating them with Firebase
                GoogleSignInAccount acct = result.getSignInAccount();
                firebaseAuthWithGoogle(acct);

                // Getting a Google token
                new AuthenticateUser().execute();
            }
        }
    }

    /**
     * Uses the users Google sign-in result to authenticate with Firebase, which stores details about
     * the user.
     * @param acct
     */
    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (!task.isSuccessful()) {

                            Log.w(LOG_TAG, "signInWithCredential", task.getException());
                            Toast.makeText(MindHiveLogin.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Retrieving the Google Token
     */
    private class AuthenticateUser extends AsyncTask<Void, Void, String> {

        String token;
        @Override
        protected String doInBackground(Void... params) {

            try {

                AccountManager am = AccountManager.get(getApplicationContext());
                Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
                token = GoogleAuthUtil.getToken(getApplicationContext(), accounts[0].name,
                        "audience:server:client_id:" + clientID);

            } catch(GoogleAuthException ex) {
                Log.d(LOG_TAG, "GoogleAuthException has been thrown by GetAndSetGoogleToken!");

            } catch(IOException ex2) {

                Log.d(LOG_TAG, "IOException has been thrown by GetAndSetGoogleToken!");
            }

            return token;
        }

        @Override
        protected void onPostExecute(String token) {

            goToNextActivity(token);
        }
    }

    /**
     * Gets the readable display name of the logged in user, i.e. Jaidyn Belbin
     * @return
     */
    private String getLoggedInUserName() {

        String name;

        if (firebaseAuth.getCurrentUser() != null) {

            for (UserInfo info : firebaseAuth.getCurrentUser().getProviderData()) {

                name = info.getDisplayName();
                return name;
            }
        }

        return null;
    }

    private void goToNextActivity(String token) {

        Intent intent = new Intent(getApplicationContext(), MindHiveMainScreen.class);
        intent.putExtra("ID Token", token);

        if (getLoggedInUserName() != null) {
            intent.putExtra("UserName", getLoggedInUserName());
        }

        startActivity(intent);
    }

    /**
     * Sign out method that signs the user out and updates the UI.
     */
    private void signOut() {

        firebaseAuth.signOut();

        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback (
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Log.d(LOG_TAG, "signOut:onResult:" + status);
                        finish();
                    }
                });
    }

    /**
     * Method to revoke access, i.e. erase the saved user credentials so they do not get
     * automatically signed in.
     */
    private void revokeAccess() {

        firebaseAuth.signOut();

        Auth.GoogleSignInApi.revokeAccess(googleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Log.d(LOG_TAG, "revokeAccess:onResult:" + status);
                        finish();
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
        Log.d(LOG_TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }
}
