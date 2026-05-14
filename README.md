# 🎓 Sistema de Detección de Riesgo Académico

> Grafo bipartito ponderado estudiantes ↔ materias con collaborative filtering
> para detección preventiva de riesgo académico.

**Proyecto Integrador — Diseño de Algoritmos y Sistemas**  
Universidad de Santander (UDES) · Ingeniería de Sistemas · 2026-1

---

## 📋 Descripción

Sistema que construye un **grafo bipartito ponderado** entre estudiantes y materias, donde cada arista tiene como peso la nota obtenida. Usa **similitud coseno con índice invertido** para identificar "gemelos académicos" (estudiantes con patrones de notas similares) y predecir qué materias futuras representan riesgo de reprobación.

### Características principales

- **Collaborative filtering:** predice riesgo basándose en el historial de gemelos académicos.
- **Cold start multinivel:** 4 estrategias encadenadas para estudiantes con pocos datos:
  1. `GradeSimilarityStrategy` — similitud coseno sobre notas (≥ 3 materias cursadas).
  2. `ICFESSimilarityStrategy` — similitud coseno sobre puntajes ICFES/Saber 11 (colombianos).
  3. `HybridSimilarityStrategy` — ponderación 70% notas + 30% ICFES.
  4. `DemographicFallbackStrategy` — matching por programa + tipo de colegio (extranjeros).
- **Escalable a N = 100 000 estudiantes** con latencia < 500 ms por consulta (verificado empíricamente).

---

## 🏗️ Arquitectura

```
src/main/java/com/universidad/riesgoacademico/
├── domain/
│   ├── model/           # Entidades inmutables (records)
│   │   ├── Estudiante.java
│   │   ├── Materia.java
│   │   ├── RegistroAcademico.java
│   │   ├── PuntajeICFES.java
│   │   ├── PerfilAdmision.java
│   │   ├── GradeEntry.java
│   │   ├── MateriaEnRiesgo.java
│   │   └── ResultadoRiesgo.java
│   ├── algorithm/       # Algoritmos y estrategias
│   │   ├── GrafoBipartito.java
│   │   ├── GradeSimilarityStrategy.java
│   │   ├── ICFESSimilarityStrategy.java
│   │   ├── HybridSimilarityStrategy.java
│   │   └── DemographicFallbackStrategy.java
│   └── service/         # Orquestación y contrato
│       ├── SimilarityStrategy.java        # Interfaz Strategy
│       └── RiesgoAcademicoService.java    # Servicio principal
├── infrastructure/
│   ├── io/              # Carga y exportación de datos
│   │   ├── DataLoader.java                # Interfaz
│   │   ├── CsvDataLoader.java             # Implementación CSV
│   │   ├── ResultadoExporter.java         # Interfaz
│   │   └── JsonResultadoExporter.java     # Implementación JSON
│   ├── persistence/
│   │   └── InMemoryEstudianteRepository.java
│   └── AnalisisPipeline.java              # Pipeline Cargar→Analizar→Exportar
└── benchmark/
    ├── RiesgoAcademicoBenchmark.java       # JMH benchmarks
    ├── BruteForceSimilarityStrategy.java   # Baseline O(N×M)
    └── DataGenerator.java                  # Datos sintéticos con seed
```

### Decisiones de arquitectura (ADRs)

| ADR | Decisión | Alternativas rechazadas |
|-----|----------|------------------------|
| [ADR-001](docs/adr/ADR-001-algoritmo-principal.md) | Coseno + índice invertido | Fuerza bruta, LSH |
| [ADR-002](docs/adr/ADR-002-estructura-datos-central.md) | HashMap + ArrayList | Matriz densa, TreeMap |
| [ADR-003](docs/adr/ADR-003-arquitectura-infraestructura.md) | Pipeline con Strategy I/O | Monolítico, Event-driven |

---

## 🚀 Requisitos

- **Java:** 17+ (compilación con `--source 17 --target 17`)
- **Maven:** 3.9+
- **Memoria:** ≥ 4 GB de heap para benchmarks con N ≥ 100 000

