# GUIDA AI VOPC - HoopHub BookGameSeat

## 📋 File Creati

1. **VOPC_BookGameSeat_SEMPLIFICATO.puml** - Versione senza DAO e Bean
2. **VOPC_BookGameSeat_COMPLETO.puml** - Versione con architettura completa

---

## 🎯 Quando Usare Quale Versione?

### VOPC Semplificato
**Usa per:**
- Prime fasi di design
- Discussioni con il team sul dominio
- Presentazioni semplificate
- Focus su business logic (Entity e Controller)

**NON conforme al codice reale!**

### VOPC Completo
**Usa per:**
- Documentazione finale del progetto
- Esame ISPW (se richiesto VOPC completo)
- Implementazione effettiva
- Review architetturale

**Conforme al 100% con il codice HoopHub!**

---

## 🔄 Differenze Principali

### 1. Layer DAO

**Semplificato**: ❌ Assente
```
Controller --> Entity (accesso diretto)
```

**Completo**: ✅ Presente
```
Controller --> DaoFactoryFacade --> DAO --> Entity
```

### 2. Layer Bean

**Semplificato**: ❌ Assente
```
Boundary visualizza Entity direttamente
```

**Completo**: ✅ Presente
```
Boundary visualizza Bean
Controller converte Model → Bean
```

### 3. Gestione Notifiche

**Entrambe le versioni** usano `NotificationController` separato per evitare dipendenze circolari.

---

## ✅ Bean Esistenti nel Codice

| Bean | Stato | Path |
|------|-------|------|
| VenueBean | ✅ Esiste | `/beans/VenueBean.java` |
| BookingBean | ✅ Esiste | `/beans/BookingBean.java` |
| FanBean | ✅ Esiste | `/beans/FanBean.java` |
| VenueManagerBean | ✅ Esiste | `/beans/VenueManagerBean.java` |
| NotificationBean | ❌ Da creare | - |
| NbaGameBean | ❌ Da creare | - |

---

## 🚀 Bean da Creare

### NotificationBean
```java
package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.model.UserType;
import it.uniroma2.hoophub.model.NotificationType;
import java.time.LocalDateTime;

public class NotificationBean {
    private Long id;
    private Long userId;
    private UserType userType;
    private NotificationType type;
    private String message;
    private Long relatedBookingId;
    private boolean isRead;
    private LocalDateTime createdAt;

    // Builder pattern
    private NotificationBean(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.userType = builder.userType;
        this.type = builder.type;
        this.message = builder.message;
        this.relatedBookingId = builder.relatedBookingId;
        this.isRead = builder.isRead;
        this.createdAt = builder.createdAt;
    }

    public static class Builder {
        // ... builder implementation
    }

    // Getters e Setters
}
```

### NbaGameBean
```java
package it.uniroma2.hoophub.beans;

import java.time.LocalDate;
import java.time.LocalTime;

public class NbaGameBean {
    private String homeTeam;
    private String awayTeam;
    private LocalDate gameDate;
    private LocalTime gameTime;
    private String venue;
    private String city;

    // Builder pattern
    private NbaGameBean(Builder builder) {
        this.homeTeam = builder.homeTeam;
        this.awayTeam = builder.awayTeam;
        this.gameDate = builder.gameDate;
        this.gameTime = builder.gameTime;
        this.venue = builder.venue;
        this.city = builder.city;
    }

    public static class Builder {
        // ... builder implementation
    }

    // Getters e Setters
}
```

**NOTA**: NbaGame **NON** ha un Model corrispondente perché i dati vengono recuperati dall'API NBA e non persistiti nel database.

---

## 🏗️ Architettura Completa - Flusso Dati

```
┌─────────────┐
│  Boundary   │ ◄─── Visualizza SOLO Bean
└──────┬──────┘
       │ chiama Controller con Bean (input)
       ▼
┌─────────────┐
│ Controller  │ ◄─── Logica applicativa
└──────┬──────┘
       │ usa
       ▼
┌─────────────┐
│DaoFactoryFac│ ◄─── Singleton, gestisce creazione DAO
└──────┬──────┘
       │ crea
       ▼
┌─────────────┐
│    DAO      │ ◄─── Persistenza (MySQL/CSV)
└──────┬──────┘
       │ accede
       ▼
┌─────────────┐
│   Entity    │ ◄─── Logica di dominio
│  (Model)    │
└─────────────┘
       │
       │ Controller converte Model → Bean
       ▼
┌─────────────┐
│    Bean     │ ◄─── Torna alla Boundary
└─────────────┘
```

---

## 📊 Pattern Implementati

### 1. Singleton
- Tutti i Controller (`getInstance()`)
- `DaoFactoryFacade`
- `SessionManager` (enum singleton)

### 2. Facade
- `DaoFactoryFacade` unifica accesso a tutti i DAO

### 3. Factory
- `FanDaoFactory`, `VenueDaoFactory`, `BookingDaoFactory`, etc.

### 4. Strategy
- `UserDaoCsv` vs `UserDaoMySql` (stesso interface, diverse implementazioni)

