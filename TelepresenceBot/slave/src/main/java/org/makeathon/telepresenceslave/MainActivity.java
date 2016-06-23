package org.makeathon.telepresenceslave;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.makeathon.telepresenceslave.roboliterate.activities.ChooseDeviceActivity;
import org.makeathon.telepresenceslave.roboliterate.activities.ConfigureDeviceActivity;
import org.makeathon.telepresenceslave.roboliterate.robotcomms.Robot;
import org.makeathon.telepresenceslave.roboliterate.views.SpeedSlider;

import events.CancelBluetoothDiscovery;
import events.ConnectionFailed;
import events.DismissConnectionDialog;
import events.ConnectionSucceeded;
import events.RobotTypeDetected;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_CONFIGURE_DEVICE = 3;

    private static final String MY_PREFS = "MyPrefs";

    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private Dialog mConnectingProgressDialog;

    private int mRobotState;
    private BluetoothAdapter mAdapter;
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

        EventBus.getDefault().register(this);

//        startActivity(new Intent(this, RemoteControlActivity.class)); // vivi test

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mIsPortsConfigured = false;

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("speed")) {
//                speed = savedInstanceState.getInt("speed");
            }

            if (savedInstanceState.containsKey("ports_configured")) {
                mIsPortsConfigured = savedInstanceState.containsKey("ports_configured");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getConnectionState()==STATE_CONNECTED) {
//            connectToRobot(); // vivi ori

        } else {
            findRobot();
        }
    }

    /**
     * Callback function whenx returning from other Activities
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
//                        connectToRobot(); // vivi ori
                        Intent service = new Intent(this, SlaveService.class);
                        service.putExtra(ChooseDeviceActivity.EXTRA_DEVICE_ADDRESS, mRobotAddress);

                        mConnectingProgressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.connecting_please_wait), true);
                        startService(service); // vivi test
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
//        closeConnections(); // vivi ori
    }

    @Override
    public void onBackPressed() {
//        closeConnections(); // vivi ori
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(DismissConnectionDialog event) {
        if (mConnectingProgressDialog != null) {
            mConnectingProgressDialog.dismiss();
        }
    }

    @Subscribe
    public void onEvent(ConnectionFailed event) {
        if (mAdapter != null) {
            Toast.makeText(getApplicationContext(),R.string.connection_failed, Toast.LENGTH_SHORT).show();
            findRobot();
        }
    }

    @Subscribe
    public void onEvent(CancelBluetoothDiscovery event) {
        if (mAdapter != null) {
            mAdapter.cancelDiscovery();
        }
    }

    @Subscribe
    public void onEvent(ConnectionSucceeded event) {
        if (mAdapter != null) {
            setConnectionState(STATE_CONNECTED);
        }
    }

    @Subscribe
    public void onEvent(RobotTypeDetected event) {
        if (!mIsPortsConfigured) {

            configureRobotPorts();
        }
    }

    private void setConnectionState(int state) {
        mRobotState = state;
    }

    private int getConnectionState() {
        return mRobotState;
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
        public boolean onTouch(View view, MotionEvent event) { // vivi ori
//            String tag = (String)view.getTag();
//            int portIndex = Integer.parseInt(tag);
//            if (event.getAction()== MotionEvent.ACTION_DOWN) {
//                mRobotCommanderThread.robotMove(portIndex, speed);
//                mRobotState =portIndex;
//            } else if (event.getAction()== MotionEvent.ACTION_UP) {
//                mRobotCommanderThread.robotMove(portIndex,0);
//                mRobotState =IDLE;                }
            return false;
        }
    };

    private View.OnTouchListener motorBackListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent event) { // vivi ori
//            String tag = (String)view.getTag();
//            int portIndex = Integer.parseInt(tag);
//            if (event.getAction()== MotionEvent.ACTION_DOWN) {
//                mRobotCommanderThread.robotMove(portIndex, -speed);
//                mRobotState =portIndex;
//            } else if (event.getAction()== MotionEvent.ACTION_UP) {
//                mRobotCommanderThread.robotMove(portIndex,0);
//                mRobotState =IDLE;                }
            return false;
        }
    };

    private View.OnClickListener noteClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

//            mRobotCommanderThread.robotBeep(Integer.parseInt((String)(view.getTag()))); // vivi ori
        }
    };

    private void setUpUI() { // vivi copied

//        speed = 50;
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
//                speed = i;
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
}
