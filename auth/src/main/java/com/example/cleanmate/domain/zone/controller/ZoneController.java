package com.example.cleanmate.domain.zone.controller;

import com.example.cleanmate.global.dto.ReorderRequest;
import com.example.cleanmate.domain.zone.dto.AddZoneRequest;
import com.example.cleanmate.domain.zone.dto.RenameZoneRequest;
import com.example.cleanmate.domain.zone.dto.ZoneResponse;
import com.example.cleanmate.domain.zone.service.ZoneService;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping
    public ApiResponse<List<ZoneResponse>> getZones(
            @CookieValue(name = "CMSESSION", required = false) String sessionId) {
        return ApiResponse.ok(zoneService.getZones(sessionId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addZone(
            @CookieValue(name = "CMSESSION", required = false) String sessionId,
            @Valid @RequestBody AddZoneRequest request) {
        zoneService.addZone(sessionId, request.name());
        return ApiResponse.ok();
    }

    @PutMapping("/order")
    public ApiResponse<Void> reorderZones(
            @CookieValue(name = "CMSESSION", required = false) String sessionId,
            @Valid @RequestBody ReorderRequest request) {
        zoneService.reorderZones(sessionId, request.ids());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> renameZone(
            @CookieValue(name = "CMSESSION", required = false) String sessionId,
            @PathVariable Long id,
            @Valid @RequestBody RenameZoneRequest request) {
        zoneService.renameZone(sessionId, id, request.name());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteZone(
            @CookieValue(name = "CMSESSION", required = false) String sessionId,
            @PathVariable Long id) {
        zoneService.deleteZone(sessionId, id);
        return ApiResponse.ok();
    }
}
