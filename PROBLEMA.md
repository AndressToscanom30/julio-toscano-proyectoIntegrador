# PROBLEMA.md — Especificación Formal

## Nombre del Problema

**Detección de Riesgo Académico mediante Grafo Bipartito Ponderado (Collaborative Filtering Académico)**

---

## Motivación

En entornos universitarios con cientos o miles de estudiantes, identificar de forma preventiva qué alumnos tienen alta probabilidad de reprobar una asignatura es crítico para la intervención oportuna. El sistema modela la relación entre estudiantes y materias como un **grafo bipartito ponderado**, donde el peso de cada arista representa la nota obtenida. Aplicando un algoritmo de similitud entre estudiantes (filtrado colaborativo), el sistema detecta *gemelos académicos*: pares de estudiantes con perfiles de notas similares. Si un gemelo académico reprobó una materia en semestres anteriores, el sistema emite una alerta preventiva antes de que el estudiante actualmente inscrito la curse.

El problema incorpora una extensión real del contexto universitario colombiano: el puntaje **ICFES (Saber 11)**, prueba de Estado que evalúa a los bachilleres antes de ingresar a la educación superior. Dado que la mayoría de universidades colombianas exigen este puntaje como requisito de admisión, el sistema lo utiliza como vector de aptitud inicial para resolver parcialmente el problema de *cold start* en estudiantes de primer semestre.

---

## Entradas

| Parámetro | Tipo | Rango / Restricciones |
|---|---|---|
| `S` — conjunto de estudiantes | `Set<Estudiante>` | 1 ≤ \|S\| ≤ 100 000; cada estudiante tiene un ID único `long` > 0 |
| `M` — conjunto de materias universitarias | `Set<Materia>` | 1 ≤ \|M\| ≤ 500; cada materia tiene un código `String` no vacío |
| `R` — conjunto de registros académicos | `Set<Registro>` | Tripletas `(estudianteId, materiaId, nota)`; sin duplicados para el mismo par `(estudianteId, materiaId)` en el mismo período |
| `nota` — calificación numérica | `double` | 0.0 ≤ nota ≤ 5.0; escala colombiana estándar; dos decimales de precisión |
| `icfes` — vector de puntaje ICFES | `PuntajeICFES` (record) | Opcional (nullable); componentes: Matemáticas, Lectura Crítica, Ciencias Naturales, Sociales y Ciudadanas, Inglés; cada componente en rango [0, 100] |
| `perfilAdmision` — datos básicos de admisión | `PerfilAdmision` (record) | Opcional; contiene: programa académico (`String`), tipo de colegio (`PUBLICO`/`PRIVADO`), país de origen (`String`). Usado como fallback de último nivel para cold start verdadero |
| `umbralSimilitud` — parámetro de consulta | `double` | 0.0 < umbralSimilitud ≤ 1.0; determina qué tan parecidos deben ser dos estudiantes para considerarse gemelos académicos |
| `umbralRiesgo` — nota de reprobación | `double` | 0.0 < umbralRiesgo ≤ 5.0; por defecto 3.0 en escala colombiana |
| `estudianteId` — ID del estudiante a evaluar | `long` | Debe existir en el sistema; ≥ 1 |

---

## Precondiciones

- **Pre1:** El conjunto de estudiantes `S` no es nulo y contiene al menos un elemento (`|S| ≥ 1`).
- **Pre2:** El conjunto de materias `M` no es nulo y contiene al menos un elemento (`|M| ≥ 1`).
- **Pre3:** Cada registro en `R` referencia un `estudianteId` que existe en `S` y un `materiaId` que existe en `M`.
- **Pre4:** No existen dos registros en `R` con el mismo par `(estudianteId, materiaId)` para el mismo período académico.
- **Pre5:** Todos los valores de `nota` satisfacen `0.0 ≤ nota ≤ 5.0`.
- **Pre6:** El `umbralSimilitud` satisface `0.0 < umbralSimilitud ≤ 1.0`.
- **Pre7:** Cada componente del vector ICFES, cuando está presente, satisface `0 ≤ componente ≤ 100`.
- **Pre8:** El `estudianteId` consultado existe en `S`.

> **Nota:** A diferencia de versiones iniciales del diseño, Pre8 **no** exige que el estudiante tenga registros en `R`. El sistema maneja la ausencia de registros mediante la estrategia de similitud apropiada según el perfil del estudiante (ver sección Estrategias de Similitud).

---

## Postcondiciones

