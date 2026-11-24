# Installation de MailFlow

## Prérequis
- Java 21+
- PostgreSQL 15+ (testé avec 18)
- Maven (inclus via mvnw)

## Base de données
CREATE USER mailflow WITH PASSWORD 'mailflow';
CREATE DATABASE mailflow OWNER mailflow;

## Lancer le backend
cd backend
.\mvnw.cmd spring-boot:run

Vérification : http://localhost:8080/actuator/health doit renvoyer {"status":"UP"}.
