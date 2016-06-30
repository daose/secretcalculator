package com.daose.secretcalculator;

import android.content.Context;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

/**
 * Created by student on 09/06/16.
 */
public class HttpRequest {

    private static HttpRequest instance = null;
    private static Context context = null;

    private static RequestQueue reqQ;

    private static final String TAG = "daose";

    private HttpRequest(Context context){
        this.context = context;
        reqQ = getRequestQueue();
    }

    public static HttpRequest getInstance(Context context){
        if(instance == null){
            instance = new HttpRequest(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue(){
        if(reqQ == null){
            Cache cache = new DiskBasedCache(context.getCacheDir(), 10*1024*1024);
            Network network = new BasicNetwork(new HurlStack());
            reqQ = new RequestQueue(cache, network);
            reqQ.start();
        }
        return reqQ;
    }

    public <T> void add(Request<T> request){
        request.setTag(TAG);
        getRequestQueue().add(request);
    }
}
