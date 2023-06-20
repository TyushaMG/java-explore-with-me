package ru.practicum.participation_request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.participation_request.model.Request;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
}
