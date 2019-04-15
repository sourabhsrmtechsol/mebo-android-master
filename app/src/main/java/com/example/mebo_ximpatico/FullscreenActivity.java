package com.example.mebo_ximpatico;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.potterhsu.rtsplibrary.NativeCallback;
import com.potterhsu.rtsplibrary.RtspClient;
//import com.yuneec.videostreaming.RTSPPlayer;
//import com.yuneec.videostreaming.VideoPlayer;
//import com.yuneec.videostreaming.VideoPlayerException;

import org.json.JSONException;
import org.json.JSONObject;

import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private RtspClient rtspClient;
    static boolean active = false;
    static int frame_count = 0;
    int width, height;
    boolean received_frame = false;
    byte[] frame;
    static Bitmap bmp;
    int img_width = 400;
    int img_height = 400;
    static boolean recording = false;
    boolean initial_gripper_set = false;
    MeboCommand meboCommand = new MeboCommand();
    File rootDir;
    long last_state_time= 0;
    long last_record_time= 0;
    int[] robot_state = {0, 0, 0, 0}; // arm, wrist_ud, wrist_rot, gripper
    int[] robot_command = {0, 0, 0, 0, 0, 0}; //forward, left_right, arm, wrist_ud, wrist_rot, gripper
    int[] last_robot_command = {0, 0, 0, 0, 0, 0}; //forward, left_right, arm, wrist_ud, wrist_rot, gripper
    EnumSet<MeboCommand.CMD> robot_state_names = EnumSet.of( MeboCommand.CMD.ARM_QUERY, MeboCommand.CMD.WRIST_UD_QUERY,
            MeboCommand.CMD.WRIST_ROTATE_QUERY, MeboCommand.CMD.CLAW_QUERY);

    EnumSet<MeboCommand.CMD> robot_command_names = EnumSet.of( MeboCommand.CMD.WHEEL_LEFT_FORWARD , MeboCommand.CMD.WHEEL_RIGHT_FORWARD,
            MeboCommand.CMD.ARM_UP, MeboCommand.CMD.WRIST_UD_UP, MeboCommand.CMD.WRIST_ROTATE_LEFT, MeboCommand.CMD.CLAW_POSITION);
    RequestQueue queue;
    public static final String TAG = "VolleyTag";

    TextView textView;

    ArrayList<TransactionDetails> list ;


//    private SurfaceView videoSurfaceView;
//
//    private Surface videoSurface;
//
//    private SurfaceHolder videoSurfaceHolder;
//
//    private RTSPPlayer videoPlayer;


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        list = new ArrayList<TransactionDetails>();
        String[] arraySpinner = new String[] {
                "Collect toys, put them into a box",
                "Clean the floor using a cloth",
                "Play with me!",
                "Follow me!",
        };
        Spinner s = (Spinner) findViewById(R.id.taskSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);

        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                double speedX = -strength * Math.cos(Math.toRadians(angle)) *7/10.;
                double speedY = strength * Math.sin(Math.toRadians(angle)) *7/10.;

                double V =(100-Math.abs(speedX)) * (speedY/100.) + speedY;
                double W= (100-Math.abs(speedY)) * (speedX/100.) + speedX;

                robot_command[0] = (int)(V-W)/2;
                robot_command[1] = (int)(V+W)/2;
            }
        });

        final SeekBar sk1=(SeekBar) findViewById(R.id.seekBar1);
        final SeekBar sk2=(SeekBar) findViewById(R.id.seekBar2);
        final SeekBar sk3=(SeekBar) findViewById(R.id.seekBar3);
        final SeekBar sk4=(SeekBar) findViewById(R.id.seekBar4);

        sk1.setOnSeekBarChangeListener(new JointSeekBarChangeListener());
        sk2.setOnSeekBarChangeListener(new JointSeekBarChangeListener());
        sk3.setOnSeekBarChangeListener(new JointSeekBarChangeListener());
        sk4.setOnSeekBarChangeListener(new JointSeekBarChangeListener());

        final TextView recordBtn = (TextView) findViewById(R.id.recordBtn);
        final TextView infoBtn = (TextView) findViewById(R.id.infoBtn);






        infoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                list.add( new TransactionDetails("Description Test1","1","100"));
                list.add( new TransactionDetails("Description Test2","2","200"));
                list.add( new TransactionDetails("Description Test3","3","300"));
                list.add( new TransactionDetails("Description Test4","4","400"));
                list.add( new TransactionDetails("Description Test5","5","500"));
                dialogTable(list);



            }
        });



        recordBtn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                recording = ! recording;
                if(recording) {
                    Toast.makeText(FullscreenActivity.this, "Recording...", Toast.LENGTH_SHORT).show();
                    String root = Environment.getExternalStorageDirectory().toString();
                    rootDir = new File(root + "/Ximpatico");
                    boolean success = rootDir.mkdirs();
                    Log.e("TAG", rootDir.getAbsolutePath() + success);

                } else {
                    Toast.makeText(FullscreenActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        active = true;
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        textView = (TextView) findViewById(R.id.textView);
        final ImageView ivPreview = (ImageView) findViewById(R.id.ivPreview);

        queue = Volley.newRequestQueue(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (active) {
                    String url ="http://192.168.99.1/ajax/command.json?";

                    for(int i = 0; i < robot_command.length; i++) {
                        if (i > 0)
                            url += "&";
                        url += meboCommand.generate_single_command(i+1,((MeboCommand.CMD) robot_command_names.toArray()[i]), robot_command[i]);
                        last_robot_command[i] = robot_command[i];
                    }
                    // Request a string response from the provided URL.
                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    // Display the first 500 characters of the response string.
                                    textView.setText("Response is: "+ response);

                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            textView.setText("That didn't work!");
                        }
                    });
                    stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    stringRequest.setTag(TAG);
                    queue.add(stringRequest);


                    if(recording && System.currentTimeMillis() - last_state_time > 1000) {
                        last_state_time = System.currentTimeMillis();
                        for (int i = 0; i < robot_state.length; i++) {
                            sendCommandToRobot((MeboCommand.CMD) robot_state_names.toArray()[i], 0, 0);
                        }
                    }

//                    Toast.makeText(FullscreenActivity.this, " " + robot_command[0] + ", " + robot_command[1], Toast.LENGTH_SHORT).show();

//                    textView.setText(" " + robot_command[0] + ", " + robot_command[1]);
                    if(recording && System.currentTimeMillis() - last_record_time > 200) {
                        recordDemonstrationFrame();

                        last_record_time = System.currentTimeMillis();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }).start();
        sendCommandToRobot(MeboCommand.CMD.CLAW_QUERY, 0, 3);
        sendCommandToRobot(MeboCommand.CMD.INIT_ALL, 0, 3);


        rtspClient = new RtspClient(new NativeCallback() {
            @Override
            public void onFrame(final byte[] frame, final int nChannel, final int width, final int height) {
                frame_count++;
                IntBuffer intBuf = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
                int[] pixels = new int[intBuf.remaining()];
                intBuf.get(pixels);

                //                            textView.setText(""+frame_count + " " + width + "*" + height);
                bmp = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ivPreview.setImageBitmap(bmp);
//                        Log.e("TAG", "Time in JAVA loop : " + (System.currentTimeMillis() - last_time));
                    }
                });

            }
        });


