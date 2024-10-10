package ru.relex.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.relex.service.impl.UserActivationServiceImpl;

@RequiredArgsConstructor
@RequestMapping("/user")
@RestController
public class ActivationController {

    private final UserActivationServiceImpl userActivationService;

    @RequestMapping(method = RequestMethod.GET, value = "/activation")
    public ResponseEntity<?> activation(@RequestParam("id") String id) {
        var res = userActivationService.activation(id);
        if (res){
            return ResponseEntity.ok().body("Successful registration!!");
        }
        return ResponseEntity.internalServerError().build();
    }
}
