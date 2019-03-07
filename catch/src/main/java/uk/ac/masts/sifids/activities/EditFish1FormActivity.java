package uk.ac.masts.sifids.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.ac.masts.sifids.database.CatchDatabase;
import uk.ac.masts.sifids.entities.CatchLocation;
import uk.ac.masts.sifids.entities.CatchSpecies;
import uk.ac.masts.sifids.entities.Fish1Form;
import uk.ac.masts.sifids.R;
import uk.ac.masts.sifids.entities.Fish1FormRow;
import uk.ac.masts.sifids.entities.Fish1FormRowSpecies;
import uk.ac.masts.sifids.entities.FisheryOffice;
import uk.ac.masts.sifids.entities.Gear;
import uk.ac.masts.sifids.entities.Port;
import uk.ac.masts.sifids.tasks.PostDataTask;
import uk.ac.masts.sifids.utilities.Csv;

/**
 * Activity for editing (and creating/deleting) a FISH1 Form.
 * Created by pgm5 on 21/02/2018.
 */
public class EditFish1FormActivity extends EditingActivity {

    //FISH1 Form being edited
    Fish1Form fish1Form;

    //Form elements
    TextView port;
    EditText pln;
    EditText vesselName;
    EditText ownerMaster;
    EditText address;
    Button saveButton;
    Button addRowButton;
    Button deleteButton;
    Port portValue;

    //Associated FISH1 Form Rows
    List<Fish1FormRow> formRows;
    public static RecyclerView recyclerView;
    public static RecyclerView.Adapter adapter;

    //Permission request needs a value
    final static int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 6954;

    //Key for saving form state
    final static String INSTANCE_FORM_ID = "instanceFormId";

