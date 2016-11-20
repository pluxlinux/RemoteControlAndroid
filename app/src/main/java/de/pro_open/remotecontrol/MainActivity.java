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

import java.io.IOException;
import java.net.InetAddress;

import JavaUtils.TCPManager.TCPManager;
import JavaUtils.TCPManager.TcpConnection;
import JavaUtils.UDPUtils.UDPBroadcast;

public class MainActivity extends AppCompatActivity {
    boolean inarea = true;
    Button trackiv;
    Button left;
    Button right;
    Button dpi;
    Button timeUntilMousePressed;
    EditText input;
    TcpConnection conToServer;
    ScrollView sv;
    int multiplier = 1;
    int ms = 500;
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
            switch (view.getId()) {
                case R.id.leftiv:
                    conToServer.writeLine("leftClick");
                    break;
                case R.id.rightiv:
                    conToServer.writeLine("rightClick");
                    break;
                case R.id.timeuntilmousepressediv:
                    if (ms < 1000) {
                        ms += 100;
                    } else {
                        ms = 200;
                    }
                    if (ms == 200) {
                        timeUntilMousePressed.setText("OFF");
                    } else {
                        timeUntilMousePressed.setText(ms + "ms");
                    }

                    break;
                case R.id.dpiv:
                    if (multiplier < 4) {
                        multiplier++;
                    } else {
                        multiplier = 1;
                    }
                    dpi.setText("dpi lv " + multiplier);
                    break;


            }
            if (view.getId() == R.id.leftiv) {
                conToServer.writeLine("leftClick");
            } else if (view.getId() == R.id.rightiv) {
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
        timeUntilMousePressed.setVisibility(View.VISIBLE);
        dpi.setVisibility(View.VISIBLE);
        timeUntilMousePressed.setOnClickListener(ocr);
        dpi.setOnClickListener(ocr);
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
            boolean mouseIsPressed = false;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    prevX = motionEvent.getX();
                    prevY = motionEvent.getY();
                    preX = motionEvent.getX();
                    preY = motionEvent.getY();
                    ts = System.currentTimeMillis();
                    c = 0;
                    inarea = true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    te = System.currentTimeMillis();
                    if ((te - ts) > ms && (motionEvent.getX() - preX) < 2 && (motionEvent.getY() - preY) < 2 && mouseIsPressed == false && ms != 200 && inarea == true) {
                        /*System.out.println("te: " + te);
                        System.out.println("ts: " + ts);
                        System.out.println(motionEvent.getX());
                        System.out.println(preX);*/
                        mouseIsPressed = true;
                        conToServer.writeLine("mousePressed");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            trackiv.setBackground(getResources().getDrawable(R.drawable.backgroundtrackpadmousepressed));
                        }
                    }
                    if (mouseIsPressed == false && ((motionEvent.getX() - preX) > 2 || (motionEvent.getY() - preY) > 2)) {
                        inarea = false;
                    }
                    conToServer.writeLine("mouse " + ((int) (motionEvent.getX() - prevX)) * multiplier + " " + ((int) (motionEvent.getY() - prevY)) * multiplier);

                        prevX = motionEvent.getX();
                        prevY = motionEvent.getY();
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    te = System.currentTimeMillis();
                    if ((te - ts) < 150 && (int) (motionEvent.getX() - preX) < 4 && (int) (motionEvent.getY() - preY) < 4 && mouseIsPressed == false) {
                        conToServer.writeLine("leftClick");
                    } else if (mouseIsPressed == true) {
                        mouseIsPressed = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            trackiv.setBackground(getResources().getDrawable(R.drawable.backgroundtrackpad));
                        }
                        conToServer.writeLine("mouseReleased");
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
        dpi = (Button) findViewById(R.id.dpiv);
        timeUntilMousePressed = (Button) findViewById(R.id.timeuntilmousepressediv);

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

                    if (editable.length() > 1) {
                        switch ((editable + "").substring(1)) {
                            case " ":
                                sendMessageToServer("keyboard_space");
                                break;
                            case "?":
                                sendMessageToServer("keyboard_custom QUESTION");
                                break;
                            case "!":
                                sendMessageToServer("keyboard_custom EXCLAMATION");
                                break;
                            case "\"":
                                sendMessageToServer("keyboard_custom QUOTATION");
                                break;
                            case "§":
                                sendMessageToServer("keyboard_custom PARAGRAPH");
                                break;
                            case "$":
                                sendMessageToServer("keyboard_custom DOLLAR");
                                break;
                            case "/":
                                sendMessageToServer("keyboard_custom SLASH");
                                break;
                            case "(":
                                sendMessageToServer("keyboard_custom BRACKET-OPEN");
                                break;
                            case ")":
                                sendMessageToServer("keyboard_custom BRACKET-CLOSE");
                                break;
                            case "=":
                                sendMessageToServer("keyboard_custom EQUAL");
                                break;
                            case "*":
                                sendMessageToServer("keyboard_custom STAR");
                                break;
                            case "'":
                                sendMessageToServer("keyboard_custom SINGLEQUOTATION");
                                break;
                            case ":":
                                sendMessageToServer("keyboard_custom COLON");
                                break;
                            case ";":
                                sendMessageToServer("keyboard_custom SEMICOLON");
                                break;
                            case "_":
                                sendMessageToServer("keyboard_custom COATING");
                                break;
                            case "@":
                                sendMessageToServer("keyboard_custom AT");
                                break;
                            case "€":
                                sendMessageToServer("keyboard_custom EURO");
                                break;
                            case "\\":
                                sendMessageToServer("keyboard_custom BACKSLASH");
                                break;
                            case "}":
                                sendMessageToServer("keyboard_custom SWIFT-BRACKET-CLOSE");
                                break;
                            case "{":
                                sendMessageToServer("keyboard_custom SWIFT-BRACKET-OPEN");
                                break;
                            case "[":
                                sendMessageToServer("keyboard_custom ECKE-BRACKET-OPEN");
                                break;
                            case "]":
                                sendMessageToServer("keyboard_custom ECKE-BRACKET-CLOSE");
                                break;
                            case "~":
                                sendMessageToServer("keyboard_custom APRX");
                                break;
                            case "%":
                                sendMessageToServer("keyboard_custom PERCENT");
                                break;
                            case "&":
                                sendMessageToServer("keyboard_custom AND");
                                break;
                            case "+":
                                sendMessageToServer("keyboard_custom PLUS");
                                break;
                            case "\n":
                                sendMessageToServer("keyboard_custom ENTER");
                                break;
                            case ",":
                                sendMessageToServer("keyboard_custom COMMA");
                                break;
                            case ".":
                                sendMessageToServer("keyboard_custom DOT");
                                break;
                            case "-":
                                sendMessageToServer("keyboard_custom MINUS");
                                break;
                            default:
                                sendMessageToServer("keyboard " + (editable + "").substring(1));
                                break;
                            //#?"-' dont work


                        }
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