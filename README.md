<h1 align="center">A Spring Boot orchestration service template for integrating Kafka, RabbitMQ and Redis</h1>

<p align="center">
    <img src="https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white"
         alt="Java">
    <img src="https://img.shields.io/badge/Apache%20Kafka-000?style=for-the-badge&logo=apachekafka"
alt="Apache Kafka">
    <img src="https://img.shields.io/badge/Rabbitmq-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white"
         alt="RabbitMQ">
    <img src="https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white"
         alt="Redis">
         
</p>

<p align="center">
  <a href="#how-to-use">How To Use</a> •
  <a href="#benchmark">Benchmark</a> •
  <a href="#credits">Credits</a>
</p>

## How To Use
Import the Postman collection to try out the API endpoints!
A description of the project can be found in the `report.pdf`.

Before running the commands below which will install the dependencies and run the executable, ensure that Kafka, RabbitMQ and Redis containers are up.
```bash
# Install git
sudo apt install git

# Install java
sudo apt install openjdk-21-jre-headless:amd64

# Clone this repository
$ git clone https://github.com/yuhaopro/java-backend-with-message-broker.git

# Go into the repository
$ cd java-backend-with-message-broker

# Install dependencies
$ mvn clean compile

# Run Kafka, RabbitMQ and Redis
$ docker compose pull && docker compose up

# Run the jar file
$ java -jar target/acp-0.0.1-SNAPSHOT.jar
```

## Benchmark

`RabbitMqService` keeps a single AMQP connection and channel open for the lifetime of the application (see `report.pdf`, section 4.1) instead of opening a new connection per request. To measure the effect, a standalone harness published 500 messages to a live RabbitMQ 4.0 broker two ways:

* **Naive**: open a new `Connection` + `Channel`, publish, then close them — once per message.
* **Optimized**: reuse one long-lived `Connection` + `Channel` across all publishes (what `RabbitMqService` actually does).

| Approach | Avg latency / message |
|---|---|
| Naive (new connection per request) | ~3.1 ms |
| Optimized (reused connection) | ~0.015 ms |
| **Speedup** | **~200x** |

Opening a new AMQP connection costs several network round trips (TCP handshake, protocol header exchange, `connection.start`/`tune`/`open`, `channel.open`) before a single message can be published; reusing the connection skips all of that and reduces a publish to a single write on an already-open channel.

## Credits

This is part of my cloud programming coursework in the University of Edinburgh. Hope you like it! 
