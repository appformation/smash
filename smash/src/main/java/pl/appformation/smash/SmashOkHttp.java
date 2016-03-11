/*
 * Copyright (C) 2015 Appformation sp. z o.o.
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
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSource;
import pl.appformation.smash.errors.SmashError;
import static pl.appformation.smash.SmashRequest.Method.DELETE;
import static pl.appformation.smash.SmashRequest.Method.GET;
import static pl.appformation.smash.SmashRequest.Method.HEAD;
import static pl.appformation.smash.SmashRequest.Method.PATCH;
import static pl.appformation.smash.SmashRequest.Method.POST;
import static pl.appformation.smash.SmashRequest.Method.PUT;

public class SmashOkHttp
{

    /** User-Agent header name */
    public static final String HEADER_USER_AGENT = "User-Agent";

    /** Default OkHttpClient instance */
    private static OkHttpClient sHttpClient = new OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * Adds {@link Interceptor} object to default {@link OkHttpClient} instance.
     *
     * @param interceptor Interceptor object
     */
    public static void addNetworkInterceptor(@NonNull Interceptor interceptor)
    {
        sHttpClient = sHttpClient.newBuilder()
                .addNetworkInterceptor(interceptor)
                .build();
    }

    private static RequestBody convertBody(SmashRequest request, BufferedSource body) throws SmashError
    {
        if (body == null && request.getMethod() == DELETE)
        {
            return RequestBody.create(null, new byte[0]);
        }

        try
        {
            if (body == null)
            {
                body = new Buffer();
            }

            return RequestBody.create(MediaType.parse(request.getBodyContentType()), body.readByteArray());
        }
        catch (IOException ioe)
        {
            throw new SmashError(ioe);
        }
    }

    private static BufferedSource getBody(SmashRequest request)
    {
        if (request.getMethod() == GET || request.getMethod() == HEAD)
        {
            return null;
        }

        return request.getBody();
    }

    static @NonNull SmashNetworkData perform(SmashRequest<?> request) throws SmashError
    {
        SmashNetworkData data = new SmashNetworkData();
        Request okRequest = null;

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        sHttpClient = sHttpClient.newBuilder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();

        try
        {
            Request.Builder okBuilder = new Request.Builder().url(request.getUrl());
            okBuilder.addHeader(HEADER_USER_AGENT, Smash.getUserAgent());

            Headers requestHeaders = request.getHeaders();
            if (requestHeaders != null)
            {
                okBuilder.headers(requestHeaders);
                if (!requestHeaders.names().contains(HEADER_USER_AGENT))
                {
                    okBuilder.addHeader(HEADER_USER_AGENT, Smash.getUserAgent());
                }
            }

            BufferedSource body = getBody(request);
            switch (request.getMethod())
            {
                case GET:
                {
                    okBuilder = okBuilder.get();
                    break;
                }
                case POST:
                {
                    okBuilder = okBuilder.post(convertBody(request, body));
                    break;
                }
                case PUT:
                {
                    okBuilder = okBuilder.put(convertBody(request, body));
                    break;
                }
                case DELETE:
                {
                    okBuilder = okBuilder.delete(convertBody(request, body));
                    break;
                }
                case HEAD:
                {
                    okBuilder = okBuilder.head();
                    break;
                }
                case PATCH:
                {
                    okBuilder = okBuilder.patch(convertBody(request, body));
                    break;
                }
            }

            boolean previousFollowing = sHttpClient.followRedirects();
            if (previousFollowing != request.isFollowingRedirects())
            {
                sHttpClient = sHttpClient.newBuilder()
                        .followRedirects(request.isFollowingRedirects())
                        .build();
            }

            okRequest = okBuilder.build();
            Response okResponse = sHttpClient.newCall(okRequest).execute();

            if (body != null)
            {
                body.close();
            }

            if (previousFollowing != sHttpClient.followRedirects())
            {
                sHttpClient = sHttpClient.newBuilder()
                        .followRedirects(request.isFollowingRedirects())
                        .build();
            }

            data.url = okResponse.request().url();
            data.code = okResponse.code();
            data.headers = okResponse.headers();
            data.source = okResponse.body().source();
            data.length = okResponse.body().contentLength();
        }
        catch (IOException ioe)
        {
            if (okRequest != null)
            {
                data.url = okRequest.url();
            }

            throw new SmashError(data, ioe);
        }

        return data;
    }

    /**
     * Removes {@link Interceptor} object from default {@link OkHttpClient} instance.
     *
     * @param interceptor Interceptor object
     */
    public static void removeNetworkInterceptor(@NonNull Interceptor interceptor)
    {
        OkHttpClient.Builder builder = sHttpClient.newBuilder();
        builder.networkInterceptors().remove(interceptor);

        sHttpClient = builder.build();
    }

}
