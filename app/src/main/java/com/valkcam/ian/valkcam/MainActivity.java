package com.valkcam.ian.valkcam;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Todo:
 *  Make on resume refresh webpage
 *  Options: quality, restart serv, random moovements on/off
 *  Send button data
 */
public class MainActivity extends Activity {

    private Socket socket;

    private static final int SERVERPORT = 5000;
    private static final String SERVER_IP = "192.168.4.1";
    private static final String piAddr = "http://192.168.4.1:8000/index.html";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        WebView mWebView = findViewById(R.id.webview);
        ImageButton btnTop = findViewById(R.id.btnTop);
        ImageButton btnBottom = findViewById(R.id.btnBottom);
        ImageButton btnLeft = findViewById(R.id.btnLeft);
        ImageButton btnRight = findViewById(R.id.btnRight);

        btnTop.setVisibility(View.INVISIBLE);
        btnBottom.setVisibility(View.INVISIBLE);
        btnLeft.setVisibility(View.INVISIBLE);
        btnRight.setVisibility(View.INVISIBLE);

        btnTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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
        new Thread(new ClientThread()).start();

    }

    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {

                socket = new Socket(SERVER_IP, SERVERPORT);

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

    }

}
