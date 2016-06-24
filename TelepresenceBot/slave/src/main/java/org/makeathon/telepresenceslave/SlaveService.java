package org.makeathon.telepresenceslave;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.greenrobot.eventbus.EventBus;
import org.makeathon.telepresenceslave.roboliterate.activities.ChooseDeviceActivity;
import org.makeathon.telepresenceslave.roboliterate.robotcomms.BluetoothCommunicator;
import org.makeathon.telepresenceslave.roboliterate.robotcomms.Commander;
import org.makeathon.telepresenceslave.roboliterate.robotcomms.CommanderImpl;
import org.makeathon.telepresenceslave.roboliterate.robotcomms.Robot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import events.CancelBluetoothDiscovery;
import events.ConnectionFailed;
import events.ConnectionSucceeded;
import events.DismissConnectionDialog;
import events.RobotTypeDetected;

/**
 * Created by hui-joo.goh on 23/06/16.
 */
public class SlaveService extends Service {

    private static final String TAG = SlaveService.class.getName();

    private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String CMD_F = "CMD_F";
    private static final String CMD_B = "CMD_B";
    private static final String CMD_L = "CMD_L";
    private static final String CMD_R = "CMD_R";
    private static final String CMD_P = "CMD_P";
    private static final String CMD_S = "CMD_S";

    private static final int ONE_MOVE_PERIOD = 1000;
    private static final int POKE_ONE_WAY_PERIOD = 500;

    private static final int MSG_ROBOT_TYPE_DETECTED = 3;
    private static final int MSG_ROBOT_CONNECTION = 6;
    private static final int STATUS_OK = 0;
    private static final int STATUS_ERROR = 16;

    private static final int IDLE = 9;
    private static final int MOVING_FORWARD =10;
    private static final int MOVING_BACK =11;
    private static final int MOVING_LEFT =12;
    private static final int MOVING_RIGHT =13;

    private static String sRobotAddress;
    private final int speed = 50;
    private final int POKE_SPEED = 5;
    private final int TURNING_SPEED = 100;



    private BluetoothSocket RobotSocket;

    private Pubnub mPubnub;

    private RobotConnectorThread mRobotConnectorThread;
    private RobotCommanderThread mRobotCommanderThread;

