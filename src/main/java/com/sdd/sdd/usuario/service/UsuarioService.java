package com.sdd.sdd.usuario.service;

import com.sdd.sdd.common.dto.PageResponse;
import com.sdd.sdd.usuario.dto.UsuarioRequest;
import com.sdd.sdd.usuario.dto.UsuarioResponse;
import com.sdd.sdd.usuario.dto.UsuarioUpdateRequest;
import com.sdd.sdd.usuario.entity.EstadoUsuario;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    UsuarioResponse registrar(UsuarioRequest request);

    UsuarioResponse obtenerPorId(Long id);

    UsuarioResponse obtenerPorUsername(String username);

    PageResponse<UsuarioResponse> listar(String username, String email, EstadoUsuario estado, Pageable pageable);

    UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request);

    void eliminarLogico(Long id);
}
