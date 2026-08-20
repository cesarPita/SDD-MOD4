Nombre: Cesar Enrique Pita Perez
Tecnologia: Spring Boot
Prompt Utilizado:

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

