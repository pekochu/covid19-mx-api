package com.pekochu.novelcovid.repository;

import com.pekochu.novelcovid.model.covid19.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, String> {

    Estado findByCve(String cve);

}
