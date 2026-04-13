package com.xiongdwm.future_backend.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xiongdwm.future_backend.bo.ThirdPartyApplicationDto;
import com.xiongdwm.future_backend.bo.ThirdPartyListingDto;
import com.xiongdwm.future_backend.bo.ThirdPartyListingParam;
import com.xiongdwm.future_backend.entity.Order;
import com.xiongdwm.future_backend.platform.entity.ThirdPartyApplication;
import com.xiongdwm.future_backend.platform.entity.ThirdPartyListing;
import com.xiongdwm.future_backend.platform.repository.ThirdPartyApplicationRepository;
import com.xiongdwm.future_backend.platform.repository.ThirdPartyListingRepository;
import com.xiongdwm.future_backend.service.OrderService;
import com.xiongdwm.future_backend.service.ThirdPartyListingService;
import com.xiongdwm.future_backend.utils.exception.ServiceException;
import com.xiongdwm.future_backend.utils.sse.GlobalEventBus;
import com.xiongdwm.future_backend.utils.sse.GlobalEventSpec;

@Service
public class ThirdPartyListingServiceImpl implements ThirdPartyListingService {

    private final ThirdPartyListingRepository listingRepository;
    private final ThirdPartyApplicationRepository applicationRepository;
    private final OrderService orderService;
    private final GlobalEventBus eventBus;
    private final GlobalEventSpec.Domain domain = GlobalEventSpec.Domain.THIRD_PARTY_LISTING;

    public ThirdPartyListingServiceImpl(ThirdPartyListingRepository listingRepository,
                                        ThirdPartyApplicationRepository applicationRepository,
                                        OrderService orderService,
                                        GlobalEventBus eventBus) {
        this.listingRepository = listingRepository;
        this.applicationRepository = applicationRepository;
        this.orderService = orderService;
        this.eventBus = eventBus;
    }

