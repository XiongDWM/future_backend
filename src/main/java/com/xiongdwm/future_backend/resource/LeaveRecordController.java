package com.xiongdwm.future_backend.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.bo.LeaveRecordParam;
import com.xiongdwm.future_backend.bo.PageableParam;
import com.xiongdwm.future_backend.entity.LeaveRecord;
import com.xiongdwm.future_backend.service.LeaveRecordService;
import com.xiongdwm.future_backend.utils.security.JwtTokenProvider;

import jakarta.annotation.Resource;

@RestController
public class LeaveRecordController {

    @Autowired
    private LeaveRecordService leaveRecordService;
    @Resource
    private JwtTokenProvider tokenProvider;

    @PostMapping("/leave/create")
    public ApiResponse<String> create(@RequestBody LeaveRecordParam record, @RequestHeader("Authorization") String token) {
        var user = tokenProvider.getUserFromRawToken(token);
        leaveRecordService.createLeaveRecord(record, user);
        return ApiResponse.success("休假申请成功");
    }

    /** 修改休假申请 */
    @PostMapping("/leave/update")
    public ApiResponse<String> update(@RequestBody LeaveRecord record) {
        leaveRecordService.updateLeaveRecord(record.getId(), record);
        return ApiResponse.success("修改成功");
    }

    /** 删除休假申请 */
    @PostMapping("/leave/delete")
    public ApiResponse<String> delete(@RequestBody Long id) {
        leaveRecordService.deleteLeaveRecord(id);
        return ApiResponse.success("删除成功");
    }

    /** 查询单条休假记录 */
    @PostMapping("/leave/get")
    public ApiResponse<LeaveRecord> get(@RequestBody Long id) {
        return ApiResponse.success(leaveRecordService.getLeaveRecord(id));
    }

    /** 销假 */
    @PostMapping("/leave/cancel")
    public ApiResponse<String> cancel(@RequestBody Long id) {
        leaveRecordService.cancelLeave(id);
        return ApiResponse.success("销假成功");
    }

    @PostMapping("/leave/list")
    public ApiResponse<Page<LeaveRecord>> list(@RequestBody PageableParam param) {
        var filters = param.getFilters();
        Long userId = filters != null && filters.containsKey("userId") && !filters.get("userId").isBlank()
                ? Long.valueOf(filters.get("userId"))
                : null;
        String type = filters != null ? filters.get("type") : null;
        var page = leaveRecordService.listLeaveRecords(param.getPageNumber() + 1, param.getPageSize(), userId, type);
        return ApiResponse.success(page);
    }
}
