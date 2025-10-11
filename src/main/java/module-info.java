module it.uniroma2.hoophub {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens it.uniroma2.hoophub to javafx.fxml;
    exports it.uniroma2.hoophub;
}