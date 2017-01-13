package com.busyweb.firebaselogindemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.busyweb.firebaselogindemo.firebase.FileListAdapter;
import com.busyweb.firebaselogindemo.firebase.MyFileInfo;
import com.busyweb.firebaselogindemo.firebase.MyFirebaseHelper;
import com.busyweb.firebaselogindemo.firebase.MyFirebaseShared;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

public class ManageCloudFiles extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "FirebaseStorageDemo";
    private static final int REQUEST_CODE_STORAGE_ACCESS = 1111;
    private static final int REQUEST_CODE_STORAGE_CREATE = 2222;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private GoogleApiClient mGoogleApiClient;

    private Activity mActivity;
    private Context mContext;
    private ProgressDialog mProgressDialog;

    private MyFirebaseShared.WorkProcessing mWorkProcessing;
    private MyFirebaseShared.FileListAdapterEventListener mFileListAdapterEventListener;

    private ImageButton mButtonUpload;
    private ImageButton mButtonRefresh;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FileListAdapter mFileListAdapter;

    private static MyFileInfo mFileInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_cloud_files);


        prepareApp();
    }

    private void prepareApp() {
        try {
            mActivity = this;
            mContext = this;
            MyFirebaseShared.gActivity = this;
            MyFirebaseShared.gContext = this;

            mRecyclerView = (RecyclerView) findViewById(R.id.recyclerViewFiles);
            mLinearLayoutManager = new LinearLayoutManager(this);
            mLinearLayoutManager.setSmoothScrollbarEnabled(true);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(mLinearLayoutManager);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());

            mButtonUpload = (ImageButton) findViewById(R.id.imageButtonUpload);
            mButtonRefresh = (ImageButton) findViewById(R.id.imageButtonRefresh);

            mButtonUpload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    startActivityForResult(intent, REQUEST_CODE_STORAGE_ACCESS);
                }
            });

            mButtonRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    refreshDatabase();
                }
            });

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

                        prepareFirebaseCloud();

                    } else {
                        Log.i(TAG, "User signed out.");

                        finish();
                    }

                }
            };

            mWorkProcessing = new MyFirebaseShared.WorkProcessing() {
                @Override
                public void Progress(final double progress) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String formatted = String.format("%.0f", progress);

                            Log.i("DBG", "WorkProcessing: " + formatted);

                            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                                mProgressDialog.setMessage(formatted + " %");
                            } else {
                                showProgressDialog(formatted + " %");
                            }
                        }
                    });
                }

                @Override
                public void ProgressMessage(final String message) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                                mProgressDialog.setMessage(message);
                            } else {
                                showProgressDialog(message);
                            }
                        }
                    });
                }

                @Override
                public void Failed() {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Failed.", Toast.LENGTH_LONG).show();
                            hideProgressDialog();
                        }
                    });
                }

                @Override
                public void Done() {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideProgressDialog();
                        }
                    });
                }
            };


            mFileListAdapterEventListener = new MyFirebaseShared.FileListAdapterEventListener() {
                @Override
                public void DownloadButtonClicked(MyFileInfo myFileInfo) {

                    mFileInfo = myFileInfo;

                    String fileNameRemote = myFileInfo.FileName;
                    Uri fileUri = Uri.parse(mFileInfo.FileUri);
                    DocumentFile documentFile = DocumentFile.fromSingleUri(MyFirebaseShared.gContext, fileUri);

                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    //intent.setType("image/*");
                    intent.setType(documentFile.getType());
                    intent.putExtra(Intent.EXTRA_TITLE, fileNameRemote);
                    startActivityForResult(intent, REQUEST_CODE_STORAGE_CREATE);
                }

                @Override
                public void DeleteButtonClicked(MyFileInfo myFileInfo) {
                    new RemoveFileTask().execute(myFileInfo);
                }
            };

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshDatabase();
                }
            }, 2000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareFirebaseCloud() {
        try {
            // prepare firebase database listener
            MyFirebaseHelper.PrepareUserDatabaseConnection();

        } catch (Exception e) {
        }
    }

    private void prepareDatabaseChangedListener() {

        if (MyFirebaseShared.gDatabaseChangedListener == null) {
            MyFirebaseShared.gDatabaseChangedListener = new MyFirebaseShared.DatabaseChangedListener() {
                @Override
                public void DatabaseItemAdded(MyFileInfo myFileInfo) {
                    //MyFirebaseShared.UserFileInfoList.add(myFileInfo);
                    MyFirebaseShared.UserFileInfoList.add(0, myFileInfo);
                    if (mFileListAdapter != null) {
                        mFileListAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void DatabaseItemRemoved(String key) {
                    MyFileInfo myFileInfo = MyFirebaseHelper.GetMyFileInfoByKey(key, MyFirebaseShared.UserFileInfoList);
                    MyFirebaseShared.UserFileInfoList.remove(myFileInfo);
                    if (mFileListAdapter != null) {
                        mFileListAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void DatabaseItemChanged(MyFileInfo myFileInfo) {
                    MyFirebaseHelper.UpdateMyFileInfo(MyFirebaseShared.UserFileInfoList, myFileInfo);
                    if (mFileListAdapter != null) {
                        mFileListAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void DatabaseItemMoved(MyFileInfo myFileInfo) {
                    MyFirebaseHelper.UpdateMyFileInfo(MyFirebaseShared.UserFileInfoList, myFileInfo);
                    if (mFileListAdapter != null) {
                        mFileListAdapter.notifyDataSetChanged();
                    }
                }
            };
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        if (requestCode == REQUEST_CODE_STORAGE_ACCESS) {
            Uri uri = null;
            if (resultCode == Activity.RESULT_OK) {
                // Get Uri from Storage Access Framework.
                uri = resultData.getData();

                String path = uri.toString();
                Log.i("DBG", path);

                // Persist access permissions.
                int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                mActivity.getContentResolver().takePersistableUriPermission(uri, takeFlags);

                uploadFile(uri);
            }
        } else if (requestCode == REQUEST_CODE_STORAGE_CREATE) {
            Uri uri = null;
            if (resultCode == Activity.RESULT_OK) {
                uri = resultData.getData();
                // Persist access permissions.
                int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                mActivity.getContentResolver().takePersistableUriPermission(uri, takeFlags);

                downloadFile(uri, mFileInfo);
            }
        }
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

    @Override
    public void finish() {
        super.finish();

        if (mValueEventListener != null) {
            MyFirebaseShared.UserDatabaseReference.removeEventListener(mValueEventListener);
        }
        if (MyFirebaseShared.DatabaseChildEventListener != null) {
            MyFirebaseShared.UserDatabaseReference.removeEventListener(MyFirebaseShared.DatabaseChildEventListener);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

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


    private void uploadFile(Uri uri) {
        if (MyFirebaseShared.FbUser == null) {
            Toast.makeText(mContext, "Please sign-in first, and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        MyFirebaseHelper.UploadFile(mContext, uri, mWorkProcessing);
    }

    private void downloadFile(Uri uriToSave, MyFileInfo myFileInfo) {
        new DownloadFileTask(uriToSave, myFileInfo).execute();
    }

    private ValueEventListener mValueEventListener = null;
    private Query mMyQuery = null;

    private void refreshDatabase() {

        MyFirebaseShared.UserFileInfoList.clear();
        mRecyclerView.setAdapter(null);

        //new LoadFilesTask().execute();

        showProgressDialog("Loading...");

        if (mMyQuery != null && mValueEventListener != null) {
            mMyQuery.removeEventListener(mValueEventListener);
        }

        mMyQuery = MyFirebaseShared.UserDatabaseReference.orderByChild("UploadTime");
        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    MyFileInfo myFileInfo = snapshot.getValue(MyFileInfo.class);
                    if (myFileInfo != null) {
                        MyFirebaseShared.UserFileInfoList.add(myFileInfo);
                    }
                }
                updateFileList();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        mMyQuery.addValueEventListener(mValueEventListener);
    }

    private void updateFileList() {
        try {
            Collections.reverse(MyFirebaseShared.UserFileInfoList);

            mFileListAdapter = new FileListAdapter(MyFirebaseShared.UserFileInfoList, mFileListAdapterEventListener);
            mRecyclerView.setAdapter(mFileListAdapter);
        } catch (Exception e){

        } finally {
            if (mMyQuery != null && mValueEventListener != null) {
                mMyQuery.removeEventListener(mValueEventListener);
            }

            prepareDatabaseChangedListener();
        }
        hideProgressDialog();
    }

    private void saveBitmap(Uri uri, Bitmap bitmap, MyFileInfo myFileInfo) {
        boolean success = MyFirebaseHelper.SaveBitmap(uri, bitmap, myFileInfo);

        String mimeType = MyFirebaseHelper.GetFileMimeType3(mContext, uri);
        if (mimeType.length() < 1) {
            mimeType = "image/*";
        }
        if (success) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Select viewer.");
            intent.setDataAndType(uri, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(Intent.createChooser(intent, "Select Viewer"));
        } else {
            Toast.makeText(mContext, "Failed to downloading.", Toast.LENGTH_SHORT).show();
        }
    }

    private class RemoveFileTask extends AsyncTask<MyFileInfo, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            showProgressDialog("Processing...");
        }

        @Override
        protected Boolean doInBackground(MyFileInfo... myFileInfos) {
            MyFileInfo myFileInfo = myFileInfos[0];

            MyFirebaseHelper.DeleteStorageFile(myFileInfo);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            hideProgressDialog();
        }
    }

    private class LoadFilesTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            showProgressDialog("Processing...");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            if (mValueEventListener != null) {
                mMyQuery.removeEventListener(mValueEventListener);
            }
            //mMyQuery = MyFirebaseShared.UserDatabaseReference.orderByChild("UploadTime");
            mMyQuery = MyFirebaseShared.UserDatabaseReference;
            mValueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        MyFileInfo myFileInfo = snapshot.getValue(MyFileInfo.class);
                        if (myFileInfo != null) {
                            MyFirebaseShared.UserFileInfoList.add(myFileInfo);
                        }
                    }
                    updateFileList();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            mMyQuery.addValueEventListener(mValueEventListener);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            //hideProgressDialog();
        }
    }



    private class DownloadFileTask extends AsyncTask<Void, Void, Bitmap> {

        private Uri uriToSave;
        private MyFileInfo fileInfo;

        public DownloadFileTask(Uri uri, MyFileInfo myFileInfo) {
            this.fileInfo = myFileInfo;
            this.uriToSave = uri;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog("Downloading...");
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {

            String fileUrl = fileInfo.DownloadLink;
            Bitmap bitmap = null;

            try {
                InputStream inputStream = new URL(fileUrl).openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            saveBitmap(uriToSave, bitmap, fileInfo);

            hideProgressDialog();
        }
    }
}
