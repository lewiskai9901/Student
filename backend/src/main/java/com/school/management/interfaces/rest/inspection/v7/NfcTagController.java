package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.NfcTagApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.platform.NfcTag;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v7/insp/nfc-tags")
public class NfcTagController {

    private final NfcTagApplicationService nfcTagService;

    @PostMapping
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<NfcTag> register(@RequestBody RegisterNfcTagRequest request) {
        return Result.success(nfcTagService.register(
                request.getTagUid(), request.getLocationName(),
                request.getPlaceId(), request.getOrgUnitId()));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<NfcTag> update(@PathVariable Long id, @RequestBody UpdateNfcTagRequest request) {
        return Result.success(nfcTagService.update(
                id, request.getLocationName(), request.getPlaceId(), request.getOrgUnitId()));
    }

    @PutMapping("/{id}/activate")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> activate(@PathVariable Long id) {
        nfcTagService.activate(id);
        return Result.success();
    }

    @PutMapping("/{id}/deactivate")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> deactivate(@PathVariable Long id) {
        nfcTagService.deactivate(id);
        return Result.success();
    }

    @GetMapping
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<List<NfcTag>> list(@RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return Result.success(nfcTagService.getActive());
        }
        return Result.success(nfcTagService.getAll());
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<NfcTag> getById(@PathVariable Long id) {
        return Result.success(nfcTagService.getById(id));
    }

    @GetMapping("/by-uid/{tagUid}")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<NfcTag> getByTagUid(@PathVariable String tagUid) {
        return Result.success(nfcTagService.getByTagUid(tagUid));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> delete(@PathVariable Long id) {
        nfcTagService.delete(id);
        return Result.success();
    }

    @Data
    public static class RegisterNfcTagRequest {
        private String tagUid;
        private String locationName;
        private Long placeId;
        private Long orgUnitId;
    }

    @Data
    public static class UpdateNfcTagRequest {
        private String locationName;
        private Long placeId;
        private Long orgUnitId;
    }
}
