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

import android.os.Looper;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SmashQueue
{

    /** Default thread pool size */
    private static final int THREAD_POOL_SIZE = 5;

    /**
     * The set of all requests currently being processed by this SmashQueue. A Request
     * will be in this set if it is waiting in any queue or currently being processed by
     * any dispatcher.
     */
    private final Set<SmashRequest<?>> mCurrentRequests = new HashSet<>();

    /** Array of dispatchers */
    private final SmashDispatcher[] mDispatchers;

    /** Priority queue of requests */
    private final PriorityBlockingQueue<SmashRequest<?>> mQueue = new PriorityBlockingQueue<>();

    /** Indicate if queue is running */
    private boolean mRunning = false;

    /** Sequence number */
    private final AtomicInteger mSequence = new AtomicInteger();

    /**
     * Creates queue with default (3) thread pool size.
     */
    public SmashQueue()
    {
        this(THREAD_POOL_SIZE);
    }

    /**
     * Creates queue with provided thread pool size.
     *
     * @param threadPoolSize Threads to use in pool
     */
    public SmashQueue(int threadPoolSize)
    {
        mDispatchers = new SmashDispatcher[threadPoolSize];
    }

    /**
     * Add request to queue, and returns passed-in request.
     * Response will be delivered on main (ui) thread.
     *
     * @param request Request to service
     * @return The passed-in request
     */
    public <T> SmashRequest<T> add(SmashRequest<T> request)
    {
        return add(request, Looper.getMainLooper());
    }

    /**
     * Add request to queue, and returns passed-in request.
     * Response will be delivered on thread handled by looper.
     *
     * Threads that should first call Looper.prepare() before using that method.
     *
     * @param request Request to service
     * @return The passed-in request
     */
    public <T> SmashRequest<T> add(SmashRequest<T> request, Looper looper)
    {
        request.setSmashQueue(this);
        request.setDeliverResponseOn(looper);

        synchronized (mCurrentRequests)
        {
            mCurrentRequests.add(request);
        }

        mQueue.add(request);
        return request;
    }

    /**
     * Cancel all current requests.
     */
    public void cancelAll()
    {
        synchronized (mCurrentRequests)
        {
            for (SmashRequest<?> request : mCurrentRequests)
            {
                request.cancel();
            }
        }
    }

    /**
     * Performs required actions when request finish his work.
     *
     * @param request Request that finished
     */
    <T> void finishRequest(SmashRequest<T> request)
    {
        synchronized (mCurrentRequests)
        {
            mCurrentRequests.remove(request);
        }
    }

    /**
     * Starts the queue.
     * Starts all dispatchers.
     */
    public void start()
    {
        if (mRunning)
        {
            stop();
        }

        for (int i = 0; i < mDispatchers.length; i++)
        {
            mDispatchers[i] = new SmashDispatcher(mQueue);
            mDispatchers[i].start();
        }
    }

    /**
     * Stops the queue.
     * Stops all dispatchers.
     */
    public void stop()
    {
        if (!mRunning)
        {
            return;
        }

        for (SmashDispatcher dispatcher : mDispatchers)
        {
            if (dispatcher != null)
            {
                dispatcher.quit();
            }
        }
    }

}
