package uk.ac.masts.sifids.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.ac.masts.sifids.R;
import uk.ac.masts.sifids.database.CatchDatabase;
import uk.ac.masts.sifids.entities.Fish1Form;

/**
 * Created by pgm5 on 21/02/2018.
 */

public class Fish1FormAdapter extends RecyclerView.Adapter<Fish1FormAdapter.ViewHolder> {

    private List<Fish1Form> forms;
    private CatchDatabase db;
    private Context context;

    public Fish1FormAdapter(List<Fish1Form> forms, Context context) {
        this.forms = forms;
        this.context = context;
    }

    @Override
    public Fish1FormAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.form_recycler_view,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Fish1FormAdapter.ViewHolder holder, int position) {
        final Fish1Form form = forms.get(position);
        db = CatchDatabase.getInstance(holder.itemView.getContext());
        Callable<String> c = new Callable<String>() {
            @Override
            public String call() {
                String rowDates = context.getString(R.string.fish_1_form_header);
                Calendar cal = Calendar.getInstance();
                Date lowerDate = db.catchDao().getDateOfEarliestRow(form.getId());
                if (lowerDate != null) {
                    cal.setTime(lowerDate);
                    cal.add(Calendar.DATE, -1 * (cal.get(Calendar.DAY_OF_WEEK) - 1));
                    rowDates = new SimpleDateFormat(context.getString(R.string.dmonthy)).format(cal.getTime());
                    rowDates += " - \n";
                    cal.add(Calendar.DATE, 6);
                    rowDates += new SimpleDateFormat(context.getString(R.string.dmonthy)).format(cal.getTime());
                }
                return rowDates;
            }
        };
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<String> future = service.submit(c);
        String dates = null;
        try {
            dates = future.get();
        } catch (Exception e) {
        }
        holder.createdAt.setText(
                String.format(
                        context.getString(R.string.fish_1_form_summary),
                        form.getPln(),
                        dates
                )
        );
        holder.editButton.setTag(R.id.form_in_question, forms.get(position).getId());
        holder.deleteButton.setTag(R.id.form_in_question, forms.get(position).getId());
    }

    @Override
    public int getItemCount() {
        return forms.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView createdAt;
        public Button editButton;
        public Button deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            createdAt = itemView.findViewById(R.id.label);
            editButton = (Button) itemView.findViewById(R.id.btn_edit_form);
            editButton.setOnClickListener(this);
            deleteButton = (Button) itemView.findViewById(R.id.btn_delete_form);
            deleteButton.setOnClickListener(this);
        }

        public void onClick(View view) {
            if (view.getId() == R.id.btn_edit_form) {
                Intent i = new Intent(view.getContext(), EditFish1FormActivity.class);
                int id = (Integer) view.getTag(R.id.form_in_question);
                i.putExtra(Fish1Form.ID, id);
                view.getContext().startActivity(i);
            }
            else if (view.getId() == R.id.btn_delete_form) {
                this.confirmDialog(view);
            }
        }

        private void confirmDialog(final View view) {
            final int formId = (Integer) view.getTag(R.id.form_in_question);
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder
                    .setMessage(context.getString(R.string.fish_1_form_deletion_confirmation_message))
                    .setPositiveButton(context.getString(R.string.yes),  new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    db.catchDao().deleteFish1Form(formId);
                                }
                            };
                            Thread newThread= new Thread(r);
                            newThread.start();
                            try {
                                newThread.join();
                            }
                            catch (InterruptedException ie) {

                            }
                            Activity activity = (Activity) view.getContext();
                            activity.finish();
                            activity.startActivity(activity.getIntent());
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
