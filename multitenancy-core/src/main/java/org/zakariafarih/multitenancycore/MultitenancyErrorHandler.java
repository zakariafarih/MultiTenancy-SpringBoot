package org.zakariafarih.multitenancycore;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class MultitenancyErrorHandler {

    @ExceptionHandler(UnknownTenantException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError unknownTenant(UnknownTenantException ex) {
        return ApiError.of("unknownTenant", ex.getMessage());
    }

    @ExceptionHandler(TenantNotResolvedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError noTenant(TenantNotResolvedException ex) {
        return ApiError.of("tenantNotResolved", ex.getMessage());
    }
}
