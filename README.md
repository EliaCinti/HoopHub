# 🏀 HoopHub
> *Connecting NBA fans with the perfect game-watching experience*

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-007396?style=for-the-badge&logo=java&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)

## 📖 About The Project

HoopHub is a reservation management system that bridges the gap between NBA fans and sports bars, creating a seamless booking experience for watching games together. Born from the passion for basketball and social viewing experiences, HoopHub makes it easy to find the perfect venue for your next game night.

### 🎯 Core Mission
- **Connect** passionate NBA fans with local sports venues
- **Simplify** the booking process for game nights
- **Enhance** the social aspect of watching basketball
- **Support** local businesses through increased visibility

## ✨ Features

### For Fans 🏀
- **Team-Centric Experience**: Register your favorite NBA team and see personalized game schedules
- **Smart Venue Search**: Filter venues by distance, accessibility, and price
- **Easy Booking**: Send reservation requests with just a few clicks
- **QR Check-in**: Receive a digital QR code for seamless venue check-in
- **Review System**: Share your experience and help other fans find great venues
- **Loyalty Program**: Earn points and level up (Bronze → Silver → Gold) by leaving reviews

### For Venue Managers 📊
- **Self-Registration**: Autonomously register and manage your venue on the platform
- **Booking Management**: Accept or decline reservation requests based on availability
- **Real-time Notifications**: Get instant alerts for new booking requests
- **Visibility Boost**: Reach passionate NBA fans actively looking for viewing venues

### For Everyone 🌟
- **Public Game Calendar**: View upcoming NBA games without registration
- **Top Venues**: Browse highest-rated venues and their reviews
- **Dual Interface**: Choose between GUI (JavaFX) or CLI based on preference

## 🛠️ Tech Stack

### Core Technologies
- **Language**: Java 17+
- **GUI Framework**: JavaFX with FXML
- **Styling**: CSS3
- **Database**: MySQL 8.0
- **Alternative Storage**: CSV File System
- **Architecture**: Model-View-Controller (MVC)

### Design Patterns Implementation
| Pattern | Purpose | Implementation |
|---------|---------|----------------|
| **Factory + Facade** | DAO Management | Seamless switching between MySQL and CSV persistence |
| **Singleton** | Database Connection | Single, efficient database connection management |
| **Observer** | Data Synchronization | Real-time sync between different persistence layers |
| **Builder** | Object Creation | Clean construction of beans and models |
| **Strategy** | Venue Ranking | Flexible, weighted ranking algorithm |
| **Decorator** | Search Filters | Dynamic combination of search criteria |

## 📁 Project Structure

```
HoopHub/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── controller/      # MVC Controllers
│   │   │   ├── model/           # Business Logic & Entities
│   │   │   ├── view/            # JavaFX Views
│   │   │   ├── dao/             # Data Access Objects
│   │   │   ├── bean/            # Data Transfer Objects
│   │   │   ├── pattern/         # Design Pattern Implementations
│   │   │   └── utils/           # Utilities & Helpers
│   │   │
│   │   └── resources/
│   │       ├── fxml/            # FXML Layout Files
│   │       ├── css/             # Stylesheets
│   │       ├── images/          # Icons & Assets
│   │       └── config/          # Configuration Files
│   │
│   └── test/                    # Unit & Integration Tests
│
├── database/
│   ├── schema.sql               # MySQL Schema
│   └── sample_data/             # CSV Sample Files
│
├── docs/                        # Documentation
│   ├── UML/                     # UML Diagrams
│   └── API/                     # API Documentation
│
└── README.md

```

## 🚀 Getting Started

### Prerequisites
- Java JDK 17 or higher
- MySQL 8.0+
- Maven 3.8+
- JavaFX SDK (if not bundled)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/HoopHub.git
   cd HoopHub
   ```

2. **Configure Database**
   ```bash
   mysql -u root -p < database/schema.sql
   ```

3. **Update configuration**
   - Copy `src/main/resources/config/database.properties.example` to `database.properties`
   - Update with your MySQL credentials

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run the application**
   
   **GUI Version:**
   ```bash
   mvn javafx:run
   ```
   
   **CLI Version:**
   ```bash
   java -jar target/hoophub-cli.jar
   ```

## 📸 Screenshots

<details>
<summary>Click to view screenshots</summary>

### GUI Interface
![Homepage](docs/screenshots/homepage-placeholder.png)
*Homepage with personalized game schedule*

![Venue Search](docs/screenshots/search-placeholder.png)
*Smart venue search with filters*

### CLI Interface
![CLI Menu](docs/screenshots/cli-placeholder.png)
*Command-line interface for power users*

</details>

## 🤝 Contributing

This is a university project for the ISPW course (Software Engineering and Web Programming). While it's primarily an individual project, suggestions and feedback are welcome!

### Development Guidelines
- Follow Java naming conventions
- Write meaningful commit messages
- Document all public methods
- Maintain test coverage above 70%
- Update UML diagrams when adding new classes

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **ISPW Course** - Tor Vergata University of Rome
- **NBA API** - For providing game schedules (via MockAPI)
- **JavaFX Community** - For excellent documentation and support
- **Design Patterns: Elements of Reusable Object-Oriented Software** - Gang of Four

## 📬 Contact

**Project Creator**: [Elia Cinti]
**University**: Tor Vergata - Roma  
**Course**: ISPW (Ingegneria del Software e Progettazione Web)  
**Academic Year**: 2025/2026

---

<p align="center">
  Made with ❤️ and ☕ for ISPW Course
  <br>
  <i>Because watching NBA games is better together!</i>
</p>
