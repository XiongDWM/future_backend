package com.xiongdwm.future_backend.service;

import java.util.List;

import com.xiongdwm.future_backend.bo.StudioListItemDto;
import com.xiongdwm.future_backend.bo.StudioRegisterParam;
import com.xiongdwm.future_backend.platform.entity.Studio;

public interface StudioService {
    Studio registerStudio(StudioRegisterParam param);
    void addUserToStudio(String username, Long studioId);
    boolean isUsernameTaken(String username);
    boolean isPlatformAdmin(String username);
    List<StudioListItemDto> listAllStudios();
}
