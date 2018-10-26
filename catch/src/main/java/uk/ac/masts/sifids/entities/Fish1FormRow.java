package uk.ac.masts.sifids.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.arch.persistence.room.ForeignKey.CASCADE;

/**
 * Created by pgm5 on 19/02/2018.
 */

@Entity(tableName = "fish_1_form_row",
        foreignKeys = {
        @ForeignKey(
                entity = Fish1Form.class,
                parentColumns = "id",
                childColumns = "form_id",
                onDelete = CASCADE
        ),
        @ForeignKey(
                entity = Gear.class,
                parentColumns = "id",
                childColumns = "gear_id"
        ),
        @ForeignKey(
                entity = CatchSpecies.class,
                parentColumns = "id",
                childColumns = "species_id"
        ),
        @ForeignKey(
                entity = CatchState.class,
                parentColumns = "id",
                childColumns = "state_id"
        ),
        @ForeignKey(
                entity = CatchPresentation.class,
                parentColumns = "id",
                childColumns = "presentation_id"
        )
},
        indices = {
                @Index(value = "form_id", name = "form_row_form_id"),
                @Index(value = "gear_id", name = "form_row_gear_id"),
                @Index(value = "species_id", name = "form_row_species_id"),
                @Index(value = "state_id", name = "form_row_state_id"),
                @Index(value = "presentation_id", name = "form_row_presentation_id"),
        }
)
public class Fish1FormRow extends ChangeLoggingEntity{

    public final static String ID = "id";
    public final static String FORM_ID = "form_id";

    @ColumnInfo(name = "form_id")
    public int formId;

    @ColumnInfo(name = "fishing_activity_date")
    public Date fishingActivityDate;

    public double latitude;

    public double longitude;

    @ColumnInfo(name = "ices_area")
    public String icesArea;

    @ColumnInfo(name = "gear_id")
    public Integer gearId;

    @ColumnInfo(name = "mesh_size")
    public int meshSize;

    @ColumnInfo(name = "species_id")
    public Integer speciesId;

    @ColumnInfo(name = "state_id")
    public Integer stateId;

    @ColumnInfo(name = "presentation_id")
    public Integer presentationId;

    public double weight;

    public boolean dis;

    public boolean bms;

    @ColumnInfo(name = "number_of_pots_hauled")
    public int numberOfPotsHauled;

    @ColumnInfo(name = "landing_or_discard_date")
    public Date landingOrDiscardDate;

    @ColumnInfo(name = "transporter_reg_etc")
    public String transporterRegEtc;

    public Fish1FormRow () {
        this.updateDates();
    }

    public Fish1FormRow (Fish1Form form, CatchLocation point) {
        this.formId = form.getId();
        this.fishingActivityDate = point.getTimestamp();
        this.latitude = point.getLatitude();
        this.longitude = point.getLongitude();
        this.updateDates();
    }

    public int getFormId() {
        return formId;
    }

    public boolean setFormId(int formId) {
        if (formId != this.getFormId()) {
            this.formId = formId;
            this.updateDates();
            return true;
        }
        return false;
    }

    public Date getFishingActivityDate() {
        return fishingActivityDate;
    }

    public boolean setFishingActivityDate(Date fishingActivityDate) {
        if (fishingActivityDate == null || !fishingActivityDate.equals(this.getFishingActivityDate())) {
            this.fishingActivityDate = fishingActivityDate;
            this.updateDates();
            return true;
        }
        return false;
    }

    public double getLatitude() {
        return latitude;
    }

