package uk.ac.masts.sifids.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.ac.masts.sifids.R;
import uk.ac.masts.sifids.database.CatchDatabase;
import uk.ac.masts.sifids.entities.CatchSpecies;
import uk.ac.masts.sifids.entities.Fish1Form;
import uk.ac.masts.sifids.entities.Fish1FormRow;

/**
 * Created by pgm5 on 21/02/2018.
 */

public class Fish1FormRowAdapter extends RecyclerView.Adapter<Fish1FormRowAdapter.ViewHolder> {

    private List<Fish1FormRow> formRows;
    CatchDatabase db;
    Context context;

    public Fish1FormRowAdapter(List<Fish1FormRow> formRows, Context context) {
        this.formRows = formRows;
        this.context = context;
    }

    @Override
    public Fish1FormRowAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.form_row_recycler_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Fish1FormRowAdapter.ViewHolder holder, int position) {
        final Fish1FormRow row = formRows.get(position);
        CatchSpecies species = null;
        db = CatchDatabase.getInstance(holder.itemView.getContext());
        Callable<CatchSpecies> c = new Callable<CatchSpecies>() {
            @Override
            public CatchSpecies call() {
                return db.catchDao().getSpeciesById(row.getSpeciesId());
            }
        };
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<CatchSpecies> future = service.submit(c);
        try {
            species = future.get();
        } catch (Exception e) {
        }
        holder.label.setText((row.toString() != null ? row.toString() : this.context.getString(R.string.fish_1_form_row_tostring_default)) + (species != null ? " " + species.getSpeciesCode() : ""));
        holder.editButton.setTag(R.id.parent_form, row.getFormId());
        holder.editButton.setTag(R.id.form_row_in_question, row.getId());
        holder.duplicateButton.setTag(R.id.parent_form, row.getFormId());
        holder.duplicateButton.setTag(R.id.form_row_in_question, row.getId());
        holder.deleteButton.setTag(R.id.parent_form, row.getFormId());
        holder.deleteButton.setTag(R.id.form_row_in_question, row.getId());
    }

    @Override
    public int getItemCount() {
        return formRows.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView label;
        public Button editButton;
        public Button duplicateButton;
        public Button deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.label);
            editButton = (Button) itemView.findViewById(R.id.btn_edit_form_row);
            editButton.setOnClickListener(this);
            duplicateButton = (Button) itemView.findViewById(R.id.btn_duplicate_form_row);
            duplicateButton.setOnClickListener(this);
            deleteButton = (Button) itemView.findViewById(R.id.btn_delete_form_row);
            deleteButton.setOnClickListener(this);
        }

        public void onClick(final View view) {
            if (view.getId() == R.id.btn_edit_form_row) {
                Intent i = new Intent(view.getContext(), EditFish1FormRowActivity.class);
                int id = (Integer) view.getTag(R.id.form_row_in_question);
                int formId = (Integer) view.getTag(R.id.parent_form);
                i.putExtra(Fish1FormRow.ID, id);
                i.putExtra(Fish1FormRow.FORM_ID, formId);
                view.getContext().startActivity(i);
            } else if (view.getId() == R.id.btn_duplicate_form_row) {
                final int oldId = (Integer) view.getTag(R.id.form_row_in_question);
                Callable<Fish1FormRow> c = new Callable<Fish1FormRow>() {
                    @Override
                    public Fish1FormRow call() {
                        return db.catchDao().getFormRow(oldId);
                    }
                };
                ExecutorService service = Executors.newSingleThreadExecutor();
                Future<Fish1FormRow> future = service.submit(c);
                Fish1FormRow oldRow = null;
                try {
                    oldRow = future.get();
                } catch (Exception e) {}
                if (oldRow != null) {
                    final Fish1FormRow newRow = oldRow.clone();
                    Callable<Integer> ca = new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return (int) db.catchDao().insertFish1FormRow(newRow);
                        }
                    };
                    Future<Integer> futureInt = service.submit(ca);
                    int newId = -1;
                    try {
                        newId = futureInt.get();
                    } catch (Exception e) {}
                    if (newId > 0) {
                        int formId = (Integer) view.getTag(R.id.parent_form);
                        Intent i = new Intent(view.getContext(), EditFish1FormRowActivity.class);
                        i.putExtra(Fish1FormRow.ID, newId);
                        i.putExtra(Fish1FormRow.FORM_ID, formId);
                        view.getContext().startActivity(i);
                        Toast.makeText(
                                view.getContext().getApplicationContext(),
                                view.getContext().getString(R.string.duplicated_fish_1_form_row),
                                Toast.LENGTH_LONG).show();
                    }
                }
            } else if (view.getId() == R.id.btn_delete_form_row) {
                this.confirmDialog(view);
            }
        }

        private void confirmDialog(final View view) {
            final int formRowId = (Integer) view.getTag(R.id.form_row_in_question);
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder
                    .setMessage(context.getString(R.string.fish_1_form_row_deletion_confirmation_message))
                    .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    db.catchDao().deleteFish1FormRow(formRowId);
                                }
                            };
                            Thread newThread = new Thread(r);
                            newThread.start();
                            try {
                                newThread.join();
                            } catch (InterruptedException ie) {

                            }
                            Activity activity = (Activity) view.getContext();
                            Intent i = new Intent(activity, EditFish1FormActivity.class);
                            int formId = (Integer) view.getTag(R.id.parent_form);
                            i.putExtra(Fish1Form.ID, formId);
                            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            activity.finish();
                            activity.startActivity(i);
                        }
                    })
                    .setNegativeButton(context.getString(R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .show();
        }
    }
}
