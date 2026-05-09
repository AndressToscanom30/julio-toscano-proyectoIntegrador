# ADR-002: Selección de la Estructura de Datos Central

## Estado

Aceptado — 2026-05-08

## Contexto

El sistema de riesgo académico representa un **grafo bipartito ponderado** donde los nodos son estudiantes y materias, y las aristas tienen como peso la nota obtenida. El algoritmo elegido (ADR-001) requiere dos estructuras fundamentales:

1. **Representación del grafo:** almacenar eficientemente las relaciones estudiante → materia con sus notas.
2. **Índice invertido:** mapear cada materia → lista de estudiantes que la cursaron, para evitar comparaciones exhaustivas durante la consulta de similitud.

### Operaciones críticas (cuello de botella)

| Operación | Frecuencia | Requisito |
|---|---|---|
| `getNotasEstudiante(id)` — obtener todas las notas de un estudiante | Cada consulta online | O(1) lookup + O(M_e) iteración |
| `getEstudiantesPorMateria(codigo)` — obtener co-estudiantes de una materia | Cada consulta online, por cada materia del estudiante | O(1) lookup + O(k) iteración |
| `getNorma(id)` — norma precalculada del vector de notas | Cada cálculo de similitud | O(1) |
| `agregarRegistro(estudianteId, materiaId, nota)` — insertar nueva arista | Carga de datos / actualización incremental | O(1) amortizado |
| `existeRegistro(estudianteId, materiaId)` — verificar duplicado | Validación de precondición Pre4 | O(1) |

### Consideraciones adicionales

- **Dispersión del grafo:** con N = 100 000 estudiantes y M = 500 materias, pero cada estudiante cursando ~35 materias en promedio, el grafo tiene ~3 500 000 aristas de un máximo de 50 000 000 posibles (7% de densidad). La EDD debe explotar esta dispersión.
- **Localidad de caché:** las consultas de similitud recorren las listas invertidas secuencialmente. Una estructura con buena localidad espacial (arrays contiguos) supera a estructuras basadas en nodos enlazados (LinkedList, TreeMap entries).
- **Memoria:** el presupuesto es < 4 GB de heap. La EDD completa (grafo + índice invertido + normas) debe caber holgadamente.

---

## Opciones Consideradas

### Opción A: Matriz de adyacencia densa (`double[][]`)

- **Descripción:** Una matriz bidimensional `double[N][M]` donde `matriz[i][j]` almacena la nota del estudiante `i` en la materia `j`, o `0.0` / `NaN` si no la cursó.
- **Complejidades:**

| Operación | Complejidad |
|---|---|
| `getNotasEstudiante(id)` | O(M) — recorrer toda la fila, incluyendo ceros |
| `getEstudiantesPorMateria(codigo)` | O(N) — recorrer toda la columna |
| `agregarRegistro` | O(1) — acceso directo por índice |
| `existeRegistro` | O(1) — verificar si `matriz[i][j] != 0` |
| **Espacio total** | **O(N × M) = 50 000 000 doubles = ~381 MB** |

- **Ventajas:**
  - Acceso O(1) por índice para lectura y escritura.
  - Excelente localidad de caché en recorridos de fila (datos contiguos en memoria).
  - No requiere hashing ni colisiones.
- **Desventajas:**
  - **Desperdicio masivo de memoria:** el 93% de las celdas son ceros (grafo disperso). 381 MB para almacenar 3.5M valores útiles de 50M posiciones.
  - **No hay índice invertido implícito:** obtener los co-estudiantes de una materia requiere recorrer la columna completa O(N), lo que anula la ventaja del algoritmo elegido en ADR-001.
  - **Mapeo de IDs:** requiere traducir `estudianteId` (long) y `materiaId` (String) a índices enteros con un mapa adicional, añadiendo overhead de indirección.
  - **No es extensible:** agregar un estudiante o materia puede requerir redimensionar la matriz completa.

### Opción B: Listas de adyacencia con `TreeMap` ordenado

- **Descripción:** Dos `TreeMap` anidados:
  - Grafo directo: `TreeMap<Long, TreeMap<String, Double>>` — estudiante → (materia → nota), ordenado por ID de estudiante y código de materia.
  - Índice invertido: `TreeMap<String, TreeMap<Long, Double>>` — materia → (estudiante → nota), ordenado por código de materia y por ID de estudiante.
- **Complejidades:**

| Operación | Complejidad |
|---|---|
| `getNotasEstudiante(id)` | O(log N) lookup + O(M_e) iteración |
| `getEstudiantesPorMateria(codigo)` | O(log M) lookup + O(k) iteración |
| `agregarRegistro` | O(log N + log M_e) + O(log M + log k) para ambos índices |
| `existeRegistro` | O(log N + log M_e) |
| **Espacio total** | **O(2 × \|E\|) ≈ 7 000 000 entries × ~80 bytes/entry ≈ 534 MB** |

- **Ventajas:**
  - Soporta iteración ordenada por ID de estudiante o código de materia (útil para reportes).
  - Permite consultas de rango (ej: "estudiantes con ID entre 1000 y 2000").
- **Desventajas:**
  - **Overhead de memoria:** cada entry de `TreeMap` (nodo Red-Black Tree) consume ~80 bytes de overhead (punteros a padre, hijos, color, key/value boxed). Para 7M entries, el overhead es ~534 MB — mayor que la matriz densa, a pesar de ser disperso.
  - **Peor localidad de caché:** los nodos del árbol están dispersos en el heap. El recorrido secuencial de la lista invertida causa cache misses frecuentes, degradando el rendimiento real respecto al teórico.
  - **O(log N) vs O(1) en lookup:** la consulta de similitud realiza miles de lookups por consulta. El factor logarítmico se acumula: log₂(100 000) ≈ 17, multiplicado por el número de co-estudiantes visitados.
  - **No necesitamos orden:** el cálculo de similitud coseno es conmutativo — el orden de iteración no afecta el resultado. Pagar por orden innecesario es un trade-off negativo.

