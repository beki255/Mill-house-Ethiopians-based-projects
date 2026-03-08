package com.mill.system;

public class Driver {
    private int id;
    private int driverId;
    private String carNumber;
    private String carType;
    private String licenseNumber;
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getDriverId() { return driverId; }
    public void setDriverId(int driverId) { this.driverId = driverId; }
    
    public String getCarNumber() { return carNumber; }
    public void setCarNumber(String carNumber) { this.carNumber = carNumber; }
    
    public String getCarType() { return carType; }
    public void setCarType(String carType) { this.carType = carType; }
    
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
}