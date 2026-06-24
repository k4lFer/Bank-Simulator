.PHONY: help up down restart logs ps infra install run-% createsuperuser

SERVICES = accounts-service transfers-service ledger-service notifications-service users-service api-gateway

help: ## Show available commands
	@grep -E '^[a-zA-Z_-]+:.*?##' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ─── Full Docker environment ────────────────────────────────

up: ## docker compose up -d --build (todo)
	docker compose up -d --build

down: ## docker compose down (stop todo)
	docker compose down

restart: down up ## Reiniciar todo

logs: ## Ver logs de todos los contenedores
	docker compose logs -f

ps: ## Estado de los contenedores (ver si corre cada servicio)
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "  🌐 App:       http://localhost"
	@echo "  📊 Kafka UI:  http://localhost:8080"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

# ─── Local dev (sin Docker) ────────────────────────────────

infra: ## Levantar solo MySQL + Kafka para desarrollo local
	docker compose up -d mysql zookeeper kafka
	@sleep 3
	@echo "Creando topics de Kafka..."
	-docker compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --topic bank.transfer.events --partitions 3 --replication-factor 1 2>/dev/null || true
	-docker compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --topic bank.account.events --partitions 3 --replication-factor 1 2>/dev/null || true
	-docker compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --topic bank.notification.events --partitions 3 --replication-factor 1 2>/dev/null || true
	-docker compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --topic bank.user.events --partitions 2 --replication-factor 1 2>/dev/null || true
	@echo "✅ Infra lista. Ahora corre: make run-<servicio>"

install: ## Compilar e instalar shared-contracts en .m2 local
	cd shared-contracts && ./mvnw clean install -DskipTests -q

# ─── Run individual services ──────────────────────────────
# Cada comando compila shared-contracts + arranca el servicio.
# Abrí uno por terminal: make run-accounts, make run-transfers, etc.

run-accounts: install
	@echo "🚀 Accounts-service en :8082"
	cd accounts-service && ./mvnw spring-boot:run -q

run-transfers: install
	@echo "🚀 Transfers-service en :8083"
	cd transfers-service && ./mvnw spring-boot:run -q

run-ledger: install
	@echo "🚀 Ledger-service en :8084"
	cd ledger-service && ./mvnw spring-boot:run -q

run-notifications: install
	@echo "🚀 Notifications-service en :8085"
	cd notifications-service && ./mvnw spring-boot:run -q

run-users: install
	@echo "🚀 Users-service en :8081"
	cd users-service && ./mvnw spring-boot:run -q

run-gateway:
	@echo "🚀 Api-gateway en :8000"
	cd api-gateway && ./mvnw spring-boot:run -q

# ─── Admin management ──────────────────────────────────────────

BOOTSTRAP_SECRET ?= change-me-in-production

ifeq ($(OS),Windows_NT)
createsuperuser: ## Crear usuario administrador vía API
	powershell -ExecutionPolicy Bypass -File scripts/create-admin.ps1 -BootstrapSecret "$(BOOTSTRAP_SECRET)"
else
createsuperuser: ## Crear usuario administrador vía API
	bash scripts/create-admin.sh "$(BOOTSTRAP_SECRET)"
endif
