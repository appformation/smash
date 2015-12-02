package pl.appformation.smash.requests;

import org.json.JSONArray;
import org.json.JSONException;
import java.io.IOException;
import okio.Okio;
import pl.appformation.smash.SmashNetworkData;
import pl.appformation.smash.SmashRequest;
import pl.appformation.smash.SmashResponse;
import pl.appformation.smash.SmashResponse.FailedListener;
import pl.appformation.smash.SmashResponse.SuccessListener;
import pl.appformation.smash.errors.SmashError;

public class SmashJsonArrayRequest extends SmashRequest<JSONArray>
{

    public SmashJsonArrayRequest(@MethodRes int method, SuccessListener<JSONArray> successListener, FailedListener failedListener)
    {
        super(method, successListener, failedListener);
    }

    public SmashJsonArrayRequest(@MethodRes int method, String url, SuccessListener<JSONArray> successListener, FailedListener failedListener)
    {
        super(method, url, successListener, failedListener);
    }

    protected SmashResponse<JSONArray> parseResponse(SmashNetworkData data)
    {
        try
        {
            JSONArray json = new JSONArray(Okio.buffer(data.source).readUtf8());
            return SmashResponse.success(json);
        }
        catch (JSONException | IOException e)
        {
            return SmashResponse.failed(new SmashError(e));
        }
    }

}
