package com.fsm.navigator.backend.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    public Admin() {}

    public Admin(String email, String password) {
        super(email, password);
    }

    @Override
    public String getRoleAsString() { return "ADMIN"; }
}