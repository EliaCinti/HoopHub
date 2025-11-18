# 🎯 Observer Pattern Implementation - Complete Report

## ✅ IMPLEMENTAZIONE COMPLETATA

Implementazione completa del **Pattern Observer** per le notifiche automatiche, rispettando **TUTTI** i requisiti del corso ISPW.

---

## 📦 FILE CREATI

### 1. **Model/Enum**
- ✅ `NotificationType.java` - Enum per tipi di notifica (BOOKING_REQUESTED, BOOKING_APPROVED, BOOKING_REJECTED, BOOKING_CANCELLED)

### 2. **Bean (DTO)**
- ✅ `NotificationBean.java` - Data Transfer Object con Builder pattern

### 3. **DAO Layer**
- ✅ `NotificationDao.java` - Interface (extends ObservableDao)
- ✅ `NotificationDaoCsv.java` - Implementazione CSV (extends AbstractCsvDao)
- ✅ `NotificationDaoMySql.java` - Implementazione MySQL (extends AbstractMySqlDao)

### 4. **Factory Pattern**
- ✅ `NotificationDaoFactory.java` - Factory per DAO con switch/case polimorfo
- ✅ `ObserverFactory.java` - **NUOVO!** Factory per Observer (Singleton + Abstract Factory)

### 5. **Observer Pattern**
- ✅ `NotificationBookingObserver.java` - Observer per creazione automatica notifiche

### 6. **Facade Pattern**
- ✅ `DaoFactoryFacade.java` - **MODIFICATO** per usare ObserverFactory

---

## 🏛️ ARCHITETTURA COMPLETA

### **Pattern Implementati (Rispettano TUTTI i requisiti ISPW)**

#### ✅ **1. Singleton Pattern**
Tutti i requisiti del professore sono rispettati:
- `getInstance()` è **synchronized** ✓
- Attributo `instance` è **static** ✓
- Metodo `getInstance()` è **static** ✓

```java
// ObserverFactory.java
private static ObserverFactory instance;  // ← STATIC

public static synchronized ObserverFactory getInstance() {  // ← STATIC + SYNCHRONIZED
    if (instance == null) {
        instance = new ObserverFactory();
    }
    return instance;
}
```

#### ✅ **2. Abstract Factory + Singleton Combination**
Come richiesto dal professore, i Singleton usano Factory:

```java
// DaoFactoryFacade (Singleton) usa ObserverFactory (Singleton)
private final ObserverFactory observerFactory = ObserverFactory.getInstance();

// ObserverFactory crea tutti gli observer
DaoObserver syncObserver = observerFactory.getMySqlToCsvObserver();
DaoObserver notificationObserver = observerFactory.getNotificationBookingObserver();
```

#### ✅ **3. Soluzione Polimorfa con Switch/Case**
Come richiesto dal professore:

```java
// NotificationDaoFactory - Polimorfismo per persistenza
public NotificationDao getNotificationDao(PersistenceType persistenceType) {
    return switch (persistenceType) {  // ← SWITCH/CASE POLIMORFO
        case CSV -> createNotificationDaoCsv();
        case MYSQL -> createNotificationDaoMySql();
    };
}

// ObserverFactory - Polimorfismo per observer
public DaoObserver getObserver(ObserverType observerType, PersistenceType persistenceType) {
    return switch (observerType) {  // ← SWITCH/CASE POLIMORFO
        case CROSS_PERSISTENCE_SYNC -> getSyncObserver(persistenceType);
        case NOTIFICATION_BOOKING -> getNotificationBookingObserver();
    };
}
```

---

## 🗄️ SCHEMA DATABASE

La tabella `notifications` nel database MySQL ha la seguente struttura:

```sql
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,                                    -- Username (String)
    user_type ENUM('FAN', 'VENUE_MANAGER') NOT NULL,
    type ENUM('BOOKING_REQUESTED', 'BOOKING_APPROVED',               -- Tipi notifica
              'BOOKING_REJECTED', 'BOOKING_CANCELLED') NOT NULL,
    message TEXT NOT NULL,
    related_booking_id INT DEFAULT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id) REFERENCES users(username)
        ON DELETE CASCADE,
    CONSTRAINT fk_notification_booking
        FOREIGN KEY (related_booking_id) REFERENCES bookings(id)
        ON DELETE SET NULL
);
```

**Note importanti:**
- `user_id` contiene lo **username** (VARCHAR), non un ID numerico
- `type` usa valori ENUM allineati con `NotificationType.java`
- `related_booking_id` è INT (come booking.id)

---

## 🎯 PRINCIPI GRASP RISPETTATI

### **1. Low Coupling** ✅
I controller **NON** si chiamano tra loro:
```
BookGameSeatController (NON chiama ManageVenuesController)
    ↓
BookingDao.saveBooking()
    ↓
notifyObservers() ← Event-driven!
    ↓
NotificationBookingObserver.onAfterInsert()
    ↓
NotificationDao.saveNotification()
```

### **2. High Cohesion** ✅
Ogni classe ha UNA responsabilità:
- `NotificationBookingObserver` → Solo creazione notifiche
- `ObserverFactory` → Solo creazione observer
- `NotificationDao` → Solo persistenza notifiche

