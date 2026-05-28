package com.xcappstore.admin.tag.service;

import com.xcappstore.admin.tag.dto.TagCreateRequest;
import com.xcappstore.admin.tag.dto.TagQueryRequest;
import com.xcappstore.admin.tag.dto.TagResponse;
import com.xcappstore.admin.tag.dto.TagUpdateRequest;
import java.util.List;

public interface TagService {
    Long create(TagCreateRequest request);

    TagResponse detail(Long id);

    List<TagResponse> list(TagQueryRequest request);

    void update(Long id, TagUpdateRequest request);

    TagResponse toggleHot(Long id);

    void delete(Long id);
}
