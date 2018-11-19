package uk.ac.masts.sifids.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.ac.masts.sifids.CatchApplication;
import uk.ac.masts.sifids.R;
import uk.ac.masts.sifids.database.CatchDatabase;
import uk.ac.masts.sifids.entities.Bycatch;
import uk.ac.masts.sifids.entities.CatchLocation;
import uk.ac.masts.sifids.singletons.RequestQueueSingleton;
import uk.ac.masts.sifids.utilities.Csv;

/**
 * Posts location data to the server for storage in database.
 * Usage:
 * new PostDataTask().execute(String url, String pln, String csv);
 * CSV fields: timestamp (string), isFishing (int), latitude (double), longitude (double)
 */
public class PostDataTask extends AsyncTask<Void, Void, Void> {

    Context context;
    CatchDatabase db;

    public PostDataTask(Context context) {
        this.context = context;
        this.db = CatchDatabase.getInstance(context);
    }

    @Override
    protected Void doInBackground(Void... params) {
        boolean noErrorsEncountered = true;
        while (this.anyLocationsToPost() && noErrorsEncountered) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String pln = prefs.getString(context.getString(R.string.pref_vessel_pln_key), "PLN");
            List<CatchLocation> locations = db.catchDao().getUnuploadedLocations();
            String csv = "";
            for (CatchLocation loc : locations) {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                df.setTimeZone(CatchApplication.TIME_ZONE);
                String rowToWrite = df.format(loc.getTimestamp());
                rowToWrite = Csv.appendToCsvRow(rowToWrite, loc.isFishing() ? 1 : 0, false, context);
                rowToWrite = Csv.appendToCsvRow(rowToWrite, loc.getLatitude(), false, context);
                rowToWrite = Csv.appendToCsvRow(rowToWrite, loc.getLongitude(), false, context);
                rowToWrite = Csv.appendToCsvRow(rowToWrite, loc.getAccuracy(), false, context);
                csv = Csv.appendRowToCsv(csv, rowToWrite);
            }
            try {
                URL url = new URL(context.getString(R.string.post_request_url));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);

                urlConnection.setRequestMethod("POST");

                MultipartEntityBuilder meBuilder = MultipartEntityBuilder.create();
                meBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                meBuilder.addTextBody("vessel_name", pln);
                meBuilder.addPart("tracks", new StringBody(csv, ContentType.TEXT_PLAIN));

                HttpEntity mpe = meBuilder.build();

                urlConnection.setRequestProperty("Connection", "Keep-Alive");
                urlConnection.addRequestProperty("Content-length", mpe.getContentLength() + "");
                urlConnection.addRequestProperty(
                        mpe.getContentType().getName(), mpe.getContentType().getValue());

                OutputStream out = urlConnection.getOutputStream();

                mpe.writeTo(out);
                urlConnection.connect();
                out.flush();
                out.close();

                InputStream input;

                if (urlConnection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                    input = urlConnection.getInputStream();
                } else {
                    input = urlConnection.getErrorStream();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                urlConnection.disconnect();

                if (urlConnection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                    final List<String> ids = new ArrayList<>();
                    for (CatchLocation loc : locations) {
                        ids.add(Integer.toString(loc.getId()));
                    }
                    db.catchDao().markLocationsUploaded(ids);
                }
                else {
                    noErrorsEncountered = false;
                }

                urlConnection.disconnect();

            } catch (Exception e) {
            }
        }
        if (anyBycatchesToPost() && noErrorsEncountered) {
            List<Bycatch> bycatches = db.catchDao().getUnsubmittedBycatches();
            for (final Bycatch bycatch : bycatches) {
                postBycatch(this.context, bycatch, new VolleyCallback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                db.catchDao().markBycatchSubmitted(bycatch.getId());
                            }
                        });
                    }

                    @Override
                    public void onError(String result) { }
                });
            }
        }
        else {
        }
        return null;
    }

    private boolean anyLocationsToPost() {
        return db.catchDao().countUnuploadedLocations() > 0;
    }

    private boolean anyBycatchesToPost() {
        return db.catchDao().countUnsubmittedBycatches() > 0;
    }

    public static void postBycatch(final Context context, final Bycatch bycatch, final VolleyCallback callback) {
        final CatchDatabase db = CatchDatabase.getInstance(context);
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        final GsonBuilder gsonBuilder = new GsonBuilder();
        JsonSerializer<Bycatch> serializer = new JsonSerializer<Bycatch>() {
            @Override
            public JsonElement serialize(
                    Bycatch src, Type typeOfSrc, JsonSerializationContext jsonSerializationContext) {
                JsonObject jsonBycatch = new JsonObject();
                jsonBycatch.addProperty(
                        "pln",
                        prefs.getString(context.getString(R.string.pref_vessel_pln_key), ""));
                Callable<String> c = new Callable<String>() {
                    @Override
                    public String call() {
                        return db.catchDao()
                                .getBycatchSpeciesName(bycatch.getBycatchSpeciesId());
                    }
                };
                ExecutorService service = Executors.newSingleThreadExecutor();
                Future<String> future = service.submit(c);
                try {
                    String animalDetails = future.get();
                    jsonBycatch.addProperty("species", animalDetails);
                } catch (Exception e) {
                    Log.e("JSON", "Exception");
                }
                if (bycatch.getWeight() != null) {
                    jsonBycatch.addProperty("weight", bycatch.getWeight());
                }
                else {
                    jsonBycatch.addProperty("count", bycatch.getNumber());
                }
                return jsonBycatch;
            }
        };
        gsonBuilder.registerTypeAdapter(Bycatch.class, serializer);
        Gson gson = gsonBuilder.create();
        try {
            final JSONObject bycatchJson = new JSONObject(gson.toJson(bycatch));
            final String url = context.getString(R.string.post_request_url);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, bycatchJson,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            callback.onSuccess(jsonObject);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (error.networkResponse != null && error.networkResponse.data != null) {
                                error = new VolleyError(new String(error.networkResponse.data));
                            }
                            callback.onError(error.getMessage());
                        }
                    }
            );
            RequestQueueSingleton.getInstance(context).addToRequestQueue(request);
        } catch (Exception e) {}
    }

    public static void postFish1Form(final Context context, final File formCsv) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    String pln = prefs.getString(context.getString(R.string.pref_vessel_pln_key), "PLN");

                    URL url = new URL(context.getString(R.string.post_request_url));
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);

                    urlConnection.setRequestMethod("POST");

                    MultipartEntityBuilder meBuilder = MultipartEntityBuilder.create();
                    meBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                    meBuilder.addTextBody("vessel_name", pln);
                    meBuilder.addPart("fish_1_form", new FileBody(formCsv, ContentType.TEXT_PLAIN));

                    HttpEntity mpe = meBuilder.build();

                    urlConnection.setRequestProperty("Connection", "Keep-Alive");
                    urlConnection.addRequestProperty("Content-length", mpe.getContentLength() + "");
                    urlConnection.addRequestProperty(
                            mpe.getContentType().getName(), mpe.getContentType().getValue());

                    OutputStream out = urlConnection.getOutputStream();

                    mpe.writeTo(out);
                    urlConnection.connect();
                    out.flush();
                    out.close();

                    InputStream input;

                    if (urlConnection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                        input = urlConnection.getInputStream();
                    } else {
                        input = urlConnection.getErrorStream();
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    urlConnection.disconnect();

                }
                catch (Exception e) { }
            }
        };
        Thread newThread = new Thread(r);
        newThread.start();
    }

    public interface VolleyCallback {
        void onSuccess(JSONObject result);
        void onError(String result);
    }
}
