package com.blueship581.hedwig.controller;

import com.blueship581.hedwig.dto.NightscoutTargetDto;
import com.blueship581.hedwig.dto.NightscoutTargetRequest;
import com.blueship581.hedwig.exception.ApiResult;
import com.blueship581.hedwig.service.NightscoutSyncService;
import com.blueship581.hedwig.service.NightscoutTargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nightscout")
@RequiredArgsConstructor
public class NightscoutController {

    private final NightscoutTargetService targetService;
    private final NightscoutSyncService nightscoutSyncService;

    // ==================== Nightscout 目标管理 ====================

    @GetMapping("/targets")
    public ApiResult<List<NightscoutTargetDto>> listTargets(
            @AuthenticationPrincipal UserDetails user) {
        return ApiResult.ok(targetService.listTargets(user.getUsername()));
    }

    @GetMapping("/targets/{id}")
    public ApiResult<NightscoutTargetDto> getTarget(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        return ApiResult.ok(targetService.getTarget(user.getUsername(), id));
    }

    @PostMapping("/targets")
    public ApiResult<NightscoutTargetDto> createTarget(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody NightscoutTargetRequest request) {
        return ApiResult.ok(targetService.createTarget(user.getUsername(), request), "创建成功");
    }

    @PutMapping("/targets/{id}")
    public ApiResult<NightscoutTargetDto> updateTarget(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @RequestBody NightscoutTargetRequest request) {
        return ApiResult.ok(targetService.updateTarget(user.getUsername(), id, request), "更新成功");
    }

    @DeleteMapping("/targets/{id}")
    public ApiResult<Void> deleteTarget(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        targetService.deleteTarget(user.getUsername(), id);
        return ApiResult.ok();
    }

    @PostMapping("/targets/{id}/toggle")
    public ApiResult<NightscoutTargetDto> toggleStatus(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        return ApiResult.ok(targetService.toggleStatus(user.getUsername(), id));
    }

    // ==================== 同步操作 ====================

    @PostMapping("/sync")
    public ApiResult<Integer> sync(@AuthenticationPrincipal UserDetails user) {
        int pushed = nightscoutSyncService.syncPendingReadings();
        return ApiResult.ok(pushed, pushed > 0 ? "同步完成" : "当前没有待同步的血糖数据");
    }
}