    public boolean setLatitude(double latitude) {
        if (latitude != this.getLatitude()) {
            this.latitude = latitude;
            this.updateDates();
            return true;
        }
        return false;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean setLongitude(double longitude) {
        if (longitude != this.getLongitude()) {
            this.longitude = longitude;
            this.updateDates();
            return true;
        }
        return false;
    }

    public String getIcesArea() {
        return icesArea;
    }

    public boolean setIcesArea(String icesArea) {
        return false;
    }

    public Integer getGearId() {
        return gearId;
    }

    public boolean setGearId(int gearId) {
        if (this.getGearId() == null || gearId != this.getGearId()) {
            this.gearId = gearId;
            this.updateDates();
            return true;
        }
        return false;
    }

    public int getMeshSize() {
        return meshSize;
    }

    public boolean setMeshSize(int meshSize) {
        if (meshSize != this.getMeshSize()) {
            this.meshSize = meshSize;
            this.updateDates();
            return true;
        }
        return false;
    }

    public Integer getSpeciesId() {
        return speciesId;
    }

    public boolean setSpeciesId(int speciesId) {
        if (this.getSpeciesId() == null || speciesId != this.getSpeciesId()) {
            this.speciesId = speciesId;
            this.updateDates();
            return true;
        }
        return false;
    }

    public Integer getStateId() {
        return stateId;
    }

    public boolean setStateId(int stateId) {
        if (this.getStateId() == null || stateId != this.getStateId()) {
            this.stateId = stateId;
            this.updateDates();
            return true;
        }
        return false;
    }

    public Integer getPresentationId() {
        return presentationId;
    }

    public boolean setPresentationId(int presentationId) {
        if (this.getPresentationId() == null || presentationId != this.getPresentationId()) {
            this.presentationId = presentationId;
            this.updateDates();
            return true;
        }
        return false;
    }

    public double getWeight() {
        return weight;
    }

    public boolean setWeight(double weight) {
        if (weight != this.getWeight()) {
            this.weight = weight;
            this.updateDates();
            return true;
        }
        return false;
    }

    public boolean isDis() {
        return dis;
    }

    public boolean setDis(boolean dis) {
        if (dis != this.isDis()) {
            this.dis = dis;
            this.updateDates();
            return true;
        }
        return false;
    }

    public boolean isBms() {
        return bms;
    }

    public boolean setBms(boolean bms) {
        if (bms != this.isBms()) {
            this.bms = bms;
            this.updateDates();
            return true;
        }
        return false;
    }

    public int getNumberOfPotsHauled() {
        return numberOfPotsHauled;
    }

    public boolean setNumberOfPotsHauled(int numberOfPotsHauled) {
        if (numberOfPotsHauled != this.getNumberOfPotsHauled()) {
            this.numberOfPotsHauled = numberOfPotsHauled;
            this.updateDates();
            return true;
        }
        return false;
    }

    public Date getLandingOrDiscardDate() {
        return landingOrDiscardDate;
    }

    public boolean setLandingOrDiscardDate(Date landingOrDiscardDate) {
        if (landingOrDiscardDate == null || !landingOrDiscardDate.equals(this.getLandingOrDiscardDate())) {
            this.landingOrDiscardDate = landingOrDiscardDate;
            this.updateDates();
            return true;
        }
        return false;
    }

    public String getTransporterRegEtc() {
        return transporterRegEtc;
    }

    public boolean setTransporterRegEtc(String transporterRegEtc) {
        if (!transporterRegEtc.equals(this.getTransporterRegEtc())) {
            this.transporterRegEtc = transporterRegEtc;
            this.updateDates();
            return true;
        }
        return false;
    }

    public String toString() {
        if (this.getFishingActivityDate() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(this.getFishingActivityDate());
            return new SimpleDateFormat("dd MMM").format(cal.getTime());
        }
        else {
            return "Date not set";
        }
    }

    public String getCoordinates() {
        return CatchLocation.getCoordinates(this.getLatitude(), this.getLongitude());
    }

    public Fish1FormRow clone() {
        Fish1FormRow newRow = new Fish1FormRow();
        try {
            newRow.setFormId(this.getFormId());
        } catch (Exception e) {}
        try {
            newRow.setFishingActivityDate(this.getFishingActivityDate());
        } catch (Exception e) {}
        try {
            newRow.setLatitude(this.getLatitude());
        } catch (Exception e) {}
        try {
            newRow.setLongitude(this.getLongitude());
        } catch (Exception e) {}
        try {
            newRow.setGearId(this.getGearId());
        } catch (Exception e) {}
        try {
            newRow.setMeshSize(this.getMeshSize());
        } catch (Exception e) {}
        try {
            newRow.setSpeciesId(this.getSpeciesId());
        } catch (Exception e) {}
        try {
            newRow.setStateId(this.getStateId());
        } catch (Exception e) {}
        try {
            newRow.setPresentationId(this.getPresentationId());
        } catch (Exception e) {}
        try {
            newRow.setWeight(this.getWeight());
        } catch (Exception e) {}
        try {
            newRow.setDis(this.isDis());
        } catch (Exception e) {}
        try {
            newRow.setBms(this.isBms());
        } catch (Exception e) {}
        try {
            newRow.setNumberOfPotsHauled(this.getNumberOfPotsHauled());
        } catch (Exception e) {}
        try {
            newRow.setLandingOrDiscardDate(this.getLandingOrDiscardDate());
        } catch (Exception e) {}
        try {
            newRow.setTransporterRegEtc(this.getTransporterRegEtc());
        } catch (Exception e) {}
        return newRow;
    }
}