- **Post1:** Para cada par de estudiantes `(e1, e2)` reportado como gemelos académicos, la similitud calculada (según la estrategia activa) satisface `similitud(e1, e2) ≥ umbralSimilitud`.
- **Post2:** El sistema retorna una lista de materias en riesgo para el estudiante consultado. Una materia `m` aparece en la lista si y solo si existe al menos un gemelo académico `e2` de `e1` tal que `e2` obtuvo `nota(e2, m) < umbralRiesgo` en un período anterior.
- **Post3:** La lista de materias en riesgo está ordenada de mayor a menor nivel de riesgo estimado (`nivelRiesgo = 1 - promedio_nota_gemelos(m)`).
- **Post4:** El resultado incluye metadatos de la estrategia utilizada: `GRADE_BASED`, `ICFES_BASED`, `HYBRID`, o `DEMOGRAPHIC_FALLBACK`, permitiendo al consejero académico conocer el nivel de confianza de la predicción.
- **Post5:** El resultado es reproducible: para las mismas entradas y el mismo `umbralSimilitud`, el sistema produce exactamente la misma lista de materias en riesgo.
- **Post6:** Si no es posible calcular similitud con ningún otro estudiante (cold start verdadero sin datos suficientes), el sistema retorna `Result.err(ColdStartException)` con el nivel de cold start identificado, sin lanzar excepción unchecked.

---

## Invariantes

- **Inv1:** El grafo es estrictamente bipartito: nunca existe una arista entre dos estudiantes ni entre dos materias. Solo existen aristas del conjunto `S` al conjunto `M`.
- **Inv2:** Los pesos de todas las aristas del grafo de notas satisfacen `0.0 ≤ peso ≤ 5.0` en todo momento.
- **Inv3:** La similitud entre cualquier par de estudiantes satisface `0.0 ≤ similitud ≤ 1.0` (dado que los vectores son no negativos y la similitud coseno está acotada en ese rango para vectores en R⁺).
- **Inv4:** El número total de aristas en el grafo satisface `|E| ≤ |S| × |M|`.
- **Inv5:** La representación interna del grafo nunca contiene aristas duplicadas: cada par `(estudianteId, materiaId)` tiene como máximo una entrada.
- **Inv6:** La estrategia de similitud seleccionada para un estudiante es determinista: dado el mismo perfil de datos disponibles, siempre se selecciona la misma estrategia.

---

## Estrategias de Similitud (Cold Start Multinivel)

El sistema implementa el **patrón Strategy** para el cálculo de similitud, seleccionando automáticamente la estrategia apropiada según los datos disponibles del estudiante:

```
┌──────────────────────────────────────────────────────────────────────┐
│              Árbol de decisión — Selección de Estrategia             │
│                                                                      │
│  ¿Tiene registros académicos universitarios (|R_e| ≥ 2)?            │
│  ├── SÍ → ¿Tiene ICFES?                                             │
│  │         ├── SÍ → HybridSimilarityStrategy (HYBRID)              │
│  │         │         Vector: 70% notas + 30% ICFES normalizado      │
│  │         └── NO → GradeSimilarityStrategy (GRADE_BASED)           │
│  │                   Similitud coseno sobre vector de notas          │
│  └── NO → ¿Tiene ICFES?                                             │
│            ├── SÍ → ICFESSimilarityStrategy (ICFES_BASED)           │
│            │         Similitud coseno sobre vector ICFES [0,100]^5   │
│            └── NO → DemographicFallbackStrategy (DEMOGRAPHIC_FALLBACK)│
│                      Agrupa por: programa + tipo colegio             │
│                      Emite advertencia: COLD_START_VERDADERO         │
└──────────────────────────────────────────────────────────────────────┘
```

### Descripción de Estrategias

| Estrategia | Aplica a | Vector de similitud | Confianza |
|---|---|---|---|
| `GradeSimilarityStrategy` | Estudiantes con ≥2 notas universitarias y sin ICFES | Notas en materias compartidas (ℝ^M) | Alta |
| `ICFESSimilarityStrategy` | Estudiantes de 1er semestre colombianos con ICFES | Vector ICFES de 5 componentes (ℝ^5) | Media |
| `HybridSimilarityStrategy` | Estudiantes con notas universitarias **y** ICFES | Concatenación ponderada: notas (70%) + ICFES normalizado (30%) | Muy alta |
| `DemographicFallbackStrategy` | Estudiantes sin ICFES y sin notas (ej: internacionales 1er semestre) | Matching por programa + tipo de colegio de origen | Baja (advertencia explícita) |

### Caso Real: Estudiante Internacional (ej. venezolano)

Un estudiante venezolano en primer semestre no tiene ICFES (examen colombiano) y no tiene aún notas universitarias. El sistema aplica `DemographicFallbackStrategy`:
- Lo agrupa con estudiantes del mismo programa académico que tuvieron perfiles similares de colegio de origen.
- Emite al consejero académico la advertencia `COLD_START_VERDADERO` con nivel de confianza **BAJO**.
- En cuanto el estudiante obtiene su primera nota universitaria (inicio de 2do semestre o corte parcial del 1er semestre), el sistema recalcula automáticamente usando `GradeSimilarityStrategy`, eliminando la advertencia.

