# Sistema de Detección de Riesgo Académico

### Grafo Bipartito Ponderado con Collaborative Filtering

> **Proyecto Integrador** · Diseño de Algoritmos y Sistemas · Unidad 12  
> Universidad de Santander (UDES) · Ingeniería de Sistemas · 2026-1

---

## Tabla de Contenido

- [Descripción del Problema](#descripción-del-problema)
- [¿Cómo funciona?](#cómo-funciona)
- [Arquitectura del Sistema](#arquitectura-del-sistema)
- [Decisiones de Diseño (ADRs)](#decisiones-de-diseño-adrs)
- [Requisitos](#requisitos)
- [Compilación y Ejecución](#compilación-y-ejecución)
- [Suite de Pruebas](#suite-de-pruebas)
- [Resultados de Benchmarks](#resultados-de-benchmarks)
- [Métricas de Calidad](#métricas-de-calidad)
- [Especificación Formal](#especificación-formal)
- [Estructura del Repositorio](#estructura-del-repositorio)
- [Autores](#autores)

---

## Descripción del Problema

En universidades con miles de estudiantes, **identificar de forma preventiva quién tiene alta probabilidad de reprobar una asignatura** es crítico para la intervención oportuna. Actualmente, las alertas llegan cuando el estudiante ya reprobó — demasiado tarde.

Este sistema modela la relación entre estudiantes y materias como un **grafo bipartito ponderado**, donde:

- **Nodos izquierdos** = estudiantes
- **Nodos derechos** = materias universitarias
- **Aristas ponderadas** = nota obtenida (0.0 – 5.0, escala colombiana)

Usando **filtrado colaborativo** (el mismo principio que Netflix para recomendaciones), el sistema encuentra *gemelos académicos*: estudiantes con patrones de notas similares. Si tu gemelo académico reprobó Cálculo II el semestre pasado, el sistema te alerta **antes** de que empieces el semestre.

### El problema del Cold Start

Un estudiante de primer semestre no tiene notas → el sistema no puede ubicarlo. Nuestra solución: un **cold start multinivel** que aprovecha datos disponibles antes de la primera nota:

```
¿Tiene notas universitarias (≥ 2)?
├── SÍ + ICFES → Estrategia Híbrida (70% notas + 30% ICFES) — Confianza MUY ALTA
├── SÍ sin ICFES → Estrategia por Notas — Confianza ALTA
└── NO → ¿Tiene ICFES (Saber 11)?
          ├── SÍ → Estrategia ICFES (5 componentes) — Confianza MEDIA
          └── NO → Estrategia Demográfica (programa + colegio) — Confianza BAJA
```

> **¿Por qué ICFES?** En Colombia, las universidades exigen el puntaje ICFES (Saber 11) para admisión. Si dos estudiantes tienen puntajes ICFES similares, es probable que tengan rendimiento académico similar. Para estudiantes **extranjeros** sin ICFES (ej: primer semestre), el sistema usa un fallback demográfico con advertencia explícita al consejero.

---

## ¿Cómo funciona?

### 1. Construcción del Grafo Bipartito

```
Estudiante A ──(4.5)──→ Cálculo I
             ──(3.8)──→ Física I
             ──(4.2)──→ Programación

Estudiante B ──(4.3)──→ Cálculo I        ← Materias compartidas con A
             ──(3.9)──→ Física I         ← Notas similares → ¡Gemelos!
             ──(2.1)──→ Cálculo II       ← B reprobó → ¡Alerta para A!
```

### 2. Similitud Coseno con Índice Invertido

En lugar de comparar cada estudiante contra todos los demás (**O(N²)**, inviable para 100K estudiantes), usamos un **índice invertido por materia**:

```
Índice invertido:
  Cálculo I  → [(A, 4.5), (B, 4.3), (C, 2.0), ...]
  Física I   → [(A, 3.8), (B, 3.9), (D, 4.1), ...]
```

Para encontrar los gemelos de A, solo visitamos las listas de las materias que A cursó. Esto reduce la complejidad de **O(N × M)** a **O(M_e × k_avg)** donde:
- `M_e` ≈ 35 (materias del estudiante)
- `k_avg` ≈ 500 (co-estudiantes promedio por materia)
- Resultado: **~17,500 operaciones** en lugar de **~50,000,000**

### 3. Generación de Alertas

```
Entrada:  estudianteId = A, umbralSimilitud = 0.7, umbralRiesgo = 3.0
                                    ↓
       Buscar gemelos con similitud ≥ 0.7
                                    ↓
       Gemelos encontrados: [B (sim=0.95), E (sim=0.82)]
                                    ↓
       ¿Algún gemelo reprobó materias que A no ha cursado?
       B reprobó Cálculo II (nota 2.1 < 3.0) ✓
                                    ↓
Salida:  [MateriaEnRiesgo("CAL2", "Cálculo II", riesgo=0.58, gemelos=1)]
```

---

## Arquitectura del Sistema

El sistema sigue **Clean Architecture** con separación estricta entre dominio e infraestructura:

```
src/main/java/com/universidad/riesgoacademico/
│
├── domain/                          ← Núcleo (sin dependencias externas)
│   │
│   ├── model/                       ← Entidades inmutables (records Java 17)
│   │   ├── Estudiante.java          Nodo del conjunto S (con ICFES y perfil opcionales)
│   │   ├── Materia.java             Nodo del conjunto M
│   │   ├── RegistroAcademico.java   Arista ponderada (estudianteId, materiaId, nota)
│   │   ├── PuntajeICFES.java        Vector ICFES de 5 componentes [0,100]^5
│   │   ├── PerfilAdmision.java      Datos de admisión (programa, colegio, país)
│   │   ├── GradeEntry.java          Entry del índice invertido (estudianteId, nota)
│   │   ├── MateriaEnRiesgo.java     Materia con nivel de riesgo [0.0, 1.0]
│   │   └── ResultadoRiesgo.java     Output: materias en riesgo + estrategia + gemelos
│   │
│   ├── algorithm/                   ← Algoritmos y estrategias (patrón Strategy)
│   │   ├── GrafoBipartito.java      Grafo directo + índice invertido + normas
│   │   ├── GradeSimilarityStrategy.java      Coseno sobre notas universitarias
│   │   ├── ICFESSimilarityStrategy.java      Coseno sobre vector ICFES (5D)
│   │   ├── HybridSimilarityStrategy.java     70% notas + 30% ICFES
│   │   └── DemographicFallbackStrategy.java  Matching demográfico (cold start)
│   │
│   └── service/                     ← Orquestación y contrato
│       ├── SimilarityStrategy.java           Interfaz Strategy
│       └── RiesgoAcademicoService.java       Servicio principal (selección automática)
│
├── infrastructure/                  ← Adaptadores (I/O, persistencia)
│   ├── io/
│   │   ├── DataLoader.java                   Interfaz de carga de datos
│   │   ├── CsvDataLoader.java               Implementación CSV
│   │   ├── ResultadoExporter.java            Interfaz de exportación
│   │   └── JsonResultadoExporter.java        Implementación JSON
│   ├── persistence/
│   │   └── InMemoryEstudianteRepository.java Repositorio en memoria
│   └── AnalisisPipeline.java                 Pipeline: Cargar → Analizar → Exportar
│
└── benchmark/                       ← Benchmarks JMH (no en producción)
    ├── RiesgoAcademicoBenchmark.java         Benchmark principal
    ├── BruteForceSimilarityStrategy.java     Baseline O(N×M) para comparación
    └── DataGenerator.java                    Generador de datos sintéticos
```

### Patrones de diseño aplicados

| Patrón | Dónde | Por qué |
|--------|-------|---------|
| **Strategy** | `SimilarityStrategy` → 4 implementaciones | Selección automática de algoritmo según datos disponibles del estudiante |
| **Pipeline** | `AnalisisPipeline` (Cargar → Analizar → Exportar) | Desacoplar etapas de I/O del dominio (ADR-003) |
| **Repository** | `InMemoryEstudianteRepository` | Abstraer persistencia del dominio |

---

## Decisiones de Diseño (ADRs)

El proyecto documenta **3 Architecture Decision Records** en formato MADR, cada uno con ≥2 alternativas rechazadas y consecuencias negativas específicas:

| ADR | Decisión | Alternativas rechazadas | Justificación clave |
|-----|----------|------------------------|---------------------|
| [ADR-001](docs/adr/ADR-001-algoritmo-principal.md) | **Coseno + índice invertido** | Fuerza bruta O(N×M), LSH/MinHash | 17,500 ops/consulta vs 50M (fuerza bruta). Resultado exacto vs aproximado (LSH) |
| [ADR-002](docs/adr/ADR-002-estructura-datos-central.md) | **HashMap + ArrayList** | Matriz densa O(N×M), TreeMap | 160 MB vs 381 MB (matriz) vs 534 MB (TreeMap). O(1) lookup vs O(log N) |
| [ADR-003](docs/adr/ADR-003-arquitectura-infraestructura.md) | **Pipeline con Strategy I/O** | Método monolítico, Event-driven | Testeable, extensible (OCP), coherente con Strategy del dominio |

---

## Requisitos

| Requisito | Versión |
|-----------|---------|
| **Java** | 17+ |
| **Maven** | 3.9+ |
| **Memoria (benchmarks)** | ≥ 4 GB heap (`-Xmx4g`) para N ≥ 100,000 |

---

## Compilación y Ejecución

```bash
# Compilar el proyecto
mvn compile

# Ejecutar los 91 tests (JUnit 5 + jqwik PBT)
mvn test

# Verificar complejidad ciclomática V(G) ≤ 10
mvn pmd:check

# Ejecutar mutation testing (PIT) — genera reporte en target/pit-reports/
mvn test-compile pitest:mutationCoverage

# Generar reporte de cobertura JaCoCo — en target/site/jacoco/
mvn test jacoco:report
```

### Benchmarks JMH

```bash
# Construir JAR de benchmarks
mvn clean package -DskipTests

# Ejecutar benchmarks (N = 1K, 10K, 100K)
java -Xmx4g -jar target/benchmarks.jar -p n=1000,10000,100000 -wi 2 -i 3 -f 1

# Ejecutar con N = 500K (requiere más memoria y tiempo)
java -Xmx6g -jar target/benchmarks.jar -p n=500000 -wi 1 -i 2 -f 1
```

---

## Suite de Pruebas

### 91 tests · 0 fallos · 5 propiedades algebraicas

| Archivo | Tests | Tipo | Qué verifica |
|---------|:-----:|------|-------------|
| `ModelTest` | 29 | Unitario (EP/BVA) | Validaciones de records, equals/hashCode, valores límite |
| `GrafoBipartitoTest` | 14 | Unitario | Inserción, lookup, índice invertido, normas, duplicados |
| `StrategyMutationKillingTest` | 28 | Mutation killing | 4 estrategias: valores exactos, fronteras, excepciones, simetría |
| `RiesgoAcademicoServiceTest` | 15 | Integración | Selección de estrategia, alertas, postcondiciones, precondiciones |
| `AlgorithmPropertyTest` | 5 | PBT (jqwik) | Propiedades algebraicas del algoritmo |

### Propiedades algebraicas verificadas (jqwik)

| # | Propiedad | Tries | Seed |
|---|-----------|:-----:|------|
| 1 | **Similitud ∈ [0, 1]** — Inv3 del sistema | 200 | 12345 |
| 2 | **Similitud es simétrica** — sim(A,B) = sim(B,A) | 100 | 67890 |
| 3 | **Análisis es idempotente** — Post5 (reproducibilidad) | 100 | 11111 |
| 4 | **Nivel de riesgo ∈ [0, 1]** — acotación del output | 150 | 22222 |
| 5 | **Norma ≥ 0** — invariante del grafo | 200 | 33333 |

---

## Resultados de Benchmarks

### Comparación: Índice Invertido vs Fuerza Bruta

| N (estudiantes) | Índice Invertido | Fuerza Bruta | Speedup |
|----------------:|-------------------:|---------------:|--------:|
| **1,000** | 0.14 ms | 0.59 ms | **4.2×** |
| **10,000** | 1.34 ms | 8.39 ms | **6.3×** |
| **100,000** | 24.49 ms | 86.89 ms | **3.5×** |
| **500,000** | 965.82 ms | 18,301.15 ms | **18.9×** |

### Análisis de la curva de crecimiento

| Transición | Factor de N | Factor de tiempo (main) | Complejidad observada |
|-----------|:-----------:|:----------------------:|:--------------------:|
| 1K → 10K | 10× | 9.6× | ~O(N) |
| 10K → 100K | 10× | 18.2× | ~O(N log N) |
| 100K → 500K | 5× | 39.4× | > O(N log N)* |

> \* La desviación a N=500K se explica por presión de GC (+2 GB heap), cache misses en listas invertidas de ~55K entries, y rehashing de HashMaps. Ver análisis completo en [BENCHMARK.md](BENCHMARK.md).

### Cumplimiento de restricciones

| Restricción | Umbral | N = 100K | ¿Cumple? |
|------------|:------:|:--------:|:--------:|
| Latencia consulta online | < 500 ms | **24.49 ms** | ✅ (margen 20×) |
| Precálculo offline | < 30 s | ~5 s | ✅ |
| Memoria del índice | < 4 GB | ~160 MB | ✅ |

---

## Métricas de Calidad

| Métrica | Umbral requerido | Resultado | Herramienta |
|---------|:----------------:|:---------:|-------------|
| Tests unitarios + PBT | ≥ 15 + ≥ 3 | **91 tests** (86 + 5) | JUnit 5 + jqwik |
| Complejidad ciclomática | V(G) ≤ 10 | ✅ **0 violaciones** | PMD |
| Line coverage | — | **96%** | JaCoCo |
| Mutation score | ≥ 60% | **74%** | PIT (STRONGER) |
| Test strength | — | **76%** | PIT |
| Latencia N=100K | < 500 ms | **24.49 ms** | JMH |
| Commits semánticos | ≥ 5 | **13** | Git log |

---

## Especificación Formal

La especificación completa del problema está en [PROBLEMA.md](PROBLEMA.md):

- **8 precondiciones** (Pre1–Pre8) — verificables en código
- **6 postcondiciones** (Post1–Post6) — validadas en tests
- **6 invariantes** (Inv1–Inv6) — verificadas con jqwik PBT
- **14 casos borde** (CB-01 a CB-14) — todos con pruebas asociadas
- **Restricción de escala:** N = 100,000 estudiantes, M = 500 materias, latencia < 500 ms
- **Cold start multinivel:** 4 estrategias encadenadas (ICFES → demográfico)
- **Trabajo futuro** documentado (fuera del alcance)

---

## Estructura del Repositorio

```
julio-toscano-proyectoIntegrador/
├── PROBLEMA.md                         # Especificación formal del problema
├── BENCHMARK.md                        # Resultados y análisis de benchmarks JMH
├── README.md                           # Este archivo
├── pom.xml                             # Configuración Maven (JUnit, jqwik, JMH, PIT, JaCoCo, PMD)
├── pmd-rules.xml                       # Ruleset PMD custom (V(G) ≤ 10)
├── benchmark-results.txt               # Salida raw de JMH
├── benchmark-results-100k.txt          # Resultados para N = 100K
├── benchmark-results-500k.txt          # Resultados para N = 500K
├── benchmark-results-500k-baseline.txt # Baseline fuerza bruta N = 500K
├── .gitignore
│
├── docs/
│   └── adr/                            # Architecture Decision Records
│       ├── ADR-001-algoritmo-principal.md
│       ├── ADR-002-estructura-datos-central.md
│       └── ADR-003-arquitectura-infraestructura.md
│
├── src/
│   ├── main/java/com/universidad/riesgoacademico/
│   │   ├── domain/
│   │   │   ├── model/        (8 clases)   # Entidades inmutables
│   │   │   ├── algorithm/    (5 clases)   # Grafo + 4 estrategias
│   │   │   └── service/      (2 clases)   # Interfaz + servicio
│   │   ├── infrastructure/   (5 clases)   # Pipeline, CSV, JSON, repositorio
│   │   └── benchmark/        (3 clases)   # JMH benchmarks + baseline
│   │
│   └── test/java/com/universidad/riesgoacademico/
│       └── domain/
│           ├── algorithm/     (3 archivos)  # GrafoBipartitoTest, StrategyMutationKillingTest, AlgorithmPropertyTest
│           ├── model/         (1 archivo)   # ModelTest (29 tests)
│           └── service/       (1 archivo)   # RiesgoAcademicoServiceTest (15 tests)
│
└── target/                             # Artefactos generados (gitignored)
    ├── site/jacoco/                    # Reporte de cobertura JaCoCo
    └── pit-reports/                    # Reporte de mutation testing PIT
```

---

## Autores

| Nombre | Rol |
|--------|-----|
| **Keiver Castellanos** | Ingeniería de Sistemas · UDES 2026 |
| **Andrés Toscano** | Ingeniería de Sistemas · UDES 2026 |

---

> **Proyecto Integrador** · Diseño de Algoritmos y Sistemas · Unidad 12  
> Universidad de Santander (UDES) · 2026-1
