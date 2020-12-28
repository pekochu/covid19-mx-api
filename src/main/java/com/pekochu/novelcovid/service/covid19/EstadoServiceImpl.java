package com.pekochu.novelcovid.service.covid19;

import com.pekochu.novelcovid.model.covid19.Estado;
import com.pekochu.novelcovid.repository.EstadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstadoServiceImpl implements EstadoService {

    @Autowired
    EstadoRepository estadoRepository;

    @Override
    public List<Estado> findAll(){
        return estadoRepository.findAll();
    }

    @Override
    public Estado findStateByCve(String cve){
        return estadoRepository.findByCve(cve);
    }
}
