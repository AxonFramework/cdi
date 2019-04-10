package org.axonframework.cdi.example.javaee.api;

import java.math.BigDecimal;

import javax.enterprise.inject.Vetoed;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

@Vetoed
public class CreateAccountRequest {

    @NotNull
    @JsonProperty
    public String accountId;

    @NotNull
    @Min(0)
    @JsonProperty
    public BigDecimal overdraftLimit;

}
