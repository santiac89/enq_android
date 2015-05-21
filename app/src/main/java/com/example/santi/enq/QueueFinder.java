package com.example.santi.enq;

import android.util.JsonReader;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Santi on 18/04/2015.
 */
public class QueueFinder {

    static private DefaultHttpClient client = new DefaultHttpClient();
    static private DefaultHttpRequestFactory reqFactory = new DefaultHttpRequestFactory();
    static private Gson gson = new Gson();

    public static List<Queue> findQueues() throws MethodNotSupportedException, IOException, JSONException {
        List<Queue> queues = new ArrayList<>();
        /*
        *   Conectar con el servidor y obtener colass
        *
        *   Interprete/Binder JSON -> JAVA
         */

        HttpResponse response = client.execute(new HttpGet("http://192.168.0.5:3000/queues?op=search"));

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
        {
            String responseAsString = EntityUtils.toString(response.getEntity());
            JsonArray array = gson.fromJson(responseAsString,JsonArray.class);

            for (JsonElement elem : array)
            {
                 queues.add(gson.fromJson(elem,Queue.class));
            }
        }

        return queues;
    }

}
