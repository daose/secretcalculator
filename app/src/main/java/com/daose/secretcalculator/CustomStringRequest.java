package com.daose.secretcalculator;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by student on 09/06/16.
 */
public class CustomStringRequest extends StringRequest {

    private HashMap<String, String> params;

    public CustomStringRequest(final int method, final String url, HashMap<String, String> params, Response.Listener<String> listener, Response.ErrorListener errorListener){
        super(method, url, listener, errorListener);
        this.params = params;
    }

    @Override
    protected HashMap<String, String> getParams(){
        return params;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        headers.put("ContentType", "application/x-www-form-urlencoded");
        return headers;
    }
}
