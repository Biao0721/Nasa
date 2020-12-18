package com.example.nasa;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ConcurrentHashMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private Boolean mHasQuit = false;
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ConcurrentHashMap<T,String> mRequestMap = new ConcurrentHashMap<>();
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T>{   //抽象类
        void onThumbnailDownload(T target,Bitmap thumbnail,byte[] bytes,String url);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler requestHandler){
        super(TAG);
        mResponseHandler = requestHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){  //handler处理一个就下载一个显示一个
            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what==MESSAGE_DOWNLOAD){
                    T target = (T)msg.obj;
                    Log.i(TAG,"Got a reuqest for URL: "+mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target,String url){
        Log.i(TAG,"Got a url: "+url);

        if(url == null){
            mRequestMap.remove(target);
        }else{
            mRequestMap.put(target,url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target).sendToTarget();
        }
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    private void handleRequest(final T target){
        // 下载图片
        try{
            final String url = mRequestMap.get(target);

            if(url==null){
                return;
            }

            final byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
            Log.i(TAG,"Bitmap created");

            mResponseHandler.post(new Runnable() {      //post新建消息，传给handler处理，其实是在主线程中完成
                @Override
                public void run() {
                    if(mRequestMap.get(target)!=url||mHasQuit){
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownload(target,bitmap,bitmapBytes,url);
               }
            });
        }catch (Exception e){
            Log.e(TAG,"Error downloading image",e);
        }
    }
}
