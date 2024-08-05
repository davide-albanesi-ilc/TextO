package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Required;
import it.cnr.ilc.texto.domain.annotation.Unique;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * @author oakgen
 */
public class Test extends Entity {

    public enum TestEnum {
        ONE,
        TWO,
        THREE
    }

    private String stringt;
    private Boolean booleant;
    private Short shortt;
    private Integer integert;
    private Long longt;
    private Float floatt;
    private Double doublet;
    private LocalDate datet;
    private LocalDateTime datetimet;
    private TestEnum enumt;
    private Test next;

    @Unique
    @Required
    public String getStringt() {
        return stringt;
    }

    public void setStringt(String stringt) {
        this.stringt = stringt;
    }

    public Boolean getBooleant() {
        return booleant;
    }

    public void setBooleant(Boolean booleant) {
        this.booleant = booleant;
    }

    public Short getShortt() {
        return shortt;
    }

    public void setShortt(Short shortt) {
        this.shortt = shortt;
    }

    public Integer getIntegert() {
        return integert;
    }

    public void setIntegert(Integer integert) {
        this.integert = integert;
    }

    public Long getLongt() {
        return longt;
    }

    public void setLongt(Long longt) {
        this.longt = longt;
    }

    public Float getFloatt() {
        return floatt;
    }

    public void setFloatt(Float floatt) {
        this.floatt = floatt;
    }

    public Double getDoublet() {
        return doublet;
    }

    public void setDoublet(Double doublet) {
        this.doublet = doublet;
    }

    public LocalDate getDatet() {
        return datet;
    }

    public void setDatet(LocalDate datet) {
        this.datet = datet;
    }

    public LocalDateTime getDatetimet() {
        return datetimet;
    }

    public void setDatetimet(LocalDateTime datetimet) {
        this.datetimet = datetimet;
    }

    public TestEnum getEnumt() {
        return enumt;
    }

    public void setEnumt(TestEnum enumt) {
        this.enumt = enumt;
    }

    public Test getNext() {
        return next;
    }

    public void setNext(Test next) {
        this.next = next;
    }

}
