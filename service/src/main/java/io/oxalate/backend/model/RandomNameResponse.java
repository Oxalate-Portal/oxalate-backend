package io.oxalate.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RandomNameResponse {
    @Getter
    @Setter
    public static class Coordinates{
        @JsonProperty("latitude")
        String latitude;
        @JsonProperty("longitude")
        String longitude;
    }

    @Getter
    @Setter
    public static class Dob{
        @JsonProperty("date")
        Date date;
        @JsonProperty("age")
        int age;
    }

    @Getter
    @Setter
    public static class Id{
        @JsonProperty("name")
        String name;
        @JsonProperty("value")
        String value;
    }

    @Getter
    @Setter
    public static class Info{
        @JsonProperty("seed")
        String seed;
        @JsonProperty("results")
        int results;
        @JsonProperty("page")
        int page;
        @JsonProperty("version")
        String version;
    }

    @Getter
    @Setter
    public static class Location{
        @JsonProperty("street")
        Street street;
        @JsonProperty("city")
        String city;
        @JsonProperty("state")
        String state;
        @JsonProperty("country")
        String country;
        @JsonProperty("postcode")
        String postcode;
        @JsonProperty("coordinates")
        Coordinates coordinates;
        @JsonProperty("timezone")
        Timezone timezone;
    }

    @Getter
    @Setter
    public static class Login{
        @JsonProperty("uuid")
        String uuid;
        @JsonProperty("username")
        String username;
        @JsonProperty("password")
        String password;
        @JsonProperty("salt")
        String salt;
        @JsonProperty("md5")
        String md5;
        @JsonProperty("sha1")
        String sha1;
        @JsonProperty("sha256")
        String sha256;
    }

    @Getter
    @Setter
    public static class Name{
        @JsonProperty("title")
        String title;
        @JsonProperty("first")
        String first;
        @JsonProperty("last")
        String last;
    }

    @Getter
    @Setter
    public static class Picture{
        @JsonProperty("large")
        String large;
        @JsonProperty("medium")
        String medium;
        @JsonProperty("thumbnail")
        String thumbnail;
    }

    @Getter
    @Setter
    public static class Registered{
        @JsonProperty("date")
        Date date;
        @JsonProperty("age")
        int age;
    }

    @Getter
    @Setter
    public static class Result{
        @JsonProperty("gender")
        String gender;
        @JsonProperty("name")
        Name name;
        @JsonProperty("location")
        Location location;
        @JsonProperty("email")
        String email;
        @JsonProperty("login")
        Login login;
        @JsonProperty("dob")
        Dob dob;
        @JsonProperty("registered")
        Registered registered;
        @JsonProperty("phone")
        String phone;
        @JsonProperty("cell")
        String cell;
        @JsonProperty("id")
        Id id;
        @JsonProperty("picture")
        Picture picture;
        @JsonProperty("nat")
        String nat;
    }

    @Getter
    @Setter
    public static class Street{
        @JsonProperty("number")
        int number;
        @JsonProperty("name")
        String name;
    }

    @Getter
    @Setter
    public static class Timezone{
        @JsonProperty("offset")
        String offset;
        @JsonProperty("description")
        String description;
    }

    @JsonProperty("results")
    ArrayList<Result> results;
    @JsonProperty("info")
    Info info;
}
