package com.dnb.kyc.model;

import com.google.gson.annotations.SerializedName;

/**
 * KYC Profile Data Model
 * 
 * Represents the input data for KYC processing.
 */
public class KycProfile {

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("nationality")
    private String nationality;

    @SerializedName("age")
    private Integer age;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("income")
    private Double income;

    @SerializedName("employment_status")
    private String employmentStatus;

    @SerializedName("employer")
    private String employer;

    @SerializedName("passport_number")
    private String passportNumber;

    @SerializedName("address")
    private String address;

    @SerializedName("city")
    private String city;

    @SerializedName("country")
    private String country;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("is_pep")
    private boolean pep;

    // Constructors
    public KycProfile() {}

    public KycProfile(String firstName, String lastName, String nationality, Integer age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.nationality = nationality;
        this.age = age;
    }

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Double getIncome() { return income; }
    public void setIncome(Double income) { this.income = income; }

    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }

    public String getEmployer() { return employer; }
    public void setEmployer(String employer) { this.employer = employer; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isPep() { return pep; }
    public void setPep(boolean pep) { this.pep = pep; }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
