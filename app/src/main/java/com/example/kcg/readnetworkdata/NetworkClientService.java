package com.example.kcg.readnetworkdata;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import it.cnr.isti.steplogger.IStepLoggerService;

import static java.nio.ByteOrder.LITTLE_ENDIAN;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class NetworkClientService extends IntentService{

    //**************SERVICE*********************
    // Intent Service fields
    private static int INTERVAL = 500;
    private static String LOG_TAG = "StepLoggerClient";
    private boolean mustRun = true;

    private Timer timer = new Timer();

    // Bound Service fields
    final String BOUNDSERVICE_PACKAGE = "it.cnr.isti.steplogger";
    final String BOUNDSERVICE_CLASS = ".StepLoggerService";
    IStepLoggerService mService;

    Boolean isBound = false;
    Intent intentService = new Intent();


    //**************NETWORK*********************

    private ServerSocket socket;
    private Socket clientSocket;

    private int portNumber;
    private boolean keepRunnig = true;

    //Bound Service Connection
    private ServiceConnection mConnection = new ServiceConnection(){
        public void onServiceConnected(ComponentName name, IBinder boundService) {
            mService = IStepLoggerService.Stub.asInterface((IBinder) boundService);
            Log.d(LOG_TAG, "onServiceConnected() : OK ");
            isBound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.d(LOG_TAG, "onServiceDisconnected() : OK ");
            isBound = false;
        }
    };

    public NetworkClientService() {
        super("stepLoggerClientService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        portNumber = intent.getExtras().getInt("PORT");

        if (intent != null) {
            intentService.setClassName(BOUNDSERVICE_PACKAGE,
                    BOUNDSERVICE_PACKAGE + BOUNDSERVICE_CLASS);

            bindService(intentService, mConnection , Context.BIND_AUTO_CREATE );

            while (mustRun) {
                if(!isBound){
                    // Try to rebind every 1000 msec
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            bindService(intentService, mConnection , Context.BIND_AUTO_CREATE );
                        }
                    }, 1000);
                } else {

                    try {
                        if (socket == null)
                            socket = new ServerSocket(portNumber);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }

                    InputStream is = null;
                    try {
                        clientSocket = socket.accept();
                        is = clientSocket.getInputStream();
                        final byte[] buffer = new byte[1024];
                        String msg = "";
                        int read;

                        while(keepRunnig) {
                            read = is.read(buffer);
                            byte[] x_bytes = new byte[]{buffer[0],buffer[1],buffer[2],buffer[3]};
                            float x = ByteBuffer.wrap(x_bytes).order(LITTLE_ENDIAN).getFloat();

                            byte[] y_bytes = new byte[]{buffer[4+0],buffer[4+1],buffer[4+2],buffer[4+3]};
                            float y = ByteBuffer.wrap(y_bytes).order(LITTLE_ENDIAN).getFloat();

                            byte[] z_bytes = new byte[]{buffer[8+0],buffer[8+1],buffer[8+2],buffer[8+3]};
                            float z = ByteBuffer.wrap(z_bytes).order(LITTLE_ENDIAN).getFloat();

                            try {
                                mService.logPosition(System.currentTimeMillis(),x,y,z);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        is.close();
                        clientSocket.close();
                        //socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

//                    try {
//                        // mService could still be null even if bound correctly
//                        if (mService != null) {
//                            // Send to the Bound Service method logPosition
//                            // current timestamp and random coords
//                            Log.d(LOG_TAG, "logPosition called");
//                            mService.logPosition(System.currentTimeMillis(),
//                                    Math.random() * 1000,
//                                    Math.random() * 1000,
//                                    Math.random() * 1000
//                            );
//                        }
//                        Thread.sleep(INTERVAL);
//                    } catch (Exception e) {
//                        Log.e(LOG_TAG, e.getMessage());
//                        e.printStackTrace();
//                    }
                }
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unbindService(mConnection);
        mService = null;
        isBound = false;
        mustRun = false;
        keepRunnig=false;
        Log.d(LOG_TAG, "Service stopped");
    }
}