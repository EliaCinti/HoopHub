module it.uniroma2.hoophub {
    // --- DIPENDENZE (Requires) ---
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

    // --- APERTURE A JAVAFX (Opens) ---
    // Fondamentali per permettere a @FXML di iniettare i componenti
    opens it.uniroma2.hoophub to javafx.fxml;
    opens it.uniroma2.hoophub.app_controller to javafx.fxml;

    // Senza queste righe, JavaFX non riesce a caricare i controller della GUI (es. LoginGraphicController)
    opens it.uniroma2.hoophub.graphic_controller.gui to javafx.fxml;
    opens it.uniroma2.hoophub.graphic_controller.cli to javafx.fxml;
    opens it.uniroma2.hoophub.graphic_controller.gui.components to javafx.fxml;

    opens it.uniroma2.hoophub.dao to javafx.fxml;
    opens it.uniroma2.hoophub.utilities to javafx.fxml;
    opens it.uniroma2.hoophub.dao.helper_dao to javafx.fxml;

    // --- APERTURE A JACKSON (JSON) ---
    // Permette alla libreria Jackson di leggere e scrivere dentro i tuoi DTO
    opens it.uniroma2.hoophub.api.dto to com.fasterxml.jackson.databind;

    // --- ESPORTAZIONI (Exports) ---
    exports it.uniroma2.hoophub;

    // CRUCIALE: Permette a JavaFX di vedere e avviare la classe GuiApplication
    exports it.uniroma2.hoophub.launcher to javafx.graphics;

    exports it.uniroma2.hoophub.app_controller;
    exports it.uniroma2.hoophub.model;
    exports it.uniroma2.hoophub.dao;
    exports it.uniroma2.hoophub.beans;
    exports it.uniroma2.hoophub.patterns.adapter;
    exports it.uniroma2.hoophub.api;
    exports it.uniroma2.hoophub.api.dto;
    exports it.uniroma2.hoophub.exception;
    exports it.uniroma2.hoophub.patterns.observer;
    exports it.uniroma2.hoophub.enums;
    exports it.uniroma2.hoophub.utilities;
    exports it.uniroma2.hoophub.dao.helper_dao;
    opens it.uniroma2.hoophub.graphic_controller.gui.sign_up to javafx.fxml;
}