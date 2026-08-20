Nombre: Cesar Enrique Pita Perez
Tecnologia: Spring Boot
Api-Doc: http://localhost:8080/v3/api-docs
Swagger: http://localhost:8080/swagger-ui/index.html
A) Prompt Utilizado:

Quiero implementar un módulo completo de Gestión de Usuarios utilizando Spec-Driven Development (SDD).

IMPORTANTE:
- Primero analiza la estructura actual del proyecto antes de realizar cambios.
- No generes código inmediatamente.
- Crea primero la especificación de la funcionalidad.
- Sigue el flujo:
  1. Requirements
  2. Design
  3. Tasks
  4. Implementation
  5. Tests
- No modifiques archivos que no sean necesarios para esta funcionalidad.
- Respeta la arquitectura y convenciones existentes del proyecto.

CONTEXTO DEL PROYECTO:
- Java 25
- Spring Boot 4.1.0
- Maven
- Spring Web MVC
- Spring Data JPA
- Hibernate
- PostgreSQL
- Lombok

FUNCIONALIDAD:
Crear un módulo de Gestión de Usuarios que permita:

1. REGISTRO DE USUARIOS
   - Registrar un nuevo usuario.
   - Campos mínimos:
     - id
     - nombres
     - apellidos
     - username
     - email
     - password
     - estado
     - fechaCreacion
     - fechaModificacion
   - El username debe ser único.
   - El email debe ser único.
   - La contraseña nunca debe almacenarse en texto plano.
   - Validar los campos obligatorios.
   - Validar formato de email.
   - No permitir usernames duplicados.
   - No permitir emails duplicados.

2. CONSULTA DE USUARIOS
   - Obtener un usuario por ID.
   - Obtener un usuario por username.
   - Listar usuarios.
   - Implementar paginación.
   - Permitir filtrar por username, email y estado.

3. EDICIÓN DE USUARIOS
   - Modificar nombres.
   - Modificar apellidos.
   - Modificar email.
   - Modificar estado.
   - No permitir duplicidad de username o email.
   - Registrar fecha de modificación.

4. ELIMINACIÓN DE USUARIOS
   - Implementar eliminación lógica.
   - No eliminar físicamente el registro de la base de datos.
   - Utilizar el campo estado para controlar si el usuario está activo o inactivo.
   - Registrar la fecha de modificación.

5. BASE DE DATOS
   - Utilizar PostgreSQL.
   - Crear las entidades JPA necesarias.
   - Crear las tablas mediante un mecanismo de migración de base de datos.
   - Preferentemente utilizar Flyway para controlar las migraciones.
   - No depender de spring.jpa.hibernate.ddl-auto=create para generar la estructura en ambientes reales.
   - Crear índices y restricciones para username y email.
   - Definir correctamente las claves primarias y restricciones UNIQUE.

6. API REST
   Crear endpoints REST siguiendo convenciones HTTP:

   POST   /api/usuarios
   GET    /api/usuarios
   GET    /api/usuarios/{id}
   PUT    /api/usuarios/{id}
   DELETE /api/usuarios/{id}

   Si se considera necesario, crear endpoints adicionales para búsquedas específicas.

7. RESPUESTAS
   - Utilizar DTOs para Request y Response.
   - No devolver la contraseña en ninguna respuesta.
   - Utilizar códigos HTTP apropiados:
     - 201 para creación.
     - 200 para consultas y actualización.
     - 204 cuando corresponda.
     - 400 para errores de validación.
     - 404 cuando el usuario no exista.
     - 409 para username/email duplicado.
   - Definir una estructura uniforme para las respuestas y errores.

8. ARQUITECTURA
   Utilizar una arquitectura por capas:

   Controller
       ↓
   Service
       ↓
   Repository
       ↓
   Entity
       ↓
   PostgreSQL

   Crear como mínimo:
   - UsuarioController
   - UsuarioService
   - UsuarioRepository
   - Usuario
   - UsuarioRequest
   - UsuarioResponse
   - DTOs necesarios
   - Manejo global de excepciones

