package edu.handong.csee.histudy.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTime {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long userId;

  @Column(unique = true)
  private String sub;

  @Column(unique = true)
  private String sid;

  @Column(unique = true)
  private String email;

  private String name;

  @Enumerated(EnumType.STRING)
  private Role role;

  @Builder
  public User(String sub, String sid, String email, String name, Role role) {
    this.sub = sub;
    this.sid = sid;
    this.email = email;
    this.name = name;
    this.role = role;
  }

  public void edit(String sid, String name) {
    this.sid = sid != null ? sid : this.sid;
    this.name = name != null ? name : this.name;
  }

  public String getSidWithMasking() {
    return this.sid.substring(0, 3) + "****" + this.sid.substring(7);
  }
}
