package de.storchp.opentracks.osmplugin.dashboardapi;

public class TracksColumn {

    public static final String _ID = "_id";
    public static final String NAME = "name"; // track name
    public static final String DESCRIPTION = "description"; // track description
    public static final String CATEGORY = "category"; // track activity type
    public static final String STARTTIME = "starttime"; // track start time
    public static final String STOPTIME = "stoptime"; // track stop time
    public static final String TOTALDISTANCE = "totaldistance"; // total distance
    public static final String TOTALTIME = "totaltime"; // total time
    public static final String MOVINGTIME = "movingtime"; // moving time
    public static final String AVGSPEED = "avgspeed"; // average speed
    public static final String AVGMOVINGSPEED = "avgmovingspeed"; // average moving speed
    public static final String MAXSPEED = "maxspeed"; // maximum speed
    public static final String MINELEVATION = "minelevation"; // minimum elevation
    public static final String MAXELEVATION = "maxelevation"; // maximum elevation
    public static final String ELEVATIONGAIN = "elevationgain"; // elevation gain

    public static final String[] PROJECTION = {
            _ID,
            NAME,
            DESCRIPTION,
            CATEGORY,
            STARTTIME,
            STOPTIME,
            TOTALDISTANCE,
            TOTALTIME,
            MOVINGTIME,
            AVGSPEED,
            AVGMOVINGSPEED,
            MAXSPEED,
            MINELEVATION,
            MAXELEVATION,
            ELEVATIONGAIN
    };
}
