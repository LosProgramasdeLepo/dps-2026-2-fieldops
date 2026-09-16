# Decisiones de diseño

## Aplicadas

**Strategy (`ActivityPolicy`).** Muestreo, medición y tránsito son ejemplos. Cada uno fija duración, riesgo y requisitos. `Activity` guarda id, ventana, zona y predecesores. Otro tipo es otra policy, no otra subclase de `Activity`.

**`Expedition` como agregado.** Sigue siendo la fachada: estados, asignaciones, permisos, warnings y seguimiento. El grafo ordenado de actividades vive en `Itinerary` (alta, baja, orden, dependencias, ciclos y ventanas). `Expedition` solo le aplica el estado y las reglas suyas (zona y período).

**Ids hacia el catálogo.** Personas, vehículos, instrumentos, consumibles y permisos viven afuera. La expedición guarda UUIDs. Si cambia la disponibilidad, el plan no se rearma.

**Catálogo de recursos (`ResourceCatalog`).** Una clase: alta, unicidad, lookup y listado. El validador, el sugeridor y el replanner la usan directa. Las certificaciones viven en la persona, no en el catálogo. El listado respeta el orden de alta: el sugeridor toma el primero que sirve.

**Persona como recurso temporal.** Se asigna a una actividad, no a la expedición entera. El enunciado no limita a una expedición a la vez. El choque es de ventanas. Ventanas adyacentes (fin = inicio) no se solapan: mañana y tarde es válido. `TimePeriod.overlaps` sigue esa regla.

**Permiso ≠ certificación.** `Permit` cubre zona y vigencia y se adjunta a la expedición. `Certification` es capacitación de una persona; las policies la piden por UUID. Vehículos e instrumentos no tienen certificación técnica. Si hace falta, se agrega entonces.

**Consumible aparte.** No tiene `Availability`. Su cupo es `stock`. Una vez asignado en expediciones que ocupan recursos, esa cantidad no se reusa. Persona, vehículo e instrumento se reusan si las ventanas no se solapan.

**Quién ocupa recursos.** `IN_REVIEW`, `APPROVED`, `IN_PROGRESS` y `SUSPENDED`. `DRAFT` no reserva. `FINISHED` libera personas y equipo; el stock del catálogo es el depósito actual, no se descuenta lo ya consumido.

**Validación fuera del agregado.** `approve` recibe `ValidationResult`; no lo calcula. `ExpeditionValidator` llama las reglas del enunciado, cada una con lo que usa: todas reciben expedición y catálogo; superposición y stock también las expediciones que ocupan. Otra regla es otra clase y una llamada más. Las demás expediciones entran como lista; se descarta el self y las que no ocupan. Un id ausente del catálogo es `RESOURCE`. Personal faltante (la activity pide certificación y no hay persona asignada) también es `RESOURCE`. `CERTIFICATION` solo si hay personas conocidas asignadas que no la tienen. Disponibilidad, stock, certificación (si no hay persona conocida que evaluar) y capacidad (si hay un vehículo desconocido) no se reportan otra vez para ese id.

**Severidad.** Superposición, disponibilidad de catálogo, stock, certificación, permiso y recurso faltante o desconocido son `CRITICAL`. Exceso de capacidad es `WARNING`: se puede justificar (otro viaje, trailer). Capacidad de una actividad = suma de los vehículos asignados a esa actividad; pasajeros = personas asignadas a la misma.

**Estados en el enum.** Las transiciones están en `Expedition`. El itinerario solo se toca en `DRAFT`. Asignaciones y permisos también en `IN_REVIEW`. Warnings solo en revisión, borrándolos `returnToDraft`. Incidentes y observaciones en estados activos. Start/finish de una actividad, solo en `IN_PROGRESS`. `startActivity` exige predecesores terminados. `finish` de la expedición exige todas las actividades cerradas. Se puede suspender desde `APPROVED` o `IN_PROGRESS`.

**Proponer asignaciones.** `AssignmentSuggester` no muta el plan: devuelve huecos (certificación, vehículo, instrumento) con el primer recurso del catálogo libre en la ventana. No choca con asignaciones propias ni con expediciones que ocupan. `addAssignment` realiza. Una sola heurística; no hay estrategia.

**Replanificación.** `Replanner` arma la alternativa sobre el agregado: cancelar actividad, correr ventanas (`delay`) y soltar asignaciones inválidas. Después rellena huecos con el sugeridor. Cancelar y atrasar tocan el itinerario: solo `DRAFT`. Reemplazar indisponibles también en `IN_REVIEW`. Si el plan ya está en revisión, `returnToDraft` y recién ahí se atrasan ventanas. `delay` corre la actividad y empuja dependientes (por el grafo, no por el orden de la lista) lo justo para que el predecesor termine antes; si alguna ventana quedaría fuera del período, no aplica nada.

**`OperationalReport`.** Se deriva del plan, no es un caso de uso. Resumen = estado y avance (planificadas / iniciadas / terminadas). Duración = suma de las estimadas (no el calendario, no paralelismo). Riesgo = el más alto. Consumo = lo que declaran las asignaciones. Resultados = los de las ejecuciones terminadas. `ActivityExecution` es inmutable: `finish` devuelve otra instancia; el agregado reemplaza la suya. `executions()` no deja terminar una actividad por fuera de `finishActivity`.

**Invariantes locales.** La ventana de tiempo no puede ser más corta que la duración de la policy. Los predecesores tienen que existir, no formar ciclos y terminar antes de que empiece la actividad (en el plan y al ejecutar). El orden del itinerario se cambia en `DRAFT` con `reorderActivities`. La zona de la actividad tiene que estar en la expedición. `TimePeriod` y `Quantity` se validan al construirse.

**Errores.** Transición ilegal (`InvalidExpeditionTransition`), no se puede aprobar (`ExpeditionNotApprovable`) y dato inválido (`IllegalArgumentException`).

## Descartadas

**Heredar `Activity`.** Cambia la regla, no el hecho de ser una actividad. Con Strategy no crece una jerarquía por tipo.

**Listas de vehículos/personas en `Expedition`.** Se asignan a una actividad, no a la expedición entera.

**Interfaz `Catalog` / `InMemoryCatalog`.** Un puerto con un implementador no se justifica hasta que haya persistencia.

**`Resource`.** El consumible no comparte disponibilidad temporal con el resto. El validador trata asignaciones temporales y stock por separado.

**State pattern.** Con el enum y `requireStatus` alcanza.

**Inyectar reglas / interfaz `ValidationRule`.** El conjunto lo fija el enunciado. Cuatro reglas miran solo el plan y el catálogo; dos también a las pares. Un contrato único obliga a parámetros de más o a overloads. El validador las llama; otra regla se agrega ahí.
