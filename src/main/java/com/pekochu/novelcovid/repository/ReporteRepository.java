package com.pekochu.novelcovid.repository;

import com.pekochu.novelcovid.model.covid19.Estado;
import com.pekochu.novelcovid.model.covid19.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, String> {

    //
    Reporte findTopByOrderByIdDesc();
    //
    List<Reporte> findByEstadoOrderByFechaDesc(Estado state);
    //
    List<Reporte> findByFechaOrderByEstadoAsc(String fecha);
    //
    List<Reporte> findByFechaOrderByConfirmadosDesc(String fecha);

}
