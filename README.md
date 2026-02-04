# 🏀 HoopHub - NBA Ticketing System

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=EliaCinti_HoopHub&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=EliaCinti_HoopHub)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=EliaCinti_HoopHub&metric=bugs)](https://sonarcloud.io/summary/new_code?id=EliaCinti_HoopHub)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=EliaCinti_HoopHub&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=EliaCinti_HoopHub)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=EliaCinti_HoopHub&metric=coverage)](https://sonarcloud.io/summary/new_code?id=EliaCinti_HoopHub)

**HoopHub** è un'applicazione gestionale progettata per la prenotazione di biglietti per partite NBA. Il sistema permette la gestione completa di eventi sportivi, venue e prenotazioni, offrendo un'esperienza differenziata per Tifosi e Gestori di Venue.

Il progetto è sviluppato in **Java** seguendo rigorosi principi di Ingegneria del Software, con particolare attenzione ai **Design Pattern GoF** e all'architettura software.

---

## 🚀 Caratteristiche Principali

* **Doppia Interfaccia Utente:**
    * 🖥️ **GUI (JavaFX):** Interfaccia grafica moderna e intuitiva.
    * 💻 **CLI (Command Line Interface):** Interfaccia testuale completa per ambienti terminale.
* **Persistenza Ibrida & Sincronizzazione Real-Time:**
    * Supporto per **MySQL** (Database Relazionale).
    * Supporto per **CSV** (File System locale).
    * **Master-Slave Sync:** Sincronizzazione automatica all'avvio.
    * **Observer Sync:** Sincronizzazione bidirezionale in tempo reale tra le due persistenze (modificando il CSV si aggiorna il DB e viceversa).
* **Gestione Ruoli:**
    * **Fan:** Ricerca partite, prenota posti, visualizza storico.
    * **Venue Manager:** Crea e gestisce Venue, accetta/rifiuta prenotazioni, gestisce capienza.

---

## 🏗️ Architettura e Design Pattern

Il progetto adotta l'architettura **MVC (Model-View-Controller)** separando nettamente la logica di business, l'interfaccia utente e la gestione dei dati.

### Design Pattern GoF Implementati

Durante lo sviluppo sono stati integrati numerosi pattern per garantire manutenibilità ed estensibilità:

1.  **Singleton:** Utilizzato per classi che necessitano di un'istanza unica globale, come `DaoFactoryFacade` (gestione persistenza), `SessionManager` (sessione utente) e `NavigatorSingleton` (navigazione GUI).
2.  **Abstract Factory:** Implementato per la creazione delle famiglie di DAO (`DaoAbstractFactory`), permettendo di istanziare DAO per MySQL, CSV o In-Memory senza modificare il codice client.
3.  **Factory Method:** Usato internamente alle factory concrete per la generazione dei singoli DAO specifici (es. `FanDao`, `BookingDao`).
4.  **Facade:** La classe `DaoFactoryFacade` agisce da interfaccia unificata, nascondendo la complessità del meccanismo di persistenza e dello switch dinamico ai controller.
5.  **Observer:**
    * Utilizzato massicciamente per la **sincronizzazione dei dati**. La classe `CrossPersistenceSyncObserver` osserva le modifiche su un supporto (es. CSV) e le replica sull'altro (es. MySQL).
    * Gestione delle **Notifiche** agli utenti.
6.  **Builder:** Implementato nelle classi del Model (`Booking`, `User`, `Venue`) per semplificare la creazione di oggetti complessi e garantire l'immutabilità dove necessario.
7.  **Adapter:** La classe `NbaApiAdapter` adatta i dati provenienti dal servizio esterno (simulato) `MockNbaScheduleApi` al formato atteso dal dominio dell'applicazione.
8.  **Template Method:** Utilizzato nella gerarchia dei Controller CLI (`AbstractCliHomepageController`) e nella classe base dei DAO (`AbstractObservableDao`) per definire lo scheletro degli algoritmi, delegando i passaggi specifici alle sottoclassi.

---

## 🛠️ Tech Stack

* **Linguaggio:** Java 21+
* **Build Tool:** Maven
* **GUI Framework:** JavaFX
* **Database:** MySQL
* **Quality Assurance:** SonarCloud (Analisi statica del codice)
* **Librerie:** OpenCSV (gestione CSV), MySQL Connector.

---

## 📂 Struttura del Progetto

* `src/main/java/it/uniroma2/hoophub`
    * `app_controller`: Controller applicativi (Logica di business).
    * `graphic_controller`: Controller grafici (Gestione input GUI/CLI).
    * `dao`: Layer di persistenza (Interfacce e Implementazioni MySQL/CSV).
    * `model`: Classi entità del dominio.
    * `beans`: Oggetti per il trasferimento dati tra layer (DTO).
    * `patterns`: Implementazione dei pattern (Observer, Factory, Facade, Adapter).
    * `sync`: Gestori della sincronizzazione dati.

---

## 🔧 Installazione e Configurazione

1.  **Prerequisiti:** JDK 21 o superiore, Maven, MySQL Server.
2.  **Database:** Eseguire lo script `hoophub_script.sql` (presente nella cartella `db/mysql`) per creare il database.
3.  **Configurazione:** Verificare il file `config.properties` per le credenziali del database.
4.  **Build:**
    ```bash
    mvn clean install
    ```
5.  **Esecuzione:**
    * GUI: `mvn javafx:run`
    * CLI: Eseguire il Jar generato specificando l'argomento `cli`.

---

## 🤝 Contributing

This is a university project for the ISPW course (Software Engineering and Web Programming). While it's primarily an individual project, suggestions and feedback are welcome!

### Development Guidelines
- Follow Java naming conventions
- Write meaningful commit messages
- Document all public methods
- Maintain test coverage above 70%
- Update UML diagrams when adding new classes

## 🙏 Acknowledgments

- **ISPW Course** - Tor Vergata University of Rome
- **NBA API** - For providing game schedules (via MockAPI)
- **JavaFX Community** - For excellent documentation and support
- **Design Patterns: Elements of Reusable Object-Oriented Software** - Gang of Four

## 📬 Contact

**Project Creator**: Elia Cinti
**University**: Tor Vergata - Roma  
**Course**: ISPW (Ingegneria del Software e Progettazione Web)  
**Academic Year**: 2025/2026

---

<p >
  Made with ❤️ and ☕ for ISPW Course
  <br>
  <i>Because watching NBA games is better together!</i>
</p>
