package com.smartemergency.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smartemergency.service.CrewService;
import com.smartemergency.web.Dto.*;

import java.util.List;

/** Endpoints available to every logged-in user. */
@RestController
@RequestMapping("/api/common")
public class CommonController {
    private final CrewService crew;

    public CommonController(CrewService crew) {
        this.crew = crew;
    }

    @GetMapping("/hospitals")
    public List<HospitalView> hospitals() { return crew.hospitals(); }
}
