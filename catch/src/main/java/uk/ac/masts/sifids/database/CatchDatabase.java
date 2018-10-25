package uk.ac.masts.sifids.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.concurrent.Executors;

import uk.ac.masts.sifids.entities.CatchLocation;
import uk.ac.masts.sifids.entities.CatchPresentation;
import uk.ac.masts.sifids.entities.CatchSpecies;
import uk.ac.masts.sifids.entities.CatchSpeciesAllowedPresentation;
import uk.ac.masts.sifids.entities.CatchSpeciesAllowedState;
import uk.ac.masts.sifids.entities.CatchState;
import uk.ac.masts.sifids.entities.Fish1Form;
import uk.ac.masts.sifids.entities.Fish1FormRow;
import uk.ac.masts.sifids.entities.FisheryOffice;
import uk.ac.masts.sifids.entities.Gear;
import uk.ac.masts.sifids.entities.Observation;
import uk.ac.masts.sifids.entities.ObservationClass;
import uk.ac.masts.sifids.entities.ObservationSpecies;
import uk.ac.masts.sifids.entities.Port;

/**
 * Created by pgm5 on 19/02/2018.
 */

@Database(
        entities = {
                Fish1Form.class,
                Fish1FormRow.class,
                CatchSpecies.class,
                CatchState.class,
                CatchPresentation.class,
                Gear.class,
                CatchSpeciesAllowedState.class,
                CatchSpeciesAllowedPresentation.class,
                CatchLocation.class,
                FisheryOffice.class,
                Port.class,
                ObservationClass.class,
                ObservationSpecies.class,
                Observation.class
    },
        version = 16
)
@TypeConverters({DateTypeConverter.class})
public abstract class CatchDatabase extends RoomDatabase{

    private static CatchDatabase INSTANCE;

    public abstract CatchDao catchDao();

    public synchronized static CatchDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = buildDatabase(context);
        }
        return INSTANCE;
    }

    private static CatchDatabase buildDatabase(final Context context) {
        return Room.databaseBuilder(context,
                CatchDatabase.class,
                "catch-database")
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Executors.newSingleThreadScheduledExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                CatchDao dao = getInstance(context).catchDao();
                                dao.insertPresentations(CatchPresentation.createPresentations());
                                dao.insertSpecies(CatchSpecies.createSpecies());
                                dao.insertStates(CatchState.createStates());
                                dao.insertGear(Gear.createGear());
                                dao.insertFisheryOffices(FisheryOffice.createFisheryOffices());
                                dao.insertPorts(Port.createPorts());
                            }
                        });
                    }

                    @Override
                    public void onOpen(@NonNull final SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        Executors.newSingleThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                CatchDao dao = getInstance(context).catchDao();
                                int observationClassesCount = dao.countObservationClasses();
                                int observationSpeciesCount = dao.countObservationSpecies();
                                int numSpecies = 0;
                                for (String[] species : ObservationSpecies.SPECIES.values()) {
                                    numSpecies += species.length;
                                }
                                if (observationClassesCount > ObservationClass.ANIMALS.length
                                        || observationSpeciesCount > numSpecies) {
                                    dao.deleteAllObservationSpecies();
                                    dao.deleteAllObservationClasses();
                                }
                                if (dao.countObservationClasses() == 0) {
                                    dao.insertObservationClasses(
                                            ObservationClass.createObservationClasses());
                                }
                                if (dao.countObservationSpecies() == 0) {
                                    dao.insertObservationSpecies(
                                            ObservationSpecies.createObservationSpecies(
                                                    dao.getObservationClasses()
                                            ));
                                }
                                if (dao.countOffices() == 0) {
                                    dao.insertFisheryOffices(
                                            FisheryOffice.createFisheryOffices());
                                }
                                if (dao.countPorts() == 0) {
                                    dao.insertPorts(
                                            Port.createPorts());
                                }
                            }
                        });
                    }
                })
                .addMigrations(
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16
                )
                .build();
    }

    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE location ADD COLUMN uploaded INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS observation_class " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT);"
            );
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS observation_species " +
                            "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, " +
                            "observation_class_id INTEGER NOT NULL, " +
                            "FOREIGN KEY(observation_class_id) REFERENCES observation_class(id) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE);"
            );
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS observation " +
                            "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "observation_class_id INTEGER NOT NULL, " +
                            "observation_species_id INTEGER, " +
                            "timestamp INTEGER, " +
                            "latitude REAL NOT NULL, " +
                            "longitude REAL NOT NULL, " +
                            "count INTEGER NOT NULL, " +
                            "notes TEXT, " +
                            "submitted INTEGER NOT NULL DEFAULT(0), " +
                            "created_at INTEGER, " +
                            "modified_at INTEGER, " +
                            "FOREIGN KEY(observation_class_id) REFERENCES observation_class(id) " +
                            "ON UPDATE NO ACTION ON DELETE NO ACTION, " +
                            "FOREIGN KEY(observation_species_id) REFERENCES observation_species(id) " +
                            "ON UPDATE NO ACTION ON DELETE NO ACTION);"
            );
        }
    };

    static final Migration MIGRATION_11_12 = new Migration(11,12) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM observation_species");
            database.execSQL("DELETE FROM observation_class");
        }
    };

    static final Migration MIGRATION_12_13 = new Migration(12,13) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM observation_species");
        }
    };

    static final Migration MIGRATION_13_14 = new Migration(13,14) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE location ADD COLUMN accuracy REAL");
        }
    };

    static final Migration MIGRATION_14_15 = new Migration(14,15) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM fishery_office");
        }
    };

    static final Migration MIGRATION_15_16 = new Migration(15,16) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM port");
        }
    };
}
