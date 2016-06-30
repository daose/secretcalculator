package com.daose.secretcalculator;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by student on 29/06/16.
 */
public class Knurld {
    private KnurldListener listener;

    private static String authToken = "";
    private static String consumerUrl = "";
    private static String enrollmentUrl = "";
    private static String verificationUrl = "";
    private static final String LOG_TAG = "Knurld";
    private static final String TOKEN_URL = "https://api.knurld.io/oauth/client_credential/accesstoken?grant_type=client_credentials";
    private static final String CREATE_CONSUMER_URL = "https://api.knurld.io/v1/consumers";
    private static final String CREATE_ENROLLMENT_URL = "https://api.knurld.io/v1/enrollments";
    private static final String CREATE_VERIFICATION_URL = "https://api.knurld.io/v1/verifications";
    private static final String AUTHENTICATE_CONSUMER_URL = "https://api.knurld.io/v1/consumers/token";
    private static final String APP_MODEL_URL = "https://api.knurld.io/v1/app-models/ecd1003f382e5a3f544d2f1dcf2ed5f9";
    private static String developerId = "";
    private static String masterDeveloperId = "";
    private Context context;
    private JSONObject intervalJson;
    private String downloadUrl;

    public Knurld(Context context, JSONObject intervalJson) {
        this.context = context;
        this.intervalJson = intervalJson;
        SharedPreferences pref = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        consumerUrl = pref.getString("consumerUrl", "NOTAURL");
        developerId = "Bearer: " + context.getResources().getString(R.string.developer_id);
        masterDeveloperId = developerId;
    }

    public Knurld(Context context){
        this.context = context;
        developerId = "Bearer: " + context.getResources().getString(R.string.developer_id);
        masterDeveloperId = developerId;
        SharedPreferences pref = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        consumerUrl = pref.getString("consumerUrl", "NOTAURL");
    }

    public void setIntervalJson(JSONObject intervalJson){
        this.intervalJson = intervalJson;
    }

    public void getVocab(){
        requestAuthToken(false, true);
    }

    public void setKnurldListener(KnurldListener listener) {
        this.listener = listener;
    }

    public static String getMasterDeveloperId(){
        return masterDeveloperId;
    }

    public void enrollUser(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        requestAuthToken(true, false);
    }

    public void verifyUser(String downloadUrl){
        this.downloadUrl = downloadUrl;
        populateVerification();
        //requestAuthToken(false, false);
    }

    private void populateEnrollment(){
        JSONObject intervalJson = this.intervalJson;
        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response){
                try{
                    enrollmentUrl = response.getString("href");
                    Log.d(LOG_TAG, "enrollmentUrl: " + enrollmentUrl);
                    success();
                } catch (JSONException e){
                    Log.e(LOG_TAG, "createEnrollment error: " + e.getMessage());
                    enrollmentUrl = "";
                    failure();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "populateEnrollment error: " + error.getMessage());
                try {
                    JSONObject errorJson = new JSONObject(error.getMessage());
                    if(errorJson.getString("message").equals("Audio has already been posted")){
                        success();
                    } else {
                        failure();
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                    failure();
                }
            }
        };

        JSONObject enrollmentJson = new JSONObject();
        try{
            enrollmentJson.accumulate("enrollment.wav", this.downloadUrl);
            enrollmentJson.accumulate("intervals", intervalJson.getJSONArray("intervals"));
        } catch (JSONException e){
            Log.e(LOG_TAG, "unable to load enrollmentJson: " + e.getMessage());
            failure();
        }

