package de.pro_open.remotecontrol;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.net.InetAddress;

import JavaUtils.TCPManager.TCPManager;
import JavaUtils.TCPManager.TcpConnection;
import JavaUtils.UDPUtils.UDPBroadcast;

public class MainActivity extends AppCompatActivity {
    Button trackiv;
    Button left;
    Button right;
    EditText input;
    TcpConnection conToServer;
    ScrollView sv;
    long ts = 0;
    long te = 0;
    //OnclickListener for server buttons
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

    //OnClickListener for left/right click buttons
    View.OnClickListener ocr = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            if (view.getId() == R.id.leftiv) {
                conToServer.writeLine("leftClick");
            } else {
                conToServer.writeLine("rightClick");
            }
        }

    };
    Boolean server_on = false;

    private void clientConnection() {
        final RelativeLayout rl = (RelativeLayout) findViewById(R.id.activity_main);
        rl.removeView(sv);
        input.setVisibility(View.VISIBLE);
        trackiv.setVisibility(View.VISIBLE);
        left.setVisibility(View.VISIBLE);
        right.setVisibility(View.VISIBLE);
        left.setOnClickListener(ocr);
        right.setOnClickListener(ocr);

        input.requestFocus();

        //OntouchListener for Trackpad
        trackiv.setOnTouchListener(new View.OnTouchListener() {

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
                    } else if ((te - ts) > 150 && (int) (motionEvent.getX() - preX) < 5 && (int) (motionEvent.getY() - preY) < 5 && (te - ts) < 1500) {
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
        trackiv = (Button) findViewById(R.id.trackiv);
        left = (Button) findViewById(R.id.leftiv);
        right = (Button) findViewById(R.id.rightiv);
        input = (EditText) findViewById(R.id.inputtv);
        sv = (ScrollView) findViewById(R.id.sv);

        input.setText("p");

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(!(editable + "").equals("p")&&!(editable+"").isEmpty()) {
                    if(editable.length()==2&&(editable+"").substring(1).equals(" ")){
                        sendMessageToServer("keyboard_space");
                    }else {
                        sendMessageToServer("keyboard " + (editable + "").substring(1));
                    }
                    input.setText("p");
                    input.setSelection(1);
                }else if((editable + "").isEmpty()){
                    sendMessageToServer("keyboard_backspace");
                    input.setText("p");
                    input.setSelection(1);
                }
            }
        });
        UDPBroadcast.startNewBroadcastRequest(4960, "", true, 10000, new UDPBroadcast.UDPBroadcastResponseListener() {
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
                    Thread.sleep(10000L);
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