    /**
     * Runs when activity is created
     *
     * @param savedInstanceState Activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_edit_fish_1_form);

        super.onCreate(savedInstanceState);

        //Set up the floating action button
        this.doFab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Handle associated FISH1 Form rows
        this.doRows();

        if (this.fish1Form != null) {
            db = CatchDatabase.getInstance(getApplicationContext());
            Callable<String> c = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String rowDates = getString(R.string.fish_1_form_header);
                    Calendar cal = Calendar.getInstance();
                    Date lowerDate = db.catchDao().getDateOfEarliestRow(fish1Form.getId());
                    if (lowerDate != null) {
                        cal.setTime(lowerDate);
                        cal.add(Calendar.DATE, -1 * (cal.get(Calendar.DAY_OF_WEEK) - 1));
                        rowDates = new SimpleDateFormat(getApplicationContext().getString(R.string.dmonthy)).format(cal.getTime());
                        rowDates += " - ";
                        cal.add(Calendar.DATE, 6);
                        rowDates += new SimpleDateFormat(getApplicationContext().getString(R.string.dmonthy)).format(cal.getTime());
                    }
                    return rowDates;
                }
            };
            ExecutorService service = Executors.newSingleThreadExecutor();
            Future<String> future = service.submit(c);
            try {
                String headerString = future.get();
                TextView header = findViewById(R.id.fish_1_form_header);
                header.setText(headerString);
            } catch (Exception e) {}
        }
    }

    /**
     * Handles whatever has been passed to this activity by the previous one
     */
    protected void processIntent() {
        final Bundle extras = getIntent().getExtras();
        String error_msg;
        Runnable r;
        if (extras != null) {
            //Load form with supplied id
            if (extras.get(Fish1Form.ID) != null) {
                final int id = (Integer) extras.get(Fish1Form.ID);
                //Database queries can't be run on the UI thread
                r = new Runnable() {
                    @Override
                    public void run() {
                        fish1Form = EditFish1FormActivity.this.db.catchDao().getForm(id);
                    }
                };
                error_msg = getString(R.string.fish_1_form_error_retrieving_from_database);
            } else {
                //No ID supplied - create new form
                fish1Form = new Fish1Form();
                //Database queries can't be run on the UI thread
                r = new Runnable() {
                    @Override
                    public void run() {
                        //Use user preferences to create form
                        FisheryOffice fisheryOfficeObject =
                                EditFish1FormActivity.this.db.catchDao()
                                        .getOffice();
                        if (fisheryOfficeObject != null) {
                            fish1Form.setFisheryOffice(
                                    String.format(
                                            getString(R.string.fish_1_form_fishery_office_string),
                                            fisheryOfficeObject.getName(),
                                            fisheryOfficeObject.getAddress()
                                    )
                            );
                            fish1Form.setEmail(fisheryOfficeObject.getEmail());
                        }
                        fish1Form.setPln(
                                EditFish1FormActivity.this.prefs.getString(
                                        getString(R.string.pref_vessel_pln_key), ""));
                        fish1Form.setVesselName(
                                EditFish1FormActivity.this.prefs.getString(
                                        getString(R.string.pref_vessel_name_key), ""));
                        fish1Form.setOwnerMaster(
                                EditFish1FormActivity.this.prefs.getString(
                                        getString(R.string.pref_owner_master_name_key),
                                        ""));
                        fish1Form.setAddress(
                                EditFish1FormActivity.this.prefs.getString(
                                        getString(R.string.pref_owner_master_address_key),
                                        ""));
                        long[] ids = EditFish1FormActivity.this.db
                                .catchDao().insertFish1Forms(fish1Form);
                        fish1Form = EditFish1FormActivity.this.db.catchDao().getForm((int) ids[0]);
                        //Have dates been supplied with which to create form rows?
                        if (
                                extras.get(Fish1Form.START_DATE) != null
                                        && extras.get(Fish1Form.START_DATE) instanceof java.util.Date
                                        && extras.get(Fish1Form.END_DATE) != null
                                        && extras.get(Fish1Form.END_DATE) instanceof java.util.Date
                                ) {
                            Calendar start = Calendar.getInstance();
                            start.setTime((Date) extras.get(Fish1Form.START_DATE));
                            Calendar end = Calendar.getInstance();
                            end.setTime((Date) extras.get(Fish1Form.END_DATE));
                            List<Fish1FormRow> rows = new ArrayList<>();
                            //For each day in the period...
                            for (
                                    Date date = start.getTime();
                                    start.before(end);
                                    start.add(Calendar.DATE, 1), date = start.getTime()) {
                                Calendar upper = Calendar.getInstance();
                                upper.setTime(date);
                                upper.add(Calendar.DATE, 1);
                                //Get all locations
                                List<CatchLocation> locations =
                                        EditFish1FormActivity.this.db.catchDao()
                                                .getLocationsBetween(date, upper.getTime());
                                //Get furthest point
                                CatchLocation furthestLocation = null;
                                double greatestDistance = 0.0;
                                for(CatchLocation location : locations ) {
                                    double distanceFromPort = location.getDistanceFromPort();
                                    if (distanceFromPort > greatestDistance) {
                                        greatestDistance = distanceFromPort;
                                        furthestLocation = location;
                                    }
                                }
                                if (furthestLocation != null) {
                                    rows.add(new Fish1FormRow(fish1Form, furthestLocation));
                                }
                            }
                            EditFish1FormActivity.this.db.catchDao().insertFish1FormRows(rows);
                        }
                    }
                };
                error_msg = getString(R.string.fish_1_form_error_creating_new_form);
            }
            Thread newThread = new Thread(r);
            newThread.start();
            try {
                newThread.join();
            } catch (InterruptedException ie) {
                returnToFish1FormsActivity(error_msg);
            }
        }
    }

