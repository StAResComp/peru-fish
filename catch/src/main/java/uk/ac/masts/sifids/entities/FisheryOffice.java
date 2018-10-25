package uk.ac.masts.sifids.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "fishery_office")
public class FisheryOffice extends EntityWithId {

    private static final String[][] OFFICES = {
            {"Peru", "Some address", "research-computing@st-andrews.ac.uk"}
    };

    public String name;
    public String address;
    public String email;

    public FisheryOffice() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static List<FisheryOffice> createFisheryOffices() {
        List<FisheryOffice> fisheryOfficeObjects = new ArrayList();
        for(String[] fisheryOfficeDetails : OFFICES) fisheryOfficeObjects.add(new FisheryOffice(fisheryOfficeDetails));
        return fisheryOfficeObjects;
    }

    @Ignore
    public FisheryOffice(String[] fisheryOfficeDetails) {
        this.setName(fisheryOfficeDetails[0]);
        this.setAddress(fisheryOfficeDetails[1]);
        this.setEmail(fisheryOfficeDetails[2]);
    }

    public String toString() {
        return this.getName();
    }
}