> **Decisión de diseño:** La estrategia `DemographicFallbackStrategy` no pretende ser precisa; su propósito es garantizar que **ningún estudiante quede sin cobertura del sistema**, generando al menos una alerta de nivel bajo en lugar de silencio total.

---

## Casos Borde Identificados

| # | Caso | Comportamiento esperado |
|---|---|---|
| CB-01 | Conjunto de registros `R` vacío (sistema vacío) | El grafo se carga sin aristas. Todos los estudiantes se procesan con `ICFESSimilarityStrategy` o `DemographicFallbackStrategy` según disponibilidad de datos. |
| CB-02 | Estudiante con un solo registro (`|R_e| = 1`) | No alcanza el umbral `|R_e| ≥ 2` para `GradeSimilarityStrategy`. Se usa `ICFESSimilarityStrategy` si tiene ICFES, o `DemographicFallbackStrategy` si no. |
| CB-03 | Estudiante internacional sin ICFES en 1er semestre | Se aplica `DemographicFallbackStrategy`. Se emite advertencia `COLD_START_VERDADERO`. Confianza: BAJA. |
| CB-04 | Estudiante internacional sin ICFES en 2do semestre (ya tiene notas) | Se aplica `GradeSimilarityStrategy` normalmente. El cold start se resuelve. |
| CB-05 | Todos los estudiantes con notas idénticas en todas las materias | La similitud coseno entre todos los pares es 1.0. Las alertas se generan correctamente para materias con promedio grupal < `umbralRiesgo`. |
| CB-06 | Estudiante que cursó materias que ningún otro cursó | Vector ortogonal a todos. Similitud = 0.0. Lista de riesgo vacía con advertencia. |
| CB-07 | `umbralSimilitud = 1.0` (gemelos perfectos) | Solo estudiantes con vectores de notas idénticos. Muy restrictivo; probablemente sin gemelos en casos reales. |
| CB-08 | Una sola materia en el sistema (`|M| = 1`) | El grafo bipartito tiene una sola "columna". Similitud coseno es 1.0 para todos los estudiantes con nota > 0 en esa materia. |
| CB-09 | Un solo estudiante en el sistema (`|S| = 1`) | No hay gemelos posibles. Lista de riesgo vacía con advertencia. |
| CB-10 | `N = |S| = 100 000` con `|M| = 500` (escala máxima) | El sistema debe completar la consulta en < 500 ms. El precálculo de índice puede tomar hasta 30 s (operación offline). |
| CB-11 | Nota exactamente igual al umbral de reprobación (`nota == umbralRiesgo`) | Condición de reprobación es estrictamente menor (`< umbralRiesgo`). `nota = 3.0` con `umbralRiesgo = 3.0` **no** se considera reprobado. |
| CB-12 | `estudianteId` no existente en el sistema | El sistema retorna `Result.err(EstudianteNoEncontradoException)` sin lanzar excepción unchecked. |
| CB-13 | Vector ICFES con todos los componentes en 0 | Norma del vector = 0. La similitud coseno es indefinida (división por cero). El sistema lo trata como ICFES ausente y cae al nivel `DemographicFallbackStrategy`. |
| CB-14 | Cambio de estrategia en tiempo de ejecución (transición warm-up) | Cuando un estudiante nuevo obtiene su primera nota universitaria, el sistema detecta el cambio de perfil y recalcula su estrategia en la siguiente consulta. No requiere reinicio del sistema. |

---

## Restricción de Escala

El sistema debe procesar hasta **N = 100 000 estudiantes** y **M = 500 materias** con:
- **Latencia de consulta online** (alerta de riesgo por estudiante): < **500 ms**
- **Tiempo de precálculo offline** (construcción del índice de similitud): < **30 segundos**
- **Memoria**: < 4 GB de heap JVM para el índice completo

El precálculo es una operación offline que se ejecuta al inicio del sistema o bajo demanda por el administrador. Las consultas individuales de riesgo (online) deben respetar el umbral de 500 ms usando el índice precalculado.

---

## Trabajo Futuro (fuera del alcance del proyecto integrador)

- Implementar retroalimentación del consejero académico para mejorar el modelo con el tiempo.
- Considerar factores socioeconómicos adicionales en `DemographicFallbackStrategy`.
- Soporte para procesamiento distribuido cuando `|S|` supere los 500 000 estudiantes.
- Validación cruzada del modelo con datos históricos reales (requiere acuerdo institucional de privacidad).
