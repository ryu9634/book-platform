package com.ryu.library.admin.book.controller;

import com.ryu.library.admin.book.dto.BookForm;
import com.ryu.library.admin.book.dto.BookResponse;
import com.ryu.library.admin.book.dto.CopyStatusAction;
import com.ryu.library.admin.book.service.BookAdminService;
import com.ryu.library.global.exception.BusinessException;
import jakarta.validation.Valid;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/books")
public class BookAdminViewController {

    private final BookAdminService bookAdminService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, Model model) {
        model.addAttribute("page", bookAdminService.findBooks(keyword, pageable));
        model.addAttribute("keyword", keyword);
        return "admin/book/list";
    }

    // ISBN이 있으면 국립중앙도서관에서 서지 정보를 받아 폼을 채웁니다.
    // 조회 실패(없는 ISBN, 이미 등록, 외부 API 장애)는 오류 화면 대신 폼 위에 안내하고 직접 입력할 수 있게 둡니다.
    @GetMapping("/new")
    public String registerForm(@RequestParam(required = false) String isbn, Model model) {
        BookForm form = BookForm.withIsbn(isbn);
        if (isbn != null && !isbn.isBlank()) {
            try {
                form = bookAdminService.lookupByIsbn(isbn.trim());
            } catch (BusinessException e) {
                model.addAttribute("lookupError", e.getMessage());
            }
        }
        return renderForm(form, model);
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("bookForm") BookForm form, BindingResult bindingResult, Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderForm(form, model);
        }
        Long bookId;
        try {
            bookId = bookAdminService.register(form);
        } catch (BusinessException e) {
            bindingResult.reject(e.getErrorCode().getCode(), e.getMessage());
            return renderForm(form, model);
        }
        redirectAttributes.addFlashAttribute("message", "도서를 등록했습니다.");
        return "redirect:/admin/books/" + bookId;
    }

    @GetMapping("/{bookId}")
    public String detail(@PathVariable Long bookId, Model model) {
        model.addAttribute("book", bookAdminService.getBook(bookId));
        model.addAttribute("copies", bookAdminService.getCopies(bookId));
        return "admin/book/detail";
    }

    @GetMapping("/{bookId}/edit")
    public String editForm(@PathVariable Long bookId, Model model) {
        BookResponse book = bookAdminService.getBook(bookId);
        model.addAttribute("bookId", bookId);
        return renderForm(BookForm.from(book, book.categoryId()), model);
    }

    @PostMapping("/{bookId}/edit")
    public String edit(@PathVariable Long bookId, @Valid @ModelAttribute("bookForm") BookForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("bookId", bookId);
        if (bindingResult.hasErrors()) {
            return renderForm(form, model);
        }
        try {
            bookAdminService.update(bookId, form);
        } catch (BusinessException e) {
            bindingResult.reject(e.getErrorCode().getCode(), e.getMessage());
            return renderForm(form, model);
        }
        redirectAttributes.addFlashAttribute("message", "도서 정보를 수정했습니다.");
        return "redirect:/admin/books/" + bookId;
    }

    @PostMapping("/{bookId}/copies")
    public String addCopies(@PathVariable Long bookId, @RequestParam(defaultValue = "1") int quantity,
            RedirectAttributes redirectAttributes) {
        int added = bookAdminService.addCopies(bookId, quantity);
        redirectAttributes.addFlashAttribute("message", "소장 자료 " + added + "권을 추가했습니다.");
        return "redirect:/admin/books/" + bookId;
    }

    // 상태 변경이 허용되지 않으면(예: 대출 중인 권 분실 처리) 오류 화면 대신 상세 화면에 이유를 보여 줍니다.
    @PostMapping("/copies/{copyId}/status")
    public String changeCopyStatus(@PathVariable Long copyId, @RequestParam CopyStatusAction action,
            @RequestParam Long bookId, RedirectAttributes redirectAttributes) {
        try {
            bookAdminService.changeCopyStatus(copyId, action);
            redirectAttributes.addFlashAttribute("message", "소장 자료 상태를 바꿨습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/books/" + bookId;
    }

    private String renderForm(BookForm form, Model model) {
        model.addAttribute("bookForm", form);
        model.addAttribute("categories", bookAdminService.categoryOptions());
        return "admin/book/form";
    }
}