    /**
     * Build the user form - bind variables to XML elements
     */
    protected void buildForm() {
        port = findViewById(R.id.port);
        pln = findViewById(R.id.pln);
        vesselName = findViewById(R.id.vessel_name);
        ownerMaster = findViewById(R.id.owner_master);
        address = findViewById(R.id.address);
        //Database queries can't be run on the UI thread
        Runnable r = new Runnable() {
            @Override
            public void run() {
                portValue = EditFish1FormActivity.this.db.catchDao().getPort();
            }
        };
        Thread newThread = new Thread(r);
        newThread.start();
        try {
            newThread.join();
        } catch (InterruptedException ie) {
            returnToFish1FormsActivity(getString(R.string.fish_1_form_loading_ports_list));
        }
        port.setText(String.format(getString(R.string.fish_1_form_port), portValue.getName()));
        saveButton = findViewById(R.id.save_form_button);
        addRowButton = findViewById(R.id.add_row_button);
        deleteButton = findViewById(R.id.delete_form_button);

        //Get any existing values from the FISH1 Form
        this.applyExistingValues();

        //Set listeners for the various buttons
        this.setListeners();
    }

    /**
     * Display existing values from FISH1 Form
     */
    private void applyExistingValues() {
        if (fish1Form != null) {
            pln.setText(fish1Form.getPln());
            vesselName.setText(fish1Form.getVesselName());
            ownerMaster.setText(fish1Form.getOwnerMaster());
            address.setText(fish1Form.getAddress());
        }
    }

