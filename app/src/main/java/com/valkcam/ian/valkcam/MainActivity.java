package com.valkcam.ian.valkcam;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageButton;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Todo:
 *
 *  Options: auto wb?
 *  Handle Host Unavailable
 *  Take Picture?
 */
public class MainActivity extends Activity  {

    public static final long SOCKET_CHECK_SEND_DELTA = 10;//ms in between checking to send
    public static final long BUTTON_SEND_DELTA = 50;//ms in between sending
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cThread = new CommHandler();
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        mWebView = findViewById(R.id.webview);

        btnTop = findViewById(R.id.btnUp);
        btnBottom = findViewById(R.id.btnDown);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnSettings = findViewById(R.id.btnSettings);

        //SETTINGS
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, PrefActivity.class));
            }
        });

        //ARROW KEYS
        btnRight.setOnTouchListener(new View.OnTouchListener() {
            long sentMillis = System.currentTimeMillis();
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if(System.currentTimeMillis() - sentMillis > BUTTON_SEND_DELTA){
                            cThread.setVar("x", 1);
                            sentMillis = System.currentTimeMillis();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        // No longer down
                        return true;
                }
                return false;
            }
        });
        btnLeft.setOnTouchListener(new View.OnTouchListener() {
            long sentMillis = System.currentTimeMillis();
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if(System.currentTimeMillis() - sentMillis > BUTTON_SEND_DELTA){
                            cThread.setVar("x", -1);
                            sentMillis = System.currentTimeMillis();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        // No longer down
                        return true;
                }
                return false;
            }
        });
        btnTop.setOnTouchListener(new View.OnTouchListener() {
            long sentMillis = System.currentTimeMillis();
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if(System.currentTimeMillis() - sentMillis > BUTTON_SEND_DELTA){
                            cThread.setVar("y", 1);
                            sentMillis = System.currentTimeMillis();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        // No longer down
                        return true;
                }
                return false;
            }
        });
        btnBottom.setOnTouchListener(new View.OnTouchListener() {
            long sentMillis = System.currentTimeMillis();
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if(System.currentTimeMillis() - sentMillis > BUTTON_SEND_DELTA){
                            cThread.setVar("y", -1);
                            sentMillis = System.currentTimeMillis();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        // No longer down
                        return true;
                }
                return false;
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

    private void updateManual(boolean manual) {
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
        private boolean restart = false;
        public boolean connected = false;

        CommHandler() {
            super();
            this.start();
        }

        @Override
        public void run() {
            super.run();

            Socket socket = new Socket();
            OutputStream out;
            PrintWriter output;

            while(!interrupted()) {
                try {
                    while (!interrupted() && !connected) {
                        try {
                            socket = new Socket(SERVER_IP, SERVERPORT);
                            connected = true;
                        } catch (UnknownHostException e) {
                            connected = false;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }

                    out = socket.getOutputStream();
                    output = new PrintWriter(out);

                    long sentmillis = System.currentTimeMillis();
                    while (!interrupted() && connected) {
                        if (socket.getInputStream().read() == -1) {
                            connected = false;
                            break;
                        }
                        if (System.currentTimeMillis() - sentmillis > SOCKET_CHECK_SEND_DELTA && updated && !variables.isEmpty()) {
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
                } catch (IOException e) {
                    e.printStackTrace();
                    connected = false;
                    socket = null;
                }
            }
        }

        public void setVar(String s, int i){
            variables.put(s, i);
            updated = true;
        }

        public void restart(){
            this.interrupt();
            restart = true;
        }

    }

}
