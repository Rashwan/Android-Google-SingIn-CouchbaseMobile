package com.android.rashwan.google_signin_couchbase;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

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


public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener , View.OnClickListener{

    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private static final String SERVER_CLIENT_ID = "435562047969-ufmr8am7j9rchr145p0j49udih764hvr.apps.googleusercontent.com";


    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestIdToken(SERVER_CLIENT_ID).build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this).addApi(Auth.GOOGLE_SIGN_IN_API,gso).build();

        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent,RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()){
            GoogleSignInAccount account = result.getSignInAccount();
            Toast.makeText(getApplicationContext(),account.getDisplayName(),Toast.LENGTH_SHORT).show();
            sendIdToken(account);
        }else {
            Toast.makeText(getApplicationContext(),"Failed to Sign in",Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);

    }
    public void sendIdToken(GoogleSignInAccount account){
        HttpClient httpClient = new DefaultHttpClient();
//        String urli = "https://www.googleapis.com/plus/v1/people/me?fields=id,name,displayName,image,emails&access_token=";
//        urli = urli.concat(account.getIdToken());
//        URI endpoint = null;
//        try {
//            endpoint = new URI(urli);
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//        HttpPost httpPost = new HttpPost(endpoint);
        HttpPost httpPost = new HttpPost("http://192.168.1.22:9000/auth/api/authenticate/google");
        JSONObject request = new JSONObject();
        JSONObject info = new JSONObject();

        try {
            info.put("accessToken",account.getIdToken());
            info.put("expiresIn",60);
            request.put("email",account.getEmail());
            request.put("info",info);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            StringEntity entity = new StringEntity(request.toString());
            entity.setContentType("application/json;charset=UTF-8");
            entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json;charset=UTF-8"));
            httpPost.setEntity(entity);
            Log.d("BlaVla", account.getIdToken());
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            final String responseBody = EntityUtils.toString(response.getEntity());
            Log.i(TAG, "Signed in as: " + response.toString());
        } catch (ClientProtocolException e) {
            Log.e(TAG, "Error sending ID token to backend.", e);
        } catch (IOException e) {
            Log.e(TAG, "Error sending ID token to backend.", e);
        }
    }
}
