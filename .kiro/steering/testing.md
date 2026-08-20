---
inclusion: always
---

# Estandares de Pruebas — Spring Boot SDD

Este archivo define las reglas obligatorias de testing para todo codigo nuevo o modificado
en este proyecto. Kiro debe aplicarlas automaticamente en cada implementacion.

---

## 1. Principios generales

Todo codigo nuevo debe ser verificable mediante pruebas.

Las pruebas deben cumplir:

| Propiedad | Descripcion |
|-----------|-------------|
| Mantenibles | Faciles de leer y modificar cuando el requerimiento cambie |
| Reproducibles | El mismo resultado independientemente del entorno o el orden de ejecucion |
| Independientes | Ninguna prueba depende del estado dejado por otra |
| Descriptivas | El nombre expresa exactamente que se esta verificando |
| Orientadas a comportamiento | Verifican que hace el codigo, no como lo hace internamente |
| Aisladas | Sin dependencia de servicios externos no controlados |

---

## 2. Piramide de pruebas

```
        [E2E]           ← pocos, solo para flujos criticos
       [API / Controller]
      [Integracion]
     [Unitarias]        ← la mayoria deben ser unitarias
```

No crear pruebas de integracion cuando una prueba unitaria sea suficiente.
No omitir pruebas unitarias por considerar que las de integracion las cubren.

---

## 3. Herramientas del proyecto

Usar exclusivamente las herramientas ya presentes — no agregar frameworks de testing sin justificacion.

| Herramienta | Uso | Como se usa en este proyecto |
|-------------|-----|------------------------------|
| JUnit 5 | Framework base | `@Test`, `@BeforeEach`, `@ExtendWith` |
| Mockito | Mocking en pruebas unitarias | `@MockitoBean`, `@Mock`, `@InjectMocks`, `when(...)`, `verify(...)` |
| AssertJ | Aserciones fluidas | `assertThat(...).isEqualTo(...)` |
| Spring `@WebMvcTest` | Pruebas de Controller (slice) | Carga solo la capa web |
| Spring `@DataJpaTest` | Pruebas de Repository (slice) | Carga solo la capa de persistencia |
| MockMvc | Simulacion de peticiones HTTP | `mockMvc.perform(get(...)).andExpect(...)` |

Importaciones correctas para Spring Boot 4.x:
```java
// WebMvcTest
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
// MockitoBean (reemplaza @MockBean de Boot 3.x)
import org.springframework.test.context.bean.override.mockito.MockitoBean;
// DataJpaTest
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
```

---

## 4. Pruebas unitarias de Service

Los Services deben tener pruebas unitarias para todas las reglas de negocio relevantes.

### Escenarios obligatorios por operacion

| Operacion | Escenarios minimos |
|-----------|-------------------|
| Crear | exitoso, username duplicado, email duplicado |
| Consultar por ID | existente, inexistente |
| Consultar por campo | existente, inexistente |
| Actualizar | exitoso, inexistente, email duplicado en otro usuario |
| Eliminar logico | exitoso, inexistente |
| Listar | con y sin filtros |

### Estructura de prueba unitaria de Service

```java
@ExtendWith(MockitoExtension.class)
class MiServiceImplTest {

    @Mock
    private MiRepository repository;

    @InjectMocks
    private MiServiceImpl service;

    @Test
    void deberiaCrearRecursoCuandoLosDatosSonValidos() {
        // Given
        when(repository.existsByNombre("nombre")).thenReturn(false);
        when(repository.save(any())).thenReturn(entidadEsperada);

        // When
        MiResponse resultado = service.crear(requestValido);

        // Then
        assertThat(resultado.getId()).isEqualTo(1L);
        verify(repository, times(1)).save(any());
    }

    @Test
    void deberiaLanzarDuplicadoExceptionCuandoElNombreYaExiste() {
        // Given
        when(repository.existsByNombre("nombre")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(DuplicadoException.class)
                .hasMessageContaining("nombre");

        verify(repository, never()).save(any());
    }
}
```

---

## 5. Pruebas de Controller

