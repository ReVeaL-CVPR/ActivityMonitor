package com.acbull.activitymonitor;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.util.Log;
import android.widget.TextView;
public class MainActivity extends AppCompatActivity {

    public static final String tag = "main";
    private TextView pro, header;
    private Button agree,disagree;
    private Intent service_intent;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    protected void onCreate(Bundle savedInstanceState) {
        Log.w(tag, "activity create");
        super.onCreate(savedInstanceState);
        if (monitor.is_existed(getApplicationContext()))
        {
            setContentView(R.layout.done);
            TelephonyManager TelephonyMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            String device_id = TelephonyMgr.getDeviceId();
            header = (TextView) findViewById(R.id.hea);
            header.setText(device_id+"\n已开启\n谢谢参与（*＾-＾*）");
            return;
        }
        setContentView(R.layout.activity_main);
        agree = (Button) findViewById(R.id.agree);
        disagree = (Button) findViewById(R.id.disagree);
        header = (TextView) findViewById(R.id.hea);
        pro = (TextView) findViewById(R.id.pro);

        agree.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!monitor.is_existed(getApplicationContext())) {
                            service_intent = new Intent(MainActivity.this, myservice.class);
                            service_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                            startService(service_intent);
                        }
                        TelephonyManager TelephonyMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                        String device_id = TelephonyMgr.getDeviceId();
                        header.setText(device_id+"\n已开启\n谢谢参与（*＾-＾*）");
                        agree.setVisibility(View.INVISIBLE);
                        disagree.setVisibility(View.INVISIBLE);
                        pro.setVisibility(View.INVISIBLE);
                    }
                }
        );
        disagree.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getApplicationContext(), SubActivity.class);
                        i.putExtra("food", monitor.class);
                        i.setData(Uri.parse("tel:18810999022"));
                        people mPerson = new people();
                        mPerson.setName("frankie");
                        mPerson.setAge(25);
                        Bundle mBundle = new Bundle();
                        mBundle.putSerializable("com.acbull.activitymonitor.ser",mPerson);
                        i.putExtras(mBundle);
                        startActivity(i);
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(tag, "destroy");
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
