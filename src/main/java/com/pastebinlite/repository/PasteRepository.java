package com.pastebinlite.repository;

import com.pastebinlite.entity.Paste;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasteRepository extends JpaRepository<Paste, String> {
}
