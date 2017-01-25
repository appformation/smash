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

import android.os.Handler;
import android.os.Process;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import pl.appformation.smash.errors.SmashError;

public class SmashDispatcher extends Thread
{

    /** Queue of requests to pick from */
    private final BlockingQueue<SmashRequest<?>> mQueue;

    /** Tells whether we should quit */
    private volatile boolean mQuit = false;

    /**
     * Creates a new dispatch thread. You must call {@link #start()} in
     * order to start dispatcher.
     *
     * @param queue Queue of incoming requests for triage
     */
    public SmashDispatcher(BlockingQueue<SmashRequest<?>> queue)
    {
        super("SmashDispatcher");
        this.mQueue = queue;
    }

    /**
     * Forces this dispatcher to quit immediately.  If any requests are still in
     * the queue, they are not guaranteed to be processed.
     */
    public void quit()
    {
        mQuit = true;
        interrupt();
    }

    @Override
    public void run()
    {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        while (true)
        {
            SmashRequest<?> request;

            try
            {
                request = mQueue.take();
            }
            catch (InterruptedException ie)
            {
                if (mQuit)
                {
                    return;
                }
                continue;
            }

            Smash.log("SmashDispatcher", "Picked up request " + request);
            SmashNetworkData data = null;

            try
            {
                if (request.isCanceled())
                {
                    request.finish();
                    continue;
                }

                data = SmashOkHttp.perform(request);
                if (data.isNotModified() && request.isResponseDelivered())
                {
                    request.finish();
                    continue;
                }

                request.setResponseDelivered(true);

                if (data.code >= 400)
                {
                    SmashError error = new SmashError(data);
                    error = request.parseNetworkError(error);

                    deliverError(request, error);
                    continue;
                }

                SmashResponse<?> response = request.parseResponse(data);
                if (!response.isSuccess())
                {
                    deliverError(request, response.getError());
                    continue;
                }

                deliverResponse(request, response);
            }
            catch (SmashError se)
            {
                se = request.parseNetworkError(se);
                deliverError(request, se);
            }
            catch (Exception e)
            {
                SmashError se = new SmashError(e);
                deliverError(request, se);
            }
            finally
            {
                if (data != null && data.source != null)
                {
                    try
                    {
                        data.source.close();
                    }
                    catch (IOException ignored)
                    {
                        Smash.log("SmashDispatcher", "Unable to close source data");
                    }
                }
            }
        }
    }

    private void deliverError(final SmashRequest request, final SmashError error)
    {
        Smash.log("SmashDispatcher", "Delivering failed response for " + request);
        deliver(request, new Runnable()
        {
            public void run()
            {
                request.deliverError(error);
                request.finish();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void deliverResponse(final SmashRequest request, final SmashResponse response)
    {
        Smash.log("SmashDispatcher", "Delivering success response for " + request);
        deliver(request, new Runnable()
        {
            public void run()
            {
                request.deliverResponse(response);
                request.finish();
            }
        });
    }

    private void deliver(SmashRequest request, Runnable runnable)
    {
        new Handler(request.getDeliverResponseOn()).post(runnable);
    }
}
