package uk.ac.masts.sifids.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.util.SparseArray;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.masts.sifids.entities.CatchLocation;
import uk.ac.masts.sifids.entities.CatchPresentation;
import uk.ac.masts.sifids.entities.CatchSpecies;
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

@Dao
public interface CatchDao {

    @Insert
    public void insertSpecies(List<CatchSpecies> species);

    @Insert
    public void insertStates(List<CatchState> states);

    @Insert
    public void insertPresentations(List<CatchPresentation> presentations);

    @Insert
    public void insertGear(List<Gear> gear);

    @Insert
    public void insertFisheryOffices(List<FisheryOffice> fisheryOffices);

    @Insert
    public void insertPorts(List<Port> ports);

    @Insert
    public long[] insertFish1Forms(Fish1Form... forms);

    @Insert
    public void insertFish1FormRows(Fish1FormRow... formRows);

    @Insert
    public void insertFish1FormRows(Collection<Fish1FormRow> formRows);

    @Insert
    public void insertLocations(CatchLocation... locations);

    @Insert
    public void insertLocation(CatchLocation location);

    @Insert
    public void insertLocations(Collection<CatchLocation> locations);

    @Insert
    public void insertObservationClasses(Collection<ObservationClass> observationClasses);

    @Insert
    public void insertObservationSpecies(Collection<ObservationSpecies> observationSpecies);

    @Insert
    public long insertObservation(Observation observation);

    @Query("SELECT * FROM catch_species")
    public List<CatchSpecies> getSpecies();

    @Query("SELECT * FROM fish_1_form")
    public List<Fish1Form> getForms();

    @Query("SELECT * FROM fish_1_form WHERE id = :id")
    public Fish1Form getForm(int id);

    @Query("SELECT * FROM fish_1_form_row WHERE form_id = :formId")
    public List<Fish1FormRow> getRowsForForm(int formId);

    @Query("SELECT * FROM fish_1_form_row WHERE id = :id")
    public Fish1FormRow getFormRow(int id);

    @Query("SELECT fishing_activity_date FROM fish_1_form_row WHERE form_id = :formId ORDER BY fishing_activity_date ASC LIMIT 1")
    public Date getDateOfEarliestRow(int formId);

    @Query("SELECT fishing_activity_date FROM fish_1_form_row WHERE form_id = :formId ORDER BY fishing_activity_date DESC LIMIT 1")
    public Date getDateOfLatestRow(int formId);

    @Query("SELECT * FROM gear")
    public List<Gear> getGear();

    @Query("SELECT * FROM gear WHERE id IN(:ids)")
    public List<Gear> getGear(Set<String> ids);

    @Query("SELECT * FROM catch_state")
    public List<CatchState> getStates();

    @Query("SELECT * FROM catch_presentation")
    public List<CatchPresentation> getPresentations();

    @Query("SELECT * FROM port")
    public List<Port> getPorts();

    @Query("SELECT name FROM port WHERE id IN(:ids)")
    public List<String> getPortNames(Set<String> ids);

    @Query("SELECT * FROM fishery_office")
    public List<FisheryOffice> getOffices();

    @Query("SELECT * FROM fishery_office WHERE id = :id")
    public FisheryOffice getOffice(Integer id);

    @Query("SELECT * FROM gear WHERE id = :id")
    public Gear getGearById(Integer id);

    @Query("SELECT * FROM catch_species WHERE id = :id")
    public CatchSpecies getSpeciesById(Integer id);

    @Query("SELECT * FROM catch_state WHERE id = :id")
    public CatchState getStateById(Integer id);

    @Query("SELECT * FROM catch_presentation WHERE id = :id")
    public CatchPresentation getPresentationById(Integer id);

    @Query("SELECT * FROM location ORDER BY timestamp DESC LIMIT :limit")
    public List<CatchLocation> getLastLocations(int limit);

    @Query("SELECT * FROM location WHERE id > :id")
    public List<CatchLocation> getLocationsSince(int id);

    @Query("SELECT * FROM location WHERE timestamp >= :start ORDER BY timestamp DESC")
    public List<CatchLocation> getLocationsSince(Date start);

    @Query("SELECT * FROM location WHERE timestamp >= :start AND TIMESTAMP < :end ORDER BY timestamp ASC LIMIT 1")
    public CatchLocation getFirstLocationBetweenDates(Date start, Date end);

