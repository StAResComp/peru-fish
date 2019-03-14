package uk.ac.masts.sifids.utilities;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import uk.ac.masts.sifids.R;

public class Csv {

    /**
     * Appends data to a string intended to be a row of a CSV file by adding a comma, followed by
     * a string representing the data
     * @param rowSoFar the row to which data should be appended
     * @param dataToAppend the data to be appended
     * @param isComplex if this is true, the data will be enclosed in double quotes
     * @param context application context. Only used if dataToAppend is a Calendar or Date.
     * @return the row with the data appended
     */
    public static String appendToCsvRow(
            String rowSoFar, Object dataToAppend, boolean isComplex, Context context) {
        if (rowSoFar == null) {
            rowSoFar = "";
        }
        if (rowSoFar.length() > 0) {
            rowSoFar += ",";
        }
        if (dataToAppend != null) {
            if (dataToAppend instanceof Calendar) {
                Calendar cal = (Calendar) dataToAppend;
                rowSoFar +=
                        new SimpleDateFormat(context.getString(R.string.ymd)).format(cal.getTime());
            }
            else if (dataToAppend instanceof Date) {
                rowSoFar += new SimpleDateFormat(
                        context.getString(R.string.ymd)).format((Date) dataToAppend);
            }
            else {
                if (isComplex) {
                    rowSoFar += "\"" + dataToAppend + "\"";
                }
                else {
                    rowSoFar += dataToAppend;
                }
            }
        }
        return rowSoFar;

    }

    /**
     * Appends a row to a CSV
     * @param csv the CSV data string
     * @param row the row to be appended to the CSV
     * @return
     */
    public static String appendRowToCsv(String csv, String row) {
        if (csv != null && !csv.equals("")) {
            csv += System.getProperty("line.separator");
        }
        csv += row;
        return csv;
    }
}
