# Decisiones de diseño

## Aplicadas

**Strategy.** Vive en `ActivityPolicy`. `SamplingPolicy`, `MeasurementPolicy` y `TransitPolicy` fijan duración, riesgo y requisitos. `Activity` guarda id, nombre, policy, ventana, zona y predecesores. Un tipo nuevo es otro record y un valor más en `permits`.

**Assignment sellado.** Mismo corte en `Assignment` con persona, vehículo, instrumento y consumible. Solo `ConsumableAssignment` declara consumo. Un tipo nuevo es otro record y un valor más en `permits`.

**Agregado.** `Expedition` concentra ciclo de vida, asignaciones, permisos, warnings y seguimiento. `Itinerary` concentra el grafo. `Expedition` le aplica estado, zona y período. `Itinerary` es público porque vive en otro paquete. Validador, sugeridor, replanner e informe son clases aparte.

**Identidad por UUID.** Personas, vehículos, instrumentos, consumibles y permisos viven en el catálogo. La expedición guarda ids.

**ResourceCatalog concreto.** Alta, unicidad y lookup. Lista personas, vehículos e instrumentos. Consumibles y permisos se buscan por id. Validador, sugeridor y replanner reciben esta clase.

**Persona por actividad.** La asignación es a una actividad. El solape de ventanas se valida entre actividades y entre expediciones que ocupan.

**Permiso y certificación.** `Permit` cubre zona y vigencia, y se adjunta a la expedición. `Certification` vive en la persona. `SamplingPolicy` y `MeasurementPolicy` la piden por UUID. Vehículos e instrumentos se habilitan por disponibilidad.

**Consumible por stock.** La cantidad asignada en la expedición validada y en las que ocupan se compara con el depósito del catálogo. Persona, vehículo e instrumento se reusan con ventanas disjuntas.

**Reserva.** `IN_REVIEW`, `APPROVED`, `IN_PROGRESS` y `SUSPENDED` ocupan. `DRAFT` y `FINISHED` dejan personas y equipo libres. El stock del catálogo es el depósito actual. Las terminadas quedan fuera del compromiso de stock.

**Validación fuera del agregado.** `approve` recibe un `ValidationResult` ya calculado. `ExpeditionValidator` llama seis reglas. Cuatro reciben expedición y catálogo. `TemporalOverlapRule` y `StockRule` también reciben las que ocupan, usando `occupyingPeers`. Otra regla es otra clase y una llamada más.

**Códigos.** `RESOURCE` cubre id ausente, permiso adjunto desconocido, y actividad cuya policy pide persona, vehículo o instrumento con esa asignación vacía. `CERTIFICATION` sale si hay personas conocidas asignadas y todas omiten la certificación pedida. `PERMIT` sale por cada actividad descubierta frente a los permisos conocidos. Lista de adjuntos vacía cuenta como todas descubiertas. Si los adjuntos son todos desconocidos sale solo `RESOURCE`. Un id ya cubierto por `RESOURCE` omite disponibilidad, stock, certificación y capacidad.

**Severidad.** `OVERLAP`, `AVAILABILITY`, `STOCK`, `CERTIFICATION`, `PERMIT` y `RESOURCE` son `CRITICAL`. `CAPACITY` es `WARNING`. La capacidad de una actividad suma los vehículos asignados a esa actividad. Los pasajeros son las personas asignadas a la misma.

**Estados.** Las transiciones viven en `Expedition`. El itinerario se edita en `DRAFT`. Asignaciones y permisos también en `IN_REVIEW`. Warnings en revisión. `returnToDraft` vale desde `IN_REVIEW`, `APPROVED`, `IN_PROGRESS` y `SUSPENDED`, y en los dos activos borra ejecuciones. `FINISHED` es terminal. Incidentes y observaciones en `IN_PROGRESS` y `SUSPENDED`. Inicio y cierre de actividad en `IN_PROGRESS`, con predecesores ya terminados. `finish` de la expedición exige todas las actividades cerradas. Se puede suspender desde `APPROVED` o `IN_PROGRESS`.

**AssignmentSuggester.** Devuelve huecos de certificación, vehículo e instrumento. Toma el primero del catálogo disponible en la ventana y libre respecto de asignaciones propias y de las que ocupan. `addAssignment` aplica.

**Replanner.** Cancela, atrasa o reemplaza indisponibles sobre el agregado y después rellena con el sugeridor. Cancelar y atrasar tocan el itinerario, así que si el estado es otro llaman a `returnToDraft`. `replaceUnavailable` corre en `DRAFT` o `IN_REVIEW` y deja el estado. Suelta persona, vehículo o instrumento inválido por catálogo o solape. El consumible asignado queda. Cancelar un predecesor suelta esa dependencia y deja las actividades dependientes. `delay` corre la actividad y empuja dependientes según el grafo. Si alguna ventana excedería el período, se rechaza y las ventanas quedan iguales.

**OperationalReport.** Se deriva del plan. El resumen trae estado, planificadas, iniciadas y terminadas. La duración es la suma de estimadas. El riesgo es el máximo de las actividades, `LOW` con itinerario vacío. El consumo es lo que declaran las asignaciones. Los resultados salen de las ejecuciones terminadas.

**ActivityExecution inmutable.** `finish` devuelve otra instancia. El agregado la reemplaza en `finishActivity`. Una copia de `executions()` deja el plan igual.

**TemporalBooking.** Unifica persona, vehículo e instrumento en una ventana. Lo usan `TemporalOverlapRule`, `AssignmentSuggester` y `Replanner`.

**Servicios estáticos.** `ExpeditionValidator`, `AssignmentSuggester` y `Replanner` se invocan por métodos estáticos. Reciben expedición, catálogo y pares.

**Invariantes.** La ventana alcanza la duración de la policy. Los predecesores existen, forman un acíclico y terminan antes del inicio, en el plan y al ejecutar. El orden se cambia en `DRAFT` con `reorderActivities`. La zona de la actividad está en la expedición. `TimePeriod`, `Quantity` y `WorkZone` se validan al construirse.

**Errores.** `InvalidExpeditionTransition` cubre transición ilegal. `ExpeditionNotApprovable` cubre aprobación bloqueada. `InvalidActivityExecution` cubre terminar dos veces. `IllegalArgumentException` cubre dato inválido.

## Descartadas

**Heredar Activity.** El tipo cambia la regla. La consecuencia es un record nuevo y ampliar `permits`.

**Listas de flota en Expedition.** La asignación es por actividad. Capacidad, certificación y solape se evalúan por ventana.

**Puerto Catalog.** Un implementador espera persistencia. Validador, sugeridor y replanner dependen de `ResourceCatalog`.

**Supertipo Resource.** El consumible se rige por stock. El resto, por disponibilidad. El validador tiene `TemporalOverlapRule` y `StockRule` por separado.

**State pattern.** El enum y `requireStatus` cubren las transiciones. Un estado nuevo es un valor y una guarda en `Expedition`.

**Interfaz ValidationRule.** Cuatro reglas reciben expedición y catálogo. Dos también las que ocupan. Un contrato único pediría parámetros de más o sobrecargas. Una regla nueva se suma como clase y como llamada en `ExpeditionValidator`.
