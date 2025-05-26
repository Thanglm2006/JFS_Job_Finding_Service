package com.example.JFS_Job_Finding_Service.Controller;
import com.example.JFS_Job_Finding_Service.DTO.PostingRequest;
import com.example.JFS_Job_Finding_Service.Services.PostService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;



@Controller
@RequestMapping("/post")
public class PostController {
    @Autowired
    private PostService postService;

    @Operation(summary = "Add a new post", description = "to post, you need to specify the Header with token and the body with title, description, and workspace picture")
    @PostMapping("/addPost")
    public ResponseEntity<?> addPost(@RequestHeader HttpHeaders headers, @RequestBody PostingRequest postingRequest) {
        return postService.addPost(headers.get("token").get(0),postingRequest);
    }
}
