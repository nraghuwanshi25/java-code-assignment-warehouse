package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("warehouse")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class WarehouseResourceImpl {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private LocationResolver locationResolver;
  @Inject private CreateWarehouseUseCase createWarehouseUseCase;
  @Inject private ReplaceWarehouseUseCase replaceWarehouseUseCase;
  @Inject private ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @GET
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @POST
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    // delegate validations & creation to use case
    createWarehouseUseCase.create(data);
    // return persisted representation
    return warehouseRepository.findByBusinessUnitCode(data.businessUnitCode);
  }

  @GET
  @Path("{id}")
  public Warehouse getAWarehouseUnitByID(@PathParam("id") String id) {
    var w = warehouseRepository.findByBusinessUnitCode(id);
    if (w == null) {
      throw new WebApplicationException("warehouse not found", 404);
    }
    return w;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response archiveAWarehouseUnitByID(@PathParam("id") String id) {
    var existing = warehouseRepository.findByBusinessUnitCode(id);
    if (existing == null) {
      throw new WebApplicationException("warehouse not found", 404);
    }
    archiveWarehouseUseCase.archive(existing);
    return Response.status(204).build();
  }

  @POST
  @Path("{businessUnitCode}/replacement")
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
      @PathParam("businessUnitCode") String businessUnitCode, @NotNull Warehouse data) {

    // ensure the incoming payload uses the same BU code
    data.businessUnitCode = businessUnitCode;
    replaceWarehouseUseCase.replace(data);
    return warehouseRepository.findByBusinessUnitCode(businessUnitCode);
  }

  private Warehouse toWarehouseResponse(
      Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