        CustomJsonRequest enrollmentRequest = new CustomJsonRequest(Request.Method.POST, Knurld.enrollmentUrl, enrollmentJson, listener, errorListener);
        HttpRequest.getInstance(context).add(enrollmentRequest);
    }

    private void createEnrollment(){
        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response){
                try{
                    enrollmentUrl = response.getString("href");
                    Log.d(LOG_TAG, "enrollmentUrl: " + enrollmentUrl);
                    //updateEnrollmentStatus(context);
                    populateEnrollment();
                } catch (JSONException e){
                    Log.e(LOG_TAG, "createEnrollment error: " + e.getMessage());
                    enrollmentUrl = "";
                    failure();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "createEnrollment error: " + error.getMessage());
                enrollmentUrl = "";
                failure();
            }
        };

        JSONObject createEnrollmentJson = new JSONObject();
        try{
            createEnrollmentJson.accumulate("consumer", Knurld.consumerUrl);
            createEnrollmentJson.accumulate("application", Knurld.APP_MODEL_URL);
        } catch (JSONException e){
            Log.e(LOG_TAG, "unable to load createEnrollmentJson: " + e.getMessage());
            failure();
        }

        CustomJsonRequest createEnrollmentRequest = new CustomJsonRequest(Request.Method.POST, Knurld.CREATE_ENROLLMENT_URL, createEnrollmentJson, listener, errorListener);
        HttpRequest.getInstance(context).add(createEnrollmentRequest);
    }

    private void getVerificationStatus(final boolean getVocabOnly){
        Log.d(LOG_TAG, "getVerificationObject");
        Response.Listener<JSONObject> rl = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(getVocabOnly){
                        Log.d(LOG_TAG, "populating vocab array");
                        JSONArray jsonArray = response.getJSONObject("instructions").getJSONObject("data").getJSONArray("phrases");
                        int length = jsonArray.length();
                        String[] vocab = new String[length];
                        if(jsonArray != null){
                            for(int i = 0; i < length; i++){
                                vocab[i] = jsonArray.getString(i);
                            }
                            listener.vocabReceived(vocab);
                        }
                    } else {
                        Log.d(LOG_TAG, "Status: " + response.getString("status"));
                        String status = response.getString("status");
                        if ("completed".equals(status)) {
                            if (response.getBoolean("verified")) {
                                success();
                            } else {
                                failure();
                            }
                        } else if ("failed".equals(status)) {
                            failure();
                            return;
                        }
                        getVerificationStatus(false);
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        };

        Response.ErrorListener el = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, error.getMessage());
                failure();
            }
        };

        JSONObject parameters = new JSONObject();
        CustomJsonRequest verificationRequest = new CustomJsonRequest(Request.Method.GET, Knurld.verificationUrl, parameters, rl, el);
        HttpRequest.getInstance(context).add(verificationRequest);
    }

    private void populateVerification(){
        JSONObject intervalJson = this.intervalJson;
        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response){
                try{
                    verificationUrl = response.getString("href");
                    Log.d(LOG_TAG, "verificationUrl: " + verificationUrl);
                    getVerificationStatus(false);
                } catch (JSONException e){
                    Log.e(LOG_TAG, "populateVerification error: " + e.getMessage());
                    verificationUrl = "";
                    failure();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "populateVerification error: " + error.getMessage());
                try {
                    JSONObject errorJson = new JSONObject(error.getMessage());
                    if(errorJson.getString("message").equals("Audio has already been posted")){
                        success();
                    } else {
                        failure();
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                    failure();
                }
            }
        };

        JSONObject verificationJson = new JSONObject();
        try{
            verificationJson.accumulate("verification.wav", this.downloadUrl);
            verificationJson.accumulate("intervals", intervalJson.getJSONArray("intervals"));
        } catch (JSONException e){
            Log.e(LOG_TAG, "unable to load verificationJson: " + e.getMessage());
            failure();
        }

        CustomJsonRequest verificationRequest = new CustomJsonRequest(Request.Method.POST, Knurld.verificationUrl, verificationJson, listener, errorListener);
        HttpRequest.getInstance(context).add(verificationRequest);
    }

    private void createVerification(final boolean getVocabOnly){
        Log.d(LOG_TAG, "createVerification");
        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response){
                try{
                    verificationUrl = response.getString("href");
                    Log.d(LOG_TAG, "verificationUrl: " + verificationUrl);
                    //updateEnrollmentStatus(context);
                    if(getVocabOnly){
                        getVerificationStatus(true);
                    } else {
                        populateVerification();
                    }
                } catch (JSONException e){
                    Log.e(LOG_TAG, "createVerification error: " + e.getMessage());
                    verificationUrl = "";
                    failure();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "createVerification error: " + error.getMessage());
                verificationUrl = "";
                failure();
            }
        };

        JSONObject createVerificationJson = new JSONObject();
        try{
            createVerificationJson.accumulate("consumer", Knurld.consumerUrl);
            createVerificationJson.accumulate("application", Knurld.APP_MODEL_URL);
        } catch (JSONException e){
            Log.e(LOG_TAG, "unable to load createVerificationJson: " + e.getMessage());
            failure();
        }

        CustomJsonRequest createVerificationRequest = new CustomJsonRequest(Request.Method.POST, Knurld.CREATE_VERIFICATION_URL, createVerificationJson, listener, errorListener);
        createVerificationRequest.setUseMasterDeveloperId(true);
        HttpRequest.getInstance(context).add(createVerificationRequest);
    }

    private void enrollConsumer(){
        createEnrollment();
    }

    private void verifyConsumer(){
        createVerification(false);
    }

    private void authenticateConsumer(final boolean isNewUser){
        Log.d(LOG_TAG, "authenticateConsumer");
        SharedPreferences pref = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        if(!pref.getBoolean("isConsumerCreated", false)){
            Log.d(LOG_TAG, "consumer doesn't exist");
            failure();
            return;
        }

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    developerId = "Bearer: " + response.getString("token");
                    Log.d(LOG_TAG, "Set developerId to: " + developerId);
                    if(isNewUser){
                        enrollConsumer();
                    } else {
                        verifyConsumer();
                    }
                } catch (JSONException e){
                    Log.e(LOG_TAG, "no token key: " + e.getMessage());
                    failure();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "error: " + error.getMessage());
                failure();
            }
        };

        JSONObject authenticateJson = new JSONObject();
        try{
            authenticateJson.accumulate("username", pref.getString("username", "null"));
            authenticateJson.accumulate("password", pref.getString("password", "null"));
        } catch (JSONException e){
            e.printStackTrace();
        }
        CustomJsonRequest authenticateRequest = new CustomJsonRequest(Request.Method.POST, AUTHENTICATE_CONSUMER_URL, authenticateJson, listener, errorListener);
        HttpRequest.getInstance(context).add(authenticateRequest);
    }

    private void createConsumer() {
        Log.d(LOG_TAG, "createConsumer");
        final String user = String.valueOf(UUID.randomUUID().getMostSignificantBits());
        final String password = "password";
        final String gender = "M";

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response){
                try{
                    consumerUrl = response.getString("href");
                    SharedPreferences pref = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("username", user);
                    editor.putString("password", password);
                    editor.putString("gender", gender);
                    editor.putBoolean("isConsumerCreated", true);
                    editor.putString("consumerUrl", consumerUrl);
                    editor.commit();
                    Log.d(LOG_TAG, "consumerUrl: " + consumerUrl);
                    authenticateConsumer(true);
                } catch (JSONException e){
                    Log.e(LOG_TAG, "did not find consumer href: " + e.getMessage());
                    consumerUrl = "";
                    failure();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error getting consumer href: " + error.getMessage());
                consumerUrl = "";
                failure();
            }
        };

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.accumulate("gender", gender);
            jsonObject.accumulate("username", user);
            jsonObject.accumulate("password", password);
            CustomJsonRequest jsonRequest = new CustomJsonRequest(Request.Method.POST, CREATE_CONSUMER_URL, jsonObject, listener, errorListener);
            HttpRequest.getInstance(context).add(jsonRequest);
        } catch (JSONException e){
            Log.e(LOG_TAG, "error accumulating jsonObject: " + e.getMessage());
            failure();
        }
    }

    private void requestAuthToken(final boolean isNewUser, final boolean getVocabOnly) {
        Log.d(LOG_TAG, "getAuthToken");
        if (!authToken.isEmpty()) {
            Log.d(LOG_TAG, "authToken not empty: " + authToken);
            if(isNewUser){
                createConsumer();
            } else {
                if(getVocabOnly){
                    createVerification(true);
                }
            }
        }

        Response.Listener<String> listener = new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject responseJson = new JSONObject(response);
                    authToken = "Bearer " + responseJson.getString("access_token");
                    Log.d(LOG_TAG, "authToken: " + authToken);

                    if (isNewUser) {
                        createConsumer();
                    } else {
                        if(getVocabOnly) {
                            createVerification(true);
                        }
                        //authenticateConsumer(false);
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "did not find auth token: " + e.getMessage());
                    authToken = "";
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error: " + error.getMessage());
                authToken = "";
            }
        };

        HashMap<String, String> params = new HashMap<>();
        params.put("client_id", context.getResources().getString(R.string.client_id));
        params.put("client_secret", context.getResources().getString(R.string.client_secret));
        CustomStringRequest loginRequest = new CustomStringRequest(Request.Method.POST, TOKEN_URL, params, listener, errorListener);
        HttpRequest.getInstance(context).add(loginRequest);
    }

    private void success() {
        this.listener.KnurldSuccess();
    }

    private void failure() {
        this.listener.KnurldFailure();
    }

    public static String getDeveloperId() {
        return developerId;
    }

    public static String getAuthToken() {
        return authToken;
    }
}
