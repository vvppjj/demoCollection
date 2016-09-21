package com.jeffrey.demo.okhttpdemo;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 用回调来通知状态
 * Created by Li on 2016/9/21.
 */

public class DownloadHelper {


    private static final String apkUrl = "http://192.168.0.160:3456/myapp.apk";
    private static final String YOUR_APP_NAME = "appName";
    private static final String APP_NEW_VERSION = "1.1.1";

    public interface DownloadListener{
        void start();
        void finish(File file);
        void error(String msg);
        void progress(int progress);
    }


    public static void downloadUpdateFile(DownloadListener  downloadListener) {
        //client 最好做成单例
        OkHttpClient  client = new OkHttpClient();
        File targetFile = createDownloadFile();
        boolean isNeedDownload = checkNeedDownload(targetFile,"");
        if (!isNeedDownload){
            //通知完成
            downloadListener.finish(targetFile);
        }

        File storeFile = createDownloadFile();
        Request request = new Request.Builder()
                .url(apkUrl)
                .build();
        Call call = client.newCall(request);
        try {
            //通知开始
            downloadListener.start();

            Response response = call.execute();
            InputStream inputStream = response.body().byteStream();
            FileOutputStream fileOutputStream = new FileOutputStream(storeFile);
            //一次读取1024个字节
            int size = 1024;
            //总大小
            long totalCount = Long.parseLong(response.header("Content-Length"));

            byte[] buffer = new byte[size];
            int readSize;
            long currentSize = 0; //已经下载的大小
            while ((readSize = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, readSize);
                currentSize +=readSize;
                //通知下当前进度
                downloadListener.progress(calcProgress(currentSize,totalCount));
            }
            inputStream.close();
            fileOutputStream.close();
            //通知完成
            downloadListener.finish(new File(targetFileName()));

        } catch (IOException e) {
            e.printStackTrace();
            //通知失败
            downloadListener.error(e.getMessage());
        }
    }

    private static File createDownloadFile() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(path, targetFileName());
        return file;
    }

    private static String targetFileName(){
        return YOUR_APP_NAME + APP_NEW_VERSION + ".apk";
    }
    /**
     * 是否需要下载
     * @return  true 需要下载
     *           false  不需要下载
     */
    private static  boolean checkNeedDownload(File targetFile,String targetMD5){
        if (targetFile != null &&targetFile.exists()){
            String apkMD5 = MD5.calculateMD5(targetFile);
            if (targetMD5.equals(apkMD5)){
                return false;
            }else {
                return true;
            }

        }else {
            return true;
        }
    }

    public static int  calcProgress(long complete,long total){
        if (total == 0){
            return 0;
        }
        BigDecimal completeBD = new BigDecimal(complete);
        BigDecimal totalBD = new BigDecimal(total);
        BigDecimal progress = completeBD.multiply(new BigDecimal(100)).divide(totalBD,4);

        return  progress.intValue();
    }
}