    /**
     * Set listeners for the "add row", "save" and "delete" buttons
     */
    private void setListeners() {

        //When adding a row, save first, then go to EditFish1FormRowActivity with form id
        addRowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveForm();
                Intent i = new Intent(EditFish1FormActivity.this,
                        EditFish1FormRowActivity.class);
                i.putExtra(Fish1FormRow.FORM_ID, fish1Form.getId());
                startActivity(i);
            }
        });

        //Save the form and go back to Fish1FormsActivity
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveForm();
                returnToFish1FormsActivity(null);
            }
        });

        //Delete form
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteForm();
            }
        });


    }

    /**
     * Display rows associated with this form
     */
    private void doRows() {
        if (fish1Form != null) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    formRows = EditFish1FormActivity.this.db.catchDao()
                            .getRowsForForm(fish1Form.getId());
                    adapter = new Fish1FormRowAdapter(formRows, EditFish1FormActivity.this);
                    adapter.notifyDataSetChanged();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView = findViewById(R.id.form_row_recycler_view);
                            recyclerView.setLayoutManager(
                                    new LinearLayoutManager(getApplication()));
                            recyclerView.setAdapter(adapter);
                        }
                    });
                }
            };
            Thread newThread = new Thread(r);
            newThread.start();
        }
    }

    /**
     * Set up the floating action button for emailing the form
     */
    private void doFab() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Need to check app has correct permissions to save attachment
                if (
                        ContextCompat.checkSelfPermission(
                                EditFish1FormActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            EditFish1FormActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    uploadFile();
                }
            }
        });
    }

    /**
     * Handover to email app, with details of email to be sent, including attachment
     */
    private void uploadFile() {
        File csvFile = createFileToSend();
        PostDataTask.postFish1Form(getApplicationContext(), csvFile);
    }

    /**
     * Save form to the database
     */
    private void saveForm() {

        //Need to keep track of whether we need an INSERT or an UPDATE
        boolean create = false;
        if (fish1Form == null) {
            create = true;
            fish1Form = new Fish1Form();
        }
        fish1Form.setPortOfDeparture(portValue.getName());
        fish1Form.setPortOfLanding(portValue.getName());
        //Only write to the database if something has changed (or form is new)
        if (
                create | fish1Form.setPln(pln.getText().toString())
                        | fish1Form.setVesselName(vesselName.getText().toString())
                        | fish1Form.setOwnerMaster(ownerMaster.getText().toString())
                        | fish1Form.setAddress(address.getText().toString())
                ) {
            if (create) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        EditFish1FormActivity.this.db.catchDao().insertFish1Forms(fish1Form);
                    }
                });
            } else {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        EditFish1FormActivity.this.db.catchDao().updateFish1Forms(fish1Form);
                    }
                });
            }
        }
        if (!pln.getText().toString().equals("")) {
            prefs.edit().putString(
                    getString(R.string.pref_vessel_pln_key), pln.getText().toString()).commit();
        }
        if (!vesselName.getText().toString().equals("")) {
            prefs.edit().putString(
                    getString(R.string.pref_vessel_name_key),
                    vesselName.getText().toString()).commit();
        }
        if (!ownerMaster.getText().toString().equals("")) {
            prefs.edit().putString(
                    getString(R.string.pref_owner_master_name_key),
                    ownerMaster.getText().toString()).commit();
        }
        if (!address.getText().toString().equals("")) {
            prefs.edit().putString(
                    getString(R.string.pref_owner_master_address_key),
                    address.getText().toString()).commit();
        }
    }

    /**
     * Delete form and return to Fish1FormsActivity
     */
    private void deleteForm() {

        if (fish1Form != null) {
            this.confirmDialog();
        } else {
            returnToFish1FormsActivity(null);
        }
    }

    /**
     * Handles dialog for confirmation of deletion
     */
    private void confirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage(getString(R.string.fish_1_form_deletion_confirmation_message))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                EditFish1FormActivity.this.db
                                        .catchDao().deleteFish1Form(fish1Form.getId());
                            }
                        };
                        Thread newThread = new Thread(r);
                        newThread.start();
                        try {
                            newThread.join();
                        } catch (InterruptedException ie) {
                            returnToFish1FormsActivity(
                                    getString(R.string.fish_1_form_error_deleting_form));
                        }
                        Intent i = new Intent(EditFish1FormActivity.this,
                                Fish1FormsActivity.class);
                        //Make sure form doesn't still appear in Fish1FormsActivity
                        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        EditFish1FormActivity.this.finish();
                        EditFish1FormActivity.this.startActivity(i);
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    /**
     * Handle non-selection in spinners.
     *
     * @param parent
     */
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    /**
     * Create CSV file for attaching to email
     *
     * @return CSV file
     */
    private File createFileToSend() {
        //Save to the database first
        this.saveForm();
        File file = null;
        try {
            file = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fish1Form.getCsvFileName());
            FileWriter fw = new FileWriter(file);
            BufferedWriter writer = new BufferedWriter(fw);
            //Write form info as comments above CSV rows
            writer.write(
                    String.format(
                            getString(R.string.csv_port_of_departure),
                            this.portValue.getName()));
            writer.newLine();
            writer.write(
                    String.format(
                            getString(R.string.csv_port_of_landing),
                            this.portValue.getName()));
            writer.newLine();
            writer.write(
                    String.format(
                            getString(R.string.csv_pln), pln.getText().toString()));
            writer.newLine();
            writer.write(
                    String.format(
                            getString(R.string.csv_vessel_name),
                            vesselName.getText().toString()));
            writer.newLine();
            writer.write(
                    String.format(
                            getString(R.string.csv_owner_master),
                            ownerMaster.getText().toString()));
            writer.newLine();
            writer.write(
                    String.format(
                            getString(R.string.csv_address),
                            address.getText().toString()));
            writer.newLine();
            //Do the header row
            writer.newLine();
            Callable<List<CatchSpecies>> callable = new Callable<List<CatchSpecies>>() {
                @Override
                public List<CatchSpecies> call() throws Exception {
                    return db.catchDao().getSpecies();
                }
            };
            ExecutorService service = Executors.newSingleThreadExecutor();
            Future<List<CatchSpecies>> future = service.submit(callable);
            String speciesHeaders = "";
            try {
                List<CatchSpecies> speciesList = future.get();
                for (CatchSpecies species : speciesList) {
                    speciesHeaders += species.getSpeciesName();
                    speciesHeaders += ",";
                }
            } catch (Exception e) {
                Toast.makeText(getBaseContext(),
                        getString(R.string.csv_not_saved), Toast.LENGTH_LONG).show();
            }
            writer.write(String.format(getString(R.string.csv_header_row),speciesHeaders));
            writer.newLine();
            //Write the rows
            for (final Fish1FormRow formRow : formRows) {
                String rowToWrite = "";
                Calendar cal = Calendar.getInstance();
                if (formRow.getFishingActivityDate() != null) {
                    cal.setTime(formRow.getFishingActivityDate());
                } else {
                    cal = null;
                }
                //strip leading comma
                rowToWrite = Csv.appendToCsvRow(rowToWrite, cal, false, this);
                rowToWrite = Csv.appendToCsvRow(rowToWrite,
                        formRow.getCoordinates(), false, this);
                final String rowSoFar = rowToWrite;
                //Need another thread for the database request
                Callable<String> c = new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        Gear gear = EditFish1FormActivity.this
                                .db.catchDao().getGearById(formRow.getGearId());
                        List<CatchSpecies> speciesList= EditFish1FormActivity.this
                                .db.catchDao().getSpecies();
                        String row = rowSoFar;
                        if (gear != null) {
                            row = Csv.appendToCsvRow(
                                    row, gear.getName(), true,
                                    EditFish1FormActivity.this);
                        } else {
                            row = Csv.appendToCsvRow(
                                    row, null, false,
                                    EditFish1FormActivity.this);
                        }
                        row = Csv.appendToCsvRow(
                                row, formRow.getMeshSize(), false,
                                EditFish1FormActivity.this);
                        for (CatchSpecies species : speciesList) {
                            Fish1FormRowSpecies rowSpecies = EditFish1FormActivity.this
                                    .db.catchDao()
                                    .getSpeciesEntryForRow(formRow.getId(), species.getId());
                            if (rowSpecies != null && rowSpecies.getWeight() != null) {
                                row = Csv.appendToCsvRow(
                                        row, rowSpecies.getWeight(), false,
                                        EditFish1FormActivity.this);
                            }
                            else {
                                row = Csv.appendToCsvRow(row,"",false,
                                        EditFish1FormActivity.this);
                            }
                        }
                        row = Csv.appendToCsvRow(
                                row, formRow.getNetSize(), false,
                                EditFish1FormActivity.this);
                        return row;
                    }
                };
                Future<String> f = service.submit(c);
                try {
                    rowToWrite = f.get();
                } catch (Exception e) {
                    Toast.makeText(getBaseContext(),
                            getString(R.string.csv_not_saved), Toast.LENGTH_LONG).show();
                }
                cal = Calendar.getInstance();
                if (formRow.getLandingOrDiscardDate() != null) {
                    cal.setTime(formRow.getLandingOrDiscardDate());
                } else {
                    cal = null;
                }
                rowToWrite = Csv.appendToCsvRow(rowToWrite, cal, false, this);
                rowToWrite = Csv.appendToCsvRow(rowToWrite, formRow.getTransporterRegEtc(),
                        true, this);
                writer.write(rowToWrite);
                writer.newLine();
            }
            writer.close();
            fw.close();
            Toast.makeText(getBaseContext(),
                    String.format(getString(R.string.csv_saved), file.getPath()),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), getString(R.string.csv_not_saved), Toast.LENGTH_LONG)
                    .show();
        }
        return file;
    }

    /**
     * Process permissions request result - if granted, then create and email file
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    uploadFile();
                }
            }, 500);
        }
    }

    /**
     * Returns the user to Fish1FormsActivity, displaying a message (if supplied).
     *
     * @param msg
     */
    private void returnToFish1FormsActivity(String msg) {
        if (msg != null && msg.length() > 0) {
            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
        }
        Intent intent = new Intent(this, Fish1FormsActivity.class);
        startActivity(intent);
        finish();
    }
}
