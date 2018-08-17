package org.axonframework.extension.example.swarm.query;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@NamedQueries({
  @NamedQuery(name = "CustomerView.findById", query = "SELECT cv FROM CustomerView cv WHERE cv.id=:id")
})
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class CustomerView {
  @Id
  @Column(name = "uuid")
  private String id;

  @Column(name = "full_name")
  private String fullName;

  @Column(name = "age")
  private int age;

  public CustomerView() {
  }

  public CustomerView(String id, String fullName, int age) {
    this.id = id;
    this.fullName = fullName;
    this.age = age;
  }
}
