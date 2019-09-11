package com.example.kcg.readnetworkdata;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ThemedSpinnerAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnNetworkDataListener{


    private Button connectionBtn;
    private TextView dataTextView;
    private EditText portEditText;
    private boolean connected = false;

    NetworkThread networkThread;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionBtn = findViewById(R.id.connectionBtn);
        dataTextView = findViewById(R.id.dataTextView);
        portEditText = findViewById(R.id.portEditText);

    }


    private void listenToNetwork(int portNumber){
        if (networkThread == null) {
            networkThread = new NetworkThread(portNumber);
            networkThread.setListener(this);
        }
        if(!networkThread.openConnection()) {
            dataTextView.setText("ERROR : PORT "+portNumber+" IN USE!!!!!");
            connected = !connected;
            connectionBtn.setText("Open Connection");
        }
    }

    public void connectionBtnClicked(View v){
        dataTextView.setText("btn pressed");
        if(!connected){
            connected=!connected;
            connectionBtn.setText("Close Connection");

            listenToNetwork(Integer.parseInt(portEditText.getText().toString()));


        }else{
            connected=!connected;
            connectionBtn.setText("Open Connection");
        }
    }


    @Override
    public void onNetworkData(final String s) {
        dataTextView.post(new Runnable() {
            @Override
            public void run() {
                dataTextView.setText(s);
            }
        });
    }
}

class NetworkThread implements Runnable{

    private ArrayList<OnNetworkDataListener> networkDataListeners = new ArrayList<>();
    private boolean keepRunnig = true;

    private ServerSocket socket;
    private Socket clientSocket;

    private int portNumber;

    public NetworkThread(int portNumber){
        this.portNumber=portNumber;
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
            int read;

            while(keepRunnig) {
                read = is.read(buffer);
                for (OnNetworkDataListener listener : networkDataListeners) {
                    listener.onNetworkData(new String(buffer));
                }
            }

            is.close();
            clientSocket.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean openConnection(){
        try {
            if (socket == null)
                socket = new ServerSocket(portNumber);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
        return true;
    }

    public void closeConnection(){
        this.keepRunnig = false;
    }
}

interface OnNetworkDataListener {


    public void onNetworkData(String s);

}
