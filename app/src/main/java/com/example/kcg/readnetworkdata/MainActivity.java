package com.example.kcg.readnetworkdata;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements OnNetworkDataListener{


    private Button connectionBtn;
    private TextView dataTextView,ipTextView;
    private EditText portEditText;
    private boolean connected = false;

    NetworkThread networkThread;

    private Intent dummyIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionBtn = findViewById(R.id.connectionBtn);
        dataTextView = findViewById(R.id.dataTextView);
        portEditText = findViewById(R.id.portEditText);
        ipTextView = findViewById(R.id.ipTextView);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());


        ipTextView.setText(ip+", Service running : "+isMyServiceRunning(NetworkClientService.class));

        dummyIntent = new Intent(this, NetworkClientService.class);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void listenToNetwork(int portNumber){
        if (networkThread == null) {
            networkThread = new NetworkThread(portNumber,getApplicationContext());
            networkThread.setListener(this);
        }
        if(!networkThread.openConnection(portNumber)) {
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
//            listenToNetwork(Integer.parseInt(portEditText.getText().toString()));

            dummyIntent.putExtra("PORT",Integer.parseInt(portEditText.getText().toString()));
            startService(dummyIntent);
        }else{
            connected=!connected;
            connectionBtn.setText("Open Connection");
//            networkThread.closeConnection();
            stopService(dummyIntent);
        }
    }


    @Override
    public void onNetworkData(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataTextView.setText(s);
            }
        });
//        dataTextView.post(new Runnable() {
//            @Override
//            public void run() {
//                dataTextView.setText(s);
//            }
//        });
    }
}



interface OnNetworkDataListener {

    public void onNetworkData(String s);

}