---

## ⚡ Compilación y Ejecución

### Compilar el proyecto

```bash
mvn compile
```

### Ejecutar pruebas unitarias (91 tests)

```bash
mvn test
```

### Ejecutar mutation testing (PIT)

```bash
mvn test-compile pitest:mutationCoverage
```

El reporte HTML se genera en `target/pit-reports/index.html`.

### Ejecutar benchmarks JMH

```bash
# Construir JAR
mvn clean package -DskipTests

# Ejecutar benchmarks (N = 1K, 10K, 100K)
java -Xmx4g -jar target/benchmarks.jar -p n=1000,10000,100000 -wi 2 -i 3 -f 1

# Ejecutar con N = 500K (requiere más memoria y tiempo)
java -Xmx6g -jar target/benchmarks.jar -p n=500000 -wi 1 -i 2 -f 1
```

---

## 📊 Resultados

### Benchmarks JMH (tiempo promedio ms/op)

| N | Índice invertido | Fuerza bruta | Speedup |
|-------:|-------------------:|---------------:|--------:|
| 1 000 | 0.14 ms | 0.59 ms | 4.2× |
| 10 000 | 1.34 ms | 8.39 ms | 6.3× |
| 100 000 | 24.49 ms | 86.89 ms | 3.5× |
| 500 000 | 965.82 ms | 18 301.15 ms | 18.9× |

> Ver análisis completo en [BENCHMARK.md](BENCHMARK.md).

### Calidad del código

| Métrica | Umbral requerido | Resultado |
|---------|:----------------:|:---------:|
| Tests unitarios | — | 91 (JUnit 5 + jqwik) |
| Mutation score (PIT) | ≥ 60% | **74%** ✅ |
| Line coverage | — | **96%** |
| Test strength | — | **76%** |
| Latencia N=100K | < 500 ms | **24.49 ms** ✅ |

---

## 📁 Estructura del repositorio

```
code/
├── PROBLEMA.md              # Especificación formal del problema
├── BENCHMARK.md             # Resultados de benchmarks JMH
├── README.md                # Este archivo
├── pom.xml                  # Configuración Maven
├── docs/
│   └── adr/                 # Architecture Decision Records
│       ├── ADR-001-algoritmo-principal.md
│       ├── ADR-002-estructura-datos-central.md
│       └── ADR-003-arquitectura-infraestructura.md
├── src/
│   ├── main/java/...        # Código fuente (24 clases)
│   └── test/java/...        # Tests (5 archivos, 91 tests)
└── target/                  # Artefactos generados (gitignored)
```

---

## 🧪 Pruebas

### Suite de pruebas (91 tests)

| Archivo | Tests | Tipo | Cobertura |
|---------|:-----:|------|-----------|
| `GrafoBipartitoTest` | 13 | Unitario | Inserción, lookup, normas, casos borde |
| `StrategyMutationKillingTest` | 28 | Unitario (mutation killing) | 4 estrategias: valores exactos, fronteras, excepciones |
| `ModelTest` | 29 | Unitario | Records, validaciones, equals/hashCode |
| `RiesgoAcademicoServiceTest` | 15 | Integración | Selección de estrategia, análisis batch |
| `AlgorithmPropertyTest` | 5 | PBT (jqwik) | Propiedades algebraicas: rango [0,1], simetría, idempotencia |

### Ejecución rápida

```bash
# Todos los tests
mvn test

# Solo tests unitarios (sin PBT)
mvn test -Dtest="!AlgorithmPropertyTest"
```

---

## 📐 Especificación formal

Ver [PROBLEMA.md](PROBLEMA.md) para:
- Pre/postcondiciones formales (Pre1–Pre8, Post1–Post6)
- Invariantes del sistema (Inv1–Inv6)
- 14 casos borde documentados
- Restricciones de escala (N = 100 000, latencia < 500 ms)

---

## 👤 Autor

Julio Andrés Toscano Morales — Ingeniería de Sistemas, UDES 2026
