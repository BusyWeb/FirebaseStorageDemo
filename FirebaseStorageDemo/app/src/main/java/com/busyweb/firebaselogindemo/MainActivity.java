package com.busyweb.firebaselogindemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.busyweb.firebaselogindemo.firebase.MyFirebaseShared;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "FirebaseStorageDemo";
    private static final int REQUEST_SIGN_IN_ID = 9999;
    private static final int REQUEST_CODE_STORAGE_ACCESS = 1111;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private GoogleApiClient mGoogleApiClient;

    private Activity mActivity;
    private Context mContext;

    private TextView mTextViewHello;
    private SignInButton mSignInButton;
    private Button mSignOutButton;
    private ProgressDialog mProgressDialog;

    private ImageButton mViewButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
        mSignInButton.setSize(SignInButton.SIZE_WIDE);
        mSignInButton.setOnClickListener(this);

        mSignOutButton = (Button) findViewById(R.id.sign_out_button);
        mSignOutButton.setOnClickListener(this);

        mTextViewHello = (TextView) findViewById(R.id.textViewHello);

        mViewButton = (ImageButton) findViewById(R.id.imageButtonView);
        mViewButton.setOnClickListener(this);

        prepareApp();
    }

    private void prepareApp() {
        try {
            mActivity = this;
            mContext = this;
            MyFirebaseShared.gActivity = this;
            MyFirebaseShared.gContext = this;

            GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(MyFirebaseShared.WEB_CLIENT_ID)
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                    .build();

            mFirebaseAuth = FirebaseAuth.getInstance();

            mAuthStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    MyFirebaseShared.FbUser = firebaseAuth.getCurrentUser();
                    MyFirebaseShared.FbRefreshToken = FirebaseInstanceId.getInstance().getToken();

                    if (MyFirebaseShared.FbUser != null) {
                        // user signed in
                        Log.i(TAG, "User signed in (uid): " + MyFirebaseShared.FbUser.getUid());
                    } else {
                        Log.i(TAG, "User signed out.");
                    }

                    updateAppUi(MyFirebaseShared.FbUser);
                }
            };

         } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.i(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        showProgressDialog("Wait...");

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.i(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.i(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        hideProgressDialog();
                    }
                });
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.imageButtonView:
                viewFiles();
                break;
        }
    }

    @Override
    public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        if (requestCode == REQUEST_SIGN_IN_ID) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(resultData);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                updateAppUi(null);
            }
        }

    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    private void signIn() {
        if (MyFirebaseShared.FbUser != null) {
            signOut();
        }

        Intent intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(intent, REQUEST_SIGN_IN_ID);
    }

    private void signOut() {
        mFirebaseAuth.signOut();

        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateAppUi(null);
                    }
                }
        );
    }

    private void showProgressDialog(final String message) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog = ProgressDialog.show(mContext, null, message);
            }
        });
    }

    private void hideProgressDialog() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
            }
        });
    }

    private void updateAppUi(final FirebaseUser user) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (user == null) {
                    mTextViewHello.setText("Sign In");
                    mSignInButton.setVisibility(View.VISIBLE);
                    mSignOutButton.setVisibility(View.GONE);

                    mViewButton.setEnabled(false);
                } else {
                    mTextViewHello.setText(user.getEmail());
                    mSignInButton.setVisibility(View.INVISIBLE);
                    mSignOutButton.setVisibility(View.VISIBLE);

                    mViewButton.setEnabled(true);
                }
            }
        });
    }

    private void viewFiles() {
        if (MyFirebaseShared.FbUser == null) {
            Toast.makeText(mContext, "Please sign-in first, and try again.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(mContext, ManageCloudFiles.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mContext.startActivity(intent);
    }



}
