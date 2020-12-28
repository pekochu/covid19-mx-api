package com.pekochu.novelcovid.service.covid19;

import com.pekochu.novelcovid.model.covid19.Estado;

import java.util.List;

public interface EstadoService {

    /**
     * Find all Estados in table
     */
    List<Estado> findAll();

    /**
     * Find Estado by its CVE
     * @param cve string CVE
     * @return state
     */
    Estado findStateByCve(String cve);
}
