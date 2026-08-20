---
inclusion: always
---

# Logging y Trazabilidad — Spring Boot SDD

Este archivo define las reglas obligatorias de logging y trazabilidad para todo codigo
nuevo o modificado en este proyecto. Kiro debe aplicarlas automaticamente en cada interaccion.

---

## 1. Framework de logging

| Componente | Tecnologia |
|------------|-----------|
| API        | SLF4J (`org.slf4j.Logger`) |
| Implementacion | Logback (incluido por Spring Boot — no agregar otra implementacion) |
| Configuracion | `logback-spring.xml` en `src/main/resources` |
| Niveles runtime | Controlados desde `application.properties` sin cambiar codigo |

No introducir Log4j, JUL ni ninguna otra implementacion de logging.

---

## 2. Declaracion del logger

Siempre declarar el logger como campo estatico privado final:

```java
private static final Logger log = LoggerFactory.getLogger(MiClase.class);
```

No usar `@Slf4j` de Lombok en este proyecto — se usa la declaracion explicita para mayor claridad.

---

## 3. Prohibicion absoluta de salida estandar

Esta ESTRICTAMENTE PROHIBIDO usar:

```java
System.out.println()
System.out.print()
System.err.println()
System.err.print()
e.printStackTrace()
```

Alternativas correctas:

| Prohibido | Correcto |
|-----------|---------|
| `System.out.println("mensaje")` | `log.info("mensaje")` |
| `System.err.println("error")` | `log.error("error")` |
| `e.printStackTrace()` | `log.error("Descripcion del error", e)` |

---

## 4. Placeholders — obligatorio

Siempre usar placeholders de SLF4J. Nunca concatenar strings.

```java
// CORRECTO
log.info("Usuario registrado id={} username={}", id, username);
log.warn("Recurso no encontrado uri={} mensaje={}", uri, mensaje);
log.error("Error interno uri={} tipo={}", uri, ex.getClass().getName(), ex);

// PROHIBIDO
log.info("Usuario registrado id=" + id + " username=" + username);
log.info("Usuario registrado: " + usuario.toString());
```

---

## 5. Niveles de logging

### ERROR
Usar para:
- Excepciones no controladas o inesperadas
- Errores de infraestructura (BD, red, integraciones)
- Fallos que requieren atencion inmediata
- Stack trace incluido como tercer argumento: `log.error("msg uri={}", uri, ex)`

### WARN
Usar para:
- Situaciones anormales recuperables
- Intentos de registro con datos duplicados
- Recursos no encontrados (cuando sea relevante a nivel de servicio)
- Comportamientos inesperados que no detienen el proceso

### INFO
Usar para:
- Eventos importantes de negocio: creacion, actualizacion, eliminacion
- Inicio y finalizacion de operaciones relevantes
- Informacion general del flujo de la aplicacion

### DEBUG
Usar para:
- Diagnostico tecnico detallado
- Valores intermedios utiles durante desarrollo
- Consultas y parametros internos

No generar DEBUG excesivo — en produccion el nivel es INFO por defecto.

---

## 6. Correlation ID

### Implementacion actual
El proyecto ya tiene implementado `CorrelationIdFilter` en `common/logging`.

Comportamiento:
- Si la peticion incluye `X-Correlation-ID`: se reutiliza.
- Si no existe: se genera un UUID nuevo.
- Se almacena en MDC con clave `correlationId`.
- Se incluye en todos los logs de la peticion via patron Logback `%X{correlationId:--}`.
- Se devuelve en la respuesta HTTP como header `X-Correlation-ID`.
- Se limpia del MDC al finalizar la peticion (bloque `finally`).

### Regla
No reimplementar este mecanismo. Reutilizar el filtro existente.
El correlationId aparece automaticamente en todos los logs via MDC — no es necesario incluirlo
manualmente en cada mensaje de log.

---

## 7. HTTP Logging centralizado

### Implementacion actual
El proyecto ya tiene implementado `HttpLoggingFilter` en `common/logging`.

Registra automaticamente por cada peticion:
- Inicio: `HTTP {metodo} {uri} - iniciando`
- Fin: `HTTP {metodo} {uri} - completado status={status} duration={ms}ms`

Ejemplo de salida:
```
INFO [abc-1234] [http-nio] HttpLoggingFilter : HTTP POST /api/usuarios - iniciando
INFO [abc-1234] [http-nio] HttpLoggingFilter : HTTP POST /api/usuarios - completado status=201 duration=87ms
```

### Regla
No duplicar este logging en Controllers ni Services.
No registrar bodies completos de requests/responses — pueden contener informacion sensible.

---

## 8. Logging por capa

### Controller
Registrar eventos de negocio relevantes con nivel INFO:
- Inicio de operacion con parametros seguros (nunca password, nunca token).
- Resultado exitoso con identificadores del recurso creado/modificado.

