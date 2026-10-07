# Sonar - InLane Technologies Assignment

To setup this application please have the following requirements satisfied in your computer:

```
Java Version 25 
Java Spring Boot >= 4.1.1
Spring Framework >= 7.0.9
PostgreSQL >= 18.0
PostgreSQL JDBC Driver 42.7.x
Maven 3.9.16
```

## 🚀 Steps for running this on your local machine
Inorder to run the application in your local machine please ensure that you have the above requirements satisfied.

### ⒈  Cloning the repository from github
Run the following command in your terminal to get the project
```
git clone https://github.com/likhithrajuuu/sonar.git
```

### 2. Configuring the `.env` in you machine
1. Please look for the file that is named as `.env.example`
2. Duplicate that file in the same root folder and rename to `.env`
3. Replace `your-database-name` with the actual database name in the `DATASOURCE_URL` field
4. Replace the credentials of `DATASOURCE_USERNAME` and `DATASOURCE_PASSWORD` with your database credentials

### 3. Running the spring boot application
Please go to the project directory in your terminal and run the following command to start the application

``` ./mvnw spring-boot:run ```

This will ensure the TomCat server is up and running on the port 8080 ! (default)