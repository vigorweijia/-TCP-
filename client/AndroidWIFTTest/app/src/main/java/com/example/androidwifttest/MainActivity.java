package com.example.androidwifttest;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.net.sip.SipSession;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity {

    public ImageView imgCompass;
    public ImageView imgArrow;

    public EditText editText;
    public TextView textView_send;
    public TextView textView_receive;

    private float lastRotateDegree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgCompass = findViewById(R.id.img_compass);
        imgArrow = findViewById(R.id.img_arrow);

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mTextViewAcceleration = findViewById(R.id.AccelerationText);
        mTextViewMagnetic = findViewById(R.id.MagneticText);
        mTextViewCalculation = findViewById(R.id.CalculationText);

        mTextViewTouch = findViewById(R.id.TouchPad);
        mTextViewTouch.setEnabled(true);
        mTextViewTouch.setOnTouchListener(new myOnTouchListener());
        mTextViewMove_Down = findViewById(R.id.move_down);
        mTextViewMove_Move = findViewById(R.id.move_move);
        mTextViewMove_Up = findViewById(R.id.move_up);

        editText = findViewById(R.id.send_editText);
        textView_send = findViewById(R.id.send_textView);
        textView_receive = findViewById(R.id.receive_textView);
        textView_send.setMovementMethod(ScrollingMovementMethod.getInstance());
        textView_receive.setMovementMethod(ScrollingMovementMethod.getInstance());

        TaskCenter.sharedCenter().setDisconnectedCallback(new TaskCenter.OnServerDisconnectedCallbackBlock() {
            @Override
            public void callback(IOException e) {
                textView_receive.setText(textView_receive.getText().toString() + "断开连接" + "\n");
            }
        });
        TaskCenter.sharedCenter().setConnectedCallback(new TaskCenter.OnServerConnectedCallbackBlock() {
            @Override
            public void callback() {
                textView_receive.setText(textView_receive.getText().toString() + "连接成功" + "\n");
            }
        });
        TaskCenter.sharedCenter().setReceivedCallback(new TaskCenter.OnReceiveCallbackBlock() {
            @Override
            public void callback(String receiceMessage) {
                String[] commands = receiceMessage.split(",");
                if(commands[0].equals("ROTATE")) {
                    float RotateDegree = -(float) Math.toDegrees(Float.valueOf(commands[1]));
                    Log.d("float", receiceMessage);
                    if (Math.abs(RotateDegree - lastRotateDegree) > 1) {
                        RotateAnimation animation = new RotateAnimation(
                                lastRotateDegree, RotateDegree,
                                Animation.RELATIVE_TO_SELF, 0.5f,
                                Animation.RELATIVE_TO_SELF, 0.5f);
                        animation.setFillAfter(true);
                        imgCompass.startAnimation(animation);
                        lastRotateDegree = RotateDegree;
                    }
                    textView_receive.setText(textView_receive.getText().toString() + commands[1] + "\n");
                }
                else if(commands[0].equals("EXPRESSION")) {
                    mTextViewCalculation.setText("运算结果： "+commands[1]);
                }
            }
        });
    }

    public void sendMessage(View view) {
        String msg = "EXPRESSION,"+editText.getText().toString();
        textView_send.setText(textView_send.getText().toString() + msg + "\n");
        TaskCenter.sharedCenter().send(msg.getBytes());
    }

    public void connect(View view) {
        TaskCenter.sharedCenter().connect("192.168.137.1",54321);
    }

    public void disconnect(View view) {
        TaskCenter.sharedCenter().disconnect();
    }

    public void clear1(View view) {
        textView_send.setText("");
    }

    public void clear2(View view) {
        textView_receive.setText("");
    }

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Sensor magneticSensor;
    private SensorEventListener mSensorEventListener;
    private TextView mTextViewAcceleration;
    private TextView mTextViewMagnetic;
    private TextView mTextViewCalculation;

    @Override
    public void onResume() {
        super.onResume();
        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float x;
                float y;
                float z;
                NumberFormat numberFormat = new DecimalFormat("0.000000");
                String xstr = "";
                String ystr = "";
                String zstr = "";
                String type = "Unknown";
                String text;
                if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    x = sensorEvent.values[0];
                    y = sensorEvent.values[1];
                    z = sensorEvent.values[2];
                    xstr = numberFormat.format(x);
                    ystr = numberFormat.format(y);
                    zstr = numberFormat.format(z);
                    mTextViewAcceleration.setText("加速度： "+xstr + ", " + ystr + ", " + zstr);
                    type = "ACCELEROMETER,";
                }
                else if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    x = sensorEvent.values[0];
                    y = sensorEvent.values[1];
                    z = sensorEvent.values[2];
                    xstr = numberFormat.format(x);
                    ystr = numberFormat.format(y);
                    zstr = numberFormat.format(z);
                    mTextViewMagnetic.setText("磁感强度： "+xstr + ", " + ystr + ", " + zstr);
                    type = "MAGNETIC,";
                }
                text = type + xstr + "," + ystr + "," + zstr;
                byte[] bytes = text.getBytes();
                TaskCenter.sharedCenter().send(bytes);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        mSensorManager.registerListener(mSensorEventListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorEventListener, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(mSensorEventListener);
    }

    private TextView mTextViewTouch;
    private TextView mTextViewMove_Down;
    private TextView mTextViewMove_Move;
    private TextView mTextViewMove_Up;

    private class myOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            NumberFormat numberformat = new DecimalFormat("0.0");
            float x = event.getX();
            float y = event.getY();
            String xstr = numberformat.format(x);
            String ystr = numberformat.format(y);
            String type = "";
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    type = "TOUCH,DOWN,";mTextViewMove_Down.setText("起始位置："+xstr+","+ystr);break;
                case MotionEvent.ACTION_MOVE:
                    type = "TOUCH,MOVE,";mTextViewMove_Move.setText("实时位置："+xstr+","+ystr);break;
                case MotionEvent.ACTION_UP:
                    type = "TOUCH,UP,";mTextViewMove_Up.setText("结束位置："+xstr+","+ystr);break;
            }
            String text = type+xstr+","+ystr;
            byte[] bytes = text.getBytes();
            TaskCenter.sharedCenter().send(bytes);
            return true;
        }
    }
}
