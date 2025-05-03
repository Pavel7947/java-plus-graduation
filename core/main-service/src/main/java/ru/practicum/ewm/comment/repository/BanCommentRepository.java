package ru.practicum.ewm.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.comment.model.BanComment;
import ru.practicum.ewm.comment.model.BanCommentId;

public interface BanCommentRepository extends JpaRepository<BanComment, BanCommentId> {
}
