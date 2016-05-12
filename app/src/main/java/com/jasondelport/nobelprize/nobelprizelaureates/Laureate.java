package com.jasondelport.nobelprize.nobelprizelaureates;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBDocument;

/**
 * Created by jasondelport on 10/05/16.
 */
@DynamoDBDocument
public class Laureate {
    /*
    id: "919",
    firstname: "Takaaki",
    surname: "Kajita",
    motivation: ""for the discovery of neutrino oscillations, which shows that neutrinos have mass"",
    share: "2"
    */
    private String id;
    private String firstname;
    private String surname;
    private String motivation;
    private String share;

    public Laureate() {
    }

    public Laureate(String firstname, String id, String motivation, String share, String surname) {
        this.firstname = firstname;
        this.id = id;
        this.motivation = motivation;
        this.share = share;
        this.surname = surname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public String getShare() {
        return share;
    }

    public void setShare(String share) {
        this.share = share;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @Override
    public String toString() {
        return "Laureate{" +
                "id=" + id +
                " firstname=" + firstname +
                ", surname='" + surname + '\'' +
                ", motivation='" + motivation + '\'' +
                ", share=" + share +
                '}';
    }
}
