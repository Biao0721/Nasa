package com.example.nasa;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PhotoFragment extends Fragment {
    private static final String TAG = "PhotoFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<MarsItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private SQLiteDatabase mDatabase;

    public static PhotoFragment newInstance(){
        return new PhotoFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute();  // 获取网站json

        mDatabase = new DatabaseHelper(getActivity()).getWritableDatabase();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownload(PhotoHolder target, Bitmap thumbnail,byte[] bytes,String url) {
                ContentValues values = new ContentValues();
                values.put("url",url);
                values.put("image",bytes);
                mDatabase.insert("nasa",null,values);
                Drawable drawable = new BitmapDrawable(getResources(),thumbnail);
                target.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo,container,false);

        mPhotoRecyclerView = v.findViewById(R.id.fragment_photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));

        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        mThumbnailDownloader.clearQueue();
        Log.i(TAG,"Background thread destroyed");
    }

    private class FetchItemsTask extends AsyncTask<Void,Void,List<MarsItem>>{
        @Override
        protected List<MarsItem> doInBackground(Void... voids) {
            return new FlickrFetcher().fetchItems("https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos?sol=1000&api_key=DEMO_KEY");
        }

        @Override
        protected void onPostExecute(List<MarsItem> marsItems) {
            mItems = marsItems;
            setupAdapter();
        }
    }

    public class PhotoHolder extends RecyclerView.ViewHolder{
        private ImageView mItemImageView;

        public PhotoHolder(View itemView){
            super(itemView);
            mItemImageView = itemView.findViewById(R.id.mars_photo_image_view);
        }

        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdaper extends RecyclerView.Adapter<PhotoHolder>{
        private List<MarsItem> mItems;

        public PhotoAdaper(List<MarsItem> items){
            mItems = items;
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            //实例化mars_item布局
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.mars_item,parent,false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
            MarsItem marsItem = mItems.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.biao);
            holder.bindDrawable(placeholder);
            // 判断照片是否已经存在数据库了
            Cursor cursor = mDatabase.query("nasa", null, "url = ? ", new String[] {marsItem.getUrl()}, null, null, null);
            if(cursor.getCount() == 0){
                mThumbnailDownloader.queueThumbnail(holder,marsItem.getUrl());
            }
            else{
                cursor.moveToFirst();
                byte[] bytes = cursor.getBlob(cursor.getColumnIndex("image"));
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                Drawable drawable = new BitmapDrawable(getResources(),bitmap);
                holder.bindDrawable(drawable);
            }
            cursor.close();
        }
    }

    private void setupAdapter(){
        if(isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdaper(mItems));
        }
    }
}
