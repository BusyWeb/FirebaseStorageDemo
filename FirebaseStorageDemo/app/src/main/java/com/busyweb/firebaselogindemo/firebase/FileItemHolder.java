package com.busyweb.firebaselogindemo.firebase;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.busyweb.firebaselogindemo.R;

/**
 * Created by BusyWeb on 1/7/2017.
 */
public class FileItemHolder extends RecyclerView.ViewHolder {

    public CardView cardViewItem;
    public TextView textViewName;
    public TextView textViewDate;
    public ImageButton imageButtonDownload;
    public ImageButton imageButtonDelete;

    public int position;

    public FileItemHolder(View itemView) {
        super(itemView);

        cardViewItem = (CardView) itemView.findViewById(R.id.cardViewFileItem);
        textViewName = (TextView) itemView.findViewById(R.id.textViewFileName);
        textViewDate = (TextView) itemView.findViewById(R.id.textViewUploadDate);
        imageButtonDownload = (ImageButton) itemView.findViewById(R.id.imageButtonDownload);
        imageButtonDelete = (ImageButton) itemView.findViewById(R.id.imageButtonDelete);
    }
}
