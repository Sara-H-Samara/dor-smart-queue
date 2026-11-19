package com.sara.backend.services;

import com.sara.backend.dtos.responses.WaitingRoomResponse;

public interface WaitingRoomService {

    WaitingRoomResponse buildTodayWaitingRoom();
}
