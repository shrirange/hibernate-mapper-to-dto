package com.example.hibernatemappertodto;

import org.springframework.data.jpa.repository.JpaRepository;
@AUDITABLE_REPO
public interface AuthorRepository extends JpaRepository<Author, Integer> {

}
