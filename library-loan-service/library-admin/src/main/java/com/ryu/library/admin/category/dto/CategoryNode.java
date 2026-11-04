package com.ryu.library.admin.category.dto;

import java.util.List;

// 2단계 분류 트리의 한 줄. 최상위 분류 아래에 하위 분류를 붙입니다.
public record CategoryNode(Long id, String name, int sortOrder, List<CategoryNode> children) {
}
