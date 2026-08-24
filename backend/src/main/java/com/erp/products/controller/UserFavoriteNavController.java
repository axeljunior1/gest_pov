package com.erp.products.controller;

import com.erp.products.service.UserFavoriteNavService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ecrans favoris de l'utilisateur connecte (barre laterale desktop) — lies au compte, pas au poste.
 */
@RestController
@RequestMapping("/api/users/me/favorites")
@RequiredArgsConstructor
public class UserFavoriteNavController {

    private final UserFavoriteNavService favoriteNavService;

    @GetMapping
    public List<String> list() {
        return favoriteNavService.list();
    }

    @PostMapping("/{navItemKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable String navItemKey) {
        favoriteNavService.add(navItemKey);
    }

    @DeleteMapping("/{navItemKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String navItemKey) {
        favoriteNavService.remove(navItemKey);
    }
}
