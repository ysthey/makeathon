package org.makeathon.telepresenceslave;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.makeathon.telepresenceslave.roboliterate.activities.RemoteControlActivity;

public class MainActivity extends AppCompatActivity {
    private Pubnub pubnub;
    private int counter = 0;

    private static final String CMD_F = "CMD_F";
    private static final String CMD_B = "CMD_B";
    private static final String CMD_L = "CMD_L";
    private static final String CMD_R = "CMD_R";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startActivity(new Intent(this, RemoteControlActivity.class)); // vivi
        pubnub = new Pubnub("pub-c-e0e0e558-9aa1-412e-a4ca-ce286e939e54", "sub-c-4b5e362c-27fd-11e6-84f2-02ee2ddab7fe");

        connect(null);
    }

    public void connect(View view){

        try {
            pubnub.subscribe("my_channel", new Callback() {
                        @Override
                        public void connectCallback(String channel, Object message) {
                            pubnub.publish("my_channel", "bot connected", new Callback() {});

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
                            String msg = message.toString();
                            System.out.println("SUBSCRIBE : " + channel + " : "
                                    + message.getClass() + " : " + msg);

                            if(CMD_F.equals(msg)){
                                onForward();
                            } else if (CMD_B.equals(msg)){
                                onBackward();
                            }
                            else if (CMD_L.equals(msg)){
                                onLeft();
                            }
                            else if (CMD_R.equals(msg)){
                                onRight();
                            }
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


    private void onForward()
    {
        System.out.println("forward command received");
    }
    private void onBackward(){
        System.out.println("backward command received");

    }
    private void onLeft(){
        System.out.println("left command received");

    }
    private void onRight(){
        System.out.println("right command received");

    }
}
