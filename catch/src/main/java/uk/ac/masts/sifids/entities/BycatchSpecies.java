package uk.ac.masts.sifids.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "bycatch_species")
public class BycatchSpecies extends EntityWithId {

    public static final int NUMBER = 1;
    public static final int WEIGHT = 2;

    public static final String[][] SPECIES = {
            {"Merluza juvenil", "WEIGHT"},
            {"Centolla", "NUMBER"},
            {"Anguila", "WEIGHT"},
            {"Cangrejo", "NUMBER"},
            {"Coral", "NUMBER"},
            {"Erizo", "NUMBER"},
            {"Estrella de mar", "NUMBER"},
            {"Bereche", "NUMBER"},
            {"Morena", "NUMBER"},
            {"Pez Bulldog", "NUMBER"},
            {"Pez Cocodrilo", "NUMBER"},
            {"Pez Zorra", "NUMBER"}
    };

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "measured_by")
    public int measuredBy;

    public BycatchSpecies() {}

    @Ignore
    public BycatchSpecies(String name, int measuredBy) {
        this.setName(name);
        this.setMeasuredBy(measuredBy);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMeasuredBy() {
        return measuredBy;
    }

    public void setMeasuredBy(int measuredBy) {
        this.measuredBy = measuredBy;
    }

    public static List<BycatchSpecies> createSpecies() {
        List<BycatchSpecies> speciesObjects = new ArrayList();
        for(String[] species : SPECIES) {
            speciesObjects.add(
                    new BycatchSpecies(species[0], (species[1].equals("WEIGHT") ? WEIGHT : NUMBER)));
        }
        return speciesObjects;
    }
}
