package uk.ac.masts.sifids.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.ac.masts.sifids.R;
import uk.ac.masts.sifids.entities.CatchLocation;
import uk.ac.masts.sifids.entities.CatchSpecies;
import uk.ac.masts.sifids.entities.EntityWithId;
import uk.ac.masts.sifids.entities.Fish1Form;
import uk.ac.masts.sifids.entities.Fish1FormRow;
import uk.ac.masts.sifids.entities.Fish1FormRowSpecies;
import uk.ac.masts.sifids.entities.Gear;

/**
 * Activity for editing (and creating/deleting) a FISH1 Form Row.
 * Created by pgm5 on 21/02/2018.
 */
public class EditFish1FormRowActivity extends EditingActivity implements AdapterView.OnItemSelectedListener, DatePickerDialog.OnDateSetListener {

    //FISH1 Form Row being edited
    Fish1FormRow fish1FormRow;
    Map<Integer, Fish1FormRowSpecies> fish1FormRowSpeciesMap;

    //ID of the parent FISH1 Form
    int formId;

    //Form elements
    TextView fishingActivityDateDisplay;
    Date fishingActivityDate;
    TextView location;
    Double latitude;
    Double longitude;
    EditText meshSize;
    EditText netSize;
    TextView landingOrDiscardDateDisplay;
    Date landingOrDiscardDate;
    EditText transporterRegEtc;
    Button saveButton;
    Button deleteButton;

    Map<Integer, EditText> speciesWeightFields;

    //Stuff for spinners
    Map<String, Spinner> spinners;
    Map<String, Adapter> adapters;
    Map<String, List> spinnerLists;
    final String GEAR_KEY = "gear";
    int gearIdValue;
    Date minDate;
    Date maxDate;

