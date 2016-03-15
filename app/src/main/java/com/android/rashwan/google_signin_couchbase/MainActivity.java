package com.android.rashwan.google_signin_couchbase;

import android.Manifest;
import android.accounts.Account;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener ,
        View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback{

    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_GIVE_EMAIL_SCOPE = 1;
    private static final int RC_PERM_GET_ACCOUNTS = 2;

    private static final String SERVER_CLIENT_ID = "435562047969-ufmr8am7j9rchr145p0j49udih764hvr.apps.googleusercontent.com";
    private static final String BACKEND_SERVER_URL = "http://192.168.1.15:9000/auth/api/authenticate/google";

    private static final String TAG = MainActivity.class.getSimpleName();

    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;

    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;

    /* Keys for persisting instance variables in savedInstanceState */
    private static final String KEY_IS_RESOLVING = "is_resolving";
    private static final String KEY_SHOULD_RESOLVE = "should_resolve";

    private TextView accountInfo ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Restore from saved instance state
        // [START restore_saved_instance_state]
        if (savedInstanceState != null) {
            mIsResolving = savedInstanceState.getBoolean(KEY_IS_RESOLVING);
            mShouldResolve = savedInstanceState.getBoolean(KEY_SHOULD_RESOLVE);
        }
        // [END restore_saved_instance_state]

        // TODO: 3/16/16 Remove this Ugly Code 
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(this);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setEnabled(false);

        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);
        accountInfo = (TextView) findViewById(R.id.account_info);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().setServerClientId(SERVER_CLIENT_ID).build())
                .addScope(Plus.SCOPE_PLUS_PROFILE).addScope(Plus.SCOPE_PLUS_LOGIN).build();


    }

    private void updateUI(Boolean isSignedIn){
        if (isSignedIn){
            Person person =  Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            if (person != null){
                accountInfo.setText(person.getDisplayName());
                Log.e("BBB","QWE");
            }else {
                Log.e("HMMMM","NO Person");
                accountInfo.setText("Signed in Error");
            }
            // Set button visibility
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
            findViewById(R.id.disconnect_button).setVisibility(View.VISIBLE);

        }else {
            Log.e("HMMM","FAILED");

            accountInfo.setText("Signed Out");
            findViewById(R.id.sign_in_button).setEnabled(true);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
            findViewById(R.id.disconnect_button).setVisibility(View.GONE);

        }
    }



    private void showSignedInUI() {
        updateUI(true);
    }

    private void showSignedOutUI() {
        updateUI(false);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_RESOLVING, mIsResolving);
        outState.putBoolean(KEY_SHOULD_RESOLVE, mShouldResolve);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG,"ONACTIVITYRESULT");
        if (requestCode == RC_SIGN_IN){
            Log.d(TAG,"ONACTIVITYRESULT SIGNIN RQUEST");

            if (resultCode != RESULT_OK) {
                Log.d(TAG,"ONACTIVITYRESULT SIGNIN RQUEST Result Bad");

                mShouldResolve = false;
            }
            Log.d(TAG,"ONACTIVITYRESULT SIGNIN RQUEST Result OK");

            mIsResolving = false;
            mGoogleApiClient.connect();
        }else if (requestCode == RC_GIVE_EMAIL_SCOPE){
            Log.e(TAG,String.valueOf(requestCode));
            String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
            new AccessTokenAsync().execute(email);
        }

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // onConnected indicates that an account was selected on the device, that the selected
        // account has granted any requested permissions to our app and that we were able to
        // establish a service connection to Google Play services.
        Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onConnected:" + bundle);
        mShouldResolve = false;
        showSignedInUI();
        if (checkAccountsPermission()){
            Log.d(TAG,"GRANTED AND CONNECTED");
            String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
            new AccessTokenAsync().execute(email);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost. The GoogleApiClient will automatically
        // attempt to re-connect. Any UI elements that depend on connection to Google APIs should
        // be hidden or disabled until onConnected is called again.
        Log.w(TAG, "onConnectionSuspended:" + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account,
        // grant permissions or resolve an error in order to sign in. Refer to the javadoc for
        // ConnectionResult to see possible error codes.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);

        if (!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Could not resolve ConnectionResult.", e);
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            } else {
                // Could not resolve the connection result, show the user an
                // error dialog.
                // TODO: 3/15/16  show the user an error dialog.
                Log.d(TAG,"Could not resolve the connection result");
                showErrorDialog(connectionResult);
            }
        } else {
            // Show the signed-out UI
            // TODO: 3/15/16  Show the signed-out UI
            Log.d(TAG,"Signed Out");
            showSignedOutUI();
        }

    }
    private void showErrorDialog(ConnectionResult connectionResult) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, RC_SIGN_IN,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                mShouldResolve = false;
                                showSignedOutUI();
                            }
                        }).show();
            } else {
                Log.w(TAG, "Google Play Services Error:" + connectionResult);
                String errorString = apiAvailability.getErrorString(resultCode);
                Toast.makeText(this, errorString, Toast.LENGTH_SHORT).show();

                mShouldResolve = false;
                showSignedOutUI();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.sign_in_button:
                onSignInClicked();
                break;
            case R.id.sign_out_button:
                onSignOutClicked();
                break;
            case R.id.disconnect_button:
                onDisconnectClicked();
                break;
        }
    }

    private void onSignInClicked() {
        // User clicked the sign-in button, so begin the sign-in process and automatically
        // attempt to resolve any errors that occur.
        mShouldResolve = true;
        mGoogleApiClient.connect();

        // Show a message to the user that we are signing in.
        Log.e(TAG,"Signing in");
    }

    private void onSignOutClicked() {
        // Clear the default account so that GoogleApiClient will not automatically
        // connect in the future.
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }

        showSignedOutUI();
    }

    private void onDisconnectClicked() {
        // Revoke all granted permissions and clear the default account.  The user will have
        // to pass the consent screen to sign in again.
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
        showSignedOutUI();
    }

    private class AccessTokenAsync extends AsyncTask<String,Void,JSONObject> {
        private final String TAG = AccessTokenAsync.class.getSimpleName();
        private static final String ACCESS_TOKEN_SCOPE = "oauth2:" + Scopes.PLUS_LOGIN + " https://www.googleapis.com/auth/plus.profile.emails.read";

        @Override
        protected JSONObject doInBackground(String... params) {
            Log.d(TAG,"IN DOINBG");
            String email = params[0];
            JSONObject request = new JSONObject();
            JSONObject info = new JSONObject();
            String accessToken = null;

            try {
                if (checkAccountsPermission()) {
                    try {
                        Account account = new Account(email, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
                        accessToken = GoogleAuthUtil.getToken(getApplicationContext(), account, ACCESS_TOKEN_SCOPE);
                    }  catch (UserRecoverableAuthException e) {
                        Log.d(TAG,"USERRECOVERABLEAUTH");
                        startActivityForResult(e.getIntent(),RC_GIVE_EMAIL_SCOPE);
                        return null;
                    }

                    Log.d("BlaVla", accessToken);

                    try {
                        info.put("accessToken", accessToken);
                        request.put("email", email);
                        request.put("info", info);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } catch (GoogleAuthException | IOException e) {
                e.printStackTrace();
            }
            Log.d("BlaVla", request.toString());

            return request;
        }

        @Override
        protected void onPostExecute(JSONObject info) {
            // Store or use the user's email address
            sendToServer(info);
        }


        private void sendToServer(JSONObject request){
            Log.d(TAG,"SENDING TO SERVER");
            if (request != null){
                try {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(BACKEND_SERVER_URL);
                    StringEntity entity = new StringEntity(request.toString());
                    entity.setContentType("application/json;charset=UTF-8");
                    entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8"));
                    httpPost.setEntity(entity);
                    HttpResponse response = httpClient.execute(httpPost);
                    final String responseBody = EntityUtils.toString(response.getEntity());
                    Log.i(TAG, "Signed in as: " + responseBody);

                } catch (ClientProtocolException e) {
                    Log.e(TAG, "Error sending ID token to backend.", e);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private boolean checkAccountsPermission() {
        Log.e(TAG,"CHECKINGPErmisiion");
        final String perm = Manifest.permission.GET_ACCOUNTS;
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), perm);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // We have the permission
            Log.e(TAG,"Permission Granted");
            return true;
        } else {
            Log.e(TAG,"Permission NOT Granted REqusting it");

            // we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{perm},
                    RC_PERM_GET_ACCOUNTS);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult:" + requestCode);
        if (requestCode == RC_PERM_GET_ACCOUNTS){
            if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
                new AccessTokenAsync().execute(email);
            } else {
                Log.d(TAG, "GET_ACCOUNTS Permission Denied.");
            }
        }
    }
}
