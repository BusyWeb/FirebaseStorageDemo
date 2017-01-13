package com.busyweb.firebaselogindemo.firebase;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.busyweb.firebaselogindemo.MainActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by BusyWeb on 1/11/2017.
 */

public class MyFirebaseHelper {

    public static SimpleDateFormat MyDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:aa");

    public static void PrepareUserDatabaseConnection() {
        if (MyFirebaseShared.FbUser == null) {
            return;
        }
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        MyFirebaseShared.UserDatabaseReference = databaseReference.child(MyFirebaseShared.UserFilesFolderName)
                .child(MyFirebaseShared.FbUser.getUid());

        if (MyFirebaseShared.UserFileInfoList == null) {
            MyFirebaseShared.UserFileInfoList = new ArrayList<MyFileInfo>();
        }

        if (MyFirebaseShared.DatabaseChildEventListener == null) {
            MyFirebaseShared.DatabaseChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    MyFileInfo myFileInfo = dataSnapshot.getValue(MyFileInfo.class);
                    if (MyFirebaseShared.gDatabaseChangedListener != null) {
                        MyFirebaseShared.gDatabaseChangedListener.DatabaseItemAdded(myFileInfo);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    MyFileInfo myFileInfo = dataSnapshot.getValue(MyFileInfo.class);
                    if (MyFirebaseShared.gDatabaseChangedListener != null) {
                        MyFirebaseShared.gDatabaseChangedListener.DatabaseItemChanged(myFileInfo);
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    String dataKey = dataSnapshot.getKey();

                    //MyFileInfo myFileInfo = dataSnapshot.getValue(MyFileInfo.class);
                    if (MyFirebaseShared.gDatabaseChangedListener != null) {
                        MyFirebaseShared.gDatabaseChangedListener.DatabaseItemRemoved(dataKey);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    MyFileInfo myFileInfo = dataSnapshot.getValue(MyFileInfo.class);
                    if (MyFirebaseShared.gDatabaseChangedListener != null) {
                        MyFirebaseShared.gDatabaseChangedListener.DatabaseItemMoved(myFileInfo);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            MyFirebaseShared.UserDatabaseReference.addChildEventListener(MyFirebaseShared.DatabaseChildEventListener);
        }
    }


    public static void UploadFile(Context context, final Uri uri, final MyFirebaseShared.WorkProcessing workProcessing) {
        //String mCameraId = cameraId;
        if (MyFirebaseShared.FbUser != null) {

            final DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
            final String fileName = documentFile.getName();
            String contentType = documentFile.getType();

            Log.i("DBG", fileName + "--------" + contentType);

            if (contentType == null || contentType.length() < 1) {
                contentType = "*/*";
            }

            StorageMetadata metadata = new StorageMetadata.Builder()
                    //.setContentType("image/jpg")
                    .setContentType(contentType)
                    .build();

            StorageReference mStorageReference = FirebaseStorage.getInstance().getReference();
            StorageReference userReference = mStorageReference.child(MyFirebaseShared.UserFilesFolderName)
                    .child(MyFirebaseShared.FbUser.getUid())
                    .child(fileName);

            UploadTask uploadTask = userReference.putFile(uri, metadata);

            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    //System.out.println("Upload is " + progress + "% done");
                    if (workProcessing != null) {
                        workProcessing.Progress(progress);
                    }
                }
            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                    workProcessing.ProgressMessage("Upload paused.");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //workProcessing.ProgressMessage("Failed to upload.");
                    workProcessing.Failed();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getMetadata().getDownloadUrl();
                    String downloadLink = downloadUrl.toString();

                    Log.i("DBG", downloadLink);

                    MyFileInfo myFileInfo = new MyFileInfo(uri.toString(), fileName, downloadLink);
                    Log.i("DBG", MyDateFormat.format(new Date(myFileInfo.UploadTime)));
                    UpdateDatabase(myFileInfo, workProcessing);
                }
            });

        }
    }

