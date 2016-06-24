package org.makeathon.telepresencebot;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ControllerActivity extends AppCompatActivity {
    private Pubnub pubnub;
    private int counter = 0;
    private static final String CMD_F = "CMD_F";
    private static final String CMD_B = "CMD_B";
    private static final String CMD_L = "CMD_L";
    private static final String CMD_R = "CMD_R";
    private static final String CMD_P = "CMD_P";
    private final ExecutorService mThreadExecutor = Executors.newSingleThreadExecutor();
    private Button mForwardButton;
    private Button mBackwardButton;

    private Button mLeftButton;

    private Button mRightButton;


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_controller);
        mForwardButton = (Button) findViewById(R.id.btn_forward);
        mBackwardButton = (Button) findViewById(R.id.btn_backward);

        mLeftButton = (Button) findViewById(R.id.btn_left);

        mRightButton = (Button) findViewById(R.id.btn_right);

        pubnub = new Pubnub("pub-c-e0e0e558-9aa1-412e-a4ca-ce286e939e54", "sub-c-4b5e362c-27fd-11e6-84f2-02ee2ddab7fe");
        // prepare intent which is triggered if the
// notification is selected



    }
    @Override
    public void onPause(){
        if (mThreadExecutor!= null){
            mThreadExecutor.shutdownNow();
        }
        super.onPause();

    }
    public void onResume(){
        super.onResume();
        Intent intent = new Intent(this, ControllerActivity.class);
// use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) 10002, intent, 0);

// build notification
// the addAction re-use the same intent to keep the example short
        Notification n  = new Notification.Builder(this)
                .setContentTitle("TelepresenceBot")
                .setContentText("launch controller")
                .setSmallIcon(R.drawable.common_full_open_on_phone)
                .setContentIntent(pIntent)
                .setAutoCancel(true).build();


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, n);
        mThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                while(true&& !Thread.interrupted()){
                    if (mForwardButton.isPressed()){
                        System.out.println("forward");
                        forward(null);
                    }
                    if (mBackwardButton.isPressed()){
                        System.out.println("backward");
                        backward(null);
                    }
                    if (mLeftButton.isPressed()){
                        System.out.println("left");
                        left(null);
                    }
                    if (mRightButton.isPressed()){
                        System.out.println("right");
                        right(null);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }

                }
            }
        });
    }



    public void forward(View view) {
        pubnub.publish("my_channel", CMD_F, new Callback() {});
    }

    public void backward(View view) {
        pubnub.publish("my_channel", CMD_B, new Callback() {
        });
    }

    public void left(View view) {
        pubnub.publish("my_channel", CMD_L, new Callback() {
        });

    }

    public void right(View view) {
        pubnub.publish("my_channel", CMD_R, new Callback() {
        });

    }


    public void poke(View view) {
        pubnub.publish("my_channel", CMD_P, new Callback() {
        });

    }


    public void connect(View view) {

        try {
            pubnub.subscribe("my_channel", new Callback() {
                        @Override
                        public void connectCallback(String channel, Object message) {
                            pubnub.publish("my_channel", "Hello from the PubNub Java SDK", new Callback() {
                            });
                            pubnub.publish("my_channel", "Hello from the PubNub Java SDK2", new Callback() {
                            });

                        }

                        @Override
                        public void disconnectCallback(String channel, Object message) {
                            System.out.println("SUBSCRIBE : DISCONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }

                        public void reconnectCallback(String channel, Object message) {
                            System.out.println("SUBSCRIBE : RECONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }

                        @Override
                        public void successCallback(String channel, Object message) {
                            System.out.println("SUBSCRIBE : " + channel + " : "
                                    + message.getClass() + " : " + message.toString());
                        }

                        @Override
                        public void errorCallback(String channel, PubnubError error) {
                            System.out.println("SUBSCRIBE : ERROR on channel " + channel
                                    + " : " + error.toString());
                        }
                    }
            );
        } catch (PubnubException e) {
            System.out.println(e.toString());
        }

    }

    public void disconnect(View view) {
        pubnub.unsubscribe("my_channel", new Callback() {
            @Override
            public void connectCallback(String channel, Object message) {
                System.out.println("UNSUBSCRIBE : DISCONNECT on channel:" + channel
                        + " : " + message.getClass() + " : "
                        + message.toString());
            }

            @Override
            public void disconnectCallback(String channel, Object message) {
                System.out.println("UNSUBSCRIBE : DISCONNECT on channel:" + channel
                        + " : " + message.getClass() + " : "
                        + message.toString());
            }

            public void reconnectCallback(String channel, Object message) {
                System.out.println("UNSUBSCRIBE : RECONNECT on channel:" + channel
                        + " : " + message.getClass() + " : "
                        + message.toString());
            }

            @Override
            public void successCallback(String channel, Object message) {
                System.out.println("UNSUBSCRIBE : " + channel + " : "
                        + message.getClass() + " : " + message.toString());
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                System.out.println("UNSUBSCRIBE : ERROR on channel " + channel
                        + " : " + error.toString());
            }
        });

    }

    @Override
    public void onDestroy() {
        pubnub.shutdown();
        super.onDestroy();

    }

    public void publish(View view) {


        pubnub.publish("my_channel", "message " + Integer.toString(counter), new Callback() {
        });
        counter++;

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Controller Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://org.makeathon.telepresencebot/http/host/path")
        );
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Controller Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://org.makeathon.telepresencebot/http/host/path")
        );
    }
}
