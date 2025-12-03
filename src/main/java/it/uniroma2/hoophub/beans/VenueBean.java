package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.enums.VenueType;

/**
 * Bean for venue data transfer.
 * Used when VenueManagers create or modify venues via UI.
 */
public class VenueBean {
    private int id;
    private String name;
    private VenueType type;
    private String address;
    private String city;
    private int maxCapacity;
    private String venueManagerUsername;

    private VenueBean(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.address = builder.address;
        this.city = builder.city;
        this.maxCapacity = builder.maxCapacity;
        this.venueManagerUsername = builder.venueManagerUsername;
    }

    public static class Builder {
        private int id;
        private String name;
        private VenueType type;
        private String address;
        private String city;
        private int maxCapacity;
        private String venueManagerUsername;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(VenueType type) {
            this.type = type;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder maxCapacity(int maxCapacity) {
            this.maxCapacity = maxCapacity;
            return this;
        }

        public Builder venueManagerUsername(String venueManagerUsername) {
            this.venueManagerUsername = venueManagerUsername;
            return this;
        }

        public VenueBean build() {
            return new VenueBean(this);
        }
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public VenueType getType() { return type; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public int getMaxCapacity() { return maxCapacity; }
    public String getVenueManagerUsername() { return venueManagerUsername; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(VenueType type) { this.type = type; }
    public void setAddress(String address) { this.address = address; }
    public void setCity(String city) { this.city = city; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public void setVenueManagerUsername(String username) {
        this.venueManagerUsername = username;
    }
}
