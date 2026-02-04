# 🏀 HoopHub - NBA Venue & Ticketing System

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=EliaCinti_HoopHub&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=EliaCinti_HoopHub)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=EliaCinti_HoopHub&metric=coverage)](https://sonarcloud.io/summary/new_code?id=EliaCinti_HoopHub)
[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/GUI-JavaFX-blue)](https://openjfx.io/)

**HoopHub** è un ecosistema digitale che connette i tifosi NBA con le migliori location dove guardare le partite. A differenza dei classici sistemi di prenotazione, HoopHub è specializzato nell'esperienza "Watch Party": permette ai Fan di prenotare posti specifici in Venue (pub, bar, arene sportive) che trasmettono le loro squadre del cuore, e ai Venue Manager di gestire eventi e capienza in tempo reale.

Il progetto è sviluppato in **Java** seguendo rigorosi principi di Ingegneria del Software, implementando un'architettura **MVC** supportata da **8 Design Pattern GoF**.

---

## 🚀 Caratteristiche Principali

* **Doppia Interfaccia (GUI & CLI):** Esperienza utente completa sia tramite interfaccia grafica JavaFX moderna, sia tramite riga di comando per ambienti server/legacy.
* **Persistenza Polimorfica:** Il sistema supporta tre modalità di salvataggio dati, intercambiabili a runtime:
    * 🐘 **MySQL:** Per ambienti di produzione robusti.
    * 📂 **CSV:** Per portabilità e file-system locale.
    * 🧠 **In-Memory:** Per testing veloce e sessioni volatili.
* **Sincronizzazione Bidirezionale (Real-Time):** Grazie al pattern Observer, le modifiche effettuate su CSV si riflettono istantaneamente su MySQL e viceversa, garantendo consistenza dei dati anche in ambienti distribuiti.
* **Sistema di Notifiche:** Gestione automatica delle notifiche per conferme prenotazioni o variazioni di palinsesto.

---

## 🆚 Analisi Competitor

HoopHub si posiziona in una nicchia specifica, colmando il vuoto tra la semplice ricerca di un locale e la garanzia del posto a sedere per l'evento sportivo.

| Competitor              | Descrizione                                       | Pro vs HoopHub                                       | Contro vs HoopHub                                                                                                                                                    |
|:------------------------|:--------------------------------------------------|:-----------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **MatchPint (Fanzo)**   | App leader per trovare bar che trasmettono sport. | Vasto database globale di locali sportivi.           | **Manca la prenotazione integrata:** spesso rimanda al telefono del locale o a form esterni. Non gestisce la capienza in tempo reale.                                |
| **TheFork / OpenTable** | Piattaforme leader per prenotazione tavoli.       | Eccellente gestione di posti e orari nei ristoranti. | **Manca la logica sportiva:** non sanno "chi gioca stasera". L'utente deve cercare manualmente se il locale ha la TV e se trasmette la partita desiderata.           |
| **Google Maps**         | Mappe e ricerca locali generica.                  | Standard assoluto per trovare luoghi e recensioni.   | **Nessuna info sul palinsesto TV:** impossibile sapere con certezza se una specifica partita NBA verrà trasmessa. Nessuna gestione prenotazioni dedicata allo sport. |

✅ **Il Vantaggio di HoopHub:** Unisce la conoscenza del calendario NBA (tramite Adapter API) con la logica di prenotazione posti (Ticketing), offrendo la certezza di sedersi davanti allo schermo giusto.

---

## 🏗️ Architettura e Design Pattern

Il sistema è basato su architettura **MVC (Model-View-Controller)**. Una caratteristica distintiva è l'uso massiccio dei **Design Pattern della Gang of Four (GoF)** per garantire estensibilità e manutenibilità.

### Pattern Creazionali
1.  **Singleton:** Utilizzato per `DaoFactoryFacade` (punto di accesso unico ai dati), `NavigatorSingleton` (gestore scene JavaFX), `SessionManager` e `ObserverFactory`.
2.  **Abstract Factory:** L'interfaccia `DaoAbstractFactory` permette di creare famiglie di DAO (MySQL, CSV, In-Memory) astraendo la logica di creazione dal client.
3.  **Factory Method:** Implementato nelle factory concrete (`MySqlDaoFactory`, `CsvDaoFactory`) per incapsulare l'istanziazione dei singoli DAO specifici.
4.  **Builder:** Utilizzato nelle classi di modello (`User`, `Fan`, `Booking`) e nei Bean (`FanBean`, etc.) per la costruzione fluente di oggetti complessi e immutabili.

### Pattern Strutturali
1. **Facade:** La classe `DaoFactoryFacade` nasconde la complessità del sottosistema di persistenza e sincronizzazione. I controller richiedono semplicemente i dati (es. `getBookingDao()`), ignorando se provengano da Database, File o RAM.
2. **Adapter:** La classe `NbaApiAdapter` funge da ponte tra il servizio esterno `MockNbaScheduleApi` e l'interfaccia interna `NbaScheduleService`, adattando i dati esterni al dominio applicativo.

### Pattern Comportamentali
1. **Observer:** Cuore della **Sincronizzazione Dati**. `CrossPersistenceSyncObserver` osserva le modifiche su un supporto (es. CSV) e le replica sull'altro (es. MySQL). Usato anche per le notifiche utente in tempo reale (`NotificationBookingObserver`).
2. **Template Method:** Definito in `AbstractController` e `AbstractCliHomepageController` per stabilire lo scheletro dell'algoritmo di gestione flussi, delegando i passaggi specifici alle sottoclassi concrete.

---

## 📂 Struttura del Codice

La codebase è organizzata in package logici per favorire la separazione delle responsabilità:

* `it.uniroma2.hoophub.model`: Entità del dominio (Business Logic Core).
* `it.uniroma2.hoophub.dao`: Interfacce e implementazioni per l'accesso ai dati (MySQL, CSV, In-Memory).
* `it.uniroma2.hoophub.beans`: **JavaBeans (DTO)** per il trasferimento dati tra View e Controller.
* `it.uniroma2.hoophub.app_controller`: Controller applicativi (Logica di business e gestione sessione).
* `it.uniroma2.hoophub.graphic_controller`: Controller grafici per gestire l'interazione utente (JavaFX e CLI).
* `it.uniroma2.hoophub.patterns`: Implementazioni pure dei pattern (Factory, Observer, Facade, Adapter).
* `it.uniroma2.hoophub.sync`: Gestori della logica di sincronizzazione tra persistenze diverse.

---

## 🔧 Installazione e Avvio

1.  **Requisiti:** JDK 21+, Maven, MySQL Server.
2.  **Database:** Eseguire lo script `db/mysql/hoophub_script.sql` per inizializzare lo schema.
3.  **Configurazione:** Verificare le credenziali in `src/main/resources/config.properties`.
4.  **Compilazione:**
    ```bash
    mvn clean install
    ```
5.  **Esecuzione:**
    * **GUI:** `mvn javafx:run`
    * **CLI:** Eseguire il JAR generato con il flag `--cli`.

---

## 👨‍💻 Autore

**Elia Cinti**
* Università di Roma Tor Vergata
* Corso: Ingegneria del Software e Progettazione Web (ISPW)
* Anno Accademico: 2025/2026

<p >
  <i>"Basketball is a game of details."</i><br>
  Made with ❤️ and ☕ for ISPW Course
</p>