package com.sara.backend.controller;

import com.sara.backend.dto.response.WaitingRoomResponse;
import com.sara.backend.service.WaitingRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/queue-status")
public class WaitingRoomController {

    @Autowired
    private WaitingRoomService waitingRoomService;

    @GetMapping
    public WaitingRoomResponse showWaitingRoom() {
        return waitingRoomService.buildTodayWaitingRoom();
    }
}
