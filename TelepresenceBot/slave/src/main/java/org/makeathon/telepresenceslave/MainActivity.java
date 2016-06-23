package org.makeathon.telepresenceslave;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.makeathon.telepresenceslave.roboliterate.activities.ChooseDeviceActivity;
import org.makeathon.telepresenceslave.roboliterate.activities.ConfigureDeviceActivity;
import org.makeathon.telepresenceslave.roboliterate.robotcomms.BluetoothCommunicator;
import org.makeathon.telepresenceslave.roboliterate.robotcomms.Commander;
import org.makeathon.telepresenceslave.roboliterate.robotcomms.CommanderImpl;
import org.makeathon.telepresenceslave.roboliterate.robotcomms.Robot;
import org.makeathon.telepresenceslave.roboliterate.views.SpeedSlider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    private static final int LOOP = 10;

    private static final int MSG_ROBOT_TYPE_DETECTED = 3;
    private static final int MSG_ROBOT_CONNECTION = 6;
    private static final int STATUS_OK = 0;
    private static final int STATUS_ERROR = 16;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_CONFIGURE_DEVICE = 3;

    private static final int IDLE = 9;
    private static final int MOVING_FORWARD =10;
    private static final int MOVING_BACK =11;
    private static final int MOVING_LEFT =12;
    private static final int MOVING_RIGHT =13;

    private static final String MY_PREFS = "MyPrefs";
    private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private Pubnub mPubnub;
    private Dialog mConnectingProgressDialog;

    private static final String CMD_F = "CMD_F";
    private static final String CMD_B = "CMD_B";
    private static final String CMD_L = "CMD_L";
    private static final String CMD_R = "CMD_R";

    private BluetoothSocket RobotSocket;
    private int mRobotState;
    private int speed = 50;
    private BluetoothAdapter mAdapter;
    private RobotConnectorThread mRobotConnectorThread;
    private RobotCommanderThread mRobotCommanderThread;
    private String mRobotAddress;

    private boolean mIsPortsConfigured;

    @Override
    public void onResume(){
        super.onResume();
        Intent intent = new Intent(this, MainActivity.class);
// use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) 10003, intent, 0);

// build notification
// the addAction re-use the same intent to keep the example short
        Notification n  = new Notification.Builder(this)
                .setContentTitle("TelepresenceBot")
                .setContentText("launch controller")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pIntent)
                .setAutoCancel(true).build();


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, n);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPubnub = new Pubnub("pub-c-e0e0e558-9aa1-412e-a4ca-ce286e939e54", "sub-c-4b5e362c-27fd-11e6-84f2-02ee2ddab7fe");

        connect(null);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mIsPortsConfigured = false;

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("speed")) {
                speed = savedInstanceState.getInt("speed");
            }

            if (savedInstanceState.containsKey("ports_configured")) {
                mIsPortsConfigured = savedInstanceState.containsKey("ports_configured");
            }
        }
    }

    private void onForward()
    {
        System.out.println("forward command received");
        int loop = LOOP;
        while (loop != 0) {
            // move up
            mRobotCommanderThread.robotMove(speed);
            mRobotState =MOVING_FORWARD;
            loop--;
        }

        // stop moving
        mRobotCommanderThread.robotMove(0);
        mRobotState =IDLE;
    }
    private void onBackward(){
        System.out.println("backward command received");

        int loop = LOOP;
        while (loop != 0) {
            // move down
            mRobotCommanderThread.robotMove(-speed);
            mRobotState =MOVING_BACK;
            loop--;
        }

        // stop moving
        mRobotCommanderThread.robotMove(0);
        mRobotState =IDLE;

    }
    private void onLeft(){
        System.out.println("left command received");

        int loop = LOOP;
        while (loop != 0) {
            // move right
            mRobotCommanderThread.robotRotate(-speed);
            mRobotState =MOVING_LEFT;
            loop--;
        }

        // stop moving
        mRobotCommanderThread.robotMove(0);
        mRobotState =IDLE;
    }
    private void onRight(){
        System.out.println("right command received");

        int loop = LOOP;
        while (loop != 0) {
            // move right
            mRobotCommanderThread.robotRotate(speed);
            mRobotState =MOVING_RIGHT;
            loop--;
        }

        // stop moving
        mRobotCommanderThread.robotMove(0);
        mRobotState =IDLE;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getConnectionState()==STATE_CONNECTED) {
            connectToRobot();

        } else {
            findRobot();
        }
    }

    /**
     * Callback function when returning from other Activities
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {


            case REQUEST_CONNECT_DEVICE:

                // When DeviceListActivity returns with a device to connect

                if (resultCode == Activity.RESULT_OK) {

                    // Get the device MAC address and send it to RobotConnectorService. Show Connecting progress dialog

                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        mRobotAddress = extras.getString(ChooseDeviceActivity.EXTRA_DEVICE_ADDRESS);
                        setConnectionState(STATE_CONNECTING);
                        connectToRobot();
                    }
                } else if (resultCode== Activity.RESULT_CANCELED) {

                    onBackPressed();
                }


                break;
            case REQUEST_CONFIGURE_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    mIsPortsConfigured = true;
//                    setUpUI(); // vivi ori
                } else if (resultCode== Activity.RESULT_CANCELED) {

                    onBackPressed();

                }

                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    findRobot();
                }
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeConnections();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (getConnectionState()==STATE_CONNECTED) {
            outState.putString("robot_address", mRobotAddress);
            outState.putBoolean("ports_configured", mIsPortsConfigured);
        }
        outState.putInt("speed", speed);
    }

    @Override
    public void onBackPressed() {
        closeConnections();
        super.onBackPressed();
    }

    @Override
    public void onDestroy(){
        mPubnub.shutdown();
        super.onDestroy();

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

    private void setConnectionState(int state) {
        mRobotState = state;
    }

    private int getConnectionState() {
        return mRobotState;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ROBOT_CONNECTION:
                    mConnectingProgressDialog.dismiss();

                    switch (msg.arg1) {

                        case STATUS_ERROR:

                            Toast.makeText(getApplicationContext(),R.string.connection_failed, Toast.LENGTH_SHORT).show();
                            setConnectionState(STATE_CONNECTING);
                            findRobot();

                            break;
                        case STATUS_OK:

                            setConnectionState(STATE_CONNECTED);

                            startRobotCommander();


                            break;
                        default:
                            break;
                    }

                    break;
                case MSG_ROBOT_TYPE_DETECTED:
                    switch (msg.arg1) {
                        case STATUS_OK:

                            if (!mIsPortsConfigured) {

                                configureRobotPorts();
                            } else {
//                                setUpUI(); // vivi ori
                            }
                            break;

                    }
            }
        }
    };

    private synchronized void connectToRobot() {
        if (mRobotConnectorThread!=null) {
            mRobotConnectorThread.cancel();
            mRobotConnectorThread = null;
        }
        mRobotConnectorThread = new RobotConnectorThread(mHandler, mRobotAddress);
        mRobotConnectorThread.start();
        mConnectingProgressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.connecting_please_wait), true);
    }

    /**
     * If Bluetooth Adapter has been detected, and Bluetooth has been turned on,
     * start ChooseDeviceActivity to allow user to select robot to connect with. Otherwise open device's
     * activity for activating Bluetooth and wait for result
     */
    private void findRobot() {
        if (mAdapter == null) {
            Toast.makeText(this, R.string.no_bluetooth, Toast.LENGTH_LONG).show();

        } else {

            if (!mAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Intent chooseDeviceIntent = new Intent(this, ChooseDeviceActivity.class);
                startActivityForResult(chooseDeviceIntent, REQUEST_CONNECT_DEVICE);
            }
        }
    }

    /**
     * Open ConfigureDevice activity if connected to robot
     */
    private void configureRobotPorts() {

        if (getConnectionState()==STATE_CONNECTED) {
            SharedPreferences sharedPreferences = getSharedPreferences(MY_PREFS, Activity.MODE_PRIVATE);

            Robot.Motor.LEFT.setPort(sharedPreferences.getInt("MOTOR_LEFT", Robot.Motor.LEFT.getPort()));
            Robot.Motor.RIGHT.setPort(sharedPreferences.getInt("MOTOR_RIGHT", Robot.Motor.RIGHT.getPort()));
            Robot.Motor.ARM.setPort(sharedPreferences.getInt("MOTOR_ARM", Robot.Motor.ARM.getPort()));

            final int robotType = Robot.getRobotType();

            if (robotType==Robot.MODEL_EV3 || robotType == Robot.MODEL_NXT) {
                Intent intent = new Intent(this, ConfigureDeviceActivity.class);
                startActivityForResult(intent, REQUEST_CONFIGURE_DEVICE);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.choose_robot_type).setTitle(R.string.cannot_get_robottype);
                builder.setPositiveButton(R.string.ev3, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Robot.setRobotType(Robot.MODEL_EV3);
                        configureRobotPorts();

                    }
                });
                builder.setNegativeButton(R.string.nxt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Robot.setRobotType(Robot.MODEL_NXT);
                        configureRobotPorts();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }

    private View.OnTouchListener motorForwardListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            String tag = (String)view.getTag();
            int portIndex = Integer.parseInt(tag);
            if (event.getAction()== MotionEvent.ACTION_DOWN) {
                mRobotCommanderThread.robotMove(portIndex, speed);
                mRobotState =portIndex;
            } else if (event.getAction()== MotionEvent.ACTION_UP) {
                mRobotCommanderThread.robotMove(portIndex,0);
                mRobotState =IDLE;                }
            return false;
        }
    };

    private View.OnTouchListener motorBackListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            String tag = (String)view.getTag();
            int portIndex = Integer.parseInt(tag);
            if (event.getAction()== MotionEvent.ACTION_DOWN) {
                mRobotCommanderThread.robotMove(portIndex, -speed);
                mRobotState =portIndex;
            } else if (event.getAction()== MotionEvent.ACTION_UP) {
                mRobotCommanderThread.robotMove(portIndex,0);
                mRobotState =IDLE;                }
            return false;
        }
    };

    private View.OnClickListener noteClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            mRobotCommanderThread.robotBeep(Integer.parseInt((String)(view.getTag())));
        }
    };

    private void setUpUI() { // vivi copied

        speed = 50;
        int robotType = Robot.getRobotType();

//        Button upButton = (Button)findViewById(R.id.button_forward);
//        upButton.setBackgroundDrawable(robotType == Robot.MODEL_NXT ? getResources().getDrawable(R.drawable.direction_up) : getResources().getDrawable(R.drawable.direction_up_3) );
//        upButton.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent event) {
//                if (event.getAction()== MotionEvent.ACTION_DOWN) {
//                    mRobotCommanderThread.robotMove(speed);
//                    mRobotState=MOVING_FORWARD;
//
//                } else if (event.getAction()== MotionEvent.ACTION_UP) {
//                    mRobotCommanderThread.robotMove(0);
//                    mRobotState=IDLE;
//                }
//                return false;
//            }
//        });

//        Button downButton = (Button)findViewById(R.id.button_back);
//        downButton.setBackgroundDrawable(robotType == Robot.MODEL_NXT ? getResources().getDrawable(R.drawable.direction_down) : getResources().getDrawable(R.drawable.direction_down_3) );
//        downButton.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent event) {
//                if (event.getAction()== MotionEvent.ACTION_DOWN) {
//                    mRobotCommanderThread.robotMove(-speed);
//                    mRobotState=MOVING_BACK;
//                } else if (event.getAction()== MotionEvent.ACTION_UP) {
//                    mRobotCommanderThread.robotMove(0);
//                    mRobotState=IDLE;                }
//                return false;
//            }
//        });

//        Button leftButton = (Button)findViewById(R.id.button_left);
//        leftButton.setBackgroundDrawable(robotType == Robot.MODEL_NXT ? getResources().getDrawable(R.drawable.direction_left) : getResources().getDrawable(R.drawable.direction_left_3) );
//        leftButton.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent event) {
//                if (event.getAction()== MotionEvent.ACTION_DOWN) {
//                    mRobotCommanderThread.robotRotate(-speed);
//                    mRobotState=MOVING_LEFT;
//                } else if (event.getAction()== MotionEvent.ACTION_UP) {
//                    mRobotCommanderThread.robotMove(0);
//                    mRobotState=IDLE;                }
//                return false;
//            }
//        });
//
//        Button rightButton = (Button)findViewById(R.id.button_right);
//        rightButton.setBackgroundDrawable(robotType == Robot.MODEL_NXT ? getResources().getDrawable(R.drawable.direction_right) : getResources().getDrawable(R.drawable.direction_right_3) );
//        rightButton.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent event) {
//                if (event.getAction()== MotionEvent.ACTION_DOWN) {
//                    mRobotCommanderThread.robotRotate(speed);
//                    mRobotState=MOVING_RIGHT;
//                } else if (event.getAction()== MotionEvent.ACTION_UP) {
//                    mRobotCommanderThread.robotMove(0);
//                    mRobotState=IDLE;                }
//                return false;
//            }
//        });
        ImageView robotImage = (ImageView)findViewById(R.id.robot_icon);

        if (Robot.getRobotType()!= Robot.MODEL_EV3) {
            LinearLayout ll = (LinearLayout)findViewById(R.id.motorD);
            ll.setVisibility(View.GONE);

            robotImage.setImageDrawable(getResources().getDrawable(R.drawable.large_icon_nxt));

        } else {
            robotImage.setImageDrawable(getResources().getDrawable(R.drawable.large_icon_ev3));
        }



        Button motorAForward = (Button)findViewById(R.id.motorA_forward);
        motorAForward.setOnTouchListener(motorForwardListener);
        Button motorBForward = (Button)findViewById(R.id.motorB_forward);
        motorBForward.setOnTouchListener(motorForwardListener);
        Button motorCForward = (Button)findViewById(R.id.motorC_forward);
        motorCForward.setOnTouchListener(motorForwardListener);
        Button motorDForward = (Button)findViewById(R.id.motorD_forward);
        motorDForward.setOnTouchListener(motorForwardListener);

        Button motorABack = (Button)findViewById(R.id.motorA_back);
        motorABack.setOnTouchListener(motorBackListener);
        Button motorBBack = (Button)findViewById(R.id.motorB_back);
        motorBBack.setOnTouchListener(motorBackListener);
        Button motorCBack = (Button)findViewById(R.id.motorC_back);
        motorCBack.setOnTouchListener(motorBackListener);
        Button motorDBack = (Button)findViewById(R.id.motorD_back);
        motorDBack.setOnTouchListener(motorBackListener);


  /*      Button armButton = (Button)findViewById(R.id.button_arm);
        armButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction()==MotionEvent.ACTION_DOWN) {
                    mRobotCommanderThread.robotMoveArm(speed);
                    mRobotState=MOVING_ARM;
                } else if (event.getAction()==MotionEvent.ACTION_UP) {
                    mRobotCommanderThread.robotMoveArm(0);
                    mRobotState=IDLE;                }
                return false;
            }
        });
*/
        SpeedSlider speedSlider = (SpeedSlider)findViewById(R.id.speedSlider);
        speedSlider.setProgress(50);
        speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                speed = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
 /*               switch (mRobotState) {
                    case MOVING_FORWARD:
                        mRobotCommanderThread.robotMove(speed);
                        break;
                    case MOVING_BACK:
                        mRobotCommanderThread.robotMove(-speed);
                        break;
                    case MOVING_LEFT:
                        mRobotCommanderThread.robotRotate(-speed);
                        break;
                    case MOVING_RIGHT:
                        mRobotCommanderThread.robotRotate(speed);
                        break;
                    case MOVING_ARM:
                        mRobotCommanderThread.robotMoveArm(speed);
                    default:
                        break;
                }*/

            }
        });

        Button beepA = (Button)findViewById(R.id.button_A);
        beepA.setOnClickListener(noteClickedListener);
        Button beepB = (Button)findViewById(R.id.button_B);
        beepB.setOnClickListener(noteClickedListener);
        Button beepC = (Button)findViewById(R.id.button_C);
        beepC.setOnClickListener(noteClickedListener);
        Button beepD = (Button)findViewById(R.id.button_D);
        beepD.setOnClickListener(noteClickedListener);
        Button beepE = (Button)findViewById(R.id.button_E);
        beepE.setOnClickListener(noteClickedListener);
        Button beepF = (Button)findViewById(R.id.button_F);
        beepF.setOnClickListener(noteClickedListener);
        Button beepG = (Button)findViewById(R.id.button_G);
        beepG.setOnClickListener(noteClickedListener);
        Button beepA2 = (Button)findViewById(R.id.button_A2);
        beepA2.setOnClickListener(noteClickedListener);
    }

    public void connect(View view){

        try {
            mPubnub.subscribe("my_channel", new Callback() {
                        @Override
                        public void connectCallback(String channel, Object message) {
                            mPubnub.publish("my_channel", "bot connected", new Callback() {});

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
        mPubnub.unsubscribe("my_channel", new Callback() {
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

    private synchronized void startRobotCommander() {
        if (mRobotCommanderThread !=null) {
            mRobotCommanderThread.cancel();
            mRobotCommanderThread = null;
        }
        mRobotCommanderThread = new RobotCommanderThread(mHandler);
        mRobotCommanderThread.start();

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
            mAdapter.cancelDiscovery();
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
            } catch (Exception e) {
                mHandler.obtainMessage(MSG_ROBOT_CONNECTION, STATUS_ERROR,0).sendToTarget();
            }

            synchronized (MainActivity.this) {
                mRobotConnectorThread = null;
            }

        }

        public void cancel() {
            try {
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
