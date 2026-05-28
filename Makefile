.PHONY: help init-db test build run clean docker-up docker-down ui

MVN ?= mvn
MYSQL ?= mysql

ADMIN_DB_HOST ?= 127.0.0.1
ADMIN_DB_PORT ?= 3306
ADMIN_DB_USERNAME ?= root
ADMIN_DB_PASSWORD ?= root123456

help:
	@echo "Java Software Admin Service"
	@echo ""
	@echo "Common commands:"
	@echo "  make docker-up     Start MySQL and Redis with Docker Compose"
	@echo "  make init-db       Initialize db_java_software_admin"
	@echo "  make run           Run admin-service on 8090"
	@echo "  make test          Run admin-service tests"
	@echo "  make build         Build admin-service jar"
	@echo "  make clean         Clean Maven build output"

init-db:
	@echo "Initializing db_java_software_admin..."
	$(MYSQL) -h$(ADMIN_DB_HOST) -P$(ADMIN_DB_PORT) -u$(ADMIN_DB_USERNAME) -p$(ADMIN_DB_PASSWORD) < database/mysql/001_init_admin_schema.sql

test:
	@echo "Testing admin-service..."
	cd admin-service && $(MVN) test

build:
	@echo "Building admin-service..."
	cd admin-service && $(MVN) clean package -DskipTests

run:
	@echo "Running admin-service..."
	cd admin-service && $(MVN) spring-boot:run

ui:
	@echo "Running standalone admin-ui at http://127.0.0.1:5178"
	cd admin-ui && python3 -m http.server 5178

clean:
	cd admin-service && $(MVN) clean
	cd operation-log-service && $(MVN) clean

docker-up:
	docker compose -f docker-compose.yml up -d

docker-down:
	docker compose -f docker-compose.yml down
