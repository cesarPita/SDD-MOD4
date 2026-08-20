---
inclusion: always
---

# Estandares de Seguridad — Spring Boot SDD

Este archivo define las reglas obligatorias de seguridad para todo codigo nuevo o modificado
en este proyecto. Kiro debe aplicarlas automaticamente en cada interaccion.

---

## 1. Principios generales

| Principio | Descripcion |
|-----------|-------------|
| Security by Design | La seguridad se incorpora desde el diseno, no como un agregado posterior |
| Least Privilege | Cada componente accede unicamente a lo que necesita |
| Defense in Depth | Multiples capas de proteccion — no depender de una sola |
| Fail Secure | Ante un error, el sistema falla de forma segura denegando acceso |
| Validacion de entradas | Toda entrada externa es no confiable hasta que se valide |
| Proteccion de informacion sensible | Los datos sensibles nunca se exponen ni se registran innecesariamente |
| Separacion de responsabilidades | Autenticacion y autorizacion son mecanismos separados |

No implementar soluciones de seguridad improvisadas ni criptografia propia.
Antes de agregar una dependencia de seguridad, verificar si Spring Boot ya la provee.

---

## 2. Estado actual del proyecto

> **IMPORTANTE**: El proyecto actualmente NO requiere JWT ni autenticacion.

La configuracion actual en `SecurityConfig` permite todas las peticiones (`anyRequest().permitAll()`).
Esta configuracion es intencional para la etapa actual del desarrollo.

**NO introducir automaticamente**:
- JWT
- OAuth2 / OIDC
- Keycloak / LDAP
- Logica de autenticacion
- Logica de autorizacion
- Filtros de seguridad adicionales

Estas funcionalidades se implementaran mediante un requerimiento especifico posterior.

---

## 3. Configuracion de Spring Security actual

```java
// SecurityConfig.java — estado actual
http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
    .csrf(csrf -> csrf.disable());
```

No modificar `SecurityConfig` sin un requerimiento explicito.
No agregar filtros de autenticacion sin un requerimiento explicito.

---

## 4. Autenticacion futura — reglas para cuando se implemente

Cuando se reciba el requerimiento de autenticacion:

- Usar estandares reconocidos (JWT con Spring Security, OAuth2/OIDC).
- No implementar criptografia propia — usar las librerias provistas por Spring Security.
- Contrasenas siempre cifradas con BCrypt (ya disponible via `BCryptPasswordEncoder` en `SecurityConfig`).
- Nunca almacenar contrasenas en texto plano, ni en logs, ni en BD sin hash.
- Nunca registrar tokens completos en logs.
- Nunca exponer secretos de firma en respuestas ni en logs.
- Integrar con el `CorrelationIdFilter` existente para trazabilidad completa.

---

## 5. JWT — reglas para cuando se implemente

Cuando se reciba el requerimiento de JWT:

| Regla | Detalle |
|-------|---------|
| Claves seguras | Minimo 256 bits para HMAC, par RSA/EC para asimetrico |
| Validar firma | Rechazar tokens con firma invalida |
| Validar expiracion | Rechazar tokens expirados |
| Validar issuer | Cuando el contexto lo requiera |
| Validar audience | Cuando el contexto lo requiera |
| No almacenar innecesariamente | No persistir JWT en BD salvo casos justificados |
| No registrar JWT | Nunca en logs, ni parcialmente |
| No exponer en respuestas | Solo devolver el token en el endpoint de login |

El `AuditContext.usuarioActual()` en `common/audit` esta preparado para resolver el usuario
desde `SecurityContextHolder` cuando se implemente JWT.

No implementar JWT hasta que sea solicitado explicitamente.

---

## 6. Autorizacion futura — reglas para cuando se implemente

- Separar completamente de la autenticacion.
- Aplicar minimo privilegio: cada endpoint requiere solo los permisos necesarios.
- Usar roles/permisos explicitos definidos en el dominio del negocio.
- Centralizar la logica de autorizacion — no duplicarla en multiples Controllers.
- Preferir anotaciones de Spring Security (`@PreAuthorize`, `@Secured`) sobre logica manual.
- Evitar verificaciones manuales de roles dentro de metodos de negocio en Services.

---

## 7. Seguridad de APIs

Todas las APIs deben cumplir independientemente del estado de autenticacion:

### Validacion de entrada
- Validar `@RequestBody` con `@Valid` y Bean Validation.
- Validar `@PathVariable` y `@RequestParam` con constraints apropiadas.
- Controlar tamaños maximos en campos de texto (`@Size`).
- No confiar en validaciones del cliente como unica capa.

### Respuestas seguras
- Usar `GlobalExceptionHandler` para todas las respuestas de error.
- Nunca devolver stack traces al cliente.
- Nunca exponer en respuestas:
  - Rutas internas del servidor
  - Credenciales o secretos
  - Configuracion de infraestructura
  - Consultas SQL
  - Nombres de clases o metodos internos
  - Mensajes de excepcion internos no controlados

### Codigos HTTP correctos
Ver tabla completa en `api-standards.md`. En contexto de seguridad:

| Situacion | Codigo |
|-----------|--------|
| No autenticado (futuro JWT) | 401 Unauthorized |
| Sin permisos (futuro JWT) | 403 Forbidden |
| Recurso no encontrado | 404 Not Found |
| Error interno | 500 (sin detalle interno) |

---

## 8. Proteccion de informacion sensible

