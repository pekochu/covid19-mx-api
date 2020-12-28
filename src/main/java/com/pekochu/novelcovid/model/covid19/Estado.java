package com.pekochu.novelcovid.model.covid19;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.persistence.*;

@Entity
@JsonSerialize
@Table(name = "covid_estados")
public class Estado {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public Estado(){ /* Empty constructor */}

    public Estado(Long id, String nombre, String cve, Long poblacion) {
        this.id = id;
        this.nombre = nombre;
        this.cve = cve;
        this.poblacion = poblacion;
    }

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "cve")
    private String cve;

    @Column(name = "poblacion")
    private Long poblacion;

    @JsonIgnore
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCve() {
        return cve;
    }

    public void setCve(String cve) {
        this.cve = cve;
    }

    public Long getPoblacion() {
        return poblacion;
    }

    public void setPoblacion(Long poblacion) {
        this.poblacion = poblacion;
    }

    @Override
    public String toString(){
        return String.format("id: %d, ", id) +
                String.format("Nombre: %s, ", nombre) +
                String.format("Clave: %s, ", cve) +
                String.format("Poblacion: %d, ", poblacion);
    }
}