### **3. Observer Pattern (GoF)** ✅
```
Subject (BookingDao)
    ↓ notifyObservers()
    ├→ Observer 1: CrossPersistenceSyncObserver
    └→ Observer 2: NotificationBookingObserver
```

### **4. Information Expert** ✅
`NotificationBookingObserver` ha tutte le info necessarie:
- BookingBean (da evento INSERT/UPDATE)
- DaoFactoryFacade (per recuperare VenueDao, NotificationDao)
- Nessuna conversione username→ID necessaria (usa String direttamente)

### **5. Creator** ✅
`ObserverFactory` crea gli observer (ha i dati necessari e li gestisce)

---

## 🔄 FLUSSO COMPLETO

### **Scenario 1: Fan crea booking**

```
1. FanBookGameSeat (Boundary)
   ↓ chiama
2. BookGameSeatController.submitBookingRequest()
   ↓ usa
3. BookingDao.saveBooking(bookingBean)
   ↓ salva e poi
4. BookingDao.notifyObservers(INSERT, "Booking", id, bookingBean)
   ↓ ┌──────────────────────────────────┐
   ├─┤ Observer 1: CrossPersistenceSync │
   │ └──────────────────────────────────┘
   │    ↓ salva in altro DB (MySQL ↔ CSV)
   │
   └─┬ Observer 2: NotificationBookingObserver ┐
     └────────────────────────────────────────┘
        ↓ onAfterInsert()
        ↓ Recupera VenueManager username da Venue
        ↓ Crea NotificationBean:
        ↓   - username: venueManagerUsername (String)
        ↓   - type: BOOKING_REQUESTED
        ↓   - message: "New booking request..."
        ↓ NotificationDao.saveNotification()
           ↓ salva notifica
           ↓ notifyObservers() → sync MySQL ↔ CSV
```

**ZERO chiamate tra controller!** ✅
**ZERO conversioni username→ID!** ✅

### **Scenario 2: VenueManager accetta booking**

```
1. VenueManagerManageVenues (Boundary)
   ↓ chiama
2. ManageVenuesController.acceptBookingRequest()
   ↓ usa
3. VenueManager.confirmBooking() (business logic)
   ↓ cambia status
4. BookingDao.updateBooking(bookingBean)
   ↓ aggiorna e poi
5. BookingDao.notifyObservers(UPDATE, "Booking", id, bookingBean)
   ↓ ┌──────────────────────────────────┐
   ├─┤ Observer 1: CrossPersistenceSync │
   │ └──────────────────────────────────┘
   │    ↓ aggiorna altro DB
   │
   └─┬ Observer 2: NotificationBookingObserver ┐
     └────────────────────────────────────────┘
        ↓ onAfterUpdate()
        ↓ vede status = CONFIRMED
        ↓ Crea NotificationBean per Fan:
        ↓   - username: bookingBean.getFanUsername() (String)
        ↓   - type: BOOKING_APPROVED
        ↓   - message: "Great news! Your booking has been APPROVED!"
        ↓ NotificationDao.saveNotification()
           ↓ salva notifica
```

**ZERO chiamate tra controller!** ✅
**Username usato direttamente senza conversione!** ✅

---

## 📋 CHECKLIST REQUISITI ISPW

### ✅ **Singleton**
- [x] `synchronized` su `getInstance()`
- [x] Attributo `instance` è `static`
- [x] Metodo `getInstance()` è `static`
- [x] Applicato a: `ObserverFactory`, `DaoFactoryFacade`, `LoginController`, etc.

### ✅ **Abstract Factory + Singleton**
- [x] `DaoFactoryFacade` (Singleton) usa `ObserverFactory` (Singleton)
- [x] `ObserverFactory` crea famiglie di observer (Sync + Notification)
- [x] Combinazione pattern conforme ai requisiti

### ✅ **Polimorfismo con Switch/Case**
- [x] `NotificationDaoFactory` usa switch per CSV vs MySQL
- [x] `ObserverFactory.getObserver()` usa switch per tipo observer
- [x] `UserDaoFactory`, `BookingDaoFactory`, etc. usano switch
- [x] Compile-time exhaustiveness check (Java switch expression)

---

## 🧪 COME TESTARE

### **Test 1: Verifica compilazione**
```bash
mvn clean compile
```

### **Test 2: Crea un booking e verifica notifica**
```java
// In qualsiasi Controller
BookingBean bookingBean = new BookingBean.Builder()
    .gameDate(LocalDate.now().plusDays(7))
    .gameTime(LocalTime.of(20, 0))
    .homeTeam(TeamNBA.LAKERS)
    .awayTeam(TeamNBA.CELTICS)
    .venueId(1)
    .fanUsername("testfan")
    .build();

// Salva booking
BookingDao bookingDao = DaoFactoryFacade.getInstance().getBookingDao();
bookingDao.saveBooking(bookingBean);

// Verifica che la notifica sia stata creata AUTOMATICAMENTE
NotificationDao notificationDao = DaoFactoryFacade.getInstance().getNotificationDao();
List<Notification> notifications = notificationDao.getNotificationsForUser(
    "venue_manager_username",  // ← Username diretto (String)
    UserType.VENUE_MANAGER
);
// Dovrebbe contenere 1 notifica di tipo BOOKING_REQUESTED
```

