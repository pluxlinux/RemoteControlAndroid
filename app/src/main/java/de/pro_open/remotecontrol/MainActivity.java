package de.pro_open.remotecontrol;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;

import JavaUtils.TCPManager.TCPManager;
import JavaUtils.TCPManager.TcpConnection;
import JavaUtils.UDPUtils.UDPBroadcast;

public class MainActivity extends AppCompatActivity {
    TcpConnection conToServer;
    long ts = 0;
    long te = 0;
    View.OnClickListener oc = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        conToServer = TCPManager.connect(((TextView) view).getText().toString().split("\n")[1], 45340, false, null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            clientConnection();

        }

    };
    Boolean server_on = false;

    private void clientConnection() {
        final RelativeLayout rl = (RelativeLayout) findViewById(R.id.activity_main);
        rl.removeAllViews();
        rl.setOnTouchListener(new View.OnTouchListener() {
            float prevX = 0;
            float prevY = 0;
            float preX = 0;
            float preY = 0;
            int c = 0;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    prevX = motionEvent.getX();
                    prevY = motionEvent.getY();
                    preX = motionEvent.getX();
                    preY = motionEvent.getY();
                    ts = System.currentTimeMillis();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    c++;
                        conToServer.writeLine("mouse " + (int) (motionEvent.getX() - prevX) + " " + (int) (motionEvent.getY() - prevY));

                        prevX = motionEvent.getX();
                        prevY = motionEvent.getY();
                        c = 0;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    te = System.currentTimeMillis();
                    if ((te - ts) < 150 && (int) (motionEvent.getX() - preX) < 20 && (int) (motionEvent.getY() - preY) < 20) {
                        conToServer.writeLine("leftClick");
                    } else if ((te - ts) > 150 && (int) (motionEvent.getX() - preX) < 20 && (int) (motionEvent.getY() - preY) < 20 && (te - ts) < 1500) {
                        conToServer.writeLine("rightClick");
                    }
                }
                return true;
            }
        });
    }

    private void sendMessageToServer(final String message){
        new Thread(new Runnable() {
            @Override
            public void run() {
                conToServer.writeLine(message);
                conToServer.flush();
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UDPBroadcast.startNewBroadcastRequest(4960, "", true, 20000, new UDPBroadcast.UDPBroadcastResponseListener() {
            @Override
            public void process(String response, final InetAddress address) {
                if (response != null && response.equalsIgnoreCase("server_online")) {
                    server_on = true;
                    final String hostname = address.getHostName();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addServerAsOnline(address.getHostAddress(), hostname);

                        }
                    });
                }
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finishedSearch();
                    }
                });
            }
        }).start();
    }

    public void addServerAsOnline(String ip, String hostname) {
        System.out.println(ip + ":" + hostname);
        Button server = new Button(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            server.setBackground(getResources().getDrawable(R.drawable.button_border));
        }
        server.setText(hostname + "\n" + ip);
        server.setOnClickListener(oc);
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll);

        ll.addView(server);
    }

    public void finishedSearch() {
        ProgressBar pb = (ProgressBar) findViewById(R.id.pb);
        if (pb != null) {
            pb.setVisibility(View.INVISIBLE);
        }
        if (server_on == false) {
            TextView tv = (TextView) findViewById(R.id.noserveronlinetv);
            tv.setText("No Server found!\nDownload the server software for Linux, Windows or Mac on www.test.de");
            tv.setVisibility(View.VISIBLE);
        } else {

        }

    }

}