    public static void DeleteStorageFile(final MyFileInfo myFileInfo) {
        try {
            StorageReference mStorageReference = FirebaseStorage.getInstance().getReference();
            StorageReference userReference = mStorageReference.child(MyFirebaseShared.UserFilesFolderName)
                    .child(MyFirebaseShared.FbUser.getUid())
                    .child(myFileInfo.FileName);

            userReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    RemoveDatabaseValue(myFileInfo);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });


        } catch (Exception e) {

        }
    }
    public static void RemoveDatabaseValue(final MyFileInfo myFileInfo) {
        try {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference userReference = databaseReference.child(MyFirebaseShared.UserFilesFolderName)
                    .child(MyFirebaseShared.FbUser.getUid())
                    .child(myFileInfo.Key);
            userReference.removeValue();
        } catch (Exception e) {
        }
    }


    public static void UpdateDatabase(MyFileInfo myFileInfo, MyFirebaseShared.WorkProcessing workProcessing) {
        try {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference userReference = databaseReference.child(MyFirebaseShared.UserFilesFolderName)
                    .child(MyFirebaseShared.FbUser.getUid())
                    .push();
            myFileInfo.Key = userReference.getKey();
            userReference.setValue(myFileInfo);

            workProcessing.Done();
        } catch (Exception e) {
            workProcessing.Failed();
        }
    }

    public static ArrayList<MyFileInfo> GetDataFromDatabase() {
        final ArrayList<MyFileInfo> myFileInfos = new ArrayList<MyFileInfo>();
        try {
            if (MyFirebaseShared.FbUser == null) {
                return myFileInfos;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return myFileInfos;
    }

    public static MyFileInfo GetMyFileInfoByKey(String key, ArrayList<MyFileInfo> items) {
        MyFileInfo myFileInfo = null;
        for(MyFileInfo item : items) {
            if (item.Key.equals(key)) {
                myFileInfo = item;
                break;
            }
        }
        return myFileInfo;
    }

    public static void UpdateMyFileInfo(ArrayList<MyFileInfo> items, MyFileInfo updated) {
        for(MyFileInfo item : items) {
            if (item.Key.equals(updated.Key)) {
                item.FileName = updated.FileName;
                item.UploadTime = updated.UploadTime;
                item.DownloadLink = updated.DownloadLink;
                break;
            }
        }
    }

    public static boolean SaveBitmap(Uri uri, Bitmap bitmap, MyFileInfo myFileInfo) {
        boolean success = false;
        try {
            if (bitmap == null) {
                return success;
            }

            OutputStream outputStream = MyFirebaseShared.gActivity.getContentResolver().openOutputStream(uri);
            if (myFileInfo.FileName.toLowerCase().endsWith("png")) {
                success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            } else {
                success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            }
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (Exception e) {
                }
                outputStream = null;
            }
        } catch (Exception e) {
        }
        return success;
    }

    public static Bitmap GetBitmapFromUri(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    public static Bitmap LoadBitmapFromInternet(String path) {
        Bitmap bitmap = null;
        try {
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static RoundedBitmapDrawable GetRoundBitmapDrawable(Resources resources, Bitmap source) {
        try {
            //Bitmap src = BitmapFactory.decodeResource(res, iconResource);
            RoundedBitmapDrawable dr =
                    RoundedBitmapDrawableFactory.create(resources, source);
            //dr.setCornerRadius(Math.max(source.getWidth(), source.getHeight()) / 2.0f);
            dr.setCircular(true);
            return  dr;
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
    }

    public static String GetFileMimeType2(String uri) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type= mime.getMimeTypeFromExtension(extension);
        }

        return type;
    }

    public static String GetFileMimeType3(Context context, Uri uri) {
        String type = "";
        try {
            DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
            String fileName = documentFile.getName();
            type = documentFile.getType();
        } catch (Exception e) {
        }
        return type;
    }

    public static String GetFileMimeType(Context context, Uri uri) {
        ContentResolver cr = context.getContentResolver();
        return cr.getType(uri);
    }

    public static String GetFilePathFromContentUri(Uri uri) {
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = MyFirebaseShared.gActivity.getContentResolver().query(uri, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }

    public static String GetFilePathFromUri(Uri uri, String type)
    {
        String[] projection = { MediaStore.Images.Media.DATA,
                MediaStore.Video.Media.DATA };
        String[] columns = {  MediaStore.MediaColumns.DATA };

        //Cursor cursor = AppShared.gActivity.getContentResolver().query(uri, projection, null, null, null);
        Cursor cursor = MyFirebaseShared.gActivity.getContentResolver().query(uri, columns, null, null, null);
        if (cursor == null) return null;
        int column_index = 0;
        if (type.equalsIgnoreCase("image")) {
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        } else if (type.equalsIgnoreCase("video")) {
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        }

        cursor.moveToFirst();

        //Uri filePathUri = Uri.parse(cursor.getString(column_index));
        //String fileName = filePathUri.getLastPathSegment().toString();

        String s = cursor.getString(column_index);
        cursor.close();
        return s;

        //InputStream inputStream = getContentResolver().openInputStream(uri);
    }
}
