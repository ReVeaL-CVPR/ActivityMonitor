package com.acbull.activitymonitor;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class broadcast_receiver extends BroadcastReceiver {
    static ReadWriteLock rwl = new ReentrantReadWriteLock();
    String fileName = "log.txt";
    static String device_id = "";
    int cnt = 0;
    @Override
    public void onReceive(final Context context, Intent intent){
        try{
            // 创建 HttpParams 以用来设置 HTTP 参数（这一部分不是必需的）
            HttpParams params = new BasicHttpParams();
            // 设置连接超时和 Socket 超时，以及 Socket 缓存大小
            HttpConnectionParams.setLinger(params, 10000);
            HttpConnectionParams.setConnectionTimeout(params, 10000);
            HttpConnectionParams.setSoTimeout(params, 10000);
            HttpConnectionParams.setSocketBufferSize(params, 8192);
        }catch(Exception e){
            e.printStackTrace();
        }
        String strAction = intent.getAction();
        //Log.i("new", strAction);
        if (!monitor.is_existed(context)){
            Intent i = new Intent(context, myservice.class);
            i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.startService(i);
        }
        if (strAction.equals("android.intent.action.TIME_TICK")){
            cnt++;
            //Log.i("new", cnt + "");
            if (cnt == 60){
                cnt = 0;
                if (!monitor.network(context).equals("WIFI"))
                    return;
                new Thread() {
                    public void run() {
                        myservice.ip = myservice.GetNetIp();
                    }
                }.start();
                if (myservice.ip.equals(""))
                    return;
                new Thread() {
                    public void run() {
                        if (device_id == "")
                        {
                            TelephonyManager TelephonyMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                            device_id = TelephonyMgr.getDeviceId();
                        }
                        //Log.i("test", context.getFilesDir() + "/log.txt");
                        HttpClient myHttpClient = new DefaultHttpClient();
                        myHttpClient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
                        myHttpClient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT,10000);
                        try {
                            record_installed_app(context);
                            FileInputStream reader = context.openFileInput("log.txt");
                            //Log.i("new", "lock");
                            rwl.readLock().lock();
                            try {
                                Log.i("new", "upload");
                                HttpPost hp = new HttpPost("http://123.57.37.103:8080/apptrans/upload?deviceid=" + device_id);
                                hp.setEntity(new InputStreamEntity(reader, reader.available()));
                                HttpResponse response = myHttpClient.execute(hp);
                                HttpEntity entity = response.getEntity();
                                int len = entity.getContent().read();
                                if (len != 0){
                                    note(context);
                                }
                                new File(context.getFilesDir()+"/log.txt").delete();
                            } catch(Exception e) {
                                e.printStackTrace();
                            } finally {
                                reader.close();
                                rwl.readLock().unlock();
                                //Log.i("new", "unlock");
                                myHttpClient.getConnectionManager().shutdown();
                            }

                            Thread.sleep(5);
                            myHttpClient = new DefaultHttpClient();
                            myHttpClient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
                            myHttpClient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT,10000);


                            try {
                                Log.i("new", "upload");
                                HttpPost hp = new HttpPost("http://123.57.37.103:8080/apptrans/upload?deviceid=" + device_id);
                                File file = new File(Environment.getExternalStorageDirectory(), "log.txt");
                                reader = new FileInputStream(file);
                                hp.setEntity(new InputStreamEntity(reader, reader.available()));
                                myHttpClient.execute(hp);
                                file.delete();
                            } catch(Exception e) {
                                e.printStackTrace();
                            } finally {
                                reader.close();
                                //Log.i("new", "unlock");
                                myHttpClient.getConnectionManager().shutdown();
                            }
                        }catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
        if (strAction.equals("android.intent.action.SCREEN_ON"))
        {
            myservice.timer = 1000;
            new Thread(){
                public  void run(){
                    //Log.i("screenBR", "屏幕解锁：ACTION_SCREEN_ON触发");
                    try {
                        JSONObject object = new JSONObject();
                        object.put("Screen", "on");
                        Calendar c = Calendar.getInstance();
                        String buf = c.get(Calendar.MONTH) + "," + c.get(Calendar.DAY_OF_MONTH) + "," + c.get(Calendar.HOUR_OF_DAY)
                                + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);
                        object.put("Date", buf);
                        rwl.writeLock().lock();
                        FileOutputStream writer = context.openFileOutput(fileName, Context.MODE_APPEND + Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
                        writer.write((object.toString()+'\n').getBytes());
                        writer.flush();
                        writer.close();
                        rwl.writeLock().unlock();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    myservice.my_thread.interrupt();
                }
            }.start();
        }
        if (strAction.equals("android.intent.action.SCREEN_OFF"))
        {
            new Thread(){
                public  void run(){
                    //Log.i("screenBR", "屏幕锁屏：ACTION_SCREEN_OFF触发");
                    try {
                        JSONObject object = new JSONObject();
                        object.put("Screen", "off");
                        Calendar c = Calendar.getInstance();
                        String buf = c.get(Calendar.MONTH) + "," + c.get(Calendar.DAY_OF_MONTH) + "," + c.get(Calendar.HOUR_OF_DAY)
                                + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);
                        object.put("Date", buf);
                        rwl.writeLock().lock();
                        FileOutputStream writer = context.openFileOutput(fileName, Context.MODE_APPEND +
                                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
                        writer.write((object.toString() + '\n').getBytes());
                        writer.flush();
                        writer.close();
                        rwl.writeLock().unlock();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }.start();
            myservice.timer = 1000000000;
        }
        if (strAction.equals("android.intent.action.ACTION_POWER_CONNECTED"))
        {
            if (!monitor.network(context).equals("WIFI"))
                return;
            new Thread() {
                public void run() {
                    myservice.ip = myservice.GetNetIp();
                }
            }.start();
            if (myservice.ip.equals(""))
                return;
            new Thread() {
                public void run() {
                    if (device_id == "")
                    {
                        TelephonyManager TelephonyMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                        device_id = TelephonyMgr.getDeviceId();
                    }
                    //Log.i("test", context.getFilesDir()+"/log.txt");
                    HttpClient myHttpClient = new DefaultHttpClient();
                    myHttpClient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
                    myHttpClient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT,10000);
                    try {
                        FileInputStream reader = context.openFileInput("log.txt");

                        rwl.readLock().lock();
                        try {
                            Log.i("new", "upload");
                            HttpPost hp = new HttpPost("http://123.57.37.103:8080/apptrans/upload?deviceid=" + device_id);
                            hp.setEntity(new InputStreamEntity(reader, reader.available()));
                            HttpResponse response = myHttpClient.execute(hp);
                            HttpEntity entity = response.getEntity();
                            int len = entity.getContent().read();
                            if (len != 0){
                                note(context);
                            }
                            new File(context.getFilesDir()+"/log.txt").delete();

                            Thread.sleep(5);

                        } catch(Exception e) {
                            e.printStackTrace();
                        } finally {
                            reader.close();
                            rwl.readLock().unlock();
                            //Log.i("new", "unlock");
                            myHttpClient.getConnectionManager().shutdown();
                        }
                        Thread.sleep(5);
                        myHttpClient = new DefaultHttpClient();
                        myHttpClient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
                        myHttpClient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 10000);


                        try {
                            Log.i("new", "upload");
                            HttpPost hp = new HttpPost("http://123.57.37.103:8080/apptrans/upload?deviceid=" + device_id);
                            File file = new File(Environment.getExternalStorageDirectory(), "log.txt");
                            reader = new FileInputStream(file);
                            hp.setEntity(new InputStreamEntity(reader, reader.available()));
                            myHttpClient.execute(hp);
                            file.delete();
                        } catch(Exception e) {
                            e.printStackTrace();
                        } finally {
                            reader.close();
                            //Log.i("new", "unlock");
                            myHttpClient.getConnectionManager().shutdown();
                        }

                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }
    public void note(final Context context)
    {
        context.getMainLooper().prepare();
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("守航者通知")//设置对话框标题
                .setMessage("调研工作已经结束，感谢您的参与！您可通过卸载本程序停止相关服务。如果您方便，请点击下方确认按钮填写一个非常简单的调查问卷，以便我们能从更多方面进行研究。再次感谢您的支持~")//设置显示的内容
                .setPositiveButton("确定",new DialogInterface.OnClickListener() {//添加确定按钮
                    @Override
                    public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件
                        // TODO Auto-generated method stub
                        Intent intent = new Intent();
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setAction("android.intent.action.VIEW");
                        Uri content_url = Uri.parse("http://123.57.37.103:8080/apptrans/upload?deviceID=" + device_id);
                        intent.setData(content_url);
                        context.startActivity(intent);
                    }
                }).setNegativeButton("返回", null).create();//在按键响应事件中显示此对话框
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
        context.getMainLooper().loop();
    }

    public void record_installed_app(Context context){
        try {
            rwl.writeLock().lock();
            FileOutputStream writer = context.openFileOutput("log.txt", Context.MODE_APPEND +
                    Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
            JSONArray ja = new JSONArray();
            List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);
            for (int i = 0; i < packages.size(); i++) {
                PackageInfo packageInfo = packages.get(i);
                ja.put(packageInfo.packageName);
            }
            writer.write((ja.toString() + '\n').getBytes());
            writer.flush();
            writer.close();
            rwl.writeLock().unlock();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
