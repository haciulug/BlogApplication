# BlogApplication

This is the BlogApplication for managing and viewing blog posts. It is built with Spring Boot and uses H2 as the database for development environments.

## Prerequisites

Before you begin, ensure you have met the following requirements:
- Java 17 or higher
- Maven 3.6 or higher
- Docker

## Setting Up for Development

1. **Clone the repository:**

   ```bash
   git clone https://github.com/haciulug/BlogApplication.git
   cd BlogApplication
   ```
   
2. **Build the project:**

   ```bash
    mvn clean install
    ```
   
3. **Run the project:** 

   ```bash
   docker run -p 8080:8080 -t -e "SPRING_PROFILES_ACTIVE=h2" -e "APP_PORT=8080"  haciulug/blog-application:0.0.1-SNAPSHOT 
   ```
