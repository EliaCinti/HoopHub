module it.uniroma2.hoophub {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires com.opencsv;
    requires jbcrypt;

    opens it.uniroma2.hoophub to javafx.fxml;
    exports it.uniroma2.hoophub;
    exports it.uniroma2.hoophub.app_controller;
    exports it.uniroma2.hoophub.model;
    exports it.uniroma2.hoophub.dao;
    exports it.uniroma2.hoophub.beans;
    exports it.uniroma2.hoophub.exception;
    exports it.uniroma2.hoophub.patterns.observer;
    exports it.uniroma2.hoophub.utilities;
    opens it.uniroma2.hoophub.app_controller to javafx.fxml;
    exports it.uniroma2.hoophub.dao.helper_dao;
    exports it.uniroma2.hoophub.enums;
}