package uk.ac.masts.sifids.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pgm5 on 19/02/2018.
 */

@Entity(tableName = "catch_species")
public class CatchSpecies extends EntityWithId {

    //Species from FISH1 Form
    private static final String[][] SPECIES = {
            {"Merluza", null, null},
            {"Cabrilla", null, null},
            {"Doncella", null, null},
            {"Falso volador", null, null},
            {"Peje", null, null},
            {"Bereche", null, null},
            {"Diablico", null, null},
            {"Cachema", null, null},
            {"Raya", null, null},
            {"Chiri", null, null},
            {"Tollo", null, null},
            {"Pota", null, null}
    };

    @ColumnInfo(name = "species_name")
    public String speciesName;

    @ColumnInfo(name = "species_code")
    public String speciesCode;

    @ColumnInfo(name = "scientific_name")
    public String scientificName;

    public static List<CatchSpecies> createSpecies() {
        List<CatchSpecies> speciesObjects = new ArrayList();
        for(String[] speciesDetails : SPECIES) speciesObjects.add(new CatchSpecies(speciesDetails));
        return speciesObjects;
    }

    @Ignore
    public CatchSpecies(String[] speciesDetails) {
        this.setSpeciesName(speciesDetails[0]);
        this.setSpeciesCode(speciesDetails[1]);
        this.setScientificName(speciesDetails[2]);
    }

    public CatchSpecies(){}

    public String getSpeciesName() {
        return speciesName;
    }

    public void setSpeciesName(String speciesName) {
        this.speciesName = speciesName;
    }

    public String getSpeciesCode() {
        return speciesCode;
    }

    public void setSpeciesCode(String speciesCode) {
        this.speciesCode = speciesCode;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String toString() {
        return this.getSpeciesName();
    }
}
