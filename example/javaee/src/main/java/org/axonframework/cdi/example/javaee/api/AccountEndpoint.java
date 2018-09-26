package org.axonframework.cdi.example.javaee.api;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.axonframework.cdi.example.javaee.command.CreateAccountCommand;
import org.axonframework.cdi.example.javaee.command.DepositMoneyCommand;
import org.axonframework.cdi.example.javaee.command.WithdrawMoneyCommand;
import org.axonframework.cdi.example.javaee.query.AccountSummary;
import org.axonframework.cdi.example.javaee.query.AccountSummaryQuery;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.model.AggregateNotFoundException;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.responsetypes.ResponseTypes;

@Path("account")
public class AccountEndpoint {

    @Inject
    private CommandGateway commandGateway;

    @Inject
    private QueryGateway queryGateway;

    @Produces("application/json")
    @Consumes("application/json")
    @POST
    public Response createAccount(@Valid final CreateAccountRequest request) {
        try {
            commandGateway.sendAndWait(new CreateAccountCommand(request.accountId, request.overdraftLimit));
            return Response.ok().build();
        } catch (CommandExecutionException ex) {
            return Response.status(422).entity(ex.getCause().getMessage()).build();
        }
    }

    @Produces("application/json")
    @Consumes("application/json")
    @Path("{accountId}/withdraw")
    @POST
    public Response withdraw(@Valid final WithdrawMoneyRequest request,
            @PathParam("accountId") @NotNull String accountId) {
        try {
            commandGateway.sendAndWait(new WithdrawMoneyCommand(accountId, request.getAmount()));
            return Response.ok().build();
        } catch (AggregateNotFoundException ex) {
            return Response.status(Status.NOT_FOUND).entity("Could not find '" + accountId + "'.").build();
        } catch (CommandExecutionException ex) {
            return Response.status(422).entity(ex.getCause().getMessage()).build();
        }
    }

    @Produces("application/json")
    @Consumes("application/json")
    @Path("{accountId}/deposit")
    @POST
    public Response depositFromAccount(@Valid final DepositMoneyRequest request,
            @PathParam("accountId") @NotNull String accountId) {
        try {
            commandGateway.sendAndWait(new DepositMoneyCommand(accountId, request.getAmount()));
            return Response.ok().build();
        } catch (AggregateNotFoundException ex) {
            return Response.status(Status.NOT_FOUND).entity("Could not find '" + accountId + "'.").build();
        } catch (CommandExecutionException ex) {
            return Response.status(422).entity(ex.getCause().getMessage()).build();
        }
    }

    @Produces("application/json")
    @Consumes("application/json")
    @GET
    public Response getAccountProjection() {
        return Response.ok(queryGateway
                .query(new AccountSummaryQuery(), ResponseTypes.multipleInstancesOf(AccountSummary.class)).join())
                .build();
    }

}
