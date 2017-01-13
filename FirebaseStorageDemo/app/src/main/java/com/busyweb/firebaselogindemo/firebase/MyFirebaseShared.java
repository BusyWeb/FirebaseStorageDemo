package com.busyweb.firebaselogindemo.firebase;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by BusyWeb on 1/11/2017.
 */

public class MyFirebaseShared {

    private static MyFirebaseShared firebaseShared = null;
    public static MyFirebaseShared getInstance() {
        if (firebaseShared == null) {
            firebaseShared = new MyFirebaseShared();
        }
        return firebaseShared;
    }

    public static Activity gActivity;
    public static Context gContext;

    // Update Web Api Client Id: found from Google APIs Console project Web client
    // google-services.json
    //"client_id": "xxxxxxx.apps.googleusercontent.com"
    //"client_type": 3
    public static final String WEB_CLIENT_ID = "your-client-id.apps.googleusercontent.com";

    public static FirebaseUser FbUser = null;
    public static String FbRefreshToken = "";

    public static GoogleSignInAccount GsAccount = null;

    public static GoogleApiClient GaClient = null;

    public interface DatabaseChangedListener {
        public void DatabaseItemAdded(MyFileInfo myFileInfo);
        public void DatabaseItemRemoved(String key);
        public void DatabaseItemChanged(MyFileInfo myFileInfo);
        public void DatabaseItemMoved(MyFileInfo myFileInfo);
    }

    public static DatabaseChangedListener gDatabaseChangedListener = null;

    public static String UserFilesFolderName = "userfiles";

    public static ArrayList<MyFileInfo> UserFileInfoList = null;

    public static DatabaseReference UserDatabaseReference = null;
    public static ChildEventListener DatabaseChildEventListener = null;

    public interface WorkProcessing {
        public void Progress(double progress);
        public void ProgressMessage(String message);
        public void Failed();
        public void Done();
    }

    public interface FileListAdapterEventListener {
        public void DownloadButtonClicked(MyFileInfo myFileInfo);
        public void DeleteButtonClicked(MyFileInfo myFileInfo);
    }


}
