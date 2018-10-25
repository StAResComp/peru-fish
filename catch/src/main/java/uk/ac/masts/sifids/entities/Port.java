package uk.ac.masts.sifids.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "port")
public class Port extends EntityWithId {

    private static final String[] PORTS = {
        "Los Organos"
    };

    public String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static List<Port> createPorts() {
        List<Port> portObjects = new ArrayList();
        for(String portName : PORTS) {
            Port port = new Port();
            port.setName(portName);
            portObjects.add(port);
        }
        return portObjects;
    }

    public String toString() {
        return this.getName();
    }
}
