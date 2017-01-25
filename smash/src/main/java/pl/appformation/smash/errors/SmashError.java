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
package pl.appformation.smash.errors;

import pl.appformation.smash.SmashNetworkData;

public class SmashError extends Exception
{

    private SmashNetworkData data;

    private String content;

    public SmashError()
    {
    }

    public SmashError(SmashNetworkData data)
    {
        this.data = data;
    }

    public SmashError(String message)
    {
        super(message);
    }

    public SmashError(String message, SmashNetworkData data)
    {
        super(message);
        this.data = data;
    }

    public SmashError(String message, Throwable throwable)
    {
        super(message, throwable);
    }

    public SmashError(String message, SmashNetworkData data, Throwable throwable)
    {
        super(message, throwable);
        this.data = data;
    }

    public SmashError(SmashNetworkData data, Throwable throwable)
    {
        super(throwable);
        this.data = data;
    }

    public SmashError(Throwable throwable)
    {
        super(throwable);
    }

    public SmashNetworkData getData()
    {
        return data;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

}
