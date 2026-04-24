package com.example.demo.service;

import com.example.demo.entity.Comment;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RedisService redisService;

    public Comment addBotComment(Long postId, Long botId, Long humanId, int depth) {

        // Vertical cap
        if (depth > 20) throw new RuntimeException("Depth limit exceeded");

        // Horizontal cap
        String botCountKey = "post:" + postId + ":bot_count";
        Long count = redisService.increment(botCountKey);

        if (count > 100) {
            throw new RuntimeException("429 Too Many Requests");
        }

        // Cooldown
        String cooldownKey = "cooldown:bot_" + botId + ":human_" + humanId;
        if (redisService.exists(cooldownKey)) {
            throw new RuntimeException("Cooldown active");
        }

        redisService.setWithTTL(cooldownKey, "1", 10);

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(botId);
        comment.setDepthLevel(depth);
        comment.setContent("Bot reply");

        return commentRepository.save(comment);
    }
}