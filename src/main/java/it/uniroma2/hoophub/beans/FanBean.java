package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.model.TeamNBA;
import java.time.LocalDate;

/**
 * Bean for transferring fan data between layers.
 * Contains minimal validation - business rules are enforced in the Model.
 */
public class FanBean extends UserBean {
    private TeamNBA favTeam;
    private LocalDate birthday;

    private FanBean(Builder builder) {
        super(builder);
        this.favTeam = builder.favTeam;
        this.birthday = builder.birthday;
    }

    public static class Builder extends UserBean.Builder<Builder> {
        private TeamNBA favTeam;
        private LocalDate birthday;

        public Builder() {
            super();
        }

        public Builder favTeam(TeamNBA favTeam) {
            this.favTeam = favTeam;
            return this;
        }

        public Builder birthday(LocalDate birthday) {
            this.birthday = birthday;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public FanBean build() {
            return new FanBean(this);
        }
    }

    public TeamNBA getFavTeam() {
        return favTeam;
    }

    public void setFavTeam(TeamNBA favTeam) {
        this.favTeam = favTeam;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }
}