package uk.ac.masts.sifids.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;

@Entity(foreignKeys = {
        @ForeignKey(
                entity = BycatchSpecies.class,
                parentColumns = "id",
                childColumns = "bycatch_species_id"
        )
})
public class Bycatch extends ChangeLoggingEntity {

    @ColumnInfo(name = "bycatch_species_id")
    public int bycatchSpeciesId;

    public Integer number;

    public Double weight;

    public boolean submitted;

    public int getBycatchSpeciesId() {
        return bycatchSpeciesId;
    }

    public void setBycatchSpeciesId(int bycatchSpeciesId) {
        if (bycatchSpeciesId != this.bycatchSpeciesId) {
            this.bycatchSpeciesId = bycatchSpeciesId;
            this.updateDates();
        }
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        if (this.getNumber() == null || !this.getNumber().equals(number)) {
            this.number = number;
            this.updateDates();
        }
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        if (this.getWeight() == null || !this.getWeight().equals(weight)) {
            this.weight = weight;
            this.updateDates();
        }
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        if (this.isSubmitted() != submitted) {
            this.submitted = submitted;
            this.updateDates();
        }
    }
}
