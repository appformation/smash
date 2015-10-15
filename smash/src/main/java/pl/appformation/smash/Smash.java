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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class Smash
{

    /** Default user agent */
    private static final String USER_AGENT = "smash/1";

    /** Cached user agent based on application name and version */
    private static String sCachedUserAgent = null;

    /** Current user agent */
    private static String sUserAgent = null;

    /**
     * Builds new SmashQueue to handle requests.
     *
     * @param context Context
     * @return Newly created {@link SmashQueue}
     */
    public static SmashQueue buildSmashQueue(Context context)
    {
        try
        {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);

            sCachedUserAgent = packageName + "/" + info.versionCode;
        }
        catch (PackageManager.NameNotFoundException ignored)
        {
        }

        SmashQueue queue = new SmashQueue();
        queue.start();

        return queue;
    }

    public static String getUserAgent()
    {
        if (sUserAgent != null)
        {
            return sUserAgent;
        }

        if (sCachedUserAgent != null)
        {
            return sCachedUserAgent;
        }

        return USER_AGENT;
    }

    static void log(String tag, String message)
    {
        Log.d(tag, message);
    }

    public static void setUserAgent(String userAgent)
    {
        Smash.sUserAgent = userAgent;
    }

}
