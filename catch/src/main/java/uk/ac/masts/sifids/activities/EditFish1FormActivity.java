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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.ac.masts.sifids.database.CatchDatabase;
import uk.ac.masts.sifids.entities.CatchLocation;
import uk.ac.masts.sifids.entities.CatchPresentation;
import uk.ac.masts.sifids.entities.CatchSpecies;
import uk.ac.masts.sifids.entities.CatchState;
import uk.ac.masts.sifids.entities.Fish1Form;
import uk.ac.masts.sifids.R;
import uk.ac.masts.sifids.entities.Fish1FormRow;
import uk.ac.masts.sifids.entities.FisheryOffice;
import uk.ac.masts.sifids.entities.Gear;
import uk.ac.masts.sifids.providers.GenericFileProvider;
import uk.ac.masts.sifids.tasks.PostDataTask;
import uk.ac.masts.sifids.utilities.Csv;

/**
 * Activity for editing (and creating/deleting) a FISH1 Form.
 * Created by pgm5 on 21/02/2018.
 */
public class EditFish1FormActivity extends EditingActivity implements AdapterView.OnItemSelectedListener {

    //FISH1 Form being edited
    Fish1Form fish1Form;

    //Form elements
    EditText fisheryOffice;
    EditText fisheryOfficeEmail;
    EditText pln;
    EditText vesselName;
    EditText ownerMaster;
    EditText address;
    EditText totalPotsFishing;
    EditText comment;
    Spinner portOfDeparture;
    Spinner portOfLanding;
    Button saveButton;
    Button addRowButton;
    Button deleteButton;

