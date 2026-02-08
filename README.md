# Sports Card Project

A full-stack web application for managing and cataloging a sports trading card collection. Built with Spring Boot and PostgreSQL, this application allows collectors to track their inventory, manage transactions, monitor market prices through web scraping, and receive email notifications for new listings.

## Features

### Card Management
- Add, edit, and delete cards with detailed attributes (year, publisher, set, player, grade, value, etc.)
- Search and filter cards by multiple criteria
- View cards with pagination and sorting
- Track autographs, inserts, parallels, and numbered cards

### Transaction Tracking
- Record purchases and sales
- Link multiple cards to transactions
- View transaction history with date filtering
- Track profit/loss on card sales

### Yahoo Auction Crawler
- Automated web scraping of Yahoo Auctions Taiwan for sports cards
- Search by configurable keywords
- Extract product details including title, image, price, and auction status
- Email notifications for new listings

### YouTube Integration
- Search YouTube for card-related videos
- Query specific channels for content

### Utilities
- Name randomizer for giveaways and selections

## Technology Stack

- **Backend:** Java 17, Spring Boot 3.1.3
- **Database:** PostgreSQL
- **Template Engine:** Thymeleaf
- **Web Scraping:** Selenium WebDriver 4.8.1
- **Build Tool:** Maven
- **API Documentation:** SpringDoc OpenAPI (Swagger UI)
- **Email:** Spring Mail (Gmail SMTP)

## Project Structure

```
SportsCardProject/
├── src/main/java/RGcards/SportsCardProject/
│   ├── bot/           # Web scraping bots (Yahoo Auctions, Baseball Reference)
│   ├── config/        # Spring configuration (async, etc.)
│   ├── controller/    # REST and web controllers
│   ├── dao/           # Data access objects and repositories
│   ├── dto/           # Data transfer objects
│   ├── entity/        # JPA entities (Card, Transaction, etc.)
│   ├── service/       # Business logic layer
│   └── util/          # Utility classes
├── src/main/resources/
│   ├── templates/     # Thymeleaf HTML templates
│   ├── static/        # CSS, JS, and images
│   └── application.properties.example
└── pom.xml
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL database
- Gmail account (for email notifications)
- YouTube Data API key (for YouTube integration)

## Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd SportsCardProject
```

### 2. Create PostgreSQL Database

```sql
CREATE DATABASE sportsCards;
CREATE USER RGcards WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE sportsCards TO RGcards;
```

### 3. Configure Application Properties

Copy the example configuration file and update with your credentials:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edit `application.properties` with your settings:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/sportsCards
spring.datasource.username=RGcards
spring.datasource.password=<YOUR_DATABASE_PASSWORD>

# JPA Settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Email Configuration (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<YOUR_EMAIL@gmail.com>
spring.mail.password=<YOUR_APP_PASSWORD>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.mail.to=<RECIPIENT_EMAIL@gmail.com>

# YouTube API
youtube.api.key=<YOUR_YOUTUBE_API_KEY>

# Thymeleaf (development settings)
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=file:src/main/resources/templates/
```

### 4. Build and Run

Using Maven Wrapper:

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

Or build a JAR:

```bash
./mvnw clean package
java -jar target/SportsCardProject-0.0.1-SNAPSHOT.jar
```

## Usage

Once running, access the application at `http://localhost:8080`

### Main Endpoints

| Path | Description |
|------|-------------|
| `/` | Home page |
| `/card` | Card management dashboard |
| `/card/all` | View all cards |
| `/card/add` | Add new card |
| `/card/search` | Search cards |
| `/transaction/all` | View all transactions |
| `/crawler/keywords` | Manage search keywords for web scraping |

### API Documentation

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`

### REST API Endpoints

- `GET /api/card/{id}` - Get card by ID
- `GET /api/card/search` - Search cards
- `DELETE /api/card/{id}` - Delete card

## External Services

### Yahoo Auctions Crawler
The application includes a Selenium-based web scraper for Yahoo Auctions Taiwan. It requires Chrome browser to be installed. The WebDriver is managed automatically.

### Gmail SMTP
For email notifications, you need to:
1. Enable 2-factor authentication on your Gmail account
2. Generate an App Password for the application
3. Use the App Password in `application.properties`

### YouTube Data API
To use YouTube features:
1. Create a project in Google Cloud Console
2. Enable YouTube Data API v3
3. Create an API key
4. Add the key to `application.properties`

## Development

Hot reload is enabled via Spring DevTools. Changes to templates and code will be automatically reloaded during development.

## License

This project is for personal use.
