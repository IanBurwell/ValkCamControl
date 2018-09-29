package com.valkcam.ian.valkcam;

import android.app.Activity;
import android.os.Bundle;
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
 *  Make on resume refresh webpage
 *  Options: quality, restart serv, random moovements on/off, auto wb etc.
 *  Send button data
 */
public class MainActivity extends Activity {

    private static final long SOCKET_SEND_DELTA = 100;//ms in between sending
    private static final int SERVERPORT = 5000;
    private static final String SERVER_IP = "192.168.1.36";
    private static final String piAddr = "http://192.168.4.1:8000/index.html";
    private CommHandler cThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cThread = new CommHandler();
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        WebView mWebView = findViewById(R.id.webview);
        final ImageButton btnTop = findViewById(R.id.btnTop);
        ImageButton btnBottom = findViewById(R.id.btnBottom);
        ImageButton btnLeft = findViewById(R.id.btnLeft);
        ImageButton btnRight = findViewById(R.id.btnRight);
        final Activity This = this;

        btnTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cThread.setVar("x", (int)(Math.random()*100));

                Toast.makeText(This, "test", Toast.LENGTH_SHORT).show();

            }
        });


        // disable scroll on touch
        /*mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });*/

        mWebView.loadUrl(piAddr);
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