### Nunca registrar en logs
- Contrasenas (en texto plano ni cifradas)
- Tokens JWT completos
- Refresh tokens
- Headers `Authorization`
- API keys
- Claves privadas o secretos de firma
- Numeros de tarjeta u otra informacion financiera
- Datos medicos sensibles

### Nunca devolver en respuestas API
- El campo `password` esta marcado como `@JsonProperty(WRITE_ONLY)` — mantener esta configuracion.
- Tokens de sesion o JWT no deben aparecer en responses de consulta de recursos.
- Datos de infraestructura o configuracion interna.

### En el codigo
- Nunca hardcodear secretos, contrasenas ni API keys en codigo fuente.
- Nunca commitear archivos `.env` con valores reales.
- Usar variables de entorno o secret managers para credenciales de servicios.

---

## 9. Base de datos y SQL

- Usar JPA con parametros — nunca concatenar strings para construir consultas.
- Los metodos derivados de Spring Data y `@Query` con parametros nombrados son seguros.
- No construir JPQL ni SQL nativo usando datos directamente del usuario.

```java
// CORRECTO — parametro nombrado
@Query("SELECT u FROM Usuario u WHERE u.email = :email")
Optional<Usuario> findByEmail(@Param("email") String email);

// CORRECTO — metodo derivado
boolean existsByUsername(String username);

// PROHIBIDO — concatenacion con dato del usuario
@Query("SELECT u FROM Usuario u WHERE u.email = '" + email + "'")
```

---

## 10. CORS

No configurar `Access-Control-Allow-Origin: *` en ambientes productivos sin justificacion.

Cuando se configure CORS:
- Definir origenes permitidos de forma explicita.
- Hacerlos configurables via `application.properties` o variables de entorno.
- No hardcodear URLs de origenes en codigo fuente.

---

## 11. Secrets y configuracion

| Donde va | Que contiene |
|----------|-------------|
| `application.properties` (versionado) | Configuracion no sensible, referencias a variables de entorno |
| Variables de entorno | Credenciales, passwords de BD, secretos JWT, API keys |
| Secret manager (produccion) | Todos los secretos productivos |
| Codigo fuente | NUNCA secretos ni credenciales |

El `application.properties` actual contiene credenciales de BD para desarrollo local.
En produccion, reemplazar con variables de entorno:
```properties
spring.datasource.password=${DB_PASSWORD}
```

---

## 12. Dependencias de seguridad

Antes de agregar cualquier dependencia relacionada con seguridad:

1. Verificar si Spring Security ya provee la funcionalidad.
2. Verificar vulnerabilidades conocidas (CVE) en la version a usar.
3. Verificar compatibilidad con Spring Boot 4.1.0.
4. No usar versiones con vulnerabilidades conocidas sin parche disponible.
5. No introducir librerias criptograficas propias o de terceros no auditados.

Dependencia de seguridad actual:
- `spring-boot-starter-security` — ya declarada. Provee BCrypt, SecurityFilterChain y base para JWT futuro.

---

## 13. Errores — respuesta al cliente vs logs

| Informacion | Cliente | Logs servidor |
|-------------|---------|---------------|
| Mensaje controlado de negocio | Si (via `ApiError.mensaje`) | Si (WARN) |
| Stack trace | NUNCA | Si (ERROR con excepcion) |
| Nombre de clase interna | NUNCA | Si en DEBUG |
| Consulta SQL fallida | NUNCA | Si en ERROR |
| Credenciales involucradas | NUNCA | NUNCA |
| Mensaje de excepcion de BD | NUNCA | Si en ERROR |

El `GlobalExceptionHandler` ya implementa esta separacion correctamente.
No bypassear ni duplicar este mecanismo.

---

## 14. Logging de seguridad

Cumplir siempre con las reglas de `logging.md`. En contexto de seguridad:

- Nunca registrar: `Authorization`, JWT, password, secret, private key, refresh token.
- Registrar eventos relevantes de seguridad con nivel apropiado:
  - Intento de acceso no autorizado (futuro) → WARN
  - Token invalido o expirado (futuro) → WARN
  - Error critico de seguridad → ERROR
- Incluir `correlationId` (ya disponible via MDC) en todos los logs de seguridad.

---

## 15. Preparacion para seguridad futura

La arquitectura esta preparada para integrar:

| Tecnologia | Punto de integracion preparado |
|------------|-------------------------------|
| Spring Security JWT | `SecurityConfig` — agregar filtro JWT |
| OAuth2 / OIDC | `SecurityConfig` — agregar `oauth2ResourceServer` |
| Keycloak | Como proveedor OAuth2/OIDC |
| Auditoria de seguridad | `AuditContext.usuarioActual()` en `common/audit` |
| Trazabilidad | `CorrelationIdFilter` ya en MDC |

No implementar ninguna de estas tecnologias hasta que sea solicitado explicitamente.

---

## 16. Regla principal — obligatoria

Ante cualquier requerimiento que implique seguridad:

1. Analizar la arquitectura de seguridad existente en `SecurityConfig`.
2. Identificar mecanismos ya implementados: BCrypt, filtros, GlobalExceptionHandler.
3. Reutilizar componentes existentes antes de crear nuevos.
4. No duplicar mecanismos de seguridad.
5. No introducir dependencias de seguridad sin justificacion explicita.
6. No implementar criptografia propia.
7. No exponer informacion sensible en respuestas ni en logs.
8. No modificar `SecurityConfig` sin un requerimiento explicito.

Estas reglas se aplican automaticamente a todo codigo nuevo o modificado en el proyecto.