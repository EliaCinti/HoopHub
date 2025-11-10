# 📚 Documentazione Completa - Architettura HoopHub

## Indice
1. [Pattern GoF Utilizzati](#1-pattern-gof-utilizzati)
2. [Factory Pattern - Architettura DAO](#2-factory-pattern---architettura-dao)
3. [Bean Pattern - Comunicazione tra Layer](#3-bean-pattern---comunicazione-tra-layer)
4. [Principi SOLID e Best Practice](#4-principi-solid-e-best-practice)
5. [Polimorfismo e Type System](#5-polimorfismo-e-type-system)

---

## 1. Pattern GoF Utilizzati

### 1.1 Singleton Pattern (Creational)

**Definizione GoF**: "Assicura che una classe abbia una sola istanza e fornisce un punto di accesso globale ad essa."

#### 📍 Implementazioni nel Progetto:

**A. LoginController (Enum-based Singleton)**
- **File**: `src/main/java/it/uniroma2/hoophub/app_controller/LoginController.java:26-50`
- **Implementazione**:
```java
public class LoginController extends AbstractController {
    private static LoginController instance;

    private LoginController() { }  // Costruttore privato

    public static synchronized LoginController getInstance() {
        if (instance == null) {
            instance = new LoginController();
        }
        return instance;
    }
}
```
- **Motivazione**: Controller applicativo gestisce logica di business senza stato UI-specifico. Una sola istanza condivisa da tutte le boundary garantisce coerenza.
- **Uso**: `LoginGraphicController.java:32`, `CliLoginGraphicController.java:55`

**B. SessionManager (Enum Singleton)**
- **File**: `src/main/java/it/uniroma2/hoophub/session/SessionManager.java:30`
- **Implementazione**:
```java
public enum SessionManager {
    INSTANCE;
    private User currentUser;
    // ...
}
```
- **Motivazione**: Gestisce sessione utente globale. Pattern Enum garantisce thread-safety e protezione da reflection/serialization.
- **Vantaggi**: Più sicuro del Singleton classico (Effective Java, Joshua Bloch)

**C. DaoFactoryFacade**
- **File**: `src/main/java/it/uniroma2/hoophub/patterns/facade/DaoFactoryFacade.java:40-76`
- **Implementazione**:
```java
public class DaoFactoryFacade {
    private static DaoFactoryFacade instance;

    private DaoFactoryFacade() { }

    public static synchronized DaoFactoryFacade getInstance() {
        if (instance == null) {
            instance = new DaoFactoryFacade();
        }
        return instance;
    }
}
```
- **Motivazione**: Punto unico di accesso a tutte le factory DAO

**D. NavigatorSingleton**
- **File**: `src/main/java/it/uniroma2/hoophub/utilities/NavigatorSingleton.java`
- **Uso**: Gestione navigazione UI globale

**E. SignUpDataSingleton**
- **File**: `src/main/java/it/uniroma2/hoophub/utilities/SignUpDataSingleton.java`
- **Uso**: Memorizzazione temporanea dati registrazione

---

### 1.2 Factory Method Pattern (Creational)

**Definizione GoF**: "Definisce un'interfaccia per creare un oggetto, ma lascia alle sottoclassi decidere quale classe istanziare."

#### 📍 Implementazioni nel Progetto:

**A. UserDaoFactory**
- **File**: `src/main/java/it/uniroma2/hoophub/patterns/factory/UserDaoFactory.java:17`
- **Struttura**:
```java
public class UserDaoFactory {
    public UserDao getUserDao(PersistenceType type) {
        return switch (type) {
            case CSV -> createUserDaoCsv();
            case MYSQL -> createUserDaoMySql();
        };
    }

    private UserDao createUserDaoCsv() { return new UserDaoCsv(); }
    private UserDao createUserDaoMySql() { return new UserDaoMySql(); }
}
```
- **Prodotto Astratto**: `UserDao` (interface)
- **Prodotti Concreti**: `UserDaoCsv`, `UserDaoMySql`

**B. FanDaoFactory**
- **File**: `src/main/java/it/uniroma2/hoophub/patterns/factory/FanDaoFactory.java:22`
- **Caratteristica Speciale**: Inietta dipendenza `UserDao` via costruttore
```java
public FanDao getFanDao(PersistenceType type) {
    UserDao userDao = new UserDaoFactory().getUserDao(type);
    return switch (type) {
        case CSV -> new FanDaoCsv(userDao);
        case MYSQL -> new FanDaoMySql(userDao);
    };
}
```

**C. VenueManagerDaoFactory**
- **File**: `src/main/java/it/uniroma2/hoophub/patterns/factory/VenueManagerDaoFactory.java:22`
- **Simile a FanDaoFactory**: Inietta `UserDao`

**D. VenueDaoFactory**
- **File**: `src/main/java/it/uniroma2/hoophub/patterns/factory/VenueDaoFactory.java`
- **Prodotto**: `VenueDao` → `VenueDaoCsv` | `VenueDaoMySql`

**E. BookingDaoFactory**
- **File**: `src/main/java/it/uniroma2/hoophub/patterns/factory/BookingDaoFactory.java`
- **Prodotto**: `BookingDao` → `BookingDaoCsv` | `BookingDaoMySql`

**F. CliViewFactory**
- **File**: `src/main/java/it/uniroma2/hoophub/utilities/CliViewFactory.java`
- **Prodotto**: Diversi tipi di view CLI

---

### 1.3 Abstract Factory Pattern (Creational)

**Definizione GoF**: "Fornisce un'interfaccia per creare famiglie di oggetti correlati senza specificarne le classi concrete."

#### 📍 Implementazione: DaoFactoryFacade

- **File**: `src/main/java/it/uniroma2/hoophub/patterns/facade/DaoFactoryFacade.java:40`

**Concetto Chiave**: Coordina TUTTE le factory per creare famiglie coerenti di DAO

**Famiglie di Prodotti**:
```
Famiglia CSV:              Famiglia MySQL:
├── UserDaoCsv            ├── UserDaoMySql
├── FanDaoCsv             ├── FanDaoMySql
├── VenueManagerDaoCsv    ├── VenueManagerDaoMySql
├── VenueDaoCsv           ├── VenueDaoMySql
└── BookingDaoCsv         └── BookingDaoMySql
```

**Codice**:
```java
public class DaoFactoryFacade {
    private PersistenceType persistenceType;

    public void setPersistenceType(PersistenceType type) {
        this.persistenceType = type;
        // Clear cache per garantire coerenza
        clearDaoCache();
    }

    public UserDao getUserDao() {
        if (userDao == null) {
            userDao = new UserDaoFactory().getUserDao(this.persistenceType);
            attachObserver(userDao);
        }
        return userDao;
    }

    public FanDao getFanDao() { /* simile */ }
    public VenueManagerDao getVenueManagerDao() { /* simile */ }
    // ...
}
```

**Uso nel Codice**:
```java
// LoginController.java:70
DaoFactoryFacade facade = DaoFactoryFacade.getInstance();
UserDao userDao = facade.getUserDao();  // Ottiene DAO della famiglia configurata
FanDao fanDao = facade.getFanDao();      // Stessa famiglia
```

**Benefici**:
- ✅ Garantisce che tutti i DAO usino la stessa persistenza (CSV o MySQL)
- ✅ Cambio di famiglia con un solo setPersistenceType()
- ✅ Nessuna inconsistenza (es. UserDaoCsv + FanDaoMySql)

---

### 1.4 Facade Pattern (Structural)

**Definizione GoF**: "Fornisce un'interfaccia unificata a un insieme di interfacce in un sottosistema."

#### 📍 Implementazione: DaoFactoryFacade (doppio ruolo!)

- **File**: `src/main/java/it/uniroma2/hoophub/patterns/facade/DaoFactoryFacade.java:40`

**Doppio Pattern**:
1. **Abstract Factory**: Crea famiglie di DAO
2. **Facade**: Semplifica accesso alle factory

**Senza Facade** (complessità esposta):
```java
// ❌ Client deve conoscere tutte le factory
UserDaoFactory userFactory = new UserDaoFactory();
FanDaoFactory fanFactory = new FanDaoFactory();
VenueManagerDaoFactory vmFactory = new VenueManagerDaoFactory();

UserDao userDao = userFactory.getUserDao(PersistenceType.MYSQL);
FanDao fanDao = fanFactory.getFanDao(PersistenceType.MYSQL);
// ... ripetitivo e error-prone
```

**Con Facade** (interfaccia semplificata):
```java
// ✅ Un solo punto di accesso
DaoFactoryFacade facade = DaoFactoryFacade.getInstance();
facade.setPersistenceType(PersistenceType.MYSQL);

UserDao userDao = facade.getUserDao();
FanDao fanDao = facade.getFanDao();
// ... semplice e coerente
```

**Sottosistema Nascosto**:
- `UserDaoFactory`
- `FanDaoFactory`
- `VenueManagerDaoFactory`
- `VenueDaoFactory`
- `BookingDaoFactory`
- Observer attachment
- Caching DAO

---

### 1.5 Observer Pattern (Behavioral)

**Definizione GoF**: "Definisce una dipendenza uno-a-molti tra oggetti, così che quando un oggetto cambia stato, tutti i suoi dipendenti sono notificati."

#### 📍 Implementazione: Cross-Persistence Sync

**Struttura**:

**A. Subject (Observable)**:
- **Interface**: `ObservableDao` (`patterns/observer/ObservableDao.java:12`)
```java
public interface ObservableDao {
    void addObserver(DaoObserver observer);
    void removeObserver(DaoObserver observer);
    void notifyObservers(DaoOperation op, String entityType, String id, Object entity);
}
```

- **Implementation**: `AbstractObservableDao` (`dao/AbstractObservableDao.java:19`)
```java
public abstract class AbstractObservableDao implements ObservableDao {
    private final List<DaoObserver> observers = new ArrayList<>();

    @Override
    public void notifyObservers(DaoOperation operation, ...) {
        for (DaoObserver observer : observers) {
            switch (operation) {
                case INSERT -> observer.onAfterInsert(...);
                case UPDATE -> observer.onAfterUpdate(...);
                case DELETE -> observer.onAfterDelete(...);
            }
        }
    }
}
```

**B. Observer**:
- **Interface**: `DaoObserver` (`patterns/observer/DaoObserver.java`)
- **Concrete Observer**: `CrossPersistenceSyncObserver` (`sync/CrossPersistenceSyncObserver.java`)

**Funzionamento**:
```
MySQL DAO (Subject)  ──notify──▶  CSV Sync Observer
   ↓ save()                           ↓ onAfterInsert()
   ↓                                   ↓ Scrive in CSV
   ↓◀────────────────────────────────┘
```

**Esempio Concreto** (FanDaoMySql.java:114):
```java
public void saveFan(FanBean fanBean) throws DAOException {
    // 1. Salva in MySQL
    stmt.executeUpdate();

    // 2. Notifica observer
    notifyObservers(DaoOperation.INSERT, "Fan", fanBean.getUsername(), fanBean);
    //                                    ↓
    //          CrossPersistenceSyncObserver riceve notifica
    //                                    ↓
    //                          Sincronizza su CSV
}
```

**Attachment** (DaoFactoryFacade.java:123-128):
```java
public UserDao getUserDao() {
    if (userDao == null) {
        userDao = new UserDaoFactory().getUserDao(this.persistenceType);
        if (this.persistenceType == PersistenceType.MYSQL) {
            ((ObservableDao) userDao).addObserver(mysqlToCsvObserver);
        } else {
            ((ObservableDao) userDao).addObserver(csvToMysqlObserver);
        }
    }
    return userDao;
}
```

---

### 1.6 Builder Pattern (Creational)

**Definizione GoF**: "Separa la costruzione di un oggetto complesso dalla sua rappresentazione."

#### 📍 Implementazioni nel Progetto:

**A. Model Builders** (con validazione):

**User.Builder** (`model/User.java:103`):
```java
public static class Builder<T extends Builder<T>> {
    protected String username;
    protected String fullName;
    protected String gender;

    public T username(String username) {
        this.username = username;
        return self();
    }

    public T fullName(String fullName) { /* ... */ }
    public T gender(String gender) { /* ... */ }

    protected void validate() {
        validateUsername(username);
        validateFullName(fullName);
        validateGender(gender);
    }
}
```

**Fan.Builder** (`model/Fan.java:195`):
```java
public static class Builder extends User.Builder<Builder> {
    private String favTeam;
    private LocalDate birthday;
    private List<Booking> bookingList;

    public Fan build() {
        validate();  // Validazione prima della costruzione
        return new Fan(this);
    }

    @Override
    protected void validate() {
        super.validate();  // Chiama validazione User
        validateFavTeam(favTeam);
        validateBirthday(birthday);
    }
}
```

**Uso**:
```java
Fan fan = new Fan.Builder()
    .username("john_doe")
    .fullName("John Doe")
    .gender("M")
    .favTeam("Lakers")
    .birthday(LocalDate.of(1990, 5, 15))
    .bookingList(Collections.emptyList())
    .build();  // Validazione automatica qui
```

**B. Bean Builders** (senza validazione):

**UserBean.Builder** (`beans/UserBean.java:18`):
```java
public static class Builder<T extends Builder<T>> extends CredentialsBean.Builder<T> {
    private String fullName;
    private String gender;

    public UserBean build() {
        return new UserBean(this);  // Nessuna validazione (solo dati)
    }
}
```

**Uso nel LoginController** (LoginController.java:98-103):
```java
private UserBean convertUserToBean(User user) {
    return new UserBean.Builder<>()
        .username(user.getUsername())
        .fullName(user.getFullName())
        .gender(user.getGender())
        .type(user.getUserType().toString())
        .build();
}
```

**Altri Builders**:
- `VenueManager.Builder` (`model/VenueManager.java:202`)
- `Venue.Builder` (`model/Venue.java`)
- `Booking.Builder` (`model/Booking.java`)
- `CredentialsBean.Builder` (`beans/CredentialsBean.java:17`)
- `FanBean.Builder` (`beans/FanBean.java`)
- `VenueManagerBean.Builder` (`beans/VenueManagerBean.java`)

**Benefici**:
- ✅ Costruzione leggibile e fluent
- ✅ Validazione centralizzata (Model)
- ✅ Immutabilità garantita dopo costruzione
- ✅ Gestione parametri opzionali

---

### 1.7 Template Method Pattern (Behavioral)

**Definizione GoF**: "Definisce lo scheletro di un algoritmo in un'operazione, delegando alcuni passi alle sottoclassi."

#### 📍 Implementazioni nel Progetto:

**A. AbstractCsvDao** (`dao/csv/AbstractCsvDao.java:37`):

**Template**:
```java
public abstract class AbstractCsvDao extends AbstractObservableDao {
    protected final File csvFile;

    protected AbstractCsvDao(String filePath) {
        this.csvFile = new File(filePath);
        initializeCsvFile();  // Template method
    }

    // Hook method (deve essere implementato)
    protected abstract String[] getHeader();

    // Template operations (riutilizzabili)
    protected synchronized long getNextId(int idColumnIndex) { /* ... */ }
    protected synchronized String[] findRowByValue(int col, String val) { /* ... */ }
    protected synchronized boolean deleteById(long id, int idCol) { /* ... */ }

    private void initializeCsvFile() {
        if (!csvFile.exists()) {
            CsvUtilities.updateFile(csvFile, getHeader(), emptyData);
        }
    }
}
```

**Concrete Classes**:

**FanDaoCsv** (`dao/csv/FanDaoCsv.java:58`):
```java
public class FanDaoCsv extends AbstractCsvDao implements FanDao {
    private static final String[] CSV_HEADER = {"username", "fav_team", "birthday"};

    @Override
    protected String[] getHeader() {
        return CSV_HEADER;  // Implementa hook method
    }

    // Riusa metodi template:
    public synchronized void saveFan(FanBean fanBean) {
        long nextId = getNextId(0);  // ✅ Da AbstractCsvDao
        String[] row = findRowByValue(1, fanBean.getUsername());  // ✅ Da AbstractCsvDao
        // ...
    }
}
```

**Altri Concrete DAOs**:
- `UserDaoCsv` (header: `["username", "password_hash", "full_name", "gender", "type"]`)
- `VenueDaoCsv` (header: `["id", "name", "venue_type", ...]`)
- `VenueManagerDaoCsv`
- `BookingDaoCsv`
- `NotificationDaoCsv`

**B. AbstractMySqlDao** (`dao/mysql/AbstractMySqlDao.java`):

**Template per operazioni MySQL**:
```java
public abstract class AbstractMySqlDao extends AbstractObservableDao {
    protected void rollbackTransaction(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException e) { /* log */ }
        }
    }

    protected void resetAutoCommit(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); } catch (SQLException e) { /* log */ }
        }
    }

    protected void validateUsernameInput(String username) { /* ... */ }
    protected void validateUserBeanInput(UserBean bean) { /* ... */ }
}
```

---

## 2. Factory Pattern - Architettura DAO

### 2.1 Gerarchia Completa

```
                    DaoFactoryFacade (Abstract Factory + Facade + Singleton)
                            │
                ┌───────────┼───────────┐
                │           │           │
        UserDaoFactory  FanDaoFactory  VenueManagerDaoFactory ...
                │           │           │
        ┌───────┴───┐   ┌───┴───┐   ┌───┴───┐
    UserDaoCsv  UserDaoMySql  FanDaoCsv  FanDaoMySql ...
        │                   │
   AbstractCsvDao      AbstractMySqlDao
        │                   │
   AbstractObservableDao ───┘
        │
   ObservableDao (interface)
```

### 2.2 Flusso di Creazione DAO

**Scenario**: LoginController deve ottenere un UserDao

**Step-by-Step**:

```java
// 1. Controller chiede il facade (Singleton)
DaoFactoryFacade facade = DaoFactoryFacade.getInstance();

// 2. Controller specifica tipo persistenza (se necessario)
facade.setPersistenceType(PersistenceType.MYSQL);

// 3. Controller richiede UserDao
UserDao userDao = facade.getUserDao();
    │
    ├─▶ facade verifica cache (userDao == null?)
    │
    ├─▶ Se null, crea usando UserDaoFactory
    │       UserDaoFactory factory = new UserDaoFactory();
    │       UserDao dao = factory.getUserDao(persistenceType);
    │           │
    │           └─▶ switch(persistenceType) {
    │                   case MYSQL: return new UserDaoMySql();
    │                   case CSV:   return new UserDaoCsv();
    │               }
    │
    ├─▶ Attacca Observer per sync
    │       if (persistenceType == MYSQL) {
    │           dao.addObserver(mysqlToCsvObserver);
    │       } else {
    │           dao.addObserver(csvToMysqlObserver);
    │       }
    │
    ├─▶ Salva in cache
    │       this.userDao = dao;
    │
    └─▶ Ritorna DAO configurato
```

### 2.3 Dependency Injection nelle Factory

**Problema**: `FanDao` necessita di `UserDao` per operazioni comuni.

**Soluzione** (FanDaoFactory.java:35-43):
```java
public FanDao getFanDao(PersistenceType persistenceType) {
    // 1. Crea dipendenza usando UserDaoFactory
    UserDao userDao = new UserDaoFactory().getUserDao(persistenceType);

    // 2. Inietta dipendenza nel costruttore
    return switch (persistenceType) {
        case CSV   -> new FanDaoCsv(userDao);    // ✅ Dependency Injection
        case MYSQL -> new FanDaoMySql(userDao);  // ✅ Dependency Injection
    };
}
```

**Costruttore FanDaoMySql** (dao/mysql/FanDaoMySql.java:82):
```java
public FanDaoMySql(UserDao userDao) {
    this.userDao = userDao;  // ✅ Riceve dipendenza
}
```

**USO nel DAO**:
```java
public void saveFan(FanBean fanBean) {
    // Delega a UserDao per operazioni comuni
    userDao.saveUser(fanBean);  // ✅ Usa dipendenza iniettata

    // Gestisce solo dati fan-specific
    stmt.setString(1, fanBean.getFavTeam());
    stmt.setDate(2, Date.valueOf(fanBean.getBirthday()));
}
```

**Benefici**:
- ✅ Nessun `new UserDao()` dentro i DAO
- ✅ Factory garantisce coerenza (stesso tipo persistenza)
- ✅ Testabilità (mock UserDao facilmente)

### 2.4 Perché NO `new XxxDao()` diretto?

**❌ Violazione**:
```java
public class FanDaoMySql {
    public void saveFan(FanBean bean) {
        UserDao userDao = new UserDaoMySql();  // ❌ MALE!
        userDao.saveUser(bean);
    }
}
```

**Problemi**:
1. **Hard-coded dependency**: Se cambi persistenza, devi modificare tutti i DAO
2. **Nessun Observer**: Il DAO creato non ha observer per sync
3. **Nessun caching**: Nuova istanza ogni volta
4. **Viola Factory Pattern**: Bypassa il meccanismo di creazione

**✅ Soluzione Corretta**:
```java
public class FanDaoMySql {
    private final UserDao userDao;

    public FanDaoMySql(UserDao userDao) {  // ✅ Dependency Injection
        this.userDao = userDao;
    }

    public void saveFan(FanBean bean) {
        userDao.saveUser(bean);  // ✅ Usa dipendenza
    }
}
```

Oppure (BookingDaoMySql.java:517-522):
```java
private Booking mapResultSetToBooking(ResultSet rs) {
    DaoFactoryFacade facade = DaoFactoryFacade.getInstance();  // ✅ Usa Factory
    FanDao fanDao = facade.getFanDao();
    VenueDao venueDao = facade.getVenueDao();
    // ...
}
```

---

## 3. Bean Pattern - Comunicazione tra Layer

### 3.1 Problema Architetturale

**Scenario Problematico** (prima del refactoring):

```java
// Boundary
public class LoginGraphicController {
    public void onLoginClick() {
        User user = loginController.login(credentials);  // ❌ Riceve Model

        // ⚠️ PERICOLO: La boundary può chiamare metodi business!
        user.updateProfileDetails("nuovo", "M");  // ❌ Non dovrebbe poterlo fare!
        user.addBooking(booking);                 // ❌ Business logic in UI!

        // OK: Ma voleva solo il tipo
        UserType type = user.getUserType();
    }
}
```

**Problema**: La Boundary ha accesso a **TUTTA** la logica business del Model.

### 3.2 Soluzione: Bean Pattern

**Architettura Corretta**:

```
┌─────────────────────────────────────────────────────────┐
│                    BOUNDARY LAYER                       │
│  - LoginGraphicController                               │
│  - CliLoginGraphicController                            │
│                                                         │
│  Conosce SOLO: Bean (DTO con getter/setter)            │
│  Non conosce: Model (business logic)                    │
└─────────────────────────────────────────────────────────┘
                          ↕ Bean
┌─────────────────────────────────────────────────────────┐
│                 CONTROLLER LAYER                        │
│  - LoginController (Singleton)                          │
│                                                         │
│  Conosce: Bean (per comunicazione) + Model (interno)   │
│  Converte: Model → Bean (output)                        │
│  Converte: Bean → Model (input)                         │
└─────────────────────────────────────────────────────────┘
                          ↕ Model
┌─────────────────────────────────────────────────────────┐
│                    DAO LAYER                            │
│  Lavora con Model (User, Fan, VenueManager)            │
└─────────────────────────────────────────────────────────┘
```

### 3.3 Bean vs Model: Differenze

| **Aspetto** | **Bean (DTO)** | **Model (Domain)** |
|-------------|----------------|-------------------|
| **Scopo** | Trasferimento dati | Business logic |
| **Contenuto** | Solo getter/setter | Getter + metodi business |
| **Layer** | Boundary ↔ Controller | Controller ↔ DAO |
| **Validazione** | Minima/nessuna | Completa (in costruttore) |
| **Esempio metodi** | `getType()`, `setType()` | `getUserType()`, `updateProfile()`, `addBooking()` |
| **Mutabilità** | Mutabile (setter) | Immutabile dove possibile |

**UserBean**:
```java
public class UserBean extends CredentialsBean {
    private String fullName;
    private String gender;

    // SOLO getter/setter - ZERO logica
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    // ❌ NON ha: updateProfile(), validate(), ecc.
}
```

**User (Model)**:
```java
public abstract class User {
    private final String username;  // Immutabile
    private String fullName;
    private String gender;

    // Getter (read-only)
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getGender() { return gender; }

    // ✅ Metodo BUSINESS con validazione
    public void updateProfileDetails(String newFullName, String newGender) {
        validateFullName(newFullName);  // Validazione!
        validateGender(newGender);

        this.fullName = newFullName;
        this.gender = newGender;
    }

    // ✅ Metodo BUSINESS astratto (polimorfismo)
    public abstract UserType getUserType();

    // ❌ NON ha setter pubblici
}
```

### 3.4 Conversione Model → Bean

**Nel Controller** (LoginController.java:97-104):

```java
public UserBean login(CredentialsBean credentials) {
    // 1. Lavora con Model internamente
    User user = retrieveUserByType(credentials, daoFactoryFacade);

    // 2. Usa Model per business logic
    storeUserSession(user);  // SessionManager accetta Model

    // 3. Converte Model → Bean prima di restituire
    return convertUserToBean(user);  // ✅ Boundary riceve Bean
}

private UserBean convertUserToBean(User user) {
    return new UserBean.Builder<>()
        .username(user.getUsername())       // Copia dati
        .fullName(user.getFullName())       // Solo dati
        .gender(user.getGender())           // Nessuna logica
        .type(user.getUserType().toString()) // Converte enum → String
        .build();
}
```

**Flusso Completo**:

```
1. Boundary (UI)
   ↓ crea CredentialsBean (dati primitivi)

2. Controller riceve Bean
   ↓ converte Bean → Model (o recupera da DAO)

3. Controller lavora con Model
   ↓ chiama metodi business: validate(), getUserType(), ecc.

4. Controller salva Model in Session (interno)

5. Controller converte Model → Bean
   ↓ estrae solo dati necessari

6. Boundary riceve Bean
   ✅ Ha SOLO accesso a dati (getType(), getFullName())
   ❌ NON può chiamare updateProfile(), addBooking()
```

### 3.5 Uso nella Boundary

**LoginGraphicController** (LoginGraphicController.java:65-67):

```java
@FXML
private void onLoginClick() {
    // Input: Bean
    CredentialsBean credentialsBean = new CredentialsBean.Builder<>()
        .username(usernameText)
        .password(passwordText)
        .build();

    // Output: Bean (non Model!)
    UserBean userBean = loginController.login(credentialsBean);

    // Uso: Solo accesso a dati
    navigateToHomepage(userBean);
}

private void navigateToHomepage(UserBean userBean) {
    // ✅ Accesso a dati
    UserType type = UserType.valueOf(userBean.getType());

    // ❌ NON può fare:
    // userBean.updateProfile(...);  // Metodo non esiste!
    // userBean.addBooking(...);     // Metodo non esiste!

    // Navigazione basata su dati
    if (type == UserType.FAN) {
        navigatorSingleton.gotoPage("/FanHomepage.fxml");
    }
}
```

### 3.6 Benefici del Bean Pattern

1. **Separation of Concerns**:
   - Boundary: Gestisce UI e dati
   - Controller: Gestisce business logic
   - Model: Incapsula logica di dominio

2. **Security**:
   - Boundary non può chiamare metodi sensibili
   - Controller controlla quali dati esporre

3. **Testabilità**:
   - Bean facilmente mockabili
   - Test boundary senza dipendenze business logic

4. **Manutenibilità**:
   - Modifiche al Model non impattano Boundary
   - Cambio business logic isolato

5. **Conformità Architetturale**:
   - Rispetta principi BCE (Boundary-Control-Entity)
   - Clean Architecture (Robert C. Martin)
   - Layered Architecture

---

## 4. Principi SOLID e Best Practice

### 4.1 Principio di Sostituibilità di Liskov (LSP)

**Definizione**: "Gli oggetti di una superclasse devono essere sostituibili con oggetti delle sue sottoclassi senza alterare la correttezza del programma."

#### 📍 Implementazione nel Progetto

**Gerarchia User**:

```java
public abstract class User {
    private final String username;
    private String fullName;
    private String gender;

    // Template method (contratto)
    public abstract UserType getUserType();

    // Operazione comune rispettata da tutti
    public void updateProfileDetails(String newFullName, String newGender) {
        validateFullName(newFullName);
        validateGender(newGender);
        this.fullName = newFullName;
        this.gender = newGender;
    }

    // equals/hashCode basati su username (coerente in tutta la gerarchia)
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }
}
```

**Fan** (estende senza violare):
```java
public class Fan extends User {
    private String favTeam;
    private LocalDate birthday;
    private final List<Booking> bookingList;

    // ✅ Implementa contratto
    @Override
    public UserType getUserType() {
        return UserType.FAN;
    }

    // ✅ ESTENDE (non sostituisce) comportamento genitore
    public void updateFanProfile(String newFullName, String newGender,
                                  String newFavTeam, LocalDate newBirthday) {
        super.updateProfileDetails(newFullName, newGender);  // ✅ Riusa
        validateFavTeam(newFavTeam);
        validateBirthday(newBirthday);
        this.favTeam = newFavTeam;
        this.birthday = newBirthday;
    }

    // ✅ AGGIUNGE comportamenti specifici (non viola precondizioni)
    public void addBooking(Booking booking) throws BookingNotAllowedException {
        // Nuova funzionalità specifica di Fan
    }

    // ✅ equals() usa stesso criterio (username)
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Fan fan)
            return fan.getUsername().equals(this.getUsername());
        return false;
    }
}
```

**VenueManager** (estende senza violare):
```java
public class VenueManager extends User {
    private String companyName;
    private String phoneNumber;

    // ✅ Implementa contratto
    @Override
    public UserType getUserType() {
        return UserType.VENUE_MANAGER;
    }

    // ✅ ESTENDE comportamento
    public void updateManagerProfile(String newFullName, String newGender,
                                      String newCompanyName, String newPhoneNumber) {
        super.updateProfileDetails(newFullName, newGender);  // ✅ Riusa
        validateCompanyName(newCompanyName);
        validatePhoneNumber(newPhoneNumber);
        this.companyName = newCompanyName;
        this.phoneNumber = newPhoneNumber;
    }

    // ✅ AGGIUNGE comportamenti specifici
    public void confirmBooking(Booking booking, Fan fan, Venue venue) { /* ... */ }

    // ✅ equals() coerente
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VenueManager manager)
            return manager.getUsername().equals(this.getUsername());
        return false;
    }
}
```

**Test di Sostituibilità** (LoginController.java:104-108):

```java
// ✅ Polimorfismo: User può essere Fan o VenueManager
if (type.equalsIgnoreCase(String.valueOf(UserType.FAN))) {
    return factory.getFanDao().retrieveFan(credentials.getUsername());
    // ↑ Ritorna Fan (sottoclasse di User)
} else if (type.equalsIgnoreCase(String.valueOf(UserType.VENUE_MANAGER))) {
    return factory.getVenueManagerDao().retrieveVenueManager(credentials.getUsername());
    // ↑ Ritorna VenueManager (sottoclasse di User)
}

// Entrambi possono essere usati come User senza problemi:
User user = retrieveUserByType(...);  // Può essere Fan o VenueManager
storeUserSession(user);  // ✅ Funziona con entrambi
UserType type = user.getUserType();  // ✅ Late binding
```

**Verifica LSP**:
- ✅ **Precondizioni**: Le sottoclassi non rafforzano precondizioni (es. `getUserType()` non richiede più parametri)
- ✅ **Postcondizioni**: Le sottoclassi non indeboliscono postcondizioni (es. `getUserType()` ritorna sempre un valore valido)
- ✅ **Invarianti**: Username rimane chiave primaria immutabile in tutta la gerarchia
- ✅ **Metodi**: `equals()` e `hashCode()` basati su username in tutta la gerarchia
- ✅ **Comportamento**: Sottoclassi ESTENDONO senza ALTERARE comportamento base

### 4.2 Single Responsibility Principle (SRP)

**Ogni classe ha UNA sola responsabilità**:

- `LoginController`: Gestisce SOLO autenticazione
- `UserDao`: Gestisce SOLO persistenza User
- `SessionManager`: Gestisce SOLO sessione utente
- `DaoFactoryFacade`: Gestisce SOLO creazione e configurazione DAO
- `UserBean`: Trasporta SOLO dati (no logica)
- `User`: Incapsula SOLO logica dominio User

### 4.3 Open/Closed Principle (OCP)

**Aperto per estensione, chiuso per modifica**:

**Esempio**: Aggiungere nuovo tipo persistenza

```java
// 1. NON modifichi UserDaoFactory (chiuso per modifica)
// 2. Aggiungi nuovo enum:
public enum PersistenceType {
    CSV,
    MYSQL,
    MONGODB  // ✅ Nuovo tipo
}

// 3. Estendi switch:
public UserDao getUserDao(PersistenceType type) {
    return switch (type) {
        case CSV -> createUserDaoCsv();
        case MYSQL -> createUserDaoMySql();
        case MONGODB -> createUserDaoMongoDb();  // ✅ Estensione
    };
}

// 4. Crei nuova classe:
public class UserDaoMongoDb implements UserDao { /* ... */ }
```

### 4.4 Dependency Inversion Principle (DIP)

**Dipendenza da astrazioni, non da implementazioni concrete**:

```java
// ✅ CORRETTO: Dipende da interfaccia
public class LoginController {
    public UserBean login(CredentialsBean credentials) {
        UserDao userDao = facade.getUserDao();  // ✅ Interfaccia
        userDao.validateUser(credentials);
        // ...
    }
}

// ❌ SBAGLIATO: Dipendenza concreta
public class LoginController {
    public UserBean login(CredentialsBean credentials) {
        UserDaoMySql userDao = new UserDaoMySql();  // ❌ Classe concreta
        // ...
    }
}
```

---

## 5. Polimorfismo e Type System

### 5.1 Schema Completo del Polimorfismo

**4 Elementi Fondamentali (GoF)**:

#### 1. Classe Astratta/Interfaccia
```java
public abstract class User {
    // Template method (hook)
    public abstract UserType getUserType();
}
```

#### 2. Implementazioni Concrete
```java
public class Fan extends User {
    @Override
    public UserType getUserType() {
        return UserType.FAN;
    }
}

public class VenueManager extends User {
    @Override
    public UserType getUserType() {
        return UserType.VENUE_MANAGER;
    }
}
```

#### 3. Binding Dinamico (Late Binding)
```java
// LoginGraphicController.java:88
User user = loginController.login(credentials);  // Può essere Fan o VenueManager
UserType type = user.getUserType();  // ✅ Decisione a RUNTIME
```

**Funzionamento a Runtime**:
```
Compilazione:
    user.getUserType() → Chiama getUserType() di User (tipo statico)

Runtime (JVM):
    user è effettivamente un Fan
    ↓
    JVM cerca implementazione in Fan.getUserType()
    ↓
    Esegue Fan.getUserType() → return UserType.FAN
```

#### 4. Meccanismo di Scelta
```java
// LoginGraphicController.java:84-91
private void navigateToHomepage(UserBean userBean) {
    UserType userType = UserType.valueOf(userBean.getType());

    // ✅ Scelta basata su tipo
    if (userType == UserType.FAN) {
        navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/FanHomepage.fxml");
    } else if (userType == UserType.VENUE_MANAGER) {
        navigatorSingleton.gotoPage("/it/uniroma2/hoophub/fxml/VenueManagerHomepage.fxml");
    }
}
```

### 5.2 Perché NON instanceof?

**❌ Anti-pattern**:
```java
// MALE: Viola Open/Closed Principle
if (user instanceof Fan) {
    Fan fan = (Fan) user;
    // ...
} else if (user instanceof VenueManager) {
    VenueManager vm = (VenueManager) user;
    // ...
}
```

**Problemi**:
- Coupling stretto con classi concrete
- Se aggiungi nuovo tipo utente, devi modificare TUTTE le `instanceof`
- Viola OCP (aperto/chiuso)

**✅ Soluzione Polimorfica**:
```java
// BENE: Usa polimorfismo
UserType type = user.getUserType();  // Polimorfismo
switch (type) {  // Basato su enum
    case FAN -> handleFan();
    case VENUE_MANAGER -> handleVenueManager();
}
```

### 5.3 Type-Safe Enum

**UserType** (utilities/UserType.java):
```java
public enum UserType {
    FAN("FAN"),
    VENUE_MANAGER("VENUE_MANAGER");

    private final String type;

    UserType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }
}
```

**Benefici**:
- ✅ Type-safe (compile-time checking)
- ✅ Nessuna stringa magica
- ✅ IDE autocomplete
- ✅ Refactoring sicuro

---

## 6. Riepilogo Architetturale Finale

### Pattern GoF Implementati (7)

| **Pattern** | **Tipo** | **Implementazione** | **File Chiave** |
|-------------|----------|---------------------|-----------------|
| **Singleton** | Creational | LoginController, SessionManager, DaoFactoryFacade | `LoginController.java:28` |
| **Factory Method** | Creational | UserDaoFactory, FanDaoFactory, ecc. | `patterns/factory/*Factory.java` |
| **Abstract Factory** | Creational | DaoFactoryFacade | `DaoFactoryFacade.java:40` |
| **Facade** | Structural | DaoFactoryFacade | `DaoFactoryFacade.java:40` |
| **Observer** | Behavioral | ObservableDao + CrossPersistenceSyncObserver | `patterns/observer/` |
| **Builder** | Creational | User.Builder, Fan.Builder, Bean.Builder | `model/*`, `beans/*` |
| **Template Method** | Behavioral | AbstractCsvDao, AbstractMySqlDao | `dao/csv/AbstractCsvDao.java` |

### Principi SOLID Rispettati

- ✅ **SRP**: Ogni classe una responsabilità
- ✅ **OCP**: Factory estendibili senza modifiche
- ✅ **LSP**: Fan/VenueManager sostituibili con User
- ✅ **ISP**: Interfacce specifiche (UserDao, FanDao, ecc.)
- ✅ **DIP**: Dipendenza da astrazioni (UserDao, non UserDaoMySql)

### Architettura a Layer

```
┌─────────────────────────────────────────────────┐
│         BOUNDARY (Presentation)                 │
│  - GUI: LoginGraphicController                  │
│  - CLI: CliLoginGraphicController               │
│  Lavora con: Bean (solo dati)                   │
└─────────────────────────────────────────────────┘
                    ↕ Bean
┌─────────────────────────────────────────────────┐
│         CONTROLLER (Application)                │
│  - LoginController (Singleton)                  │
│  Converte: Bean ↔ Model                         │
│  Gestisce: Business logic                       │
└─────────────────────────────────────────────────┘
                    ↕ Model
┌─────────────────────────────────────────────────┐
│         DAO (Persistence)                       │
│  - Factory: DaoFactoryFacade (Abstract Factory) │
│  - DAO: UserDao, FanDao, VenueManagerDao        │
│  - Impl: CSV, MySQL (Factory Method)            │
│  - Sync: Observer Pattern                       │
└─────────────────────────────────────────────────┘
                    ↕
┌─────────────────────────────────────────────────┐
│         DATABASE                                │
│  - CSV Files                                    │
│  - MySQL Database                               │
└─────────────────────────────────────────────────┘
```

---

## 7. Riferimenti Bibliografici

### Libri

1. **Design Patterns: Elements of Reusable Object-Oriented Software**
   - Gamma, Helm, Johnson, Vlissides (Gang of Four)
   - Addison-Wesley, 1994
   - Tutti i pattern GoF

2. **Effective Java (3rd Edition)**
   - Joshua Bloch
   - Addison-Wesley, 2018
   - Enum Singleton, Builder pattern

3. **Clean Architecture**
   - Robert C. Martin
   - Prentice Hall, 2017
   - Layer separation, Dependency Inversion

4. **Applying UML and Patterns**
   - Craig Larman
   - Prentice Hall, 2004
   - BCE (Boundary-Control-Entity) pattern

### Standard Java

- **Java Beans Specification (JSR 57)**
  - Oracle
  - Bean pattern conventions

---

**Fine Documentazione** 📚