    @Override
    @Transactional("platformTransactionManager")
    public ThirdPartyListing post(ThirdPartyListingParam param, Long studioId, String studioName) {
        var listing = new ThirdPartyListing();
        listing.setStudioId(studioId);
        listing.setStudioName(studioName);
        listing.setGameType(param.gameType());
        listing.setRankInfo(param.rankInfo());
        listing.setDescription(param.description());
        listing.setOriginalPrice(param.originalPrice());
        listing.setPrice(param.price());
        listing.setCustomerTran(param.customerTran());
        if (param.orderType() != null && !param.orderType().isBlank()) {
            listing.setOrderType(ThirdPartyListing.OrderType.valueOf(param.orderType()));
        }
        listing.setPostedAt(new Date());
        listing.setStatus(ThirdPartyListing.Status.OPEN);
        listing = listingRepository.saveAndFlush(listing);
        // 广播：所有工作室均可看到新挂单
        eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.CREATE, true, listing.getId(), null);
        return listing;
    }

    @Override
    public Page<ThirdPartyListingDto> listOpen(int page, int size, String gameType, Long currentStudioId) {
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "postedAt"));
        Page<ThirdPartyListing> raw;
        boolean hasGame = gameType != null && !gameType.isBlank();
        if (currentStudioId != null) {
            // 登录状态：排除本工作室自己的挂单
            raw = hasGame
                    ? listingRepository.findByStatusAndGameTypeAndStudioIdNotOrderByPostedAtDesc(
                            ThirdPartyListing.Status.OPEN, gameType, currentStudioId, pageable)
                    : listingRepository.findByStatusAndStudioIdNotOrderByPostedAtDesc(
                            ThirdPartyListing.Status.OPEN, currentStudioId, pageable);
        } else {
            raw = hasGame
                    ? listingRepository.findByStatusAndGameTypeOrderByPostedAtDesc(
                            ThirdPartyListing.Status.OPEN, gameType, pageable)
                    : listingRepository.findByStatusOrderByPostedAtDesc(ThirdPartyListing.Status.OPEN, pageable);
        }
        return raw.map(l -> toDto(l, currentStudioId));
    }

    @Override
    @Transactional("platformTransactionManager")
    public void cancel(Long listingId, Long studioId) {
        var listing = requireOwner(listingId, studioId);
        if (listing.getStatus() != ThirdPartyListing.Status.OPEN) {
            throw new ServiceException("只有挂单中的甩单才可撤回");
        }
        listing.setStatus(ThirdPartyListing.Status.CANCELLED);
        listingRepository.saveAndFlush(listing);
        // 广播：所有工作室收到撤回通知
        eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.CANCEL, true, listingId, null);
    }

    @Override
    @Transactional("platformTransactionManager")
    public void done(Long listingId, Long studioId, boolean passed, String failureReason) {
        var listing = requireOwner(listingId, studioId);
        if (listing.getStatus() != ThirdPartyListing.Status.TAKEN) {
            throw new ServiceException("只有已接单的甩单才可审核");
        }
        if (passed) {
            listing.setStatus(ThirdPartyListing.Status.DONE);
        } else {
            if (failureReason == null || failureReason.isBlank()) {
                throw new ServiceException("炸单必须填写原因");
            }
            listing.setStatus(ThirdPartyListing.Status.FAILURE);
            listing.setFailureReason(failureReason);
            // 同步炸单状态到已接受的申请
            var acceptedApps = applicationRepository.findByListingIdAndStatus(listingId, ThirdPartyApplication.Status.ACCEPTED);
            for (var app : acceptedApps) {
                app.setStatus(ThirdPartyApplication.Status.FAILURE);
                app.setFailureReason(failureReason);
                applicationRepository.saveAndFlush(app);
                // 定向通知接单方：订单被标记为炸单
                eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.UPDATE, true, app.getId(), app.getStudioId());
            }
        }
        listingRepository.saveAndFlush(listing);
        // 广播：通知大厅此单状态变更
        eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.UPDATE, true, listingId, null);
    }

    @Override
    @Transactional("platformTransactionManager")
    public void apply(Long listingId, Long studioId, String studioName, String note) {
        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ServiceException("挂单不存在"));
        if (listing.getStatus() != ThirdPartyListing.Status.OPEN) {
            throw new ServiceException("该挂单已不再接受申请");
        }
        if (listing.getStudioId().equals(studioId)) {
            throw new ServiceException("不能申请自己的挂单");
        }
        if (applicationRepository.existsByListingIdAndStudioId(listingId, studioId)) {
            throw new ServiceException("已申请过该挂单，请勿重复提交");
        }
        var application = new ThirdPartyApplication();
        application.setListingId(listingId);
        application.setStudioId(studioId);
        application.setStudioName(studioName);
        application.setAppliedAt(new Date());
        application.setStatus(ThirdPartyApplication.Status.PENDING);
        application.setNote(note);
        application = applicationRepository.saveAndFlush(application);
        // 定向推送给挂单方：有新申请
        eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.CREATE, true, application.getId(), listing.getStudioId());
    }

    @Override
    @Transactional("platformTransactionManager")
    public void confirmApplicant(Long applicationId, Long studioId,String customerId) {
        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException("申请记录不存在"));
        var listing = requireOwner(application.getListingId(), studioId);
        if (listing.getStatus() != ThirdPartyListing.Status.OPEN) {
            throw new ServiceException("该挂单已不在接单中");
        }
        if (application.getStatus() != ThirdPartyApplication.Status.PENDING) {
            throw new ServiceException("该申请已处理");
        }
        // 接受此申请
        application.setStatus(ThirdPartyApplication.Status.ACCEPTED);
        // 同步挂单信息到申请
        application.setCustomerId(customerId);
        application.setOriginalPrice(listing.getOriginalPrice());
        application.setPrice(listing.getPrice());
        application.setCustomerTran(listing.isCustomerTran());
        application.setOrderType(listing.getOrderType());
        applicationRepository.saveAndFlush(application);
        // 挂单标记为已被接单
        listing.setStatus(ThirdPartyListing.Status.TAKEN);
        listing.setCustomerId(customerId);
        listingRepository.saveAndFlush(listing);
        // 自动拒绝同一挂单下其他 PENDING 的申请
        var otherPending = applicationRepository.findByListingIdAndStatus(
                listing.getId(), ThirdPartyApplication.Status.PENDING);
        for (var other : otherPending) {
            other.setStatus(ThirdPartyApplication.Status.REJECTED);
            applicationRepository.saveAndFlush(other);
            // 定向通知被拒绝方
            eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.DELETE, true, other.getId(), other.getStudioId());
        }
        // 定向推送给申请方：已被接受
        eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.UPDATE, true, applicationId, application.getStudioId());
    }

    @Override
    @Transactional("platformTransactionManager")
    public void rejectApplicant(Long applicationId, Long studioId) {
        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException("申请记录不存在"));
        requireOwner(application.getListingId(), studioId);
        if (application.getStatus() != ThirdPartyApplication.Status.PENDING) {
            throw new ServiceException("该申请已处理");
        }
        application.setStatus(ThirdPartyApplication.Status.REJECTED);
        applicationRepository.saveAndFlush(application);
        // 定向推送给申请方：已被拒绝
        eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.DELETE, true, applicationId, application.getStudioId());
    }

    @Override
    public List<ThirdPartyApplicationDto> getApplicationsByListing(Long listingId, Long studioId) {
        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ServiceException("挂单不存在"));
        if (!listing.getStudioId().equals(studioId)) {
            throw new ServiceException("无权查看该挂单的申请");
        }
        return applicationRepository.findByListingIdOrderByAppliedAtDesc(listingId)
                .stream().map(this::toAppDto).toList();
    }

    @Override
    public List<ThirdPartyApplicationDto> getMyApplications(Long studioId) {
        return applicationRepository.findByStudioIdOrderByAppliedAtDesc(studioId)
                .stream().map(this::toAppDto).toList();
    }

    @Override
    public List<ThirdPartyListingDto> getMyListings(Long studioId) {
        return listingRepository.findByStudioIdOrderByPostedAtDesc(studioId)
                .stream().map(l -> toDto(l, null)).toList();
    }

    // ─── private helpers ───────────────────────────────────────────────────────

    private ThirdPartyListing requireOwner(Long listingId, Long studioId) {
        var listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ServiceException("挂单不存在"));
        if (!listing.getStudioId().equals(studioId)) {
            throw new ServiceException("无权操作该挂单");
        }
        return listing;
    }

    private ThirdPartyListingDto toDto(ThirdPartyListing l, Long currentStudioId) {
        int appCount = applicationRepository.countByListingId(l.getId());
        boolean applied = currentStudioId != null
                && applicationRepository.existsByListingIdAndStudioId(l.getId(), currentStudioId);
        return new ThirdPartyListingDto(
                l.getId(), l.getStudioId(), l.getStudioName(),
                l.getGameType(), l.getRankInfo(), l.getDescription(),
                l.getOriginalPrice(), l.getPrice(), l.isCustomerTran(),
                l.getOrderType() != null ? l.getOrderType().name() : null,
                l.getPostedAt(), l.getStatus().name(),
                appCount, applied, l.getFailureReason(), l.getCustomerId(),
                l.getPicStart(), l.getPicEnd());
    }

    private ThirdPartyApplicationDto toAppDto(ThirdPartyApplication a) {
        // 查挂单信息补充 gameType 和 listingStudioName
        String gameType = null;
        String listingStudioName = null;
        var listingOpt = listingRepository.findById(a.getListingId());
        if (listingOpt.isPresent()) {
            var listing = listingOpt.get();
            gameType = listing.getGameType();
            listingStudioName = listing.getStudioName();
        }
        return new ThirdPartyApplicationDto(
                a.getId(), a.getListingId(), a.getStudioId(), a.getStudioName(),
                a.getAppliedAt(), a.getStatus().name(), a.getNote(), a.getFailureReason(),
                a.getCustomerId(), a.getOriginalPrice(), a.getPrice(),
                a.isCustomerTran(),
                a.getOrderType() != null ? a.getOrderType().name() : null,
                a.getOrderId(), a.getPicStart(), a.getPicEnd(),
                gameType, listingStudioName);
    }

    @Override
    @Transactional("platformTransactionManager")
    public String dispatch(Long applicationId, Long studioId, Long palId, List<Long> collaboratorPalIds) {
        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException("申请记录不存在"));
        if (!application.getStudioId().equals(studioId)) {
            throw new ServiceException("无权操作该申请");
        }
        if (application.getStatus() != ThirdPartyApplication.Status.ACCEPTED) {
            throw new ServiceException("只有已接受的申请才能派单");
        }
        if (application.getOrderId() != null) {
            throw new ServiceException("该申请已创建过工单");
        }
        var listing = listingRepository.findById(application.getListingId())
                .orElseThrow(() -> new ServiceException("挂单不存在"));

        // 根据 orderType 映射 Order.Type
        Order.Type orderType;
        if (listing.getOrderType() == ThirdPartyListing.OrderType.MALE) {
            orderType = Order.Type.SECOND_HAND;
        } else {
            orderType = Order.Type.SECOND_HAND_G;
        }

        // 计算收入分摊
        double income = application.getPrice();
        int totalPeople = 1 + (collaboratorPalIds != null ? collaboratorPalIds.size() : 0);
        double slice = income / totalPeople;

        // 创建工单（走租户库事务）
        var order = new Order();
        order.setType(orderType);
        order.setCustomer(application.getCustomerId());
        order.setResource(listing.getStudioName()); // 来源：发布方工作室
        order.setGameType(listing.getGameType());
        order.setRankInfo(listing.getRankInfo());
        order.setLowIncome(slice);
        order.setAmount(1);
        order.setUnitType(Order.UnitType.HOUR);
        order.setStatus(Order.Status.IN_PROGRESS);
        order.setPlatformSecondHand(true);
        order.setThirdPartyApplicationId(applicationId);
        order.setToWhom(application.getCustomerId());
        orderService.createOrder(order);

        // 派单给打手
        orderService.assignedOrderToUser(palId, order.getOrderId());

        // 为协作打手创建子单
        if (collaboratorPalIds != null) {
            for (Long collabPalId : collaboratorPalIds) {
                orderService.addCollaborator(order.getOrderId(), collabPalId, slice);
            }
        }

        // 回写 orderId 到申请（平台库）
        application.setOrderId(order.getOrderId());
        applicationRepository.saveAndFlush(application);

        // 通知申请方
        eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.UPDATE, true, applicationId, studioId);
        return order.getOrderId();
    }

    @Override
    @Transactional("platformTransactionManager")
    public void syncPicStart(String orderId, String picStart) {
        applicationRepository.findByOrderId(orderId).ifPresent(app -> {
            app.setPicStart(picStart);
            applicationRepository.saveAndFlush(app);
            listingRepository.findById(app.getListingId()).ifPresent(listing -> {
                listing.setPicStart(picStart);
                listingRepository.saveAndFlush(listing);
            });
            // 通知挂单方和申请方
            eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.UPDATE, true, app.getListingId(), null);
        });
    }

    @Override
    @Transactional("platformTransactionManager")
    public void syncPicEnd(String orderId, String picEnd) {
        applicationRepository.findByOrderId(orderId).ifPresent(app -> {
            app.setPicEnd(picEnd);
            applicationRepository.saveAndFlush(app);
            listingRepository.findById(app.getListingId()).ifPresent(listing -> {
                listing.setPicEnd(picEnd);
                listingRepository.saveAndFlush(listing);
            });
            // 通知挂单方和申请方
            eventBus.emitAfterCommitTo(domain, GlobalEventSpec.Action.UPDATE, true, app.getListingId(), null);
        });
    }

}
