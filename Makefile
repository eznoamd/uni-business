.PHONY: mvn-build docker-build docker-run db-run compose-up compose-down run-local clean

# Build the project JAR using Maven
mvn-build:
	mvn clean package

# Build Docker image for the app
docker-build: mvn-build
	docker build -t uni-business:latest .

# Run the app image (uses .env for configuration)
docker-run: docker-build
	docker run --rm --env-file .env --name uni-business-app -p 8080:8080 --link unibusiness-db:db uni-business:latest

# Run Postgres DB container (reads vars from .env)
db-run:
	docker run --rm --name unibusiness-db --env-file .env -p 5432:5432 -d postgres:15

# Start app+db via docker-compose (recommended)
compose-up:
	docker compose up --build

# Stop and remove compose resources
compose-down:
	docker compose down -v

# Run the app locally from the built JAR
run-local: mvn-build
	java -jar target/uni-business-1.0-SNAPSHOT.jar

clean:
	mvn clean || true
	docker rmi uni-business:latest || true