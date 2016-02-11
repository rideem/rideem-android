package io.rideem.api;

import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Android rideem API client.
 *
 * All API action methods return a {@link Rideem.Task}. This allows the caller
 * to decide where, when, and how to run the task. The caller can invoke the
 * Rideem.Task's get method immediately if not in the UI thread, store it for
 * in an AsyncTask, or use it with {@link Rideem#async}.
 *
 * @version 1.0.0
 */

public class Rideem {

    /**
     * Rideem.Task represents an API task to execute.
     *
     * @since 1.0.0
     */
    public static class Task<T> implements Callable, Runnable {
        /**
         * Executes a request and returns the value of this task.
         * This is a blocking network call so can not be used in the UI thread.
         *
         * @return T The value from the request.
         */
        public T
        get()
        {
            T o = null;
            try { o = _get(); } catch (Exception e) {}
            return o;
        }

        @Override
        public T
        call() throws Exception { return _get(); }

        @Override
        public void
        run() { get(); }

        /**
         * Internal _get method allowing exceptions through.
         * Overriden and implemented in each API call.
         */
        public T
        _get() { return null; }
    }

    /**
     * Rideem.Code represents a promo code.
     *
     * @see Rideem#from
     * @since 1.0.0
     */
    public static class Code {
        /**
         * The value of the code.
         * @since 1.0.0
         */
        public String code;

        /**
         * The delay in seconds for the next code availability.
         * @since 1.0.0
         */
        public int delay;

        /**
         * Checks if this code is an empty string.
         *
         * @since 1.0.0
         */
        public boolean
        empty()
        {
            return code == null || code.length() == 0;
        }

        /**
         * Create a Rideem.Code object from a JSONObject.
         *
         * @param json JSONObject return from from {@link Rideem#req}
         * @return The created Rideem.Code
         * @since 1.0.0
         */
        public static Rideem.Code
        create(JSONObject json)
        {
            Rideem.Code code = new Rideem.Code();
            if (json.has("code")) code.code = json.optString("code");
            if (json.has("delay")) code.delay = json.optInt("delay");

            return code;
        }

        private Code() { code = ""; delay = 1; }
    }

    /**
     * Creates an instance of the rideem API
     *
     * @return Rideem API
     * @see Rideem
     * @since 1.0.0
     */
    public static Rideem
    create() {
        return Rideem.create(null);
    }

    /**
     * Creates an instance of the rideem API
     *
     * @param key The app secret key.
     * @return Rideem API
     * @see Rideem
     * @since 1.0.0
     */
    public static Rideem
    create(String key)
    {
        return new Rideem(key);
    }

    /**
     * Redeems a code from an app from the default promotion.
     *
     * @param app The name of the app
     *
     * @return Task providing the code
     * @see Rideem.Code
     * @see Rideem.Task
     * @since 1.0.0
     */
    public Rideem.Task<Rideem.Code>
    from(String app)
    {
        return from(app, null, null);
    }

    /**
     * Redeems a code from an app for a promotion.
     *
     * @param app The name of the app
     * @param promo The name of the promotion
     *
     * @return Task providing the code
     * @see Rideem.Code
     * @see Rideem.Task
     * @since 1.0.0
     */
    public Rideem.Task<Rideem.Code>
    from(String app, String promo)
    {
        return from(app, promo, null);
    }

    /**
     * Redeems a code from an app for a private promotion.
     *
     * @param app The name of the app
     * @param promo The name of the promotion
     * @param key The key for the promotion
     *
     * @return Task providing the code
     * @see Rideem.Code
     * @see Rideem.Task
     * @since 1.0.0
     */
    public Rideem.Task<Rideem.Code>
    from(final String app, final String promo, final String key)
    {
        return new Rideem.Task<Rideem.Code> () {
            @Override
            public Rideem.Code _get() {
                Rideem.Code code = new Rideem.Code();
                String k = (null != key) ? key : _key;
                String endpoint = _host + "/rideem/from/" + app;
                if (null != promo) endpoint += "/for/" + promo;
                if (null != k) endpoint += "?key=" + k;

                Rideem.Response resp = req(new HttpGet(endpoint));

                return Rideem.Code.create(resp.json);
            }
        };
    }

    /**
     * Posts a request for an app.
     *
     * @param app The name of the app
     * @return Task providing the current number of requests for this app
     * @see Rideem.Task
     * @since 1.0.0
     */
    public Rideem.Task<Integer>
    request(final String app)
    {
        return new Rideem.Task<Integer> () {
            @Override
            public Integer _get() {
                int count = 0;
                String endpoint = _host + "/rideem/request/" + app;

                Rideem.Response resp = req(new HttpPost(endpoint));
                JSONObject json = resp.json;

                if (json.has("count")) count = json.optInt("count");

                return count;
            }
        };
    }

    /**
     * Runs a {@link Rideem.Task} asynchronously.
     *
     * @return A Future representing the result of the task
     * @see Rideem.Task
     * @see Future
     * @since 1.0.0
     */
    public <T> Future<T>
    async(Rideem.Task<T> t)
    {
        if (null == _executor) {
            _executor = Executors.newFixedThreadPool(10);
        }

        @SuppressWarnings("unchecked")
        Callable<T> c = (Callable<T>)t;

        return _executor.submit(c);
    }

    /**
     * Shuts down the execution service if {@link Rideem#async} was used.
     *
     * @see ExecutionService
     * @see Rideem.async
     * @since 1.0.0
     */
    public void
    async_shutdown()
    {
        if (null != _executor) _executor.shutdownNow();
    }

    /**
     * Sets the host to use for requests.
     *
     * @param host The host URL including scheme - https://rideem.io
     * @return This rideem object for chaining
     * @since 1.0.0
     */
    public Rideem
    host(String host)
    {
        _host = host;
        return this;
    }

    /**
     * Sets the app secret key.
     *
     * @param key The app secret key
     * @return This rideem object for chaining
     * @since 1.0.0
     */
    public Rideem
    key(String key)
    {
        _key = key;
        return this;
    }

    /**
     * Sets ExecutorService used in {@link Rideem#async}.
     *
     * @param executor The execution service
     * @return This rideem object for chaining
     * @since 1.0.0
     */
    public Rideem
    executor(ExecutorService executor)
    {
        _executor = executor;
        return this;
    }

    private
    Rideem(String key)
    {
        _key = key;
    }

    /**
     * Performs an HTTP request and returns a {@link Rideem.Response}.
     *
     * @param endpoint The API endpoint for the request
     * @return The response
     * @see Rideem.Response
     * @since 1.0.0
     */
    private Rideem.Response
    req(HttpUriRequest endpoint)
    {
        Rideem.Response r = new Rideem.Response();
        try {
            HttpClient client = new DefaultHttpClient();
            HttpResponse resp = client.execute(endpoint);
            HttpEntity entity = resp.getEntity();

            r.status = resp.getStatusLine().getStatusCode();

            if (null == entity) return r;

            InputStream is = entity.getContent();
            if (null != is) {
                StringBuilder sb = new StringBuilder();
                BufferedReader br =
                    new BufferedReader(new InputStreamReader(is));
                String line = "";

                while((line = br.readLine()) != null)
                    sb.append(line);

                is.close();

                r.json = new JSONObject(sb.toString());
            }
        } catch (JSONException e) {
            r.status = 500;
        } catch (IOException e) {
            r.status = 500;
        }

        return r;
    }

    /**
     * Response object returned from {@link Rideem#req}.
     *
     * @since 1.0.0
     */
    private static class Response {
        public JSONObject json;
        public int status;
        public Response() { json = new JSONObject(); status = 500; }
    };

    private String _host = "https://rideem.io";
    private String _key; // app secret key
    private ExecutorService _executor;
}
