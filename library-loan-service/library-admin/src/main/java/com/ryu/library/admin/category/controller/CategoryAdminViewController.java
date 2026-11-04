package com.ryu.library.admin.category.controller;

import com.ryu.library.admin.category.service.CategoryAdminService;
import com.ryu.library.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
public class CategoryAdminViewController {

    private final CategoryAdminService categoryAdminService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tree", categoryAdminService.tree());
        return "admin/category/list";
    }

    @PostMapping
    public String create(@RequestParam(required = false) Long parentId, @RequestParam String name,
            @RequestParam(defaultValue = "0") int sortOrder, RedirectAttributes redirectAttributes) {
        return run(redirectAttributes, "분류를 추가했습니다.", () -> categoryAdminService.create(parentId, name, sortOrder));
    }

    @PostMapping("/{categoryId}")
    public String rename(@PathVariable Long categoryId, @RequestParam String name,
            @RequestParam(defaultValue = "0") int sortOrder, RedirectAttributes redirectAttributes) {
        return run(redirectAttributes, "분류를 수정했습니다.", () -> categoryAdminService.rename(categoryId, name, sortOrder));
    }

    @PostMapping("/{categoryId}/delete")
    public String delete(@PathVariable Long categoryId, RedirectAttributes redirectAttributes) {
        return run(redirectAttributes, "분류를 삭제했습니다.", () -> categoryAdminService.delete(categoryId));
    }

    private String run(RedirectAttributes redirectAttributes, String successMessage, Runnable action) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("message", successMessage);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