    @Query("SELECT * FROM location WHERE timestamp >= :start AND TIMESTAMP < :end AND fishing = 1 ORDER BY timestamp ASC LIMIT 1")
    public CatchLocation getFirstFishingLocationBetweenDates(Date start, Date end);

    @Query("SELECT * FROM location WHERE timestamp > :start AND TIMESTAMP < :end AND (latitude < :lower_lat OR latitude >= :upper_lat OR longitude < :lower_long OR longitude >= :upper_long) ORDER BY timestamp ASC LIMIT 1")
    public CatchLocation getFirstLocationOutsideBoundsBetweenDates(Date start, Date end, double lower_lat, double upper_lat, double lower_long, double upper_long);

    @Query("SELECT * FROM location WHERE timestamp > :start AND TIMESTAMP < :end AND fishing = 1 AND (latitude < :lower_lat OR latitude >= :upper_lat OR longitude < :lower_long OR longitude >= :upper_long) ORDER BY timestamp ASC LIMIT 1")
    public CatchLocation getFirstFishingLocationOutsideBoundsBetweenDates(Date start, Date end, double lower_lat, double upper_lat, double lower_long, double upper_long);

    @Query("SELECT * FROM location WHERE timestamp >= :start AND TIMESTAMP < :end AND latitude >= 36.0 AND latitude < 85.5 AND longitude >= -44.0 AND longitude < 68.5 ORDER BY timestamp ASC LIMIT 1")
    public CatchLocation getFirstValidIcesLocationBetweenDates(Date start, Date end);

    @Query("SELECT * FROM location WHERE timestamp >= :start AND TIMESTAMP < :end AND latitude >= 36.0 AND latitude < 85.5 AND longitude >= -44.0 AND longitude < 68.5 AND fishing = 1 ORDER BY timestamp ASC LIMIT 1")
    public CatchLocation getFirstValidIcesFishingLocationBetweenDates(Date start, Date end);

    @Query("SELECT COUNT(*) FROM location WHERE uploaded = 0")
    public int countUnuploadedLocations();

    @Query("SELECT * FROM location WHERE uploaded = 0 LIMIT 999")
    public List<CatchLocation> getUnuploadedLocations();

    @Query("SELECT * FROM observation_class")
    public List<ObservationClass> getObservationClasses();

    @Query("SELECT COUNT(*) FROM observation_class")
    public int countObservationClasses();

    @Query("SELECT name FROM observation_class WHERE id = :id")
    public String getObservationClassName(int id);

    @Query("SELECT COUNT(*) FROM observation_species")
    public int countObservationSpecies();

    @Query("SELECT COUNT(*) FROM observation_species WHERE observation_class_id = :observationClassId")
    public int countObservationSpecies(int observationClassId);

    @Query("SELECT * FROM observation_species WHERE observation_class_id = :observationClassId")
    public List<ObservationSpecies> getObservationSpecies(int observationClassId);

    @Query("SELECT name FROM observation_species WHERE id = :id")
    public String getObservationSpeciesName(int id);

    @Query("SELECT * FROM location WHERE ABS(timestamp - :timestamp) < 600000 ORDER BY ABS(timestamp - :timestamp) LIMIT 1")
    public CatchLocation getLocationAt(Date timestamp);

    @Query("SELECT COUNT(*) FROM observation WHERE submitted = 1")
    public int countSubmittedObservations();

    @Query("SELECT COUNT(*) FROM observation WHERE submitted = 0")
    public int countUnsubmittedObservations();

    @Query("SELECT * FROM observation WHERE submitted = 0")
    public List<Observation> getUnsubmittedObservations();

    @Query("UPDATE observation SET submitted = 1 WHERE id = :id")
    public void markObservationSubmitted(int id);

    @Update
    public void updateFish1Forms(Fish1Form... forms);

    @Update
    public void updateFish1FormRows(Fish1FormRow... forms);

    @Query("UPDATE location SET uploaded = 1 WHERE id IN (:ids)")
    public void markLocationsUploaded(List<String> ids);

    @Query("DELETE FROM fish_1_form WHERE id = :id")
    public void deleteFish1Form(int id);

    @Query("DELETE FROM fish_1_form_row WHERE id = :id")
    public void deleteFish1FormRow(int id);

    @Query("DELETE FROM observation_species")
    public void deleteAllObservationSpecies();

    @Query("DELETE FROM observation_class")
    public void deleteAllObservationClasses();


}
