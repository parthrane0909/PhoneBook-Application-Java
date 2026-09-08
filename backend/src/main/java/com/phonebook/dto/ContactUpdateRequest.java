package com.phonebook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ContactUpdateRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 50)
    @JsonProperty("phone_number")
    private String phoneNumber;

    @Email
    @Size(max = 255)
    private String email;

    private String address;

    @Size(max = 10)
    private List<String> tags;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
