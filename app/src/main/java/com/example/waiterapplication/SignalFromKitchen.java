package com.example.waiterapplication;

public class SignalFromKitchen {
    private Integer tableNr;
    private Boolean starter;
    private Boolean main;
    private Boolean dessert;

    public void setTableNr(Integer tableNr){this.tableNr=tableNr;}
    public void setStarter(Boolean starter){this.starter=starter;}
    public void setMain(Boolean main){this.main=main;}
    public void setDessert(Boolean dessert){this.dessert=dessert;}
    public Integer getTableNr(){return tableNr;}
    public Boolean getStarter(){return starter;}
    public Boolean getMain(){return main;}
    public Boolean getDessert(){return dessert;}
}
