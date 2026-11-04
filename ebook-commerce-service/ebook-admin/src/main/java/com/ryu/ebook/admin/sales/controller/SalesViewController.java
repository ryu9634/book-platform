package com.ryu.ebook.admin.sales.controller;

import com.ryu.ebook.admin.sales.service.SalesService;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SalesViewController {

    private final SalesService salesService;

    @GetMapping("/admin/sales")
    public String monthly(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month, Model model) {
        model.addAttribute("sales", salesService.monthly(month));
        return "admin/sales/monthly";
    }
}