Usar `@WebMvcTest` para pruebas de slice de la capa web.
Mockear el Service con `@MockitoBean`.
Importar `SecurityConfig` con `@Import` para que Spring Security no bloquee las peticiones.

### Escenarios obligatorios por endpoint

| Endpoint | Escenarios minimos |
|----------|-------------------|
| POST crear | 201 exitoso, 400 validacion, 409 conflicto |
| GET por ID | 200 exitoso, 404 no encontrado |
| GET por campo unico | 200 exitoso, 404 no encontrado |
| GET listar | 200 con resultados, 200 con filtros |
| PUT actualizar | 200 exitoso, 400 validacion, 404 no encontrado, 409 conflicto |
| DELETE logico | 204 exitoso, 404 no encontrado |

### Estructura de prueba de Controller

```java
@WebMvcTest(MiController.class)
@Import(SecurityConfig.class)
class MiControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    MiService miService;

    @Test
    void deberiaRetornar201CuandoElRecursoSeCreaCorrectamente() throws Exception {
        when(miService.crear(any())).thenReturn(responseBase);

        mockMvc.perform(post("/api/recursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void deberiaRetornar400CuandoElEmailEsInvalido() throws Exception {
        mockMvc.perform(post("/api/recursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonConEmailInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());
    }
}
```

### Nota sobre ObjectMapper en Spring Boot 4.x
`spring-boot-starter-webmvc-test` no auto-configura `ObjectMapper` como bean en el contexto
de slice. Registrarlo via `@TestConfiguration` como se muestra arriba.

Para requests con campos `@JsonProperty(WRITE_ONLY)` (como `password`), construir el JSON
como `Map` para garantizar que el campo se incluya en la serializacion:

```java
String json = objectMapper.writeValueAsString(Map.of(
    "nombres", "Juan",
    "password", "Secreto123"
));
```

---

## 6. Pruebas de Repository

Usar `@DataJpaTest` para pruebas de slice de la capa de persistencia.

Crear pruebas de Repository cuando:
- Existan metodos derivados de Spring Data no triviales.
- Existan `@Query` personalizadas.
- Existan constraints de unicidad importantes para el negocio.
- Existan reglas de persistencia criticas.

No crear pruebas para operaciones CRUD basicas que Spring Data provee directamente
(`save`, `findById`, `deleteById`) sin logica adicional.

### Configuracion para PostgreSQL real (sin H2)

```java
@DataJpaTest(excludeAutoConfiguration = JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(UsuarioRepositoryTest.NoOpAuditingConfig.class)
class MiRepositoryTest {

    @TestConfiguration
    static class NoOpAuditingConfig {
        @Bean("jpaAuditingHandler")
        AuditingHandler jpaAuditingHandler() {
            AuditingHandler handler = mock(AuditingHandler.class);
            when(handler.markCreated(any())).thenAnswer(inv -> inv.getArgument(0));
            when(handler.markModified(any())).thenAnswer(inv -> inv.getArgument(0));
            return handler;
        }
    }
}
```

---

## 7. Pruebas de integracion

Las pruebas de integracion deben usar una base de datos controlada.

Opciones en orden de preferencia:
1. PostgreSQL real via `@TestPropertySource` con `ddl-auto=create-drop` (patron actual del proyecto).
2. Testcontainers si se agrega al proyecto en el futuro.

Nunca conectar a:
- Base de datos de produccion.
- Base de datos compartida con otros desarrolladores.
- Servicios externos no controlados.

---

## 8. Datos de prueba

Usar exclusivamente datos ficticios y controlados:

```java
// CORRECTO
.username("jperez_test")
.email("juan.test@example.com")
.password("$2a$10$hashedpassword")

// PROHIBIDO
.username("usuario_real_produccion")
.email("correo_real@empresa.com")
.password("passwordReal123")
```

Nunca incluir en pruebas:
- Contraseñas reales
- Tokens JWT reales
- Informacion personal real de personas
- Credenciales de servicios reales
- Secretos o claves privadas

---

## 9. Convencion de nombres

Usar nombres descriptivos que expresen el escenario completo:

