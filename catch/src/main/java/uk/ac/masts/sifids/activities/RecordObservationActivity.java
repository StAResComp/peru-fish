package uk.ac.masts.sifids.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.GridLayout;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.ac.masts.sifids.R;
import uk.ac.masts.sifids.database.CatchDatabase;
import uk.ac.masts.sifids.entities.Bycatch;
import uk.ac.masts.sifids.entities.BycatchSpecies;
import uk.ac.masts.sifids.tasks.PostDataTask;

public class RecordObservationActivity extends AppCompatActivityWithMenuBar implements View.OnClickListener {

    CatchDatabase db;
    SharedPreferences prefs;
    ArrayList<LinearLayout> formSections;
    int currentSectionIndex = 0;
    BycatchSpecies animalSeen = null;
    int numberSeen = 0;
    double weightSeen = 0.0;
    Bycatch bycatch = null;

    /**
     * Runs when activity is created
     *
     * @param savedInstanceState Activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_record_observation);

        super.onCreate(savedInstanceState);

        //Set up the action bar/menu
        setupActionBar();

        //Initialise database
        this.db = CatchDatabase.getInstance(getApplicationContext());

        this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        this.setFormSections();

        Callable<List<BycatchSpecies>> c = new Callable<List<BycatchSpecies>>() {
            @Override
            public List<BycatchSpecies> call() {
                return db.catchDao().getBycatchSpecies();
            }
        };
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<List<BycatchSpecies>> future = service.submit(c);
        try {
            List<BycatchSpecies> animals = future.get();
            for (BycatchSpecies animal : animals) {
                addAnimalToGrid(animal);
            }
        } catch (Exception e) {
        }
    }

    private void setFormSections() {
        formSections = new ArrayList<>();
        formSections.add(0, (LinearLayout) findViewById(R.id.obs_what_seen_section));
        formSections.add(1, (LinearLayout) findViewById(R.id.obs_count_section));
        formSections.add(2, (LinearLayout) findViewById(R.id.obs_weight_section));
        formSections.add(3, (LinearLayout) findViewById(R.id.obs_post_submission_section));
    }

    private void nextSection() {
        nextSection(null);
    }

    public void nextSection(View v) {
        if (this.animalSeen != null) {
            formSections.get(currentSectionIndex).setVisibility(View.GONE);
            if ((currentSectionIndex == 0 && animalSeen.getMeasuredBy() == BycatchSpecies.WEIGHT)
                    || currentSectionIndex == 1) {
                currentSectionIndex += 2;
            } else {
                currentSectionIndex++;
            }
            if (currentSectionIndex >= formSections.size()) {
                this.finish();
                this.startActivity(this.getIntent());
            } else {
                formSections.get(currentSectionIndex).setVisibility(View.VISIBLE);
            }
        }
    }

    private void previousSection() {
        previousSection(null);
    }

    public void previousSection(View v) {
        if (currentSectionIndex > 0) {
            formSections.get(currentSectionIndex).setVisibility(View.GONE);
            if (currentSectionIndex == 2) {
                currentSectionIndex -= 2;
            }
            else {
                currentSectionIndex--;
            }
            formSections.get(currentSectionIndex).setVisibility(View.VISIBLE);
        }
    }

    private void addAnimalToGrid(BycatchSpecies animal) {

        ImageButton button = new ImageButton(this);
        button.setImageResource(
                this.getResources().getIdentifier(
                        animal.getName().toLowerCase().replace(" ", "_"),
                        "drawable", this.getPackageName()));
        button.setBackground(null);
        button.setId(animal.getId());
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setTag(animal);
        button.setAdjustViewBounds(true);
        //button.setPadding(dpToPx(1), 0, dpToPx(1), 0);
        LinearLayout.LayoutParams lllp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        button.setLayoutParams(lllp);
        button.setContentDescription(animal.getName());
        button.setOnClickListener(this);

        TextView caption = new TextView(this);
        lllp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        caption.setLayoutParams(lllp);
        caption.setGravity(Gravity.CENTER);
        caption.setText(animal.getName());
        caption.setTag(animal);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        GridLayout.LayoutParams gllp = new GridLayout.LayoutParams();
        gllp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        gllp.width = 0;
        gllp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1);
        wrapper.setLayoutParams(gllp);
        wrapper.setTag(animal);

        wrapper.addView(button);
        wrapper.addView(caption);
        ((GridLayout) findViewById(R.id.obs_animal_image_grid)).addView(wrapper);

    }

    private int dpToPx(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public void onClick(View view) {
        Object tag = view.getTag();
        if (tag instanceof BycatchSpecies) {
            this.animalSeen = (BycatchSpecies) tag;
            this.nextSection();
        }
    }

    public void submitCount(View view) {
        this.numberSeen = Integer.parseInt(
                ((EditText) findViewById(R.id.bycatch_number)).getText().toString());
        submitBycatch(view);
    }

    public void submitWeight(View view) {
        this.weightSeen = Double.parseDouble(
                ((EditText) findViewById(R.id.bycatch_weight)).getText().toString());
        submitBycatch(view);
    }

    public void submitBycatch(View view) {
        if (
                this.animalSeen != null
                        && (this.numberSeen > 0 || this.weightSeen > 0.0)
                ) {
            this.bycatch = new Bycatch();
            bycatch.setBycatchSpeciesId(animalSeen.getId());
            if (animalSeen.getMeasuredBy() == BycatchSpecies.WEIGHT) {
                bycatch.setWeight(weightSeen);
            }
            else {
                bycatch.setNumber(numberSeen);
            }
            this.persistObservation();
            PostDataTask.postBycatch(this, bycatch, new PostDataTask.VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {

                    Runnable r = new Runnable(){
                        @Override
                        public void run() {
                            db.catchDao().markBycatchSubmitted(bycatch.getId());
                        }
                    };

                    Thread newThread= new Thread(r);
                    newThread.start();
                    try {
                        newThread.join();
                    }
                    catch (InterruptedException ie) {

                    }

                    updateSubmissionMessage(true);
                    Toast.makeText(RecordObservationActivity.this,
                            getString(R.string.observation_toast_success),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String result) {
                    updateSubmissionMessage(false);
                    Toast.makeText(RecordObservationActivity.this,
                            getString(R.string.observation_toast_error),
                            Toast.LENGTH_LONG).show();
                }
            });
            nextSection();
        } else {
            Toast.makeText(getBaseContext(),
                    getString(R.string.observation_toast_insufficient),
                    Toast.LENGTH_LONG).show();
        }
    }

    protected void updateSubmissionMessage(boolean success) {
        TextView msgView = findViewById(R.id.obs_submission_msg);
        int[] submissionDetails = {0,0};
        Callable<int[]> c = new Callable<int[]>() {
            @Override
            public int[] call() {
                int[] submissionDetails = new int[2];
                submissionDetails[0] = db.catchDao().countSubmittedBycatches();
                submissionDetails[1] = db.catchDao().countUnsubmittedBycatches();
                return submissionDetails;
            }
        };
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<int[]> future = service.submit(c);
        try {
            submissionDetails = future.get();
        } catch (Exception e) {
        }
        String msg = String.format(getString(R.string.observation_thank_you),
                (success ? getString(R.string.observation_submission_successful) :
                        getString(R.string.observation_submission_unsuccessful)),
                Integer.toString(submissionDetails[0]), Integer.toString(submissionDetails[1]));
        msgView.setText(msg);
    }

    private void persistObservation() {
        if (this.bycatch != null) {
            Callable<Long> c = new Callable<Long>() {
                @Override
                public Long call() {
                    return db.catchDao().insertBycatch(bycatch);
                }
            };
            ExecutorService service = Executors.newSingleThreadExecutor();
            Future<Long> future = service.submit(c);
            try {
                long observationId = future.get();
                bycatch.setId((int) observationId);
            } catch (Exception e) {
            }
        }
    }

    public void reload(View v) {
        this.finish();
        this.startActivity(this.getIntent());
    }

    public void goHome(View v) {
        this.finish();
    }
}
