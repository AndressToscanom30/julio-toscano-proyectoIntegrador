# ADR-003: Arquitectura de la Capa de Infraestructura

## Estado

Aceptado — 2026-05-09

## Contexto

El sistema de riesgo académico tiene un núcleo algorítmico completo (dominio) que necesita interactuar con el mundo exterior: **cargar datos de entrada** (estudiantes, materias y registros académicos) y **exportar resultados** del análisis de riesgo. La capa de infraestructura debe diseñarse de forma que:

1. **No contamine el dominio:** las clases de `domain/algorithm/` y `domain/model/` no deben depender de detalles de I/O (Pre-requisito de clean architecture, Unidad 8).
2. **Sea extensible a nuevos formatos:** el equipo de consejería académica puede requerir importar datos desde Excel, base de datos SQL o API REST en el futuro.
3. **Sea testeable:** cada componente debe poder probarse de forma aislada.

### Operaciones requeridas

| Operación | Frecuencia | Formato actual |
|---|---|---|
| Cargar estudiantes (id, nombre, ICFES, perfil) | Una vez al inicio del sistema | CSV |
| Cargar materias (código, nombre) | Una vez al inicio del sistema | CSV |
| Cargar registros académicos (estudianteId, materiaId, nota) | Una vez al inicio + actualizaciones incrementales | CSV |
| Exportar resultados de análisis de riesgo | Bajo demanda (por estudiante o batch) | JSON |

### Decisión clave

¿Cómo orquestar el flujo completo: carga de datos → análisis de riesgo → exportación de resultados?

---

## Opciones Consideradas

### Opción A: Método monolítico en `Main.main()`

- **Descripción:** Un único método `main()` que ejecuta secuencialmente: abrir archivos CSV, parsear línea por línea, registrar en el servicio, llamar a `analizarRiesgo()` para cada estudiante, y escribir el JSON de salida. Sin abstracciones adicionales.
- **Ventajas:**
  - Implementación directa sin indirección.
  - Código más corto (una sola clase).
  - No requiere diseñar interfaces.
- **Desventajas:**
  - **Violación de SRP:** `main()` maneja parsing, lógica de negocio y serialización en un solo bloque. Complejidad ciclomática V(G) alta si se añaden validaciones.
  - **No testeable de forma unitaria:** no se puede probar la carga CSV sin también ejecutar el análisis, ni probar la exportación JSON sin cargar datos primero.
  - **Rigidez de formato:** cambiar de CSV a Excel o a base de datos requiere reescribir el método completo.
  - **Sin reutilización:** el flujo de carga no se puede invocar desde un benchmark o un test de integración sin duplicar código.

### Opción B (elegida): Pipeline con interfaces Strategy para I/O

- **Descripción:** Se define un pipeline de tres etapas (`Cargar → Analizar → Exportar`), donde cada etapa está encapsulada detrás de una interfaz:
  - `DataLoader` — interfaz para cargar datos de cualquier fuente (implementación: `CsvDataLoader`).
  - `ResultadoExporter` — interfaz para exportar resultados en cualquier formato (implementación: `JsonResultadoExporter`).
  - `AnalisisPipeline` — orquestador que conecta las etapas usando las interfaces, sin depender de implementaciones concretas.
- **Complejidades:**
  - Carga CSV: O(|R|) donde |R| = número de registros.
  - Exportación JSON: O(|alertas|) por estudiante.
  - Pipeline completo (batch): O(|R| + N × costo_consulta).
- **Ventajas:**
  - **Testeable:** cada etapa se puede probar aisladamente con mocks. La carga CSV se prueba sin ejecutar análisis, la exportación se prueba con resultados predefinidos.
  - **Extensible:** añadir un nuevo formato (Excel, SQL) solo requiere implementar la interfaz `DataLoader`, sin modificar el pipeline ni el dominio.
  - **Coherente con el dominio:** reutiliza el patrón Strategy ya presente en `SimilarityStrategy` del dominio, manteniendo consistencia arquitectónica.
  - **Reutilizable:** los benchmarks JMH pueden usar `DataLoader` para generar datos, y los tests de integración pueden usar `AnalisisPipeline` directamente.
  - **V(G) bajo:** cada clase tiene una única responsabilidad, manteniendo la complejidad ciclomática ≤ 10.
