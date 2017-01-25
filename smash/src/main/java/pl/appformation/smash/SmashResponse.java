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

import pl.appformation.smash.errors.SmashError;

public class SmashResponse<T>
{

    /** Parsed response, or null in the case of error. */
    private final T mResult;

    /** Detailed error information. */
    private final SmashError mError;

    /**
     * Interface for delivering successful response
     */
    public interface SuccessListener<T>
    {
        void onResponse(T response);
    }

    /**
     * Interface for delivering failed response
     */
    public interface FailedListener
    {
        void onFailedResponse(SmashError error);
    }

    /**
     * Returns a successful response containing the parsed result.
     */
    public static <T> SmashResponse<T> success(T result)
    {
        return new SmashResponse<>(result);
    }

    /**
     * Returns a failed response containing the given error code and an optional
     * localized message displayed to the user.
     */
    public static <T> SmashResponse<T> failed(SmashError error)
    {
        return new SmashResponse<>(error);
    }

    private SmashResponse(T result)
    {
        this.mResult = result;
        this.mError = null;
    }

    private SmashResponse(SmashError error)
    {
        this.mResult = null;
        this.mError = error;
    }

    /**
     * Returns parsed response if successful.
     */
    public T getResult()
    {
        return mResult;
    }

    /**
     * Returns true whether this response is considered successful.
     */
    public boolean isSuccess()
    {
        return mError == null;
    }

    /**
     * Returns reported error if failed.
     */
    public SmashError getError()
    {
        return mError;
    }

}
