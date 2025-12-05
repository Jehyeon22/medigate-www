package com.example.demo.service;

import com.example.demo.entity.Post;
import com.example.demo.repository.PostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;

    @Value("${app.upload.path:/mnt/efs/uploads}")
    private String uploadPath;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<Post> findAll() {
        return postRepository.findAll();
    }

    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    public Post save(Post post) {
        return postRepository.save(post);
    }

    public Post saveWithFile(Post post, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            // EFS 경로에 파일 저장
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadPath, fileName);
            
            // 디렉토리 생성
            Files.createDirectories(filePath.getParent());
            
            // 파일 저장
            Files.copy(file.getInputStream(), filePath);
            
            post.setFilePath(filePath.toString());
        }
        return postRepository.save(post);
    }

    public void deleteById(Long id) {
        // 파일도 함께 삭제
        postRepository.findById(id).ifPresent(post -> {
            if (post.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(post.getFilePath()));
                } catch (IOException e) {
                    // 로깅 처리
                }
            }
        });
        postRepository.deleteById(id);
    }

    public boolean isEfsAccessible() {
        try {
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return Files.isWritable(path);
        } catch (IOException e) {
            return false;
        }
    }
}
