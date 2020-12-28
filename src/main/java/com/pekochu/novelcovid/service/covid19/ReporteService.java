package com.pekochu.novelcovid.service.covid19;

import com.pekochu.novelcovid.model.covid19.Estado;
import com.pekochu.novelcovid.model.covid19.Reporte;

import java.util.List;

public interface ReporteService {

    /**
     * Register new summary report
     * @param report reportDto argument
     * @return reportDto result
     */
    Reporte createReport(Reporte report);

    /**
     * Register all the reports
     * @param reports save all the reports on the database table
     * @return List of saved reports
     */
    List<Reporte> saveAllReports(List<Reporte> reports);

    /**
     * Get the last reporte stored in the database
     * @return reporte
     */
    Reporte lastReport();

    /**
     * Find report by state id
     * @param state object of type Estado
     * @return list of reports
     */
    List<Reporte> findReportsByState(Estado state);

    /**
     * Get all the summary reports of national data
     * @return list of reports
     */
    List<Reporte> getAllNationalReports();

    /**
     * Register new summary report
     * @param date date of the report
     * @return list of reports
     */
    List<Reporte> findReportsByDate(String date);

    /**
     * Find state reports by date ordered by "confirmados"
     * @param date date of the report
     * @return list of reports
     */
    List<Reporte> topStateReportsByDate(String date);

    /**
     * Update summary report
     * @param report reportDto argument
     * @return reportDto result
     */
    Reporte updateReport(Reporte report);

    /**
     * Update all reports passed in a list
     * @param reports list of reports
     * @return set of reports if query successful
     */
    List<Reporte> updateReport(List<Reporte> reports);
}