- **Desventajas:**
  - **Overhead de abstracciones:** para un proyecto académico con un solo formato de entrada (CSV) y un solo formato de salida (JSON), las interfaces añaden indirección que no se aprovecha completamente en esta versión. Se acepta este overhead por testabilidad y coherencia arquitectónica.
  - **Más clases:** 5-6 clases adicionales vs 1 sola clase monolítica. Aumenta la superficie de navegación del código.

### Opción C: Event-driven con Observer/Listener

- **Descripción:** La carga de datos emite eventos (`EstudianteRegistradoEvent`, `RegistroAcademicoEvent`) que los consumidores procesan asincrónicamente. El análisis de riesgo se dispara como reacción a un evento `DatosCargadosEvent`.
- **Ventajas:**
  - Desacoplamiento total entre productores y consumidores de datos.
  - Soporta procesamiento asíncrono y extensión por plugins.
- **Desventajas:**
  - **Over-engineering para la escala del proyecto:** el sistema es batch (carga offline → consulta online). No hay flujo de eventos en tiempo real que justifique el patrón.
  - **Complejidad de depuración:** el flujo de ejecución se vuelve no lineal, dificultando el rastreo de errores.
  - **Overhead de memoria:** cada evento requiere un objeto adicional y el bus de eventos mantiene referencias a todos los listeners.
  - No se justifica cuando el flujo es estrictamente secuencial (cargar → analizar → exportar).

---

## Decisión

Se elige la **Opción B: Pipeline con interfaces Strategy para I/O** porque:

1. **Testabilidad:** cada componente de I/O se puede probar aisladamente, cumpliendo los estándares de la rúbrica (pruebas unitarias por clase).
2. **Coherencia arquitectónica:** extiende el patrón Strategy ya usado en el dominio (`SimilarityStrategy`) a la capa de infraestructura, manteniendo una arquitectura homogénea.
3. **Separación domain/infrastructure:** las interfaces `DataLoader` y `ResultadoExporter` residen en el paquete `infrastructure/io/`, sin contaminar `domain/`. El dominio no conoce CSV ni JSON.
4. **Reutilización en benchmarks:** `DataGenerator` para los benchmarks JMH puede implementar la misma lógica de carga que `CsvDataLoader`, asegurando consistencia entre pruebas y producción.

---

## Consecuencias

**Positivas:**
- Separación clara: `domain/` no tiene imports de `java.io` ni de `infrastructure/`.
- Las pruebas unitarias de `CsvDataLoader` y `JsonResultadoExporter` son independientes del núcleo algorítmico.
- Añadir un `ExcelDataLoader` o un `DatabaseExporter` en el futuro solo requiere implementar una interfaz, sin modificar las clases existentes (OCP).
- El `AnalisisPipeline` se puede reutilizar en los benchmarks JMH cambiando solo el `DataLoader`.

**Negativas:**
- **Sobre-ingeniería potencial:** en esta versión del proyecto, solo existe un formato de entrada (CSV) y uno de salida (JSON). Las interfaces podrían percibirse como abstracciones prematuras. Se justifican por testabilidad y coherencia, no por extensibilidad real a corto plazo.
- **Más clases en el proyecto:** 6 clases nuevas en `infrastructure/` vs 1 clase monolítica. Aumenta la complejidad de navegación del código, aunque cada clase individual es simple (< 80 líneas).
- **Serialización JSON manual:** sin dependencia de Jackson/Gson, la serialización JSON se implementa con `StringBuilder`. Esto es frágil para objetos complejos, pero aceptable para la estructura simple de `ResultadoRiesgo`.
