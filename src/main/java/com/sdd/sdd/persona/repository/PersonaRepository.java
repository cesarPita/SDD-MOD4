package com.sdd.sdd.persona.repository;

import com.sdd.sdd.persona.entity.Persona;
import com.sdd.sdd.persona.entity.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long>,
                                            JpaSpecificationExecutor<Persona> {

    boolean existsByTipoDocumentoAndNumeroDocumentoAndComplemento(
            TipoDocumento tipoDocumento,
            String numeroDocumento,
            String complemento);

    Optional<Persona> findByTipoDocumentoAndNumeroDocumentoAndComplemento(
            TipoDocumento tipoDocumento,
            String numeroDocumento,
            String complemento);
}
