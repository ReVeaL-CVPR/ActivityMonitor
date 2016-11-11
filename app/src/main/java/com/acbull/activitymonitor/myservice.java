package com.acbull.activitymonitor;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Debug;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.acbull.activitymonitor.utils.ShellUtils;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class myservice extends Service{
    static ReadWriteLock rwl = new ReentrantReadWriteLock();
    static long timer = 1000;
    static Thread my_thread;
    static String ip = "", oldip = "";
    String net = "", oldnet = "";
    int battery = 0;
    public static final String tag = "service";
    public boolean flag = true;
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            //判断它是否是为电量变化的Broadcast Action
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                //获取当前电量
                battery = intent.getIntExtra("level", 0);
                //Log.i("new", battery+"");
            }
        }
    };
    @Override
    public IBinder onBind(Intent intent){
       //Log.w(tag, "service bind");
        return null;
    }
    @Override
    public void onCreate(){
        super.onCreate();
        try{
            // 创建 HttpParams 以用来设置 HTTP 参数（这一部分不是必需的）
            HttpParams params = new BasicHttpParams();
            // 设置连接超时和 Socket 超时，以及 Socket 缓存大小
            HttpConnectionParams.setLinger(params, 5 * 1000);
            HttpConnectionParams.setConnectionTimeout(params, 5 * 1000);
            HttpConnectionParams.setSoTimeout(params, 5 * 1000);
            HttpConnectionParams.setSocketBufferSize(params, 8192);
        }catch(Exception e){
            e.printStackTrace();
        }
       //Log.i(tag, "service create");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);



        broadcast_receiver receiver = new broadcast_receiver();
        getApplicationContext().registerReceiver(receiver, filter);
        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        IntentFilter intentFilter;
        intentFilter = new IntentFilter();
        //addAction
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        NetworkChangeReceive re= new NetworkChangeReceive();
        registerReceiver(re, intentFilter);
        Notification notification = new Notification();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        startForeground(1120, notification);
       //Log.i(tag, "receiver create");
    }

    public class routine implements Runnable{
        @Override
        public void run()
        {
            int cnt = 0;
            String old = "";
            while(flag) {
                cnt += 1;
                try {
                    Thread.sleep(timer);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String buf = getTopActivity(getApplicationContext());
                if (cnt % 10 == 9) {
                    cnt = 0;
                    getLog();
                    JSONObject jo = getBackActivity(getApplicationContext());
                    Log.i("test", jo.toString());
                    new WThread(jo).start();

                }
                if (old.equals(""))
                    old = buf;
                else if (!old.equals(buf)) {
                    old = buf;
                    try {
                        JSONObject jo = new JSONObject();
                        jo.put("activity", buf);
                        new WThread(jo).start();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int id){
       //Log.i("test", "service start command:" + this);
        my_thread = new Thread(new routine());
        my_thread.start();
        return START_STICKY;
    }
    @Override
    public void onDestroy(){
        flag = false;
       //Log.w(tag, "destroy");
        super.onDestroy();
    }

    JSONObject getBackActivity(Context context)
    {
        JSONObject jo = new JSONObject();

        String device_model = Build.MODEL; // 设备型号
        String version_sdk = Build.VERSION.SDK; // 设备SDK版本
        String version_release = Build.VERSION.RELEASE; // 设备的系统版本
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        try {
            jo.put("device_model",device_model);
            jo.put("version_sdk",version_sdk);
            jo.put("version_release",version_release);
            HashMap<Integer, String> uid_map = new HashMap<>();
            HashMap<Integer, String> pid_map = new HashMap<>();
            JSONArray task = new JSONArray();
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = manager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                JSONObject jj = new JSONObject();
                jj.put("package", processInfo.pkgList[0]);
                jj.put("process", processInfo.processName);
                jj.put("importance", String.format("%d/%d", processInfo.importance, processInfo.importanceReasonCode));
                task.put(jj);
                uid_map.put(processInfo.uid, processInfo.pkgList[0]);
                pid_map.put(processInfo.pid, processInfo.pkgList[0]);
               //Log.i("liuyi", processInfo.pkgList[0]);
            }
            jo.put("background_task", task);
            JSONArray service = new JSONArray();
            List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(100);
            Iterator<ActivityManager.RunningServiceInfo> l = services.iterator();
            while (l.hasNext()) {
                ActivityManager.RunningServiceInfo si = l.next();
                String tmp = si.service.toString();
                tmp = tmp.substring(14, tmp.length() - 1);
                service.put(tmp);
                int id = si.uid;
                if (!uid_map.containsKey(id)){
                    uid_map.put(id, tmp);
                }
                id = si.pid;
                if (!pid_map.containsKey(id)){
                    pid_map.put(id, tmp);
                }
               //Log.i("liuyi", tmp);
            }
            jo.put("background_service", service);

            Set<Integer> set = pid_map.keySet();
            int[] pid_list = new int[set.size()];
            int cnt = 0;
            for (int i : set)
                pid_list[cnt++] = i;
           //Log.i("liuyi", "memory" + pid_list.length);
            JSONArray memory = new JSONArray();
            cnt = 0;
            Debug.MemoryInfo[] infos = manager.getProcessMemoryInfo(pid_list);
            for (Debug.MemoryInfo info : infos){
                int total = info.getTotalPss();
                if (total != 0)
                {
                    JSONObject jj = new JSONObject();
                    jj.put("name", pid_map.get(pid_list[cnt]));
                    jj.put("total", total);
                    jj.put("dalvik", info.dalvikPss);
                    jj.put("native", info.nativePss);
                    jj.put("sharedDirty", info.getTotalSharedDirty());
                    jj.put("privateDirty", info.getTotalPrivateDirty());
                    memory.put(jj);
                }
            }

            jo.put("memory", memory);
           //Log.i("liuyi", "network" + uid_map.size());
            JSONArray network = new JSONArray();
            for (int i : uid_map.keySet()){
                long b = TrafficStats.getUidTxBytes(i);
                if (b != 0){
                    JSONObject jj = new JSONObject();
                    jj.put("name",  uid_map.get(i));
                    jj.put("RxBytes", b);
                    jj.put("TxBytes", TrafficStats.getUidTxBytes(i));
                    jj.put("RxPackets", TrafficStats.getUidRxPackets(i));
                    jj.put("TxPackets", TrafficStats.getUidTxPackets(i));
                    network.put(jj);
                }
            }
            jo.put("network", network);

        }catch (Exception e){
            e.printStackTrace();
        }

        return jo;
    }

    String getTopActivity(Context context)
    {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH) {
            ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
            if (runningTaskInfos != null) {
                String tmp = (runningTaskInfos.get(0).topActivity).toString();
                return tmp.substring(14, tmp.length() - 1);
            }
            else
                return "null";
        }
        else
        {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return processInfo.pkgList[0];
                }
            }
            return "null";
        }
    }

    class WThread extends Thread {
        String buf;
        JSONObject object;
        WThread(JSONObject jo)
        {
            object = jo;
            //Log.i("new", buf);
        }
        public void run ()
        {
            try {
                object.put("Activity", buf);
                object.put("Battery", battery);
                Calendar c = Calendar.getInstance();
                buf = c.get(Calendar.MONTH) + "," + c.get(Calendar.DAY_OF_MONTH) + "," + c.get(Calendar.HOUR_OF_DAY)
                        + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);
                object.put("Date", buf);
                if (net.equals("")) {
                    net = monitor.network(getApplicationContext());
                }
                object.put("Net_type", net);
                if (net.equals(oldnet) && !oldip.equals(""))
                    ip = oldip;
                else if (!net.equals("OFF_NETWORK"))
                    oldip = ip = GetNetIp();
                else
                    oldip = ip = "";
                oldnet = net;
                object.put("Net_ip", ip);
                //Log.i("new", object.toString());
                rwl.writeLock().lock();
                FileOutputStream writer = openFileOutput("log.txt", Context.MODE_APPEND +
                        Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
                writer.write((object.toString()+'\n').getBytes());
                writer.flush();
                writer.close();
                rwl.writeLock().unlock();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    class NetworkChangeReceive extends BroadcastReceiver {
        @Override
        public void onReceive(Context cxt, Intent intent) {
            //Log.i("new", "update");
            ConnectivityManager mgrConn = (ConnectivityManager)cxt.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = mgrConn.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected()) {
                oldnet = net = "OFF_NETWORK";
                oldip = ip = "";
                return;
            }
            else
            {
                String type = networkInfo.getTypeName();
                //Log.i("new", type);
                if (type.equalsIgnoreCase("WIFI")) {
                    net = "WIFI";
                } else if (type.equalsIgnoreCase("MOBILE")) {
                    String proxyHost = android.net.Proxy.getDefaultHost();
                    net = TextUtils.isEmpty(proxyHost)
                            ? (monitor.isFastMobileNetwork(cxt) ? "NETWORK_3G" : "NETWORK_2G" ): "NETWORK_WAP";
                }
            }
            new Thread() {
                public void run() {
                    try {
                        oldip = ip = GetNetIp();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }
    static public String GetNetIp() {
        URL infoUrl = null;
        InputStream inStream = null;
        String ipLine = "";
        HttpURLConnection httpConnection = null;
        try {
            infoUrl = new URL("http://1212.ip138.com/ic.asp");
            URLConnection connection = infoUrl.openConnection();
            connection.setReadTimeout(3000);
            connection.setConnectTimeout(3000);
            httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inStream = httpConnection.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inStream, "GBK"));
                StringBuilder strber = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null)
                    strber.append(line + "\n");
                int start = strber.indexOf("<center>");
                int end = strber.indexOf("</center>", start);
                //Log.i("new", start + " " + end);
                ipLine = strber.substring(start + 14, end);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                inStream.close();
                httpConnection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ipLine;
    }

    public static void getLog()
    {
        System.out.println("--------func start--------"); // 方法启动
        try
        {
            ArrayList<String> cmdLine=new ArrayList<>();   //设置命令   logcat -d 读取日志
            cmdLine.add("logcat -d -v time ActivityManager:I *:S >> /sdcard/log.txt");
            cmdLine.add("logcat -c");
            ShellUtils.execCommand(cmdLine, true);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("--------func end--------");
    }
}


