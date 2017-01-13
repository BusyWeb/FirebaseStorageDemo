package com.busyweb.firebaselogindemo.firebase;

import com.google.firebase.storage.UploadTask;

/**
 * Created by BusyWeb on 1/11/2017.
 */

public class MyFileInfo {

    public String FileName;
    public String DownloadLink;
    public Long UploadTime;
    public String Key;
    public String FileUri;

    public MyFileInfo() {}
    public MyFileInfo(String fileUri, String fileName, String downloadLink) {
        FileUri = fileUri;
        FileName = fileName;
        DownloadLink = downloadLink;
        UploadTime = System.currentTimeMillis();
    }

//        public Map<String, Object> toMap() {
//            HashMap<String, Object> result = new HashMap<>();
//            result.put("FileName", FileName);
//            result.put("DownloadLink", DownloadLink);
//            return result;
//        }
}
