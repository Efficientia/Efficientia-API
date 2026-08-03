package com.example.efficientia.Controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api")
public class EfficientiaController {
    @GetMapping("/docsCount")
    public String getQuantDocs() {
        return "Ta funcionando o endpoint da contagem";
    }

    @GetMapping("/getDocs")
    public String getDocs() {
        return "Ta funcionando o endpoint do pull de documentos";
    }
    
    @PostMapping("/postDocs")
    public String postDocs(@RequestBody String nome) {
        String name = nome.toUpperCase();
        
        return name;
    }
    
}