9. SEGURIDAD
   - Utilizar BCrypt o el mecanismo recomendado actualmente por Spring Security para almacenar contraseñas.
   - Nunca registrar passwords en logs.
   - Nunca devolver passwords en las respuestas.
   - Validar y sanitizar los datos de entrada.
   - No almacenar credenciales directamente en el código fuente.

10. AUDITORÍA
   Registrar:
   - fecha de creación
   - fecha de modificación
   - usuario que creó el registro, si la arquitectura actual lo permite
   - usuario que modificó el registro, si la arquitectura actual lo permite.

11. PRUEBAS
   Crear pruebas unitarias para Service.
   Crear pruebas de Repository cuando corresponda.
   Crear pruebas de integración para los endpoints REST.
   Probar como mínimo:
   - registro exitoso
   - username duplicado
   - email duplicado
   - datos inválidos
   - consulta existente
   - consulta inexistente
   - modificación exitosa
   - modificación inexistente
   - eliminación lógica
   - paginación
   - filtros

12. CALIDAD
   Antes de implementar:
   - analiza las dependencias actuales del pom.xml;
   - verifica la configuración actual de PostgreSQL;
   - revisa si ya existe una estrategia de migraciones;
   - revisa si existen clases, paquetes o componentes que puedan reutilizarse;
   - evita duplicar funcionalidades existentes.

RESULTADO ESPERADO:

Primero genera la Spec completa con:

.kiro/specs/gestion-usuarios/
├── requirements.md
├── design.md
└── tasks.md

No implementes todavía el código hasta que la especificación, diseño y tareas estén definidos y validados.

Después de completar la especificación, espera mi aprobación para iniciar la implementación.

B) Implementar manejo de logs
Actúa como un arquitecto senior de Spring Boot.

Quiero implementar primero un sistema de logging profesional en la aplicación. **Todavía NO implementes JWT ni Spring Security**. La seguridad con JWT será implementada en una etapa posterior.

### 1. Analizar el proyecto

Antes de realizar cambios:

* Analiza la estructura actual del proyecto.
* Identifica Controllers, Services, Repositories, configuración, excepciones y filtros existentes.
* Identifica si ya existe algún mecanismo de logging.
* Revisa el `GlobalExceptionHandler` existente.
* Identifica la versión de Spring Boot y las dependencias actuales.
* Respeta la arquitectura existente y evita introducir dependencias innecesarias.

### 2. Implementar logging centralizado

Utiliza el mecanismo estándar de Spring Boot:

* SLF4J
* Logback

No utilizar `System.out.println()`.

Implementa una configuración centralizada que permita controlar los niveles:

* ERROR
* WARN
* INFO
* DEBUG

La configuración debe poder modificarse sin cambiar código fuente.

### 3. Correlation ID

Implementa un `OncePerRequestFilter` para manejar un `X-Correlation-ID`.

Comportamiento:

* Si la petición contiene `X-Correlation-ID`, reutilizarlo.
* Si no existe, generar un UUID.
* Guardarlo en MDC.
* Incluirlo automáticamente en todos los logs generados durante la petición.
* Devolverlo en la respuesta HTTP mediante `X-Correlation-ID`.
* Limpiar el MDC correctamente al finalizar la petición.

Esto debe permitir rastrear una petición completa desde Controller → Service → Repository → Exception Handler.

### 4. Logging HTTP

Implementa logging centralizado de las peticiones HTTP.

Registrar como mínimo:

* método HTTP
* URI/endpoint
* correlationId
* código HTTP de respuesta
* tiempo de procesamiento
* resultado de la petición

Ejemplo conceptual:

```text
INFO [correlationId=abc-123] HTTP POST /api/usuarios
INFO [correlationId=abc-123] HTTP POST /api/usuarios completed status=201 duration=125ms
```

No registrar:

* passwords
* tokens
* Authorization
* secretos
* credenciales
* información sensible

### 5. Logging por capas

Agregar logging donde realmente aporte valor.

#### Controller

