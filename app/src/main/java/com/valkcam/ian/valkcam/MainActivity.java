package com.valkcam.ian.valkcam;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Todo:
 *
 *  Options: quality, restart serv, random moovements on/off, auto wb etc.
 *  Send button data
 *  Handle Host Unavailable
 */
public class MainActivity extends Activity  {

    public static final long SOCKET_SEND_DELTA = 100;//ms in between sending
    public static final int SERVERPORT = 5000;
    public static final String SERVER_IP = "192.168.1.36";
    public static final String piAddr = "http://192.168.4.1:8000/index.html";
    private CommHandler cThread;

    WebView mWebView;
    ImageButton btnTop;
    ImageButton btnBottom;
    ImageButton btnLeft;
    ImageButton btnRight;
    ImageButton btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cThread = new CommHandler();
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        mWebView = findViewById(R.id.webview);

        btnTop = findViewById(R.id.btnTop);
        btnBottom = findViewById(R.id.btnBottom);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnSettings = findViewById(R.id.btnSettings);

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, PrefActivity.class));

            }
        });

        btnTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cThread.setVar("x", 5);
            }
        });

        mWebView.loadUrl(piAddr);

    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePrefs();
    }

    @Override
    protected void onPause() {
        super.onPause();
        updatePrefs();
    }

    public static boolean updatePrefs = false;
    private void updatePrefs(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.getBoolean("pref_restart", false)){
            mWebView.reload();
            cThread = new CommHandler();
            prefs.edit().putBoolean("pref_restart", false).apply();
        }

        if(Integer.parseInt(prefs.getString("pref_mode", "0")) == 0)
            updateManual(true);
        else
            updateManual(false);

        if(updatePrefs){
            cThread.setVar("mode", Integer.parseInt(prefs.getString("pref_mode", "0")));
            cThread.setVar("quality", Integer.parseInt(prefs.getString("pref_quality", "1")));
            cThread.setVar("update", 1);//always do last in-case data is sent in two bursts
        }
    }

    private void updateManual(boolean manual)

    {
        if(manual) {
            btnBottom.setVisibility(View.VISIBLE);
            btnLeft.setVisibility(View.VISIBLE);
            btnRight.setVisibility(View.VISIBLE);
            btnTop.setVisibility(View.VISIBLE);
        }else{
            btnBottom.setVisibility(View.INVISIBLE);
            btnLeft.setVisibility(View.INVISIBLE);
            btnRight.setVisibility(View.INVISIBLE);
            btnTop.setVisibility(View.INVISIBLE);
        }
    }

    class CommHandler extends Thread{

        private HashMap<String, Integer> variables = new HashMap<>();
        private boolean updated = false;

        public CommHandler() {
            super();
            this.start();
        }

        @Override
        public void run() {
            super.run();

            Socket socket;
            OutputStream out;
            PrintWriter output;

            try {
                socket = new Socket(SERVER_IP, SERVERPORT);
                out = socket.getOutputStream();
                output = new PrintWriter(out);

                long sentmillis = System.currentTimeMillis();
                while(!interrupted()){
                    if(System.currentTimeMillis() - sentmillis > SOCKET_SEND_DELTA && updated && !variables.isEmpty()){
                        sentmillis = System.currentTimeMillis();
                        output.println(variables.toString());
                        output.flush();
                        variables.clear();
                        updated = false;
                    }
                }

                socket.close();
                output.close();
                out.close();
            } catch (Exception e1) {
                e1.printStackTrace();

            }
        }

        public void setVar(String s, int i){
            variables.put(s, i);
            updated = true;
        }

    }

}
