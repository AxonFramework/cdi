package org.axonframework.extension.example.swarm.rest;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.extension.example.swarm.aggregate.Customer;
import org.axonframework.extension.example.swarm.api.CreateCustomerCommand;
import org.axonframework.extension.example.swarm.query.CustomerView;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

@ApplicationScoped
@Path("/customer")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerEndpoint {

  @PersistenceContext(name = "MyPU")
  private EntityManager em;

  @Inject
  private CommandGateway commandGateway;

  @GET
  @Path("{id}")
  public Response getCustomer(@PathParam("id") String id) {
    CustomerView customer = em.createNamedQuery("CustomerView.findById", CustomerView.class)
      .setParameter("id", id).getSingleResult();
    return Response.ok(customer).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createCustomer(Customer customer) {
    String customerId = UUID.randomUUID().toString();
    commandGateway.sendAndWait(new CreateCustomerCommand(customerId, customer.getFullName(), customer.getAge()));
    return Response.created(URI.create("http://localhost:8080/customer/" + customerId)).build();
  }
}