    private Thread mThread;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mThread run");
            connectToRobot(sRobotAddress);
        }
    };

    private static SlaveService sInstance;


    public static synchronized  SlaveService getInstance(){
        return sInstance;
    }


    @Override
    public synchronized void onCreate() {
        Log.d(TAG, "onCreate");
        sInstance = this;
        mPubnub = new Pubnub("pub-c-e0e0e558-9aa1-412e-a4ca-ce286e939e54", "sub-c-4b5e362c-27fd-11e6-84f2-02ee2ddab7fe");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        if (intent != null) {
            Log.d(TAG, "onStartCommand sRobotAddress " + sRobotAddress);
            sRobotAddress = intent.getStringExtra(ChooseDeviceActivity.EXTRA_DEVICE_ADDRESS);

            startRobotCommanderThread();
        } else {
            stopSelf();
        }

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
        disconnect();

    }

    public void disconnect(){
        //pubnubDisconnect();
        closeConnections();
        sInstance = null;

    }


    private void startRobotCommanderThread() {
        Log.d(TAG, "startRobotCommanderThread");

        // Do not restart thread if already running
        if (mThread == null || !mThread.isAlive()) {

            // Start thread
            mThread = new Thread(mRunnable);
            mThread.start();
        }
    }

    private void interruptRobotCommanderThread() {
        Log.d(TAG, "interruptRobotCommanderThread");

        if (mThread != null) {
            mThread.interrupt();
        }
    }

    public void onForward() {
        Log.d(TAG, "onForward");

        // move forward
        mRobotCommanderThread.robotMove(speed);
        threadWait(ONE_MOVE_PERIOD);

        // stop moving
        mRobotCommanderThread.robotMove(0);
    }

    public void onBackward(){
        Log.d(TAG, "onBackward");

        // move backward
        mRobotCommanderThread.robotMove(-speed);
        threadWait(ONE_MOVE_PERIOD);

        // stop moving
        mRobotCommanderThread.robotMove(0);
    }

    public void onLeft(){
        Log.d(TAG, "onLeft");
/*
        // move left
        mRobotCommanderThread.robotRotate(-TURNING_SPEED);
        threadWait(ONE_MOVE_PERIOD);

        // stop moving
        mRobotCommanderThread.robotMove(0);*/
        int motorAPortIndex = 0; // port num obtained from #activity_remotecontroller.xml view tag
        int motorDPortIndex = 3;
        mRobotCommanderThread.robotMove(motorAPortIndex, -TURNING_SPEED);
        mRobotCommanderThread.robotMove(motorDPortIndex, TURNING_SPEED);
        threadWait(ONE_MOVE_PERIOD);
        mRobotCommanderThread.robotMove(0);



    }

    public void onPoke(){
        Log.d(TAG, "onPoke");

        // move motor B & C backwards
        int motorBPortIndex = 1; // port num obtained from #activity_remotecontroller.xml view tag
        int motorCPortIndex = 2;
        mRobotCommanderThread.robotMove(motorBPortIndex, -POKE_SPEED-5, 90,false);
        mRobotCommanderThread.robotMove(motorCPortIndex, -POKE_SPEED-50, 300, true);

        //mRobotCommanderThread.robotMove(motorCPortIndex, -speed);

        //threadWait(POKE_ONE_WAY_PERIOD);

        // stop motor B & C
        //mRobotCommanderThread.robotMove(motorBPortIndex,0);
        //mRobotCommanderThread.robotMove(motorCPortIndex,0);

        // move motor B & C forward
        mRobotCommanderThread.robotMove(motorBPortIndex, POKE_SPEED+ 20,90,false);

        mRobotCommanderThread.robotMove(motorCPortIndex, POKE_SPEED+50, 300, true);

        //mRobotCommanderThread.robotMove(motorCPortIndex, speed);

        //threadWait(POKE_ONE_WAY_PERIOD);

        // stop motor B & C
        //mRobotCommanderThread.robotMove(motorBPortIndex,0);
        //mRobotCommanderThread.robotMove(motorCPortIndex,0);


    }

    public void onRight(){
        Log.d(TAG, "onRight");
/*
        // move right
        mRobotCommanderThread.robotRotate(TURNING_SPEED);
        threadWait(ONE_MOVE_PERIOD);

        // stop moving
        mRobotCommanderThread.robotMove(0);*/
        int motorAPortIndex = 0; // port num obtained from #activity_remotecontroller.xml view tag
        int motorDPortIndex = 3;
        mRobotCommanderThread.robotMove(motorAPortIndex, TURNING_SPEED);
        mRobotCommanderThread.robotMove(motorDPortIndex, -TURNING_SPEED);
        threadWait(ONE_MOVE_PERIOD);
        mRobotCommanderThread.robotMove(0);
    }

    public void onSlap(){
        Log.d(TAG, "onSlap");

        // move motor B & C backwards
        int motorBPortIndex = 1; // port num obtained from #activity_remotecontroller.xml view tag
        int motorCPortIndex = 2;

        mRobotCommanderThread.robotMove(motorBPortIndex, -POKE_SPEED, 130,true);
        mRobotCommanderThread.robotMove(motorCPortIndex, -POKE_SPEED-40, 300, true);

        mRobotCommanderThread.robotMove(motorBPortIndex, POKE_SPEED+ 20,130,false);

        mRobotCommanderThread.robotMove(motorCPortIndex, POKE_SPEED+40, 300, true);
        mRobotCommanderThread.robotMove(0);



    }
    private void threadWait(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
                            } else if (CMD_S.equals(msg)){
                                onSlap();
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

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ROBOT_CONNECTION:
                    EventBus.getDefault().post(new DismissConnectionDialog());

                    switch (msg.arg1) {

                        case STATUS_ERROR:

                            EventBus.getDefault().post(new ConnectionFailed());
//                            setConnectionState(STATE_CONNECTING); // vivi ori
//                            findRobot();

                            break;
                        case STATUS_OK:

                            EventBus.getDefault().post(new ConnectionSucceeded());
//                            setConnectionState(STATE_CONNECTED); // vivi ori

                            startRobotCommander();

                            break;
                        default:
                            break;
                    }

                    break;
                case MSG_ROBOT_TYPE_DETECTED:
                    switch (msg.arg1) {
                        case STATUS_OK:

                            EventBus.getDefault().post(new RobotTypeDetected());
//                            if (!mIsPortsConfigured) { // vivi ori
//
//                                configureRobotPorts();
//                            } else {
//                                setUpUI();
//                            }
                            break;
                    }
            }
        }
    };

    private synchronized void connectToRobot(String robotAddress) {
        if (mRobotConnectorThread!=null) {
            mRobotConnectorThread.cancel();
            mRobotConnectorThread = null;
        }
        mRobotConnectorThread = new RobotConnectorThread(mHandler, robotAddress);
        mRobotConnectorThread.start();
//        mConnectingProgressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.connecting_please_wait), true);
    }

    private synchronized void startRobotCommander() {
        if (mRobotCommanderThread !=null) {
            mRobotCommanderThread.cancel();
            mRobotCommanderThread = null;
        }
        mRobotCommanderThread = new RobotCommanderThread(mHandler);
        mRobotCommanderThread.start();
    }

    private void closeConnections() {

        if (mRobotCommanderThread !=null)
        {
            mRobotCommanderThread.cancel();
            mRobotCommanderThread = null;
        }

        if (mRobotConnectorThread !=null)
        {
            mRobotConnectorThread.cancel();
            mRobotConnectorThread = null;
        }

        if (RobotSocket!=null) {
            try {
                RobotSocket.close();
            } catch (IOException ioe) {
                Log.e(TAG,"Cannot close socket");
            }
        }
    }

    private class RobotConnectorThread extends Thread {

        private Handler mHandler;
        private String mAddress;

        public RobotConnectorThread(Handler handler, String address) {
            mAddress = address;
            mHandler = handler;
        }

        public void run() {
            setName("ConnectThread");
            EventBus.getDefault().post(new CancelBluetoothDiscovery());
            if (RobotSocket!=null) {

                try {
                    RobotSocket.close();

                } catch (IOException e2) {
                    Log.e(TAG,"Could not close socket");
                }
            }

            try {
                BluetoothSocket robotBTSocketTemporary;
                BluetoothDevice robotDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mAddress);
                if (robotDevice == null) {
                    mHandler.obtainMessage(MSG_ROBOT_CONNECTION, STATUS_ERROR,0).sendToTarget();
                }
                robotBTSocketTemporary = robotDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
                try {
                    robotBTSocketTemporary.connect();
                } catch (IOException e) {


                    // try another method for connection, this should work on the HTC desire, credited to Michael Biermann, MindDROID project

                    try {
                        Method mMethod = robotDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                        robotBTSocketTemporary = (BluetoothSocket) mMethod.invoke(robotDevice, Integer.valueOf(1));
                        robotBTSocketTemporary.connect();

                    } catch (IOException e1) {
                        mHandler.obtainMessage(MSG_ROBOT_CONNECTION, STATUS_ERROR,0).sendToTarget();
                        return;

                    }
                }
                RobotSocket = robotBTSocketTemporary;

                mHandler.obtainMessage(MSG_ROBOT_CONNECTION,STATUS_OK,0).sendToTarget();
                pubnubConnect();

            } catch (Exception e) {
                mHandler.obtainMessage(MSG_ROBOT_CONNECTION, STATUS_ERROR,0).sendToTarget();
            }

            synchronized (SlaveService.this) {
                mRobotConnectorThread = null;
            }

        }

        public void cancel() {
            try {
                pubnubDisconnect();

                if (RobotSocket!=null)
                    RobotSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close socket " + e.getMessage());
            }
        }
    }

    private class RobotCommanderThread extends Thread implements Commander.CommanderListener {
        private BluetoothCommunicator mCommunicator;
        private Handler mHandler;
        private Commander mRobotCommander;

        private boolean isTypeDetected;


        public RobotCommanderThread(Handler handler) {

            mHandler = handler;
            isTypeDetected = false;

        }


        public void robotMove(int speed) {
            if (mRobotCommander!=null)
                mRobotCommander.doRobotMove(speed,0,false);
        }


        public void robotMove(int portIndex, int speed) {
            if (mRobotCommander!=null) {

                mRobotCommander.doRobotMove(portIndex, speed, 0, false);
            }
        }

        public void robotMove(int portIndex, int speed, int degree) {
            if (mRobotCommander!=null) {

                mRobotCommander.doRobotMove(portIndex, speed,degree, false);
            }
        }

        public void robotMove(int portIndex, int speed, int degree, boolean block) {
            if (mRobotCommander!=null) {

                mRobotCommander.doRobotMove(portIndex, speed,degree, block);
            }
        }

        public void robotRotate(int speed) {
            if (mRobotCommander!=null)
                mRobotCommander.doRobotRotate(speed, 0, false);
        }

/*        public void robotMoveArm(int speed) {
            if (mRobotCommander!=null)
                mRobotCommander.doRobotTurnArm(speed,0,false);
        }*/

        public void robotBeep(int frequency) {
            if (mRobotCommander!=null)
                mRobotCommander.doRobotBeep(speed,frequency,600,false);
        }

        public void run() {

            try {
                InputStream robotInputStream = RobotSocket.getInputStream();
                OutputStream robotOutputStream = RobotSocket.getOutputStream();
                mCommunicator = BluetoothCommunicator.getInstance(robotInputStream, robotOutputStream);
                mRobotCommander = new CommanderImpl(getApplicationContext(), mCommunicator, this);
                mRobotCommander.detectRobotType();


            } catch (IOException e) {
                e.printStackTrace();
                mHandler.obtainMessage(MSG_ROBOT_CONNECTION, STATUS_ERROR).sendToTarget();
            }

        }

        @Override
        public void onCommunicationFailure(int status) {
            if (mHandler!=null) {
                Message message = mHandler.obtainMessage(MSG_ROBOT_CONNECTION, STATUS_ERROR);
                message.sendToTarget();
            }
        }

        public void cancel() {

            mCommunicator=null;
            mRobotCommander=null;
            try {
                if (RobotSocket!=null)
                    RobotSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close socket " + e.getMessage());
            }
        }

        @Override
        public void onPortsConfigured(int status) {


        }

        @Override
        public void onProgramLineExecuted(int status) {}

        @Override
        public void onProgramComplete(int status) {}

        @Override
        public void onPortsDetected(int status) {}

        @Override
        public void updateMotorReading(int status, int value) {}

        @Override
        public void updateSensorReading(Robot.Sensor sensor, int status, int value) {}

        @Override
        public void onRobotTypeDetected(int robotType) {
            if (!isTypeDetected)
                mHandler.obtainMessage(MSG_ROBOT_TYPE_DETECTED, STATUS_OK).sendToTarget();
            isTypeDetected = true;

        }
    }
}
