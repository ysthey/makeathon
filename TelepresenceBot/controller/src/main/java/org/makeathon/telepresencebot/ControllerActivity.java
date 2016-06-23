package org.makeathon.telepresencebot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

public class ControllerActivity extends AppCompatActivity {
    private Pubnub pubnub;
    private int counter = 0;
    private static final String CMD_F = "CMD_F";
    private static final String CMD_B = "CMD_B";
    private static final String CMD_L = "CMD_L";
    private static final String CMD_R = "CMD_R";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        pubnub = new Pubnub("pub-c-e0e0e558-9aa1-412e-a4ca-ce286e939e54", "sub-c-4b5e362c-27fd-11e6-84f2-02ee2ddab7fe");

    }

    public void forward(View view){
        pubnub.publish("my_channel", CMD_F, new Callback() {});
    }

    public void backward(View view){
        pubnub.publish("my_channel", CMD_B, new Callback() {});
    }
    public void left(View view){
        pubnub.publish("my_channel", CMD_L, new Callback() {});

    }
    public void right(View view){
        pubnub.publish("my_channel", CMD_R, new Callback() {});

    }

    public void connect(View view){

        try {
            pubnub.subscribe("my_channel", new Callback() {
                        @Override
                        public void connectCallback(String channel, Object message) {
                            pubnub.publish("my_channel", "Hello from the PubNub Java SDK", new Callback() {});
                            pubnub.publish("my_channel", "Hello from the PubNub Java SDK2", new Callback() {});

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

    public void disconnect(View view){
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
    public void onDestroy(){
        pubnub.shutdown();
        super.onDestroy();

    }

    public void publish(View view){


        pubnub.publish("my_channel", "message " + Integer.toString(counter), new Callback() {});
        counter ++;

    }
}