```java
log.info("Registrando usuario username={}", request.getUsername());
log.info("Usuario registrado exitosamente id={} username={}", response.getId(), response.getUsername());
```

No duplicar lo que ya registra `HttpLoggingFilter`.

### Service
Registrar operaciones importantes de negocio:
- Validaciones que detectan conflictos (WARN).
- Operaciones de creacion, actualizacion, eliminacion exitosas (INFO).
- Busquedas con resultado negativo relevantes (WARN o DEBUG segun el caso).

```java
log.warn("Intento de registro con username duplicado: username={}", username);
log.info("Usuario creado correctamente id={} username={}", guardado.getId(), guardado.getUsername());
```

### Repository
No agregar logging en Repositories salvo errores tecnicos relevantes.
Spring Data JPA genera sus propios logs de SQL cuando se configura `org.hibernate.SQL=DEBUG`.

### GlobalExceptionHandler
Cada handler debe registrar el evento con el nivel apropiado:
- `RecursoNoEncontradoException` → `log.warn(...)`
- `DuplicadoException` → `log.warn(...)`
- Validacion → `log.warn(...)`
- `Exception` generica → `log.error("...", ex)` con stack trace completo

El stack trace NUNCA se expone al cliente — solo se registra en servidor.

---

## 9. Informacion sensible — NUNCA registrar

| Tipo de dato | Accion |
|-------------|--------|
| Passwords / contrasenas | Nunca registrar, ni enmascarados |
| Tokens JWT | Nunca registrar |
| Refresh tokens | Nunca registrar |
| Header `Authorization` | Nunca registrar |
| Secretos / claves API | Nunca registrar |
| Claves privadas | Nunca registrar |
| Numeros de tarjeta | Nunca registrar |
| Datos medicos sensibles | Nunca registrar |

Si se requiere identificar un dato sensible en logs, usar enmascaramiento:
`log.debug("Procesando usuario email={}***", email.substring(0, 3))`

---

## 10. Configuracion de archivos de log

### Estructura actual (logback-spring.xml)
```
logs/
 ├── application.log        (INFO+ — rotacion diaria / 50MB / 30 dias)
 ├── application-error.log  (solo ERROR — rotacion diaria / 20MB / 30 dias)
 └── audit.log              (canal AUDIT — preparado para auditoria futura)
```

### Politica de rotacion configurada
- `SizeAndTimeBasedRollingPolicy`: rotacion diaria y por tamaño maximo.
- Archivos comprimidos en `.log.gz` al rotar.
- Limites de retencion configurados para evitar crecimiento ilimitado.

No modificar esta configuracion sin justificacion. Si se necesitan nuevos destinos de log,
agregarlos como appenders adicionales en `logback-spring.xml`.

---

## 11. Auditoria

### Estado actual
El canal de auditoria esta preparado pero no activo completamente.

- Logger `AUDIT` declarado en `logback-spring.xml` — escribe en `audit.log`.
- `AuditContext.java` en `common/audit` — placeholder para usuario autenticado.

### Cuando se implemente JWT
Actualizar `AuditContext.usuarioActual()` para resolver desde `SecurityContextHolder`.
Los eventos de auditoria deben incluir:
- Usuario que realizo la operacion
- Operacion realizada
- Recurso afectado (tipo + id)
- Fecha/hora
- Resultado
- correlationId (ya disponible via MDC)

No implementar auditoria completa hasta que exista autenticacion JWT.

---

## 12. Preparacion para observabilidad futura

La arquitectura de logging esta preparada para integracion futura con:

| Plataforma | Punto de integracion |
|------------|---------------------|
| OpenTelemetry | `correlationId` en MDC es el trace ID natural |
| Jaeger / Zipkin | Compatible con MDC propagation |
| ELK / OpenSearch | Formato de archivo plano — agregar encoder JSON cuando se requiera |
| Grafana Loki | Compatible con archivos rotados |
| Dynatrace | Agent-based — no requiere cambios de codigo |

Para habilitar formato JSON en logs de archivo (requerido por ELK/Loki), agregar en `logback-spring.xml`:
```xml
<!-- TODO (observabilidad): reemplazar PatternLayoutEncoder por LogstashEncoder -->
```

No agregar estas plataformas hasta que sean requeridas.

---

## 13. Regla principal — obligatoria

Antes de agregar cualquier log nuevo:

1. **Verificar** si `HttpLoggingFilter` o `CorrelationIdFilter` ya registran esa informacion.
2. **Determinar** el nivel correcto segun la tabla de niveles de este documento.
3. **Verificar** que el mensaje no contiene informacion sensible.
4. **Usar** placeholders `{}` — nunca concatenacion de strings.
5. **Considerar** si el correlationId ya aparece via MDC (si es asi, no incluirlo manualmente).

Estas reglas son obligatorias para todo codigo generado o modificado en el proyecto.