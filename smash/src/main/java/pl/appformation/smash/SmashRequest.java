/*
 * Copyright (C) 2015 Appformation sp. z o.o.
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.appformation.smash;

import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.util.Map;
import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;
import pl.appformation.smash.SmashResponse.FailedListener;
import pl.appformation.smash.SmashResponse.SuccessListener;
import pl.appformation.smash.errors.SmashError;

/**
 * Base abstract class for all requests.
 *
 * @param <T> Type of parsed response this request provide
 */
public abstract class SmashRequest<T> implements Comparable<SmashRequest<T>>
{

    /** Default parameters encoding */
    private static final String PARAMS_ENCODING = "UTF-8";

    /** Support request methods */
    public interface Method
    {
        int GET = 0;
        int POST = 1;
        int PUT = 2;
        int DELETE = 3;
        int HEAD = 4;
        int PATCH = 5;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Method.GET, Method.POST, Method.PUT, Method.DELETE, Method.HEAD, Method.PATCH})
    public @interface MethodRes {}

    /** Whether or not this request has been canceled */
    private boolean mCanceled = false;

    /** Response thread Looper */
    private Looper mDeliverResponseOn;

    /** HTTP request method */
    private final @MethodRes int mMethod;

    /** Successful request listener */
    private final SuccessListener<T> mSuccessListener;

    /** Failed request listener */
    private final FailedListener mFailedListener;

    /** Whether or not response of this request has been delivered */
    private boolean mResponseDelivered = false;

    /** SmashQueue that handles this request */
    private SmashQueue mSmashQueue;

    /** Request URL */
    private String mUrl;

    /**
     * Creates a new request.
     *
     * @param method Method to use in request
     * @param successListener Successful request listener
     * @param failedListener Failed request listener
     */
    public SmashRequest(@MethodRes int method, SuccessListener<T> successListener, FailedListener failedListener)
    {
        this.mMethod = method;
        this.mSuccessListener = successListener;
        this.mFailedListener = failedListener;
    }

    /**
     * Creates a new request with URL value.
     *
     * @param method Method to use in request
     * @param url URL to use in request
     * @param successListener Successful request listener
     * @param failedListener Failed request listener
     */
    public SmashRequest(@MethodRes int method, String url, SuccessListener<T> successListener, FailedListener failedListener)
    {
        this(method, successListener, failedListener);
        this.mUrl = url;
    }

    /**
     * Cancels request.
     */
    public void cancel()
    {
        this.mCanceled = true;
    }

    /**
     * Default implementation of delivering error on failed response listener.
     *
     * @param error SmashError
     */
    public void deliverError(SmashError error)
    {
        mFailedListener.onFailedResponse(error);
    }

    /**
     * Default implementation of delivering response to success listener.
     *
     * @param response Response
     */
    public void deliverResponse(SmashResponse<T> response)
    {
        mSuccessListener.onResponse(response.getResult());
    }

    /**
     * Converts params into an application/x-www-form-urlencoded encoded string.
     */
    private String encodeParameters(Map<String, String> params, String paramsEncoding)
    {
        StringBuilder encodedParams = new StringBuilder();

        try
        {
            for (Map.Entry<String, String> entry : params.entrySet())
            {
                encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
                encodedParams.append('=');
                encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
                encodedParams.append('&');
            }
            return encodedParams.toString();
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
        }
    }

    /**
     * Notifies the request queue that this request has finished (successfully or with error).
     */
    void finish()
    {
        if (mSmashQueue != null)
        {
            mSmashQueue.finishRequest(this);
        }
    }

    /**
     * Returns thread looper in which to deliver response.
     *
     * @return Looper
     */
    final Looper getDeliverResponseOn()
    {
        return mDeliverResponseOn;
    }

    /**
     * Returns BufferedSource as body for use in POST, PUT, DELETE, PATCH.
     */
    protected BufferedSource getBody()
    {
        Map<String, String> params = getParams();
        if (params != null && params.size() > 0)
        {
            Buffer buffer = new Buffer();
            buffer.writeUtf8(encodeParameters(params, getParamsEncoding()));

            return buffer;
        }

        return null;
    }

    /**
     * Returns the content type of the POST or PUT body.
     */
    String getBodyContentType()
    {
        return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
    }

    /**
     * Returns request method.
     */
    @MethodRes int getMethod()
    {
        return mMethod;
    }

    /**
     * Returns a Map<String, String> of parameters to be used for a POST or PUT request.
     * Note that you can directly override {@link #getBody()} for custom data.
     */
    protected Map<String, String> getParams()
    {
        return null;
    }

    /**
     * Returns which encoding should be used when converting POST or PUT parameters.
     */
    protected String getParamsEncoding()
    {
        return PARAMS_ENCODING;
    }

    /**
     * Returns request URL.
     */
    String getUrl()
    {
        return mUrl;
    }

    /**
     * Returns true if this request has been canceled.
     */
    public boolean isCanceled()
    {
        return mCanceled;
    }

    /**
     * Returns true if request is already in queue.
     */
    private boolean isInQueue()
    {
        return mSmashQueue != null;
    }

    /**
     * Returns true if this request has been canceled.
     */
    public boolean isResponseDelivered()
    {
        return mResponseDelivered;
    }

    /**
     * Subclasses can override this method to parse network error response
     * and return a more specific error.
     *
     * The default implementation just returns the passed SmashError.
     *
     * @param error the error retrieved from the network
     * @return SmashError augmented with additional information
     */
    protected SmashError parseNetworkError(SmashError error)
    {
        try
        {
            if (error.getData() != null && error.getData().source != null)
            {
                error.setContent(Okio.buffer(error.getData().source).readUtf8());
            }
        }
        catch (IOException ioe)
        {
            throw new RuntimeException("Unable to parse error response", ioe);
        }

        return error;
    }

    /**
     * Subclasses must implement this to parse the raw network response
     * and return an appropriate response type. This method will be
     * called from a worker thread.  The response will not be delivered
     * if you return null.
     *
     * @param data Response network data
     * @return The parsed response, or null in the case of an error
     */
    protected abstract SmashResponse parseResponse(SmashNetworkData data);

    /**
     * Sets thread looper on which response will be delivered.
     */
    final void setDeliverResponseOn(Looper deliverResponseOn)
    {
        mDeliverResponseOn = deliverResponseOn;
    }

    /**
     * Sets whether response was delivered.
     */
    final void setResponseDelivered(boolean responseDelivered)
    {
        this.mResponseDelivered = responseDelivered;
    }

    /**
     * Sets SmashQueue that handles this request. Once requests is added to queue, this
     * method will be invoked.
     *
     * @param smashQueue SmashQueue that handles this request
     */
    void setSmashQueue(SmashQueue smashQueue)
    {
        this.mSmashQueue = smashQueue;
    }

    /**
     * Sets request URL. This method should not be used after request is added to the queue.
     *
     * @param url New URL
     * @throws UnsupportedOperationException when request is already in queue
     */
    public final void setUrl(String url)
    {
        if (isInQueue())
        {
            throw new UnsupportedOperationException("Can't change URL, request is already in the queue");
        }

        this.mUrl = url;
    }

    /**
     * Compares two SmashRequest objects. Order of check
     * - priority
     * - sequence
     */
    public int compareTo(@NonNull SmashRequest<T> another)
    {
        return 0;
    }

}
