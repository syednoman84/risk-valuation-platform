# Risk Valuation Platform

A comprehensive microservices-based financial risk modeling and valuation platform designed for enterprise-grade loan portfolio analysis, risk assessment, and pricing calculations.

## 🏗️ Architecture Overview

The platform follows a microservices architecture with four core services that work together to provide end-to-end risk valuation capabilities:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Position      │    │   Assumption    │    │     Model       │    │     Model       │
│  Management     │    │   Management    │    │   Management    │    │   Execution     │
│   Service       │    │    Service      │    │    Service      │    │    Service      │
│   Port: 8080    │    │   Port: 8081    │    │   Port: 8082    │    │   Port: 8083    │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │                       │
         └───────────────────────┼───────────────────────┼───────────────────────┘
                                 │                       │
                         ┌─────────────────┐    ┌─────────────────┐
                         │   PostgreSQL    │    │   File System   │
                         │    Database     │    │     Storage     │
                         └─────────────────┘    └─────────────────┘
```

## 🚀 Key Features

- **Multi-Step Model Chaining**: Execute complex financial models in sequential steps with data flow between stages
- **Dynamic Expression Engine**: MVEL-based expression evaluation for flexible model calculations
- **Position File Management**: Upload and process loan portfolios in various formats
- **Assumption Management**: Manage model parameters, lookup tables, and configuration data
- **RESTful APIs**: Comprehensive REST endpoints with OpenAPI documentation
- **Audit Trail**: Complete execution history and results tracking
- **Scalable Architecture**: Independent microservices for horizontal scaling

## 🛠️ Technology Stack

### Backend
- **Java 17** - Modern Java runtime
- **Spring Boot 3.5.4** - Application framework
- **Spring Data JPA** - Data persistence layer
- **Spring WebFlux** - Reactive web framework
- **PostgreSQL** - Primary database
- **Flyway** - Database migration management
- **MVEL 2.5.2** - Expression evaluation engine

### Development & Documentation
- **Maven** - Build and dependency management
- **Lombok** - Code generation
- **SpringDoc OpenAPI** - API documentation
- **Jackson** - JSON processing
- **Hibernate** - ORM with JSONB support

## 📋 Services Overview

### 1. Position Management Service (Port: 8080)
Manages financial position data including loan portfolios, investment positions, and related financial instruments.

**Key Endpoints:**
- `POST /api/positions/upload` - Upload position files (ZIP/CSV)
- `GET /api/positions` - Retrieve position data
- `GET /api/positions/{id}` - Get specific position details
- `DELETE /api/positions/{id}` - Remove position data

**Features:**
- Multi-format file upload support
- Position data validation and parsing
- Historical position tracking
- Bulk position operations

### 2. Assumption Management Service (Port: 8081)
Handles model assumptions, parameters, lookup tables, and configuration data used in risk calculations.

**Key Endpoints:**
- `POST /api/assumptions` - Create assumption sets
- `GET /api/assumptions` - List all assumption sets
- `GET /api/assumptions/{id}` - Get specific assumption set
- `PUT /api/assumptions/{id}` - Update assumption parameters
- `POST /api/assumptions/upload` - Upload assumption files (CSV tables)

**Features:**
- Key-value parameter management
- CSV lookup table support
- Version control for assumption sets
- Bulk assumption operations

### 3. Model Management Service (Port: 8082)
Manages financial model definitions, versioning, and model metadata for risk calculations.

**Key Endpoints:**
- `POST /api/models` - Create new models
- `GET /api/models` - List all models
- `GET /api/models/{id}` - Get model definition
- `PUT /api/models/{id}` - Update model
- `GET /api/models/{id}/versions` - Get model versions

**Features:**
- Dynamic model definition using JSON
- Expression-based field calculations
- Model versioning and history
- Input/output field mapping
- Derived field calculations

### 4. Model Execution Service (Port: 8083)
Orchestrates model execution, handles chaining, and manages execution results.

**Key Endpoints:**
- `POST /api/chains/execute-template` - Execute model chains
- `GET /api/executions` - List execution history
- `GET /api/executions/{id}` - Get execution details
- `GET /api/executions/{id}/results` - Get execution results
- `POST /api/models/execute` - Execute single models

**Features:**
- Multi-step model chain execution
- Cross-step data flow management
- Asynchronous execution processing
- Result aggregation and storage
- Execution status tracking

## 🔧 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Git

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd risk-valuation-platform
```

