package com.salon.app.module.outlet.service;

import com.salon.app.module.outlet.dto.request.OutletRequest;
import com.salon.app.module.outlet.dto.response.OutletResponse;

import java.util.List;
import java.util.UUID;

public interface OutletService {
    List<OutletResponse> getAllActiveOutlets();
    List<OutletResponse> getAllOutlets();
    OutletResponse getOutletById(UUID id);
    OutletResponse createOutlet(OutletRequest request);
    OutletResponse updateOutlet(UUID id, OutletRequest request);
    void deleteOutlet(UUID id);
}
