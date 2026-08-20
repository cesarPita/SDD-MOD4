---
inclusion: always
---

# Estandares Tecnicos Obligatorios — Spring Boot SDD

Este archivo establece las reglas tecnicas obligatorias para todo codigo generado o modificado
en este proyecto. Kiro debe aplicarlas automaticamente en cada interaccion.

---

## 1. Logging

### Prohibido
- `System.out.println()`
- `System.err.println()`
- `e.printStackTrace()`
- Cualquier mecanismo de salida estandar que no sea SLF4J

### Obligatorio
- Todo logging mediante **SLF4J** (`org.slf4j.Logger` / `LoggerFactory`)
- Declaracion del logger como campo estatico privado final:

```java
private static final Logger log = LoggerFactory.getLogger(MiClase.class);
```

### Niveles correctos
| Nivel   | Cuando usarlo |
|---------|--------------|
| `ERROR` | Excepciones no controladas, fallos de integracion, errores de BD |
| `WARN`  | Situaciones anormales recuperables, recursos no encontrados, duplicados |
| `INFO`  | Operaciones importantes de negocio: creacion, actualizacion, eliminacion |
| `DEBUG` | Informacion de diagnostico tecnico, detalles internos |

### Placeholders — obligatorio
```java
// CORRECTO
log.info("Usuario creado correctamente id={}", id);
log.warn("Recurso no encontrado uri={} mensaje={}", uri, mensaje);

// PROHIBIDO
log.info("Usuario creado correctamente id=" + id);
```

### Informacion sensible — NUNCA registrar
- Passwords / contrasenas
- Tokens JWT
- Headers `Authorization`
- Secretos o claves de API
- Informacion personal identificable innecesaria

### Antes de agregar logs
Verificar si ya existe `CorrelationIdFilter`, `HttpLoggingFilter` u otro mecanismo centralizado
que ya registre la informacion requerida. No duplicar logs existentes.

---

## 2. Swagger / OpenAPI

Todos los endpoints REST nuevos o modificados deben documentarse con Swagger/OpenAPI.

### Anotaciones requeridas
- `@Tag` — en la clase controller (agrupa endpoints)
- `@Operation` — en cada metodo (describe el proposito)
- `@ApiResponse` / `@ApiResponses` — para cada codigo HTTP posible
- `@Parameter` — para parametros de path, query y header
- `@Schema` — en DTOs para describir campos

### La documentacion debe incluir como minimo
- Proposito del endpoint
- Parametros (path, query, body)
- Request body con ejemplo
- Respuestas HTTP exitosas y de error
- Codigos de error relevantes (400, 404, 409, 500)
- Modelos utilizados

### Evitar
- Documentacion redundante o que repita lo que ya es obvio
- Anotar endpoints que no son parte de la API publica
- Duplicar descripciones entre `@Operation` y Javadoc

---

## 3. Controllers

### Responsabilidades permitidas
- Exponer endpoints REST
- Validar entrada con `@Valid`
- Delegar toda logica al Service correspondiente
- Construir respuestas HTTP apropiadas (`ResponseEntity`)
- Logging de entrada/salida de operaciones (INFO)

### Prohibido en Controllers
- Logica de negocio compleja
- Acceso directo a Repositories
- Bloques `try/catch` para logica de negocio (usar `GlobalExceptionHandler`)
- `System.out.println()`

### Respuestas HTTP
| Operacion       | Codigo esperado |
|-----------------|----------------|
| POST (crear)    | 201 Created + Location header |
| GET             | 200 OK |
| PUT (actualizar)| 200 OK |
| DELETE logico   | 204 No Content |
| No encontrado   | 404 Not Found  |
| Duplicado       | 409 Conflict   |
| Validacion      | 400 Bad Request |

---

## 4. Services

### Responsabilidades
- Contener toda la logica de negocio
- Orquestar llamadas a Repositories
- Lanzar excepciones del dominio (`RecursoNoEncontradoException`, `DuplicadoException`)
- Logging de operaciones importantes con nivel INFO/WARN

### Prohibido en Services
- `System.out.println()`
- Devolver entidades JPA directamente (usar DTOs/mappers)
- Logica de presentacion o construccion de respuestas HTTP

---

## 5. Repositories

### Responsabilidades
- Acceso a datos unicamente
- Consultas derivadas de Spring Data o `@Query`
- Uso de `JpaSpecificationExecutor` para filtros dinamicos

### Prohibido en Repositories
- Logica de negocio
- Logging excesivo (solo errores tecnicos relevantes)
- Transformaciones de datos o mapeos

---

## 6. Excepciones

### Reglas
- Usar `GlobalExceptionHandler` existente para manejar excepciones globalmente
- Lanzar `RecursoNoEncontradoException` para recursos no encontrados (→ 404)
- Lanzar `DuplicadoException` para conflictos de unicidad (→ 409)
- No exponer stack traces al cliente
- El stack trace se registra **solo** en servidor con `log.error(..., ex)`

### Prohibido
- Bloques `try/catch` para absorber excepciones silenciosamente
- Crear nuevas clases de excepcion sin justificacion
- Devolver mensajes de error con informacion interna del sistema

---

## 7. Calidad del codigo

Antes de generar cualquier codigo nuevo, seguir este orden:

1. **Revisar** las implementaciones existentes en el proyecto
2. **Reutilizar** componentes, excepciones, mappers y configuraciones ya existentes
3. **Evitar duplicacion** — si algo ya existe, no recrearlo
4. **Mantener la arquitectura** en capas: Controller → Service → Repository
5. **No introducir dependencias** sin justificacion explicita
6. **No modificar** codigo no relacionado con el requerimiento actual
7. **Mantener compatibilidad** con Spring Boot 4.1.0 y Java 25 (GraalVM)

### Estructura de paquetes actual
```
com.sdd.sdd
├── common
│   ├── audit        (AuditContext — preparado para JWT)
│   ├── dto          (ApiError, PageResponse)
│   ├── exception    (GlobalExceptionHandler, RecursoNoEncontradoException, DuplicadoException)
│   └── logging      (CorrelationIdFilter, HttpLoggingFilter)
├── config           (JpaAuditingConfig, SecurityConfig)
└── usuario
    ├── controller   (UsuarioController)
    ├── dto          (UsuarioRequest, UsuarioResponse, UsuarioUpdateRequest)
    ├── entity       (Usuario, EstadoUsuario)
    ├── mapper       (UsuarioMapper)
    ├── repository   (UsuarioRepository)
    └── service      (UsuarioService, UsuarioServiceImpl)
```

---

## 8. Seguridad

- Spring Security esta configurado en `SecurityConfig` — no modificar sin justificacion
- JWT se implementara en una etapa posterior — no anticipar su implementacion
- No registrar headers de autenticacion en logs
- Passwords siempre cifrados con BCrypt antes de persistir

---

## Resumen de prohibiciones absolutas

| Prohibido | Alternativa |
|-----------|-------------|
| `System.out.println()` | `log.info()` / `log.debug()` |
| `System.err.println()` | `log.error()` |
| `e.printStackTrace()` | `log.error("mensaje", e)` |
| Concatenar strings en logs | Placeholders `{}` de SLF4J |
| Registrar passwords o tokens | No registrar informacion sensible |
| Logica de negocio en Controller | Delegar al Service |
| Acceso a BD desde Controller | Usar Repository solo desde Service |
| Exponer stack trace al cliente | Usar `GlobalExceptionHandler` |