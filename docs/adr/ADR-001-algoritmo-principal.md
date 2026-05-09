# ADR-001: Selección del Algoritmo Principal

## Estado

Aceptado — 2026-05-08

## Contexto

El sistema de detección de riesgo académico requiere calcular la **similitud entre estudiantes** a partir de un grafo bipartito ponderado (estudiantes ↔ materias). El objetivo es encontrar, para un estudiante dado, sus *gemelos académicos* (estudiantes con perfil de notas similar) y a partir de ellos generar alertas de materias en riesgo.

### Restricciones del problema

- **Escala:** hasta `N = 100 000` estudiantes y `M = 500` materias.
- **Latencia de consulta online:** < 500 ms por estudiante.
- **Precálculo offline:** aceptable hasta 30 segundos.
- **Representación:** grafo bipartito disperso — cada estudiante cursa en promedio 6–8 materias por semestre y acumula ~30–40 a lo largo de la carrera, por lo que `|E| << N × M`.

### Operaciones críticas

1. **Precálculo:** construir la representación vectorial de cada estudiante y el índice de similitud.
2. **Consulta online:** dado un `estudianteId`, encontrar los k gemelos más similares con `similitud ≥ umbralSimilitud` y generar la lista de materias en riesgo.

---

## Opciones Consideradas

### Opción A: Similitud coseno con comparación por fuerza bruta (pairwise)

- **Descripción:** Para cada consulta, se calcula la similitud coseno del estudiante consultado contra **todos** los demás estudiantes. Se seleccionan aquellos con similitud ≥ umbral.
- **Complejidad temporal:**
  - Precálculo: O(N × M) para construir los vectores de notas.
  - Consulta: **O(N × M)** por consulta — se recorren los N estudiantes y se calcula el producto punto sobre las M dimensiones del vector.
- **Complejidad espacial:** O(N × M) para la matriz de notas.
- **Ventajas:**
  - Implementación trivial (doble bucle for).
  - Resultado exacto — no hay aproximación.
  - Sin estructura adicional de indexación.
- **Desventajas:**
  - Para N = 100 000 y M = 500, cada consulta requiere `100 000 × 500 = 50 000 000` operaciones de punto flotante. Estimación: ~300–500 ms por consulta sin optimización, violando el umbral de 500 ms al considerar overhead de GC y caché miss.
  - **No escala:** si N crece a 200 000, la latencia se duplica linealmente.
  - El 90% del cómputo es desperdiciado: la mayoría de pares de estudiantes no comparten materias (vectores ortogonales → similitud = 0).

### Opción B: Locality-Sensitive Hashing (LSH) con MinHash

- **Descripción:** Se aplica MinHash para generar firmas compactas de cada estudiante y LSH para agrupar candidatos similares en *buckets*. Solo se calcula la similitud exacta entre candidatos del mismo bucket.
- **Complejidad temporal:**
  - Precálculo: O(N × M × b) donde b = número de funciones hash (~100–200).
  - Consulta: O(b + k × M) donde k = candidatos en el bucket (típicamente << N).
- **Complejidad espacial:** O(N × b) para las firmas + O(N) para las tablas hash.
- **Ventajas:**
  - Sublineal en N para la consulta — no necesita comparar contra todos los estudiantes.
  - Probado en sistemas de recomendación a gran escala (Netflix, Spotify).
- **Desventajas:**
  - **Resultado aproximado:** puede perder gemelos reales (falsos negativos) o incluir no-gemelos (falsos positivos). La tasa de error depende del número de bandas y filas del LSH, y requiere calibración empírica.
  - **Complejidad de implementación:** MinHash + LSH requiere implementar múltiples familias de funciones hash, particionamiento en bandas y filas, y manejo de colisiones.
  - Para nuestro rango de N (100 000), el overhead del hashing puede no compensar el ahorro, ya que existe una alternativa exacta con complejidad similar en la práctica (Opción C).
  - **Similitud Jaccard vs. coseno:** MinHash estima similitud Jaccard (presencia/ausencia), no coseno (ponderada por nota). Adaptarlo a coseno (SimHash) añade complejidad sin beneficio claro a esta escala.

### Opción C (elegida): Similitud coseno con índice invertido por materia

