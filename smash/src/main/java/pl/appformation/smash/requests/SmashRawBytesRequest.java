package pl.appformation.smash.requests;

import java.io.IOException;
import okio.Okio;
import pl.appformation.smash.SmashNetworkData;
import pl.appformation.smash.SmashRequest;
import pl.appformation.smash.SmashResponse;
import pl.appformation.smash.SmashResponse.FailedListener;
import pl.appformation.smash.SmashResponse.SuccessListener;
import pl.appformation.smash.errors.SmashError;

public class SmashRawBytesRequest extends SmashRequest<byte[]>
{

    public SmashRawBytesRequest(@MethodRes int method, SuccessListener<byte[]> successListener, FailedListener failedListener)
    {
        super(method, successListener, failedListener);
    }

    public SmashRawBytesRequest(@MethodRes int method, String url, SuccessListener<byte[]> successListener, FailedListener failedListener)
    {
        super(method, url, successListener, failedListener);
    }

    protected SmashResponse<byte[]> parseResponse(SmashNetworkData data)
    {
        try
        {
            return SmashResponse.success(Okio.buffer(data.source).readByteArray());
        }
        catch (IOException ioe)
        {
            return SmashResponse.failed(new SmashError(ioe));
        }
    }

}
