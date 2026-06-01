package com.digitaltwin.central.controller;

import com.digitaltwin.central.dto.ArtistResponseDto;
import com.digitaltwin.central.service.ArtistService;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

@RestController
@Tag(name = "Artists", description = "Retrieve artist information and metadata. Use to display artist bios, genres, and link them to events.")
@RequestMapping("/api/artists")
public class ArtistController {

    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    @GetMapping
    @Operation(summary = "List artists", description = "Return all artists with basic metadata. Use to build artist directories and selection lists.")
    public List<ArtistResponseDto> getAllArtists() {
        return artistService.getAllArtists();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get artist", description = "Fetch detailed information about an artist by ID. Use when showing artist profile pages.")
    public ArtistResponseDto getArtistById(@Parameter(description = "Artist ID") @PathVariable Long id) {
        return artistService.getArtistById(id);
    }
}
