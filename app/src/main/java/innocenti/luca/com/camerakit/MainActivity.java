package innocenti.luca.com.camerakit;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.support.annotation.FractionRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SizeF;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.google.android.gms.location.DetectedActivity;
import com.wonderkiln.camerakit.CameraView;


import java.awt.font.NumericShaper;
import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.nlopez.smartlocation.OnActivityUpdatedListener;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;

import static io.nlopez.smartlocation.SmartLocation.with;


public class MainActivity extends AppCompatActivity implements SensorEventListener, OnLocationUpdatedListener, OnActivityUpdatedListener {

    @BindView(R.id.camera)
    CameraView camera;
    @BindView(R.id.btn_takepicture)
    Button takepicture;
    @BindView(R.id.angolotxt)
    EditText angolo;

    private SensorManager mSensorManager;

    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    private double az;
    private double pi;
    private double ro;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];


    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];
    private int stato;
    private double az_0;
    private double distanza;
    private double pimezzo = Math.PI / 2;
    private double altezza_occhi = 1.65;
    private double alpha = 0.5;
    private double altezza;
    private double altezza_filtrata;

    private static final int LOCATION_PERMISSION_ID = 1001;
    private LocationGooglePlayServicesProvider provider;
    private double latitude;
    private double longitude;
    private NumberPicker alt_occhi;
    private SmartLocation smartLocation;
    private int orientazione;
    private ToggleButton xy;
    private boolean xyb;
    private double hfov;
    private double vfov;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        alt_occhi = (NumberPicker) findViewById(R.id.numberPicker);

        // FOV
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            for (String cameraId : manager.getCameraIdList()) {
                try {
                    CameraCharacteristics info = manager.getCameraCharacteristics(cameraId);
                    if (info.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        SizeF sensorSize = info.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                        float[] focalLengths = info.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                        if (focalLengths != null && focalLengths.length > 0) {
                            hfov = (float) (2.0f * Math.atan(sensorSize.getWidth() / (2.0f * focalLengths[0])));
                            vfov = (float) (2.0f * Math.atan(sensorSize.getHeight() / (2.0f * focalLengths[0])));
                            Log.d("FOV", "HFOV "+Double.toString(Math.toDegrees(hfov))+"VFOV "+Double.toString(Math.toDegrees(vfov)));
                        }
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                // Do something with the characteristics
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


        // ----------------------

        xy = (ToggleButton) findViewById(R.id.toggleButton);
        xy.setTextOff("X");
        xy.setTextOn("Y");
        xy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked)
                {
                    az_0=az;
                    xyb = true;
                }
                else
                {
                    xyb = false;
                }

            }
        });

        xy.setEnabled(false);



        ButterKnife.bind(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);



        stato = 0;
        alt_occhi.setMaxValue(230);
        alt_occhi.setMinValue(120);
        alt_occhi.setValue(165);
        alt_occhi.setWrapSelectorWheel(true);
        alt_occhi.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                Log.d("picker ", Integer.toString(i1));
                altezza_occhi = i1/100.0;
                Log.d("occhi", Double.toString(altezza_occhi));
            }
        });


        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_ID);
            return;
        }
        startLocation();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_ID && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocation();
        }
    }





    @OnClick(R.id.btn_takepicture) void clicca()
    {
        switch (stato)
        {
            case 0:
                orientazione = getResources().getConfiguration().orientation;
                if (orientazione == 2) {
                    stato = 1;
                    az_0 = az;
                    distanza = Math.abs(altezza_occhi / Math.tan(pimezzo + ro));
                    takepicture.setText("Reset");
                    xy.setEnabled(true);
                    Log.d("Misura", Double.toString(distanza));
                    Log.d("Az_0", Double.toString(Math.toDegrees(az)));
                    break;
                }
                else
                {
                    Toast toast = Toast.makeText(this, "Put the phone in landascape mode", Toast.LENGTH_LONG);
                    toast.show();
                }
            case 1:
                stato = 0;
                takepicture.setText(R.string.take_picture);
                angolo.setText("");
                xy.setEnabled(false);
                xy.setChecked(false);
                break;
        }


        if (stato == 1)
        {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);

        camera.start();
    }

    @Override
    protected void onPause() {
        camera.stop();
        angolo.setText("");
        stato = 0;
        xy.setEnabled(false);
        xy.setChecked(false);
        takepicture.setText(R.string.take_picture);
        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        stopLocation();
        super.onStop();
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
        }

        if  (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mMagnetometerReading, 0, mMagnetometerReading.length);
        }
        updateOrientationAngles();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public void updateOrientationAngles() {
        mSensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagnetometerReading);
        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        az = (mOrientationAngles[0]+(2*Math.PI))%(2*Math.PI);
        pi = mOrientationAngles[1];
        ro = mOrientationAngles[2];

        //Log.d("bussola", Double.toString(Math.toDegrees(az)));

        DecimalFormat df_fov = new DecimalFormat("#.#");
        DecimalFormat df2 = new DecimalFormat("#.##");
        DecimalFormat dfcoord = new DecimalFormat(".#######");


        //int inclination = (int) Math.round(Math.toDegrees(Math.acos(mRotationMatrix[8])));

        if (stato > 0)
        {
            Log.d("TTT",Double.toString(Math.toDegrees(ro))+ " H "+Double.toString(distanza * Math.tan(pimezzo + ro)));
            if (Math.abs(Math.toDegrees(ro)) < -90) {
                altezza = altezza_occhi + distanza * Math.tan(pimezzo + ro);
            }
            else
            {
                altezza = altezza_occhi -  distanza * Math.tan(pimezzo + ro);
            }

            //Log.d("angolo", Double.toString(Math.toDegrees(pimezzo+ro)));
            Log.d("altezza", Double.toString(altezza));
            altezza_filtrata = (alpha * altezza_filtrata) + ((1.0  - alpha)*altezza);

            String altezza_str = df2.format(altezza_filtrata);
            String distanza_str = df2.format(distanza);
            String hfov_str = df_fov.format(2* distanza * Math.tan(hfov/2));
            String vfov_str = df_fov.format(2* distanza * Math.tan(vfov/2));

            double x = 0.0;
            // calcolo di X
            if ((az-az_0) > Math.PI)
            {
                if (az > az_0)
                {
                    az_0 = (az_0+(2*Math.PI));
                }
                else
                {
                    az = (az+(2*Math.PI));
                }

            }

            double delta = Math.abs(az_0-az);
            x = distanza*Math.tan(delta);

            Double facing = new Double(Math.toDegrees((az + (Math.PI/2)) % (2 * Math.PI)));
            int facing_i = facing.intValue();


            Log.d("X ", "AZ = " +Double.toString(Math.toDegrees(az))+" Az_0"+ Double.toString((Math.toDegrees(az_0)))+" Diff" + Double.toString(Math.toDegrees(az-az_0)));

            String x_str = df2.format(x);
            String lat_str = dfcoord.format(latitude);
            String lng_str = dfcoord.format(longitude);

            if (xyb == false)
            {
                angolo.setText("Dist. : "+ distanza_str +" m \nHFOV : "+ hfov_str+  "m  VFOV : "+ vfov_str+  "m\nH :" + altezza_str+ " m\n Azimuth : "+ Integer.toString(facing_i) + "\n"+lat_str+"/"+lng_str);

            }
            else
            {
                angolo.setText("Dist. : "+ distanza_str +" m \nX :"+ x_str + " m" + "\n Azimuth : "+ Integer.toString(facing_i) + "\n"+lat_str+"/"+lng_str);

            }

        }

    }


    private void startLocation() {

        provider = new LocationGooglePlayServicesProvider();
        provider.setCheckLocationSettings(true);

        smartLocation = new SmartLocation.Builder(this).logging(true).build();

        smartLocation.location(provider).start((OnLocationUpdatedListener) this);
        smartLocation.activity().start(this);

    }

    private void stopLocation() {
        with(this).location().stop();
        with(this).activity().stop();

        //locationText.setText("Location stopped!");

    }

    private void showLocation(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
    }

    @Override
    public void onLocationUpdated(Location location) {
        showLocation(location);

    }

    @Override
    public void onActivityUpdated(DetectedActivity detectedActivity) {

    }
}