    /**
     * Runs when activity is created
     *
     * @param savedInstanceState Activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_edit_fish_1_form_row);

        super.onCreate(savedInstanceState);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                Date currentLowest = db.catchDao().getDateOfEarliestRow(formId);
                if (currentLowest != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(currentLowest);
                    cal.add(Calendar.DATE, -1 * (cal.get(Calendar.DAY_OF_WEEK) - 1));
                    minDate = cal.getTime();
                    cal.add(Calendar.DATE, 6);
                    maxDate = cal.getTime();
                }
            }
        };
        Thread newThread = new Thread(r);
        newThread.start();
    }

    protected void processIntent() {
        Intent intent = this.getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (!extras.isEmpty() && extras.containsKey(Fish1FormRow.FORM_ID)) {
                this.formId = extras.getInt(Fish1FormRow.FORM_ID);
            }
            if (!extras.isEmpty() && extras.containsKey(Fish1FormRow.ID)) {
                final int id = extras.getInt(Fish1FormRow.ID);
                fish1FormRowSpeciesMap = new HashMap<>();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        fish1FormRow = EditFish1FormRowActivity.this.db.catchDao().getFormRow(id);
                        List<Fish1FormRowSpecies> fish1FormRowSpeciesList =
                                EditFish1FormRowActivity.this.db.catchDao().getSpeciesForRow(id);
                        for (Fish1FormRowSpecies species : fish1FormRowSpeciesList) {
                            fish1FormRowSpeciesMap.put(species.getSpeciesId(), species);
                        }
                    }
                };

                Thread newThread = new Thread(r);
                newThread.start();
                try {
                    newThread.join();
                } catch (InterruptedException ie) {

                }
            }
        }
    }

    protected void buildForm() {

        this.spinnerLists = new HashMap();

        this.spinnerLists.put(GEAR_KEY, new ArrayList<Gear>());

        this.adapters = new HashMap();
        this.spinners = new HashMap();

        this.loadOptions();

        fishingActivityDateDisplay = (TextView) findViewById(R.id.fishing_activity_date);

        location = findViewById(R.id.fishing_location);

        this.createSpinner(GEAR_KEY, R.id.gear);
        meshSize = (EditText) findViewById(R.id.mesh_size);
        meshSize = (EditText) findViewById(R.id.net_size);

        Callable<List<CatchSpecies>> c = new Callable<List<CatchSpecies>>() {
            @Override
            public List<CatchSpecies> call() {
                return db.catchDao().getSpecies();
            }
        };
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<List<CatchSpecies>> future = service.submit(c);
        try {
            List<CatchSpecies> speciesList = future.get();
            int index = 1;
            Resources res = getResources();
            speciesWeightFields = new HashMap<>();
            for (CatchSpecies species : speciesList) {
                TextView speciesName = (TextView) findViewById(
                        res.getIdentifier(
                                "species" + index, "id",
                                getApplicationContext().getPackageName()));
                speciesName.setText(species.getSpeciesName());
                speciesWeightFields.put(species.getId(), (EditText) findViewById(
                        res.getIdentifier(
                                "weight" + index, "id",
                                getApplicationContext().getPackageName())));
                index++;
            }
        }
        catch (Exception e) {
        }

        landingOrDiscardDateDisplay = (TextView) findViewById(R.id.landing_or_discard_date);
        transporterRegEtc = (EditText) findViewById(R.id.transporter_reg_etc);

        saveButton = (Button) findViewById(R.id.save_form_row_button);
        deleteButton = (Button) findViewById(R.id.delete_form_row_button);

        this.applyExistingValues();

        this.setListeners();
    }

    private void applyExistingValues() {

        if (fish1FormRow != null && fish1FormRow.getFishingActivityDate() != null) {
            fishingActivityDate = fish1FormRow.getFishingActivityDate();
            this.updateDateDisplay(fishingActivityDate, fishingActivityDateDisplay,
                    getString(R.string.fish_1_form_row_fishing_activity_date));
        }
        if (fish1FormRow != null) {
            formId = fish1FormRow.getFormId();
            latitude = fish1FormRow.getLatitude();
            longitude = fish1FormRow.getLongitude();
            location.setText(
                    String.format(getString(R.string.fish_1_form_row_location), fish1FormRow.getCoordinates()));
            for (int i = 0; i < adapters.get(GEAR_KEY).getCount(); i++) {
                if (fish1FormRow.getGearId() != null
                        && ((Gear) adapters.get(GEAR_KEY).getItem(i)).getId()
                        == fish1FormRow.getGearId())
                    spinners.get(GEAR_KEY).setSelection(i);
            }
            for (int i = 0; i < adapters.get(GEAR_KEY).getCount(); i++) {
                if (fish1FormRow.getGearId() != null
                        && ((Gear) adapters.get(GEAR_KEY).getItem(i)).getId()
                        == fish1FormRow.getGearId())
                    spinners.get(GEAR_KEY).setSelection(i);
            }
            meshSize.setText(Integer.toString(fish1FormRow.getMeshSize()));
            netSize.setText(Integer.toString(fish1FormRow.getNetSize()));
        } else {
            meshSize.setText(this.prefs.getString(getString(R.string.pref_mesh_size_key), ""));
            netSize.setText(this.prefs.getString(getString(R.string.pref_net_size_key), ""));
        }
        if (fish1FormRow != null) {
            for (int rowSpeciesId : fish1FormRowSpeciesMap.keySet()) {
                Fish1FormRowSpecies species = fish1FormRowSpeciesMap.get(rowSpeciesId);
                EditText weightField = speciesWeightFields.get(species.getSpeciesId());
                if (weightField != null && species.getWeight() != null) {
                    weightField.setText(species.getWeight().toString());
                }
            }
        }
        if (fish1FormRow != null
                && fish1FormRow.getLandingOrDiscardDate() != null) {
            landingOrDiscardDate = fish1FormRow.getLandingOrDiscardDate();
            this.updateDateDisplay(landingOrDiscardDate, landingOrDiscardDateDisplay,
                    getString(R.string.fish_1_form_row_landing_or_discard_date));
        }
        if (fish1FormRow != null
                && fish1FormRow.getTransporterRegEtc() != null
                && !fish1FormRow.getTransporterRegEtc().equals("")) {
            transporterRegEtc.setText(fish1FormRow.getTransporterRegEtc());
        } else if (fish1FormRow == null) {
            transporterRegEtc.setText(
                    this.prefs.getString(getString(R.string.pref_buyer_details_key), ""));
        }

    }

    private void setListeners() {

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean create = false;

                if (fish1FormRow == null && formId != 0) {
                    create = true;
                    fish1FormRow = new Fish1FormRow();
                    fish1FormRow.setFormId(formId);
                    fish1FormRowSpeciesMap = new HashMap<>();
                }
                boolean dataEntered = false;
                if (latitude != null && fish1FormRow.setLatitude(latitude)) {
                    dataEntered = true;
                }
                if (longitude != null && fish1FormRow.setLongitude(longitude)) {
                    dataEntered = true;
                }
                if (fish1FormRow.setFishingActivityDate(fishingActivityDate)) {
                    dataEntered = true;
                }
                try {
                    if (fish1FormRow.setGearId(gearIdValue)) {
                        dataEntered = true;
                    }
                } catch (NullPointerException npe) { }
                try {
                    if (fish1FormRow.setMeshSize(Integer.parseInt(meshSize.getText().toString()))) {
                        dataEntered = true;
                    }
                } catch (NumberFormatException nfe) {
                }
                try {
                    if (fish1FormRow.setNetSize(Integer.parseInt(netSize.getText().toString()))) {
                        dataEntered = true;
                    }
                } catch (NumberFormatException nfe) {
                }
                for (final Integer speciesId : speciesWeightFields.keySet()) {
                    String weightString = speciesWeightFields.get(speciesId).getText().toString();
                    if (weightString != null && weightString.length() > 0) {
                        double weight = Double.parseDouble(
                                speciesWeightFields.get(speciesId).getText().toString());
                        if (fish1FormRowSpeciesMap.containsKey(speciesId)) {
                            if (fish1FormRowSpeciesMap.get(speciesId).setWeight(weight)) {
                                dataEntered = true;
                            }
                        } else {
                            Fish1FormRowSpecies fish1FormRowSpecies =
                                    new Fish1FormRowSpecies(fish1FormRow.getId(), speciesId, weight);
                            fish1FormRowSpeciesMap.put(speciesId, fish1FormRowSpecies);
                            dataEntered = true;
                        }
                    }
                }
                if (fish1FormRow.setLandingOrDiscardDate(landingOrDiscardDate)) {
                    dataEntered = true;
                }
                if (fish1FormRow.setTransporterRegEtc(transporterRegEtc.getText().toString())) {
                    dataEntered = true;
                }

                if (dataEntered) {
                    if (create) {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                int rowId = (int) EditFish1FormRowActivity.this.db.catchDao()
                                        .insertFish1FormRow(fish1FormRow);
                                for (Fish1FormRowSpecies rowSpecies : fish1FormRowSpeciesMap.values()) {
                                    rowSpecies.setFormRowId(rowId);
                                }
                                EditFish1FormRowActivity.this.db.catchDao()
                                        .insertFish1FormRowSpecies(fish1FormRowSpeciesMap.values());
                            }
                        };
                        Thread newThread = new Thread(r);
                        newThread.start();
                        try {
                            //Don't want to go back to form before this is saved
                            newThread.join();
                        } catch (InterruptedException ie) {

                        }
                    } else {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                EditFish1FormRowActivity.this.db.catchDao()
                                        .updateFish1FormRows(fish1FormRow);
                                EditFish1FormRowActivity.this.db.catchDao()
                                        .insertFish1FormRowSpecies(fish1FormRowSpeciesMap.values());
                                EditFish1FormRowActivity.this.db.catchDao()
                                        .updateFish1FormRowSpecies(fish1FormRowSpeciesMap.values());
                            }
                        };
                        Thread newThread = new Thread(r);
                        newThread.start();
                        try {
                            //Don't want to go back to form before this is saved
                            newThread.join();
                        } catch (InterruptedException ie) {

                        }
                    }
                }
                EditFish1FormRowActivity.this.returnToEditFish1FormActivity();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteFormRow();
            }
        });
    }

    private void deleteFormRow() {

        if (fish1FormRow != null) {
            this.confirmDialog();
        } else {
            this.returnToEditFish1FormActivity();
        }
    }

    private void confirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage(getString(R.string.fish_1_form_row_deletion_confirmation_message))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                EditFish1FormRowActivity.this.db.catchDao()
                                        .deleteFish1FormRow(fish1FormRow.getId());
                            }
                        };
                        Thread newThread = new Thread(r);
                        newThread.start();
                        try {
                            newThread.join();
                        } catch (InterruptedException ie) {

                        }
                        EditFish1FormRowActivity.this.returnToEditFish1FormActivity();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    private void updateDateDisplay(Date date, TextView display, String prefix) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        display.setText(prefix + new SimpleDateFormat(getString(R.string.dmonthy)).format(cal.getTime()));
    }

    private void updateCoordinatesFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final Date startDate = cal.getTime();
        cal.add(Calendar.DATE, 1);
        final Date endDate = cal.getTime();
        Callable<List<CatchLocation>> c = new Callable<List<CatchLocation>>() {
            @Override
            public List<CatchLocation> call() {
                return db.catchDao().getLocationsBetween(startDate, endDate);
            }
        };
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<List<CatchLocation>> future = service.submit(c);
        try {
            List<CatchLocation> locations = future.get();
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
                latitude = furthestLocation.getLatitude();
                longitude = furthestLocation.getLongitude();
                location.setText(
                        String.format(getString(R.string.fish_1_form_row_location),
                                furthestLocation.getCoordinates()));
            }
        } catch (Exception e) {
        }
    }

    private void loadOptions() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                EditFish1FormRowActivity.this.spinnerLists.put(
                        EditFish1FormRowActivity.this.GEAR_KEY,
                        EditFish1FormRowActivity.this.db.catchDao().getGear());
                List speciesList = EditFish1FormRowActivity.this.db.catchDao().getSpecies();
                for (String idString :
                        EditFish1FormRowActivity.this.prefs.getStringSet(
                                getString(R.string.pref_species_key), new HashSet<String>())) {
                    speciesList = EditFish1FormRowActivity.rearrangeList(
                            speciesList, Integer.parseInt(idString));
                }
            }
        };

        Thread newThread = new Thread(r);
        newThread.start();
        try {
            newThread.join();
        } catch (InterruptedException ie) {

        }
    }

    private static List<EntityWithId> rearrangeList(List<EntityWithId> list, int id) {
        Iterator<EntityWithId> it = list.iterator();
        while (it.hasNext()) {
            EntityWithId item = it.next();
            if (item.getId() == id) {
                it.remove();
                list.add(0, item);
                return list;
            }
        }
        return list;
    }

    private void createSpinner(String mapKey, int widgetId) {
        ArrayAdapter adapter =
                new ArrayAdapter(
                        this, android.R.layout.simple_list_item_activated_1,
                        this.spinnerLists.get(mapKey));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.adapters.put(mapKey, adapter);
        Spinner spinner = findViewById(widgetId);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
        this.spinners.put(mapKey, spinner);
    }

    public void showFishingActivityDatePickerDialog(View v) {
        DialogFragment datePickerFragment = new DatePickerFragment();
        Bundle bundle = new Bundle();
        if (this.minDate != null) {
            bundle.putLong("min", minDate.getTime());
        }
        if (this.maxDate != null) {
            bundle.putLong("max", maxDate.getTime());
        }
        datePickerFragment.setArguments(bundle);
        datePickerFragment.show(getFragmentManager(), "fishing_activity_date");
    }

    public void showLandingOrDiscardDatePickerDialog(View v) {
        DialogFragment datePickerFragment = new DatePickerFragment();
        Bundle bundle = new Bundle();
        if (this.fishingActivityDate != null) {
            bundle.putLong("min", fishingActivityDate.getTime());
        } else if (this.minDate != null) {
            bundle.putLong("min", minDate.getTime());
        }
        if (this.maxDate != null) {
            bundle.putLong("max", maxDate.getTime());
        }
        datePickerFragment.setArguments(bundle);
        datePickerFragment.show(getFragmentManager(), "landing_or_discard_date");
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        switch (parent.getId()) {
            case R.id.gear:
                this.gearIdValue = ((Gear) parent.getItemAtPosition(pos)).getId();
                break;
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        String tag = view.getTag().toString();
        Calendar c = Calendar.getInstance();
        c.set(year, month, day);
        if (tag == "fishing_activity_date") {
            this.fishingActivityDate = c.getTime();
            this.updateDateDisplay(fishingActivityDate, fishingActivityDateDisplay,
                    getString(R.string.fish_1_form_row_fishing_activity_date));
            this.updateCoordinatesFromDate(c.getTime());
        } else if (tag == "landing_or_discard_date") {
            this.landingOrDiscardDate = c.getTime();
            this.updateDateDisplay(landingOrDiscardDate, landingOrDiscardDateDisplay,
                    getString(R.string.fish_1_form_row_landing_or_discard_date));
        }
    }

    public static class DatePickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            DatePickerDialog dialog =
                    new DatePickerDialog(getActivity(),
                            (EditFish1FormRowActivity) getActivity(), year, month, day);
            Bundle bundle = getArguments();
            if (bundle != null && bundle.containsKey("min")) {
                dialog.getDatePicker().setMinDate(bundle.getLong("min"));
            }
            if (bundle != null && bundle.containsKey("max")) {
                dialog.getDatePicker().setMaxDate(bundle.getLong("max"));
            } else {
                dialog.getDatePicker().setMaxDate(c.getTimeInMillis());
            }
            dialog.getDatePicker().setTag(this.getTag());
            return dialog;
        }
    }

    @Override
    public void onBackPressed() {
        this.returnToEditFish1FormActivity();
    }

    private void returnToEditFish1FormActivity() {
        Intent i = new Intent(this, EditFish1FormActivity.class);
        i.putExtra(Fish1Form.ID, this.formId);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        this.startActivity(i);
        this.finish();
    }

    // from https://stackoverflow.com/questions/14212518/is-there-a-way-to-define-a-min-and-max-value-for-edittext-in-android
    private class InputFilterMinMax implements InputFilter {

        private int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public InputFilterMinMax(String min, String max) {
            this.min = Integer.parseInt(min);
            this.max = Integer.parseInt(max);
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                // Remove the string out of destination that is to be replaced
                String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
                // Add the new string in
                newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());
                int input = Integer.parseInt(newVal);
                if (isInRange(min, max, input))
                    return null;
            } catch (NumberFormatException nfe) { }
            return "";
        }

        private boolean isInRange(int a, int b, int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }
    }
}