- **Descripción:** Se construye un **índice invertido** donde cada materia mapea a la lista de estudiantes que la cursaron junto con su nota. Para consultar los gemelos de un estudiante `e`, solo se visitan las listas invertidas de las materias que `e` cursó, acumulando las contribuciones al producto punto únicamente para los co-estudiantes (estudiantes que comparten al menos una materia con `e`).
- **Complejidad temporal:**
  - Precálculo: O(N × M_avg + N) donde M_avg ≈ 30–40 es el promedio de materias por estudiante. Incluye la construcción del índice invertido y el cálculo de las normas de cada vector.
  - Consulta: **O(M_e × k_avg)** donde M_e = materias del estudiante consultado (~30–40) y k_avg = promedio de estudiantes por materia (~200–800 en una universidad real). En el peor caso absoluto (todas las materias compartidas con todos): O(M × N), equivalente a la fuerza bruta, pero este escenario es prácticamente imposible en un grafo disperso.
  - **Estimación práctica:** para M_e = 35 y k_avg = 500 → 35 × 500 = 17 500 operaciones por consulta. **Tres órdenes de magnitud menos que la Opción A.**
- **Complejidad espacial:** O(|E|) para el índice invertido + O(N) para las normas precalculadas. Dado que `|E| << N × M`, esto es significativamente menor que la Opción A.
- **Ventajas:**
  - **Resultado exacto:** calcula la similitud coseno real, sin aproximación.
  - **Explota la dispersión del grafo:** solo procesa co-estudiantes, ignorando los pares sin materias en común (que tienen similitud = 0 por definición).
  - **Implementación con estructuras estándar de Java:** `HashMap<String, List<GradeEntry>>` para el índice invertido. No requiere librerías externas.
  - **Compatible con el patrón Strategy:** la interfaz `SimilarityStrategy` puede tener múltiples implementaciones (coseno por notas, por ICFES, híbrida) que usan el mismo índice invertido como infraestructura compartida.
  - **Actualización incremental:** al agregar un nuevo registro académico, solo se actualiza la lista invertida de una materia — O(1) amortizado.
- **Desventajas:**
  - Si una materia tiene una lista invertida muy larga (ej: Cálculo I con 10 000 estudiantes), la consulta para un estudiante con muchas materias "populares" será más lenta. **Mitigación:** se puede limitar la búsqueda a los top-k candidatos por materia con un heap.
  - Requiere memoria adicional para el índice invertido, aunque para |E| ≈ 3 000 000 (100 000 × 30) con entries de ~32 bytes, el índice ocupa ~96 MB, dentro del presupuesto de 4 GB.
  - La consulta no está paralelizada en esta versión. Para escenarios futuros con N > 500 000, sería necesario particionar el índice.

---

## Decisión

Se elige la **Opción C: Similitud coseno con índice invertido por materia** porque:

1. **Exactitud:** a diferencia de LSH (Opción B), no introduce error de aproximación. Los consejeros académicos necesitan confiabilidad total en las alertas de riesgo: un falso negativo (no alertar a un estudiante en riesgo) tiene consecuencias académicas reales.
2. **Rendimiento práctico:** la complejidad de consulta O(M_e × k_avg) con M_e ≈ 35 y k_avg ≈ 500 produce ~17 500 operaciones, completándose en < 5 ms — dos órdenes de magnitud bajo el umbral de 500 ms. La Opción A requiere ~50 000 000 operaciones por consulta.
3. **Simplicidad de implementación:** usa `HashMap` y `ArrayList` de la JDK estándar, sin dependencias externas. La Opción B requiere implementar MinHash y LSH desde cero.
4. **Dispersión del grafo:** el grafo bipartito universitario es inherentemente disperso (estudiante promedio cursa ~35 materias de 500 posibles = 7% de densidad). El índice invertido explota esta propiedad natural.

---

## Consecuencias

**Positivas:**
- Consultas de riesgo exactas en < 5 ms estimados para N = 100 000.
- Precálculo del índice en < 5 s (operación offline ejecutada al cargar datos).
- Actualización incremental O(1) al registrar nuevas notas.
- Código mantenible con estructuras de la JDK estándar.

**Negativas:**
- **Peor caso degenerado:** si el grafo deja de ser disperso (ej: universidad con solo 5 materias y 100 000 estudiantes), la consulta se degrada a O(N) por materia, equivalente a fuerza bruta. Esto no ocurre en una universidad real con M ≥ 100, pero el sistema no tiene una protección explícita contra este caso. Se documenta como limitación conocida.
- **Sin paralelismo:** la consulta es secuencial. Si en el futuro N crece a 500 000+, sería necesario particionar el índice invertido por shards de materias y consultar en paralelo. Esto queda fuera del alcance del proyecto integrador.
- **Memoria del índice:** ~96 MB para el índice completo a escala máxima. En dispositivos con < 2 GB de heap, podría requerir ajuste del parámetro `-Xmx`. Se documenta el requisito mínimo de memoria en el README.
