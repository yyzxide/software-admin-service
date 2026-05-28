package com.xcappstore.admin.tag.controller;

import com.xcappstore.admin.common.ApiResponse;
import com.xcappstore.admin.tag.dto.TagCreateRequest;
import com.xcappstore.admin.tag.dto.TagQueryRequest;
import com.xcappstore.admin.tag.dto.TagResponse;
import com.xcappstore.admin.tag.dto.TagUpdateRequest;
import com.xcappstore.admin.tag.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<List<TagResponse>> list(@Valid TagQueryRequest request) {
        return ApiResponse.success(tagService.list(request));
    }

    @GetMapping("/hot")
    public ApiResponse<List<TagResponse>> hot() {
        TagQueryRequest request = new TagQueryRequest();
        request.setIsHot(1);
        return ApiResponse.success(tagService.list(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TagResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(tagService.detail(id));
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody TagCreateRequest request) {
        Long tagId = tagService.create(request);
        return ApiResponse.success(Map.of("tag_id", tagId));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody TagUpdateRequest request) {
        tagService.update(id, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/toggle-hot")
    public ApiResponse<TagResponse> toggleHot(@PathVariable Long id) {
        return ApiResponse.success(tagService.toggleHot(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ApiResponse.success(null);
    }
}
