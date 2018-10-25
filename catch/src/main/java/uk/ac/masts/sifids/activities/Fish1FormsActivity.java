package uk.ac.masts.sifids.activities;

import android.Manifest;
import android.app.ActivityManager;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.Switch;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

import uk.ac.masts.sifids.CatchApplication;
import uk.ac.masts.sifids.R;
import uk.ac.masts.sifids.database.CatchDatabase;
import uk.ac.masts.sifids.entities.Fish1Form;
import uk.ac.masts.sifids.services.CatchLocationService;

public class Fish1FormsActivity extends AppCompatActivityWithMenuBar {

    FloatingActionButton fab;
    public static RecyclerView recyclerView;
    public static RecyclerView.Adapter adapter;
    List<Fish1Form> forms;
    CatchDatabase db;
    Calendar selectedWeekStart;
    Switch locationSwitch;
    Switch fishingSwitch;

    final static int PERMISSION_REQUEST_FINE_LOCATION = 568;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fish_1_forms);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = CatchDatabase.getInstance(getApplicationContext());
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Executors.newSingleThreadScheduledExecutor().execute(new Runnable() {
            @Override
            public void run() {
                int portId = db.catchDao().getPortId();
                prefs.edit().putInt(getString(R.string.pref_port_key),portId).apply();
            }
        });

        locationSwitch = findViewById(R.id.toggle_location_tracking);
        fishingSwitch = findViewById(R.id.toggle_fishing);

        locationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (
                        isChecked && ContextCompat.checkSelfPermission(
                                Fish1FormsActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            Fish1FormsActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_REQUEST_FINE_LOCATION);
                } else {
                    ((CatchApplication) Fish1FormsActivity.this.getApplication()).setTrackingLocation(isChecked);
                    if (!isChecked) {
                        ((Switch) findViewById(R.id.toggle_fishing)).setChecked(false);
                    }
                }
            }
        });

        fishingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CatchApplication app = (CatchApplication) getApplication();
                app.setFishing(isChecked);
                if (isChecked) {
                    ((Switch) findViewById(R.id.toggle_location_tracking)).setChecked(true);
                    Toast.makeText(getBaseContext(), getString(R.string.started_fishing),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getBaseContext(), getString(R.string.stopped_fishing),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(Fish1FormsActivity.this);
                final Calendar mostRecentSunday = Calendar.getInstance();
                mostRecentSunday.add(Calendar.DATE, -1 * (mostRecentSunday.get(Calendar.DAY_OF_WEEK) - 1));
                mostRecentSunday.set(Calendar.HOUR_OF_DAY, 0);
                mostRecentSunday.set(Calendar.MINUTE, 0);
                mostRecentSunday.set(Calendar.SECOND, 0);
                final Calendar sundayPreviousToMostRecent = (Calendar) mostRecentSunday.clone();
                sundayPreviousToMostRecent.add(Calendar.DATE, -7);
                DateFormat df = new SimpleDateFormat("dd MMM");
                selectedWeekStart = null;
                final CharSequence[] items = {
                        String.format(getString(R.string.this_week), df.format(mostRecentSunday.getTime())),
                        String.format(getString(R.string.last_week), df.format(sundayPreviousToMostRecent.getTime())),
                        getString(R.string.other_week)
                };
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which <= 1) {
                            Intent i = new Intent(Fish1FormsActivity.this, EditFish1FormActivity.class);
                            if (which == 0) selectedWeekStart = mostRecentSunday;
                            else if (which == 1) selectedWeekStart = sundayPreviousToMostRecent;
                            i.putExtra("start_date", selectedWeekStart.getTime());
                            selectedWeekStart.add(Calendar.DATE, 7);
                            i.putExtra("end_date", selectedWeekStart.getTime());
                            startActivity(i);
                        } else {
                            DatePickerDialog picker = new DatePickerDialog(
                                    Fish1FormsActivity.this,
                                    new DatePickerDialog.OnDateSetListener() {
                                        @Override
                                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                            Calendar chosenDate = Calendar.getInstance();
                                            chosenDate.set(year, month, dayOfMonth, 0, 0);
                                            chosenDate.add(Calendar.DATE, -1 * (chosenDate.get(Calendar.DAY_OF_WEEK) - 1));
                                            Intent i = new Intent(Fish1FormsActivity.this, EditFish1FormActivity.class);
                                            i.putExtra("start_date", chosenDate.getTime());
                                            chosenDate.add(Calendar.DATE, 7);
                                            i.putExtra("end_date", chosenDate.getTime());
                                            startActivity(i);
                                        }
                                    },
                                    sundayPreviousToMostRecent.get(Calendar.YEAR),
                                    sundayPreviousToMostRecent.get(Calendar.MONTH),
                                    sundayPreviousToMostRecent.get(Calendar.DAY_OF_MONTH)
                            );
                            picker.show();
                        }
                    }
                });
                builder.create().show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                forms = db.catchDao().getForms();
                adapter = new Fish1FormAdapter(forms, Fish1FormsActivity.this);
                adapter.notifyDataSetChanged();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView = (RecyclerView) findViewById(R.id.form_recycler_view);
                        recyclerView.setLayoutManager(new LinearLayoutManager(getApplication()));
                        recyclerView.setAdapter(adapter);
                    }
                });
            }
        };

        Thread newThread = new Thread(r);
        newThread.start();

        locationSwitch.setChecked(((CatchApplication) this.getApplication()).isTrackingLocation());

        fishingSwitch.setChecked(((CatchApplication) this.getApplication()).isFishing());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (
                requestCode == PERMISSION_REQUEST_FINE_LOCATION
                        && grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ((Switch) findViewById(R.id.toggle_location_tracking)).setChecked(true);
        }
    }
}