```java
// CORRECTO
void deberiaRegistrarUsuarioCuandoLosDatosSonValidos()
void deberiaLanzarDuplicadoExceptionCuandoElUsernameYaExiste()
void deberiaRetornar404CuandoElUsuarioNoExiste()
void deberiaRetornar201ConLocationHeaderCuandoElRegistroEsExitoso()

// PROHIBIDO
void test1()
void testUsuario()
void testOk()
void registrar()
```

---

## 10. Patron Given / When / Then

Estructurar cada prueba con comentarios o bloques claros:

```java
@Test
void deberiaActualizarEmailCuandoNoEstaEnUso() {
    // Given
    when(repository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
    when(repository.existsByEmailAndIdNot("nuevo@test.com", 1L)).thenReturn(false);
    when(repository.save(any())).thenReturn(usuarioActualizado);

    // When
    UsuarioResponse resultado = service.actualizar(1L, updateRequest);

    // Then
    assertThat(resultado.getEmail()).isEqualTo("nuevo@test.com");
    verify(repository, times(1)).save(any());
}
```

---

## 11. Casos negativos — obligatorios

Nunca probar solo el camino feliz. Por cada operacion incluir al menos:

- Datos invalidos (Bean Validation)
- Campos obligatorios ausentes o vacios
- Recursos inexistentes (404)
- Duplicados (409)
- Excepciones de negocio esperadas
- Valores en los limites (min/max de campos)

---

## 12. Logging en pruebas

- No usar `System.out.println()` para depurar pruebas.
- Si se necesita diagnostico, usar el logger de la clase bajo prueba o revisar la salida de Surefire.
- Las pruebas no deben verificar el contenido exacto de los logs salvo que se este probando
  especificamente el mecanismo de logging (ej: `CorrelationIdFilterTest`).

---

## 13. Seguridad — pruebas futuras

Cuando se implemente JWT/Spring Security, agregar pruebas para:

- Autenticacion valida → acceso permitido
- Autenticacion invalida → 401
- Token expirado → 401
- Token invalido → 401
- Endpoint publico sin token → 200
- Endpoint protegido sin token → 401
- Usuario sin permisos → 403
- Usuario con permisos → acceso permitido

No implementar estas pruebas hasta que exista la funcionalidad JWT correspondiente.

---

## 14. Ejecucion obligatoria

Despues de cualquier modificacion de codigo, ejecutar en orden:

```bash
# Compilar y ejecutar todas las pruebas
mvn test

# Si hubo cambios en dependencias o configuracion
mvn clean test

# Para verificar el artefacto final
mvn clean package
```

### Comandos para pruebas especificas

```bash
# Una clase de prueba
mvn test -Dtest=UsuarioServiceImplTest

# Varias clases
mvn test -Dtest=UsuarioServiceImplTest,UsuarioControllerTest

# Un metodo especifico
mvn test -Dtest=UsuarioControllerTest#deberiaRetornar201CuandoElRegistroEsExitoso
```

### Si una prueba falla

1. Leer el mensaje de error completo.
2. Identificar si el fallo es en el codigo de produccion o en la prueba.
3. Corregir segun corresponda.
4. Volver a ejecutar.
5. **No ignorar errores. No marcar pruebas con `@Disabled` para ocultar fallos.**

---

## 15. No modificar pruebas para ocultar errores

Una prueba que falla es informacion valiosa. Modificar una prueba para que pase
sin entender la causa es una violacion de esta regla.

Modificar una prueba solo cuando:
- El requerimiento funcional cambio genuinamente.
- La prueba tenia un bug propio (no el codigo de produccion).
- Se agrego nueva funcionalidad que cambia el comportamiento esperado.

---

## 16. Regla principal — obligatoria

Todo requerimiento funcional nuevo debe incluir pruebas antes de considerarse completo.

Flujo obligatorio al finalizar cualquier implementacion:

1. Compilar: `mvn compile`
2. Ejecutar pruebas: `mvn test`
3. Revisar errores si los hay
4. Corregir codigo o pruebas segun corresponda
5. Volver a ejecutar hasta obtener BUILD SUCCESS
6. Informar resultado: clases de prueba ejecutadas, total de pruebas, fallos

Estas reglas se aplican automaticamente a todo codigo nuevo o modificado en el proyecto.