### Opción C (elegida): Lista de adyacencia con `HashMap` + índice invertido con `HashMap` y `ArrayList`

- **Descripción:** Dos estructuras complementarias basadas en hashing:
  - **Grafo directo:** `HashMap<Long, Map<String, Double>>` — estudiante → {materia → nota}. Cada valor es un `HashMap` interno que permite lookup O(1) por materia.
  - **Índice invertido:** `HashMap<String, List<GradeEntry>>` — materia → lista de `GradeEntry(estudianteId, nota)`. Cada lista es un `ArrayList` para localidad de caché en recorridos secuenciales.
  - **Normas precalculadas:** `HashMap<Long, Double>` — estudiante → norma euclidiana del vector de notas, precalculada durante la carga.
- **Complejidades:**

| Operación | Complejidad |
|---|---|
| `getNotasEstudiante(id)` | **O(1)** lookup + O(M_e) iteración |
| `getEstudiantesPorMateria(codigo)` | **O(1)** lookup + O(k) iteración |
| `agregarRegistro` | **O(1)** amortizado en ambas estructuras |
| `existeRegistro` | **O(1)** — `grafo.get(estudianteId).containsKey(materiaId)` |
| `getNorma(id)` | **O(1)** — lookup directo |
| **Espacio total** | **O(\|E\| + N) ≈ 3 500 000 entries × ~48 bytes ≈ 160 MB** |

- **Ventajas:**
  - **O(1) en todas las operaciones críticas:** la consulta de similitud ejecuta miles de lookups por consulta; O(1) vs O(log N) marca una diferencia medible a escala.
  - **Memoria eficiente para grafos dispersos:** ~160 MB vs ~381 MB (matriz) o ~534 MB (TreeMap). Usa solo memoria proporcional a las aristas existentes, no a las posibles.
  - **Excelente localidad de caché en el índice invertido:** `ArrayList<GradeEntry>` almacena los entries contiguamente en memoria. El recorrido secuencial durante el cálculo de similitud se beneficia del prefetching del CPU.
  - **`GradeEntry` como record:** `record GradeEntry(long estudianteId, double nota)` — inmutable, compacto (16 bytes + overhead de objeto ≈ 32 bytes), con `equals`/`hashCode` generado automáticamente.
  - **Implementación estándar JDK:** `HashMap`, `ArrayList` y `record` son todas clases estándar de Java 17. No requiere librerías externas ni implementación propia.
  - **Actualización incremental natural:** `grafo.computeIfAbsent(id, k -> new HashMap<>()).put(materia, nota)` en O(1).
- **Desventajas:**
  - **Sin orden natural:** no se puede iterar sobre estudiantes o materias en orden sin un paso adicional de sorting. Para reportes ordenados, se requiere `new ArrayList<>(map.keySet())` + `Collections.sort()`. Esto no afecta la consulta de similitud pero sí la generación de informes.
  - **Rehashing ocasional:** si el `HashMap` se llena más allá del load factor (0.75), se redimensiona copiando todas las entries. En la carga inicial de 100 000 estudiantes, esto ocurre ~17 veces (log₂(100 000)). Se puede mitigar con `new HashMap<>(capacidadInicial)` pre-dimensionado.
  - **Datos duplicados:** el grafo directo y el índice invertido almacenan la misma información en direcciones opuestas. El costo de memoria se duplica respecto a tener solo una estructura. Esta duplicación es el precio de las consultas O(1) en ambas direcciones — un trade-off aceptado conscientemente.

---

## Decisión

Se elige la **Opción C: `HashMap` + `ArrayList` con índice invertido** porque:

1. **O(1) en todas las operaciones críticas** — el algoritmo de similitud (ADR-001) realiza miles de lookups por consulta. La diferencia entre O(1) y O(log N) con N = 100 000 es un factor de ~17× en cada lookup.
2. **Memoria proporcional a aristas reales** — ~160 MB vs ~381 MB (matriz densa) para un grafo con 7% de densidad. Escala correctamente si M crece.
3. **Localidad de caché en el índice invertido** — `ArrayList` permite recorrido secuencial con prefetching, crítico para el inner loop del cálculo de similitud.
4. **Sin implementación propia** — usa exclusivamente clases de la JDK estándar (`HashMap`, `ArrayList`, `record`). Menor riesgo de bugs y mantenimiento más simple.

---

## Consecuencias

**Positivas:**
- Lookup O(1) para grafo directo, índice invertido y normas precalculadas.
- Memoria total estimada de ~160 MB para escala máxima (N = 100 000, M = 500), muy dentro del presupuesto de 4 GB.
- `record GradeEntry` es inmutable y compacto — alineado con las convenciones del curso (inmutabilidad preferida, sin magic numbers).
- Actualización incremental O(1) cuando se registra una nueva nota.

**Negativas:**
- **Datos duplicados entre grafo directo e índice invertido** — la misma relación estudiante-materia-nota existe en ambas estructuras. Se acepta esta duplicación (~2× memoria) como precio de consultas O(1) bidireccionales. La alternativa (mantener solo una estructura y recalcular la otra bajo demanda) degradaría la latencia de consulta de O(M_e × k_avg) a O(N × M_avg), anulando la ventaja del algoritmo.
- **Rehashing en carga inicial** — mitigado con pre-dimensionamiento: `new HashMap<>((int)(N / 0.75) + 1)`.
- **Sin soporte nativo de iteración ordenada** — aceptable porque la consulta de similitud no requiere orden y los reportes ordenados son operaciones infrecuentes (batch offline).
