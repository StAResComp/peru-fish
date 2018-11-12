package uk.ac.masts.sifids.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pgm5 on 19/02/2018.
 */

@Entity(tableName = "gear")
public class Gear extends EntityWithId {

    private static final String[] GEAR = {
            "Red cortina",
            "Otros"
    };

    public String name;

    public boolean hasMeshSize;

    public static List<Gear> createGear() {
        List<Gear> gearObjects = new ArrayList();
        for(String name : GEAR) gearObjects.add(new Gear(name));
        return gearObjects;
    }

    @Ignore
    public Gear(String name) {
        this.setName(name);
    }

    public Gear() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHasMeshSize() {
        return hasMeshSize;
    }

    public void setHasMeshSize(boolean hasMeshSize) {
        this.hasMeshSize = hasMeshSize;
    }

    public String toString() {
        return this.getName();
    }
}