### 5. Observer
- Cross-persistence sync (MySQL ↔ CSV)

### 6. Builder
- Tutti i Model (`new Fan.Builder()...`)
- Tutti i Bean (`new VenueBean.Builder()...`)

### 7. DTO/Bean Pattern
- Separazione Boundary ↔ Controller via Bean

---

## ⚠️ Scelta NotificationController

Nel VOPC ho usato un `NotificationController` **separato** invece di far chiamare i controller tra loro.

### ❌ Versione Originale (con problemi)
```
BookGameSeatController → ManageVenuesController.notifyVenueManager()
ManageVenuesController → ManageSeatsController.notifyFan()
```

**Problemi**:
- Dipendenze circolari
- Viola Single Responsibility
- Diverso dal tuo stile attuale (LoginController e SignUpController sono indipendenti)

### ✅ Versione Corretta (nel VOPC)
```
BookGameSeatController → NotificationController.notifyVenueManager()
ManageVenuesController → NotificationController.notifyFan()
ManageSeatsController (indipendente)
```

**Vantaggi**:
- Nessuna dipendenza circolare
- Ogni controller ha una responsabilità
- Conforme al tuo stile di programmazione
- Facile da testare

---

## 🎓 Per l'Esame ISPW

### Domande Probabili

**Q: Perché BookGameSeatNBASchedule è una Boundary?**
> È la boundary tra il sistema e l'attore esterno "NBA API". Come insegnato, ogni attore (umano o sistema) ha una boundary dedicata.

**Q: Perché usi i Bean invece di passare direttamente i Model?**
> Per separare le responsabilità: la Boundary (UI) non deve conoscere la logica di business dei Model. I Bean sono DTO (Data Transfer Objects) che contengono solo dati, senza comportamento.

**Q: Perché usi DaoFactoryFacade invece di istanziare i DAO direttamente?**
> Pattern Facade: semplifica l'accesso ai DAO nascondendo la complessità delle Factory. Inoltre gestisce automaticamente la configurazione degli Observer per la sincronizzazione MySQL↔CSV.

**Q: Quali sono le differenze tra Aggregazione e Composizione?**
> - **Composizione (◆)**: Fan *-- Booking - Lifecycle dipendente. Se elimino Fan, elimino le Booking.
> - **Aggregazione (◇)**: VenueManager o-- Venue - Lifecycle indipendente. Venue può esistere senza VenueManager.

**Q: Come gestisci le transazioni tra Entity e persistenza?**
> I Controller coordinano le operazioni:
> 1. Recuperano le Entity dai DAO
> 2. Chiamano i metodi di business sulle Entity (es. `fan.addBooking()`)
> 3. Usano i DAO per persistere le modifiche
> 4. Tutto è sincronizzato MySQL↔CSV tramite Observer pattern

---

## 📝 Checklist Implementazione

Quando implementi il caso d'uso "BookGameSeat":

### Fase 1: Bean
- [x] VenueBean (esiste)
- [x] BookingBean (esiste)
- [ ] NotificationBean (da creare)
- [ ] NbaGameBean (da creare)

### Fase 2: DAO
- [ ] NotificationDao interface
- [ ] NotificationDaoCsv
- [ ] NotificationDaoMySql
- [ ] NotificationDaoFactory
- [ ] Aggiornare DaoFactoryFacade

### Fase 3: Controller
- [ ] BookGameSeatController
- [ ] NotificationController
- [ ] Aggiornare ManageVenuesController
- [ ] Aggiornare ManageSeatsController

### Fase 4: Boundary
- [ ] CliFanBookGameSeatGraphicController
- [ ] CliVenueManagerManageVenuesGraphicController
- [ ] BookGameSeatNBASchedule (servizio API NBA)

### Fase 5: Model
- [ ] Verificare che Booking, Venue, Fan abbiano tutti i metodi necessari
- [ ] Aggiungere eventuali metodi mancanti

---

## 🔗 File da Visualizzare

### Visualizza VOPC Semplificato
1. Apri `/home/user/HoopHub/VOPC_BookGameSeat_SEMPLIFICATO.puml`
2. Usa un viewer PlantUML (es. PlantUML extension in VS Code)

### Visualizza VOPC Completo
1. Apri `/home/user/HoopHub/VOPC_BookGameSeat_COMPLETO.puml`
2. Usa un viewer PlantUML

### Online
Puoi anche copiare il contenuto su: https://www.plantuml.com/plantuml/uml/

---

## 📞 Prossimi Passi

1. **Rivedi il VOPC Completo** e verifica che sia conforme alle tue aspettative
2. **Decidi quale NotificationController** preferisci (separato o integrato)
3. **Crea NotificationBean e NbaGameBean** seguendo il pattern degli altri Bean
4. **Implementa i Controller** partendo da BookGameSeatController
5. **Testa** l'integrazione con il codice esistente

---

## ❓ Domande?

Se hai dubbi su:
- Relazioni tra classi (composizione vs aggregazione)
- Pattern implementati
- Flusso dei dati
- Scelte architetturali

Fammi sapere e ti aiuto! 🚀