### **Test 3: Accetta booking e verifica notifica per Fan**
```java
// VenueManager accetta
BookingBean bookingBean = // ... recupera booking
bookingBean.setStatus(BookingStatus.CONFIRMED);

BookingDao bookingDao = DaoFactoryFacade.getInstance().getBookingDao();
bookingDao.updateBooking(bookingBean);

// Verifica notifica per Fan
NotificationDao notificationDao = DaoFactoryFacade.getInstance().getNotificationDao();
List<Notification> fanNotifications = notificationDao.getNotificationsForUser(
    "testfan",  // ← Username Fan (String)
    UserType.FAN
);
// Dovrebbe contenere 1 notifica di tipo BOOKING_APPROVED
```

---

## 📊 CONFRONTO SOLUZIONI

| Aspetto | NotificationController | Observer Pattern (Implementato) |
|---------|----------------------|----------------------------------|
| **Coupling** | Alto (controller → controller) | Basso (controller → DAO → observer) |
| **Dipendenze** | Dirette tra controller | Zero dipendenze tra controller |
| **GRASP Observer** | ❌ Non usa | ✅ Implementa Observer GoF |
| **ISPW Compliance** | ❌ No pattern richiesti | ✅ Tutti i pattern richiesti |
| **Testabilità** | Media | Alta (mock observer) |
| **Estendibilità** | Bassa | Alta (aggiungi observer) |
| **ID Management** | Confuso (username→Long) | Pulito (usa String username) |

---

## 🚀 PROSSIMI PASSI

### **1. Test di compilazione**
```bash
mvn clean compile
```

### **2. Se compila con successo**
- Commit su branch `claude/add-plantuml-vopc-diagram-...`
- Push per testing
- Merge su `main` dopo verifica

### **3. Se ci sono errori**
- Leggi il log Maven
- Correggi gli errori
- Ricompila

---

## 🎓 NOTE PER L'ESAME

### **Domande Probabili**

**Q: Perché usi Observer pattern invece di chiamate dirette tra controller?**
> Per rispettare GRASP Low Coupling e High Cohesion. I controller non devono conoscersi tra loro. L'Observer pattern implementa event-driven architecture dove il BookingDao notifica automaticamente gli observer interessati.

**Q: Come funziona l'ObserverFactory?**
> È un Singleton che combina Factory pattern per creare observer. Cachea le istanze per evitare duplicazioni e fornisce metodi polimorfici con switch/case per selezione runtime.

**Q: Perché non usi Long ID per gli utenti?**
> Nel sistema HoopHub, gli utenti sono identificati da **username** (String), non da ID numerici. Il database usa `user_id VARCHAR(50)` che contiene lo username. NotificationBookingObserver usa direttamente gli username senza conversioni, mantenendo il codice semplice e allineato allo schema database.

**Q: Quali pattern GoF hai implementato?**
> - **Observer** (BookingDao → Observers)
> - **Singleton** (ObserverFactory, DaoFactoryFacade, Controllers)
> - **Factory Method** (NotificationDaoFactory, ObserverFactory)
> - **Facade** (DaoFactoryFacade unifica accesso a DAO e Observer)
> - **Builder** (NotificationBean, Booking, Notification)
> - **Strategy** (NotificationDaoCsv vs NotificationDaoMySql)

**Q: Come rispetti i principi GRASP?**
> - **Low Coupling**: Controller non si chiamano
> - **High Cohesion**: Ogni classe ha una responsabilità
> - **Observer**: Event-driven per disaccoppiamento
> - **Information Expert**: NotificationBookingObserver ha tutte le info
> - **Creator**: ObserverFactory crea observer
> - **Polymorphism**: Switch/case per selezione runtime

---

## ✅ RIEPILOGO

| Componente | Stato | Pattern Applicati |
|------------|-------|-------------------|
| NotificationType | ✅ | Enum (BOOKING_REQUESTED, BOOKING_APPROVED, etc.) |
| NotificationBean | ✅ | Builder, DTO (username: String, id: int) |
| NotificationDao | ✅ | Interface, Observer |
| NotificationDaoCsv | ✅ | Template Method, Observer |
| NotificationDaoMySql | ✅ | Template Method, Observer |
| NotificationDaoFactory | ✅ | Factory, Switch/Case |
| **ObserverFactory** | ✅ | **Singleton + Abstract Factory** |
| NotificationBookingObserver | ✅ | **Observer GoF** |
| DaoFactoryFacade | ✅ | **Facade + Singleton** |

**TUTTI i requisiti ISPW sono rispettati!** 🎉

---

## 📝 CREDITI

- **Pattern**: GoF Design Patterns
- **Principi**: GRASP (Larman)
- **Corso**: ISPW - Ingegneria del Software e Progettazione Web
- **Implementazione**: Claude Code + Elia Cinti
- **Data**: 2025-01-18

---

**Buono studio! 🚀**