//        try {
//            videoSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
//            videoSurfaceHolder = videoSurfaceView.getHolder();
//            videoSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
//
//                @Override
//                public void surfaceCreated(SurfaceHolder holder) {
//                    videoSurface = holder.getSurface();
//                    try {
//                        videoPlayer.setSurface(videoSurface);
//
//                        Log.e("TAG", "setSurface");
//                    } catch (VideoPlayerException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                    videoSurface = holder.getSurface();
//                    try {
//                        videoPlayer.setSurface(videoSurface);
//                        videoPlayer.start();
//                        Log.e("TAG", "videoPlayer.start();");
//                    } catch (VideoPlayerException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void surfaceDestroyed(SurfaceHolder holder) {
//                    try {
//                        videoPlayer.setSurface(null);
//                    } catch (VideoPlayerException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//
//            videoPlayer = (RTSPPlayer) VideoPlayer.getPlayer(VideoPlayer.PlayerType.LIVE_STREAM);
//            videoPlayer.initializePlayer();
//            videoPlayer.setDataSource("rtsp://stream:video@192.168.99.1:554/media/stream2");
//
//            Log.e("TAG", "setDataSource");
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (VideoPlayerException e) {
//            e.printStackTrace();
//        }


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (active) {
                    if (rtspClient.play("rtsp://stream:video@192.168.99.1:554/media/stream2") == 0)
                        break;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(FullscreenActivity.this, "Connection error, retry after 3 s", Toast.LENGTH_SHORT).show();
                        }
                    });

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        isStoragePermissionGranted();
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first


    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public  void onStop() {
        super.onStop();
        active = false;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        rtspClient.stop();
        if (queue != null) {
            queue.cancelAll(TAG);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
//        rtspClient.stop();
//        rtspClient.dispose();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

    }

    protected void sendCommandToRobot(MeboCommand.CMD cmd, int value, int retries) {

        String url ="http://192.168.99.1/ajax/command.json?"
                + meboCommand.generate_single_command(1, cmd, value);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        textView.setText("Response is: "+ response);
                        try {
                            JSONObject jObject = new JSONObject(response);
                            String aJsonString = jObject.getString("response");
                            if(aJsonString.contains("ARM")) {
                                robot_state[0] = Integer.valueOf(aJsonString.substring(4));
                            } else if(aJsonString.contains("WRIST_UD")) {
                                robot_state[1] = Integer.valueOf(aJsonString.substring(9));
                            } else if(aJsonString.contains("WRIST_ROTATE")) {
                                robot_state[2] = Integer.valueOf(aJsonString.substring(13));
                            } else if(aJsonString.contains("CLAW")) {
                                initial_gripper_set = true;
                                robot_state[3] = Integer.valueOf(aJsonString.substring(5));
                                if(!initial_gripper_set) {
                                    ((SeekBar) findViewById(R.id.seekBar4)).setProgress(robot_state[3] );
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("That didn't work!");
            }
        });
        if(retries == 0)
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        stringRequest.setTag(TAG);
        queue.add(stringRequest);
    }

    class JointSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

//        boolean isTouching = false;
        int speed = 0;
//        SeekBar seekBar;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar.getId() == R.id.seekBar4)
                this.speed = (progress);
            else
                this.speed = (progress - 50)*2;
            setSpeed(seekBar, speed);

        }

        @Override
        public void onStartTrackingTouch(final SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (seekBar.getId() != R.id.seekBar4) {
                seekBar.setProgress(50);
                setSpeed(seekBar, 1);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                setSpeed(seekBar, 0);
            }

        }

        public void setSpeed(SeekBar seekBar, int speed) {
            if(seekBar.getId() == R.id.seekBar1) {
                robot_command[2] = speed;
            } else if (seekBar.getId() == R.id.seekBar2) {
                robot_command[3] = speed;
            } else if (seekBar.getId() == R.id.seekBar3) {
                robot_command[4] = speed;
            } else if (seekBar.getId() == R.id.seekBar4) {
                robot_command[5] = speed;
            }
        }
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    public static void zip(String[] files, String zipFile) throws IOException {
        int BUFFER_SIZE = 8192;
        BufferedInputStream origin = null;
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        try {
            byte data[] = new byte[BUFFER_SIZE];

            for (int i = 0; i < files.length; i++) {
                FileInputStream fi = new FileInputStream(files[i]);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                }
                finally {
                    origin.close();
                }
            }
        }
        finally {
            out.close();
        }
    }

    public void recordDemonstrationFrame() {
        String fname = "Image-" + frame_count + ".jpg";
        File file = new File(rootDir, fname);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            Bitmap resizedBMP = getResizedBitmap(bmp, img_width, img_height);
            resizedBMP.compress(Bitmap.CompressFormat.JPEG, 70, out);
            resizedBMP.recycle();
            out.flush();
            out.close();
            Log.e("TAG", "file saved. " + fname);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", e.getMessage());
        }
        Log.e("TAG", robot_command[0] + ", " + robot_command[1] + "----" + robot_state[0] + ", " + robot_state[1] + ", " + robot_state[2] + ", " + robot_state[3]);
    }



    public void dialogTable(ArrayList<TransactionDetails> list)
    {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Context dialogContext = builder.getContext();
        LayoutInflater inflater = LayoutInflater.from(dialogContext);
        View alertView = inflater.inflate(R.layout.table_dialog, null);
        builder.setView(alertView);
        TableLayout stk = (TableLayout)alertView.findViewById(R.id.table_main);
        TableRow tbrow0 = new TableRow(this);
        TextView tv0 = new TextView(this);
        tv0.setText(" Sl.No ");
        tv0.setTextColor(Color.WHITE);
        tv0.setBackground(getResources().getDrawable(R.drawable.cell_shape));
        tbrow0.addView(tv0);
        TextView tv1 = new TextView(this);
        tv1.setText(" Task description ");
        tv1.setTextColor(Color.WHITE);
        tv1.setBackground(getResources().getDrawable(R.drawable.cell_shape));
        tbrow0.addView(tv1);
        TextView tv2 = new TextView(this);
        tv2.setText(" Number of demonstrations ");
        tv2.setTextColor(Color.WHITE);
        tv2.setBackground(getResources().getDrawable(R.drawable.cell_shape));
        tbrow0.addView(tv2);
        TextView tv3 = new TextView(this);
        tv3.setText(" Demonstration duration ");
        tv3.setTextColor(Color.WHITE);
        tv3.setBackground(getResources().getDrawable(R.drawable.cell_shape));
        tbrow0.addView(tv3);

        stk.addView(tbrow0);
        for (int i = 0; i < list.size(); i++) {
            TransactionDetails details = list.get(i);
            TableRow tbrow = new TableRow(this);
            TextView t1v = new TextView(this);
            t1v.setText("" + (i+1));
            t1v.setTextColor(Color.WHITE);
            t1v.setGravity(Gravity.CENTER);
            tbrow.addView(t1v);
            TextView t2v = new TextView(this);
            t2v.setText(details.getTaskDescription());
            t2v.setTextColor(Color.WHITE);
            t2v.setGravity(Gravity.CENTER);
            tbrow.addView(t2v);
            TextView t3v = new TextView(this);
            t3v.setText(details.getDemonstrationsNo());
            t3v.setTextColor(Color.WHITE);
            t3v.setGravity(Gravity.CENTER);
            tbrow.addView(t3v);
            TextView t4v = new TextView(this);
            t4v.setText(details.getDemonstrationDuration());
            t4v.setTextColor(Color.WHITE);
            t4v.setGravity(Gravity.CENTER);
            tbrow.addView(t4v);
            stk.addView(tbrow);
        }

        builder.setCancelable(true);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }


}
