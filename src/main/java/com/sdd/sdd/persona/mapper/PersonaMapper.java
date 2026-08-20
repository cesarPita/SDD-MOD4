package com.sdd.sdd.persona.mapper;

import com.sdd.sdd.persona.dto.PersonaRequest;
import com.sdd.sdd.persona.dto.PersonaResponse;
import com.sdd.sdd.persona.entity.Persona;

/**
 * Clase utilitaria para el mapeo entre la entidad {@link Persona} y sus DTOs.
 *
 * <p>No es instanciable. Todos los métodos son estáticos.
 *
 * <p>{@code toEntity()} no asigna {@code id}, {@code fechaCreacion} ni {@code fechaModificacion};
 * esos campos son gestionados automáticamente por JPA Auditing.
 */
public final class PersonaMapper {

    private PersonaMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte una entidad {@link Persona} a su DTO de respuesta.
     * Mapea todos los campos incluyendo los de auditoría.
     *
     * @param persona entidad persistida
     * @return DTO de respuesta con todos los campos
     */
    public static PersonaResponse toResponse(Persona persona) {
        return PersonaResponse.builder()
                .id(persona.getId())
                .tipoDocumento(persona.getTipoDocumento())
                .numeroDocumento(persona.getNumeroDocumento())
                .complemento(persona.getComplemento())
                .fechaNacimiento(persona.getFechaNacimiento())
                .apellidoPaterno(persona.getApellidoPaterno())
                .apellidoMaterno(persona.getApellidoMaterno())
                .apellidoEsposo(persona.getApellidoEsposo())
                .nombres(persona.getNombres())
                .genero(persona.getGenero())
                .estadoCivil(persona.getEstadoCivil())
                .fechaCreacion(persona.getFechaCreacion())
                .fechaModificacion(persona.getFechaModificacion())
                .build();
    }

    /**
     * Construye una nueva entidad {@link Persona} a partir del DTO de creación.
     * Los campos {@code id}, {@code fechaCreacion} y {@code fechaModificacion} no se asignan
     * aquí; son gestionados por JPA Auditing.
     *
     * @param request DTO con los datos de la nueva persona
     * @return entidad lista para persistir
     */
    public static Persona toEntity(PersonaRequest request) {
        return Persona.builder()
                .tipoDocumento(request.getTipoDocumento())
                .numeroDocumento(request.getNumeroDocumento())
                .complemento(request.getComplemento())
                .fechaNacimiento(request.getFechaNacimiento())
                .apellidoPaterno(request.getApellidoPaterno())
                .apellidoMaterno(request.getApellidoMaterno())
                .apellidoEsposo(request.getApellidoEsposo())
                .nombres(request.getNombres())
                .genero(request.getGenero())
                .estadoCivil(request.getEstadoCivil())
                .build();
    }
}
