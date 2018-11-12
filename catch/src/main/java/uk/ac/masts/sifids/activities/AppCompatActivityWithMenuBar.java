package uk.ac.masts.sifids.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import uk.ac.masts.sifids.CatchApplication;
import uk.ac.masts.sifids.R;

/**
 * Base class to allow MenuBar stuff to be handled in one place for most Activities. Inheritance for
 * SettingsActivity is different; these methods must be implemented separately there.
 */
public abstract class AppCompatActivityWithMenuBar extends AppCompatActivity {

    /**
     * Inflates supplied menu. Should always return true.
     * @param menu Menu to be inflated
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Handles selection of items in the menu
     * @param item Selected item
     * @return true if item is processed successfully
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent intent;
        switch (item.getItemId()) {
            case R.id.activity_catch:
                intent = new Intent(this, Fish1FormsActivity.class);
                startActivity(intent);
                return true;
            case R.id.activity_map:
                intent = new Intent(this, MapActivity.class);
                startActivity(intent);
                return true;
            case R.id.activity_record_observation:
                intent = new Intent(this, RecordObservationActivity.class);
                startActivity(intent);
                return true;
            case R.id.home:
                this.onBackPressed();
                return true;
            case android.R.id.home:
                this.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    protected void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up saveButton in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
}
