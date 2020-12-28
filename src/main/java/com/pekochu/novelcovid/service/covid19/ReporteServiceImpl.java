package com.pekochu.novelcovid.service.covid19;

import com.pekochu.novelcovid.model.covid19.Estado;
import com.pekochu.novelcovid.model.covid19.Reporte;
import com.pekochu.novelcovid.repository.ReporteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReporteServiceImpl implements ReporteService{

    @Autowired
    ReporteRepository reporteRepository;

    @Override
    public Reporte createReport(Reporte report) {
        if(!reporteRepository.existsById(String.valueOf(report.getId()))) {
            return reporteRepository.save(report);
        }else{
            return null;
        }
    }

    @Override
    public List<Reporte> saveAllReports(List<Reporte> reports) {
        return reporteRepository.saveAll(reports);
    }

    @Override
    public Reporte lastReport() {
        return reporteRepository.findTopByOrderByIdDesc();
    }

    @Override
    public List<Reporte> findReportsByState(Estado state) {
        return reporteRepository.findByEstadoOrderByFechaDesc(state);
    }

    @Override
    public List<Reporte> getAllNationalReports() {
        Estado state = new Estado();
        state.setId(33L);
        return reporteRepository.findByEstadoOrderByFechaDesc(state);
    }

    @Override
    public List<Reporte> findReportsByDate(String date) {
        return reporteRepository.findByFechaOrderByEstadoAsc(date);
    }

    @Override
    public List<Reporte> topStateReportsByDate(String date) {
        return reporteRepository.findByFechaOrderByConfirmadosDesc(date);
    }

    @Override
    public Reporte updateReport(Reporte report) {
        return reporteRepository.save(report);
    }

    @Override
    public List<Reporte> updateReport(List<Reporte> reports) {
        return reporteRepository.saveAll(reports);
    }
}
