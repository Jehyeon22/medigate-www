package com.example.demo.controller;

import com.example.demo.entity.Post;
import com.example.demo.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/posts")
    public List<Post> getAllPosts() {
        return postService.findAll();
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        return postService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/posts")
    public Post createPost(@RequestBody Post post) {
        return postService.save(post);
    }

    @PostMapping("/posts/upload")
    public Post createPostWithFile(
            @RequestParam("title") String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        
        return postService.saveWithFile(post, file);
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // 헬스체크 - ECS 헬스체크용
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("database", "connected");  // 실제로는 DB 연결 체크 필요
        health.put("efs", postService.isEfsAccessible() ? "accessible" : "not accessible");
        return ResponseEntity.ok(health);
    }

    // 루트 경로
    @GetMapping("/")
    public Map<String, String> home() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello from ECS!");
        response.put("status", "running");
        return response;
    }
}
