package com.sdd.sdd.persona.service;

import com.sdd.sdd.common.dto.PageResponse;
import com.sdd.sdd.common.exception.DuplicadoException;
import com.sdd.sdd.common.exception.RecursoNoEncontradoException;
import com.sdd.sdd.persona.dto.PersonaRequest;
import com.sdd.sdd.persona.dto.PersonaResponse;
import com.sdd.sdd.persona.entity.Persona;
import com.sdd.sdd.persona.entity.TipoDocumento;
import com.sdd.sdd.persona.mapper.PersonaMapper;
import com.sdd.sdd.persona.repository.PersonaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link PersonaService}.
 * Contiene toda la lógica de negocio del módulo Persona.
 * No registra datos sensibles en los logs.
 */
@Service
@Transactional
public class PersonaServiceImpl implements PersonaService {

    private static final Logger log = LoggerFactory.getLogger(PersonaServiceImpl.class);

    private final PersonaRepository repository;

    public PersonaServiceImpl(PersonaRepository repository) {
        this.repository = repository;
    }

    // ── Registro ──────────────────────────────────────────────────────────────

    @Override
    public PersonaResponse crear(PersonaRequest request) {
        log.debug("Verificando unicidad documental tipoDocumento={} numeroDocumento={}",
                request.getTipoDocumento(), request.getNumeroDocumento());

        if (repository.existsByTipoDocumentoAndNumeroDocumentoAndComplemento(
                request.getTipoDocumento(),
                request.getNumeroDocumento(),
                request.getComplemento())) {
            log.warn("Intento de registro con identidad documental duplicada: tipoDocumento={} numeroDocumento={}",
                    request.getTipoDocumento(), request.getNumeroDocumento());
            throw new DuplicadoException(
                    "La combinación de tipo de documento '" + request.getTipoDocumento()
                    + "', número '" + request.getNumeroDocumento() + "' ya está registrada.");
        }

        Persona persona = PersonaMapper.toEntity(request);
        Persona guardada = repository.save(persona);

        log.info("Persona creada correctamente id={}", guardada.getId());
        return PersonaMapper.toResponse(guardada);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PersonaResponse obtenerPorId(Long id) {
        log.debug("Buscando persona por id={}", id);
        Persona persona = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Persona no encontrada id={}", id);
                    return new RecursoNoEncontradoException(
                            "Persona con id " + id + " no encontrada.");
                });
        return PersonaMapper.toResponse(persona);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResponse obtenerPorDocumento(TipoDocumento tipoDocumento,
                                               String numeroDocumento,
                                               String complemento) {
        log.debug("Buscando persona por tipoDocumento={} numeroDocumento={}",
                tipoDocumento, numeroDocumento);
        Persona persona = repository.findByTipoDocumentoAndNumeroDocumentoAndComplemento(
                        tipoDocumento, numeroDocumento, complemento)
                .orElseThrow(() -> {
                    log.warn("Persona no encontrada tipoDocumento={} numeroDocumento={}",
                            tipoDocumento, numeroDocumento);
                    return new RecursoNoEncontradoException(
                            "Persona con tipo de documento '" + tipoDocumento
                            + "' y número '" + numeroDocumento + "' no encontrada.");
                });
        return PersonaMapper.toResponse(persona);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PersonaResponse> listar(String nombres,
                                                String apellidoPaterno,
                                                TipoDocumento tipoDocumento,
                                                Pageable pageable) {
        log.debug("Listando personas con filtros nombres={} apellidoPaterno={} tipoDocumento={}",
                nombres, apellidoPaterno, tipoDocumento);

        Specification<Persona> spec = Specification.where((Specification<Persona>) null);

        if (nombres != null && !nombres.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("nombres")), "%" + nombres.toLowerCase() + "%"));
        }
        if (apellidoPaterno != null && !apellidoPaterno.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("apellidoPaterno")),
                            "%" + apellidoPaterno.toLowerCase() + "%"));
        }
        if (tipoDocumento != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("tipoDocumento"), tipoDocumento));
        }

        return PageResponse.of(repository.findAll(spec, pageable).map(PersonaMapper::toResponse));
    }
}
