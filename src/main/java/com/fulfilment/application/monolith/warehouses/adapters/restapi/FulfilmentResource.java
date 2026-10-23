package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.usecases.FulfilmentUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("fulfilment")
@ApplicationScoped
public class FulfilmentResource {

  @Inject FulfilmentUseCase fulfilmentUseCase;

  @POST
  @Path("store/{storeId}/product/{productId}/warehouse/{buc}")
  @Transactional
  public Response associate(@PathParam("storeId") Long storeId, @PathParam("productId") Long productId, @PathParam("buc") String buc) {
    fulfilmentUseCase.associate(productId, storeId, buc);
    return Response.status(201).build();
  }
}

