package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouseUseCase;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;


@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private LocationResolver locationResolver;
  @Inject private CreateWarehouseUseCase createWarehouseUseCase;
  @Inject private ReplaceWarehouseUseCase replaceWarehouseUseCase;
  @Inject private ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @Override
  public Response listAllWarehousesUnits() {
    var list = warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
    return Response.ok(list).build();
  }

  @Override
  public Response createANewWarehouseUnit(@NotNull Warehouse data) {
    // validation
    Response validation = validateApiWarehouse(data);
    if (validation != null) {
      return validation;
    }
    var domainWarehouse = fromApiToDomain(data);
    createWarehouseUseCase.create(domainWarehouse);
    // return persisted representation (map domain model back to API bean)
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse persisted =
        warehouseRepository.findByBusinessUnitCode(data.getBusinessUnitCode());
    if (persisted == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("warehouse not found after creation").build();
    }
    var responseEntity = toWarehouseResponse(persisted);
    URI location = UriBuilder.fromPath("/warehouse").path(persisted.businessUnitCode).build();
    return Response.created(location).entity(responseEntity).build();
  }

  @Override
  public Response getAWarehouseUnitByID(String id) {
    var w = warehouseRepository.findDbById(Long.valueOf(id));
    if (w == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("Warehouse unit not found").build();
    }
    return Response.ok(toWarehouseResponse(w.toWarehouse())).build();
  }

  @Override
  public Response archiveAWarehouseUnitByID(String id) {
    // validate input
    if (id == null || id.trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("businessUnitCode is required").build();
    }
    var existing = warehouseRepository.findDbById(Long.valueOf(id));
    if (existing == null) {
      throw new WebApplicationException("Warehouse unit not found", 404);
    }
    if (existing.archivedAt != null) {
      throw new WebApplicationException("warehouse already archived", 400);
    }
    archiveWarehouseUseCase.archive(existing.toWarehouse());
    return Response.noContent().build();
  }

  @Override
  public Response replaceTheCurrentActiveWarehouse(
          String businessUnitCode, @NotNull Warehouse data) {
    // ensure the incoming payload uses the same BU code
    data.setBusinessUnitCode(businessUnitCode);
    Response validation = validateApiWarehouse(data);
    if (validation != null) {
      return validation;
    }
    var domain = fromApiToDomain(data);
    replaceWarehouseUseCase.replace(domain);
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse persisted =
        warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    if (persisted == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("warehouse not found after replacement").build();
    }
    return Response.ok(toWarehouseResponse(persisted)).build();
  }

  private Warehouse toWarehouseResponse(
          com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }

  // map incoming API bean to domain model used by use-cases/repository
  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse fromApiToDomain(
      com.warehouse.api.beans.Warehouse api) {
    var d = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    d.businessUnitCode = api.getBusinessUnitCode();
    d.location = api.getLocation();
    d.capacity = api.getCapacity();
    d.stock = api.getStock();
    return d;
  }

  // common request-level validation for API Warehouse bean
  private Response validateApiWarehouse(com.warehouse.api.beans.Warehouse data) {
    if (data == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Invalid request parameters: body required").build();
    }
    if (data.getBusinessUnitCode() == null || data.getBusinessUnitCode().trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Invalid request parameters: businessUnitCode").build();
    }
    if (data.getLocation() == null || data.getLocation().trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Invalid request parameters: location").build();
    }
    if (data.getCapacity() == null || data.getCapacity() < 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Invalid request parameters: capacity").build();
    }
    if (data.getStock() == null || data.getStock() < 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Invalid request parameters: stock").build();
    }
    return null;
  }

}
