package com.daose.secretcalculator;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.daose.secretcalculator.Knurld;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by student on 09/06/16.
 */
public class CustomJsonRequest extends JsonObjectRequest {

    private boolean useMasterDeveloperId = false;

    public CustomJsonRequest(int method, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
        //Log.d("CustomJsonRequest", jsonRequest.toString());
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError{
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", Knurld.getAuthToken());
        if(useMasterDeveloperId){
            headers.put("Developer-Id", Knurld.getMasterDeveloperId());
        } else {
            headers.put("Developer-Id", Knurld.getDeveloperId());
        }
        return headers;
    }

    public void setUseMasterDeveloperId(boolean bool){
        useMasterDeveloperId = bool;
    }



    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response){
        try{
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e){
            return Response.error(new ParseError(e));
        } catch (JSONException je){
            return Response.error(new ParseError(je));
        }
    }

    @Override
    protected VolleyError parseNetworkError(VolleyError volleyError){
        if(volleyError.networkResponse != null && volleyError.networkResponse.data != null){
            VolleyError error = new VolleyError(new String(volleyError.networkResponse.data));
            volleyError = error;
        }
        return volleyError;
    }

}
