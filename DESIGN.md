# Decisiones de diseño

## Aplicadas

**Strategy (`ActivityPolicy`).** Muestreo, medición y tránsito son ejemplos. Cada uno fija duración, riesgo y requisitos. `Activity` guarda id, ventana, zona y predecesores. Otro tipo es otra policy, no otra subclase de `Activity`.

**`Expedition` como agregado.** Sigue siendo la fachada: estados, asignaciones, permisos, warnings y seguimiento. El grafo ordenado de actividades vive en `Itinerary` (alta, baja, orden, dependencias, ciclos y ventanas). `Expedition` solo le aplica el estado y las reglas suyas (zona y período).

**Ids hacia el catálogo.** Personas, vehículos, instrumentos, consumibles y permisos viven afuera. La expedición guarda UUIDs. Si cambia la disponibilidad, el plan no se rearma.

**Estados en el enum.** Las transiciones están en `Expedition`. El itinerario solo se toca en `DRAFT`. Asignaciones y permisos también en `IN_REVIEW`. Warnings solo en revisión, borrándolos `returnToDraft`. Incidentes y observaciones en estados activos. Start/finish de una actividad, solo en `IN_PROGRESS`. `startActivity` exige predecesores terminados. `finish` de la expedición exige todas las actividades cerradas. Se puede suspender desde `APPROVED` o `IN_PROGRESS`.

**`OperationalReport`.** Se deriva del plan, no es un caso de uso. Resumen = estado y avance (planificadas / iniciadas / terminadas). Duración = suma de las estimadas (no el calendario, no paralelismo). Riesgo = el más alto. Consumo = lo que declaran las asignaciones. Resultados = los de las ejecuciones terminadas, copiados para no exponer `ActivityExecution` mutable.

**Invariantes locales.** La ventana de tiempo no puede ser más corta que la duración de la policy. Los predecesores tienen que existir, no formar ciclos y terminar antes de que empiece la actividad (en el plan y al ejecutar). El orden del itinerario se cambia en `DRAFT` con `reorderActivities`. La zona de la actividad tiene que estar en la expedición. `TimePeriod` y `Quantity` se validan al construirse.

**Errores.** Transición ilegal (`InvalidExpeditionTransition`), no se puede aprobar (`ExpeditionNotApprovable`) y dato inválido (`IllegalArgumentException`).

## Descartadas

**Heredar `Activity`.** Cambia la regla, no el hecho de ser una actividad. Con Strategy no crece una jerarquía por tipo.

**Listas de vehículos/personas en `Expedition`.** Se asignan a una actividad, no a la expedición entera.

## No aplicadas

**State pattern.** Con el enum y `requireStatus` alcanza. Una clase por estado no cambiaba las reglas de arriba.
