package com.saasai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saasai.dto.ModelRoute;
import com.saasai.service.PackageRoutingService;

@RestController
@RequestMapping("/test")
public class TestController {

    private final PackageRoutingService packageRoutingService;

    public TestController(PackageRoutingService packageRoutingService) {
        this.packageRoutingService = packageRoutingService;
    }

    // @GetMapping("/route")
    // public ModelRoute test() {
    //     return packageRoutingService.resolveRoute(
    //             "admin.tai@coquan.gov.vn"   // email có trong DB
    //     );
    // }
}