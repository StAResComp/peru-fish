package uk.ac.masts.sifids.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(tableName = "fish_1_form_row_species",
        foreignKeys = {
                @ForeignKey(
                        entity = Fish1FormRow.class,
                        parentColumns = "id",
                        childColumns = "form_row_id",
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = CatchSpecies.class,
                        parentColumns = "id",
                        childColumns = "species_id"
                )
        },
        indices = {
                @Index(value = "form_row_id", name = "form_row_species_form_row_id"),
                @Index(value = "species_id", name = "form_row_species_species_id")
        }
)
public class Fish1FormRowSpecies extends ChangeLoggingEntity {

    @ColumnInfo(name = "form_row_id")
    public int formRowId;

    @ColumnInfo(name = "species_id")
    public int speciesId;

    @ColumnInfo
    public Double weight;

    public Fish1FormRowSpecies() {
        this.updateDates();
    }

    public Fish1FormRowSpecies(int formRowId, int speciesId) {
        this.formRowId = formRowId;
        this.speciesId = speciesId;
        this.updateDates();
    }

    public Fish1FormRowSpecies(int formRowId, int speciesId, double weight) {
        this.formRowId = formRowId;
        this.speciesId = speciesId;
        this.weight = weight;
        this.updateDates();
    }

    public int getFormRowId() {
        return formRowId;
    }

    public void setFormRowId(int formRowId) {
        this.formRowId = formRowId;
        this.updateDates();
    }

    public int getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(int speciesId) {
        this.speciesId = speciesId;
        this.updateDates();
    }

    public Double getWeight() {
        return weight;
    }

    public boolean setWeight(Double weight) {
        if (!weight.equals(this.getWeight())) {
            this.weight = weight;
            this.updateDates();
            return true;
        }
        return false;
    }
}
