package com.mart.listlock.request;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.mart.listlock.common.LogW;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class DownloadService extends IntentService {

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_FINISHED = 1;
    public static final int STATUS_ERROR = 2;
    public static final String URL_TAG = "url";
    public static final String BYTE_ARRAY_TAG = "data";
    public static final String REQUEST_ID_TAG = "request_id";
    public static final String RECEIVER_TAG = "receiver";

    private static final int BUFFER_SIZE = 1024;
    private static final int MAX_FILE_SIZE = 1048576;
    private static final int TIME_OUT = 3000;
    private static final String LOG_TAG = DownloadService.class.getName();

    public DownloadService() {
        super(LOG_TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        LogW.d(LOG_TAG, "service started");

        final ResultReceiver receiver = intent.getParcelableExtra("receiver");
        String url = intent.getStringExtra(URL_TAG);

        Bundle bundle = new Bundle();
        bundle.putInt(REQUEST_ID_TAG, intent.getIntExtra(REQUEST_ID_TAG, -1));

        if (url == null || !url.isEmpty()) {
            receiver.send(STATUS_RUNNING, Bundle.EMPTY);

            try {
                byte[] data = downloadData(new URL(url));

                if (data == null)
                    throw new DownloadException("received byte array is null");

                bundle.putByteArray(BYTE_ARRAY_TAG, data);
            } catch (Exception e) {
                bundle.putString(Intent.EXTRA_TEXT, Log.getStackTraceString(e));
                receiver.send(STATUS_ERROR, bundle);
            }
        }

        receiver.send(STATUS_FINISHED, bundle);
        LogW.d(LOG_TAG, "service stopped");
        this.stopSelf();
    }

    private byte[] downloadData(URL url) throws IOException, DownloadException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        URLConnection connection = url.openConnection();

        connection.setConnectTimeout(TIME_OUT);
        connection.setReadTimeout(TIME_OUT);

        InputStream inputStream = connection.getInputStream();

        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

        final byte[] dataBuffer = new byte[BUFFER_SIZE];
        int count;
        int totalCount = 0;
        while ((count = bufferedInputStream.read(dataBuffer, 0, BUFFER_SIZE)) != -1) {
            byteArrayOutputStream.write(dataBuffer, 0, count);
            totalCount += count;

            if (totalCount > MAX_FILE_SIZE - BUFFER_SIZE) {
                // next total count will be larger than maximum file size
                throw new DownloadException("file larger than maximum size " + MAX_FILE_SIZE);
            }
        }

        byte[] data = byteArrayOutputStream.toByteArray();

        inputStream.close();
        byteArrayOutputStream.close();

        return data;
    }
}
