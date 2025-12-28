module it.uniroma2.hoophub {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.opencsv;
    requires jbcrypt;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;
    requires java.logging;
    requires java.sql;

    opens it.uniroma2.hoophub to javafx.fxml;
    exports it.uniroma2.hoophub;
    exports it.uniroma2.hoophub.app_controller;
    exports it.uniroma2.hoophub.model;
    exports it.uniroma2.hoophub.dao;
    exports it.uniroma2.hoophub.beans;
    exports it.uniroma2.hoophub.patterns.adapter;

    exports it.uniroma2.hoophub.api;
    exports it.uniroma2.hoophub.api.dto;
    opens it.uniroma2.hoophub.api.dto to com.fasterxml.jackson.databind;

    exports it.uniroma2.hoophub.exception;
    exports it.uniroma2.hoophub.patterns.observer;

    opens it.uniroma2.hoophub.app_controller to javafx.fxml;
    exports it.uniroma2.hoophub.enums;
    opens it.uniroma2.hoophub.dao to javafx.fxml;

    exports it.uniroma2.hoophub.utilities;
    opens it.uniroma2.hoophub.utilities to javafx.fxml;

    exports it.uniroma2.hoophub.dao.helper_dao;
    opens it.uniroma2.hoophub.dao.helper_dao to javafx.fxml;
}