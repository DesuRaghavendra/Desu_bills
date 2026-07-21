# OCR-Based Dynamic Table Management System

A production-grade web application to upload table images, parse them via an OCR microservice, verify/edit them in an interactive spreadsheet interface, and save custom tables dynamically inside a relational PostgreSQL system.

## Project Stack

- **Backend:** Java 21 (LTS) & Spring Boot 3.4.x (Web, JPA, Validation, Flyway, MapStruct, Lombok)
- **Frontend:** React 19.x & Vite (Material UI, Axios, React Router)
- **OCR Service:** Python 3.11+ & FastAPI (OpenCV, PaddleOCR, PaddlePaddle)
- **Database:** PostgreSQL 16+

---

## Codebase Architecture

```text
d:/desu_bills_project/
├── backend/                  # Java Spring Boot Enterprise API
│   ├── src/main/java/com/system/
│   │   ├── controller/       # Exposed REST controllers
│   │   ├── service/          # Core business services
│   │   ├── repository/       # Spring Data JPA repositories
│   │   ├── entity/           # Database persistent entities
│   │   ├── dto/              # API payload transfer objects
│   │   ├── config/           # Application configuration
│   │   ├── exception/        # Exception handlers
│   │   ├── mapper/           # MapStruct translation mappers
│   │   ├── validation/       # Request validations
│   │   └── util/             # Helpers and utility methods
│   ├── pom.xml               # Maven configuration
│   └── Dockerfile            # Multi-stage JVM runtime container setup
│
├── frontend/                 # React UI Client Application
│   ├── src/
│   │   ├── components/       # Shared UI components
│   │   ├── pages/            # Page routers
│   │   ├── services/         # Axios API clients
│   │   ├── hooks/            # Custom hooks
│   │   ├── utils/            # Helper scripts
│   │   ├── layouts/          # Layout shells
│   │   └── contexts/         # State contexts
│   ├── package.json          # Node dependencies and scripts
│   └── vite.config.js        # Vite configuration
│
└── ocr_service/              # Python Vision Microservice
    ├── app.py                # FastAPI bootstrap application
    ├── routers/              # Route endpoints
    ├── services/             # Core OCR pipelines
    ├── models/               # Pydantic schema structures
    ├── preprocessing.py      # OpenCV image adjustments
    ├── table_parser.py       # Tabular data reconstruction algorithms
    ├── requirements.txt      # Python dependencies
    └── Dockerfile            # FastAPI container packaging
```

---

## Operational Launch Instructions

### 1. Docker Compose (Orchestrated Stack)

To pull, build, and link the containers (PostgreSQL database, Spring Boot, and FastAPI OCR service) together automatically:

```bash
docker-compose up --build
```

- **Backend API:** `http://localhost:8080`
- **FastAPI Documentation:** `http://localhost:8000/docs`
- **PostgreSQL Database Port:** `5432`

### 2. Local Manual Startup

If launching individual services natively:

#### A. Backend (Java Spring Boot)
Ensure a running instance of PostgreSQL is available, then configure connections in `backend/src/main/resources/application.yml` or export appropriate env properties:

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

To execute test suites:
```bash
mvn clean test
```

#### B. OCR Microservice (Python FastAPI)
Establish a local virtual environment:

```bash
cd ocr_service
python -m venv venv
venv\Scripts\activate  # Windows
source venv/bin/activate  # macOS/Linux

pip install -r requirements.txt
uvicorn app:app --reload --host 0.0.0.0 --port 8000
```

- Health Check Route: `GET http://localhost:8000/health`

#### C. Frontend (React UI)
Configure environments and run the hot-reloading dev server:

```bash
cd frontend
npm install
npm run dev
```

To compile production bundles:
```bash
npm run build
```
