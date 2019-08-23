package com.hypertrack.hyperlog;

import android.content.Context;
import android.os.Build;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.hypertrack.hyperlog.utils.CustomGson;
import com.hypertrack.hyperlog.utils.HLDateTimeUtility;
import com.hypertrack.hyperlog.utils.Utils;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Omer Younus on 2019-08-23.
 */
class HLHTTPPostRequest<T> extends Request<T> {

    private static final String TAG = HLHTTPMultiPartPostRequest.class.getSimpleName();
    private final Class<T> mResponseType;
    private final WeakReference<Response.Listener<T>> mListener;

    private Context context;
    private String packageName;
    private Gson mGson;

    private final HashMap<String, String> additionalHeaders;
    private final String body;

    HLHTTPPostRequest(String url, String requestBody, HashMap<String, String> additionalHeaders,
                               Context context, Class<T> responseType, Response.Listener<T> listener, Response.ErrorListener errorListener) {

        super(Method.POST, url, errorListener);

        this.body = requestBody;
        this.additionalHeaders = additionalHeaders;
        this.mResponseType = responseType;
        this.context = context;
        this.mListener = new WeakReference<>(listener);
        this.packageName = context.getPackageName();
        this.mGson = CustomGson.gson();

    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {

        Map<String, String> params = new HashMap<>();
        params.put("User-Agent", context.getPackageName() + " (Android " + Build.VERSION.RELEASE + ")");
        params.put("Device-Time", HLDateTimeUtility.getCurrentTime());
        params.put("Device-ID", Utils.getDeviceId(context));
        params.put("App-ID", packageName);
        params.put("Content-Type", "");


        if (this.additionalHeaders != null) {
            Iterator<Map.Entry<String, String>> iterator = this.additionalHeaders.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> header = iterator.next();
                params.put(header.getKey(), header.getValue());
            }
        }
        return params;
    }

    @Override
    protected VolleyError parseNetworkError(VolleyError volleyError) {

        if (volleyError == null || volleyError.networkResponse == null)
            return super.parseNetworkError(volleyError);

        try {
            String json = new String(
                    volleyError.networkResponse.data, HttpHeaderParser.parseCharset(volleyError.networkResponse.headers));

            HyperLog.i(TAG, "Status Code: " + volleyError.networkResponse.statusCode +
                    " Data: " + json);

        } catch (Exception e) {
            HyperLog.e(TAG, "Exception occurred while HTTPPatchRequest parseNetworkError: " + e, e);
        }

        return super.parseNetworkError(volleyError);
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(
                    response.data, HttpHeaderParser.parseCharset(response.headers));

            return Response.success(
                    mGson.fromJson(json, mResponseType), HttpHeaderParser.parseCacheHeaders(response));

        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(T response) {
        HyperLog.i(TAG, "deliverResponse: ");
        if (mListener != null && mListener.get() != null)
            mListener.get().onResponse(response);
    }

    @Override
    public byte[] getBody() {
        try {
          return body.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getBodyContentType() {
        return "application/json; charset=utf-8";
    }
}
