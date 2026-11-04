package com.ryu.ebook.admin.ebook.controller;

import com.ryu.ebook.admin.ebook.dto.EbookForm;
import com.ryu.ebook.admin.ebook.service.EbookAdminService;
import com.ryu.ebook.ebook.domain.EbookStatus;
import com.ryu.ebook.global.exception.BusinessException;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/ebooks")
public class EbookAdminViewController {

    private final EbookAdminService ebookAdminService;

    @GetMapping
    public String list(@RequestParam(required = false) EbookStatus status, @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, Model model) {
        model.addAttribute("page", ebookAdminService.search(status, keyword, pageable));
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        return "admin/ebook/list";
    }

    @GetMapping("/new")
    public String registerForm(Model model) {
        return renderForm(new EbookForm(), null, model);
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("ebookForm") EbookForm form, BindingResult bindingResult, Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderForm(form, null, model);
        }
        try {
            Long id = ebookAdminService.register(form);
            redirectAttributes.addFlashAttribute("message", "전자책을 등록했습니다. 본문 파일을 올려 주세요.");
            return "redirect:/admin/ebooks/" + id;
        } catch (BusinessException e) {
            bindingResult.reject(e.getErrorCode().getCode(), e.getMessage());
            return renderForm(form, null, model);
        }
    }

    @GetMapping("/{ebookId}")
    public String detail(@PathVariable Long ebookId, Model model) {
        model.addAttribute("ebook", ebookAdminService.get(ebookId));
        return "admin/ebook/detail";
    }

    @GetMapping("/{ebookId}/edit")
    public String editForm(@PathVariable Long ebookId, Model model) {
        return renderForm(ebookAdminService.editForm(ebookId), ebookId, model);
    }

    @PostMapping("/{ebookId}/edit")
    public String edit(@PathVariable Long ebookId, @Valid @ModelAttribute("ebookForm") EbookForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderForm(form, ebookId, model);
        }
        try {
            ebookAdminService.update(ebookId, form);
        } catch (BusinessException e) {
            bindingResult.reject(e.getErrorCode().getCode(), e.getMessage());
            return renderForm(form, ebookId, model);
        }
        redirectAttributes.addFlashAttribute("message", "전자책 정보를 수정했습니다.");
        return "redirect:/admin/ebooks/" + ebookId;
    }

    @PostMapping("/{ebookId}/status")
    public String changeStatus(@PathVariable Long ebookId, @RequestParam EbookStatus status,
            RedirectAttributes redirectAttributes) {
        ebookAdminService.changeStatus(ebookId, status);
        redirectAttributes.addFlashAttribute("message", "판매 상태를 바꿨습니다.");
        return "redirect:/admin/ebooks/" + ebookId;
    }

    @PostMapping("/{ebookId}/file")
    public String uploadFile(@PathVariable Long ebookId, @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) throws IOException {
        try {
            ebookAdminService.uploadFile(ebookId, file.getOriginalFilename(), file.getSize(), file.getInputStream());
            redirectAttributes.addFlashAttribute("message", "본문 파일을 올렸습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/ebooks/" + ebookId;
    }

    private String renderForm(EbookForm form, Long ebookId, Model model) {
        model.addAttribute("ebookForm", form);
        model.addAttribute("ebookId", ebookId);
        model.addAttribute("categories", ebookAdminService.categoryOptions());
        return "admin/ebook/form";
    }
}