2. **Setup PostgreSQL databases**
```sql
CREATE DATABASE position_management;
CREATE DATABASE assumption_management;
CREATE DATABASE model_management;
CREATE DATABASE model_execution;
```

3. **Configure database connections**
Update `application.properties` in each service with your database credentials.

4. **Build the platform**
```bash
mvn clean install
```

5. **Start services** (in separate terminals)
```bash
# Position Management Service
cd position-management-service && mvn spring-boot:run

# Assumption Management Service  
cd assumption-management-service && mvn spring-boot:run

# Model Management Service
cd model-management-service && mvn spring-boot:run

# Model Execution Service
cd model-execution-service && mvn spring-boot:run
```

6. **Verify installation**
- Position Service: http://localhost:8080/swagger-ui.html
- Assumption Service: http://localhost:8081/swagger-ui.html
- Model Service: http://localhost:8082/swagger-ui.html
- Execution Service: http://localhost:8083/swagger-ui.html

## 📊 Example Usage

### 1. Upload Position Data
```bash
curl -X POST "http://localhost:8080/api/positions/upload" \
  -F "name=Loan Portfolio Q4 2024" \
  -F "positionDate=2024-12-31" \
  -F "file=@loan_portfolio.zip"
```

### 2. Create Assumption Set
```bash
curl -X POST "http://localhost:8081/api/assumptions" \
  -F "name=Risk Parameters Q4" \
  -F "basePD=0.03" \
  -F "baseLGD=0.45" \
  -F "riskMultiplier=0.02"
```

### 3. Define Risk Model
```bash
curl -X POST "http://localhost:8082/api/models" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Credit Risk Assessment",
    "modelDefinition": {
      "inputs": {
        "positionFields": ["creditScore", "ltvRatio", "principal"],
        "assumptionValues": ["basePD", "baseLGD"]
      },
      "derivedFields": [
        {
          "name": "riskScore",
          "expression": "assumption.keyLookup.basePD * (creditScore < 650 ? 1.5 : 1.0)"
        }
      ],
      "outputs": [
        {"name": "loanId", "expression": "loanId"},
        {"name": "riskScore", "expression": "riskScore"}
      ]
    }
  }'
```

### 4. Execute Model Chain
```bash
curl -X POST "http://localhost:8083/api/chains/execute-template" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Risk Assessment Chain",
    "positionFileId": "position-file-id",
    "steps": [
      {
        "modelId": "model-id",
        "assumptionSetId": "assumption-id",
        "outputPrefix": "risk_"
      }
    ]
  }'
```

## 🧪 Testing

The platform includes comprehensive test scenarios in the `test-data` directory:

- **Model Definitions**: Pre-built models for various risk calculations
- **Sample Data**: Position files and assumption sets for testing
- **Test Scripts**: cURL scripts for end-to-end testing
- **Chain Examples**: Multi-step model execution examples

Run test scenarios:
```bash
cd test-data/test-scenarios-curls
./test-scenario-3-steps-chain-curls.sh
```

## 📁 Project Structure

```
risk-valuation-platform/
├── assumption-management-service/     # Assumption and parameter management
├── model-execution-service/          # Model orchestration and execution
├── model-management-service/         # Model definition and versioning
├── position-management-service/      # Position data management
├── test-data/                       # Test scenarios and sample data
│   ├── assumptions/                 # Sample assumption sets
│   ├── models/                      # Model definitions
│   ├── positionfiles/              # Sample position data
│   └── test-scenarios-curls/       # Test scripts
├── assumption_files/               # Uploaded assumption files
├── position_files/                # Uploaded position files
└── helper-queries.sql             # Database utility queries
```

## 🔍 Model Chain Execution Flow

1. **Position Upload**: Load loan/investment data
2. **Assumption Setup**: Configure model parameters
3. **Model Definition**: Create calculation logic
4. **Chain Execution**: Run multi-step calculations
5. **Result Aggregation**: Collect and store outputs

### Example 3-Step Chain:
1. **Risk Assessment** → Calculate PD, LGD, risk scores
2. **Pricing Calculation** → Apply risk premiums, adjust rates  
3. **Payment Calculation** → Compute final payments and profitability

---

**Built for enterprise financial risk management and valuation workflows.**