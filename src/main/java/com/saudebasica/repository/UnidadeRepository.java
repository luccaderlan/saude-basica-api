package com.saudebasica.repository;

import com.saudebasica.domain.UnidadeSaude;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeRepository extends JpaRepository<UnidadeSaude, Long> {
}
