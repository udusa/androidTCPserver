package com.example.kcg.readnetworkdata;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;

import it.cnr.isti.steplogger.IStepLoggerService;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class NetworkThread implements Runnable{



    private Context mContext;
    private ArrayList<OnNetworkDataListener> networkDataListeners = new ArrayList<>();
    private boolean keepRunnig = true;

    private ServerSocket socket;
    private Socket clientSocket;

    private int portNumber;


    public NetworkThread(int portNumber,Context mContext){

        this.portNumber = portNumber;
        this.mContext = mContext;
    }

    public void setListener(OnNetworkDataListener listener){
        networkDataListeners.add(listener);
    }

    @Override
    public void run() {
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


                for (OnNetworkDataListener listener : networkDataListeners) {
//                    Log.d("ReAd",""+read);
                    listener.onNetworkData(""+x+","+y+","+z);
                }
            }

            is.close();
            clientSocket.close();
            //socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean openConnection(int portNumber){
        try {
            if (socket == null)
                socket = new ServerSocket(portNumber);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        keepRunnig = true;
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
        return true;
    }

    public void closeConnection(){
        this.keepRunnig = false;
    }

//    public void setContext(Context applicationContext) {
//        this.mContext = applicationContext;
//    }
}