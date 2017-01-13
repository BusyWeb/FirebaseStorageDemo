package com.busyweb.firebaselogindemo.firebase;

import android.bluetooth.BluetoothClass;
import android.os.ParcelFileDescriptor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.busyweb.firebaselogindemo.R;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by BusyWeb on 1/7/2017.
 */

public class FileListAdapter extends RecyclerView.Adapter<FileItemHolder> {

    public ArrayList<MyFileInfo> mFiles;
    public MyFirebaseShared.FileListAdapterEventListener mEventListener;

    public FileListAdapter(ArrayList<MyFileInfo> files, MyFirebaseShared.FileListAdapterEventListener eventListener) {
        mFiles = files;
        mEventListener = eventListener;
    }

    @Override
    public FileItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(MyFirebaseShared.gContext).inflate(R.layout.cardview_file_item, parent, false);
        FileItemHolder holder = new FileItemHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(FileItemHolder holder, int position) {
        try {
            final MyFileInfo myFileInfo = mFiles.get(position);
            holder.textViewName.setText(myFileInfo.FileName);
            String dt = MyFirebaseHelper.MyDateFormat.format(new Date(myFileInfo.UploadTime));
            holder.textViewDate.setText(dt);
            holder.imageButtonDownload.setTag(myFileInfo);
            holder.imageButtonDelete.setTag(myFileInfo);

            holder.imageButtonDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mEventListener != null) {
                        mEventListener.DownloadButtonClicked(myFileInfo);
                    }
                }
            });
            holder.imageButtonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mEventListener != null) {
                        mEventListener.DeleteButtonClicked(myFileInfo);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}
