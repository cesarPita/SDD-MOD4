package com.sdd.sdd.persona.service;

import com.sdd.sdd.common.dto.PageResponse;
import com.sdd.sdd.persona.dto.PersonaRequest;
import com.sdd.sdd.persona.dto.PersonaResponse;
import com.sdd.sdd.persona.entity.TipoDocumento;
import org.springframework.data.domain.Pageable;

public interface PersonaService {

    PersonaResponse crear(PersonaRequest request);

    PersonaResponse obtenerPorId(Long id);

    PersonaResponse obtenerPorDocumento(TipoDocumento tipoDocumento,
                                        String numeroDocumento,
                                        String complemento);

    PageResponse<PersonaResponse> listar(String nombres,
                                         String apellidoPaterno,
                                         TipoDocumento tipoDocumento,
                                         Pageable pageable);
}
