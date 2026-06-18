.PHONY: help setup doctor init-db test build run clean docker-up docker-app-up docker-down ui smoke

ifneq (,$(wildcard .env))
include .env
export
endif

MVN ?= mvn
MYSQL ?= mysql

ADMIN_DB_HOST ?= 127.0.0.1
ADMIN_DB_HOST_PORT ?= 3308
ADMIN_DB_PORT ?= $(ADMIN_DB_HOST_PORT)
ADMIN_DB_USERNAME ?= root
ADMIN_DB_PASSWORD ?= root123456
ADMIN_SERVER_PORT ?= 8090

help:
	@echo "Java Software Admin Service"
	@echo ""
	@echo "Common commands:"
	@echo "  make setup         Create .env from .env.example if missing"
	@echo "  make doctor        Check local development dependencies"
	@echo "  make docker-up     Start MySQL and Redis with Docker Compose"
	@echo "  make docker-app-up Start MySQL, Redis and admin-service with Docker Compose"
	@echo "  make init-db       Initialize db_java_software_admin"
	@echo "  make run           Run admin-service on $${ADMIN_SERVER_PORT:-8090}"
	@echo "  make test          Run admin-service tests"
	@echo "  make build         Build admin-service jar"
	@echo "  make clean         Clean Maven build output"
	@echo "  make smoke         Run local HTTP smoke checks"

setup:
	@if [ -f .env ]; then \
		echo ".env already exists"; \
	else \
		cp .env.example .env; \
		echo "Created .env from .env.example"; \
	fi

doctor:
	@bash scripts/doctor.sh

init-db:
	@echo "Initializing db_java_software_admin..."
	@for sql in database/mysql/*.sql; do \
		echo "Applying $$sql"; \
		MYSQL_PWD="$(ADMIN_DB_PASSWORD)" $(MYSQL) -h"$(ADMIN_DB_HOST)" -P"$(ADMIN_DB_PORT)" -u"$(ADMIN_DB_USERNAME)" < "$$sql"; \
	done

test:
	@echo "Testing admin-service..."
	cd admin-service && $(MVN) test

build:
	@echo "Building admin-service..."
	cd admin-service && $(MVN) clean package -DskipTests

run:
	@echo "Running admin-service on $${ADMIN_SERVER_PORT:-8090}..."
	cd admin-service && env -u DEBUG $(MVN) spring-boot:run

ui:
	@echo "Running standalone admin-ui at http://127.0.0.1:5178"
	cd admin-ui && python3 -m http.server 5178

smoke:
	@echo "Running smoke checks against admin-service..."
	bash scripts/smoke-test.sh

clean:
	cd admin-service && $(MVN) clean

docker-up:
	docker compose -f docker-compose.yml up -d mysql redis

docker-app-up:
	docker compose -f docker-compose.yml --profile app up -d --build

docker-down:
	docker compose -f docker-compose.yml down
