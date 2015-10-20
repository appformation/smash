package pl.appformation.smash.requests;

import java.io.IOException;
import okio.Okio;
import pl.appformation.smash.SmashNetworkData;
import pl.appformation.smash.SmashRequest;
import pl.appformation.smash.SmashResponse;
import pl.appformation.smash.SmashResponse.FailedListener;
import pl.appformation.smash.SmashResponse.SuccessListener;
import pl.appformation.smash.errors.SmashError;

public class SmashStringRequest extends SmashRequest<String>
{

    public SmashStringRequest(@MethodRes int method, SuccessListener<String> successListener, FailedListener failedListener)
    {
        super(method, successListener, failedListener);
    }

    public SmashStringRequest(@MethodRes int method, String url, SuccessListener<String> successListener, FailedListener failedListener)
    {
        super(method, url, successListener, failedListener);
    }

    protected SmashResponse parseResponse(SmashNetworkData data)
    {
        try
        {
            return SmashResponse.success(Okio.buffer(data.source).readUtf8());
        }
        catch (IOException ioe)
        {
            return SmashResponse.failed(new SmashError(ioe));
        }
    }

}
