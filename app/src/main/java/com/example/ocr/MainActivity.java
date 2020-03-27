package com.example.ocr;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG ="MainActivity" ;
    private static final int requestPermissionID=101;
    SurfaceView mCameraView;
    TextView mTextView;
    CameraSource mCameraSource;
    int flag=0;
    int phone_start,phone_end;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView=findViewById(R.id.surfaceView);
        mTextView=findViewById(R.id.textView);

        startCameraSource();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults)
    {
        if(requestCode!=requestPermissionID)
        {
            Log.d(TAG,"Got unexpected permission result: "+requestCode);
            super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        }
        if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
        {
            try
            {
                if(ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
                {
                    return;
                }
                mCameraSource.start(mCameraView.getHolder());

            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

    }
    public void insert_contacts(String phone_no)
    {
        Intent intent=new Intent(ContactsContract.Intents.Insert.ACTION);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE,phone_no);
        startActivity(intent);
        finish();

    }
    public  boolean is_valid_no(String phone_no)
    {
        int count=0;
        for(int i=0;i<phone_no.length();i++)
        {
            if(phone_no.charAt(i)>='0' && phone_no.charAt(i)<='9')
            {
                if(count==0)
                    phone_start=i;
                count++;
                phone_end=i;
            }

        }
        if(count==10 || count==11)
            return true;

            //if(phone_no.indexOf("+91")!=-1)

            return false;

            //Double.parseDouble(phone_no);




    }

    private void startCameraSource()
    {
        final AlertDialog.Builder dlgAlert=new AlertDialog.Builder(this);
        final TextRecognizer textRecognizer=new TextRecognizer.Builder(getApplicationContext()).build();

        if(!textRecognizer.isOperational())
        {
            Log.w(TAG,"Detector dependencies not loaded yet.");
        }
        else
        {
            //initialise camera source to use high quality and set Autofocus on
            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280,1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(4.0f)
                    .build();

            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try
                    {
                        if(ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
                        {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }
                        mCameraSource.start(mCameraView.getHolder());


                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }

                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {

                    final SparseArray<TextBlock> items=detections.getDetectedItems();

                    if(items.size()!=0 && flag==0)
                    {
                        mTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder=new StringBuilder();
                                String new_string="";
                                String temp_string="";

                                for(int i=0;i<items.size();i++)
                                {
                                    TextBlock item=items.valueAt(i);


                                    stringBuilder.append(item.getValue());
                                    temp_string=item.getValue();
                                    new_string=temp_string;
                                    /*if(temp_string.indexOf("Mobile")!=-1)
                                    {
                                        int index=temp_string.indexOf("Mobile:");
                                        int j=index+7;
                                        while(j<temp_string.length()) {
                                            if (Character.isLetter(temp_string.charAt(j)))
                                                break;
                                            j++;
                                        }
                                            new_string=item.getValue().toString().substring(index+7,j);

                                    }*/

                                    stringBuilder.append("\n");

                                }
                                mTextView.setText("" + stringBuilder.toString());
                                if(new_string!="") {
                                    if ((is_valid_no(new_string) == true)) {

                                        flag = 1;
                                        int phone_pos=phone_start;
                                        //Toast.makeText(getApplicationContext(),"phone pos:"+phone_pos+" phone end: "+phone_end,Toast.LENGTH_LONG).show();
                                        final String phone_no=new_string.substring(phone_pos,phone_end+1);
                                        dlgAlert.setMessage("Do you want to add \n" + phone_no + " to your contacts?");
                                        dlgAlert.setTitle("Success");
                                        dlgAlert.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                insert_contacts(phone_no);
                                            }
                                        });
                                        dlgAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                flag = 0;
                                            }
                                        });
                                        dlgAlert.setCancelable(true);
                                        dlgAlert.create().show();


                                    }
                                }


                            }
                        });
                    }

                }
            });



        }
    }
}
