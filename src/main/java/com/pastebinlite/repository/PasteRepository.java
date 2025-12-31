package com.pastebinlite.repository;

import com.pastebinlite.entity.Paste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasteRepository extends JpaRepository<Paste, String> {
}
