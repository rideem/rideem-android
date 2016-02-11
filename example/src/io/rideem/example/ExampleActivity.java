package io.rideem.example;

import io.rideem.api.Rideem;



import android.app.Activity;
import android.widget.TextView;
import android.os.Bundle;
import android.os.AsyncTask;

import android.util.Log;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

public class ExampleActivity extends Activity
{
    private final String TAG = "Dummy";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final TextView tv = (TextView)findViewById(R.id.textbox);
        Log.d(TAG, "onCreate");

        final Rideem rideem = Rideem.create(/* app key */)
            .host("http://10.0.2.2:8080");

        rideem.from("app"); // does nothing - ever
        rideem.request("app"); // does nothing - ever
        /* rideem.from("app").get(); // blocking call - avoid in UI thread */

        // storing the result for later
        final Rideem.Task<Rideem.Code> rideem_from = rideem.from("app");
        final Rideem.Task<Integer> rideem_request = rideem.request("app");

        /* Using rideem.async - initiates the request in background */
        // Asynchronous - Safe in UI thread
        // Actually executes the requests - calls .get internally

        // request an app ignoring the result
        rideem.async(rideem.request("app"));
        rideem.async(rideem_request); // using the stored result


        // store the future code for later
        final Future<Rideem.Code> f_code = rideem.async(rideem.from("app"));


        new AsyncTask<Void, Void, String>() {
            @Override
            protected String
            doInBackground(Void... nothing) {
                Rideem.Code code = rideem_from.get(); // using saved results
                Rideem.Code another_code = rideem_from.get();
                int number_of_requests = rideem_request.get();

                Log.d(TAG, "Code: " + code.code +
                    " : Next available in " + code.delay + " seconds");
                Log.d(TAG, "Another Code: " + another_code.code);

                try {
                code = f_code.get();
                // might be first code since it was already executing before
                Log.d(TAG, "Future Code: " + code.code);
                } catch (ExecutionException e) { // thrown if failed to run
                } catch (InterruptedException e) {} // thrown if cancelled

                code = rideem.from("app").get();
                Log.d(TAG, "Yet Another Code: " + code.code);

                // redeem a code for a specific promo
                rideem.from("app", "promo").get();
                //
                // redeem a code for a specific promo using a promo key
                rideem.from("app", "promo", "key").get();


                Log.d(TAG, "Requests for app: " + number_of_requests);
                return (code.empty()) ? "NONE" : code.code;
            }

            @Override
            protected void
            onPostExecute(String result)
            {
                String codes = "Codes: " + result;

                /* using the future code in the UI thread once it completed */
                if (f_code.isDone()) { // true if completed in doInBackground
                try {
                    // only Future results are non-blocking when done
                    Rideem.Code code = f_code.get(); // non-blocking if done
                    if (!code.empty())
                        codes +=  " : " + code.code;
                } catch (ExecutionException e) { // thrown if failed to run
                } catch (InterruptedException e) {} // thrown if cancelled
                }

                tv.setText(codes);
            }

        }.execute();

    }
}
