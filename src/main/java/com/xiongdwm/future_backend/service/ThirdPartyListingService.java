package com.xiongdwm.future_backend.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.xiongdwm.future_backend.bo.ThirdPartyApplicationDto;
import com.xiongdwm.future_backend.bo.ThirdPartyListingDto;
import com.xiongdwm.future_backend.bo.ThirdPartyListingParam;
import com.xiongdwm.future_backend.platform.entity.ThirdPartyListing;

public interface ThirdPartyListingService {

    /** 发布挂单 */
    ThirdPartyListing post(ThirdPartyListingParam param, Long studioId, String studioName);

    /** 浏览大厅（分页，可按游戏类型过滤），currentStudioId 用于标记是否已申请 */
    Page<ThirdPartyListingDto> listOpen(int page, int size, String gameType, Long currentStudioId);

    /** 撤回自己的挂单 */
    void cancel(Long listingId, Long studioId);

    /** 审核（完成或炸单）*/
    void done(Long listingId, Long studioId, boolean passed, String failureReason);

    /** 申请接单 */
    void apply(Long listingId, Long studioId, String studioName, String note);

    /** 挂单方确认某个申请（接受）*/
    void confirmApplicant(Long applicationId, Long studioId, String customerId);

    /** 挂单方拒绝某个申请 */
    void rejectApplicant(Long applicationId, Long studioId);

    /** 查看某个挂单的所有申请（仅挂单方） */
    List<ThirdPartyApplicationDto> getApplicationsByListing(Long listingId, Long studioId);

    /** 查看自己提交的所有申请 */
    List<ThirdPartyApplicationDto> getMyApplications(Long studioId);

    /** 查看自己发布的所有挂单 */
    List<ThirdPartyListingDto> getMyListings(Long studioId);

    /** 申请方派单：根据申请数据创建二手订单 */
    String dispatch(Long applicationId, Long studioId, Long palId, java.util.List<Long> collaboratorPalIds);

    /** 同步Order的开工截图到申请和挂单（平台库操作） */
    void syncPicStart(String orderId, String picStart);

    /** 同步Order的完成截图到申请和挂单（平台库操作） */
    void syncPicEnd(String orderId, String picEnd);
}
