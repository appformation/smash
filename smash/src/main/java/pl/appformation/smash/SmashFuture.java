/*
 * Copyright (C) 2015-2017 Appformation sp. z o.o.
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

import android.support.annotation.NonNull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import pl.appformation.smash.SmashResponse.FailedListener;
import pl.appformation.smash.SmashResponse.SuccessListener;
import pl.appformation.smash.errors.SmashError;

/**
 * Specialized success and failure response that provides blocking access
 * to response of request.
 *
 * SmashFuture also accepts as optional parameters success and failed listeners.
 * These will be invoked on SmashRequest#getDeliverResponseOn() looper.
 *
 * @param <T> Type of parsed response this future handle
 */
public class SmashFuture<T> implements Future<T>, SuccessListener<T>, FailedListener
{

    /** Optional failed listener */
    private FailedListener mForwardFailedListener;

    /** Optional success listener */
    private SuccessListener<T> mForwardSuccessListener;

    /** Optional SmashRequest provided to this listener */
    private SmashRequest<T> mRequest;

    /** Result received from success */
    private T mResult;

    /**
     * Boolean flag indicating result is received, we can't
     * rely on non-null for result as this might be expected value.
     */
    private boolean mResultReceived = false;

    /** Exception caught */
    private Exception mException;

    /**
     * Default constructor
     */
    public SmashFuture()
    {
        this(null, null);
    }

    /**
     * Optional listeners constructor
     */
    public SmashFuture(SuccessListener<T> successListener, FailedListener failedListener)
    {
        this.mForwardSuccessListener = successListener;
        this.mForwardFailedListener = failedListener;
    }

    /**
     * {@inheritDoc}
     */
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        if (mRequest == null)
        {
            return false;
        }

        if (!isDone() && !mRequest.isCanceled())
        {
            mRequest.cancel();
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public T get() throws InterruptedException, ExecutionException
    {
        try
        {
            return perform(0);
        }
        catch (TimeoutException te)
        {
            throw new ExecutionException(te);
        }
    }

    /**
     * {@inheritDoc}
     */
    public T get(long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
    {
        return perform(TimeUnit.MILLISECONDS.convert(timeout, unit));
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCancelled()
    {
        return mRequest != null && mRequest.isCanceled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDone()
    {
        return mResultReceived || mException != null || isCancelled();
    }

    /**
     * Handles failed response from dispatcher.
     */
    public synchronized void onFailedResponse(SmashError error)
    {
        if (mForwardFailedListener != null)
        {
            mForwardFailedListener.onFailedResponse(error);
        }

        mResultReceived = false;
        mException = error;

        notifyAll();
    }

    /**
     * Handles successful response from dispatcher.
     */
    public synchronized void onResponse(T response)
    {
        if (mForwardSuccessListener != null)
        {
            mForwardSuccessListener.onResponse(response);
        }

        mResultReceived = true;
        mResult = response;

        notifyAll();
    }

    /**
     * Perform synchronized get on future.
     */
    private synchronized T perform(long timeout) throws InterruptedException, ExecutionException, TimeoutException
    {
        if (mException != null)
        {
            throw new ExecutionException(mException);
        }
        if (timeout < 0)
        {
            throw new ExecutionException(new IllegalStateException("Timeout can't be negative"));
        }

        if (mResultReceived)
        {
            return mResult;
        }

        wait(timeout);

        if (mException != null)
        {
            throw new ExecutionException(mException);
        }

        if (!mResultReceived)
        {
            throw new TimeoutException();
        }

        return mResult;
    }

    /**
     * Sets optional request for use in eventual cancel.
     */
    public void setRequest(SmashRequest<T> request)
    {
        mRequest = request;
    }

}
