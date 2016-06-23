package org.makeathon.telepresenceslave;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

/**
 * Created by hui-joo.goh on 23/06/16.
 */
public class SlaveService extends Service {

    private static final String TAG = SlaveService.class.getName();
    private Pubnub mPubnub;

    private static final String CMD_F = "CMD_F";
    private static final String CMD_B = "CMD_B";
    private static final String CMD_L = "CMD_L";
    private static final String CMD_R = "CMD_R";
    private static final String CMD_P = "CMD_P";

    private Thread mThread;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mThread run");
            // do nothing, just await incoming pubnub messages
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        mPubnub = new Pubnub("pub-c-e0e0e558-9aa1-412e-a4ca-ce286e939e54", "sub-c-4b5e362c-27fd-11e6-84f2-02ee2ddab7fe");
        pubnubConnect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        startThread();
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        pubnubDisconnect();
    }

    private void startThread() {
        Log.d(TAG, "startThread");

        // Do not restart thread if already running
        if (mThread == null || !mThread.isAlive()) {

            // Start thread
            mThread = new Thread(mRunnable);
            mThread.start();
        }
    }

    private void interruptThread() {
        Log.d(TAG, "interruptThread");

        if (mThread != null) {
            mThread.interrupt();
        }
    }

    private void onForward() {
        Log.d(TAG, "onForward");

//        int loop = LOOP;
//        while (loop != 0) {
//            // move up
//            mRobotCommanderThread.robotMove(speed);
//            mRobotState =MOVING_FORWARD;
//            loop--;
//        }
//
//        // stop moving
//        mRobotCommanderThread.robotMove(0);
//        mRobotState =IDLE;
    }

    private void onBackward(){
        Log.d(TAG, "onBackward");

//        int loop = LOOP;
//        while (loop != 0) {
//            // move down
//            mRobotCommanderThread.robotMove(-speed);
//            mRobotState =MOVING_BACK;
//            loop--;
//        }
//
//        // stop moving
//        mRobotCommanderThread.robotMove(0);
//        mRobotState =IDLE;

    }

    private void onLeft(){
        Log.d(TAG, "onLeft");

//        int loop = LOOP;
//        while (loop != 0) {
//            // move right
//            mRobotCommanderThread.robotRotate(-speed);
//            mRobotState =MOVING_LEFT;
//            loop--;
//        }
//
//        // stop moving
//        mRobotCommanderThread.robotMove(0);
//        mRobotState =IDLE;
    }

    private void onPoke(){
        Log.d(TAG, "onPoke");
    }

    private void onRight(){
        Log.d(TAG, "onRight");

//        int loop = LOOP;
//        while (loop != 0) {
//            // move right
//            mRobotCommanderThread.robotRotate(speed);
//            mRobotState =MOVING_RIGHT;
//            loop--;
//        }
//
//        // stop moving
//        mRobotCommanderThread.robotMove(0);
//        mRobotState =IDLE;
    }

    public void pubnubConnect() {
        Log.d(TAG, "pubnubConnect");

        try {
            mPubnub.subscribe("my_channel", new Callback() {
                        @Override
                        public void connectCallback(String channel, Object message) {
                            mPubnub.publish("my_channel", "bot connected", new Callback() {});

                        }

                        @Override
                        public void disconnectCallback(String channel, Object message) {
                            Log.d(TAG, "SUBSCRIBE : DISCONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }

                        public void reconnectCallback(String channel, Object message) {
                            Log.d(TAG, "SUBSCRIBE : RECONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }

                        @Override
                        public void successCallback(String channel, Object message) {
                            String msg = message.toString();
                            Log.d(TAG, "SUBSCRIBE : " + channel + " : "
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
                            } else if (CMD_P.equals(msg)){
                                onPoke();
                            }
                        }

                        @Override
                        public void errorCallback(String channel, PubnubError error) {
                            Log.d(TAG, "SUBSCRIBE : ERROR on channel " + channel
                                    + " : " + error.toString());
                        }
                    }
            );
        } catch (PubnubException e) {
            Log.e(TAG, "Failed to unsubscribe pubnub", e);
        }

    }

    public void pubnubDisconnect(){
        Log.d(TAG, "pubnubDisconnect");

        mPubnub.unsubscribe("my_channel", new Callback() {
            @Override
            public void connectCallback(String channel, Object message) {
                Log.d(TAG, "UNSUBSCRIBE : DISCONNECT on channel:" + channel
                        + " : " + message.getClass() + " : "
                        + message.toString());
            }

            @Override
            public void disconnectCallback(String channel, Object message) {
                Log.d(TAG, "UNSUBSCRIBE : DISCONNECT on channel:" + channel
                        + " : " + message.getClass() + " : "
                        + message.toString());
            }

            public void reconnectCallback(String channel, Object message) {
                Log.d(TAG, "UNSUBSCRIBE : RECONNECT on channel:" + channel
                        + " : " + message.getClass() + " : "
                        + message.toString());
            }

            @Override
            public void successCallback(String channel, Object message) {
                Log.d(TAG, "UNSUBSCRIBE : " + channel + " : "
                        + message.getClass() + " : " + message.toString());
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                Log.d(TAG, "UNSUBSCRIBE : ERROR on channel " + channel
                        + " : " + error.toString());
            }
        });

        mPubnub.shutdown();
    }
}
