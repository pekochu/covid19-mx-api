package com.pekochu.novelcovid.service.covid19;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public interface CovidSummaryProvider {

    /**
     * Find and save the latest statistics stored in
     * Mexico's Open Data, John Hopkins University and the
     * CONACYT platform Semaphore Color
     */
    void updateData();

    void downloadDataCSV(boolean force);

    void readCovidCSV();

    void resetDataset();

    Map<Integer, String> getSemaphore();

    void getRecoveries();
}