Registrar:

* inicio de operaciones importantes
* resultado de operaciones
* parámetros únicamente cuando sean seguros de registrar

#### Service

Registrar:

* operaciones de negocio relevantes
* creación
* actualización
* eliminación
* validaciones importantes
* situaciones excepcionales

#### Repository

No agregar logging excesivo.

Registrar únicamente errores o situaciones técnicas relevantes.

No registrar automáticamente cada consulta SQL de producción.

### 6. Niveles de logging

Aplicar estas reglas:

`ERROR`

* excepciones
* fallos de integración
* errores de base de datos
* errores inesperados

`WARN`

* situaciones anormales recuperables
* validaciones importantes
* recursos no encontrados cuando sea relevante

`INFO`

* operaciones importantes de negocio
* inicio/finalización de procesos relevantes
* información general de la aplicación

`DEBUG`

* información técnica utilizada para diagnóstico
* detalles internos que no deberían aparecer normalmente en producción

### 7. GlobalExceptionHandler

Integrar el logging con el `GlobalExceptionHandler` existente.

Cada excepción debe:

* generar un log apropiado
* incluir el `correlationId`
* registrar stack trace únicamente en el servidor
* devolver al cliente una respuesta controlada
* no exponer información interna de la aplicación

No duplicar el manejo de excepciones existente.

### 8. Archivos de log

Configurar Logback para permitir:

* salida por consola
* salida a archivo
* rotación de logs
* límite de tamaño
* retención de archivos antiguos

Preparar una estructura similar a:

```text
logs/
 ├── application.log
 ├── application-error.log
 └── audit.log
```

Si consideras que separar `audit.log` en esta etapa es prematuro, déjalo preparado pero no implementes una solución compleja.

### 9. Auditoría

Por ahora NO implementar un sistema completo de auditoría persistente.

Únicamente dejar preparada la arquitectura para que posteriormente podamos registrar:

* usuario que realizó la operación
* operación
* recurso afectado
* fecha/hora
* resultado
* correlationId

La identificación del usuario se incorporará posteriormente cuando implementemos JWT.

### 10. Preparación para observabilidad

La implementación debe quedar preparada para una futura integración con:

* OpenTelemetry
* Jaeger
* Dynatrace
* ELK/OpenSearch
* Grafana Loki

No instalar estas herramientas todavía.

El objetivo actual es únicamente dejar correctamente implementado el logging y el correlationId.

### 11. Buenas prácticas

Aplicar las siguientes reglas:

* Utilizar SLF4J.
* Utilizar placeholders:

```java
log.info("Usuario creado correctamente id={}", id);
```

en lugar de concatenaciones.

* No registrar objetos completos si pueden contener información sensible.
* No registrar credenciales.
* No registrar JWT.
* No registrar headers completos.
* No registrar passwords.
* No generar logs excesivos.
* No modificar la lógica de negocio.
* No crear clases innecesarias.

### 12. Pruebas

Después de implementar:

1. Compila el proyecto.
2. Ejecuta las pruebas existentes.
3. Si es posible, agrega pruebas para:

   * generación de correlationId
   * reutilización de `X-Correlation-ID`
   * propagación mediante MDC
   * limpieza del MDC
   * respuesta HTTP con `X-Correlation-ID`
   * logging del tiempo de respuesta
   * manejo de excepciones

### 13. Resultado final

Al terminar, muéstrame:

1. Archivos creados.
2. Archivos modificados.
3. Dependencias agregadas.
4. Configuración de logging.
5. Ejemplo de logs generados.
6. Cómo probar el `X-Correlation-ID` desde Postman.
7. Resultado de la compilación.
8. Resultado de las pruebas.

IMPORTANTE:

* NO implementar JWT.
* NO implementar Spring Security.
* NO modificar autenticación.
* NO modificar autorización.
* NO cambiar endpoints existentes.
* NO cambiar lógica de negocio.

Primero quiero dejar una base sólida de logging y trazabilidad. La implementación de JWT se realizará posteriormente sobre esta base.
