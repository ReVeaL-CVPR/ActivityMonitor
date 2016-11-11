package com.acbull.activitymonitor;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.util.List;

/**
 * Created by apple on 16/1/21.
 */

public class monitor {
    static public String network(Context cxt)
    {

        String result = "";
        ConnectivityManager mgrConn = (ConnectivityManager)cxt.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = mgrConn.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected())
            result = "OFF_NETWORK";
        else
        {
            String type = networkInfo.getTypeName();
            //Log.i("new", type);
            if (type.equalsIgnoreCase("WIFI")) {
                result = "WIFI";
            } else if (type.equalsIgnoreCase("MOBILE")) {
                String proxyHost = android.net.Proxy.getDefaultHost();
                result = TextUtils.isEmpty(proxyHost)
                        ? (isFastMobileNetwork(cxt) ? "NETWORK_3G" : "NETWORK_2G" ): "NETWORK_WAP";
            }
        }
        return result;
    }

    public static boolean is_existed(Context context)
    {
        Object service_name = "com.acbull.activitymonitor";
        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE) ;
        List<ActivityManager.RunningServiceInfo> Service_list = manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo info : Service_list){
            //Log.i("check", info.service.getPackageName());
            if (info.service.getPackageName().equals(service_name))
                return true;
        }
        return false;
    }

    public static boolean isFastMobileNetwork(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        switch (telephonyManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return false; // ~ 50-100 kbps
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return false; // ~ 14-64 kbps
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return false; // ~ 50-100 kbps
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return true; // ~ 400-1000 kbps
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return true; // ~ 600-1400 kbps
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return false; // ~ 100 kbps
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return true; // ~ 2-14 Mbps
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return true; // ~ 700-1700 kbps
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return true; // ~ 1-23 Mbps
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return true; // ~ 400-7000 kbps
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return true; // ~ 1-2 Mbps
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return true; // ~ 5 Mbps
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return true; // ~ 10-20 Mbps
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return false; // ~25 kbps
            case TelephonyManager.NETWORK_TYPE_LTE:
                return true; // ~ 10+ Mbps
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return false;
            default:
                return false;
        }
    }
}
