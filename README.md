# BlogApplication

This is the BlogApplication for managing and viewing blog posts. It is built with Spring Boot. The application can be run with either an in-memory H2 database for development or MySQL for production environments using Docker.
## Prerequisites

Before you begin, ensure you have met the following requirements:
- Java 17 or higher
- Maven 3.6 or higher
- Docker
- Docker Compose

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
   
3. **Run the project using Docker (with H2 Database):** 

This will run the application with an in-memory H2 database for development purposes.


   ```bash
   docker run -p 8080:8080 -t -e "SPRING_PROFILES_ACTIVE=test" -e "APP_PORT=8080"  haciulug/blog-application:0.0.1-SNAPSHOT 
   ```

## Running with Docker Compose (MySQL in Production)
To run the application in a production-like environment using MySQL, use Docker Compose:

1. **Start the application using Docker Compose:**

This command will start both the MySQL database and the BlogApplication container. The application will connect to the MySQL database with the defined credentials.

    ```bash
    docker-compose up -d
    ```

## Environment Variables

- `SPRING_PROFILES_ACTIVE`: The active Spring profile. Set to `test` for the in-memory H2 database or `prod` for production with MySQL.
- `APP_PORT`: The port on which the application will run. Default is `8080`.
- JWT Configuration(Optional):
  - `JWT_SECRET`: The secret key used to sign the JWT token.
  - `JWT_EXPIRATION`: The expiration time of the JWT token in milliseconds.
  - `JWT_REFRESH_EXPIRATION`: The expiration time of the refresh token in milliseconds.