    //Stuff for spinners
    List<String> ports;
    String portOfDepartureValue;
    String portOfLandingValue;
    ArrayAdapter<CharSequence> portOfDepartureAdapter;
    ArrayAdapter<CharSequence> portOfLandingAdapter;

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
                    String rowDates = "Dates not set";
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
                                        .getOffice(
                                                Integer.parseInt(
                                                        EditFish1FormActivity.this.prefs.getString(
                                                                getString(R.string.pref_fishery_office_key),
                                                                "1"
                                                        )
                                                )
                                        );
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
                        fish1Form.setTotalPotsFishing(
                                Integer.parseInt(
                                        EditFish1FormActivity.this.prefs.getString(
                                                getString(R.string.pref_total_pots_fishing_key),
                                                "0")));
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
                                //Get location where fishing started...
                                CatchLocation point =
                                        EditFish1FormActivity.this.db.catchDao()
                                                .getFirstFishingLocationBetweenDates(date,
                                                        upper.getTime());
                                if (point != null) {
                                    rows.add(new Fish1FormRow(fish1Form, point));
                                    //Need to check if fishing activity moved into another ICES Area
                                    while (point != null &&
                                            point.getTimestamp().before(upper.getTime())) {
                                        Map<Integer, Double> bounds =
                                                point.getIcesRectangleBounds();
                                        if (bounds == null) {
                                            point = EditFish1FormActivity.this.db.catchDao()
                                                    .getFirstValidIcesFishingLocationBetweenDates(
                                                            point.getTimestamp(), upper.getTime());
                                        }
                                        else {
                                            point = EditFish1FormActivity.this.db.catchDao()
                                                    .getFirstFishingLocationOutsideBoundsBetweenDates(
                                                            point.getTimestamp(),
                                                            upper.getTime(),
                                                            bounds.get(CatchLocation.LOWER_LAT),
                                                            bounds.get(CatchLocation.UPPER_LAT),
                                                            bounds.get(CatchLocation.LOWER_LONG),
                                                            bounds.get(CatchLocation.UPPER_LONG));
                                        }
                                        if (point != null) {
                                            rows.add(new Fish1FormRow(fish1Form, point));
                                        }
                                    }
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
        fisheryOffice = findViewById(R.id.fishery_office);
        fisheryOfficeEmail = findViewById(R.id.fishery_office_email);
        pln = findViewById(R.id.pln);
        vesselName = findViewById(R.id.vessel_name);
        ownerMaster = findViewById(R.id.owner_master);
        address = findViewById(R.id.address);
        totalPotsFishing = findViewById(R.id.total_pots_fishing);
        comment = findViewById(R.id.comment);
        //Database queries can't be run on the UI thread
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Set portsSet = new HashSet<String>();
                portsSet.add(Integer.toString(EditFish1FormActivity.this.prefs.getInt(
                        getString(R.string.pref_port_key),0)));
                ports = EditFish1FormActivity.this.db.catchDao().getPortNames(portsSet);
            }
        };
        Thread newThread = new Thread(r);
        newThread.start();
        try {
            newThread.join();
        } catch (InterruptedException ie) {
            returnToFish1FormsActivity(getString(R.string.fish_1_form_loading_ports_list));
        }

        //Ports are multiple-choice
        portOfDepartureAdapter =
                new ArrayAdapter(this,
                        android.R.layout.simple_list_item_activated_1, ports);
        portOfDepartureAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        portOfDeparture = findViewById(R.id.port_of_departure);
        portOfDeparture.setAdapter(portOfDepartureAdapter);
        portOfDeparture.setOnItemSelectedListener(this);

        portOfLandingAdapter =
                new ArrayAdapter(this,
                        android.R.layout.simple_list_item_activated_1, ports);
        portOfLandingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        portOfLanding = findViewById(R.id.port_of_landing);
        portOfLanding.setAdapter(portOfLandingAdapter);
        portOfLanding.setOnItemSelectedListener(this);

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
            fisheryOffice.setText(fish1Form.getFisheryOffice());
            fisheryOfficeEmail.setText(fish1Form.getEmail());
            pln.setText(fish1Form.getPln());
            vesselName.setText(fish1Form.getVesselName());
            ownerMaster.setText(fish1Form.getOwnerMaster());
            address.setText(fish1Form.getAddress());
            totalPotsFishing.setText(Integer.toString(fish1Form.getTotalPotsFishing()));
            portOfDeparture.setSelection(
                    portOfDepartureAdapter.getPosition(fish1Form.getPortOfDeparture()));
            portOfLanding.setSelection(
                    portOfLandingAdapter.getPosition(fish1Form.getPortOfDeparture()));
            comment.setText(fish1Form.getCommentsAndBuyersInformation());
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
                    createAndEmailFile();
                }
            }
        });
    }

    /**
     * Handover to email app, with details of email to be sent, including attachment
     */
    private void createAndEmailFile() {
        Intent emailIntent = new Intent();
        emailIntent.setAction(Intent.ACTION_SEND);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent.setType("vnd.android.cursor.dir/email");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{
                fish1Form.getEmail(),
                prefs.getString(getString(R.string.pref_owner_master_email_key), ""),
                getString(R.string.email)
        });
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fish_1_form_email_subject));
        emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.fish_1_form_email_text));
        File csvFile = createFileToSend();
        emailIntent.putExtra(Intent.EXTRA_STREAM, GenericFileProvider.getUriForFile(
                this, "uk.ac.masts.sifids", csvFile));
        PostDataTask.postFish1Form(getApplicationContext(), csvFile);
        startActivityForResult(emailIntent, 101);
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
        //Only write to the database if something has changed (or form is new)
        if (
                create || fish1Form.setFisheryOffice(fisheryOffice.getText().toString())
                        || fish1Form.setEmail(fisheryOfficeEmail.getText().toString())
                        || fish1Form.setPln(pln.getText().toString())
                        || fish1Form.setVesselName(vesselName.getText().toString())
                        || fish1Form.setOwnerMaster(ownerMaster.getText().toString())
                        || fish1Form.setAddress(address.getText().toString())
                        || fish1Form.setTotalPotsFishing(
                        Integer.parseInt(totalPotsFishing.getText().toString()))
                        || fish1Form.setCommentsAndBuyersInformation(
                        comment.getText().toString())
                        || fish1Form.setPortOfDeparture(portOfDepartureValue)
                        || fish1Form.setPortOfLanding(portOfLandingValue)
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
     * Handle selection of items in spinners.
     *
     * @param parent
     * @param view
     * @param pos
     * @param id
     */
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        switch (parent.getId()) {
            case R.id.port_of_departure:
                this.portOfDepartureValue = parent.getItemAtPosition(pos).toString();
                break;
            case R.id.port_of_landing:
                this.portOfLandingValue = parent.getItemAtPosition(pos).toString();
                break;
        }
        portOfDepartureAdapter.notifyDataSetChanged();

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
                            getString(R.string.csv_fishery_office),
                            this.fisheryOffice.getText().toString()));
            writer.newLine();
            writer.write(
                    String.format(
                            getString(R.string.csv_email),
                            this.fisheryOfficeEmail.getText().toString()));
            writer.newLine();
            writer.write(
                    String.format(
                            getString(R.string.csv_port_of_departure),
                            this.portOfDepartureValue));
            writer.newLine();
            writer.write(
                    String.format(
                            getString(R.string.csv_port_of_landing),
                            this.portOfLandingValue));
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
            writer.write(
                    String.format(
                            getString(R.string.csv_total_pots_fishing),
                            totalPotsFishing.getText().toString()));
            writer.newLine();
            writer.write(
                    String.format(
                            getString(R.string.csv_comments_buyers_information),
                            comment.getText().toString()));
            writer.newLine();
            //Do the header row
            writer.newLine();
            writer.write(getString(R.string.csv_header_row));
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
                rowToWrite = Csv.appendToCsvRow(rowToWrite,
                        formRow.getIcesArea(), true, this);
                final String rowSoFar = rowToWrite;
                //Need another thread for the database request
                Callable<String> c = new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        Gear gear = EditFish1FormActivity.this
                                .db.catchDao().getGearById(formRow.getGearId());
                        CatchSpecies species = EditFish1FormActivity.this
                                .db.catchDao().getSpeciesById(formRow.getSpeciesId());
                        CatchState state = EditFish1FormActivity.this
                                .db.catchDao().getStateById(formRow.getStateId());
                        CatchPresentation presentation = EditFish1FormActivity.this
                                .db.catchDao().getPresentationById(formRow.getPresentationId());
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
                        if (species != null) {
                            row = Csv.appendToCsvRow(
                                    row, species.toString(), true,
                                    EditFish1FormActivity.this);
                        } else {
                            row = Csv.appendToCsvRow(
                                    row, null, false,
                                    EditFish1FormActivity.this);
                        }
                        if (state != null) {
                            row = Csv.appendToCsvRow(
                                    row, state.getName(), true,
                                    EditFish1FormActivity.this);
                        } else {
                            row = Csv.appendToCsvRow(
                                    row, null, false,
                                    EditFish1FormActivity.this);
                        }
                        if (presentation != null) {
                            row = Csv.appendToCsvRow(
                                    row, presentation.getName(), true,
                                    EditFish1FormActivity.this);
                        } else {
                            row = Csv.appendToCsvRow(
                                    row, null, false,
                                    EditFish1FormActivity.this);
                        }
                        return row;
                    }
                };
                ExecutorService service = Executors.newSingleThreadExecutor();
                Future<String> future = service.submit(c);
                try {
                    rowToWrite = future.get();
                } catch (Exception e) {
                    Toast.makeText(getBaseContext(),
                            getString(R.string.csv_not_saved), Toast.LENGTH_LONG).show();
                }
                rowToWrite = Csv.appendToCsvRow(
                        rowToWrite, formRow.getWeight(), false, this);
                rowToWrite = Csv.appendToCsvRow(
                        rowToWrite, formRow.isDis(), false, this);
                rowToWrite = Csv.appendToCsvRow(
                        rowToWrite, formRow.isBms(), false, this);
                rowToWrite = Csv.appendToCsvRow(
                        rowToWrite,
                        formRow.getNumberOfPotsHauled(), false, this);
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
                    createAndEmailFile();
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
