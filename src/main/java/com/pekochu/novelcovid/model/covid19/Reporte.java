package com.pekochu.novelcovid.model.covid19;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.persistence.*;

@Entity
@JsonSerialize
@Table(name = "covid_reportes")
public class Reporte {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Reporte(){ /* Empty constructor */ }

    public Reporte(String fecha, Long sospechosos,
                   Long confirmados, Long defunciones, Long recuperados, Long activos) {
        // this.id = id;
        // this.stateId = stateId;
        this.fecha = fecha;
        this.sospechosos = sospechosos;
        this.confirmados = confirmados;
        this.defunciones = defunciones;
        this.recuperados = recuperados;
        this.activos = activos;
    }

    @Column(name = "fecha")
    private String fecha;

    @Column(name = "sospechosos")
    private Long sospechosos;

    @Column(name = "confirmados")
    private Long confirmados;

    @Column(name = "defunciones")
    private Long defunciones;

    @Column(name = "recuperados")
    private Long recuperados;

    @Column(name = "activos")
    private Long activos;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_id")
    private Estado estado;

    @Column(name = "semaforo")
    private String semaforo;

    @JsonIgnore
    public Long getId() {
        return id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public Long getSospechosos() {
        return sospechosos;
    }

    public void setSospechosos(Long sospechosos) {
        this.sospechosos = sospechosos;
    }

    public Long getConfirmados() {
        return confirmados;
    }

    public void setConfirmados(Long confirmados) {
        this.confirmados = confirmados;
    }

    public Long getDefunciones() {
        return defunciones;
    }

    public void setDefunciones(Long defunciones) {
        this.defunciones = defunciones;
    }

    public Long getRecuperados() {
        return recuperados;
    }

    public void setRecuperados(Long recuperados) {
        this.recuperados = recuperados;
    }

    public Long getActivos() {
        return activos;
    }

    public void setActivos(Long activos) {
        this.activos = activos;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getSemaforo() { return semaforo;  }

    public void setSemaforo(String semaforo) { this.semaforo = semaforo; }

    @Override
    public String toString(){
        StringBuilder buffer = new StringBuilder();
        buffer.append(String.format("Reporte del dia %s ", this.fecha));
        buffer.append(String.format("con estado: %s | ", this.estado.getCve()));
        buffer.append(String.format("Confirmados: %s\t", this.confirmados));
        buffer.append(String.format("Recuperados: %s\t", this.recuperados));
        buffer.append(String.format("Defunciones: %s\n", this.defunciones));

        return buffer.toString();
    }

}
