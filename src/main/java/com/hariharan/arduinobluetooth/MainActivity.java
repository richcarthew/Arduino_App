package com.hariharan.arduinobluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.widget.VideoView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
//    private final String DEVICE_NAME="MyBTBee";
private final String DEVICE_NAME="=richBT1";

    // this prog communicates with the Arduino via the program:
    //
    //private final String DEVICE_ADD="20:13:11:12:39:49"; // RichC

    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    Button startButton, sendButton, clearButton, stopButton;
    TextView textView;
    EditText editText;
    boolean deviceConnected=false;
    Thread thread;
    byte buffer[];
    int bufferPosition;
    boolean stopThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        textView.setSingleLine(false);
        setUiEnabled(false);


        if (ActivityCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, //this returns true if the user has already denied permission at some point
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(MainActivity.this, "I know you said no, but I'm asking again", Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION); //permissions wrapped in array syntax allow multiple perms
            //and because this is going to result in opening a dialog box, I'm paasing in a request code - that's what my made up constant "MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION" was
            return; //asynchronous as it's not immediately calling for the BT to begin etc - so I have to handle the result when it comes back..so add a new method after the onCreate method to capture it.
        }
    }
    /*
     The below method is an implementation of the onRequestPermissionsResult method. It's overriding the super classes version of the method.
    Deal with an import that's required. NonNull is an Android annotation, and by adding it here I'm guaranteeing that I'm not receiving null strings back.

    This method will be called after the user has responded to the dialog. If they grant permission then the array of integer values named
    grantResults will have a length greater than 0, and because I only requested one permission the first item at index zero will have a value
    matching the constant Permission_Granted. And in that case I'll display a message to the user telling them permission was granted and then
    once again I'll call my method to make the phone call. If they deny permission then I'll show them a message saying that that's what they did.

    list of dangerous permissions for Android 6:

    https://developer.android.com/guide/topics/security/permissions.html#normal-dangerous
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission was granted!",
                            Toast.LENGTH_SHORT).show();
                    BTinit();
                } else {
                    Toast.makeText(MainActivity.this, "Permission was denied!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void setUiEnabled(boolean bool)
    {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);

    }

    public boolean BTinit()
    {
        boolean found=false;
        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show();
        }
        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            Toast.makeText(MainActivity.this, "bluetoothAdapter.isEnabled()",
                    Toast.LENGTH_SHORT).show();
        }



        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Please Pair the Device first",Toast.LENGTH_SHORT).show();
        }
        else
        {
            for (BluetoothDevice iterator : bondedDevices)
            {
                Toast.makeText(MainActivity.this, iterator.getAddress(),
                        Toast.LENGTH_SHORT).show();
                //if(iterator.getAddress().equals(DEVICE_ADD))

                if(iterator.getName().equals(DEVICE_NAME))// rjc NOT getAddress()

                {
                    device=iterator;
                    found=true;
                    Toast.makeText(MainActivity.this, "found Rich Artwork",
                            Toast.LENGTH_SHORT).show();
                    break;
                }
                else
                {
                    Toast.makeText(MainActivity.this, "found == false",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
        return found;
    }

    public boolean BTconnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return connected;
    }

    public void onClickStart(View view) {
        if(BTinit())
        {
            if(BTconnect())
            {
                final String ready = "r"; //use this to let Arduino know that the video is ready

                setUiEnabled(true);
                deviceConnected=true;
                beginListenForData();
               // textView.append(" \nRichC Conn!\n");
                textView.append(" \n=richBT1 Conn!\n");

                //let Ard know that dev is connected and ready to play video
                try {
                    outputStream.write(ready.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            else {
                Toast.makeText(MainActivity.this, "No connection BTconnect () in onClickStart method",
                        Toast.LENGTH_SHORT).show();
            }


        }
        else {
            Toast.makeText(MainActivity.this, "!BTinit in onClickStart method",
                    Toast.LENGTH_SHORT).show();
        }
    }





    void playVideo(char flag)
    {
        getWindow().setFormat(PixelFormat.UNKNOWN);
        //set up video to play
        VideoView mVideoView2 = (VideoView)findViewById(R.id.videoView1);

        String uriPath2 = "android.resource://com.hariharan.arduinobluetooth/"+R.raw.video2;
        Uri uri2 = Uri.parse(uriPath2);
        mVideoView2.setVideoURI(uri2);
        mVideoView2.requestFocus();

        final String finished = "f"; //use this to let Arduino know that the video has finished

            //set up handler for when video finishes playing and notify arduino
        mVideoView2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            public void onCompletion(MediaPlayer mp) {
                textView.setText("");
                textView.append(" Video finish handler - could sent something to Arduino here...");

                try {
                    outputStream.write(finished.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //https://developer.android.com/training/scheduling/wakelock.html
        /*
        Managing keeping the screen on etc

        */



        if( String.valueOf(flag).equalsIgnoreCase("p"))//p for play
        {
            mVideoView2.start();
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            /*
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1;
            getWindow().setAttributes(params);
            */

            unlockScreen();

        }
        else if( String.valueOf(flag).equalsIgnoreCase("s"))//p for play
        {
            //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mVideoView2.stopPlayback();
        }


    }
    private void unlockScreen() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    /*
    after onClickStart returns successful, we are communicating with the sculpture..



     */
    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount]; //get the number of bytes returned by byteCount

                            inputStream.read(rawBytes);// Reads up to len bytes of data from the input stream into an array of bytes.
                            final String string=new String(rawBytes,"UTF-8");//new string is the array made by the .read(rawBytes) above
                            final char flag = string.charAt(0);
                            handler.post(new Runnable() {
                                        public void run()
                                        {
                                            textView.append(string); //output that new array as string to the user feedback view
                                            //textView.append(String.valueOf(flag));
                                            playVideo(flag);

                                            if( String.valueOf(flag).equalsIgnoreCase("p"))//p for play
                                            {
                                                textView.setText("");
                                                textView.append(" Received play from Arduino, play video1");

                                            }
                                            else if (String.valueOf(flag).equalsIgnoreCase("s"))
                                            {
                                                textView.setText("");
                                                textView.append(" Received stop from Arduino, stop video1");
                                            }


                                        }
                                    });




                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();
        string.concat("\n");
        try {
            outputStream.write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        textView.append("\nSent Data:"+string+"\n");

    }

    public void onClickStop(View view) throws IOException {
        stopThread = true;
        outputStream.close();
        inputStream.close();
        socket.close();
        setUiEnabled(false);
        deviceConnected=false;
        textView.append("\nConnection Closed!\n");
    }

    public void onClickClear(View view) {
        textView.setText("");
    }
}
